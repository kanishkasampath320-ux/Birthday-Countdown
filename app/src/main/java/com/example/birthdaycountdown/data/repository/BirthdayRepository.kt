package com.example.birthdaycountdown.data.repository

import com.example.birthdaycountdown.data.local.BirthdayDao
import com.example.birthdaycountdown.data.model.Birthday
import kotlinx.coroutines.flow.Flow

class BirthdayRepository(private val birthdayDao: BirthdayDao) {
    val allBirthdays: Flow<List<Birthday>> = birthdayDao.getAllBirthdays()

    suspend fun insert(birthday: Birthday) {
        birthdayDao.insertBirthday(birthday)
    }

    suspend fun update(birthday: Birthday) {
        birthdayDao.updateBirthday(birthday)
    }

    suspend fun delete(birthday: Birthday) {
        birthdayDao.deleteBirthday(birthday)
    }

    suspend fun getBirthdayById(id: Int): Birthday? {
        return birthdayDao.getBirthdayById(id)
    }

    suspend fun deleteAll() {
        birthdayDao.deleteAllBirthdays()
    }
}
