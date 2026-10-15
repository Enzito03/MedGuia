package com.example.medguiabase

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.medguiabase.alarm.AlarmAlertActivity
import com.example.medguiabase.alarm.AlarmReceiver
import com.example.medguiabase.alarm.NotificationHelper
import com.example.medguiabase.data.AlarmeEntity
import com.example.medguiabase.data.AppDatabase
import com.example.medguiabase.data.MedGuiaRepository
import com.example.medguiabase.data.RegistroDoseEntity
import com.example.medguiabase.data.UsuarioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        setContent { MedGuiaApp() }
    }
}

data class Palette(
    val bg: Color, val surface: Color, val surface2: Color, val border: Color,
    val text: Color, val textSecondary: Color, val textMuted: Color,
    val accentDoc: Color, val accentDocBg: Color, val accentPat: Color, val accentPatBg: Color,
    val success: Color, val danger: Color, val warning: Color, val warningBg: Color
)

val LightPalette = Palette(
    bg = Color(0xFFFAFAFA), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFF3F3F1), border = Color(0xFFE5E4E1),
    text = Color(0xFF1C1C1A), textSecondary = Color(0xFF5F5E5A), textMuted = Color(0xFF9B9995),
    accentDoc = Color(0xFF2F7DD1), accentDocBg = Color(0xFFE6F1FB), accentPat = Color(0xFF0F8F74), accentPatBg = Color(0xFFE3F6F1),
    success = Color(0xFF1F9D55), danger = Color(0xFFD8373A), warning = Color(0xFFC97A12), warningBg = Color(0xFFFAECD9)
)

val DarkPalette = Palette(
    bg = Color(0xFF17181A), surface = Color(0xFF1F2023), surface2 = Color(0xFF26282B), border = Color(0xFF34363A),
    text = Color(0xFFF2F2F0), textSecondary = Color(0xFFB7B6B2), textMuted = Color(0xFF7C7B77),
    accentDoc = Color(0xFF2F7DD1), accentDocBg = Color(0xFF16324D), accentPat = Color(0xFF0F8F74), accentPatBg = Color(0xFF0E3329),
    success = Color(0xFF1F9D55), danger = Color(0xFFD8373A), warning = Color(0xFFC97A12), warningBg = Color(0xFF3A2C14)
)

@Composable
fun loadUriBitmap(uriString: String?): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        if (uriString != null) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap = bmp?.asImageBitmap()
            } catch (_: Exception) {}
        }
    }
    return bitmap
}

