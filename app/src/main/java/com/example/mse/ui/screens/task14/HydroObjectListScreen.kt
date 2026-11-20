package com.example.mse.ui.screens.task14

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mse.data.source.MockHydroObjectSource
import com.example.mse.model.HydroObject
import com.example.mse.model.ObjectSort
import com.example.mse.ui.components.EmptyState
import com.example.mse.ui.components.HydroMapView
import com.example.mse.ui.components.HydroObjectCard
import com.example.mse.ui.components.SortMenu
import com.example.mse.ui.components.TypeFilterChips
import com.example.mse.ui.components.formatUpdated

private const val LIST = "list"
private const val DETAIL = "detail/{id}"

@Composable
fun HydroObjectListScreen(
    viewModel: HydroObjectListViewModel = viewModel(
        factory = viewModelFactory { initializer { HydroObjectListViewModel(MockHydroObjectSource()) } }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    NavHost(nav, startDestination = LIST) {
        composable(LIST) {
            ListWithMap(state, viewModel, onOpenDetails = { nav.navigate("detail/$it") })
        }
        composable(DETAIL) { entry ->
            val item = state.items.firstOrNull { it.id == entry.arguments?.getString("id") }
            DetailScreen(item, nav, onShowOnMap = { viewModel.select(it.id); nav.popBackStack() })
        }
    }
}

@Composable
private fun ListWithMap(state: ObjectListUiState, vm: HydroObjectListViewModel, onOpenDetails: (String) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val wide = totalWidth >= 720.dp
        val visible = state.visible

        val map: @Composable (Modifier) -> Unit = { modifier ->
            HydroMapView(visible, state.selectedId, userPosition = null, onMarkerClick = vm::selectFromMap, modifier = modifier)
        }
        val list: @Composable (Modifier) -> Unit = { modifier ->
            ObjectList(state, vm, onOpenDetails, modifier)
        }

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                list(Modifier.width(totalWidth * 0.45f).fillMaxHeight())
                map(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                map(Modifier.fillMaxWidth().height(totalHeight * 0.36f))
                list(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun ObjectList(state: ObjectListUiState, vm: HydroObjectListViewModel, onOpenDetails: (String) -> Unit, modifier: Modifier) {
    val listState = rememberLazyListState()
    val visible = state.visible

    // Зміна фільтра/сортування: LazyColumn тримається за ключ першого елемента, тож повертаємось угору.
    LaunchedEffect(state.query) { listState.scrollToItem(0) }

    LaunchedEffect(state.listScrollTarget) {
        val target = state.listScrollTarget ?: return@LaunchedEffect
        val index = visible.indexOfFirst { it.id == target }
        if (index >= 0) listState.animateScrollToItem(index)
        vm.onScrolled()
    }

    Column(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            TypeFilterChips(state.query.types, vm::toggleType)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SortMenu(
                    options = listOf(ObjectSort.NAME, ObjectSort.TYPE, ObjectSort.POWER, ObjectSort.UPDATED),
                    selected = state.query.sort, descending = state.query.descending,
                    onSelect = vm::setSort, onToggleDirection = vm::toggleDirection
                )
                Text("${visible.size} з ${state.items.size}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            state.isLoading -> CenterBox { CircularProgressIndicator() }
            state.error != null -> CenterBox {
                EmptyState(state.error, actionLabel = "Повторити", onAction = vm::load)
            }
            state.items.isEmpty() -> CenterBox { EmptyState("Дані про енергетичні об'єкти відсутні") }
            visible.isEmpty() -> CenterBox {
                EmptyState("За вибраними фільтрами нічого не знайдено", actionLabel = "Скинути фільтри", onAction = vm::resetFilters)
            }
            else -> LazyColumn(
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(visible, key = { _, o -> o.id }) { _, item ->
                    HydroObjectCard(
                        item = item,
                        selected = item.id == state.selectedId,
                        onClick = { vm.select(item.id) },
                        trailing = {
                            TextButton(onClick = { onOpenDetails(item.id) }) { Text("Детальніше") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DetailScreen(item: HydroObject?, nav: NavHostController, onShowOnMap: (HydroObject) -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(item?.name ?: "Об'єкт") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            EmptyState("Об'єкт не знайдено", Modifier.padding(padding))
            return@Scaffold
        }
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val wide = maxWidth >= 720.dp
            val facts = listOf(
                "Тип" to item.type.title,
                "Річка" to item.river,
                "Населений пункт" to item.city,
                "Координати" to "%.3f, %.3f".format(item.lat, item.lon),
                "Встановлена потужність" to "${item.capacityMw} МВт",
                "Поточна потужність" to "${item.outputMw} МВт (${item.loadPercent} %)",
                "Статус" to item.status.title,
                "Оновлено" to formatUpdated(item.updatedAt)
            )
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(item.description, style = MaterialTheme.typography.bodyLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (wide) 4 else 2
                ) {
                    facts.forEach { (k, v) ->
                        Column(Modifier.width(if (wide) 200.dp else 150.dp)) {
                            Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(v, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                OutlinedButton(onClick = { onShowOnMap(item) }) { Text("Показати на мапі") }
            }
        }
    }
}
