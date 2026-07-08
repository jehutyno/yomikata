package com.jehutyno.yomikata.screens.content

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jehutyno.yomikata.audio.VoicesManager
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.content.word.WordDetailFragment
import com.jehutyno.yomikata.ui.theme.YomikataTheme
import com.jehutyno.yomikata.ui.wordlist.WordListScreen
import com.jehutyno.yomikata.ui.wordlist.tabToLevel
import com.jehutyno.yomikata.ui.wordlist.WordListUiState
import com.jehutyno.yomikata.util.onTTSinit
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.quiz.Level
import com.jehutyno.yomikata.util.getSerializableHelper
import com.jehutyno.yomikata.util.showWordSelectionDialog
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider
import org.kodein.di.singleton


/**
 * Created by valentin on 30/09/2016.
 */
class ContentFragment(private val di: DI) : Fragment(), ContentContract.View, TextToSpeech.OnInitListener {

    private lateinit var quizIds: LongArray
    private var quizTitle: String = ""
    private var level: Level? = null

    // Kodein
    private val subDI by DI.lazy {
        extend(di)
        bind<VoicesManager>() with singleton { VoicesManager(requireActivity()) }
        bind<ContentContract.Presenter>() with provider {
            ContentPresenter(
                instance(),
                instance(arg = lifecycleScope), instance(arg = quizIds), instance(),
                quizIds, null   // null = load all words; tabs handle in-memory filtering
            )
        }
    }
    private val mpresenter: ContentContract.Presenter by subDI.instance()
    private val voices: VoicesManager by subDI.instance()

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

    // Compose state
    private var uiState by mutableStateOf(WordListUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            quizIds = requireNotNull(requireArguments().getLongArray(Extras.EXTRA_QUIZ_IDS)) { "EXTRA_QUIZ_IDS missing" }
            quizTitle = requireNotNull(requireArguments().getString(Extras.EXTRA_QUIZ_TITLE)) { "EXTRA_QUIZ_TITLE missing" }
            level = requireArguments().getSerializableHelper(Extras.EXTRA_LEVEL, Level::class.java)
        }

        val initialTab = when (level) {
            Level.LOW -> 1
            Level.MEDIUM -> 2
            Level.HIGH -> 3
            Level.MASTER -> 4
            null -> 0
        }
        uiState = uiState.copy(title = quizTitle, selectedTab = initialTab)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        tts = TextToSpeech(activity, this)
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                YomikataTheme {
                    WordListScreen(
                        state = uiState,
                        onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                        onTabSelected = {
                            uiState = uiState.copy(selectedTab = it)
                            publishFilterLevel()
                        },
                        onSearchQueryChanged = { uiState = uiState.copy(searchQuery = it) },
                        onToggleGrid = { uiState = uiState.copy(isGrid = !uiState.isGrid) },
                        onWordClick = { word -> navigateToWordDetail(word) },
                        onFavoriteClick = { word -> showSelectionPicker(word) },
                        onAudioClick = { word -> voices.speakWord(word, ttsSupported, tts) },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mpresenter.start()

        mpresenter.words.observe(viewLifecycleOwner) { words ->
            uiState = uiState.copy(words = words ?: emptyList())
        }
        mpresenter.wordsInSelections.observe(viewLifecycleOwner) { ids ->
            uiState = uiState.copy(selectedWordIds = (ids ?: emptyList()).toSet())
        }
        mpresenter.quizCount.observe(viewLifecycleOwner) { count ->
            uiState = uiState.copy(quizCount = count ?: 0)
        }
        mpresenter.masterCount.observe(viewLifecycleOwner) { count ->
            uiState = uiState.copy(masterCount = count ?: 0)
        }
        mpresenter.highCount.observe(viewLifecycleOwner) { count ->
            uiState = uiState.copy(highCount = count ?: 0)
        }
        mpresenter.mediumCount.observe(viewLifecycleOwner) { count ->
            uiState = uiState.copy(mediumCount = count ?: 0)
        }
        mpresenter.lowCount.observe(viewLifecycleOwner) { count ->
            uiState = uiState.copy(lowCount = count ?: 0)
        }
    }

    override fun onResume() {
        super.onResume()
        // Cette page vient de devenir visible (ViewPager) → son filtre courant pilote le niveau
        // sur lequel l'activité lancera le quiz.
        publishFilterLevel()
    }

    /** Publie le niveau du filtre courant à l'activité hôte, pour lancer le quiz sur ce sous-ensemble. */
    private fun publishFilterLevel() {
        (activity as? ContentActivity)?.currentFilterLevel = tabToLevel(uiState.selectedTab)
    }

    override fun onInit(status: Int) {
        ttsSupported = onTTSinit(activity, status, tts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tts?.stop()
        tts?.shutdown()
        voices.releasePlayer()
    }

    private fun showSelectionPicker(word: Word) {
        // Star color updates reactively via the wordsInSelections Flow → no onChanged needed.
        showWordSelectionDialog(word.id, mpresenter, mpresenter)
    }

    private fun navigateToWordDetail(word: Word) {
        val allWords = mpresenter.words.value ?: return
        val position = allWords.indexOfFirst { it.id == word.id }
        if (position < 0) return

        val bundle = Bundle().apply {
            putLongArray(Extras.EXTRA_QUIZ_IDS, quizIds)
            putString(Extras.EXTRA_QUIZ_TITLE, quizTitle)
            putSerializable(Extras.EXTRA_LEVEL, level)
            putInt(Extras.EXTRA_WORD_POSITION, position)
            putString(Extras.EXTRA_SEARCH_STRING, uiState.searchQuery)
        }
        val fragment = WordDetailFragment(di)
        fragment.arguments = bundle
        requireActivity().supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "word_detail")
            .addToBackStack("word_detail")
            .commit()
    }

    // ContentContract.View — data arrives via LiveData observation above
    override fun displayWords(words: List<Word>) = Unit
    override fun displayStats() = Unit

}
