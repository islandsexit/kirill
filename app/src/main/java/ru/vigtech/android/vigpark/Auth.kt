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
class Auth: ViewModel(){

    lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var editor:SharedPreferences.Editor



    var authSuccess: MutableLiveData<Int> = MutableLiveData()
        set(value) {
            field = value
            editor.putInt(AUTHSUCCESS, value.value!!).commit()
        }



    var uuidKey: String = ""
        get() = preferences.getString(UUIDKEY, "").toString()
    var secureKey: String = ""
        get() = preferences.getString(SECUREKEY, "").toString()
        set(value) {
            field = value
            editor.putString(SECUREKEY, value).commit()
        }



    fun initViewModel(){
        preferences =
        PreferenceManager.getDefaultSharedPreferences(
            context
        )

        editor = preferences.edit()

        if (uuidKey.isBlank()){
            uuidKey = UUID.randomUUID().toString()
            editor.putString(UUIDKEY, uuidKey).commit()
        }
        authSuccess = MutableLiveData(preferences.getInt(AUTHSUCCESS, 1))
    }









}