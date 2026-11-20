package com.example.mse.ui.screens.task13

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mse.model.LevelStatus
import com.example.mse.model.SensorState

@Composable
fun SensorStreamsScreen(viewModel: SensorStreamsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val running = state.basic.collecting || state.medium.running || state.advanced.sensor != SensorState.Stopped

    LazyVerticalGrid(
        columns = GridCells.Adaptive(360.dp),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Потоки даних датчиків Київського водосховища", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (running) {
                        OutlinedButton(onClick = viewModel::stopAll) {
                            Icon(Icons.Default.Stop, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Зупинити")
                        }
                    }
                    Button(onClick = viewModel::startAll) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                        Text(if (running) "Перезапустити" else "Запустити моніторинг")
                    }
                }
                Row(
                    Modifier.toggleable(state.faultInjected, role = Role.Switch, onValueChange = viewModel::setFault),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(checked = state.faultInjected, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Імітувати збій датчиків (рівня та припливу)")
                }
            }
        }

        item { BasicCard(state.basic) }
        item { MediumCard(state.medium) }
        item { AdvancedCard(state, viewModel::setOutflow) }
        item { LogCard(state.log) }
    }
}

@Composable
private fun Section(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BasicCard(basic: BasicState) {
    Section("Базовий: холодний Flow", "flow { emit() } стартує при collect; сирі значення без обробки (видно викиди 0.0 м)") {
        if (basic.samples.isEmpty()) {
            Text("Потік не запущено", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Відліків отримано: ${basic.count}", style = MaterialTheme.typography.labelMedium)
            basic.samples.forEach {
                Text("%.2f м".format(it), fontFamily = FontFamily.Monospace, fontSize = 14.sp,
                    color = if (it < 90) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun MediumCard(medium: MediumState) {
    Section("Середній: map / filter / debounce / catch", "Викиди відкидає filter, map рахує % наповнення, debounce стабілізує статус") {
        medium.reading?.let { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("Рівень", "%.2f м".format(r.levelM))
                Metric("Наповнення", "%.0f %%".format(r.fillPercent))
            }
        } ?: Text("Очікування даних…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Metric("Отримано", medium.received.toString())
            Metric("Відкинуто", medium.dropped.toString())
            Metric("Стабільний статус", medium.stableStatus?.title ?: "—")
        }
        medium.error?.let {
            Text("Помилка: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AdvancedCard(state: SensorUiState, onOutflow: (Double) -> Unit) {
    val a = state.advanced
    Section("Просунутий: гарячі потоки, combine / zip, retry", "SharedFlow (shareIn) → combine рівня, припливу, скиду та прогнозу; retryWhen з backoff") {
        val (label, color) = when (val s = a.sensor) {
            SensorState.Stopped -> "Зупинено" to MaterialTheme.colorScheme.onSurfaceVariant
            SensorState.Running -> "Датчики в нормі" to Color(0xFF2E7D32)
            is SensorState.Retrying -> "Повтор ${s.attempt}/${s.maxAttempts} через ${s.delayMs} мс" to Color(0xFFEF6C00)
            is SensorState.Failed -> "Збій: ${s.message}" to MaterialTheme.colorScheme.error
        }
        AssistChip(onClick = {}, label = { Text(label, color = color) })

        a.snapshot?.let { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("Рівень", "%.2f м".format(s.levelM))
                Metric("Прогноз +6 год", "%.2f м".format(s.forecastLevelM))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Metric("Приплив", "%.0f м³/с".format(s.inflowM3s))
                Metric("Скид", "%.0f м³/с".format(s.outflowM3s))
                Metric("Баланс", "%+.0f м³/с".format(s.balanceM3s))
            }
            Text("Статус: ${s.status.title}", color = if (s.status == LevelStatus.NORMAL) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
        } ?: Text("Очікування даних…", color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("Скид через агрегати: %.0f м³/с".format(state.outflowM3s), style = MaterialTheme.typography.labelMedium)
        Slider(value = state.outflowM3s.toFloat(), onValueChange = { onOutflow(it.toDouble()) }, valueRange = 600f..1800f)

        HorizontalDivider()
        a.pair?.let { (l, i) -> Text("zip (n-й рівень + n-й приплив): %.2f м · %.0f м³/с".format(l, i), style = MaterialTheme.typography.bodySmall) }
        Text("Підписників hot-потоку: ${a.subscribers} · повторів: ${a.retries} · відновлень: ${a.recoveries}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LogCard(log: List<String>) {
    Section("Журнал подій", "Помилки та відновлення потоків") {
        if (log.isEmpty()) Text("Подій ще немає", color = MaterialTheme.colorScheme.onSurfaceVariant)
        log.take(8).forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
    }
}
