package com.jehutyno.yomikata.screens.content

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.jehutyno.yomikata.R

/**
 * Created by valentin on 21/12/2016.
 */

class ActionBarCallBack : ActionMode.Callback {
    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.title = "Selection Mode"
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select_all -> {}
            R.id.unselect_all -> {}
            R.id.add_selection -> {}
        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_content_select, menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {

    }

}