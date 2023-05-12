package com.jehutyno.yomikata.screens.answers

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentContentBinding
import com.jehutyno.yomikata.managers.VoicesManager
import com.jehutyno.yomikata.model.Answer
import com.jehutyno.yomikata.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kodein.di.*
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.titleResource
import java.util.*


/**
 * Created by valentin on 25/10/2016.
 */
class AnswersFragment(private val di: DI) : Fragment(), AnswersContract.View, AnswersAdapter.Callback, TextToSpeech.OnInitListener {

    // kodein
    private val subDI by DI.lazy {
        extend(di)
        bind<AnswersContract.Presenter>() with provider {
            AnswersPresenter(instance(arg = lifecycleScope), instance(), instance())
        }
        bind<VoicesManager>() with singleton { VoicesManager(requireActivity()) }
    }
    @Suppress("unused")
    private val voicesManager: VoicesManager by subDI.instance()
    private val presenter: AnswersContract.Presenter by subDI.instance()

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: AnswersAdapter

    private var tts: TextToSpeech? = null
    private var ttsSupported: Int = TextToSpeech.LANG_NOT_SUPPORTED

    // View Binding
    private var _binding: FragmentContentBinding? = null
    private val binding get() = _binding!!


    override fun onInit(status: Int) {
        ttsSupported = onTTSinit(activity, status, tts)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(activity, this)

        val answersListRaw = LocalPersistence.readObjectFromFile(context, "answers")
        val answersList = answersListRaw as ArrayList<*>
        val answers = answersListRaw.filterIsInstance<Answer>()
        if (answers.size != answersList.size) {
            Log.e("Failed cast", "Some items in the read list of answers were not of the type Answer")
        }
        adapter = AnswersAdapter(requireActivity(), this)
        layoutManager = LinearLayoutManager(activity)
        runBlocking {
            adapter.replaceData(presenter.getAnswersWordsSentences(answers))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerviewContent.let {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.start()
    }

    override fun displayAnswers() {

    }

    override fun onSelectionClick(position: Int, view: View) = runBlocking {
        val selections = presenter.getSelections()
        val popup = PopupMenu(requireActivity(), view)
        popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
        for ((i, selection) in selections.withIndex()) {
            popup.menu.add(1, i, i, selection.getName()).isChecked = presenter.isWordInQuiz(adapter.items[position].second.id, selection.id)
            popup.menu.setGroupCheckable(1, true, false)
        }
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.add_selection -> addSelection(adapter.items[position].second.id)
                else -> {
                    runBlocking {
                        if (!it.isChecked)
                            presenter.addWordToSelection(adapter.items[position].second.id, selections[it.itemId].id)
                        else {
                            presenter.deleteWordFromSelection(adapter.items[position].second.id, selections[it.itemId].id)
                        }
                        it.isChecked = !it.isChecked
                    }
                }
            }
            true
        }
        popup.show()
    }

    private fun addSelection(wordId: Long) {
        val input = EditText(activity)
        input.setSingleLine()
        input.hint = getString(R.string.selection_name)

        val container = FrameLayout(requireActivity())
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
        params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
        input.layoutParams = params
        container.addView(input)

        requireActivity().alertDialog {
            titleResource = R.string.new_selection
            setView(container)

            okButton {
                lifecycleScope.launch {
                    val selectionId = presenter.createSelection(input.text.toString())
                    presenter.addWordToSelection(wordId, selectionId)
                }
            }
            cancelButton()
        }.show()
    }


    override fun onReportClick(position: Int) {
        reportError(requireActivity(), adapter.items[position].second, adapter.items[position].third)
    }

    override fun onTTSClick(position: Int) {
        val word = adapter.items[position].second
        voicesManager.speakWord(word, ttsSupported, tts)
    }

    override fun onSentenceTTSClick(position: Int) {
        val sentence = adapter.items[position].third
        voicesManager.speakSentence(sentence, ttsSupported, tts)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}