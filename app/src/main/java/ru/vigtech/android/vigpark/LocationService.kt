package ru.vigtech.android.vigpark

import android.Manifest
import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.gson.JsonObject
import ru.vigtech.android.vigpark.api.ApiClient
import java.io.IOException


class LocationService : Service(), LocationListener {
    private var message: JsonObject? = null
    var isGPSEnable = false
    var latitude = 0.0
    var longitude = 0.0
    var locationManager: LocationManager? = null
    var intent: Intent? = null
    var params: MutableList<Pair<String, String>> = arrayListOf()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getNotificationChannel(
            notificationManager
        )
        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this, channelId)
        val notification: Notification = notificationBuilder.setOngoing(true)
            .setContentText("VIGPARK Активен")
            .setSmallIcon(R.mipmap.sym_def_app_icon) // .setPriority(PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        this.startForeground(110, notification)

        intent = Intent(str_receiver)
        message = JsonObject()
        locationManager = getApplicationContext().getSystemService(LOCATION_SERVICE) as LocationManager?
        isGPSEnable = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.e("catch", "location!=null")
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && androidx.core.app.ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L,10f,this)
    }

    override fun onLocationChanged(p0: Location) {
        var location: Location? = p0
        if (isGPSEnable) {
            if (androidx.core.app.ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && androidx.core.app.ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            location = locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                Log.e("catch", "location!=null")
                message?.addProperty("lat", location.getLatitude())
                message?.addProperty("lng", location.getLongitude())
//                if (MainActivity.stsrtFinish) {
                if (true) {
                    params!!.add(
                        Pair(
                            "Latitude",
                            java.lang.String.valueOf(location.getLatitude())
                        )
                    )
                    params!!.add(
                        Pair(
                            "Longitude",
                            java.lang.String.valueOf(location.getLongitude())
                        )
                    )
                } else {
                    params!!.add(Pair("Latitude", "0.0"))
                    params!!.add(Pair("Longitude", "0.0"))
                }
                Thread {
                    try {
                        //todo location send
                            ApiClient.POST_location(location.getLongitude(),location.getLatitude())
                        Log.e("catch", params!!.last().toString())
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e("catch", "catch")
                    }
                }.start()
                latitude = location.getLatitude()
                longitude = location.getLongitude()
                fn_update(location)
            }
        }
    }


    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
   override fun onProviderEnabled(provider: String) {}
   override fun onProviderDisabled(provider: String) {}


    private fun fn_update(location: Location) {
        intent?.putExtra("latutide", location.getLatitude().toString() + "")
        intent?.putExtra("longitude", location.getLongitude().toString() + "")
        sendBroadcast(intent)
    }

    companion object {
        var str_receiver = "servicetutorial.service.receiver"
    }

    private fun getNotificationChannel(notificationManager: NotificationManager): String {
        val channelId = "channelid"
        val channelName = "VigPark"
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        channel.importance = NotificationManager.IMPORTANCE_NONE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(channel)
        return channelId
    }
}