package com.example.mse.ui.screens.task15

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mse.data.source.MockHydroObjectSource
import com.example.mse.model.HydroObject
import com.example.mse.model.ObjectSort
import com.example.mse.ui.components.EmptyState
import com.example.mse.ui.components.HydroObjectCard
import com.example.mse.ui.components.SortMenu
import com.example.mse.ui.components.StatusFilterChips
import com.example.mse.ui.components.TypeFilterChips
import com.example.mse.ui.components.formatUpdated

@Composable
fun HydroObjectSearchScreen(
    viewModel: HydroObjectSearchViewModel = viewModel(
        factory = viewModelFactory { initializer { HydroObjectSearchViewModel(MockHydroObjectSource()) } }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    var details by remember { mutableStateOf<HydroObject?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        val filters: @Composable () -> Unit = { FilterPanel(state, searchText, viewModel) }
        val results: @Composable (Modifier, Boolean) -> Unit = { m, narrow -> Results(state, viewModel, m, narrow, onDetails = { details = it }) }

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(12.dp)) { filters() }
                results(Modifier.weight(1f).fillMaxHeight(), false)
            }
        } else {
            results(Modifier.fillMaxSize(), true)
        }
    }

    details?.let { item ->
        AlertDialog(
            onDismissRequest = { details = null },
            confirmButton = { TextButton(onClick = { details = null }) { Text("Закрити") } },
            title = { Text(item.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.description)
                    Text("Річка: ${item.river}, ${item.city}")
                    Text("Координати: %.3f, %.3f".format(item.lat, item.lon))
                    Text("Встановлена потужність: ${item.capacityMw} МВт")
                    Text("Поточна потужність: ${item.outputMw} МВт")
                    Text("Статус: ${item.status.title}")
                    Text("Оновлено: ${formatUpdated(item.updatedAt)}")
                }
            }
        )
    }
}

@Composable
private fun FilterPanel(state: SearchUiState, searchText: String, vm: HydroObjectSearchViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchText,
            onValueChange = vm::setSearch,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Пошук за назвою або містом") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchText.isNotEmpty()) IconButton(onClick = { vm.setSearch("") }) { Icon(Icons.Default.Clear, "Очистити") }
            }
        )
        Text("Тип об'єкта", style = MaterialTheme.typography.labelLarge)
        TypeFilterChips(state.query.types, vm::toggleType)
        Text("Статус роботи", style = MaterialTheme.typography.labelLarge)
        StatusFilterChips(state.query.statuses, vm::toggleStatus)
        Text("Потужність від ${state.query.minCapacityMw.toInt()} МВт", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = state.query.minCapacityMw.toFloat(),
            onValueChange = { vm.setMinCapacity((it / 50).toInt() * 50.0) },
            valueRange = 0f..1000f
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            SortMenu(
                options = listOf(ObjectSort.NAME, ObjectSort.TYPE, ObjectSort.POWER, ObjectSort.UPDATED),
                selected = state.query.sort, descending = state.query.descending,
                onSelect = vm::setSort, onToggleDirection = vm::toggleDirection
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Знайдено ${state.visible.size} з ${state.items.size}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            if (state.activeFilters > 0) TextButton(onClick = vm::resetFilters) { Text("Скинути (${state.activeFilters})") }
        }
    }
}

@Composable
private fun Results(state: SearchUiState, vm: HydroObjectSearchViewModel, modifier: Modifier, narrow: Boolean, onDetails: (HydroObject) -> Unit) {
    Box(modifier) {
        val searchText by vm.searchText.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        LaunchedEffect(state.query) { listState.scrollToItem(0) }
        Column(Modifier.fillMaxSize()) {
            if (state.isLoading) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (narrow) item { FilterPanel(state, searchText, vm) }
                    when {
                        state.error != null -> item { EmptyState(state.error, actionLabel = "Повторити", onAction = vm::load) }
                        state.items.isEmpty() -> item { EmptyState("Дані про енергетичні об'єкти відсутні") }
                        state.visible.isEmpty() -> item {
                            EmptyState("За вибраними фільтрами нічого не знайдено", actionLabel = "Скинути фільтри", onAction = vm::resetFilters)
                        }
                        else -> items(state.visible, key = { it.id }) { item ->
                            HydroObjectCard(
                                item = item,
                                checked = item.id in state.selectedIds,
                                selected = item.id in state.selectedIds,
                                onCheckedChange = { vm.toggleSelected(item.id) },
                                onClick = { vm.toggleSelected(item.id) },
                                trailing = { TextButton(onClick = { onDetails(item) }) { Text("Детальніше") } }
                            )
                        }
                    }
                }
            }
            if (state.selectedIds.isNotEmpty()) SelectionBar(state, vm)
        }
    }
}

@Composable
private fun SelectionBar(state: SearchUiState, vm: HydroObjectSearchViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Вибрано: ${state.selectedIds.size}", fontWeight = FontWeight.Bold)
                Text(
                    "Σ ${state.selectedOutputMw.toInt()} / ${state.selectedCapacityMw.toInt()} МВт" +
                        if (state.hiddenSelected > 0) " · ${state.hiddenSelected} приховано фільтрами" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = vm::selectAllVisible) { Text("Усі знайдені") }
            TextButton(onClick = vm::clearSelection) { Text("Скинути") }
        }
    }
}
