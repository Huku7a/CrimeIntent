package com.nkee.crimeintent

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.nkee.crimeintent.database.CrimeDatabase
import com.nkee.crimeintent.model.Crime
import java.util.UUID

private const val DATABASE_NAME = "crime-database"

class CrimeRepository private constructor(context: Context) {

    private val database: CrimeDatabase = Room.databaseBuilder(
        context = context.applicationContext,
        CrimeDatabase::class.java,
        name = DATABASE_NAME
    ).build()

    private val crimeDao = database.crimeDao()

    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()

    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)

    companion object {
        private var INSTANCE: CrimeRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository {
            return INSTANCE ?: throw IllegalStateException("CrimeRepository must be initialized")
        }
    }
}