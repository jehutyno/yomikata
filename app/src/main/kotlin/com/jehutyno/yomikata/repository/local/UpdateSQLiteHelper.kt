package com.jehutyno.yomikata.repository.local

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log
import com.jehutyno.yomikata.R
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Created by valentin on 07/10/2016.
 */
class UpdateSQLiteHelper(var context: Context, val filePath: String) : ManagedSQLiteOpenHelper(context, UpdateSQLiteHelper.UPDATE_DATABASE_NAME, null, DATABASE_VERSION) {

    lateinit private var database: SQLiteDatabase
    private var flag: Boolean = false

    companion object {
        val UPDATE_DATABASE_NAME = "yomikataz_update.db"
        val DATABASE_VERSION = 12

        private var instance: UpdateSQLiteHelper? = null

        @Synchronized
        fun getInstance(ctx: Context, filePath: String): UpdateSQLiteHelper {
            if (instance == null) {
                instance = UpdateSQLiteHelper(ctx.applicationContext, filePath)
            }
            instance!!.forceDatabaseReset()
            return instance!!
        }
    }

    /**
     * Creates a empty database on the system and rewrites it with your own
     * database.
     */
    @Throws(IOException::class)
    fun createDataBase() {
        val dbExist = checkDataBase()
        if (dbExist) {
            // do nothing - database already exist
        } else {
            this.readableDatabase
            try {
                copyDataBase()
            } catch (e: IOException) {
                e.printStackTrace()
                throw Error("Error copying database")
            }
        }
    }

    @Throws(IOException::class)
    fun forceDatabaseReset() {
        this.readableDatabase
        try {
            copyDataBase()
        } catch (e: IOException) {
            throw Error("Error copying database")
        }
    }

    /**
     * Check if the database already exist to avoid re-copying the file each
     * time you open the application.
     *
     * @return true if it exists, false if it doesn't
     */
    private fun checkDataBase(): Boolean {
        var checkDB: SQLiteDatabase? = null
        try {
            val myPath = context.getString(R.string.db_path) + UPDATE_DATABASE_NAME
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
        if (checkDB != null) {
            checkDB.close()
        }

        return checkDB != null
    }

    /**
     * Copies your database from your local assets-folder to the just created
     * empty database in the system folder, from where it can be accessed and
     * handled. This is done by transfering bytestream.
     */
    @Throws(IOException::class)
    private fun copyDataBase() {
        // Open your local db as the input stream
        val myInput = if (!filePath.isEmpty())
            File(filePath).inputStream()
        else
            context.assets.open(SQLiteHelper.DATABASE_NAME)
        // Path to the just created empty db
        val outFileName = context.getString(R.string.db_path) + UPDATE_DATABASE_NAME
        // Open the empty db as the output stream
        val myOutput = FileOutputStream(outFileName)
        // transfer bytes from the inputfile to the outputfile
        val buffer = ByteArray(1024)
        var length = myInput.read(buffer)
        while (length > 0) {
            myOutput.write(buffer, 0, length)
            length = myInput.read(buffer)
        }
        // Close the streams
        myOutput.flush()
        myOutput.close()
        myInput.close()
    }

    @Throws(SQLException::class)
    fun openDataBase() {
        // Open the database
        val myPath = context.getString(R.string.db_path) + UPDATE_DATABASE_NAME
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
        if (!flag) {
            try {
                createDataBase()
            } catch (ioe: IOException) {
                throw Error("Unable to create database")
            }

            try {
                openDataBase()
                database = writableDatabase
                flag = true
            } catch (sqle: SQLException) {
                throw sqle
            }
        }

        return database
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.w(SQLiteHelper::class.java.name,
            "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data")
        // db.execSQL("DROP TABLE IF EXISTS " + TABLE_COMMENTS);
        onCreate(db)
    }
}

// Access property for Context
val Context.updatedatabase: UpdateSQLiteHelper
    get() = UpdateSQLiteHelper.getInstance(applicationContext, "")