package com.jehutyno.yomikata.screens.search

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentContentBinding
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.word.WordDetailDialogFragment
import com.jehutyno.yomikata.screens.word.WordsAdapter
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.view.WordSelectorActionModeCallback
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider


/**
 * Created by valentin on 13/10/2016.
 */
class SearchResultFragment(private val di: DI) : Fragment(), SearchResultContract.View, WordsAdapter.Callback {
    private lateinit var adapter: WordsAdapter
    private lateinit var actionModeCallback: ActionMode.Callback
    private lateinit var layoutManager: LinearLayoutManager
    private var searchString = ""

    // kodein
    private val subDI by DI.lazy {
        extend(di)
        bind<SearchResultContract.Presenter>() with provider {
            SearchResultPresenter(instance(), instance(arg = lifecycleScope), instance())
        }
    }
    private val searchResultPresenter : SearchResultContract.Presenter by subDI.instance()

    // View Binding
    private var _binding: FragmentContentBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = WordsAdapter(requireActivity(), this)
        actionModeCallback = WordSelectorActionModeCallback (
            ::requireActivity, adapter, searchResultPresenter, searchResultPresenter
        )
        layoutManager = GridLayoutManager(context, 2)

        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()

        searchResultPresenter.words.observe(viewLifecycleOwner) {
            if (it.isEmpty()) {
                displayNoResults()
            } else {
                displayResults(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        searchResultPresenter.start()
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
                searchResultPresenter.updateSearchString(searchString)
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

    override fun onCheckChange(position: Int, check: Boolean) = runBlocking {
        searchResultPresenter.updateWordCheck(adapter.items[position].id, check)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
