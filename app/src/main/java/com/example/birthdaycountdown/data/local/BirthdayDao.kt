package com.example.birthdaycountdown.data.local

import androidx.room.*
import com.example.birthdaycountdown.data.model.Birthday
import kotlinx.coroutines.flow.Flow

@Dao
interface BirthdayDao {
    @Query("SELECT * FROM birthdays ORDER BY name ASC")
    fun getAllBirthdays(): Flow<List<Birthday>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBirthday(birthday: Birthday)

    @Update
    suspend fun updateBirthday(birthday: Birthday)

    @Delete
    suspend fun deleteBirthday(birthday: Birthday)

    @Query("SELECT * FROM birthdays WHERE id = :id")
    suspend fun getBirthdayById(id: Int): Birthday?

    @Query("DELETE FROM birthdays")
    suspend fun deleteAllBirthdays()
}
