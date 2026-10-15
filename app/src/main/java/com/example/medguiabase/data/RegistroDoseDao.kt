package com.example.medguiabase.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDoseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistroDose(registro: RegistroDoseEntity)

    @Query("SELECT * FROM registro_doses WHERE usuarioId = :usuarioId ORDER BY timestamp DESC")
    fun getRegistrosForUser(usuarioId: Long): Flow<List<RegistroDoseEntity>>
}
