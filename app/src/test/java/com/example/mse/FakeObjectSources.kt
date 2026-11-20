package com.example.mse

import com.example.mse.data.source.HydroObjectSource
import com.example.mse.model.HydroObject
import com.example.mse.model.HydroObjectType
import com.example.mse.model.OperatingStatus

fun testObject(
    id: String,
    name: String = id,
    type: HydroObjectType = HydroObjectType.HPP,
    capacityMw: Double = 100.0,
    status: OperatingStatus = OperatingStatus.RUNNING,
    lat: Double = 50.0,
    lon: Double = 30.0,
    updatedAt: Long = 0
) = HydroObject(id, name, type, "Дніпро", "Тест", lat, lon, capacityMw, capacityMw / 2, status, "опис", updatedAt)

class FakeObjectSource(private val items: List<HydroObject>) : HydroObjectSource {
    override suspend fun load() = items
}
