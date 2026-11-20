package com.example.mse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mse.model.HydroObject
import com.example.mse.model.OperatingStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun statusColor(status: OperatingStatus): Color = when (status) {
    OperatingStatus.RUNNING -> Color(0xFF2E7D32)
    OperatingStatus.REDUCED -> Color(0xFFEF6C00)
    OperatingStatus.MAINTENANCE -> Color(0xFF546E7A)
    OperatingStatus.OFFLINE -> Color(0xFFC62828)
}

fun formatUpdated(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

fun formatDistance(km: Double): String =
    if (km < 10) "%.1f км".format(km) else "%.0f км".format(km)

@Composable
fun StatusBadge(status: OperatingStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(statusColor(status), CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(status.title, style = MaterialTheme.typography.labelMedium, color = statusColor(status), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HydroObjectCard(
    item: HydroObject,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showDescription: Boolean = true,
    showPowerDetails: Boolean = true,
    distanceKm: Double? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    Card(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            if (checked != null) {
                Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.type.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    buildString {
                        append("${item.city}, р. ${item.river}")
                        if (distanceKm != null) append(" · ${formatDistance(distanceKm)} від вас")
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showDescription) Text(item.description, style = MaterialTheme.typography.bodyMedium)
                if (showPowerDetails) {
                    Text("Потужність: ${item.outputMw.toInt()} / ${item.capacityMw.toInt()} МВт (${item.loadPercent} %)",
                        style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { item.loadPercent / 100f }, modifier = Modifier.fillMaxWidth(),
                        color = statusColor(item.status))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(item.status)
                    Spacer(Modifier.weight(1f))
                    Text("Оновлено ${formatUpdated(item.updatedAt)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                trailing?.invoke()
            }
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Default.SearchOff, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) OutlinedButton(onClick = onAction) { Text(actionLabel) }
    }
}
