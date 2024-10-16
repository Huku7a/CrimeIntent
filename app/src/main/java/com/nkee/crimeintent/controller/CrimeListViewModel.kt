package com.nkee.crimeintent.controller

import androidx.lifecycle.ViewModel
import com.nkee.crimeintent.data.CrimeRepository
import com.nkee.crimeintent.model.Crime

class CrimeListViewModel : ViewModel() {
    private val crimeRepository = CrimeRepository.get()
    fun addCrime(crime: Crime) = crimeRepository.addCrime(crime)
    val crimeListLiveData = crimeRepository.getCrimes()

}