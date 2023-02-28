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
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentContentGraphBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.util.DimensionHelper
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Prefs
import com.jehutyno.yomikata.util.animateSeekBar
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.find
import org.jetbrains.anko.okButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.withArguments
import java.util.*


/**
 * Created by valentin on 30/09/2016.
 */
class ContentFragment : Fragment(), ContentContract.View, WordsAdapter.Callback, DialogInterface.OnDismissListener {
    private var mpresenter: ContentContract.Presenter? = null
    private lateinit var adapter: WordsAdapter
    private lateinit var quizIds: LongArray
    private var quizTitle: String = ""
    private var level = -1
    private var lastPosition = -1
    lateinit private var selections: List<Quiz>

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
            quizIds = arguments!!.getLongArray(Extras.EXTRA_QUIZ_IDS)!!
            quizTitle = arguments!!.getString(Extras.EXTRA_QUIZ_TITLE)!!
            level = arguments!!.getInt(Extras.EXTRA_LEVEL, -1)
        }

        if (savedInstanceState != null) {
            lastPosition = savedInstanceState.getInt("position")
        }

        adapter = WordsAdapter(activity!!, this)
        setHasOptionsMenu(true)
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
    }

    override fun displayStats() {
        val count = mpresenter!!.countQuiz(quizIds)
        val low = mpresenter!!.countLow(quizIds)
        val medium = mpresenter!!.countMedium(quizIds)
        val high = mpresenter!!.countHigh(quizIds)
        val master = mpresenter!!.countMaster(quizIds)
        if (level > -1) {
            binding.seekLowContainer.visibility = if (level == 0) VISIBLE else GONE
            binding.seekMediumContainer.visibility = if (level == 1) VISIBLE else GONE
            binding.seekHighContainer.visibility = if (level == 2) VISIBLE else GONE
            binding.seekMasterContainer.visibility = if (level == 3) VISIBLE else GONE
        }
        animateSeekBar(binding.seekLow, 0, low, count)
        binding.textLow.text = low.toString()
        animateSeekBar(binding.seekMedium, 0, medium, count)
        binding.textMedium.text = medium.toString()
        animateSeekBar(binding.seekHigh, 0, high, count)
        binding.textHigh.text = high.toString()
        animateSeekBar(binding.seekMaster, 0, master, count)
        binding.textMaster.text = master.toString()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mpresenter == null)
            mpresenter = ContentPresenter(context!!.appKodein.invoke().instance(), context!!.appKodein.invoke().instance(), this)

        binding.recyclerviewContent.let {
            it.adapter = adapter
            it.layoutManager = GridLayoutManager(context, 2)
        }
    }

    override fun displayWords(words: List<Word>) {
        adapter.replaceData(words)
    }


    override fun onItemClick(position: Int) {
        val dialog = WordDetailDialogFragment().withArguments(
                Extras.EXTRA_QUIZ_IDS to quizIds,
                Extras.EXTRA_QUIZ_TITLE to quizTitle,
                Extras.EXTRA_LEVEL to level,
                Extras.EXTRA_WORD_POSITION to position,
                Extras.EXTRA_SEARCH_STRING to "")
        dialog.show(childFragmentManager, "")
        dialog.isCancelable = true
    }

    override fun onCategoryIconClick(position: Int) {
        activity!!.startActionMode(actionModeCallback)
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
                activity!!.startActionMode(actionModeCallback)
        }
        return super.onOptionsItemSelected(item)
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            adapter.checkMode = true
            adapter.notifyDataSetChanged()
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                3 -> {
                    adapter.items.forEach { it.isSelected = 1 }
                    adapter.notifyDataSetChanged()
                }
                4 -> {
                    adapter.items.forEach { it.isSelected = 0 }
                    adapter.notifyDataSetChanged()
                }
                1 -> {
                    val popup = PopupMenu(activity!!, activity!!.find(1))
                    popup.menuInflater.inflate(R.menu.popup_selections, popup.menu)
                    for ((i, selection) in selections.withIndex()) {
                        popup.menu.add(1, i, i, selection.getName()).isChecked = false
                    }
                    popup.setOnMenuItemClickListener {
                        val selectedWords: ArrayList<Word> = arrayListOf()
                        adapter.items.forEach { if (it.isSelected == 1) selectedWords.add(it) }
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
                2 -> {

                    val popup = PopupMenu(activity!!, activity!!.find(2))
                    var i = 0
                    for (selection in selections) {
                        popup.menu.add(1, i, i, selection.getName()).isChecked = false
                        i++
                    }
                    popup.setOnMenuItemClickListener {
                        val selectedWords: ArrayList<Word> = arrayListOf()
                        adapter.items.forEach { if (it.isSelected == 1) selectedWords.add(it) }
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
            }
            return false
        }

        private fun addSelection(selectedWords: ArrayList<Word>) {
            alert {
                title = getString(R.string.new_selection)
                val input = EditText(activity)
                input.setSingleLine()
                input.hint = getString(R.string.selection_name)
                val container = FrameLayout(requireActivity())
                val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.leftMargin = DimensionHelper.getPixelFromDip(activity, 20)
                params.rightMargin = DimensionHelper.getPixelFromDip(activity, 20)
                input.layoutParams = params
                container.addView(input)
                customView = container
                okButton {
                    var selectionId = mpresenter!!.createSelection(input.text.toString())
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
            menu.add(0, 1, 0, getString(R.string.add_to_selections)).setIcon(R.drawable.ic_selections_selected)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, 2, 0, getString(R.string.remove_from_selection)).setIcon(R.drawable.ic_unselect)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, 3, 0, getString(R.string.select_all)).setIcon(R.drawable.ic_select_multiple)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, 4, 0, getString(R.string.unselect_all)).setIcon(R.drawable.ic_unselect_multiple)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            adapter.checkMode = false
            adapter.notifyDataSetChanged()
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

    fun unlockFullVersion() {
        adapter.notifyDataSetChanged()
    }

}