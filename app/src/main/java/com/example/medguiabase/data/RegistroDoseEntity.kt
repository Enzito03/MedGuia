package com.example.medguiabase.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "registro_doses")
data class RegistroDoseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val usuarioId: Long,
    val alarmeId: Long,
    val status: String, // "tomado" ou "perdido"
    val timestamp: Long = System.currentTimeMillis()
)
