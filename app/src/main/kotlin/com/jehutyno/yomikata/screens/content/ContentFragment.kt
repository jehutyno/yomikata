package com.jehutyno.yomikata.screens.content

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.jehutyno.yomikata.R
import com.jehutyno.yomikata.databinding.FragmentContentGraphBinding
import com.jehutyno.yomikata.model.Word
import com.jehutyno.yomikata.screens.word.WordDetailDialogFragment
import com.jehutyno.yomikata.screens.word.WordsAdapter
import com.jehutyno.yomikata.util.Extras
import com.jehutyno.yomikata.util.Level
import com.jehutyno.yomikata.util.SeekBarsManager
import com.jehutyno.yomikata.util.getSerializableHelper
import com.jehutyno.yomikata.view.WordSelectorActionModeCallback
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider


/**
 * Created by valentin on 30/09/2016.
 */
class ContentFragment(private val di: DI) : Fragment(), ContentContract.View, WordsAdapter.Callback {

    private lateinit var adapter: WordsAdapter
    private lateinit var actionModeCallback: ActionMode.Callback
    private lateinit var quizIds: LongArray
    private var quizTitle: String = ""
    private var level: Level? = null
    private var lastPosition = -1
    private var dialog: WordDetailDialogFragment? = null

    // kodein
    private val subDI by DI.lazy {
        extend(di)
        bind<ContentContract.Presenter>() with provider {
            ContentPresenter (
                instance(),
                instance(arg = lifecycleScope), instance(arg = quizIds), instance(),
                quizIds, level
            )
        }
    }
    private val mpresenter: ContentContract.Presenter by subDI.instance()

    // seekBars
    private lateinit var seekBars : SeekBarsManager

    // View Binding
    private var _binding: FragmentContentGraphBinding? = null
    private val binding get() = _binding!!

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("position", (binding.recyclerviewContent.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            quizIds = requireArguments().getLongArray(Extras.EXTRA_QUIZ_IDS)!!
            quizTitle = requireArguments().getString(Extras.EXTRA_QUIZ_TITLE)!!
            level = requireArguments().getSerializableHelper(Extras.EXTRA_LEVEL, Level::class.java)
        }

        if (savedInstanceState != null) {
            lastPosition = savedInstanceState.getInt("position")
        }

        adapter = WordsAdapter(requireActivity(), this)
        actionModeCallback = WordSelectorActionModeCallback (
            ::requireActivity, adapter, mpresenter, mpresenter
        )
        setHasOptionsMenu(true)
    }


    private val wordsObserver = Observer<List<Word>> {
        words -> displayWords(words)
    }

    override fun onStart() {
        super.onStart()
        mpresenter.start()
        mpresenter.words.observe(viewLifecycleOwner, wordsObserver)

        displayStats()
    }

    override fun onResume() {
        super.onResume()
        val position =
            if (lastPosition != -1) lastPosition
            else (binding.recyclerviewContent.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
        lastPosition = -1
        mpresenter.start()
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
            binding.seekMasterContainer.visibility = if (level == Level.MASTER) VISIBLE else GONE
        }
        seekBars.setTextViews(binding.textLow, binding.textMedium, binding.textHigh, binding.textMaster)
        mpresenter.let {
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
        mpresenter.words.removeObserver(wordsObserver)

        dialog = WordDetailDialogFragment(di)
        dialog!!.arguments = bundle
        dialog!!.show(childFragmentManager, "")
        dialog!!.isCancelable = true

        dialog!!.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                // continue observing
                mpresenter.words.observe(viewLifecycleOwner, wordsObserver)
            }
        })
    }

    override fun onCategoryIconClick(position: Int) {
        requireActivity().startActionMode(actionModeCallback)
    }

    override fun onCheckChange(position: Int, check: Boolean) = runBlocking {
        mpresenter.updateWordCheck(adapter.items[position].id, check)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
