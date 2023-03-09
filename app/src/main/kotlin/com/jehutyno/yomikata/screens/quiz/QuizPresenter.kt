package com.jehutyno.yomikata.screens.quiz

import android.content.Context
import android.content.SharedPreferences
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
import java.util.*

/**
 * Created by valentin on 18/10/2016.
 */
class QuizPresenter(
    val context: Context,
    private val quizRepository: QuizRepository, private val wordRepository: WordRepository, private val sentenceRepository: SentenceRepository,
    private val statsRepository: StatsRepository, private val quizView: QuizContract.View,
    private var quizIds: LongArray, private var strategy: QuizStrategy, private val quizTypes: IntArray) : QuizContract.Presenter {

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
    private var hasMistaken = false
    private var errorMode = false
    private var quizEnded = false

    init {
        isFuriDisplayed = defaultSharedPreferences.getBoolean(Prefs.FURI_DISPLAYED.pref, true)
        quizView.setPresenter(this)
    }

    override fun start() {
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList("errors", answers)
        outState.putBoolean("hasMistaken", hasMistaken)
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
        hasMistaken = savedInstanceState.getBoolean("hasMistaken")
        answers = savedInstanceState.getParcelableArrayList("errors")!!
        val random0 : Word? = savedInstanceState.getParcelable("random0")
        val random1 : Word? = savedInstanceState.getParcelable("random1")
        val random2 : Word? = savedInstanceState.getParcelable("random2")
        val random3 : Word? = savedInstanceState.getParcelable("random3")
        if (random0 != null) randoms.add(Pair(random0, savedInstanceState.getInt("random0_color")))
        if (random1 != null) randoms.add(Pair(random1, savedInstanceState.getInt("random1_color")))
        if (random2 != null) randoms.add(Pair(random2, savedInstanceState.getInt("random2_color")))
        if (random3 != null) randoms.add(Pair(random3, savedInstanceState.getInt("random3_color")))
        val words = LocalPersistence.readObjectFromFile(context, "words") as ArrayList<Word>
        val types = LocalPersistence.readObjectFromFile(context, "types") as ArrayList<QuizType>

        quizWords = (0..words.size - 1).map { Pair(words[it], types[it]) }
        quizView.displayWords(quizWords)
        currentItem = savedInstanceState.getInt("position") - 1 // -1 because setUpQuiz will do the +1
        quizView.setPagerPosition(currentItem)
        if (hasMistaken)
            quizView.displayEditDisplayAnswerButton()

        setUpNextQuiz()
        sessionCount = savedInstanceState.getInt("session_count")
    }

    override fun initQuiz() {
        sessionCount = defaultSharedPreferences.getString("length", "10")!!.toInt()
        when (strategy) {
            QuizStrategy.STRAIGHT, QuizStrategy.SHUFFLE, QuizStrategy.LOW_STRAIGHT, QuizStrategy.MEDIUM_STRAIGHT,
            QuizStrategy.HIGH_STRAIGHT, QuizStrategy.MASTER_STRAIGHT, QuizStrategy.LOW_SHUFFLE,
            QuizStrategy.MEDIUM_SHUFFLE, QuizStrategy.HIGH_SHUFFLE, QuizStrategy.MASTER_SHUFFLE -> {
                loadWords(quizIds)
            }
            QuizStrategy.PROGRESSIVE -> {
                quizWords = getNextWords()
                quizView.displayWords(quizWords)
                setUpNextQuiz()
            }
        }
    }

    override fun loadWords(quizIds: LongArray) {
        val level = getQuizLevelIfAny()
        if (level != -1) {
            // Quiz by Level
            wordRepository.getWordsByLevel(quizIds, level, object : WordRepository.LoadWordsCallback {
                override fun onWordsLoaded(words: List<Word>) {
                    quizWords = createWordTypePair(
                        if (strategy == QuizStrategy.LOW_SHUFFLE
                            || strategy == QuizStrategy.MEDIUM_SHUFFLE
                            || strategy == QuizStrategy.HIGH_SHUFFLE
                            || strategy == QuizStrategy.MASTER_SHUFFLE)
                            shuffle(words.toMutableList()) else words)

                    quizView.displayWords(quizWords)
                    setUpNextQuiz()
                }

                override fun onDataNotAvailable() {
                    quizView.noWords()
                }
            })
        } else {
            // Quiz From Home Page
            wordRepository.getWords(quizIds, object : WordRepository.LoadWordsCallback {
                override fun onWordsLoaded(words: List<Word>) {
                    quizWords = createWordTypePair(if (strategy == QuizStrategy.SHUFFLE) shuffle(words.toMutableList()) else words)
                    quizView.displayWords(quizWords)

                    setUpNextQuiz()
                }

                override fun onDataNotAvailable() {
                    quizView.noWords()
                }
            })
        }
    }

    override fun setUpNextQuiz() {
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
                quizView.displayQCMMode()
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
                quizView.displayQCMMode()
                // TTS at start
                quizView.speakWord(if (errorMode) errors[currentItem].first else quizWords[currentItem].first)
                // QCM options
                randoms = generateQCMRandoms(word, quizType, word.japanese)
                setupQCMQAudioQuiz()
            }
            QuizType.TYPE_EN_JAP -> {
                // Keyboard
                quizView.hideKeyboard()
                quizView.displayQCMMode()
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
                quizView.displayQCMMode()
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

    fun setupQCMPronunciationQuiz() {
        quizView.displayQCMNormalTextViews()
        quizView.displayQCMTv1(randoms[0].first.reading.split("/")[0].split(";")[0].trim(), randoms[0].second)
        quizView.displayQCMTv2(randoms[1].first.reading.split("/")[0].split(";")[0].trim(), randoms[1].second)
        quizView.displayQCMTv3(randoms[2].first.reading.split("/")[0].split(";")[0].trim(), randoms[2].second)
        quizView.displayQCMTv4(randoms[3].first.reading.split("/")[0].split(";")[0].trim(), randoms[3].second)
    }

    fun setupQCMQAudioQuiz() {
        quizView.displayQCMNormalTextViews()
        quizView.displayQCMTv1(randoms[0].first.japanese.split("/")[0].split(";")[0].trim(), randoms[0].second)
        quizView.displayQCMTv2(randoms[1].first.japanese.split("/")[0].split(";")[0].trim(), randoms[1].second)
        quizView.displayQCMTv3(randoms[2].first.japanese.split("/")[0].split(";")[0].trim(), randoms[2].second)
        quizView.displayQCMTv4(randoms[3].first.japanese.split("/")[0].split(";")[0].trim(), randoms[3].second)
    }

    fun setupQCMEnJapQuiz() {
        quizView.displayQCMFuriTextViews()
        val word1 = getQCMDisPlayForEnJap(randoms[0].first)
        quizView.displayQCMFuri1(word1, 0, word1.length, ContextCompat.getColor(context, randoms[0].second))
        val word2 = getQCMDisPlayForEnJap(randoms[1].first)
        quizView.displayQCMFuri2(word2, 0, word2.length, ContextCompat.getColor(context, randoms[1].second))
        val word3 = getQCMDisPlayForEnJap(randoms[2].first)
        quizView.displayQCMFuri3(word3, 0, word3.length, ContextCompat.getColor(context, randoms[2].second))
        val word4 = getQCMDisPlayForEnJap(randoms[3].first)
        quizView.displayQCMFuri4(word4, 0, word4.length, ContextCompat.getColor(context, randoms[3].second))
    }

    fun setupQCMJapEnQuiz() {
        quizView.displayQCMNormalTextViews()
        quizView.displayQCMTv1(randoms[0].first.getTrad().trim(), randoms[0].second)
        quizView.displayQCMTv2(randoms[1].first.getTrad().trim(), randoms[1].second)
        quizView.displayQCMTv3(randoms[2].first.getTrad().trim(), randoms[2].second)
        quizView.displayQCMTv4(randoms[3].first.getTrad().trim(), randoms[3].second)
    }

    fun getQCMDisPlayForEnJap(word: Word): String {
        return if (word.isKana == 2) {
            if (isFuriDisplayed)
                sentenceRepository.getSentenceById(word.sentenceId).jap
            else
                sentenceNoFuri(sentenceRepository.getSentenceById(word.sentenceId))
        }
        else if (isFuriDisplayed)
            " {${word.japanese};${word.reading}} "
        else
            word.japanese.trim()
    }

    fun getQCMLengthForAudio(word: Word): Int {
        return word.japanese.trim().length + 1
    }

    fun generateQCMRandoms(word: Word, quizType: QuizType, answerToAvoid: String): ArrayList<Pair<Word, Int>> {
        // Generate 3 different random words
        val random = getRandomWords(word.id, answerToAvoid, word.japanese.length, 3, quizType)
        val randoms = arrayListOf<Pair<Word, Int>>()
        random.forEach { randoms.add(Pair(it, android.R.color.white)) }
        // Add the good answer at a random place
        randoms.add(Random().nextInt(4), Pair(word, android.R.color.white))

        return randoms
    }

    fun getQuizLevelIfAny(): Int {
        return if (strategy == QuizStrategy.LOW_STRAIGHT) 0
        else if (strategy == QuizStrategy.MEDIUM_STRAIGHT) 1
        else if (strategy == QuizStrategy.HIGH_STRAIGHT) 2
        else if (strategy == QuizStrategy.MASTER_STRAIGHT) 3
        else if (strategy == QuizStrategy.LOW_SHUFFLE) 0
        else if (strategy == QuizStrategy.MEDIUM_SHUFFLE) 1
        else if (strategy == QuizStrategy.HIGH_SHUFFLE) 2
        else if (strategy == QuizStrategy.MASTER_SHUFFLE) 3
        else -1
    }

    fun createWordTypePair(words: List<Word>): List<Pair<Word, QuizType>> {
        if (words.size < sessionLength!! || words.size < sessionCount) {
            sessionLength = words.size // To be sure the session length is not bigger than the number of words
            sessionCount = words.size
        }
        val quizWordsPair = arrayListOf<Pair<Word, QuizType>>()
        words.forEach {
            quizWordsPair.add(Pair(it, getQuizType(it)))
        }
        return quizWordsPair
    }

    fun getQuizType(word: Word): QuizType {
        val returnTypes: IntArray
        if (quizTypes.contains(QuizType.TYPE_AUTO.type)) {
            val autoTypes = arrayListOf<Int>()
            if (defaultSharedPreferences.getBoolean(Prefs.FULL_VERSION.pref, false)) {
                when (word.level) {
                    0 -> {
                        autoTypes.add(QuizType.TYPE_PRONUNCIATION_QCM.type)
                        autoTypes.add(QuizType.TYPE_JAP_EN.type)
                    }
                    1 -> {
                        autoTypes.add(QuizType.TYPE_PRONUNCIATION_QCM.type)
                        autoTypes.add(QuizType.TYPE_JAP_EN.type)
                        autoTypes.add(QuizType.TYPE_EN_JAP.type)
                        if (ttsSupported != TextToSpeech.LANG_MISSING_DATA && ttsSupported != TextToSpeech.LANG_NOT_SUPPORTED)
                            autoTypes.add(QuizType.TYPE_AUDIO.type)
                    }
                    else -> {
                        autoTypes.add(QuizType.TYPE_PRONUNCIATION_QCM.type)
                        autoTypes.add(QuizType.TYPE_JAP_EN.type)
                        autoTypes.add(QuizType.TYPE_EN_JAP.type)
                        autoTypes.add(QuizType.TYPE_PRONUNCIATION.type)
                        if (ttsSupported != TextToSpeech.LANG_MISSING_DATA && ttsSupported != TextToSpeech.LANG_NOT_SUPPORTED)
                            autoTypes.add(QuizType.TYPE_AUDIO.type)
                    }
                }
            } else {
                autoTypes.add(QuizType.TYPE_PRONUNCIATION_QCM.type)
                autoTypes.add(QuizType.TYPE_PRONUNCIATION.type)
            }
            returnTypes = autoTypes.toIntArray()
        } else {
            returnTypes = quizTypes
        }
        return QuizType.values()[returnTypes[Random().nextInt(returnTypes.size)]]
    }

    override fun onOption1Click() {
        onAnswerGiven(0)
    }

    override fun onOption2Click() {
        onAnswerGiven(1)
    }

    override fun onOption3Click() {
        onAnswerGiven(2)
    }

    override fun onOption4Click() {
        onAnswerGiven(3)
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

    fun onAnswerGiven(choice: Int) {
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

    override fun onAnswerGiven(answer: String) {
        onAnswerGiven(answer, -1)
    }

    fun onAnswerGiven(answer: String, choice: Int) {
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
        if (hasMistaken)
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

    fun updateRepetitionAndPoints(word: Word, quizType: QuizType, result: Boolean) {
        val speed = defaultSharedPreferences.getString("speed", "2")?.toInt()
        val fromLevel = word.level
        var toLevel = word.level
        val fromPoints = word.points
        var toPoints = word.points
        if (!hasMistaken) {
            if (result && word.level < 3) {
                if (word.points + quizType.points * speed!! >= 200) {
                    updateRepetitions(word.id, word.level + 2, word.points, result)
                    updateWordLevel(word.id, word.level + 2)
                    toLevel = word.level + 2
                    toPoints = 0
                } else if (word.points + quizType.points * speed >= 100) {
                    updateRepetitions(word.id, word.level + 1, word.points, result)
                    updateWordLevel(word.id, word.level + 1)
                    toLevel = word.level + 1
                    toPoints = 0
                } else {
                    updateRepetitions(word.id, quizWords[currentItem].first.level, word.points + quizType.points * speed, result)
                    updateWordPoints(word.id, word.points + quizType.points * speed)
                    toLevel = word.level + 2
                    toPoints = word.points + quizType.points * speed
                }
            } else if (!result && word.level > 0) {
                if (word.level == 3 || word.points - quizType.points * speed!! < 0) {
                    updateRepetitions(word.id, word.level - 1, word.points, result)
                    updateWordLevel(word.id, word.level - 1)
                    toLevel = word.level - 1
                    toPoints = 0
                } else {
                    updateRepetitions(word.id, word.level, 0, result)
                    updateWordPoints(word.id, 0)
                    toLevel = word.level
                    toPoints = 0
                }
            } else if (result && word.level >= 3) {
                val points = word.points + quizType.points * speed!!
                updateRepetitions(word.id, word.level, points, result)
                updateWordPoints(word.id, points)
                toLevel = word.level + 1
                toPoints = points
            } else {
                updateRepetitions(word.id, word.level, word.points, result)
            }
            word.level = toLevel
            word.points = toPoints
            quizView.animateColor(currentItem, word, currentSentence, quizType, fromLevel, toLevel, fromPoints, toPoints)
        }

        hasMistaken = !result
    }

    fun addCurrentWordToAnswers(answer: String) {
        val word = quizWords[currentItem].first
        val color = if (!hasMistaken) "#77d228" else "#d22828'"
        if (answers.size > 0 && answers[0].wordId == word.id) {
            answers[0].answer += "<br><font color='$color'>$answer</font>"
        } else {
            answers.add(0, Answer(
                if (hasMistaken) 0 else 1,
                "<font color='$color'>$answer</font>",
                word.id,
                currentSentence.id,
                quizWords[currentItem].second)
            )
        }
    }

    override fun onNextWord() {
        sessionCount--
        hasMistaken = false
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
        } else
            setUpNextQuiz()
    }

    override fun onLaunchErrorSession() {
        currentItemBackup = currentItem
        sessionCount = errors.size
        currentItem = -1
        errorMode = true
        quizView.displayWords(shuffle(errors))
        setUpNextQuiz()
    }

    override fun onLaunchNextProgressiveSession() {
        sessionCount = if (quizWords.size < defaultSharedPreferences.getString("length", "10")!!.toInt()) quizWords.size else defaultSharedPreferences.getString("length", "10")!!.toInt()
        currentItem = -1
        initQuiz()
    }


    override fun onContinueQuizAfterErrorSession() {
        errorMode = false
        sessionCount = if (quizWords.size < defaultSharedPreferences.getString("length", "10")!!.toInt()) quizWords.size else defaultSharedPreferences.getString("length", "10")!!.toInt()
        if (strategy == QuizStrategy.PROGRESSIVE) {
            quizWords = getNextWords()
            quizView.displayWords(quizWords)
            currentItem = -1
        } else {
            quizView.displayWords(quizWords)
            currentItem = currentItemBackup
        }
        setUpNextQuiz()
    }

    override fun onContinueAfterNonProgressiveSessionEnd() {
        sessionCount = if (quizWords.size < defaultSharedPreferences.getString("length", "10")!!.toInt()) quizWords.size else defaultSharedPreferences.getString("length", "10")!!.toInt()
        setUpNextQuiz()
    }

    override fun onRestartQuiz() {
        errorMode = false
        currentItem = -1
        answers.clear()
        initQuiz()
    }

    override fun onFinishQuiz() {
        quizView.hideKeyboard()
        quizView.finishQuiz()
    }


    override fun loadSelections() {
        quizRepository.getQuiz(Categories.CATEGORY_SELECTIONS, object : QuizRepository.LoadQuizCallback {
            override fun onQuizLoaded(quizzes: List<Quiz>) {
                quizView.selectionLoaded(quizzes)
            }

            override fun onDataNotAvailable() {
                quizView.noSelections()
            }

        })
    }

    override fun createSelection(quizName: String): Long {
        return quizRepository.saveQuiz(quizName, Categories.CATEGORY_SELECTIONS)
    }

    override fun addWordToSelection(wordId: Long, quizId: Long) {
        quizRepository.addWordToQuiz(wordId, quizId)
    }

    override fun isWordInQuiz(wordId: Long, quizId: Long): Boolean {
        return wordRepository.isWordInQuiz(wordId, quizId)
    }

    override fun isWordInQuizzes(wordId: Long, quizIds: Array<Long>): ArrayList<Boolean> {
        return wordRepository.isWordInQuizzes(wordId, quizIds)
    }

    override fun deleteWordFromSelection(wordId: Long, selectionId: Long) {
        quizRepository.deleteWordFromQuiz(wordId, selectionId)
    }

    override fun updateWordPoints(wordId: Long, points: Int) {
        wordRepository.updateWordPoints(wordId, points)
    }

    override fun updateWordLevel(wordId: Long, level: Int) {
        wordRepository.updateWordLevel(wordId, level)
    }

    override fun getRandomWords(wordId: Long, answer: String, wordSize: Int, limit: Int, quizType: QuizType): ArrayList<Word> {
        return wordRepository.getRandomWords(wordId, answer, wordSize, limit, quizType)
    }

    override fun getNextWords(): List<Pair<Word, QuizType>> {
        val words = wordRepository.getWordsByRepetition(quizIds, 0, sessionLength!!)
        if (words.size < sessionLength!!) {
            words.addAll(wordRepository.getWordsByRepetition(quizIds, -1, sessionLength!! - words.size))
        }
        for (i in 1..101) {
            if (words.size < sessionLength!!) {
                words.addAll(wordRepository.getWordsByRepetition(quizIds, i, sessionLength!! - words.size))
            } else
                break
        }
        shuffle(words)

        return createWordTypePair(words)
    }

    override fun getWord(id: Long): Word {
        return wordRepository.getWordById(id)
    }

    override fun getRandomSentence(word: Word): Sentence {
        val sentence = sentenceRepository.getRandomSentence(word, getCategoryLevel(word.baseCategory))
        return if (word.isKana == 2 || sentence == null)
            sentenceRepository.getSentenceById(word.sentenceId)
        else
            sentence
    }

    override fun updateRepetitions(id: Long, level: Int, points: Int, result: Boolean) {
        val newRepetition =
            if (result) {
                when (level) {
                    0 -> if (points > 50) 6 else 4
                    1 -> if (points > 50) 12 else 8
                    2 -> if (points > 50) 16 else 14
                    else -> Math.min(20 + (points / 10), 100)
                }
            } else {
                when (level) {
                    0 -> if (points > 50) 3 else 2
                    1 -> if (points > 50) 6 else 4
                    2 -> if (points > 50) 9 else 7
                    else -> if (points > 50) 12 else 10
                }
            }
        wordRepository.updateWordRepetition(id, newRepetition)
    }

    override fun decreaseAllRepetitions() {
        wordRepository.decreaseWordsRepetition(quizIds)
    }

    override fun saveAnswerResultStat(word: Word, result: Boolean) {
        statsRepository.addStatEntry(StatAction.ANSWER_QUESTION, word.id,
            Calendar.getInstance().timeInMillis, if (result) StatResult.SUCCESS else StatResult.FAIL)
    }

    override fun saveWordSeenStat(word: Word) {
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

    override fun setIsFuriDisplayed(isFuriDisplayed: Boolean) {
        this.isFuriDisplayed = isFuriDisplayed
        if (quizWords[currentItem].second == QuizType.TYPE_EN_JAP) {
            setupQCMEnJapQuiz()
        }
    }

    override fun onReportClick(position: Int) {
        quizView.reportError(quizWords[position].first, currentSentence)
    }

    override fun hasMistaken(): Boolean {
        return hasMistaken
    }

}