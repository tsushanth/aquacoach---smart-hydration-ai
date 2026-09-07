package com.factory.aquacoachsmarthydrationai.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DailyTotal(
    val dayEpochDay: Long,
    val totalMl: Int
)

@Dao
interface WaterEntryDao {

    @Insert
    suspend fun insert(entry: WaterEntry): Long

    @Delete
    suspend fun delete(entry: WaterEntry)

    @Query("DELETE FROM water_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    @Query("SELECT * FROM water_entries WHERE dayEpochDay = :dayEpochDay ORDER BY timestampEpochMillis DESC")
    fun observeEntriesForDay(dayEpochDay: Long): Flow<List<WaterEntry>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_entries WHERE dayEpochDay = :dayEpochDay")
    fun observeTotalForDay(dayEpochDay: Long): Flow<Int>

    @Query(
        "SELECT dayEpochDay, SUM(amountMl) as totalMl FROM water_entries " +
            "WHERE dayEpochDay BETWEEN :startDayEpochDay AND :endDayEpochDay " +
            "GROUP BY dayEpochDay ORDER BY dayEpochDay ASC"
    )
    fun observeDailyTotals(startDayEpochDay: Long, endDayEpochDay: Long): Flow<List<DailyTotal>>

    @Query("SELECT * FROM water_entries ORDER BY timestampEpochMillis DESC")
    fun observeAllEntries(): Flow<List<WaterEntry>>

    @Query("DELETE FROM water_entries")
    suspend fun clearAll()
}
