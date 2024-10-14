package com.nkee.crimeintent

import android.app.Application
import com.nkee.crimeintent.data.CrimeRepository

class CriminalIntentApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}