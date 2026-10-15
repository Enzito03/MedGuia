package com.example.medguiabase.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medguiabase.data.AppDatabase
import com.example.medguiabase.data.RegistroDoseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("alarm_id", 0L)
        val usuarioId = intent.getLongExtra("usuario_id", 0L)
        val remedyName = intent.getStringExtra("remedy_name") ?: "Remédio"
        val remedyIcon = intent.getStringExtra("remedy_icon") ?: "💊"
        val remedyPhoto = intent.getStringExtra("remedy_photo")
        val retryCount = intent.getIntExtra("retry_count", 0)

        if (intent.action == "ACTION_CONFIRM_DOSE") {
            if (usuarioId > 0 && alarmId > 0) {
                val db = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    db.registroDoseDao().insertRegistroDose(
                        RegistroDoseEntity(
                            usuarioId = usuarioId,
                            alarmeId = alarmId,
                            status = "tomado"
                        )
                    )
                }
            }

            NotificationHelper.cancelNotification(context, alarmId.toInt())
            NotificationHelper.cancelNotification(context, alarmId.toInt() * 10 + 1)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val exactIntent = Intent(context, AlarmReceiver::class.java)
            alarmManager.cancel(PendingIntent.getBroadcast(context, (alarmId * 10).toInt(), exactIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            alarmManager.cancel(PendingIntent.getBroadcast(context, (alarmId * 10 + 1).toInt(), exactIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            for (retry in 1..3) {
                alarmManager.cancel(PendingIntent.getBroadcast(context, (alarmId * 100 + retry).toInt(), exactIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            return
        }

        NotificationHelper.showAlarmNotification(context, alarmId, usuarioId, remedyName, remedyIcon, remedyPhoto, false)

        if (retryCount < 3) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("usuario_id", usuarioId)
                putExtra("remedy_name", remedyName)
                putExtra("remedy_icon", remedyIcon)
                putExtra("remedy_photo", remedyPhoto)
                putExtra("retry_count", retryCount + 1)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (alarmId * 100 + retryCount + 1).toInt(),
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + 5 * 60 * 1000 // 5 minutes
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            if (usuarioId > 0 && alarmId > 0) {
                val db = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    db.registroDoseDao().insertRegistroDose(
                        RegistroDoseEntity(
                            usuarioId = usuarioId,
                            alarmeId = alarmId,
                            status = "perdido"
                        )
                    )
                }
            }
        }
    }
}
