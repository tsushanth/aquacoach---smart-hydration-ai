package com.factory.aquacoachsmarthydrationai.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.factory.aquacoachsmarthydrationai.data.local.AppDatabase
import com.factory.aquacoachsmarthydrationai.data.local.WaterEntry
import com.factory.aquacoachsmarthydrationai.data.local.WaterEntryDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class WaterEntryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WaterEntryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.waterEntryDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun entry(amountMl: Int, dayEpochDay: Long, timestampEpochMillis: Long = dayEpochDay * 1000) =
        WaterEntry(amountMl = amountMl, timestampEpochMillis = timestampEpochMillis, dayEpochDay = dayEpochDay)

    @Test
    fun `insert returns a generated id and the entry is queryable`() = runTest {
        val id = dao.insert(entry(amountMl = 250, dayEpochDay = 100))

        assertTrue(id > 0)
        dao.observeEntriesForDay(100).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeEntriesForDay orders results by timestamp descending`() = runTest {
        dao.insert(entry(amountMl = 100, dayEpochDay = 5, timestampEpochMillis = 1000))
        dao.insert(entry(amountMl = 200, dayEpochDay = 5, timestampEpochMillis = 3000))
        dao.insert(entry(amountMl = 150, dayEpochDay = 5, timestampEpochMillis = 2000))

        dao.observeEntriesForDay(5).test {
            val entries = awaitItem()
            assertEquals(listOf(3000L, 2000L, 1000L), entries.map { it.timestampEpochMillis })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeEntriesForDay only returns entries for the requested day`() = runTest {
        dao.insert(entry(amountMl = 100, dayEpochDay = 1))
        dao.insert(entry(amountMl = 200, dayEpochDay = 2))

        dao.observeEntriesForDay(1).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeTotalForDay sums all entries for that day and defaults to zero`() = runTest {
        dao.observeTotalForDay(9).test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        dao.insert(entry(amountMl = 300, dayEpochDay = 9))
        dao.insert(entry(amountMl = 400, dayEpochDay = 9))

        dao.observeTotalForDay(9).test {
            assertEquals(700, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeDailyTotals groups sums by day within the requested range`() = runTest {
        dao.insert(entry(amountMl = 100, dayEpochDay = 1))
        dao.insert(entry(amountMl = 150, dayEpochDay = 1))
        dao.insert(entry(amountMl = 200, dayEpochDay = 2))
        dao.insert(entry(amountMl = 999, dayEpochDay = 10)) // outside range

        dao.observeDailyTotals(1, 2).test {
            val totals = awaitItem()
            assertEquals(2, totals.size)
            assertEquals(250, totals.first { it.dayEpochDay == 1L }.totalMl)
            assertEquals(200, totals.first { it.dayEpochDay == 2L }.totalMl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes a specific entry`() = runTest {
        dao.insert(entry(amountMl = 100, dayEpochDay = 1, timestampEpochMillis = 1))
        val second = entry(amountMl = 200, dayEpochDay = 1, timestampEpochMillis = 2)
        dao.insert(second)

        dao.observeEntriesForDay(1).test {
            val inserted = awaitItem().first { it.timestampEpochMillis == 2L }
            dao.delete(inserted)
            val afterDelete = awaitItem()
            assertEquals(1, afterDelete.size)
            assertEquals(100, afterDelete.first().amountMl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteById removes only the targeted row`() = runTest {
        val idToDelete = dao.insert(entry(amountMl = 100, dayEpochDay = 1))
        dao.insert(entry(amountMl = 200, dayEpochDay = 1))

        dao.deleteById(idToDelete)

        dao.observeEntriesForDay(1).test {
            val remaining = awaitItem()
            assertEquals(1, remaining.size)
            assertEquals(200, remaining.first().amountMl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeAllEntries returns entries across all days`() = runTest {
        dao.insert(entry(amountMl = 100, dayEpochDay = 1))
        dao.insert(entry(amountMl = 200, dayEpochDay = 2))
        dao.insert(entry(amountMl = 300, dayEpochDay = 3))

        dao.observeAllEntries().test {
            assertEquals(3, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearAll removes every entry`() = runTest {
        dao.insert(entry(amountMl = 100, dayEpochDay = 1))
        dao.insert(entry(amountMl = 200, dayEpochDay = 2))

        dao.clearAll()

        dao.observeAllEntries().test {
            assertEquals(0, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
