package com.jehutyno.yomikata.screens.word

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.VhKanjiSoloBinding
import com.jehutyno.yomikata.databinding.VhWordDetailBinding
import com.jehutyno.yomikata.model.KanjiSoloRadical
import com.jehutyno.yomikata.model.Sentence
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.model.getWordColor
import com.jehutyno.yomikata.util.QuizType
import com.jehutyno.yomikata.util.getWordPositionInFuriSentence
import com.jehutyno.yomikata.util.sentenceNoAnswerFuri
import com.jehutyno.yomikata.util.sentenceNoFuri


class WordFragment (
    private val wordKanjiSentence: Triple<LiveData<Word>, LiveData<List<KanjiSoloRadical>?>, LiveData<Sentence?>>,
    private val quizType: QuizType?,
    private val callback: WordPagerAdapter.InternalCallback
) : Fragment() {

    // View Binding
    private var _binding: VhWordDetailBinding? = null
    private val binding get() = _binding!!


    private val kanjiSoloRadicalViews = mutableListOf<View>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        _binding = VhWordDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun onWordUpdate(word: Word) {
        val wordDisplay =
            if (quizType != null)
                word.japanese
            else
                "    {${word.japanese};${word.reading}}    "
        wordDisplay.let { binding.furiWord.text_set(wordDisplay, 0, it.length,
            getWordColor(requireContext(), word.points)
        ) }

        binding.textTraduction.text = word.getTrad()

        binding.textTraduction.visibility =
            if (quizType == QuizType.TYPE_JAP_EN || word.isKana == 2) View.INVISIBLE else View.VISIBLE

        binding.btnCopy.setOnClickListener(getCopyListener(word))
        binding.btnSelection.setOnClickListener { callback.onSelectionClick(binding.btnSelection, word) }
        binding.btnReport.setOnClickListener { callback.onReportClick(
            Triple(word, wordKanjiSentence.second.value!!, wordKanjiSentence.third.value!!)
        ) }
        binding.btnTts.setOnClickListener { callback.onWordTTSClick(word) }

        binding.levelDown.setOnClickListener { callback.onLevelDown() }
        binding.levelUp.setOnClickListener { callback.onLevelUp() }

        val sentence = wordKanjiSentence.third.value
        if (sentence != null)
            onWordOrSentenceUpdate(word, sentence)
    }

    private fun onKanjiSoloRadicalUpdate(kanjiSoloRadicals: List<KanjiSoloRadical>) {
        // completely reload by first removing current views
        kanjiSoloRadicalViews.forEach {
            binding.containerInfo.removeView(it)
        }
        kanjiSoloRadicalViews.clear()
        // set up views, and add them to list
        kanjiSoloRadicals.forEach {
            val radicalLayoutBinding = VhKanjiSoloBinding.inflate(layoutInflater)
            radicalLayoutBinding.separator.visibility = if (kanjiSoloRadicalViews.isNotEmpty()) View.VISIBLE else View.GONE
            radicalLayoutBinding.kanjiSolo.text = it.kanji
            radicalLayoutBinding.ksTrad.text = it.getTrad()
            radicalLayoutBinding.ksStrokes.text = it.strokes.toString()
            radicalLayoutBinding.kunyomiTitle.visibility = if (it.kunyomi.isEmpty() || quizType != null) View.GONE else View.VISIBLE
            radicalLayoutBinding.ksKunyomi.visibility = if (it.kunyomi.isEmpty() || quizType != null) View.GONE else View.VISIBLE
            radicalLayoutBinding.ksKunyomi.text = it.kunyomi
            radicalLayoutBinding.onyomiTitle.visibility = if (it.onyomi.isEmpty() || quizType != null) View.GONE else View.VISIBLE
            radicalLayoutBinding.ksOnyomi.visibility = if (it.onyomi.isEmpty() || quizType != null) View.GONE else View.VISIBLE
            radicalLayoutBinding.ksOnyomi.text = it.onyomi
            radicalLayoutBinding.radical.text = it.radical
            radicalLayoutBinding.radicalStroke.text = it.radStroke.toString()
            radicalLayoutBinding.radicalTrad.text = it.getRadTrad()
            kanjiSoloRadicalViews.add(radicalLayoutBinding.root)
        }
        // add the views to the info container
        kanjiSoloRadicalViews.forEach{
            binding.containerInfo.addView(it)
        }
        // update visibility of info container
        binding.containerInfo.visibility =
            if (kanjiSoloRadicalViews.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun getCopyListener(word: Word): (View?) -> Unit {
        return {
            val popup = PopupMenu(requireActivity(), binding.btnCopy)
            popup.menuInflater.inflate(R.menu.popup_copy, popup.menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.copy_word -> {
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(
                            requireContext().getString(R.string.copy_word),
                            word.japanese)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireActivity(), requireContext().getString(R.string.word_copied), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(
                            requireContext().getString(R.string.copy_sentence),
                            sentenceNoFuri(wordKanjiSentence.third.value!!)
                        )
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireActivity(), requireContext().getString(R.string.sentence_copied), Toast.LENGTH_SHORT).show()
                    }
                }
                true
            }
            popup.show()
        }
    }

    private fun onSentenceUpdate(sentence: Sentence) {
        binding.textSentenceEn.text = sentence.getTrad()
        binding.btnSentenceTts.setOnClickListener { callback.onSentenceTTSClick(sentence) }

        val word = wordKanjiSentence.first.value!!
        onWordOrSentenceUpdate(word, sentence)
    }

    private fun onWordOrSentenceUpdate(word: Word, sentence: Sentence) {
        val wordTruePosition = getWordPositionInFuriSentence(sentence.jap, word)
        wordTruePosition.let {
            binding.furiSentence.text_set (
                if (quizType == null)
                    sentence.jap
                else
                    sentenceNoAnswerFuri(sentence, word),
                it,
                it + word.japanese.length,
                getWordColor(requireContext(), word.points)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.levelDown.visibility = if (quizType != null) View.GONE else View.VISIBLE
        binding.levelUp.visibility = if (quizType != null) View.GONE else View.VISIBLE

        binding.close.setOnClickListener { callback.onCloseClick() }
    }

    override fun onStart() {
        super.onStart()

        wordKanjiSentence.first.observe(viewLifecycleOwner, ::onWordUpdate)
        wordKanjiSentence.second.observe(viewLifecycleOwner) {
            if (it == null)
                return@observe
            onKanjiSoloRadicalUpdate(it)
        }
        wordKanjiSentence.third.observe(viewLifecycleOwner) {
            if (it == null)
                return@observe
            onSentenceUpdate(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
