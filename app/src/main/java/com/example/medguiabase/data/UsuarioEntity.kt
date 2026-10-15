package com.example.medguiabase.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nome: String,
    val email: String,
    val senha: String,
    val tipo: String, // "medico" ou "paciente"
    val crm: String?,
    val especialidade: String?,
    val medicoVinculadoId: Long?,
    val vinculoStatus: String = "nenhum" // "nenhum", "pendente", "aceito", "recusado"
)
