package com.jehutyno.yomikata

interface BaseView<in T> {

    fun setPresenter(presenter: T)

}
