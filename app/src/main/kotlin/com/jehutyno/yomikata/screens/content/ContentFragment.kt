package com.jehutyno.yomikata.screens.content

import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.PopupMenu
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.FrameLayout
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentContentGraphBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.util.DimensionHelper
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.SeekBarsManager
import org.kodein.di.*
import splitties.alertdialog.appcompat.*
import java.util.*


/**
 * Created by valentin on 30/09/2016.
 */
class ContentFragment(private val di: DI) : Fragment(), ContentContract.View, WordsAdapter.Callback, DialogInterface.OnDismissListener {

    private var mpresenter: ContentContract.Presenter? = null
    private lateinit var adapter: WordsAdapter
    private lateinit var quizIds: LongArray
    private var quizTitle: String = ""
    private var level = -1
    private var lastPosition = -1
    private lateinit var selections: List<Quiz>

    // seekBars
    private lateinit var seekBars : SeekBarsManager

    // View Binding
    private var _binding: FragmentContentGraphBinding? = null
    private val binding get() = _binding!!


    override fun setPresenter(presenter: ContentContract.Presenter) {
        mpresenter = presenter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("position", (binding.recyclerviewContent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            quizIds = requireArguments().getLongArray(Extras.EXTRA_QUIZ_IDS)!!
            quizTitle = requireArguments().getString(Extras.EXTRA_QUIZ_TITLE)!!
            level = requireArguments().getInt(Extras.EXTRA_LEVEL, -1)
        }

        if (savedInstanceState != null) {
            lastPosition = savedInstanceState.getInt("position")
        }

        adapter = WordsAdapter(requireActivity(), this)
        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        mpresenter?.start()
        mpresenter?.loadWords(quizIds, level)
        mpresenter?.loadSelections()

        displayStats()
    }

    override fun onResume() {
        super.onResume()
        val position =
            if (lastPosition != -1) lastPosition
            else (binding.recyclerviewContent.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
        lastPosition = -1
        mpresenter?.start()
        mpresenter?.loadWords(quizIds, level)
        binding.recyclerviewContent.scrollToPosition(position)
        mpresenter?.loadSelections()

        displayStats()
        seekBars.animateAll()
    }

    override fun onPause() {
        super.onPause()

        // cancel animation in case it is currently running
        seekBars.cancelAll()

        // set all to zero to prepare for the next animation when the page resumes again
        binding.seekLow.progress = 0
        binding.seekMedium.progress = 0
        binding.seekHigh.progress = 0
        binding.seekMaster.progress = 0
    }

    override fun displayStats() {
        seekBars.count = mpresenter!!.countQuiz(quizIds)
        seekBars.low = mpresenter!!.countLow(quizIds)
        seekBars.medium = mpresenter!!.countMedium(quizIds)
        seekBars.high = mpresenter!!.countHigh(quizIds)
        seekBars.master = mpresenter!!.countMaster(quizIds)
        if (level > -1) {
            binding.seekLowContainer.visibility = if (level == 0) VISIBLE else GONE
            binding.seekMediumContainer.visibility = if (level == 1) VISIBLE else GONE
            binding.seekHighContainer.visibility = if (level == 2) VISIBLE else GONE
            binding.seekMasterContainer.visibility = if (level == 3) VISIBLE else GONE
        }
        binding.textLow.text = seekBars.low.toString()
        binding.textMedium.text = seekBars.medium.toString()
        binding.textHigh.text = seekBars.high.toString()
        binding.textMaster.text = seekBars.master.toString()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mpresenter == null) {
            mpresenter = ContentPresenter(di.direct.instance(), di.direct.instance(), this@ContentFragment)
        }

        binding.recyclerviewContent.let {
            it.adapter = adapter
            it.layoutManager = GridLayoutManager(context, 2)
        }

        // initialize seekBarsManager
        seekBars = SeekBarsManager(binding.seekLow, binding.seekMedium, binding.seekHigh, binding.seekMaster)
    }

    override fun displayWords(words: List<Word>) {
        adapter.replaceData(words)
    }


    override fun onItemClick(position: Int) {
        val bundle = Bundle()
        bundle.putLongArray(Extras.EXTRA_QUIZ_IDS, quizIds)
        bundle.putString(Extras.EXTRA_QUIZ_TITLE, quizTitle)
        bundle.putInt(Extras.EXTRA_LEVEL, level)
        bundle.putInt(Extras.EXTRA_WORD_POSITION, position)
        bundle.putString(Extras.EXTRA_SEARCH_STRING, "")

        val dialog = WordDetailDialogFragment(di)
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "")
        dialog.isCancelable = true
    }

    override fun onCategoryIconClick(position: Int) {
        requireActivity().startActionMode(actionModeCallback)
    }

