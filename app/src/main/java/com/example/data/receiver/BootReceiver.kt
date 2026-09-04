package com.example.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.StudyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for rescheduling upcoming agenda and evaluation
 * notifications upon device reboot or package update.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = StudyDatabase.getInstance(context)
                    val now = System.currentTimeMillis()
                    val events = db.agendaDao().getAllEventsList()
                    for (event in events) {
                        if (!event.isCompleted && event.dateTime > now && event.reminderOption != "NONE") {
                            NotificationHelper.scheduleReminder(
                                context = context,
                                eventId = event.id,
                                subjectName = event.subjectName,
                                title = event.title,
                                eventDateTime = event.dateTime,
                                reminderOption = event.reminderOption,
                                reminderHour = event.reminderHour,
                                reminderMinute = event.reminderMinute
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BootReceiver", "Error rescheduling notifications", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
