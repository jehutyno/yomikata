package com.jehutyno.yomikata.repository.migration

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper

/**
 * Created by valentin on 07/10/2016.
 */
class DatabaseHelper(var context: Context, val dbName: String, val path: String) : ManagedSQLiteOpenHelper(context, dbName, null, Companion.DATABASE_VERSION) {

    lateinit private var database: SQLiteDatabase
    private var flag: Boolean = false

    companion object {
        private val DATABASE_VERSION = 999

        private var instance: DatabaseHelper? = null

        @Synchronized
        fun getInstance(ctx: Context, name: String, path: String): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(ctx.applicationContext, name, path)
            }
            return instance!!
        }
    }

    @Throws(SQLException::class)
    fun openDataBase() {
        // Open the database
        val myPath = path + dbName
        SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)
    }

    @Synchronized override fun close() {
        flag = false
        database.close()
        super.close()
    }

    override fun onCreate(database: SQLiteDatabase) {

    }

    @Throws(SQLException::class)
    fun open(): SQLiteDatabase {
        try {
            openDataBase()
            database = writableDatabase
            flag = true
        } catch (sqle: SQLException) {
            throw sqle
        }
        return database
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }
}