package ru.vigtech.android.vigpark.viewmodel

import androidx.lifecycle.ViewModel
import ru.vigtech.android.vigpark.database.Crime
import ru.vigtech.android.vigpark.database.CrimeRepository

class CrimeListViewModel : ViewModel() {

    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData = crimeRepository.getCrimes()
    val crimeUnsendLiveData = crimeRepository.getUnsendCrimes()

    fun addCrime(crime: Crime) {
        crimeRepository.addCrime(crime)
    }

    fun deleteCrime(crime: Crime){
        crimeRepository.deleteCrime(crime)
    }

    fun getCrimeFromPosition(position:Int): Crime {
        return crimeRepository.getCrimeFromPosition(position)
    }


}
