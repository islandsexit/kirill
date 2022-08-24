package ru.vigtech.android.vigpark

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import okhttp3.internal.wait
import ru.vigtech.android.vigpark.api.ApiClient
import ru.vigtech.android.vigpark.database.Crime
import ru.vigtech.android.vigpark.database.CrimeRepository
import ru.vigtech.android.vigpark.fragment.CrimeFragment
import ru.vigtech.android.vigpark.fragment.CrimeListFragment
import java.io.File
import java.util.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), CrimeListFragment.Callbacks {

    lateinit var test_button: Button
    val crimeRepository = CrimeRepository.get()




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


//        ApiClient.buidAuthModule(this)
//        val viewModel = ViewModelProvider(this).get(Auth::class.java)
//
//        val authObserver = Observer<Boolean>{
//            if (it){
//
//            }
//        }



        alertKey()




        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getpermission = Intent()
                getpermission.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getpermission)
            }
        }


        val MY_CAMERA_REQUEST: Int = 2
        if (checkSelfPermission(
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), MY_CAMERA_REQUEST
            )
        }


        val currentFragment =
            supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment == null) {
            val fragment = CrimeListFragment.newInstance()
            supportFragmentManager
                .beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit()
        }

    }

    private fun alertKey() {
        if (!ApiClient.authentication?.authSuccess?.value!!) {
            val alert = AlertDialog.Builder(this)
            val edittext = EditText(this)
            edittext.hint = SpannableStringBuilder("xxx-xxx-xxx-xxx");
            alert.setMessage("Программное обеспечение защищено")
            alert.setTitle("Введите лицензионный ключ")

            alert.setView(edittext)

            alert.setPositiveButton(
                "Ок"
            ) { dialog, whichButton ->
                ApiClient.authentication?.secureKey = edittext.text.toString()



                ApiClient.postAuthKeys()

                Log.i("AUUUUUUUUCTHHHHHH", "Api ${ApiClient.authentication?.authSuccess?.value!!}")

            }

            alert.setNegativeButton(
                "У меня нет ключа"
            ) { dialog, whichButton ->
                this.finish()
            }
            alert.setCancelable(false)
            alert.show()
        }
    }

    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeFragment.newInstance(crimeId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    fun add_crime(view: View) {
        crimeRepository.addCrime(Crime())
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {

                return true
            }
            KeyEvent.KEYCODE_SEARCH -> {

                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                onBackPressed()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                val fm = supportFragmentManager
                val fragment: CrimeListFragment? =
                    fm.findFragmentById(R.id.fragment_container) as CrimeListFragment?
                fragment?.cameraxHelper?.takePicture()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                return false
            }
        }
        return super.onKeyDown(keyCode, event)
    }


}



