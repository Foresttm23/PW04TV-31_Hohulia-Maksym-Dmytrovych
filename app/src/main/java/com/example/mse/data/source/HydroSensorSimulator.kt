package com.example.mse.data.source

import com.example.mse.logic.SensorFailureException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.sin
import kotlin.random.Random

// Усі методи повертають ХОЛОДНІ потоки: генерація стартує при collect і окремо для кожного колектора.
class HydroSensorSimulator(
    private val periodMs: Long = 800,
    private val seed: Int = 7,
    private val failureInjected: () -> Boolean = { false }
) {
    // Кожен 9-й відлік — "викид" (обрив лінії → 0.0), щоб було що відсіювати filter-ом.
    fun waterLevel(faultable: Boolean = false): Flow<Double> = flow {
        val random = Random(seed)
        var tick = 0
        while (true) {
            if (faultable && failureInjected()) throw SensorFailureException("Втрачено зв'язок з датчиком рівня")
            tick++
            val level = 102.2 + 0.9 * sin(tick / 7.0) + random.nextDouble(-0.05, 0.05)
            emit(if (tick % 9 == 0) 0.0 else level)
            delay(periodMs)
        }
    }

    fun inflow(): Flow<Double> = flow {
        val random = Random(seed + 1)
        var tick = 0
        while (true) {
            if (failureInjected()) throw SensorFailureException("Датчик припливу не відповідає")
            tick++
            emit(1150.0 + 260.0 * sin(tick / 5.0) + random.nextDouble(-15.0, 15.0))
            delay(periodMs)
        }
    }

    fun forecastLevel(): Flow<Double> = flow {
        var tick = 0
        while (true) {
            tick++
            emit(102.2 + 0.9 * sin((tick * 2 + 4) / 7.0))
            delay(periodMs * 2)
        }
    }
}
