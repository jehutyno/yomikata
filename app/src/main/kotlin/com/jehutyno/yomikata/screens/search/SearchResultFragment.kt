package com.jehutyno.yomikata.screens.search

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import android.view.*
import android.widget.EditText
import android.widget.FrameLayout
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.model.Quiz
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.content.WordsAdapter
import com.jehutyno.yomikata.screens.content.word.WordDetailDialogFragment
import com.jehutyno.yomikata.util.DimensionHelper
import com.jehutyno.yomikata.util.Extras
import kotlinx.android.synthetic.main.fragment_content.*
import org.jetbrains.anko.cancelButton
import org.jetbrains.anko.find
import org.jetbrains.anko.okButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.support.v4.withArguments
import java.util.*

/**
 * Created by valentin on 13/10/2016.
 */
class SearchResultFragment : Fragment(), SearchResultContract.View, WordsAdapter.Callback {
    private lateinit var searchResultPresenter : SearchResultContract.Presenter
    private lateinit var adapter: WordsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var searchString = ""
    lateinit private var selections: List<Quiz>

    override fun setPresenter(presenter: SearchResultContract.Presenter) {
        searchResultPresenter = presenter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = WordsAdapter(activity!!, this)
        layoutManager = GridLayoutManager(context, 2)

        setHasOptionsMenu(true)
    }


    override fun onResume() {
        super.onResume()
        searchResultPresenter.start()
        searchResultPresenter.loadSelections()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerview_content.let {
            it.adapter = adapter
            it.layoutManager = layoutManager
        }
    }

    override fun displayResults(words: List<Word>) {
        adapter.replaceData(words)
    }

    override fun displayNoResults() {
        adapter.clearData()
        toast(getString(R.string.search_no_results))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val searchMenuItem = menu.findItem(R.id.search)
        var searchView = searchMenuItem.actionView as SearchView
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
        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener{
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                activity!!.finish()
                return true
            }

        })

        searchMenuItem.expandActionView()

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onItemClick(position: Int) {
        val dialog = WordDetailDialogFragment().withArguments(
            Extras.EXTRA_QUIZ_IDS to longArrayOf(),
            Extras.EXTRA_WORD_POSITION to position,
            Extras.EXTRA_SEARCH_STRING to searchString)
        dialog.show(childFragmentManager, "")
        dialog.isCancelable = true

    }

    override fun onCategoryIconClick(position: Int) {
        activity!!.startActionMode(actionModeCallback)
    }

    override fun onCheckChange(position: Int, check: Boolean) {
        searchResultPresenter.updateWordCheck(adapter.items[position].id, check)
    }

    val actionModeCallback = object : ActionMode.Callback {
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
                    var selectionId = searchResultPresenter.createSelection(input.text.toString())
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

}