@Composable
fun MedGuiaApp() {
    val context = LocalContext.current
    val repository = remember {
        MedGuiaRepository(AppDatabase.getDatabase(context))
    }
    val scope = rememberCoroutineScope()

    var isDark by remember { mutableStateOf(false) }
    val pal = if (isDark) DarkPalette else LightPalette
    var screen by remember { mutableStateOf("profile") }
    var loggedInUser by remember { mutableStateOf<UsuarioEntity?>(null) }
    var firstPatient by remember { mutableStateOf<UsuarioEntity?>(null) }
    var selectedPatientForDoctor by remember { mutableStateOf<UsuarioEntity?>(null) }
    var doctorPatients by remember { mutableStateOf<List<UsuarioEntity>>(emptyList()) }
    var notifRead by remember { mutableStateOf(false) }

    var alarmesList by remember { mutableStateOf<List<AlarmeEntity>>(emptyList()) }
    var registrosList by remember { mutableStateOf<List<RegistroDoseEntity>>(emptyList()) }

    // Load first patient to check if account exists on device
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            firstPatient = repository.getFirstPatient()
        }
    }

    // Load doctor patients & check pending patients for doctor notifications
    LaunchedEffect(loggedInUser) {
        loggedInUser?.let { user ->
            if (user.tipo == "medico") {
                repository.getPatientsForDoctor(user.id).collect { list ->
                    doctorPatients = list
                    // If there are pending patients, mark unread notification
                    if (list.any { it.vinculoStatus == "pendente" }) {
                        notifRead = false
                    }
                }
            } else if (user.tipo == "paciente") {
                scope.launch {
                    launch(Dispatchers.IO) {
                        repository.getAlarmesForUser(user.id).collect { list ->
                            alarmesList = list
                        }
                    }
                    launch(Dispatchers.IO) {
                        repository.getRegistrosForUser(user.id).collect { list ->
                            registrosList = list
                        }
                    }
                }
            }
        }
    }

    fun scheduleAlarm(context: Context, alarm: AlarmeEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val parts = alarm.horario.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toIntOrNull() ?: 8
            val minute = parts[1].toIntOrNull() ?: 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("usuario_id", alarm.usuarioId)
                putExtra("remedy_name", alarm.nomeRemedio)
                putExtra("remedy_icon", alarm.icone)
                putExtra("remedy_photo", alarm.fotoUri)
                putExtra("is_pre_warning", false)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (alarm.id * 10).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val showIntent = Intent(context, AlarmAlertActivity::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("usuario_id", alarm.usuarioId)
                putExtra("remedy_name", alarm.nomeRemedio)
                putExtra("remedy_icon", alarm.icone)
                putExtra("remedy_photo", alarm.fotoUri)
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                (alarm.id * 10 + 99).toInt(),
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)


            val preIntent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("usuario_id", alarm.usuarioId)
                putExtra("remedy_name", alarm.nomeRemedio)
                putExtra("remedy_icon", alarm.icone)
                putExtra("remedy_photo", alarm.fotoUri)
                putExtra("is_pre_warning", true)
            }
            val prePendingIntent = PendingIntent.getBroadcast(
                context,
                (alarm.id * 10 + 1).toInt(),
                preIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val preTime = calendar.timeInMillis - 10 * 60 * 1000
            if (preTime > System.currentTimeMillis()) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        preTime,
                        prePendingIntent
                    )
                } catch (_: SecurityException) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, preTime, prePendingIntent)
                }
            }
        }
    }

    fun confirmDose(alarmId: Long) {
        loggedInUser?.let { user ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    repository.insertRegistroDose(
                        RegistroDoseEntity(
                            usuarioId = user.id,
                            alarmeId = alarmId,
                            status = "tomado"
                        )
                    )
                }
            }
        }
        NotificationHelper.cancelNotification(context, (alarmId * 10).toInt())
        NotificationHelper.cancelNotification(context, (alarmId * 10 + 1).toInt())

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 10).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun logout() {
        loggedInUser = null
        screen = "profile"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = pal.bg) {
        when (screen) {
            "profile" -> ProfileScreen(pal, { screen = "d-login" }, {
                scope.launch {
                    val p = withContext(Dispatchers.IO) { repository.getFirstPatient() }
                    firstPatient = p
                    screen = if (p != null) "p-login2" else "p-login1"
                }
            }, { isDark = !isDark })
            "d-login" -> DoctorLoginScreen(pal, repository, { screen = "profile" }, { user ->
                loggedInUser = user
                screen = "d-home"
            }, { screen = "d-signup" })
            "d-signup" -> DoctorSignupScreen(pal, repository, { screen = "d-login" }, { user ->
                loggedInUser = user
                screen = "d-home"
            })
            "d-home" -> DoctorHomeScreen(pal, loggedInUser, doctorPatients, notifRead, { notifRead = true }, ::logout, { screen = it })
            "d-patients" -> DoctorPatientsScreen(pal, repository, loggedInUser, { screen = "d-home" }, { screen = it }, { patient ->
                selectedPatientForDoctor = patient
                screen = "d-patient-detail"
            })
            "d-patient-detail" -> {
                if (selectedPatientForDoctor != null) {
                    DoctorPatientDetailScreen(pal, repository, selectedPatientForDoctor!!, { screen = "d-patients" }, { alarm -> scheduleAlarm(context, alarm) })
                }
            }
            "d-history" -> StubScreen(pal, "Histórico", "🕘", { screen = "d-home" }, false, "historico", { screen = it }, null)
            "d-profile" -> StubScreen(pal, "Perfil", "👤", { screen = "d-home" }, false, "perfil", { screen = it }, ::logout)
            "p-login1" -> PatientLogin1Screen(pal, { screen = "profile" }, { screen = "p-login-form" })
            "p-login-form" -> PatientLoginFormScreen(pal, repository, { screen = "p-login1" }, { user ->
                loggedInUser = user
                screen = "p-home"
            }, { screen = "p-signup" })
            "p-signup" -> PatientSignupScreen(pal, repository, { user ->
                loggedInUser = user
                firstPatient = user
                screen = "p-home"
            }, { screen = "p-login-form" })
            "p-login2" -> PatientLogin2Screen(pal, firstPatient, { screen = "profile" }, {
                loggedInUser = firstPatient
                screen = "p-home"
            })
            "p-home" -> PatientHomeScreen(pal, loggedInUser, alarmesList, registrosList, notifRead, { notifRead = true }, ::logout, { confirmDose(it) }, { screen = it })
            "p-alarms" -> PatientAlarmsScreen(pal, repository, loggedInUser, { screen = "p-home" }, { alarm -> scheduleAlarm(context, alarm) }, { screen = it })
            "p-profile" -> StubScreen(pal, "Perfil", "👤", { screen = "p-home" }, true, "perfil", { screen = it }, ::logout)
        }
    }
}

@Composable
fun HeaderRow(pal: Palette, title: String?, onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp, color = pal.textSecondary) }
        } else {
            Spacer(Modifier.width(40.dp))
        }
        if (title != null) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = pal.text, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        } else {
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
fun IconCircle(bg: Color, emoji: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 24.sp)
        }
    }
}

@Composable
fun OptionRow(pal: Palette, emoji: String, iconBg: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(pal.surface)
            .border(1.dp, pal.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconBg), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = pal.text, fontSize = 14.sp)
            Text(subtitle, color = pal.textMuted, fontSize = 12.sp)
        }
        Text("›", color = pal.textMuted, fontSize = 18.sp)
    }
}

