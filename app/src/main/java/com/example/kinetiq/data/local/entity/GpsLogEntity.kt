package com.example.kinetiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "gps_logs")
data class GpsLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val gpsOk: Boolean = false
)