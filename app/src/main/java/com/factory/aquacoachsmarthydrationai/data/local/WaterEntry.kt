package com.factory.aquacoachsmarthydrationai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val timestampEpochMillis: Long,
    val dayEpochDay: Long
)