@Composable
fun ProfileScreen(pal: Palette, onDoctor: () -> Unit, onPatient: () -> Unit, onToggleTheme: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        Text("MEDGUIA", color = pal.textMuted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text("Quem está entrando?", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Selecione uma das opções abaixo para acessar o aplicativo.", fontSize = 13.sp, color = pal.textSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text("🔊", fontSize = 20.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        OptionRow(pal, "🩺", pal.accentDocBg, "Sou médico", "Acesso para profissionais", onDoctor)
        Spacer(Modifier.height(12.dp))
        OptionRow(pal, "👤", pal.accentPatBg, "Sou paciente", "Acesso para consultas e exames", onPatient)
        Spacer(Modifier.height(12.dp))
        OptionRow(pal, "🌙", pal.surface2, "Tema escuro", "Alternar entre claro e escuro", onToggleTheme)
    }
}

@Composable
fun DoctorLoginScreen(pal: Palette, repository: MedGuiaRepository, onBack: () -> Unit, onEnter: (UsuarioEntity) -> Unit, onSignup: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var senhaError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        HeaderRow(pal, null, onBack)
        Text("MedGuia", color = pal.textMuted, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        IconCircle(pal.accentDocBg, "🩺")
        Spacer(Modifier.height(6.dp))
        Text("Login do médico", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = false; errorMessage = null },
            label = { Text("Email ou CRM") },
            isError = emailError,
            supportingText = if (emailError) { { Text("Campo obrigatório", color = pal.danger) } } else null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it; senhaError = false; errorMessage = null },
            label = { Text("Senha") },
            visualTransformation = PasswordVisualTransformation(),
            isError = senhaError,
            supportingText = if (senhaError) { { Text("Campo obrigatório", color = pal.danger) } } else null,
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(errorMessage!!, color = pal.danger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                emailError = email.isBlank()
                senhaError = senha.isBlank()
                if (!emailError && !senhaError) {
                    scope.launch {
                        val user = withContext(Dispatchers.IO) { repository.loginUsuario(email.trim(), senha) }
                        if (user != null && user.tipo == "medico") {
                            onEnter(user)
                        } else {
                            errorMessage = "Email/senha incorretos"
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = pal.accentDoc),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Entrar") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onSignup, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Criar conta", color = pal.text) }
    }
}

@Composable
fun DoctorSignupScreen(pal: Palette, repository: MedGuiaRepository, onBack: () -> Unit, onDone: (UsuarioEntity) -> Unit) {
    var nome by remember { mutableStateOf("") }
    var crm by remember { mutableStateOf("") }
    var esp by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }

    var nomeError by remember { mutableStateOf(false) }
    var crmError by remember { mutableStateOf(false) }
    var espError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var senhaError by remember { mutableStateOf(false) }
    var confirmarError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        HeaderRow(pal, null, onBack)
        IconCircle(pal.accentDocBg, "🩺")
        Spacer(Modifier.height(6.dp))
        Text("Criar conta de médico", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(nome, { nome = it; nomeError = false }, label = { Text("Nome completo") }, isError = nomeError, supportingText = if (nomeError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(crm, { crm = it; crmError = false }, label = { Text("CRM") }, isError = crmError, supportingText = if (crmError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(esp, { esp = it; espError = false }, label = { Text("Especialidade") }, isError = espError, supportingText = if (espError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(email, { email = it; emailError = false }, label = { Text("Email") }, isError = emailError, supportingText = if (emailError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(senha, { senha = it; senhaError = false }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), isError = senhaError, supportingText = if (senhaError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(confirmar, { confirmar = it; confirmarError = false }, label = { Text("Confirmar senha") }, visualTransformation = PasswordVisualTransformation(), isError = confirmarError, supportingText = if (confirmarError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())

        if (errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(errorMessage!!, color = pal.danger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                nomeError = nome.isBlank()
                crmError = crm.isBlank()
                espError = esp.isBlank()
                emailError = email.isBlank()
                senhaError = senha.isBlank()
                confirmarError = confirmar.isBlank()

                if (!nomeError && !crmError && !espError && !emailError && !senhaError && !confirmarError) {
                    if (senha != confirmar) {
                        errorMessage = "As senhas não coincidem"
                    } else {
                        scope.launch {
                            val user = UsuarioEntity(
                                nome = nome.trim(),
                                email = email.trim(),
                                senha = senha,
                                tipo = "medico",
                                crm = crm.trim(),
                                especialidade = esp.trim(),
                                medicoVinculadoId = null
                            )
                            val id = withContext(Dispatchers.IO) { repository.insertUsuario(user) }
                            onDone(user.copy(id = id))
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = pal.accentDoc),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Criar conta") }
    }
}

@Composable
fun StatCard(pal: Palette, label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(pal.surface2).padding(12.dp)) {
        Text(label, fontSize = 11.sp, color = pal.textMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pal.text)
    }
}

@Composable
fun ActivityRow(pal: Palette, dotColor: Color, text: String, danger: Boolean) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(pal.surface).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = if (danger) pal.danger else pal.text, modifier = Modifier.weight(1f))
        Text("›", color = pal.textMuted)
    }
}

@Composable
fun NavItem(pal: Palette, emoji: String, label: String, active: Boolean, activeColor: Color, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(label, fontSize = 10.sp, color = if (active) activeColor else pal.textMuted)
    }
}

@Composable
fun BottomNavDoctor(pal: Palette, active: String, onNav: (String) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(pal.border))
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            NavItem(pal, "🏠", "Início", active == "inicio", pal.accentDoc) { onNav("d-home") }
            NavItem(pal, "👥", "Pacientes", active == "pacientes", pal.accentDoc) { onNav("d-patients") }
            NavItem(pal, "🕘", "Histórico", active == "historico", pal.accentDoc) { onNav("d-history") }
            NavItem(pal, "👤", "Perfil", active == "perfil", pal.accentDoc) { onNav("d-profile") }
        }
    }
}

@Composable
fun BottomNavPatient(pal: Palette, active: String, onNav: (String) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(pal.border))
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceAround) {
            NavItem(pal, "💊", "Remédios", active == "remedios", pal.accentPat) { onNav("p-home") }
            NavItem(pal, "⏰", "Alarmes", active == "alarmes", pal.accentPat) { onNav("p-alarms") }
            NavItem(pal, "👤", "Perfil", active == "perfil", pal.accentPat) { onNav("p-profile") }
        }
    }
}

@Composable
fun DoctorHomeScreen(pal: Palette, user: UsuarioEntity?, patients: List<UsuarioEntity>, notifRead: Boolean, onNotif: () -> Unit, onLogout: () -> Unit, onNav: (String) -> Unit) {
    val doctorName = user?.nome ?: "Dra. Camila"
    val pendingCount = patients.count { it.vinculoStatus == "pendente" }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLogout) { Text("←", fontSize = 20.sp, color = pal.textSecondary) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Boa noite,", fontSize = 12.sp, color = pal.textMuted)
                Text(doctorName, fontWeight = FontWeight.Bold, color = pal.text)
            }
            IconButton(onClick = onNotif) {
                Box {
                    Text("🔔", fontSize = 19.sp)
                    if (!notifRead && pendingCount > 0) {
                        Box(Modifier.size(8.dp).align(Alignment.TopEnd).clip(CircleShape).background(pal.danger))
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(pal, "Pacientes Pendentes", "$pendingCount", Modifier.weight(1f))
            StatCard(pal, "Adesão média", "87%", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Text("ATIVIDADE RECENTE", fontSize = 11.sp, color = pal.textMuted)
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pendingCount > 0) {
                ActivityRow(pal, pal.danger, "$pendingCount paciente(s) aguardando aprovação de vínculo.", true)
            }
            ActivityRow(pal, pal.success, "Maria S. — tomou às 8h", false)
            ActivityRow(pal, pal.success, "Ana C. — tomou às 10h30", false)
            ActivityRow(pal, pal.success, "Carlos R. — tomou às 12h", false)
        }
        BottomNavDoctor(pal, "inicio", onNav)
    }
}

@Composable
fun DoctorPatientsScreen(
    pal: Palette,
    repository: MedGuiaRepository,
    doctor: UsuarioEntity?,
    onBack: () -> Unit,
    onNav: (String) -> Unit,
    onSelectPatient: (UsuarioEntity) -> Unit
) {
    var patients by remember { mutableStateOf<List<UsuarioEntity>>(emptyList()) }
    var patientToReject by remember { mutableStateOf<UsuarioEntity?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(doctor) {
        doctor?.let { d ->
            repository.getPatientsForDoctor(d.id).collect { list ->
                patients = list
            }
        }
    }

    val pending = patients.filter { it.vinculoStatus == "pendente" }
    val accepted = patients.filter { it.vinculoStatus == "aceito" }
    val others = patients.filter { it.vinculoStatus != "pendente" && it.vinculoStatus != "aceito" }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        HeaderRow(pal, "Gerenciar Pacientes", onBack)
        Spacer(Modifier.height(14.dp))

        LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (pending.isNotEmpty()) {
                item {
                    Text("PENDENTES DE APROVAÇÃO", fontSize = 11.sp, color = pal.warning, fontWeight = FontWeight.Bold)
                }
                items(pending) { patient ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(pal.warningBg).border(1.dp, pal.warning, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👤", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(patient.nome, fontWeight = FontWeight.Bold, color = pal.text, fontSize = 14.sp)
                            Text(patient.email, color = pal.textMuted, fontSize = 12.sp)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { repository.updateVinculoStatus(patient.id, "aceito") }
                            }
                        }) {
                            Text("✓", color = pal.success, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { patientToReject = patient }) {
                            Text("✕", color = pal.danger, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text("PACIENTES ATIVOS", fontSize = 11.sp, color = pal.textMuted, fontWeight = FontWeight.Bold)
            }

            if (accepted.isEmpty() && others.isEmpty()) {
                item {
                    Text("Nenhum paciente aceito no momento.", color = pal.textMuted, fontSize = 13.sp)
                }
            } else {
                items(accepted + others) { patient ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(pal.surface)
                            .border(1.dp, pal.border, RoundedCornerShape(12.dp))
                            .clickable { onSelectPatient(patient) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👤", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(patient.nome, fontWeight = FontWeight.Bold, color = pal.text, fontSize = 14.sp)
                            Text(patient.email, color = pal.textMuted, fontSize = 12.sp)
                        }
                        Text("›", color = pal.textMuted, fontSize = 18.sp)
                    }
                }
            }
        }

        BottomNavDoctor(pal, "pacientes", onNav)
    }

    if (patientToReject != null) {
        AlertDialog(
            onDismissRequest = { patientToReject = null },
            title = { Text("Recusar Paciente") },
            text = { Text("Tem certeza que deseja recusar o paciente ${patientToReject?.nome}?", color = pal.danger) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val p = patientToReject!!
                        scope.launch {
                            withContext(Dispatchers.IO) { repository.updateVinculoStatus(p.id, "recusado") }
                            patientToReject = null
                        }
                    }
                ) { Text("Recusar", color = pal.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { patientToReject = null }) { Text("Manter paciente", color = pal.textMuted) }
            }
        )
    }
}

@Composable
fun DoctorPatientDetailScreen(
    pal: Palette,
    repository: MedGuiaRepository,
    patient: UsuarioEntity,
    onBack: () -> Unit,
    onAlarmSaved: (AlarmeEntity) -> Unit
) {
    var alarmes by remember { mutableStateOf<List<AlarmeEntity>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var remedioNome by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("08:00") }
    var icone by remember { mutableStateOf("💊") }
    var selectedColor by remember { mutableStateOf("#1E88E5") }
    var fotoUri by remember { mutableStateOf<String?>(null) }
    var nomeError by remember { mutableStateOf(false) }
    var duplicateError by remember { mutableStateOf<String?>(null) }

    val icons = listOf("🌸", "☀️", "⭐", "🌙", "💊", "❤️", "🔵", "🟣")
    val colors = listOf("#E53935", "#1E88E5", "#43A047", "#8E24AA", "#FB8C00", "#D81B60", "#00ACC1", "#6D4C41")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        fotoUri = uri?.toString()
    }

    LaunchedEffect(patient) {
        repository.getAlarmesForUser(patient.id).collect { list ->
            alarmes = list
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        HeaderRow(pal, patient.nome, onBack)
        Spacer(Modifier.height(10.dp))

        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = pal.accentDoc),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("+ Adicionar Alarme para o Paciente") }

        Spacer(Modifier.height(14.dp))
        Text("ALARMES CADASTRADOS", fontSize = 11.sp, color = pal.textMuted)
        Spacer(Modifier.height(8.dp))

        if (alarmes.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("⏰", fontSize = 38.sp)
                Spacer(Modifier.height(10.dp))
                Text("Nenhum alarme para este paciente.", color = pal.textMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alarmes) { alarme ->
                    val alarmColor = try { Color(android.graphics.Color.parseColor(alarme.cor)) } catch (_: Exception) { pal.accentDoc }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(pal.surface).border(1.dp, alarmColor, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alarme.icone, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alarme.nomeRemedio, fontWeight = FontWeight.Bold, color = pal.text, fontSize = 14.sp)
                            Text(alarme.horario, color = pal.textMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        val calendar = Calendar.getInstance()
        AlertDialog(
            onDismissRequest = { showDialog = false; duplicateError = null },
            title = { Text("Adicionar Alarme - ${patient.nome}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = remedioNome,
                        onValueChange = { remedioNome = it; nomeError = false },
                        label = { Text("Nome do Remédio") },
                        isError = nomeError,
                        supportingText = if (nomeError) { { Text("Campo obrigatório", color = pal.danger) } } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    horario = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Horário: $horario")
                    }

                    Text("Escolha o ícone:", fontSize = 13.sp, color = pal.textMuted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        icons.forEach { emoji ->
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (icone == emoji) pal.accentDocBg else pal.surface2)
                                    .clickable { icone = emoji },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 18.sp) }
                        }
                    }

                    Text("Escolha a cor:", fontSize = 13.sp, color = pal.textMuted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.forEach { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { pal.accentDoc }
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (selectedColor == hex) 2.dp else 0.dp, pal.text, CircleShape)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {}
                        }
                    }

                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (fotoUri == null) "📸 Adicionar Foto da Embalagem (Opcional)" else "📸 Foto Adicionada ✓")
                    }

                    if (duplicateError != null) {
                        Text(duplicateError!!, color = pal.danger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        nomeError = remedioNome.isBlank()
                        if (!nomeError) {
                            scope.launch {
                                val exists = withContext(Dispatchers.IO) {
                                    repository.checkAlarmExistsByIconAndColor(patient.id, icone, selectedColor)
                                }
                                if (exists) {
                                    duplicateError = "Esta combinação de ícone e cor já está em uso para outro alarme."
                                } else {
                                    val newAlarm = AlarmeEntity(
                                        usuarioId = patient.id,
                                        horario = horario,
                                        nomeRemedio = remedioNome,
                                        icone = icone,
                                        cor = selectedColor,
                                        fotoUri = fotoUri,
                                        ativo = true
                                    )
                                    val id = withContext(Dispatchers.IO) { repository.insertAlarme(newAlarm) }
                                    onAlarmSaved(newAlarm.copy(id = id))
                                    remedioNome = ""
                                    fotoUri = null
                                    showDialog = false
                                    duplicateError = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = pal.accentDoc)
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; duplicateError = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun StubScreen(pal: Palette, title: String, emoji: String, onBack: () -> Unit, isPatient: Boolean, active: String, onNav: (String) -> Unit, onLogout: (() -> Unit)?) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        HeaderRow(pal, title, onBack)
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 38.sp)
            Spacer(Modifier.height(10.dp))
            Text("Tela \"$title\" — funcionalidade em desenvolvimento.", color = pal.textMuted, textAlign = TextAlign.Center)
            if (onLogout != null) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onLogout) { Text("Sair", color = pal.text) }
            }
        }
        if (isPatient) BottomNavPatient(pal, active, onNav) else BottomNavDoctor(pal, active, onNav)
    }
}

