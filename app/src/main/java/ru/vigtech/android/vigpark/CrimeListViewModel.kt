package ru.vigtech.android.vigpark

import androidx.lifecycle.ViewModel

class CrimeListViewModel : ViewModel() {

    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData = crimeRepository.getCrimes()
    val crimeUnsendLiveData = crimeRepository.getUnsendCrimes()

    fun addCrime(crime: Crime) {
        crimeRepository.addCrime(crime)
    }

    fun deleteCrime(crime:Crime){
        crimeRepository.deleteCrime(crime)
    }

    fun getCrimeFromPosition(position:Int):Crime{
        return crimeRepository.getCrimeFromPosition(position)
    }


}
