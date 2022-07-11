package ru.vigtech.android.vigpark

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class MyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            context.startForegroundService(Intent(context, LocationService::class.java))
        }
        catch (e:Exception){
            Log.i("catch", "Alarm don't working")
        }
        Log.i("catch", "Alarm working")
    }
}