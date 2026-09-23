package com.example.birthdaycountdown.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.birthdaycountdown.data.model.Birthday

@Database(entities = [Birthday::class], version = 2, exportSchema = false)
abstract class BirthdayDatabase : RoomDatabase() {
    abstract fun birthdayDao(): BirthdayDao

    companion object {
        @Volatile
        private var INSTANCE: BirthdayDatabase? = null

        fun getDatabase(context: Context): BirthdayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BirthdayDatabase::class.java,
                    "birthday_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
