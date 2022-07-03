package ru.vigtech.android.vigpark

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import ru.vigtech.android.vigpark.api.ApiClient
import java.util.*


class MainActivity : AppCompatActivity(), CrimeListFragment.Callbacks {

    lateinit var test_button: Button
    public val crimeRepository = CrimeRepository.get()






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // Call setNavigationItemSelectedListener on the NavigationView to detect when items are clicked
//


        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                val getpermission = Intent()
                getpermission.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivity(getpermission)
            }
        }



        val MY_READ_EXTERNAL_REQUEST : Int = 1
        val MY_CAMERA_REQUEST : Int = 2
        if (checkSelfPermission(
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), MY_CAMERA_REQUEST)
        }
        if (checkSelfPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_READ_EXTERNAL_REQUEST)
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

    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeFragment.newInstance(crimeId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }


    fun add_crime(view: View){
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



