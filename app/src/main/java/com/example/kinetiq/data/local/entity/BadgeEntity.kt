package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val title: String,
    val description: String,
    val iconUrl: String? = null,
    val triggerRule: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)