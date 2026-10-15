package com.example.medguiabase.data

import kotlinx.coroutines.flow.Flow

class MedGuiaRepository(private val database: AppDatabase) {
    private val usuarioDao = database.usuarioDao()
    private val alarmeDao = database.alarmeDao()
    private val registroDoseDao = database.registroDoseDao()

    suspend fun insertUsuario(usuario: UsuarioEntity): Long = usuarioDao.insertUsuario(usuario)
    suspend fun loginUsuario(emailOrCrm: String, senha: String): UsuarioEntity? {
        val user = usuarioDao.getUsuarioByEmailOrCrm(emailOrCrm)
        if (user != null && user.senha == senha) return user
        return null
    }
    suspend fun getFirstPatient(): UsuarioEntity? = usuarioDao.getFirstPatient()
    suspend fun getMedicoByCrm(crm: String): UsuarioEntity? = usuarioDao.getMedicoByCrm(crm)
    fun getPatientsForDoctor(medicoId: Long): Flow<List<UsuarioEntity>> = usuarioDao.getPatientsForDoctor(medicoId)
    suspend fun updateVinculoStatus(pacienteId: Long, status: String) = usuarioDao.updateVinculoStatus(pacienteId, status)

    suspend fun insertAlarme(alarme: AlarmeEntity): Long = alarmeDao.insertAlarme(alarme)
    suspend fun deleteAlarme(alarme: AlarmeEntity) = alarmeDao.deleteAlarme(alarme)
    fun getAlarmesForUser(usuarioId: Long): Flow<List<AlarmeEntity>> = alarmeDao.getAlarmesForUser(usuarioId)
    suspend fun getAlarmesListForUser(usuarioId: Long): List<AlarmeEntity> = alarmeDao.getAlarmesListForUser(usuarioId)
    suspend fun checkAlarmExistsByIconAndColor(usuarioId: Long, icone: String, cor: String): Boolean {
        return alarmeDao.getAlarmByIconAndColor(usuarioId, icone, cor) != null
    }

    suspend fun insertRegistroDose(registro: RegistroDoseEntity) = registroDoseDao.insertRegistroDose(registro)
    fun getRegistrosForUser(usuarioId: Long): Flow<List<RegistroDoseEntity>> = registroDoseDao.getRegistrosForUser(usuarioId)
}