@Composable
fun PatientLogin1Screen(pal: Palette, onBack: () -> Unit, onPlus: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        HeaderRow(pal, null, onBack)
        IconCircle(pal.accentPatBg, "👤")
        Spacer(Modifier.height(6.dp))
        Text("Login do paciente", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(130.dp).clip(CircleShape).border(1.5.dp, pal.textMuted, CircleShape).clickable(onClick = onPlus),
                contentAlignment = Alignment.Center
            ) { Text("+", fontSize = 44.sp, color = pal.textMuted) }
        }
        Spacer(Modifier.height(24.dp))
        Text("🔊", fontSize = 19.sp)
    }
}

@Composable
fun PatientLoginFormScreen(pal: Palette, repository: MedGuiaRepository, onBack: () -> Unit, onEnter: (UsuarioEntity) -> Unit, onSignup: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var senhaError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        HeaderRow(pal, "MedGuia", onBack)
        IconCircle(pal.accentPatBg, "👤")
        Spacer(Modifier.height(6.dp))
        Text("Login do paciente", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = false; errorMessage = null },
            label = { Text("✉️ Email") },
            isError = emailError,
            supportingText = if (emailError) { { Text("Campo obrigatório", color = pal.danger) } } else null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it; senhaError = false; errorMessage = null },
            label = { Text("🔒 Senha") },
            visualTransformation = PasswordVisualTransformation(),
            isError = senhaError,
            supportingText = if (senhaError) { { Text("Campo obrigatório", color = pal.danger) } } else null,
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(errorMessage!!, color = pal.danger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(10.dp))
        Text("🔊", fontSize = 19.sp)
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                emailError = email.isBlank()
                senhaError = senha.isBlank()
                if (!emailError && !senhaError) {
                    scope.launch {
                        val user = withContext(Dispatchers.IO) { repository.loginUsuario(email, senha) }
                        if (user != null && user.tipo == "paciente") {
                            onEnter(user)
                        } else {
                            errorMessage = "Email/senha incorretos"
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = pal.accentPat),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Entrar") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onSignup, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Ainda não tenho cadastro", color = pal.text) }
        Spacer(Modifier.height(12.dp))
        Text(
            "ℹ️ Importante: seu cadastro no MedGuia é criado e liberado diretamente pelo médico responsável, durante a sua consulta física.",
            fontSize = 11.sp, color = pal.textMuted,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(pal.surface2).padding(10.dp)
        )
    }
}

