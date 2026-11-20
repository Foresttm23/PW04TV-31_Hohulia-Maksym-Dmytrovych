package com.example.mse.ui.screens.task15

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mse.data.source.HydroObjectSource
import com.example.mse.logic.applyQuery
import com.example.mse.model.HydroObject
import com.example.mse.model.HydroObjectType
import com.example.mse.model.ObjectQuery
import com.example.mse.model.ObjectSort
import com.example.mse.model.OperatingStatus
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<HydroObject> = emptyList(),
    val query: ObjectQuery = ObjectQuery(),
    val selectedIds: Set<String> = emptySet()
) {
    val visible: List<HydroObject> get() = items.applyQuery(query)
    val selectedObjects: List<HydroObject> get() = items.filter { it.id in selectedIds }
    val selectedCapacityMw: Double get() = selectedObjects.sumOf { it.capacityMw }
    val selectedOutputMw: Double get() = selectedObjects.sumOf { it.outputMw }
    val hiddenSelected: Int get() = selectedObjects.count { o -> visible.none { it.id == o.id } }
    val activeFilters: Int
        get() = listOf(query.search.isNotBlank(), query.types.isNotEmpty(), query.statuses.isNotEmpty(), query.minCapacityMw > 0)
            .count { it }
}

private const val SEARCH_DEBOUNCE_MS = 250L

@OptIn(FlowPreview::class)
class HydroObjectSearchViewModel(private val source: HydroObjectSource) : ViewModel() {
    // Текст у полі оновлюється одразу, а до списку доходить через debounce — фільтрація не смикається на кожну літеру.
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    private val filters = MutableStateFlow(ObjectQuery())
    private val items = MutableStateFlow<List<HydroObject>?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val selected = MutableStateFlow<Set<String>>(emptySet())

    private val debouncedSearch = _searchText.debounce { if (it.isEmpty()) 0L else SEARCH_DEBOUNCE_MS }.distinctUntilChanged()

    val uiState: StateFlow<SearchUiState> = combine(items, filters, debouncedSearch, selected, error) { list, f, search, sel, err ->
        SearchUiState(
            isLoading = list == null && err == null,
            error = err,
            items = list.orEmpty(),
            query = f.copy(search = search),
            selectedIds = sel
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SearchUiState())

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            error.value = null
            runCatching { source.load() }
                .onSuccess { items.value = it }
                .onFailure { error.value = it.message ?: "Не вдалося завантажити об'єкти" }
        }
    }

    fun setSearch(text: String) { _searchText.value = text }

    fun toggleType(type: HydroObjectType) = filters.update { it.copy(types = it.types.toggle(type)) }

    fun toggleStatus(status: OperatingStatus) = filters.update { it.copy(statuses = it.statuses.toggle(status)) }

    fun setMinCapacity(mw: Double) = filters.update { it.copy(minCapacityMw = mw) }

    fun setSort(sort: ObjectSort) = filters.update { it.copy(sort = sort) }

    fun toggleDirection() = filters.update { it.copy(descending = !it.descending) }

    fun resetFilters() {
        _searchText.value = ""
        filters.value = ObjectQuery()
    }

    fun toggleSelected(id: String) = selected.update { it.toggle(id) }

    fun selectAllVisible() = selected.update { it + uiState.value.visible.map { o -> o.id } }

    fun clearSelection() { selected.value = emptySet() }

    private fun <T> Set<T>.toggle(value: T) = if (value in this) this - value else this + value
}
