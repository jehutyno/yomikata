package com.jehutyno.yomikata.screens.search

import android.os.Bundle
import android.view.*
import android.view.MenuItem
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentContentBinding
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.content.WordsAdapter
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.util.DimensionHelper
import com.jehutyno.yomikata.util.Extras
import org.kodein.di.DI
import splitties.alertdialog.appcompat.*
import java.util.*

/**
 * Created by valentin on 13/10/2016.
 */
class SearchResultFragment(private val di: DI) : Fragment(), SearchResultContract.View, WordsAdapter.Callback {
    private lateinit var searchResultPresenter : SearchResultContract.Presenter
    private lateinit var adapter: WordsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var searchString = ""
    private lateinit var selections: List<Quiz>

    // View Binding
    private var _binding: FragmentContentBinding? = null
    private val binding get() = _binding!!


    override fun setPresenter(presenter: SearchResultContract.Presenter) {
        searchResultPresenter = presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = WordsAdapter(requireActivity(), this)
        layoutManager = GridLayoutManager(context, 2)

        setHasOptionsMenu(true)
    }


    override fun onResume() {
        super.onResume()
        searchResultPresenter.start()
        searchResultPresenter.loadSelections()
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

    override fun displayResults(words: List<Word>) {
        adapter.replaceData(words)
    }

    override fun displayNoResults() {
        adapter.clearData()
        val toast = Toast.makeText(context, R.string.search_no_results, Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val searchMenuItem = menu.findItem(R.id.search)
        val searchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                searchString = query.replace("'", " ").replace("\"", " ")
                searchResultPresenter.loadWords(searchString)
                if (!searchView.isIconified) {
                    searchView.isIconified = true
                }
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
//                if (!s.isEmpty()) {
//                    searchString = s
//                    searchResultPresenter.loadWords(s)
//                }
                return false
            }
        })

        searchMenuItem.setIcon(R.drawable.ic_arrow_back_orange_24dp)
        searchMenuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                activity!!.finish()
                return true
            }
        })

        searchMenuItem.expandActionView()

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onItemClick(position: Int) {
        val bundle = Bundle()
        bundle.putLongArray(Extras.EXTRA_QUIZ_IDS, longArrayOf())
        bundle.putInt(Extras.EXTRA_WORD_POSITION, position)
        bundle.putString(Extras.EXTRA_SEARCH_STRING, searchString)

        val dialog = WordDetailDialogFragment(di)
        dialog.arguments = bundle
        dialog.show(childFragmentManager, "")
        dialog.isCancelable = true

    }

    override fun onCategoryIconClick(position: Int) {
        requireActivity().startActionMode(actionModeCallback)
    }

    override fun onCheckChange(position: Int, check: Boolean) {
        searchResultPresenter.updateWordCheck(adapter.items[position].id, check)
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
                    popup.setOnMenuItemClickListener {it ->
                        val selectedWords: ArrayList<Word> = arrayListOf()
                        adapter.items.forEach { item -> if (item.isSelected == 1) selectedWords.add(item) }
                        val selectionItemId = it.itemId
                        when (it.itemId) {
                            R.id.add_selection -> addSelection(selectedWords)
                            else -> {
                                selectedWords.forEach {
                                    if (!searchResultPresenter.isWordInQuiz(it.id, selections[selectionItemId].id))
                                        searchResultPresenter.addWordToSelection(it.id, selections[selectionItemId].id)
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
                                    if (searchResultPresenter.isWordInQuiz(it.id, selections[selectionItemId].id))
                                        searchResultPresenter.deleteWordFromSelection(it.id, selections[selectionItemId].id)
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
                    val selectionId = searchResultPresenter.createSelection(input.text.toString())
                    selectedWords.forEach {
                        searchResultPresenter.addWordToSelection(it.id, selectionId)
                    }
                    searchResultPresenter.loadSelections()
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