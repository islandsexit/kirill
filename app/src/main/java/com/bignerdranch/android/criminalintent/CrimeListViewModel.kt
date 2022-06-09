package com.bignerdranch.android.criminalintent

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
