package com.example.ukbocw.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(context: Context) {

    val PREFERENCE_NAME = "UKBOCWWB"
    val DEVICE_ID = "androidDeviceId"

    private val mPrefs: SharedPreferences =
        context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    fun setDataInPref(keyString: String, value: String?) {
        val mEditor = mPrefs.edit()
        mEditor.putString(keyString, value)
        mEditor.commit()
    }

    fun getDataFromPref(keyString: String): String? {
        return mPrefs.getString(keyString, "")
    }
}