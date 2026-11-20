package com.example.mse.ui.screens.task16

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.source.HydroObjectSource
import com.example.mse.data.source.LocationSource
import com.example.mse.logic.GeoMath
import com.example.mse.logic.applyQuery
import com.example.mse.model.GeoPoint
import com.example.mse.model.HydroObject
import com.example.mse.model.HydroObjectType
import com.example.mse.model.ObjectQuery
import com.example.mse.model.ObjectSort
import com.example.mse.model.OperatingStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LocationPermission { UNKNOWN, GRANTED, DENIED }

data class MapUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<HydroObject> = emptyList(),
    val query: ObjectQuery = ObjectQuery(),
    val selectedId: String? = null,
    val listScrollTarget: String? = null,
    val permission: LocationPermission = LocationPermission.UNKNOWN,
    val userPosition: GeoPoint? = null,
    val locationError: String? = null,
    val centerOnUserTick: Int = 0
) {
    val visible: List<HydroObject> get() = items.applyQuery(query, userPosition)

    fun distanceKm(item: HydroObject): Double? =
        userPosition?.let { GeoMath.distanceKm(it, GeoPoint(item.lat, item.lon)) }

    val nearest: HydroObject? get() = items.minByOrNull { distanceKm(it) ?: Double.MAX_VALUE }.takeIf { userPosition != null }
}

class HydroMapViewModel(
    private val source: HydroObjectSource,
    private val location: LocationSource
) : ViewModel() {
    private val _state = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _state.asStateFlow()

    private var locationJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { source.load() }
                .onSuccess { list -> _state.update { it.copy(isLoading = false, items = list) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Не вдалося завантажити об'єкти") } }
        }
    }

    // Єдина точка входу від UI після діалогу дозволу: без дозволу LocationManager навіть не викликається.
    fun onPermissionResult(granted: Boolean) {
        if (!granted) {
            locationJob?.cancel()
            _state.update { it.copy(permission = LocationPermission.DENIED, userPosition = null, locationError = null) }
            return
        }
        _state.update { it.copy(permission = LocationPermission.GRANTED, locationError = null) }
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            location.updates()
                .catch { e -> _state.update { it.copy(locationError = e.message ?: "Помилка геолокації") } }
                .collect { p -> _state.update { it.copy(userPosition = p) } }
        }
    }

    fun centerOnUser() = _state.update { it.copy(centerOnUserTick = it.centerOnUserTick + 1) }

    fun select(id: String) = _state.update { it.copy(selectedId = id) }

    fun selectFromMap(id: String) = _state.update { it.copy(selectedId = id, listScrollTarget = id) }

    fun onScrolled() = _state.update { it.copy(listScrollTarget = null) }

    fun toggleType(type: HydroObjectType) = changeQuery { q ->
        q.copy(types = if (type in q.types) q.types - type else q.types + type)
    }

    fun toggleStatus(status: OperatingStatus) = changeQuery { q ->
        q.copy(statuses = if (status in q.statuses) q.statuses - status else q.statuses + status)
    }

    fun setMinCapacity(mw: Double) = changeQuery { it.copy(minCapacityMw = mw) }

    fun setSort(sort: ObjectSort) = changeQuery { it.copy(sort = sort) }

    fun toggleDirection() = changeQuery { it.copy(descending = !it.descending) }

    fun resetFilters() = changeQuery { ObjectQuery() }

    private fun changeQuery(transform: (ObjectQuery) -> ObjectQuery) = _state.update { state ->
        val next = state.copy(query = transform(state.query))
        if (next.selectedId != null && next.visible.none { it.id == next.selectedId }) next.copy(selectedId = null) else next
    }
}
