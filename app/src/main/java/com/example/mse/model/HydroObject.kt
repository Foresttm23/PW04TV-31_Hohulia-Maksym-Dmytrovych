package com.example.mse.model

enum class HydroObjectType(val title: String) {
    HPP("ГЕС"),
    PSPP("ГАЕС"),
    SMALL_HPP("Мала ГЕС")
}

enum class OperatingStatus(val title: String) {
    RUNNING("Працює"),
    REDUCED("Знижене навантаження"),
    MAINTENANCE("Ремонт"),
    OFFLINE("Не працює")
}

data class HydroObject(
    val id: String,
    val name: String,
    val type: HydroObjectType,
    val river: String,
    val city: String,
    val lat: Double,
    val lon: Double,
    val capacityMw: Double,
    val outputMw: Double,
    val status: OperatingStatus,
    val description: String,
    val updatedAt: Long
) {
    val loadPercent: Int get() = if (capacityMw > 0) (outputMw / capacityMw * 100).toInt() else 0
}

enum class ObjectSort(val title: String) {
    NAME("Назва"),
    TYPE("Тип"),
    POWER("Потужність"),
    UPDATED("Дата оновлення"),
    DISTANCE("Відстань")
}

data class ObjectQuery(
    val search: String = "",
    val types: Set<HydroObjectType> = emptySet(),
    val statuses: Set<OperatingStatus> = emptySet(),
    val minCapacityMw: Double = 0.0,
    val sort: ObjectSort = ObjectSort.NAME,
    val descending: Boolean = false
)

data class GeoPoint(val lat: Double, val lon: Double)

data class LevelReading(val levelM: Double, val fillPercent: Double, val status: LevelStatus)

data class CascadeSnapshot(
    val levelM: Double,
    val inflowM3s: Double,
    val outflowM3s: Double,
    val forecastLevelM: Double,
    val status: LevelStatus
) {
    val balanceM3s: Double get() = inflowM3s - outflowM3s
}

sealed interface SensorState {
    data object Stopped : SensorState
    data object Running : SensorState
    data class Retrying(val attempt: Int, val maxAttempts: Int, val delayMs: Long) : SensorState
    data class Failed(val message: String) : SensorState
}
