package com.example.mse.ui.screens.task16

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mse.data.source.AndroidLocationSource
import com.example.mse.data.source.MockHydroObjectSource
import com.example.mse.model.ObjectSort
import com.example.mse.ui.components.EmptyState
import com.example.mse.ui.components.HydroMapView
import com.example.mse.ui.components.HydroObjectCard
import com.example.mse.ui.components.SortMenu
import com.example.mse.ui.components.StatusFilterChips
import com.example.mse.ui.components.TypeFilterChips
import com.example.mse.ui.components.formatDistance

@Composable
fun HydroMapScreen() {
    val context = LocalContext.current
    val viewModel: HydroMapViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HydroMapViewModel(MockHydroObjectSource(), AndroidLocationSource(context.applicationContext)) }
        }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var filtersOpen by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        // Достатньо будь-якого з двох: користувач може дати лише приблизну геолокацію.
        viewModel.onPermissionResult(result.values.any { it })
    }
    val requestPermission = {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    LaunchedEffect(Unit) {
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (granted) viewModel.onPermissionResult(true) else requestPermission()
    }

    val visible = state.visible

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight
        val wide = totalWidth >= 720.dp

        val map: @Composable (Modifier) -> Unit = { modifier ->
            Box(modifier) {
                HydroMapView(
                    objects = visible, selectedId = state.selectedId, userPosition = state.userPosition,
                    onMarkerClick = viewModel::selectFromMap, centerOnUserTick = state.centerOnUserTick,
                    modifier = Modifier.fillMaxSize()
                )
                FilledTonalIconButton(
                    onClick = viewModel::centerOnUser,
                    enabled = state.userPosition != null,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) { Icon(Icons.Default.MyLocation, "Моя позиція") }
            }
        }

        val list: @Composable (Modifier) -> Unit = { modifier ->
            Column(modifier) {
                LocationBanner(state, onRequest = requestPermission)
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { filtersOpen = !filtersOpen },
                        label = { Text("Фільтри") },
                        leadingIcon = { Icon(Icons.Default.FilterList, null) }
                    )
                    SortMenu(
                        options = ObjectSort.entries.filter { it != ObjectSort.DISTANCE || state.userPosition != null },
                        selected = state.query.sort, descending = state.query.descending,
                        onSelect = viewModel::setSort, onToggleDirection = viewModel::toggleDirection
                    )
                }
                if (filtersOpen) {
                    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TypeFilterChips(state.query.types, viewModel::toggleType)
                        StatusFilterChips(state.query.statuses, viewModel::toggleStatus)
                        Text("Потужність від ${state.query.minCapacityMw.toInt()} МВт", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = state.query.minCapacityMw.toFloat(),
                            onValueChange = { viewModel.setMinCapacity((it / 50).toInt() * 50.0) },
                            valueRange = 0f..1000f
                        )
                    }
                }
                Text(
                    "Показано ${visible.size} з ${state.items.size}",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ObjectList(state, viewModel, Modifier.weight(1f).fillMaxWidth())
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                list(Modifier.width(totalWidth * 0.42f).fillMaxHeight())
                map(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                map(Modifier.fillMaxWidth().height(totalHeight * 0.38f))
                list(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun LocationBanner(state: MapUiState, onRequest: () -> Unit) {
    val context = LocalContext.current
    val (text, action) = when {
        state.permission == LocationPermission.DENIED -> "Дозвіл на геолокацію не надано — відстані недоступні." to "Дозволити"
        state.locationError != null -> state.locationError to null
        state.permission == LocationPermission.GRANTED && state.userPosition == null -> "Визначаємо позицію…" to null
        state.userPosition != null -> state.nearest?.let {
            "Найближчий об'єкт: ${it.name}, ${formatDistance(state.distanceKm(it) ?: 0.0)}"
        } to null
        else -> null to null
    }
    if (text == null) return
    val isProblem = state.permission == LocationPermission.DENIED || state.locationError != null
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isProblem) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isProblem) Icon(Icons.Default.LocationOff, null, Modifier.padding(end = 8.dp))
            Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            if (action != null) {
                TextButton(onClick = {
                    // Після двох відмов система більше не показує діалог — відправляємо в налаштування застосунку.
                    val activity = context as? android.app.Activity
                    val canAsk = activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ?: true
                    if (canAsk) onRequest() else context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    )
                }) { Text(action) }
            }
        }
    }
}

@Composable
private fun ObjectList(state: MapUiState, vm: HydroMapViewModel, modifier: Modifier) {
    val listState = rememberLazyListState()
    val visible = state.visible

    LaunchedEffect(state.query) { listState.scrollToItem(0) }

    LaunchedEffect(state.listScrollTarget) {
        val target = state.listScrollTarget ?: return@LaunchedEffect
        val index = visible.indexOfFirst { it.id == target }
        if (index >= 0) listState.animateScrollToItem(index)
        vm.onScrolled()
    }

    when {
        state.isLoading -> Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.error != null -> Box(modifier, contentAlignment = Alignment.Center) {
            EmptyState(state.error, actionLabel = "Повторити", onAction = vm::load)
        }
        state.items.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) { EmptyState("Дані про енергетичні об'єкти відсутні") }
        visible.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) {
            EmptyState("За вибраними фільтрами нічого не знайдено", actionLabel = "Скинути фільтри", onAction = vm::resetFilters)
        }
        else -> LazyColumn(
            modifier = modifier, state = listState,
            contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(visible, key = { it.id }) { item ->
                HydroObjectCard(
                    item = item,
                    selected = item.id == state.selectedId,
                    distanceKm = state.distanceKm(item),
                    onClick = { vm.select(item.id) }
                )
            }
        }
    }
}
