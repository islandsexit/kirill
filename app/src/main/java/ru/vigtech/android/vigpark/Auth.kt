package ru.vigtech.android.vigpark

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import androidx.work.impl.utils.Preferences
import java.util.*

const val UUIDKEY = "uuidKey"
const val SECUREKEY = "secureKey"
const val AUTHSUCCESS = "authSuccess"
class Auth(context: Context): ViewModel(){


    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor = preferences.edit()



    var authSuccess: MutableLiveData<Boolean> = MutableLiveData(preferences.getBoolean(AUTHSUCCESS, false))
        set(value) {
            field = value
            editor.putBoolean(AUTHSUCCESS, value.value!!).commit()
        }

    var uuidKey: String = preferences.getString(UUIDKEY, "").toString()
    var secureKey: String = preferences.getString(SECUREKEY, "").toString()
        set(value) {
            field = value
            editor.putString(SECUREKEY, value).commit()
        }


    init {
        if (uuidKey.isBlank()){
            uuidKey = UUID.randomUUID().toString()
            editor.putString(UUIDKEY, uuidKey).commit()
        }

    }








}