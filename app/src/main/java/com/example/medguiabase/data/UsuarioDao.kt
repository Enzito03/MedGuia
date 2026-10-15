package com.example.medguiabase.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: UsuarioEntity): Long

    @Query("SELECT * FROM usuarios WHERE LOWER(TRIM(email)) = LOWER(TRIM(:emailOrCrm)) OR TRIM(crm) = TRIM(:emailOrCrm) LIMIT 1")
    suspend fun getUsuarioByEmailOrCrm(emailOrCrm: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun getUsuarioById(id: Long): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE tipo = 'paciente' LIMIT 1")
    suspend fun getFirstPatient(): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE TRIM(crm) = TRIM(:crm) AND tipo = 'medico' LIMIT 1")
    suspend fun getMedicoByCrm(crm: String): UsuarioEntity?

    @Query("SELECT * FROM usuarios WHERE medicoVinculadoId = :medicoId ORDER BY CASE WHEN vinculoStatus = 'pendente' THEN 0 ELSE 1 END, vinculoStatus ASC")
    fun getPatientsForDoctor(medicoId: Long): Flow<List<UsuarioEntity>>

    @Query("UPDATE usuarios SET vinculoStatus = :status WHERE id = :pacienteId")
    suspend fun updateVinculoStatus(pacienteId: Long, status: String)
}