    override fun onCheckChange(position: Int, check: Boolean) {
        mpresenter!!.updateWordCheck(adapter.items[position].id, check)
    }

    override fun onDismiss(dialog: DialogInterface?) {
        mpresenter!!.loadWords(quizIds, level)
        displayStats()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (activity != null) {
            inflater.inflate(R.menu.menu_content, menu)
            super.onCreateOptionsMenu(menu, inflater)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select_mode ->
                requireActivity().startActionMode(actionModeCallback)
        }
        return super.onOptionsItemSelected(item)
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            adapter.checkMode = true
            adapter.notifyItemRangeChanged(0, adapter.items.size)
            return false
        }

        private val ADD_TO_SELECTIONS = 1
        private val REMOVE_FROM_SELECTIONS = 2
        private val SELECT_ALL = 3
        private val UNSELECT_ALL = 4

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                ADD_TO_SELECTIONS -> {
                    val popup = PopupMenu(activity!!, activity!!.findViewById(item.itemId))
                    popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
                    for ((i, selection) in selections.withIndex()) {
                        popup.menu.add(1, i, i, selection.getName()).isChecked = false
                    }
                    popup.setOnMenuItemClickListener { it ->
                        val selectedWords: ArrayList<Word> = arrayListOf()
                        adapter.items.forEach { item -> if (item.isSelected == 1) selectedWords.add(item) }
                        val selectionItemId = it.itemId
                        when (it.itemId) {
                            R.id.add_selection -> addSelection(selectedWords)
                            else -> {
                                selectedWords.forEach {
                                    if (!mpresenter!!.isWordInQuiz(it.id, selections[selectionItemId].id))
                                        mpresenter!!.addWordToSelection(it.id, selections[selectionItemId].id)
                                }
                                it.isChecked = !it.isChecked
                            }
                        }
                        true
                    }
                    popup.show()
                }
                REMOVE_FROM_SELECTIONS -> {
                    val popup = PopupMenu(activity!!, activity!!.findViewById(item.itemId))
                    for ((i, selection) in selections.withIndex()) {
                        popup.menu.add(1, i, i, selection.getName()).isChecked = false
                    }
                    popup.setOnMenuItemClickListener {it ->
                        val selectedWords: ArrayList<Word> = arrayListOf()
                        adapter.items.forEach { item -> if (item.isSelected == 1) selectedWords.add(item) }
                        val selectionItemId = it.itemId
                        when (it.itemId) {
                            else -> {
                                selectedWords.forEach {
                                    if (mpresenter!!.isWordInQuiz(it.id, selections[selectionItemId].id))
                                        mpresenter!!.deleteWordFromSelection(it.id, selections[selectionItemId].id)
                                }
                                it.isChecked = !it.isChecked
                            }
                        }
                        true
                    }
                    popup.show()

                }
                SELECT_ALL -> {
                    adapter.items.forEach { it.isSelected = 1 }
                    adapter.notifyItemRangeChanged(0, adapter.items.size)
                }
                UNSELECT_ALL -> {
                    adapter.items.forEach { it.isSelected = 0 }
                    adapter.notifyItemRangeChanged(0, adapter.items.size)
                }
            }
            return false
        }

        private fun addSelection(selectedWords: ArrayList<Word>) {
            val input = EditText(activity)
            input.setSingleLine()
            input.hint = getString(R.string.selection_name)

            val container = FrameLayout(requireActivity())
            val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
            params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
            input.layoutParams = params
            container.addView(input)

            requireContext().alertDialog {
                titleResource = R.string.new_selection
                setView(container)

                okButton {
                    val selectionId = mpresenter!!.createSelection(input.text.toString())
                    selectedWords.forEach {
                        mpresenter!!.addWordToSelection(it.id, selectionId)
                    }
                    mpresenter!!.loadSelections()
                }
                cancelButton { }
            }.show()
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.title = null
            menu.add(0, ADD_TO_SELECTIONS, 0, getString(R.string.add_to_selections)).setIcon(R.drawable.ic_selections_selected)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, REMOVE_FROM_SELECTIONS, 0, getString(R.string.remove_from_selection)).setIcon(R.drawable.ic_unselect)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, SELECT_ALL, 0, getString(R.string.select_all)).setIcon(R.drawable.ic_select_multiple)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, UNSELECT_ALL, 0, getString(R.string.unselect_all)).setIcon(R.drawable.ic_unselect_multiple)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            adapter.checkMode = false
            adapter.notifyItemRangeChanged(0, adapter.items.size)
        }
    }

    override fun selectionLoaded(quizzes: List<Quiz>) {
        selections = quizzes
    }

    override fun noSelections() {
        selections = emptyList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}