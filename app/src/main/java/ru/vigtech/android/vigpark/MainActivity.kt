package ru.vigtech.android.vigpark

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), MY_CAMERA_REQUEST)
        }
        if (checkSelfPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(), MY_READ_EXTERNAL_REQUEST)
        }



        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MyAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        var time = Calendar.getInstance();
        time.setTimeInMillis(System.currentTimeMillis());
        time.add(Calendar.SECOND, 5);

        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(),3000, pendingIntent);
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


    private fun fn_permission() {
       val boolean_permission = true;


        if (boolean_permission) {
            val intent2 = Intent(getApplicationContext(), LocationService::class.java);
//            startService(intent2);
            startForegroundService(intent2)
        } else {
            Toast.makeText(getApplicationContext(), "Please enable the gps", Toast.LENGTH_SHORT).show();
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
             2 -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    fn_permission();


                } else {
                    Toast.makeText(
                        applicationContext,
                        "Please allow the permission",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    }



