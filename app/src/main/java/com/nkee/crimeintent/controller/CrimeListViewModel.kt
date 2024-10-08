package com.nkee.crimeintent.controller

import androidx.lifecycle.ViewModel
import com.nkee.crimeintent.model.Crime

class CrimeListViewModel : ViewModel() {
    val crimes = mutableListOf<Crime>()

    init {
        for (i in 1 .. 100) {
            crimes += Crime(title = "Crime #$i", isSolved = i % 2 == 0)
        }
    }
}