package com.example.mse

import com.example.mse.logic.SensorFailureException
import com.example.mse.logic.SensorPipeline
import com.example.mse.logic.SensorPipeline.retryWithBackoff
import com.example.mse.model.CascadeSnapshot
import com.example.mse.model.LevelStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorStreamsTest {

    @Test
    fun `combine склеює чотири потоки і перераховує баланс та статус при кожній зміні`() = runTest(UnconfinedTestDispatcher()) {
        val level = MutableSharedFlow<Double>()
        val inflow = MutableSharedFlow<Double>()
        val outflow = MutableSharedFlow<Double>()
        val forecast = MutableSharedFlow<Double>()
        val snapshots = mutableListOf<CascadeSnapshot>()
        SensorPipeline.snapshots(level, inflow, outflow, forecast).onEach(snapshots::add).launchIn(backgroundScope)

        level.emit(102.5); inflow.emit(1300.0); outflow.emit(1000.0)
        assertEquals("без прогнозу снапшота ще немає", 0, snapshots.size)

        forecast.emit(102.8)
        assertEquals(300.0, snapshots.single().balanceM3s, 0.001)

        outflow.emit(1500.0)
        assertEquals(-200.0, snapshots.last().balanceM3s, 0.001)

        level.emit(103.4)
        assertEquals(LevelStatus.WARNING, snapshots.last().status)
        assertEquals(1500.0, snapshots.last().outflowM3s, 0.001)
    }

    @Test
    fun `retryWithBackoff відновлює потік після збоїв з подвоєнням затримки`() = runTest {
        var attempts = 0
        val flaky = flow {
            if (attempts++ < 2) throw SensorFailureException("немає зв'язку")
            emit(42.0)
        }
        val retries = mutableListOf<Pair<Int, Long>>()

        val result = flaky.retryWithBackoff(maxAttempts = 4, baseDelayMs = 1000) { n, d -> retries += n to d }.toList()

        assertEquals(listOf(42.0), result)
        assertEquals(listOf(1 to 1000L, 2 to 2000L), retries)
        assertEquals("1000 + 2000 мс віртуального часу", 3000L, testScheduler.currentTime)
    }
}
