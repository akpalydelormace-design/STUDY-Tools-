package com.example.data.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Calendar

class StudyNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Évaluation à venir"
        val subject = intent.getStringExtra("EXTRA_SUBJECT") ?: "Matière"
        val subtitle = intent.getStringExtra("EXTRA_SUBTITLE") ?: "N'oublie pas de réviser !"
        val eventId = intent.getLongExtra("EXTRA_EVENT_ID", 0L)

        NotificationHelper.showNotification(
            context = context,
            notificationId = eventId.toInt(),
            title = "⏰ $subject — $title",
            message = subtitle
        )
    }
}

object NotificationHelper {

    const val CHANNEL_ID = "study_tools_evaluations"
    private const val CHANNEL_NAME = "Évaluations et Devoirs"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rappels et notifications d'évaluations scolaires"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun scheduleReminder(
        context: Context,
        eventId: Long,
        subjectName: String,
        title: String,
        eventDateTime: Long,
        reminderOption: String, // NONE, SAME_DAY, 1_DAY_BEFORE, 2_DAYS_BEFORE, CUSTOM
        reminderHour: Int = 8,
        reminderMinute: Int = 0
    ) {
        if (reminderOption == "NONE") return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val targetCal = Calendar.getInstance().apply {
            timeInMillis = eventDateTime
            when (reminderOption) {
                "SAME_DAY" -> {
                    set(Calendar.HOUR_OF_DAY, reminderHour)
                    set(Calendar.MINUTE, reminderMinute)
                    set(Calendar.SECOND, 0)
                }
                "1_DAY_BEFORE" -> {
                    add(Calendar.DAY_OF_YEAR, -1)
                    set(Calendar.HOUR_OF_DAY, reminderHour)
                    set(Calendar.MINUTE, reminderMinute)
                    set(Calendar.SECOND, 0)
                }
                "2_DAYS_BEFORE" -> {
                    add(Calendar.DAY_OF_YEAR, -2)
                    set(Calendar.HOUR_OF_DAY, reminderHour)
                    set(Calendar.MINUTE, reminderMinute)
                    set(Calendar.SECOND, 0)
                }
                "CUSTOM" -> {
                    set(Calendar.HOUR_OF_DAY, reminderHour)
                    set(Calendar.MINUTE, reminderMinute)
                    set(Calendar.SECOND, 0)
                }
            }
        }

        val triggerAtMillis = targetCal.timeInMillis
        // Only schedule if in future
        if (triggerAtMillis <= System.currentTimeMillis()) return

        val reminderDesc = when (reminderOption) {
            "1_DAY_BEFORE" -> "Évaluation demain ! N'oublie pas de réviser."
            "2_DAYS_BEFORE" -> "Évaluation dans 2 jours ! N'oublie pas de réviser."
            else -> "Évaluation aujourd'hui ! Bonne chance pour ton épreuve."
        }

        val intent = Intent(context, StudyNotificationReceiver::class.java).apply {
            action = "com.example.studytools.ACTION_SHOW_NOTIFICATION"
            putExtra("EXTRA_EVENT_ID", eventId)
            putExtra("EXTRA_SUBJECT", subjectName)
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_SUBTITLE", reminderDesc)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w("NotificationHelper", "Could not set exact alarm due to permission", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, eventId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, StudyNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
