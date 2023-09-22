package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.preference.PreferenceManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.*
import com.jehutyno.yomikata.repository.QuizRepository
import com.jehutyno.yomikata.repository.SentenceRepository
import com.jehutyno.yomikata.repository.StatsRepository
import com.jehutyno.yomikata.repository.WordRepository
import com.jehutyno.yomikata.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Random


/**
 * Created by valentin on 18/10/2016.
 */
class QuizPresenter(
    val context: Context,
    private val quizRepository: QuizRepository, private val wordRepository: WordRepository,
    private val sentenceRepository: SentenceRepository, private val statsRepository: StatsRepository,
    private val quizView: QuizContract.View, private var quizIds: LongArray,
    private val strategy: QuizStrategy, private val level: Level?,
    private val quizTypes: ArrayList<QuizType>, private val rng: Random,
    coroutineScope: CoroutineScope) : QuizContract.Presenter {

    private val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)


    /**
     * Word handler
     *
     * Used to obtain either the current word in a normal quiz, or the current word in
     * an error session, depending on [errorMode]
     */
    private class WordHandler {
        /** False for normal session, True for error session (reviewing incorrect words).
         *  Does not apply for progressive study sessions */
        var errorMode = false

        var quizWords = listOf<Pair<Word, QuizType>>()
        var errors = arrayListOf<Pair<Word, QuizType>>()
        /** The index of quizWords of the current word */
        var currentItem = -1
        /** The index of errors, if you are currently in an error session */
        var currentItemErrorMode = -1

        /** Increment the active index */
        fun increment() {
            if (errorMode)
                currentItemErrorMode++
            else
                currentItem++
        }

        /** Set the active index to -1 */
        fun reset() {
            if (errorMode)
                currentItemErrorMode = -1
            else
                currentItem = -1
        }

        /** Returns the active index */
        fun getActiveIndex(): Int {
            return if (errorMode)
                currentItemErrorMode
            else
                currentItem
        }

        /** Get the current word depending on whether [errorMode] is True or False */
        fun getCurrentWord(index: Int? = null): Word {
            return if (errorMode)
                errors[index?: currentItemErrorMode].first
            else
                quizWords[index?: currentItem].first
        }

        /** Get the current quizType depending on whether [errorMode] is True or False */
        fun getCurrentQuizType(index: Int? = null): QuizType {
            return if (errorMode)
                errors[index?: currentItemErrorMode].second
            else
                quizWords[index?: currentItem].second
        }
    }
    private val wordHandler = WordHandler()
    private val quizWords get() = wordHandler.quizWords
    private val errors get() = wordHandler.errors

    private var randoms = arrayListOf<Pair<Word, Int>>() // We store the word and the color in order to be able to change it and keep it saved as well
    private var answers = arrayListOf<Answer>()
    private var currentSentence = Sentence() // TODO save instance state
    /** Active count to keep track of remaining words in current session.
     *  Keep in mind that a quiz could run out of words before this counter reaches 0. */
    private var sessionCount = -1
    /** Session length of choice in user preferences, only used for non-error session */
    private val prefSessionLength = defaultSharedPreferences.getString("length", "10")!!.toInt()
    /** Total length of current session */
    private var sessionLength = prefSessionLength

    private var ttsSupported = TextToSpeech.LANG_NOT_SUPPORTED
    private var isFuriDisplayed = false
    private var previousAnswerWrong = false  // true if and only if wrong choice in the current word

    private val wordsFlowJob: Job
    private lateinit var words: StateFlow<List<Word>>
    private val selectionsFlowJob: Job
    private lateinit var selections: StateFlow<List<Quiz>>

    init {
        wordsFlowJob = coroutineScope.launch {
            words = wordRepository.getWordsByLevel(quizIds, level).stateIn(coroutineScope)
        }
        selectionsFlowJob = coroutineScope.launch {
            selections = quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS).stateIn(coroutineScope)
        }
        isFuriDisplayed = defaultSharedPreferences.getBoolean(Prefs.FURI_DISPLAYED.pref, true)
        quizView.setPresenter(this)
    }

    override fun start() {
    }

    override suspend fun getWords() : List<Word> {
        wordsFlowJob.join()
        return words.value
    }

    override suspend fun getSelections(): List<Quiz> {
        selectionsFlowJob.join()
        return selections.value
    }

    // TODO: also save errorMode, errors
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList("answers", answers)
        outState.putBoolean("previousQCMAnswerWrong", previousAnswerWrong)
        if (randoms.size == 4) {
            outState.putParcelable("random0", randoms[0].first)
            outState.putParcelable("random1", randoms[1].first)
            outState.putParcelable("random2", randoms[2].first)
            outState.putParcelable("random3", randoms[3].first)
            outState.putInt("random0_color", randoms[0].second)
            outState.putInt("random1_color", randoms[1].second)
            outState.putInt("random2_color", randoms[2].second)
            outState.putInt("random3_color", randoms[3].second)
        }
        outState.putInt("session_count", sessionCount)
        val words = arrayListOf<Word>()
        val types = arrayListOf<QuizType>()
        quizWords.forEach {
            words.add(it.first)
            types.add(it.second)
        }
        LocalPersistence.witeObjectToFile(context, words, "words")
        LocalPersistence.witeObjectToFile(context, types, "types")

        outState.putInt("position", wordHandler.currentItem)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        quizView.reInitUI()
        previousAnswerWrong = savedInstanceState.getBoolean("previousQCMAnswerWrong")

        val random0: Word?
        val random1: Word?
        val random2: Word?
        val random3: Word?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            answers = savedInstanceState.getParcelableArrayList("answers", Answer::class.java)!!
            random0 = savedInstanceState.getParcelable("random0", Word::class.java)
            random1 = savedInstanceState.getParcelable("random1", Word::class.java)
            random2 = savedInstanceState.getParcelable("random2", Word::class.java)
            random3 = savedInstanceState.getParcelable("random3", Word::class.java)
        } else {
            @Suppress("DEPRECATION")
            answers = savedInstanceState.getParcelableArrayList("answers")!!
            @Suppress("DEPRECATION")
            random0 = savedInstanceState.getParcelable("random0")
            @Suppress("DEPRECATION")
            random1 = savedInstanceState.getParcelable("random1")
            @Suppress("DEPRECATION")
            random2 = savedInstanceState.getParcelable("random2")
            @Suppress("DEPRECATION")
            random3 = savedInstanceState.getParcelable("random3")
        }
        if (random0 != null) randoms.add(Pair(random0, savedInstanceState.getInt("random0_color")))
        if (random1 != null) randoms.add(Pair(random1, savedInstanceState.getInt("random1_color")))
        if (random2 != null) randoms.add(Pair(random2, savedInstanceState.getInt("random2_color")))
        if (random3 != null) randoms.add(Pair(random3, savedInstanceState.getInt("random3_color")))

        val wordsListRaw = LocalPersistence.readObjectFromFile(context, "words")
        val wordsList = wordsListRaw as ArrayList<*>
        val words = wordsListRaw.filterIsInstance<Word>()
        if (words.size != wordsList.size) {
            Log.e("Failed cast", "Some items in the read list of words were not of the type Word")
        }
        val typesListRaw = LocalPersistence.readObjectFromFile(context, "types")
        val typesList = typesListRaw as ArrayList<*>
        val types = typesListRaw.filterIsInstance<QuizType>()
        if (types.size != typesList.size) {
            Log.e("Failed cast", "Some items in the read list of quiz types were not of the type QuizType")
        }

        wordHandler.quizWords = words.indices.map { i -> Pair(words[i], types[i]) }
        quizView.displayWords(quizWords)
        wordHandler.currentItem = savedInstanceState.getInt("position") - 1 // -1 because setUpQuiz will do the +1
        quizView.setPagerPosition(wordHandler.currentItem)
        if (previousAnswerWrong)
            quizView.displayEditDisplayAnswerButton()
        runBlocking {
            setUpNextQuiz()
        }
        sessionCount = savedInstanceState.getInt("session_count")
    }

    /**
     * Init quiz
     *
     * Called when a new list of words is needed to start a quiz.
     *
     * (it is never called in errorMode)
     */
    override suspend fun initQuiz() {
        wordHandler.reset()     // make sure list index is reset

        wordHandler.quizWords = when (strategy) {
            QuizStrategy.PROGRESSIVE -> {
                getNextProgressiveWords()
            }
            QuizStrategy.STRAIGHT,
            QuizStrategy.SHUFFLE -> {
                loadWords()
            }
        }
        quizView.displayWords(quizWords)

        // To be sure the session length is not bigger than the number of words
        sessionLength = prefSessionLength.coerceAtMost(quizWords.size)

        if (prefSessionLength == -1) {
            // -1 => infinite session
            if (strategy == QuizStrategy.PROGRESSIVE) {
                // set length, count to 1
                sessionLength = 1
                quizView.incrementInfiniteCount()
            } else {
                // use size
                sessionLength = quizWords.size
            }
        }

        sessionCount = sessionLength    // initialize count

        setUpNextQuiz()
    }

    /**
     * Load words
     *
     * Get the Words and QuizTypes for a non-progressive style quiz.
     *
     * @return Pairs of Words and QuizTypes, shuffled if required by strategy.
     */
    override suspend fun loadWords(): List<Pair<Word, QuizType>> {
        val words = getWords()
        if (words.isEmpty()) {
            quizView.noWords()
            return listOf()
        }

        return createWordTypePair(
            if (strategy == QuizStrategy.SHUFFLE)
                words.shuffled()
            else
                words
        )
    }

    /**
     * Quiz type config
     *
     * @property showKeyboard True if keyboard should be shown. If false, multiple choice quiz is loaded.
     * @property convertHiragana Only applies if showKeyboard = true: convert user input to hiragana.
     * @property forceAudioAtStart If true: play audio at start regardless of user preference.
     * @property qcmDisplayHint String to display a hint, currently only applies to qcm (showKeyboard = false)
     * (e.g. what type of answer is expected from the user)
     */
    private inner class QuizTypeConfig(
        private val showKeyboard: Boolean, private val convertHiragana: Boolean,
        private val forceAudioAtStart: Boolean, private val qcmDisplayHint: String?
    ) {

        suspend fun setUpQuiz(quizType: QuizType, word: Word) {
            if (showKeyboard) {
                if (convertHiragana) {
                    quizView.setHiraganaConversion(word.isKana == 0)
                }
                quizView.showKeyboard()
                quizView.displayEditMode()
            } else {
                quizView.hideKeyboard()
                randoms = generateQCMRandoms(word, quizType, word.reading)

                qcmDisplayHint.let { text ->
                    quizView.displayQCMMode(text)
                }
            }

            if (forceAudioAtStart || defaultSharedPreferences.getBoolean("play_start", false)) {
                quizView.speakWord(word)
            }

            when (quizType) {
                QuizType.TYPE_PRONUNCIATION -> {}
                QuizType.TYPE_PRONUNCIATION_QCM -> setupQCMPronunciationQuiz()
                QuizType.TYPE_AUDIO -> setupQCMQAudioQuiz()
                QuizType.TYPE_EN_JAP -> setupQCMEnJapQuiz()
                QuizType.TYPE_JAP_EN -> setupQCMJapEnQuiz()
                QuizType.TYPE_AUTO -> TODO()
            }
        }

    }

    /**
     * Set up next quiz
     *
     * Moves to the next item in the pager adapter and shows the keyboard / multiple choice
     * depending on the QuizType of the next word.
     */
    override suspend fun setUpNextQuiz() {
        wordHandler.increment()

        val word = wordHandler.getCurrentWord()
        val quizType = wordHandler.getCurrentQuizType()

        quizView.setPagerPosition(wordHandler.getActiveIndex())

        currentSentence = getRandomSentence(word)
        quizView.setSentence(currentSentence)

        when (quizType) {
            QuizType.TYPE_PRONUNCIATION -> {
                QuizTypeConfig(
                    true, true, false, null
                )
            }
            QuizType.TYPE_PRONUNCIATION_QCM -> {
                QuizTypeConfig(
                    false, false, false,
                    context.getString(
                        if (word.isKana == 0)
                            R.string.give_hiragana_reading_hint
                        else
                            R.string.give_romaji_hint
                    )
                )
            }
            QuizType.TYPE_AUDIO -> {
                QuizTypeConfig(
                    false, false, true,
                    context.getString(R.string.give_word_or_kanji_hint)
                )
            }
            QuizType.TYPE_EN_JAP -> {
                QuizTypeConfig(
                    false, false, false,
                    context.getString(R.string.translate_to_japanese_hint)
                )
            }
            QuizType.TYPE_JAP_EN -> {
                QuizTypeConfig(
                    false, false, false,
                    context.getString(R.string.translate_to_english_hint)
                )
            }
            QuizType.TYPE_AUTO -> TODO()
        }.setUpQuiz(quizType, word)

        if (!wordHandler.errorMode)
            saveWordSeenStat(word)
    }

    private fun setupQCMPronunciationQuiz() {
        quizView.displayQCMNormalTextViews()
        quizView.displayQCMTv(
            randoms.map { it.first.reading.split("/")[0].split(";")[0].trim() },
            randoms.map { it.second }
        )
    }

    private fun setupQCMQAudioQuiz() {
        quizView.displayQCMNormalTextViews()
        quizView.displayQCMTv(
            randoms.map { it.first.japanese.split("/")[0].split(";")[0].trim() },
            randoms.map { it.second }
        )
    }

    private suspend fun setupQCMEnJapQuiz() {
        quizView.displayQCMFuriTextViews()
        val words = randoms.map { getQCMDisPlayForEnJap(it.first) }
        quizView.displayQCMFuri(
            words,
            words.map { 0 },
            words.map { it.length },
            randoms.map { it.second }
        )
    }

    private fun setupQCMJapEnQuiz() {
        quizView.displayQCMNormalTextViews()
        quizView.displayQCMTv(randoms.map{ it.first.getTrad().trim() }, randoms.map{ it.second })
    }

    private suspend fun getQCMDisPlayForEnJap(word: Word): String {
        return if (word.isKana == 2) {
            val sentence = sentenceRepository.getSentenceById(word.sentenceId!!)
            if (isFuriDisplayed)
                sentence.jap
            else
                sentenceNoFuri(sentence)
        }
        else if (isFuriDisplayed)
            " {${word.japanese};${word.reading}} "
        else
            word.japanese.trim()
    }

    fun getQCMLengthForAudio(word: Word): Int {
        return word.japanese.trim().length + 1
    }

    private suspend fun generateQCMRandoms(word: Word, quizType: QuizType, answerToAvoid: String): ArrayList<Pair<Word, Int>> {
        // Generate 3 different random words
        val random = getRandomWords(word.id, answerToAvoid, word.japanese.length, 3, quizType)
        // TODO: this may crash if getRandomWords returns less than 3 words
        val randoms = arrayListOf<Pair<Word, Int>>()
        random.forEach { randoms.add(Pair(it, android.R.color.white)) }
        // Add the good answer at a random place
        randoms.add(rng.nextInt(4), Pair(word, android.R.color.white))

        return randoms
    }

    /**
     * Create word type pair
     *
     * @param words List of Words
     * @return The original Words paired with a random [QuizType] (see [getQuizType])
     */
    private fun createWordTypePair(words: List<Word>): List<Pair<Word, QuizType>> {
        return words.map { word ->
            Pair(word, getQuizType(word))
        }
    }

    /**
     * Get quiz type
     *
     * @param word A word for which to generate a QuizType.
     * @return A randomly chosen type from the quizTypes. If AUTO is one of the selected quizTypes,
     * then a random type is chosen based on the difficulty of the type compared to the Word Level.
     */
    private fun getQuizType(word: Word): QuizType {
        if (!quizTypes.contains(QuizType.TYPE_AUTO))
            return quizTypes.random()

        // AUTO is selected
        // add types depending on difficulty
        val autoTypes = mutableListOf<QuizType>()

        // always add simplest quiz types
        autoTypes.add(QuizType.TYPE_PRONUNCIATION_QCM)
        autoTypes.add(QuizType.TYPE_JAP_EN)

        // add more more difficult quiz types depending on level
        if (word.level >= Level.MEDIUM) {
            autoTypes.add(QuizType.TYPE_EN_JAP)
            if (ttsSupported != TextToSpeech.LANG_MISSING_DATA && ttsSupported != TextToSpeech.LANG_NOT_SUPPORTED)
                autoTypes.add(QuizType.TYPE_AUDIO)
        }
        if (word.level >= Level.HIGH) {
            autoTypes.add(QuizType.TYPE_PRONUNCIATION)
        }

        return autoTypes.random()
    }

    override suspend fun onOptionClick(choice: Int) {
        onAnswerGiven(choice - 1)
    }

    override fun onDisplayAnswersClick() {
        quizView.openAnswersScreen(answers)
    }

    override fun onSpeakWordTTS() {
        quizView.speakWord(wordHandler.getCurrentWord())
    }

    override fun onSpeakSentence() {
        quizView.launchSpeakSentence(currentSentence)
    }

    private suspend fun onAnswerGiven(choice: Int) {
        val option = randoms[choice]
        when (wordHandler.getCurrentQuizType()) {
            QuizType.TYPE_PRONUNCIATION_QCM -> onAnswerGiven(option.first.reading.trim(), choice)
            QuizType.TYPE_JAP_EN -> onAnswerGiven(
                option.first.getTrad().trim(),
                choice)
            QuizType.TYPE_EN_JAP -> onAnswerGiven(
                option.first.japanese.trim(),
                choice)
            QuizType.TYPE_AUDIO -> onAnswerGiven(option.first.reading.trim(), choice)
            else -> {
            }
        }
    }

    override suspend fun onAnswerGiven(answer: String) {
        onAnswerGiven(answer, -1)
    }

    /**
     * On answer given
     *
     * Updates the word's stats. Adds the word to the given answers, and to errors if answer
     * was wrong. Saves result in stat_entry in database. Changes previousAnswerWrong according
     * to the result.
     *
     * Colors the word depending on the answer, and animates a check mark. Speaks the word
     * if correct answer and play_end is True in user preferences.
     *
     * @param answer User's answer
     * @param choice Index corresponding to multiple choice (0, 1, 2, 3), or -1 if keyboard entry
     */
    private suspend fun onAnswerGiven(answer: String, choice: Int) {
        val word = wordHandler.getCurrentWord()
        val quizType = wordHandler.getCurrentQuizType()
        val result = checkWord(word, quizType, answer)
        updateRepetitionAndPoints(word, quizType, result)
        if (!wordHandler.errorMode) {
            if (!previousAnswerWrong) {
                // add to answers list if this is the first time answering
                addCurrentWordToAnswers(answer, result)
                if (!result) {
                    // add to errors if answer is wrong, but is not a repeated mistake
                    wordHandler.errors.add(Pair(word, quizType))
                }
            }
            saveAnswerResultStat(word, result)
        }
        previousAnswerWrong = !result
        val color = if (result) R.color.level_master_4 else R.color.level_low_1
        when (quizType) {
            QuizType.TYPE_PRONUNCIATION -> {
                quizView.setEditTextColor(color)
            }
            QuizType.TYPE_PRONUNCIATION_QCM -> {
                if (choice == -1) Log.e("QuizTypeissue", "Type Issue. QuizType = ${quizType.type}, but choice -1")
                else randoms[choice] = Pair(randoms[choice].first, color)
                setupQCMPronunciationQuiz()
            }
            QuizType.TYPE_AUDIO -> {
                if (choice == -1) Log.e("QuizTypeissue", "Type Issue. QuizType = ${quizType.type}, but choice -1")
                else randoms[choice] = Pair(randoms[choice].first, color)
                setupQCMQAudioQuiz()
            }
            QuizType.TYPE_EN_JAP -> {
                if (choice == -1) Log.e("QuizTypeissue", "Type Issue. QuizType = ${quizType.type}, but choice -1")
                else randoms[choice] = Pair(randoms[choice].first, color)
                setupQCMEnJapQuiz()
            }
            QuizType.TYPE_JAP_EN -> {
                if (choice == -1) Log.e("QuizTypeissue", "Type Issue. QuizType = ${quizType.type}, but choice -1")
                else randoms[choice] = Pair(randoms[choice].first, color)
                setupQCMJapEnQuiz()
            }
            QuizType.TYPE_AUTO -> TODO()
        }

        if (result && defaultSharedPreferences.getBoolean("play_end", true))
            quizView.speakWord(wordHandler.getCurrentWord())

        quizView.animateCheck(result)
    }

    override fun onEditActionClick() {
        if (previousAnswerWrong)
            quizView.displayEditAnswer(wordHandler.getCurrentWord().reading)
        else
            quizView.clearEdit()
    }

    /**
     * Check word
     *
     * Checks if the given answer matches the correct word.
     *
     * @param word The correct word
     * @param quizType The quizType corresponding to the word
     * @param answer The user's answer. If keyboard entry, this method will parse it
     * @return True if answer matches word, False otherwise.
     */
    private fun checkWord(word: Word, quizType: QuizType, answer: String): Boolean {
        when (quizType) {
            QuizType.TYPE_JAP_EN -> {
                return word.getTrad().trim() == answer
            }
            QuizType.TYPE_EN_JAP -> {
                return word.japanese.trim() == answer
            }
            else -> {
                word.reading.split("/").forEach {
                    if (it.trim() == answer.trim().replace("-", "ー")) {
                        return true
                    }
                }
                word.reading.split(";").forEach {
                    if (it.trim() == answer.trim().replace("-", "ー")) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Update repetition and points
     *
     * Updates the word's points, level, repetition in the database and in memory.
     * Also animates the color change caused by the change in points.
     *
     * Does nothing if previousAnswerWrong.
     *
     * @param word Word
     * @param quizType QuizType
     * @param result True if correct answer, False if wrong answer
     */
    private suspend fun updateRepetitionAndPoints(word: Word, quizType: QuizType, result: Boolean) {
        if (previousAnswerWrong) {
            return  // do not update since the user already got it wrong on this word
        }

        val speed = defaultSharedPreferences.getString("speed", "2")!!.toInt()

        val newPoints = addPoints(word.points, result, quizType, speed)
        val newLevel = getLevelFromPoints(newPoints)
        val newRepetition = getRepetition(newPoints, result)

        // update database word
        updateWordPoints(word.id, newPoints)
        updateWordLevel(word.id, newLevel)
        updateRepetitions(word.id, newRepetition)

        quizView.animateColor(wordHandler.getActiveIndex(), word, currentSentence, quizType, word.points, newPoints)

        // update in-memory word
        word.level = newLevel
        word.points = newPoints
    }

    private fun addCurrentWordToAnswers(answer: String, result: Boolean) {
        val word = wordHandler.getCurrentWord()
        val color = if (result) "#77d228" else "#d22828"
        if (answers.size > 0 && answers[0].wordId == word.id) {
            answers[0].answer += "<br><font color='$color'>$answer</font>"
        } else {
            answers.add(0, Answer(
                if (result) 1 else 0,
                "<font color='$color'>$answer</font>",
                word.id,
                currentSentence.id,
                wordHandler.getCurrentQuizType())
            )
        }
    }

    /**
     * On next word
     *
     * Decrements sessionCount, resets previousAnswerWrong.
     *
     * Shows a dialog depending on whether the session (or the entire quiz) has ended,
     * and also depending on quiz strategy.
     */
    override suspend fun onNextWord() {
        if (!wordHandler.errorMode) decreaseAllRepetitions()

        sessionCount--
        previousAnswerWrong = false
        quizView.reInitUI()

        // set to true if normal words have run out
        val quizEnded = wordHandler.currentItem >= quizWords.size - 1

        // handle errorMode
        if (wordHandler.errorMode) {
            // check if you have reached the end of error list
            if (wordHandler.currentItemErrorMode >= errors.size - 1) {
                quizView.showAlertErrorSessionEnd(quizEnded)
            } else {
                setUpNextQuiz()
            }
            return
        }

        // handle progressive session first, since they do not have a normal quiz end
        if (strategy == QuizStrategy.PROGRESSIVE) {
            // check if infinite session (only for progressive, since other strategies
            // set sessionCount = size, so they are not really infinite)
            if (prefSessionLength == -1) {
                // infinite session -> continuously load new session
                onLaunchNextProgressiveSession()
                return
            }
        }
        else if (quizEnded) {   // this is mutually exclusive with progressive strategy
            // quiz has completely ended -> replace errors with ALL previous errors
            // use answers with result == 0 <-> error
            wordHandler.errors.clear()
            answers.filter { it.result == 0 }
                   .mapTo(wordHandler.errors) { Pair(getWord(it.wordId), it.quizType) }
            quizView.showAlertQuizEnd(errors.size > 0)
            return
        }

        if (sessionCount == 0) {
            // end of session
            sessionCount = sessionLength    // reset count
            if (strategy == QuizStrategy.PROGRESSIVE) {
                quizView.showAlertProgressiveSessionEnd()
            } else {
                quizView.showAlertNonProgressiveSessionEnd(errors.size > 0)
            }
            return
        }

        // not the end of the session -> keep going
        setUpNextQuiz()
    }

    override suspend fun onLaunchErrorSession() {
        wordHandler.errorMode = true
        wordHandler.reset()
        errors.shuffle()
        quizView.displayWords(errors)
        setUpNextQuiz()
    }

    override suspend fun onLaunchNextProgressiveSession() {
        initQuiz()  // this is only called when you've run out of words => re-initialize
    }

    override suspend fun onContinueQuizAfterErrorSession() {
        wordHandler.errorMode = false
        wordHandler.errors.clear()
        quizView.displayWords(quizWords)
        setUpNextQuiz()
    }

    override suspend fun onContinueAfterNonProgressiveSessionEnd() {
        wordHandler.errors.clear()
        setUpNextQuiz()
    }

    override suspend fun onRestartQuiz() {
        wordHandler.errorMode = false
        wordHandler.errors.clear()
        answers.clear()
        initQuiz()
    }

    override fun onFinishQuiz() {
        quizView.hideKeyboard()
        quizView.finishQuiz()
    }


    override suspend fun createSelection(quizName: String): Long {
        return quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override suspend fun addWordToSelection(wordId: Long, quizId: Long) {
        quizRepository.addWordToQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuiz(wordId: Long, quizId: Long): Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override suspend fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override suspend fun deleteWordFromSelection(wordId: Long, selectionId: Long) {
        quizRepository.deleteWordFromQuiz(wordId, selectionId)
    }

    override suspend fun updateWordPoints(wordId: Long, points: Int) {
        wordRepository.updateWordPoints(wordId, points)
    }

    override suspend fun updateWordLevel(wordId: Long, level: Level) {
        wordRepository.updateWordLevel(wordId, level)
    }

    override suspend fun getRandomWords(wordId: Long, answer: String, wordSize: Int, limit: Int, quizType: QuizType): ArrayList<Word> {
        return wordRepository.getRandomWords(wordId, answer, wordSize, limit, quizType)
    }

    /**
     * Get next progressive words
     *
     * Returned words are shuffled.
     *
     * @return Pairs of Words and QuizTypes based on their repetition value. Zero-repetition words and
     * new words are returned, along with enough other words to fill the [sessionLength].
     */
    override suspend fun getNextProgressiveWords(): List<Pair<Word, QuizType>> {
        // first get words that need to be reviewed (rep = 0)
        val words = wordRepository.getWordsByRepetition(quizIds, 0, prefSessionLength)
        // if session length not reached yet, get completely new words (rep = -1)
        if (words.size < prefSessionLength || (words.size == 0 && prefSessionLength == -1)) {
            words.addAll(wordRepository.getWordsByRepetition(quizIds, -1, prefSessionLength - words.size))
        }
        // fill up to session length with other words (rep >= 1)
        if (words.size < prefSessionLength || (words.size == 0 && prefSessionLength == -1)) {
            words.addAll(wordRepository.getWordsByMinRepetition(quizIds, 1, prefSessionLength - words.size))
        }
        if (prefSessionLength == -1) {
            if (words.isNotEmpty()) {
                // keep only one word if session = infinite
                val first = words[0]
                words.clear()
                words.add(first)
            }
        }

        words.shuffle()

        return createWordTypePair(words)
    }

    override suspend fun getWord(id: Long): Word {
        return wordRepository.getWordById(id)
    }

    /**
     * Get random sentence
     *
     * @param word Word to find a sentence for
     * @return A random sentence containing the Word.
     */
    override suspend fun getRandomSentence(word: Word): Sentence {
        val sentence = sentenceRepository.getRandomSentence(word, getCategoryLevel(word.baseCategory))
        return if (word.isKana == 2 || sentence == null)
            sentenceRepository.getSentenceById(word.sentenceId!!)
        else
            sentence
    }

    override suspend fun updateRepetitions(id: Long, repetition: Int) {
        wordRepository.updateWordRepetition(id, repetition)
    }

    override suspend fun decreaseAllRepetitions() {
        wordRepository.decreaseWordsRepetition(quizIds)
    }

    override suspend fun saveAnswerResultStat(word: Word, result: Boolean) {
        statsRepository.addStatEntry(StatAction.ANSWER_QUESTION, word.id,
            Calendar.getInstance().timeInMillis, if (result) StatResult.SUCCESS else StatResult.FAIL)
    }

    override suspend fun saveWordSeenStat(word: Word) {
        statsRepository.addStatEntry(StatAction.WORD_SEEN, word.id,
            Calendar.getInstance().timeInMillis, StatResult.OTHER)
    }

    override fun setTTSSupported(ttsSupported: Int) {
        this.ttsSupported = ttsSupported
    }

    override fun getTTSForCurrentItem(): String {
        val word = wordHandler.getCurrentWord()
        return if (word.isKana >= 1)
            word.japanese.split("/")[0].split(";")[0]
        else word.reading.split("/")[0].split(";")[0]
    }

    override suspend fun setIsFuriDisplayed(isFuriDisplayed: Boolean) {
        this.isFuriDisplayed = isFuriDisplayed
        if (wordHandler.getCurrentQuizType() == QuizType.TYPE_EN_JAP) {
            setupQCMEnJapQuiz()
        }
    }

    override fun onReportClick(position: Int) {
        quizView.reportError(wordHandler.getCurrentWord(position), currentSentence)
    }

    override fun previousAnswerWrong(): Boolean {
        return previousAnswerWrong
    }

}
