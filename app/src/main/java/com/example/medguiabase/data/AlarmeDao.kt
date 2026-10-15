package com.example.medguiabase.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarme(alarme: AlarmeEntity): Long

    @Delete
    suspend fun deleteAlarme(alarme: AlarmeEntity)

    @Query("SELECT * FROM alarmes WHERE usuarioId = :usuarioId ORDER BY horario ASC")
    fun getAlarmesForUser(usuarioId: Long): Flow<List<AlarmeEntity>>

    @Query("SELECT * FROM alarmes WHERE usuarioId = :usuarioId ORDER BY horario ASC")
    suspend fun getAlarmesListForUser(usuarioId: Long): List<AlarmeEntity>

    @Query("SELECT * FROM alarmes WHERE usuarioId = :usuarioId AND icone = :icone AND cor = :cor LIMIT 1")
    suspend fun getAlarmByIconAndColor(usuarioId: Long, icone: String, cor: String): AlarmeEntity?
}
