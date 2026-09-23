package com.example.birthdaycountdown.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.birthdaycountdown.data.local.BirthdayDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class BirthdayNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check user preferences
        val prefs = applicationContext.getSharedPreferences("birthday_prefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)
        
        if (!notificationsEnabled) return Result.success()

        val database = BirthdayDatabase.getDatabase(applicationContext)
        val birthdays = database.birthdayDao().getAllBirthdays().first()
        val today = LocalDate.now()
        val currentTime = LocalTime.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        birthdays.forEach { birthday ->
            try {
                val dob = LocalDate.parse(birthday.dateOfBirth, dateFormatter)
                if (dob.month == today.month && dob.dayOfMonth == today.dayOfMonth) {
                    val reminderTime = LocalTime.parse(birthday.reminderTime, timeFormatter)
                    // Simplified: check if current hour matches reminder hour
                    if (currentTime.hour == reminderTime.hour) {
                        showNotification(birthday.name)
                    }
                }
            } catch (e: Exception) { }
        }

        return Result.success()
    }

    private fun showNotification(name: String) {
        val channelId = "birthday_alerts"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Birthday Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Celebration Time! 🎉")
            .setContentText("It's $name's birthday! Time to celebrate.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(name.hashCode(), notification)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notificationManager.notify(name.hashCode(), notification)
        }
    }

    companion object {
        fun scheduleDailyWorker(context: Context) {
            val request = PeriodicWorkRequestBuilder<BirthdayNotificationWorker>(
                1, TimeUnit.HOURS
            ).setConstraints(
                Constraints.Builder().setRequiresBatteryNotLow(true).build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "BirthdayHourlyCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
