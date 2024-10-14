package com.nkee.crimeintent.controller

import androidx.lifecycle.ViewModel
import com.nkee.crimeintent.data.CrimeRepository

class CrimeListViewModel : ViewModel() {
    private val crimeRepository = CrimeRepository.get()
    val crimeListLiveData = crimeRepository.getCrimes()

}