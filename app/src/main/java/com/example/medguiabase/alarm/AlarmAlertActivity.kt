package com.example.medguiabase.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class AlarmAlertActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var alarmId: Long = 0L
    private var usuarioId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        alarmId = intent.getLongExtra("alarm_id", 0L)
        usuarioId = intent.getLongExtra("usuario_id", 0L)
        val remedyName = intent.getStringExtra("remedy_name") ?: "Remédio"
        val remedyIcon = intent.getStringExtra("remedy_icon") ?: "💊"
        val remedyPhoto = intent.getStringExtra("remedy_photo")

        var bitmap: androidx.compose.ui.graphics.ImageBitmap? = null
        if (remedyPhoto != null) {
            try {
                val uri = android.net.Uri.parse(remedyPhoto)
                val inputStream = contentResolver.openInputStream(uri)
                val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap = bmp?.asImageBitmap()
            } catch (_: Exception) {}
        }

        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF17181A)) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap,
                            contentDescription = null,
                            modifier = Modifier.size(110.dp).clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Box(
                            modifier = Modifier.size(90.dp).clip(CircleShape).background(Color(0xFFC97A12)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(remedyIcon, fontSize = 42.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    Text("Hora do Remédio!", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(remedyName, color = Color(0xFFB7B6B2), fontSize = 18.sp)
                    Spacer(Modifier.height(48.dp))

                    Button(
                        onClick = { confirmAndStop() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F9D55)),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Tomado / Confirmar", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { stopAlarmAndFinish() },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Adiar (10 min)", fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }

    private fun confirmAndStop() {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = "ACTION_CONFIRM_DOSE"
            putExtra("alarm_id", alarmId)
            putExtra("usuario_id", usuarioId)
        }
        sendBroadcast(intent)
        stopAlarmAndFinish()
    }

    private fun stopAlarmAndFinish() {
        try { ringtone?.stop() } catch (_: Exception) {}
        try { vibrator?.cancel() } catch (_: Exception) {}
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmAndFinish()
    }
}
