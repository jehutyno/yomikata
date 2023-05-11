package com.jehutyno.yomikata.screens.content

import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentContentGraphBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.util.DimensionHelper
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.SeekBarsManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.titleResource


/**
 * Created by valentin on 30/09/2016.
 */
class ContentFragment(private val di: DI) : Fragment(), ContentContract.View, WordsAdapter.Callback {

    private var mpresenter: ContentContract.Presenter? = null
    private lateinit var adapter: WordsAdapter
    private lateinit var quizIds: LongArray
    private var quizTitle: String = ""
    private var level: Level? = null
    private var lastPosition = -1
    private var dialog: WordDetailDialogFragment? = null

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
            level = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireArguments().getSerializable(Extras.EXTRA_LEVEL, Level::class.java)
            } else {
                @Suppress("DEPRECATION")
                requireArguments().getSerializable(Extras.EXTRA_LEVEL) as Level?
            }
        }

        if (savedInstanceState != null) {
            lastPosition = savedInstanceState.getInt("position")
        }

        adapter = WordsAdapter(requireActivity(), this)
        setHasOptionsMenu(true)
    }


    private val wordsObserver = Observer<List<Word>> {
        words -> displayWords(words)
    }

    override fun onStart() {
        super.onStart()
        mpresenter?.start()
        mpresenter?.words?.observe(viewLifecycleOwner, wordsObserver)

        displayStats()
    }

    override fun onResume() {
        super.onResume()
        val position =
            if (lastPosition != -1) lastPosition
            else (binding.recyclerviewContent.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
        lastPosition = -1
        mpresenter?.start()
        binding.recyclerviewContent.scrollToPosition(position)

        seekBars.animateAll()
    }

    override fun onPause() {
        super.onPause()

        // cancel animation in case it is currently running
        // set all to zero to prepare for the next animation when the page resumes again
        seekBars.resetAll()
    }

    override fun displayStats() {
        if (level != null) {   // no need to update visibilities using LiveData, since it is only set once per fragment
            binding.seekLowContainer.visibility = if (level == Level.LOW) VISIBLE else GONE
            binding.seekMediumContainer.visibility = if (level == Level.MEDIUM) VISIBLE else GONE
            binding.seekHighContainer.visibility = if (level == Level.HIGH) VISIBLE else GONE
            binding.seekMasterContainer.visibility = if (level == Level.MASTER || level == Level.MAX)
                                                                                VISIBLE else GONE
        }
        seekBars.setTextViews(binding.textLow, binding.textMedium, binding.textHigh, binding.textMaster)
        mpresenter!!.let {
            seekBars.setObservers(it.quizCount,
                it.lowCount, it.mediumCount, it.highCount, it.masterCount, viewLifecycleOwner)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mpresenter == null) {
            mpresenter = ContentPresenter (
                di.direct.instance(),
                this@ContentFragment,
                di.direct.instance(arg = lifecycleScope), di.direct.instance(arg = quizIds), di.direct.instance(),
                quizIds, level
            )
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
        bundle.putSerializable(Extras.EXTRA_LEVEL, level)
        bundle.putInt(Extras.EXTRA_WORD_POSITION, position)
        bundle.putString(Extras.EXTRA_SEARCH_STRING, "")

        // unbind observer to prevent word from disappearing while viewing in detail dialog
        mpresenter?.words?.removeObserver(wordsObserver)

        dialog = WordDetailDialogFragment(di)
        dialog!!.arguments = bundle
        dialog!!.show(childFragmentManager, "")
        dialog!!.isCancelable = true

        dialog!!.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                // continue observing
                mpresenter?.words?.observe(viewLifecycleOwner, wordsObserver)
            }
        })
    }

    override fun onCategoryIconClick(position: Int) {
        requireActivity().startActionMode(actionModeCallback)
    }

    override fun onCheckChange(position: Int, check: Boolean) = runBlocking {
        mpresenter!!.updateWordCheck(adapter.items[position].id, check)
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
            val selections: List<Quiz>
            runBlocking {
                selections = mpresenter!!.getSelections()
            }
            when (item.itemId) {
                ADD_TO_SELECTIONS -> {
                    val popup = PopupMenu(activity!!, activity!!.findViewById(item.itemId))
                    popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
                    for ((i, selection) in selections.withIndex()) {
                        popup.menu.add(1, i, i, selection.getName()).isChecked = false
                    }
                    popup.setOnMenuItemClickListener {it -> runBlocking {
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
                    }}
                    popup.show()
                }
                REMOVE_FROM_SELECTIONS -> {
                    val popup = PopupMenu(activity!!, activity!!.findViewById(item.itemId))
                    for ((i, selection) in selections.withIndex()) {
                        popup.menu.add(1, i, i, selection.getName()).isChecked = false
                    }
                    popup.setOnMenuItemClickListener {it -> runBlocking {
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
                    }}
                    popup.show()

                }
                SELECT_ALL -> {
                    runBlocking {
                        mpresenter!!.updateWordsCheck(adapter.items.map{it.id}.toLongArray(), true)
                    }
                    adapter.items.forEach {
                        it.isSelected = 1
                    }
                    adapter.notifyItemRangeChanged(0, adapter.items.size)
                }
                UNSELECT_ALL -> {
                    runBlocking {
                        mpresenter!!.updateWordsCheck(adapter.items.map{it.id}.toLongArray(), false)
                    }
                    adapter.items.forEach {
                        it.isSelected = 0
                    }
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
                    MainScope().launch {// don't use lifecycle since creation might
                        // take a while, and we don't want the quiz selection to stop even if the activity stops
                        // use time out to prevent unexpected problems
                        withTimeout(2000L) {
                            val selectionId = mpresenter!!.createSelection(input.text.toString())
                            selectedWords.forEach {
                                mpresenter!!.addWordToSelection(it.id, selectionId)
                            }
                        }
                    }
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