@Composable
fun PatientSignupScreen(pal: Palette, repository: MedGuiaRepository, onDone: (UsuarioEntity) -> Unit, onBack: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var vincular by remember { mutableStateOf(true) }
    var crm by remember { mutableStateOf("") }

    var nomeError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var senhaError by remember { mutableStateOf(false) }
    var confirmarError by remember { mutableStateOf(false) }
    var crmError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        HeaderRow(pal, null, onBack)
        IconCircle(pal.accentPatBg, "👤")
        Spacer(Modifier.height(6.dp))
        Text("Criar conta de paciente", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(nome, { nome = it; nomeError = false }, label = { Text("Nome completo") }, isError = nomeError, supportingText = if (nomeError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(email, { email = it; emailError = false }, label = { Text("Email") }, isError = emailError, supportingText = if (emailError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(senha, { senha = it; senhaError = false }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), isError = senhaError, supportingText = if (senhaError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(confirmar, { confirmar = it; confirmarError = false }, label = { Text("Confirmar senha") }, visualTransformation = PasswordVisualTransformation(), isError = confirmarError, supportingText = if (confirmarError) { { Text("Campo obrigatório", color = pal.danger) } } else null, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(pal.surface2).padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Vincular a um médico", fontWeight = FontWeight.SemiBold, color = pal.text, modifier = Modifier.weight(1f))
                Switch(checked = vincular, onCheckedChange = { vincular = it }, colors = SwitchDefaults.colors(checkedTrackColor = pal.accentPat))
            }
            Spacer(Modifier.height(6.dp))
            Text("Opcional. Sem vínculo, você mesmo configura seus remédios e alarmes.", fontSize = 12.sp, color = pal.textMuted)
            if (vincular) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = crm,
                    onValueChange = { crm = it; crmError = false },
                    label = { Text("CRM do médico") },
                    isError = crmError,
                    supportingText = if (crmError) { { Text("Campo obrigatório", color = pal.danger) } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(errorMessage!!, color = pal.danger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                nomeError = nome.isBlank()
                emailError = email.isBlank()
                senhaError = senha.isBlank()
                confirmarError = confirmar.isBlank()
                crmError = vincular && crm.isBlank()

                if (!nomeError && !emailError && !senhaError && !confirmarError && !crmError) {
                    if (senha != confirmar) {
                        errorMessage = "As senhas não coincidem"
                    } else {
                        scope.launch {
                            val doctor = if (vincular && crm.isNotBlank()) {
                                withContext(Dispatchers.IO) { repository.getMedicoByCrm(crm) }
                            } else null

                            val user = UsuarioEntity(
                                nome = nome,
                                email = email,
                                senha = senha,
                                tipo = "paciente",
                                crm = null,
                                especialidade = null,
                                medicoVinculadoId = doctor?.id,
                                vinculoStatus = if (doctor != null) "pendente" else "nenhum"
                            )
                            val id = withContext(Dispatchers.IO) { repository.insertUsuario(user) }
                            onDone(user.copy(id = id))
                        }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = pal.accentPat),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Criar conta") }
    }
}

@Composable
fun PatientLogin2Screen(pal: Palette, firstPatient: UsuarioEntity?, onBack: () -> Unit, onEnter: () -> Unit) {
    val patientName = firstPatient?.nome ?: "Luiza"
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        HeaderRow(pal, null, onBack)
        Text("Login do paciente", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = pal.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(36.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(130.dp).clip(CircleShape).background(pal.accentPatBg).border(2.dp, pal.accentPat, CircleShape).clickable(onClick = onEnter),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 56.sp) }
        }
        Spacer(Modifier.height(8.dp))
        Text(patientName, fontSize = 15.sp, color = pal.text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("🔊", fontSize = 19.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
fun MedCard(pal: Palette, alarme: AlarmeEntity, registros: List<RegistroDoseEntity>, onConfirm: (Long) -> Unit) {
    val alarmColor = try {
        Color(android.graphics.Color.parseColor(alarme.cor))
    } catch (_: Exception) {
        pal.accentPat
    }

    val reg = registros.firstOrNull { it.alarmeId == alarme.id }
    val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    val status = when {
        reg != null -> reg.status
        alarme.horario == currentTimeStr || alarme.horario < currentTimeStr -> "agora"
        else -> "pendente"
    }

    val bitmap = loadUriBitmap(alarme.fotoUri)

    when (status) {
        "tomado" -> Row(
            Modifier.fillMaxWidth().alpha(0.55f).clip(RoundedCornerShape(16.dp)).background(pal.surface).border(1.dp, alarmColor, RoundedCornerShape(16.dp)).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(10.dp))
            }
            Text(alarme.icone, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(alarme.horario, fontWeight = FontWeight.Bold, color = pal.text)
                Text(alarme.nomeRemedio, fontSize = 12.sp, color = pal.textMuted)
            }
            Text("✓ Tomado", fontSize = 12.sp, color = pal.success, fontWeight = FontWeight.Bold)
        }
        "perdido" -> Row(
            Modifier.fillMaxWidth().alpha(0.7f).clip(RoundedCornerShape(16.dp)).background(pal.warningBg).border(1.dp, pal.danger, RoundedCornerShape(16.dp)).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(10.dp))
            }
            Text(alarme.icone, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(alarme.horario, fontWeight = FontWeight.Bold, color = pal.text)
                Text(alarme.nomeRemedio, fontSize = 12.sp, color = pal.textMuted)
            }
            Text("✕ Perdido", fontSize = 12.sp, color = pal.danger, fontWeight = FontWeight.Bold)
        }
        "agora" -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(alarmColor.copy(alpha = 0.15f)).border(2.dp, alarmColor, RoundedCornerShape(16.dp)).clickable { onConfirm(alarme.id) }.padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(10.dp))
            }
            Text(alarme.icone, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(alarme.horario, fontWeight = FontWeight.Bold, color = pal.text)
                Text(alarme.nomeRemedio, fontSize = 12.sp, color = pal.textMuted)
                Text("AGORA — toque para confirmar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = alarmColor)
            }
        }
        else -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(pal.surface).border(1.dp, alarmColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)).padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(Modifier.width(10.dp))
            }
            Text(alarme.icone, fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(alarme.horario, fontWeight = FontWeight.Bold, color = pal.textMuted)
                Text(alarme.nomeRemedio, fontSize = 12.sp, color = pal.textMuted)
            }
        }
    }
}

@Composable
fun PatientHomeScreen(
    pal: Palette,
    user: UsuarioEntity?,
    alarmes: List<AlarmeEntity>,
    registros: List<RegistroDoseEntity>,
    notifRead: Boolean,
    onNotif: () -> Unit,
    onLogout: () -> Unit,
    onConfirm: (Long) -> Unit,
    onNav: (String) -> Unit
) {
    val patientName = user?.nome?.let { it.split(" ").first() } ?: "Maria"
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLogout) { Text("←", fontSize = 20.sp, color = pal.textSecondary) }
            Text("Bom dia, $patientName", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = pal.text, modifier = Modifier.weight(1f))
            Text("🔊", fontSize = 19.sp, modifier = Modifier.padding(end = 8.dp))
            IconButton(onClick = onNotif) {
                Box {
                    Text("🔔", fontSize = 19.sp)
                    if (!notifRead) {
                        Box(Modifier.size(8.dp).align(Alignment.TopEnd).clip(CircleShape).background(pal.danger))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (alarmes.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("💊", fontSize = 38.sp)
                Spacer(Modifier.height(10.dp))
                Text("Nenhum alarme cadastrado.\nVá na aba Alarmes para adicionar.", color = pal.textMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alarmes) { alarme ->
                    MedCard(pal, alarme, registros, onConfirm)
                }
            }
        }
        BottomNavPatient(pal, "remedios", onNav)
    }
}

@Composable
fun PatientAlarmsScreen(
    pal: Palette,
    repository: MedGuiaRepository,
    user: UsuarioEntity?,
    onBack: () -> Unit,
    onAlarmSaved: (AlarmeEntity) -> Unit,
    onNav: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var alarmes by remember { mutableStateOf<List<AlarmeEntity>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var alarmToDelete by remember { mutableStateOf<AlarmeEntity?>(null) }

    var remedioNome by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("08:00") }
    var icone by remember { mutableStateOf("💊") }
    var selectedColor by remember { mutableStateOf("#1E88E5") }
    var fotoUri by remember { mutableStateOf<String?>(null) }

    var nomeError by remember { mutableStateOf(false) }
    var duplicateError by remember { mutableStateOf<String?>(null) }

    val icons = listOf("🌸", "☀️", "⭐", "🌙", "💊", "❤️", "🔵", "🟣")
    val colors = listOf("#E53935", "#1E88E5", "#43A047", "#8E24AA", "#FB8C00", "#D81B60", "#00ACC1", "#6D4C41")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        fotoUri = uri?.toString()
    }

    LaunchedEffect(user) {
        user?.let { u ->
            repository.getAlarmesForUser(u.id).collect { list ->
                alarmes = list
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        HeaderRow(pal, "Alarmes e Remédios", onBack)
        Spacer(Modifier.height(10.dp))

        Button(
            onClick = { showDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = pal.accentPat),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("+ Adicionar Alarme") }

        Spacer(Modifier.height(14.dp))
        Text("MEUS ALARMES", fontSize = 11.sp, color = pal.textMuted)
        Spacer(Modifier.height(8.dp))

        if (alarmes.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("⏰", fontSize = 38.sp)
                Spacer(Modifier.height(10.dp))
                Text("Nenhum alarme configurado.", color = pal.textMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alarmes) { alarme ->
                    val alarmColor = try { Color(android.graphics.Color.parseColor(alarme.cor)) } catch (_: Exception) { pal.accentPat }
                    val bitmap = loadUriBitmap(alarme.fotoUri)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(pal.surface).border(1.dp, alarmColor, RoundedCornerShape(12.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (bitmap != null) {
                            Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(alarme.icone, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alarme.nomeRemedio, fontWeight = FontWeight.Bold, color = pal.text, fontSize = 14.sp)
                            Text(alarme.horario, color = pal.textMuted, fontSize = 12.sp)
                        }
                        IconButton(onClick = { alarmToDelete = alarme }) {
                            Text("✕", color = pal.danger, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        BottomNavPatient(pal, "alarmes", onNav)
    }

    if (alarmToDelete != null) {
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = { Text("Excluir Alarme") },
            text = { Text("Deseja realmente excluir o alarme de ${alarmToDelete?.nomeRemedio}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val alarm = alarmToDelete!!
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.deleteAlarme(alarm)
                            }
                            alarmToDelete = null
                        }
                    }
                ) { Text("Excluir", color = pal.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(
                    onClick = { alarmToDelete = null }
                ) { Text("Manter alarme", color = pal.textMuted) }
            }
        )
    }

    if (showDialog) {
        val calendar = Calendar.getInstance()
        AlertDialog(
            onDismissRequest = { showDialog = false; duplicateError = null },
            title = { Text("Novo Alarme") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = remedioNome,
                        onValueChange = { remedioNome = it; nomeError = false },
                        label = { Text("Nome do Remédio") },
                        isError = nomeError,
                        supportingText = if (nomeError) { { Text("Campo obrigatório", color = pal.danger) } } else null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    horario = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Horário: $horario")
                    }

                    Text("Escolha o ícone:", fontSize = 13.sp, color = pal.textMuted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        icons.forEach { emoji ->
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (icone == emoji) pal.accentPatBg else pal.surface2)
                                    .clickable { icone = emoji },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 18.sp) }
                        }
                    }

                    Text("Escolha a cor:", fontSize = 13.sp, color = pal.textMuted)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.forEach { hex ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { pal.accentPat }
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (selectedColor == hex) 2.dp else 0.dp, pal.text, CircleShape)
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {}
                        }
                    }

                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (fotoUri == null) "📸 Adicionar Foto da Embalagem (Opcional)" else "📸 Foto Adicionada ✓")
                    }

                    if (duplicateError != null) {
                        Text(duplicateError!!, color = pal.danger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        nomeError = remedioNome.isBlank()
                        if (!nomeError && user != null) {
                            scope.launch {
                                val exists = withContext(Dispatchers.IO) {
                                    repository.checkAlarmExistsByIconAndColor(user.id, icone, selectedColor)
                                }
                                if (exists) {
                                    duplicateError = "Esta combinação de ícone e cor já está em uso para outro alarme."
                                } else {
                                    val newAlarm = AlarmeEntity(
                                        usuarioId = user.id,
                                        horario = horario,
                                        nomeRemedio = remedioNome,
                                        icone = icone,
                                        cor = selectedColor,
                                        fotoUri = fotoUri,
                                        ativo = true
                                    )
                                    val id = withContext(Dispatchers.IO) { repository.insertAlarme(newAlarm) }
                                    onAlarmSaved(newAlarm.copy(id = id))
                                    remedioNome = ""
                                    fotoUri = null
                                    showDialog = false
                                    duplicateError = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = pal.accentPat)
                ) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false; duplicateError = null }) { Text("Cancelar") }
            }
        )
    }
}
