package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
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
import java.util.*


/**
 * Created by valentin on 18/10/2016.
 */
class QuizPresenter(
    val context: Context,
    private val quizRepository: QuizRepository, private val wordRepository: WordRepository, private val sentenceRepository: SentenceRepository,
    private val statsRepository: StatsRepository, private val quizView: QuizContract.View,
    private var quizIds: LongArray, private var strategy: QuizStrategy, private val quizTypes: ArrayList<QuizType>,
    coroutineScope: CoroutineScope) : QuizContract.Presenter {

    private val defaultSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var quizWords = listOf<Pair<Word, QuizType>>()
    private var errors = arrayListOf<Pair<Word, QuizType>>()
    private var randoms = arrayListOf<Pair<Word, Int>>() // We store the word and the color in order to be able to change it and keep it saved as well
    private var answers = arrayListOf<Answer>()
    private var currentSentence = Sentence() // TODO save instance state
    private var currentItem = -1
    private var currentItemBackup = -1
    private var sessionCount = -1
    private var sessionLength = defaultSharedPreferences.getString("length", "10")?.toInt()

    private var ttsSupported = TextToSpeech.LANG_NOT_SUPPORTED
    private var isFuriDisplayed = false
    private var previousAnswerWrong = false  // true if and only if wrong choice in the current word
    private var errorMode = false
    private var quizEnded = false

    private val wordsFlowJob: Job
    private lateinit var words: StateFlow<List<Word>>
    private val selectionsFlowJob: Job
    private lateinit var selections: StateFlow<List<Quiz>>

    init {
        wordsFlowJob = coroutineScope.launch {
            words = wordRepository.getWordsByLevel(quizIds, getQuizLevelIfAny(strategy)).stateIn(coroutineScope)
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList("errors", answers)
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

        outState.putInt("position", currentItem)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        quizView.reInitUI()
        previousAnswerWrong = savedInstanceState.getBoolean("previousQCMAnswerWrong")

        val random0: Word?
        val random1: Word?
        val random2: Word?
        val random3: Word?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            answers = savedInstanceState.getParcelableArrayList("errors", Answer::class.java)!!
            random0 = savedInstanceState.getParcelable("random0", Word::class.java)
            random1 = savedInstanceState.getParcelable("random1", Word::class.java)
            random2 = savedInstanceState.getParcelable("random2", Word::class.java)
            random3 = savedInstanceState.getParcelable("random3", Word::class.java)
        } else {
            @Suppress("DEPRECATION")
            answers = savedInstanceState.getParcelableArrayList("errors")!!
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

        quizWords = words.indices.map { i -> Pair(words[i], types[i]) }
        quizView.displayWords(quizWords)
        currentItem = savedInstanceState.getInt("position") - 1 // -1 because setUpQuiz will do the +1
        quizView.setPagerPosition(currentItem)
        if (previousAnswerWrong)
            quizView.displayEditDisplayAnswerButton()
        runBlocking {
            setUpNextQuiz()
        }
        sessionCount = savedInstanceState.getInt("session_count")
    }

    override suspend fun initQuiz() {
        sessionCount = defaultSharedPreferences.getString("length", "10")!!.toInt()
        quizWords = when (strategy) {
            QuizStrategy.STRAIGHT, QuizStrategy.SHUFFLE, QuizStrategy.LOW_STRAIGHT, QuizStrategy.MEDIUM_STRAIGHT,
            QuizStrategy.HIGH_STRAIGHT, QuizStrategy.MASTER_STRAIGHT, QuizStrategy.LOW_SHUFFLE,
            QuizStrategy.MEDIUM_SHUFFLE, QuizStrategy.HIGH_SHUFFLE, QuizStrategy.MASTER_SHUFFLE -> {
                loadWords()
            }
            QuizStrategy.PROGRESSIVE -> {
                getNextProgressiveWords()
            }
        }
        quizView.displayWords(quizWords)
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
        quizWords =
            createWordTypePair(
                if (strategy.isShuffleType())
                    words.shuffled()
                else
                    words
            )

        return quizWords
    }

    /**
     * Set up next quiz
     *
     * Moves to the next item in the pager adapter and shows the keyboard / multiple choice
     * depending on the QuizType of the next word.
     */
    override suspend fun setUpNextQuiz() {
        if (!errorMode && currentItem != -1) decreaseAllRepetitions()
        currentItem++
        val quizType = if (errorMode) errors[currentItem].second else quizWords[currentItem].second
        val word = if (errorMode) errors[currentItem].first else quizWords[currentItem].first
        currentSentence = getRandomSentence(word)
        quizView.setSentence(currentSentence)
        quizView.setPagerPosition(currentItem)
        when (quizType) {
            QuizType.TYPE_PRONUNCIATION -> {
                // Keyboard
                quizView.showKeyboard()
                quizView.setHiraganaConversion(word.isKana == 0)
                quizView.displayEditMode()
                // TTS at start
                if (defaultSharedPreferences.getBoolean("play_start", false))
                    quizView.speakWord(if (errorMode) errors[currentItem].first else quizWords[currentItem].first)
            }
            QuizType.TYPE_PRONUNCIATION_QCM -> {
                // Keyboard
                quizView.hideKeyboard()
                quizView.displayQCMMode(if (word.isKana == 0)
                                            context.getString(R.string.give_hiragana_reading_hint)
                                        else
                                            context.getString(R.string.give_romaji_hint))
                // TTS at start
                if (defaultSharedPreferences.getBoolean("play_start", false))
                    quizView.speakWord(if (errorMode) errors[currentItem].first else quizWords[currentItem].first)
                // QCM options
                randoms = generateQCMRandoms(word, quizType, word.reading)
                setupQCMPronunciationQuiz()
            }
            QuizType.TYPE_AUDIO -> {
                // Keyboard
                quizView.hideKeyboard()
                quizView.displayQCMMode(context.getString(R.string.give_word_or_kanji_hint))
                // TTS at start
                quizView.speakWord(if (errorMode) errors[currentItem].first else quizWords[currentItem].first)
                // QCM options
                randoms = generateQCMRandoms(word, quizType, word.japanese)
                setupQCMQAudioQuiz()
            }
            QuizType.TYPE_EN_JAP -> {
                // Keyboard
                quizView.hideKeyboard()
                quizView.displayQCMMode(context.getString(R.string.translate_to_japanese_hint))
                // TTS at stat
                if (defaultSharedPreferences.getBoolean("play_start", false))
                    quizView.speakWord(if (errorMode) errors[currentItem].first else quizWords[currentItem].first)
                // QCM options
                randoms = generateQCMRandoms(word, quizType, word.japanese)
                setupQCMEnJapQuiz()
            }
            QuizType.TYPE_JAP_EN -> {
                // Keyboard
                quizView.hideKeyboard()
                quizView.displayQCMMode(context.getString(R.string.translate_to_english_hint))
                // TTS at start
                if (defaultSharedPreferences.getBoolean("play_start", false))
                    quizView.speakWord(if (errorMode) errors[currentItem].first else quizWords[currentItem].first)
                // QCM Options
                randoms = generateQCMRandoms(word, quizType, word.japanese)
                setupQCMJapEnQuiz()
            }
            QuizType.TYPE_AUTO -> TODO()
        }

        if (!errorMode)
            saveWordSeenStat(word)
    }

    private fun setupQCMPronunciationQuiz() {
        quizView.displayQCMNormalTextViews()
        for (i in 0..3) {
            quizView.displayQCMTv(i + 1,
                randoms[i].first.reading.split("/")[0].split(";")[0].trim(),
                randoms[i].second
            )
        }
    }

    private fun setupQCMQAudioQuiz() {
        quizView.displayQCMNormalTextViews()
        for (i in 0..3) {
            quizView.displayQCMTv(i + 1,
                randoms[i].first.japanese.split("/")[0].split(";")[0].trim(),
                randoms[i].second
            )
        }
    }

    private suspend fun setupQCMEnJapQuiz() {
        quizView.displayQCMFuriTextViews()
        for (i in 0..3) {
            val word = getQCMDisPlayForEnJap(randoms[i].first)
            quizView.displayQCMFuri(i + 1,
                word, 0, word.length, ContextCompat.getColor(context, randoms[i].second)
            )
        }
    }

    private fun setupQCMJapEnQuiz() {
        quizView.displayQCMNormalTextViews()
        for (i in 0..3) {
            quizView.displayQCMTv(i + 1, randoms[i].first.getTrad().trim(), randoms[i].second)
        }
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
        val randoms = arrayListOf<Pair<Word, Int>>()
        random.forEach { randoms.add(Pair(it, android.R.color.white)) }
        // Add the good answer at a random place
        randoms.add(Random().nextInt(4), Pair(word, android.R.color.white))

        return randoms
    }

    /**
     * Get quiz level if any
     *
     * @return The level of a strategy, or -1 if strategy does not correspond to any specific level
     * such as QuizStrategy.SHUFFLE
     */
    private fun getQuizLevelIfAny(strategy: QuizStrategy): Int {
        return when(strategy) {
            QuizStrategy.LOW_STRAIGHT -> 0
            QuizStrategy.MEDIUM_STRAIGHT -> 1
            QuizStrategy.HIGH_STRAIGHT -> 2
            QuizStrategy.MASTER_STRAIGHT -> 3
            QuizStrategy.LOW_SHUFFLE -> 0
            QuizStrategy.MEDIUM_SHUFFLE -> 1
            QuizStrategy.HIGH_SHUFFLE -> 2
            QuizStrategy.MASTER_SHUFFLE -> 3
            else -> -1
        }
    }

    /**
     * Create word type pair
     *
     * @param words List of Words
     * @return The original Words paired with a random [QuizType] (see [getQuizType])
     */
    private fun createWordTypePair(words: List<Word>): List<Pair<Word, QuizType>> {
        if (words.size < sessionLength!! || words.size < sessionCount) {
            sessionLength = words.size // To be sure the session length is not bigger than the number of words
            sessionCount = words.size
        }
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
        if (word.level.atLeast(Level.LOW)) {
            autoTypes.add(QuizType.TYPE_PRONUNCIATION_QCM)
            autoTypes.add(QuizType.TYPE_JAP_EN)
        }
        if (word.level.atLeast(Level.MEDIUM)) {
            autoTypes.add(QuizType.TYPE_EN_JAP)
            if (ttsSupported != TextToSpeech.LANG_MISSING_DATA && ttsSupported != TextToSpeech.LANG_NOT_SUPPORTED)
                autoTypes.add(QuizType.TYPE_AUDIO)
        }
        if (word.level.atLeast(Level.HIGH)) {
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
        quizView.speakWord(if (errorMode) errors[currentItem].first else quizWords[currentItem].first)
    }

    override fun onSpeakSentence() {
        quizView.launchSpeakSentence(currentSentence)
    }

    private suspend fun onAnswerGiven(choice: Int) {
        val option = randoms[choice]
        when (if (errorMode) errors[currentItem].second else quizWords[currentItem].second) {
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

    private suspend fun onAnswerGiven(answer: String, choice: Int) {
        val word = if (errorMode) errors[currentItem].first else quizWords[currentItem].first
        val quizType = if (errorMode) errors[currentItem].second else quizWords[currentItem].second
        val result = checkWord(word, quizType, answer)
        updateRepetitionAndPoints(word, quizType, result)
        if (!errorMode) {
            addCurrentWordToAnswers(answer)
            saveAnswerResultStat(word, result)
        }
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
            quizView.speakWord(if (errorMode) errors[currentItem].first else quizWords[currentItem].first)

        quizView.animateCheck(result)

    }

    override fun onEditActionClick() {
        if (previousAnswerWrong)
            quizView.displayEditAnswer(if (errorMode) errors[currentItem].first.reading else quizWords[currentItem].first.reading)
        else
            quizView.clearEdit()
    }

    private fun checkWord(word: Word, quizType: QuizType, answer: String): Boolean {
        var result = false

        when (quizType) {
            QuizType.TYPE_JAP_EN -> {
                result = word.getTrad().trim() == answer
            }
            QuizType.TYPE_EN_JAP -> {
                result = word.japanese.trim() == answer
            }
            else -> {
                word.reading.split("/").forEach {
                    if (it.trim() == answer.trim().replace("-", "ー")) {
                        result = true
                    }
                }
                word.reading.split(";").forEach {
                    if (it.trim() == answer.trim().replace("-", "ー")) {
                        result = true
                    }
                }
            }
        }

        return result
    }

    private suspend fun updateRepetitionAndPoints(word: Word, quizType: QuizType, result: Boolean) {
        if (previousAnswerWrong) {
            return  // do not update since the user already got it wrong on this word
        }
        previousAnswerWrong = !result

        val speed = defaultSharedPreferences.getString("speed", "2")!!.toInt()

        val newPoints = addPoints(word.points, result, quizType, speed)
        val newLevel = getLevelFromPoints(newPoints)
        val newRepetition = getRepetition(newPoints, result)

        // update database word
        updateWordPoints(word.id, newPoints)
        updateWordLevel(word.id, newLevel)
        updateRepetitions(word.id, newRepetition)

        quizView.animateColor(currentItem, word, currentSentence, quizType, word.points, newPoints)

        // update in-memory word
        word.level = newLevel
        word.points = newPoints
    }

    private fun addCurrentWordToAnswers(answer: String) {
        val word = quizWords[currentItem].first
        val color = if (!previousAnswerWrong) "#77d228" else "#d22828'"
        if (answers.size > 0 && answers[0].wordId == word.id) {
            answers[0].answer += "<br><font color='$color'>$answer</font>"
        } else {
            answers.add(0, Answer(
                if (previousAnswerWrong) 0 else 1,
                "<font color='$color'>$answer</font>",
                word.id,
                currentSentence.id,
                quizWords[currentItem].second)
            )
        }
    }

    override suspend fun onNextWord() {
        sessionCount--
        previousAnswerWrong = false
        quizView.reInitUI()

        if (!errorMode) {
            quizEnded =
                if (strategy == QuizStrategy.PROGRESSIVE) false
                else currentItem >= quizWords.size - 1
        }
        if (sessionCount == 0 || (!errorMode && quizEnded)) {
            if (errorMode) {
                quizView.showAlertErrorSessionEnd(quizEnded)
            } else {
                errors.clear()
                val errorLength = if (strategy != QuizStrategy.PROGRESSIVE && quizEnded)
                    answers.size - 1
                else
                    sessionLength!! - 1
                (errorLength downTo 0)
                    .filter { it < answers.size && it >= 0 }
                    .filter { answers[it].result == 0 }
                    .mapTo(errors) { Pair(getWord(answers[it].wordId), answers[it].quizType) }

                if (strategy == QuizStrategy.PROGRESSIVE)
                    quizView.showAlertProgressiveSessionEnd(errors.size > 0)
                else {
                    if (quizEnded)
                        quizView.showAlertQuizEnd(errors.size > 0)
                    else
                        quizView.showAlertNonProgressiveSessionEnd(errors.size > 0)
                }
            }
        } else {
            setUpNextQuiz()
        }
    }

    override suspend fun onLaunchErrorSession() {
        currentItemBackup = currentItem
        sessionCount = errors.size
        currentItem = -1
        errorMode = true
        quizView.displayWords(errors.shuffled())
        setUpNextQuiz()
    }

    override suspend fun onLaunchNextProgressiveSession() {
        sessionCount = if (quizWords.size < defaultSharedPreferences.getString("length", "10")!!.toInt()) quizWords.size else defaultSharedPreferences.getString("length", "10")!!.toInt()
        currentItem = -1
        initQuiz()
    }


    override suspend fun onContinueQuizAfterErrorSession() {
        errorMode = false
        sessionCount = if (quizWords.size < defaultSharedPreferences.getString("length", "10")!!.toInt()) quizWords.size else defaultSharedPreferences.getString("length", "10")!!.toInt()
        if (strategy == QuizStrategy.PROGRESSIVE) {
            quizWords = getNextProgressiveWords()
            quizView.displayWords(quizWords)
            currentItem = -1
        } else {
            quizView.displayWords(quizWords)
            currentItem = currentItemBackup
        }
        setUpNextQuiz()
    }

    override suspend fun onContinueAfterNonProgressiveSessionEnd() {
        sessionCount = if (quizWords.size < defaultSharedPreferences.getString("length", "10")!!.toInt()) quizWords.size else defaultSharedPreferences.getString("length", "10")!!.toInt()
        setUpNextQuiz()
    }

    override suspend fun onRestartQuiz() {
        errorMode = false
        currentItem = -1
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
        val words = wordRepository.getWordsByRepetition(quizIds, 0, sessionLength!!)
        // if session length not reached yet, get completely new words (rep = -1)
        if (words.size < sessionLength!!) {
            words.addAll(wordRepository.getWordsByRepetition(quizIds, -1, sessionLength!! - words.size))
        }
        // fill up to session length with other words (rep >= 1)
        words.addAll(wordRepository.getWordsByMinRepetition(quizIds, 1, sessionLength!! - words.size))
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
        val word = if (errorMode) errors[currentItem].first else quizWords[currentItem].first
        return if (word.isKana >= 1)
            word.japanese.split("/")[0].split(";")[0]
        else word.reading.split("/")[0].split(";")[0]
    }

    override suspend fun setIsFuriDisplayed(isFuriDisplayed: Boolean) {
        this.isFuriDisplayed = isFuriDisplayed
        if (quizWords[currentItem].second == QuizType.TYPE_EN_JAP) {
            setupQCMEnJapQuiz()
        }
    }

    override fun onReportClick(position: Int) {
        quizView.reportError(quizWords[position].first, currentSentence)
    }

    override fun previousAnswerWrong(): Boolean {
        return previousAnswerWrong
    }

}
