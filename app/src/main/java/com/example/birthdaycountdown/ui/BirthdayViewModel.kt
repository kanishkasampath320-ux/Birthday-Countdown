package com.example.birthdaycountdown.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.birthdaycountdown.data.local.BirthdayDatabase
import com.example.birthdaycountdown.data.model.Birthday
import com.example.birthdaycountdown.data.repository.BirthdayRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class BirthdayViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BirthdayRepository
    val allBirthdays: StateFlow<List<Birthday>>

    private val _selectedCalendarDate = MutableStateFlow(LocalDate.now())
    val selectedCalendarDate = _selectedCalendarDate.asStateFlow()

    private val prefs = application.getSharedPreferences("birthday_prefs", Context.MODE_PRIVATE)

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode = _isDarkMode.asStateFlow()

    init {
        val birthdayDao = BirthdayDatabase.getDatabase(application).birthdayDao()
        repository = BirthdayRepository(birthdayDao)
        allBirthdays = repository.allBirthdays.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    fun setSelectedCalendarDate(date: LocalDate) {
        _selectedCalendarDate.value = date
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun insert(name: String, dateOfBirth: String, imageUri: String?, reminderTime: String) {
        viewModelScope.launch {
            repository.insert(Birthday(name = name, dateOfBirth = dateOfBirth, imageUri = imageUri, reminderTime = reminderTime))
            // Highlight the added birthday in calendar
            try {
                setSelectedCalendarDate(LocalDate.parse(dateOfBirth))
            } catch (e: Exception) {}
        }
    }

    fun update(birthday: Birthday) {
        viewModelScope.launch {
            repository.update(birthday)
        }
    }

    fun delete(birthday: Birthday) {
        viewModelScope.launch {
            repository.delete(birthday)
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }
}
