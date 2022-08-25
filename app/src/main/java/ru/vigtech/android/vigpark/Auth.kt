package ru.vigtech.android.vigpark

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import androidx.work.impl.utils.Preferences
import ru.vigtech.android.vigpark.api.ApiClient
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

    private fun MutableLiveData<Int>.savePreferences(num:Int){

        this.postValue(num)
        editor.putInt(AUTHSUCCESS, num).commit()
     }



    var uuidKey: String = ""
        get() = preferences.getString(UUIDKEY, "").toString()
        set(value){
            field = value
            editor.putString(UUIDKEY, value).commit()
        }
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

                if (uuidKey == ""){
                    uuidKey = UUID.randomUUID().toString()
                    editor.putString(UUIDKEY, uuidKey).commit()
                }
                authSuccess = MutableLiveData(preferences.getInt(AUTHSUCCESS, 1))
                Log.i("AUTHC", "uuidKey: ${preferences.getString(UUIDKEY, "none")}, authSucess: ${preferences.getInt(
                    AUTHSUCCESS, 4)}, secKey:${preferences.getString(SECUREKEY, "none")}")
            }

            fun onCheckLicence(isSuccess: Boolean){
                when(isSuccess){
                    true -> authSuccess.savePreferences(3)
                    false -> when(authSuccess.value){
                        1-> authSuccess.savePreferences(2)
                        2-> authSuccess.savePreferences(1)
                    }
                }


                Log.i("AUTHC", "uuidKey: ${preferences.getString(UUIDKEY, "none")}, authSucess: ${preferences.getInt(
                    AUTHSUCCESS, 4)}, secKey:${preferences.getString(SECUREKEY, "none")}")

            }
        }









