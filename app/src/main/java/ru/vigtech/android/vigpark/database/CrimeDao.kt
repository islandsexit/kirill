package ru.vigtech.android.vigpark.database

import androidx.lifecycle.LiveData
import androidx.room.*
import ru.vigtech.android.vigpark.Crime
import java.util.*

@Dao
interface CrimeDao {

    @Query("SELECT * FROM crime ORDER BY date DESC ")
    fun getCrimes(): LiveData<List<Crime>>

    @Query("SELECT * FROM crime WHERE send = 0")
    fun getUnsendCrimes(): LiveData<List<Crime>>

    @Query("SELECT * FROM crime  ORDER BY date DESC LIMIT 1 OFFSET (:position)")
    fun getCrimeFromPosition(position: Int): Crime

    @Query("SELECT * FROM crime WHERE id=(:id)")
    fun getCrime(id: UUID): LiveData<Crime?>

    @Update
    fun updateCrime(crime: Crime)

    @Insert
    fun addCrime(crime: Crime)

    @Delete
    fun deleteCrime(crime: Crime)
}