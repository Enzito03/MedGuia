package com.example.medguiabase.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarmes")
data class AlarmeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val usuarioId: Long,
    val horario: String, // "HH:mm"
    val nomeRemedio: String,
    val icone: String,
    val cor: String = "#2F7DD1",
    val fotoUri: String? = null,
    val ativo: Boolean = true
)
