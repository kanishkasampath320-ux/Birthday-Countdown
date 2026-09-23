package com.example.birthdaycountdown.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "birthdays")
data class Birthday(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val dateOfBirth: String, // Format: YYYY-MM-DD
    val imageUri: String? = null,
    val reminderTime: String = "09:00" // Format: HH:mm
)
