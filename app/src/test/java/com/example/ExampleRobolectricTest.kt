package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccessCodeEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CampaignEntity
import com.example.data.local.PrizeEntity
import com.example.data.repository.CodeValidationResult
import com.example.data.repository.RoletaRepository
import com.example.data.repository.SpinExecutionResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: RoletaRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoletaRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Roleta da Sorte", appName)
    }

    @Test
    fun `test access code validation flow and single spin consumption`() = runBlocking {
        // Seed initial data
        repository.seedInitialDataIfEmpty()

        // 1. Valid code unlocks wheel
        val validResult = repository.validateAccessCode("ROULET-8K42P")
        assertTrue("Expected valid result", validResult is CodeValidationResult.Valid)

        // 2. Invalid code returns Invalid
        val invalidResult = repository.validateAccessCode("CODE-DOES-NOT-EXIST")
        assertTrue("Expected invalid result", invalidResult is CodeValidationResult.Invalid)

        // 3. Pre-used code returns AlreadyUsed
        val usedResult = repository.validateAccessCode("ROULET-USED01")
        assertTrue("Expected already used", usedResult is CodeValidationResult.AlreadyUsed)

        // 4. Spin wheel with valid code
        val spinResult = repository.executeSpinAtomic("ROULET-8K42P")
        assertTrue("Expected spin success", spinResult is SpinExecutionResult.Success)
        val success = spinResult as SpinExecutionResult.Success
        assertNotNull(success.prize)

        // 5. Code is now used and cannot be spun or validated again
        val revalidation = repository.validateAccessCode("ROULET-8K42P")
        assertTrue("Expected already used after spin", revalidation is CodeValidationResult.AlreadyUsed)

        val secondSpin = repository.executeSpinAtomic("ROULET-8K42P")
        assertTrue("Expected second spin error", secondSpin is SpinExecutionResult.Error)
    }
}
