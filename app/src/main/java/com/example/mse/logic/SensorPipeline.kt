package com.example.mse.logic

import com.example.mse.model.CascadeSnapshot
import com.example.mse.model.LevelReading
import com.example.mse.model.LevelStatus
import com.example.mse.model.Reservoir
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.zip

class SensorFailureException(message: String) : Exception(message)

object SensorPipeline {
    val reservoir = Reservoir.KYIV

    // Поза цим діапазоном — збій датчика (обрив лінії дає 0).
    val plausibleLevels = 95.0..110.0

    fun isPlausible(levelM: Double) = !levelM.isNaN() && levelM in plausibleLevels

    fun levelReadings(raw: Flow<Double>): Flow<LevelReading> =
        raw.filter(::isPlausible)
            .map { LevelReading(it, reservoir.fillPercent(it), reservoir.statusOf(it)) }

    // Біля порога статус "миготить"; debounce віддає його, лише коли він не змінювався windowMs.
    fun stableStatus(readings: Flow<LevelReading>, windowMs: Long): Flow<LevelStatus> =
        readings.map { it.status }.distinctUntilChanged().debounce(windowMs)

    fun snapshots(
        level: Flow<Double>,
        inflow: Flow<Double>,
        outflow: Flow<Double>,
        forecast: Flow<Double>
    ): Flow<CascadeSnapshot> = combine(level, inflow, outflow, forecast) { l, i, o, f ->
        CascadeSnapshot(l, i, o, f, reservoir.statusOf(l))
    }

    fun pairedSamples(level: Flow<Double>, inflow: Flow<Double>): Flow<Pair<Double, Double>> =
        level.zip(inflow) { l, i -> l to i }

    fun <T> Flow<T>.retryWithBackoff(
        maxAttempts: Int,
        baseDelayMs: Long,
        onRetry: (attempt: Int, delayMs: Long) -> Unit
    ): Flow<T> = retryWhen { cause, attempt ->
        if (cause is SensorFailureException && attempt < maxAttempts) {
            val delayMs = baseDelayMs shl attempt.toInt()
            onRetry(attempt.toInt() + 1, delayMs)
            delay(delayMs)
            true
        } else false
    }
}
