package com.jehutyno.yomikata.util

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable


/**
 * Helper functions to deal with deprecated getParcelable, getSerializable, etc. functions
 */


fun <T: Parcelable> Bundle.getParcelableArrayListHelper(key: String?, clazz: Class<out T>): ArrayList<T>? {
    // Force the app class loader so the Parcelable's class (and its CREATOR) résout
    // même quand le système restaure le Bundle avec le class loader de la plateforme.
    classLoader = clazz.classLoader
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getParcelableArrayList(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelableArrayList(key)
        }
    } catch (e: Exception) {
        // Certaines ROM (MIUI/Android 13…) lèvent en validant la classe Parcelable
        // contre `clazz`. La variante non typée saute cette validation → pas de crash.
        try {
            @Suppress("DEPRECATION")
            this.getParcelableArrayList(key)
        } catch (e2: Exception) {
            null
        }
    }
}

fun <T: Parcelable> Bundle.getParcelableHelper(key: String?, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getParcelable(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        this.getParcelable(key)
    }
}

fun <T> Parcel.readParcelableHelper(loader: ClassLoader?, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.readParcelable(loader, clazz)
    } else {
        @Suppress("DEPRECATION")
        this.readParcelable(loader)
    }
}

fun <T : Serializable?> Bundle.getSerializableHelper(key: String?, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getSerializable(key, clazz)
    } else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        this.getSerializable(key) as T?
    }
}

fun <T : Serializable?> Intent.getSerializableExtraHelper(name: String?, clazz: Class<T>): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getSerializableExtra(name, clazz)
    } else {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        this.getSerializableExtra(name) as T?
    }
}

fun <T: Parcelable> Intent.getParcelableArrayListExtraHelper(name: String?, clazz: Class<out T>): ArrayList<T>? {
    // Force the app class loader so the Parcelable's class (et son CREATOR) résout
    // même quand le système redélivre l'Intent avec le class loader de la plateforme.
    setExtrasClassLoader(clazz.classLoader)
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.getParcelableArrayListExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            this.getParcelableArrayListExtra(name)
        }
    } catch (e: Exception) {
        // Certaines ROM (MIUI/Android 13…) lèvent en validant la classe Parcelable
        // contre `clazz`. La variante non typée saute cette validation → pas de crash.
        try {
            @Suppress("DEPRECATION")
            this.getParcelableArrayListExtra(name)
        } catch (e2: Exception) {
            null
        }
    }
}
