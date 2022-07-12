package ru.vigtech.android.vigpark

import android.app.Application
import ru.vigtech.android.vigpark.database.CrimeRepository

class CriminalIntentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}