package com.example.mse.ui.screens.task14

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.source.HydroObjectSource
import com.example.mse.logic.applyQuery
import com.example.mse.model.HydroObject
import com.example.mse.model.HydroObjectType
import com.example.mse.model.ObjectQuery
import com.example.mse.model.ObjectSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ObjectListUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val items: List<HydroObject> = emptyList(),
    val query: ObjectQuery = ObjectQuery(),
    val selectedId: String? = null,
    // Заповнюється лише кліком по маркеру: список має прокрутитись до картки. Клік по картці прокрутки не потребує.
    val listScrollTarget: String? = null
) {
    val visible: List<HydroObject> get() = items.applyQuery(query)
    val selected: HydroObject? get() = items.firstOrNull { it.id == selectedId }
}

class HydroObjectListViewModel(private val source: HydroObjectSource) : ViewModel() {
    private val _state = MutableStateFlow(ObjectListUiState())
    val uiState: StateFlow<ObjectListUiState> = _state.asStateFlow()

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

    fun select(id: String) = _state.update { it.copy(selectedId = id) }

    fun selectFromMap(id: String) = _state.update { it.copy(selectedId = id, listScrollTarget = id) }

    fun onScrolled() = _state.update { it.copy(listScrollTarget = null) }

    fun toggleType(type: HydroObjectType) = changeQuery { q ->
        q.copy(types = if (type in q.types) q.types - type else q.types + type)
    }

    fun setSort(sort: ObjectSort) = changeQuery { it.copy(sort = sort) }

    fun toggleDirection() = changeQuery { it.copy(descending = !it.descending) }

    fun resetFilters() = changeQuery { ObjectQuery() }

    private fun changeQuery(transform: (ObjectQuery) -> ObjectQuery) = _state.update { state ->
        val next = state.copy(query = transform(state.query))
        if (next.selectedId != null && next.visible.none { it.id == next.selectedId }) next.copy(selectedId = null) else next
    }
}
