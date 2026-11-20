package com.example.mse.logic

import com.example.mse.model.GeoPoint
import com.example.mse.model.HydroObject
import com.example.mse.model.ObjectQuery
import com.example.mse.model.ObjectSort
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object GeoMath {
    private const val EARTH_RADIUS_KM = 6371.0

    fun distanceKm(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h))
    }
}

// Фільтри працюють за принципом "І": об'єкт має пройти кожен активний рівень.
fun List<HydroObject>.applyQuery(query: ObjectQuery, userPosition: GeoPoint? = null): List<HydroObject> {
    val needle = query.search.trim().lowercase()
    val filtered = filter { o ->
        (needle.isEmpty() || o.name.lowercase().contains(needle) || o.city.lowercase().contains(needle)) &&
            (query.types.isEmpty() || o.type in query.types) &&
            (query.statuses.isEmpty() || o.status in query.statuses) &&
            o.capacityMw >= query.minCapacityMw
    }
    val ascending = when (query.sort) {
        ObjectSort.NAME -> filtered.sortedBy { it.name }
        ObjectSort.TYPE -> filtered.sortedWith(compareBy({ it.type.ordinal }, { it.name }))
        ObjectSort.POWER -> filtered.sortedBy { it.capacityMw }
        ObjectSort.UPDATED -> filtered.sortedBy { it.updatedAt }
        ObjectSort.DISTANCE ->
            if (userPosition == null) filtered.sortedBy { it.name }
            else filtered.sortedBy { GeoMath.distanceKm(userPosition, GeoPoint(it.lat, it.lon)) }
    }
    return if (query.descending) ascending.reversed() else ascending
}
