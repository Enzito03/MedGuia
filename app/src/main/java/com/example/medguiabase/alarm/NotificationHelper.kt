package com.example.medguiabase.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.medguiabase.R

object NotificationHelper {
    private const val CHANNEL_ID = "medguia_alarms"
    private const val CHANNEL_NAME = "Lembretes de Remédios"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para lembretes de medicação do MedGuia"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showAlarmNotification(context: Context, alarmId: Long, usuarioId: Long, remedyName: String, remedyIcon: String, remedyPhoto: String?, isPreWarning: Boolean) {
        createNotificationChannel(context)

        val fullScreenIntent = Intent(context, AlarmAlertActivity::class.java).apply {
            putExtra("alarm_id", alarmId)
            putExtra("usuario_id", usuarioId)
            putExtra("remedy_name", remedyName)
            putExtra("remedy_icon", remedyIcon)
            putExtra("remedy_photo", remedyPhoto)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isPreWarning) "Aviso de Medicamento" else "Hora do Remédio!"
        val message = if (isPreWarning) "Hora de tomar $remedyName em 10 minutos!" else "Chegou a hora de tomar $remedyName."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alarmId.toInt(), notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}
