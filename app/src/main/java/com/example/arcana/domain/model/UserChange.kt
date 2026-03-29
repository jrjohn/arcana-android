package com.example.arcana.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserChange(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Int,
    val type: ChangeType,
    val name: String? = null,
    val job: String? = null
)

enum class ChangeType {
    CREATE, UPDATE, DELETE
}
