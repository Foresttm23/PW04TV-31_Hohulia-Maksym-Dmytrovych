package com.example.mse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mse.model.HydroObjectType
import com.example.mse.model.ObjectSort
import com.example.mse.model.OperatingStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TypeFilterChips(selected: Set<HydroObjectType>, onToggle: (HydroObjectType) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HydroObjectType.entries.forEach { type ->
            FilterChip(selected = type in selected, onClick = { onToggle(type) }, label = { Text(type.title) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusFilterChips(selected: Set<OperatingStatus>, onToggle: (OperatingStatus) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OperatingStatus.entries.forEach { status ->
            FilterChip(selected = status in selected, onClick = { onToggle(status) }, label = { Text(status.title) })
        }
    }
}

@Composable
fun SortMenu(
    options: List<ObjectSort>,
    selected: ObjectSort,
    descending: Boolean,
    onSelect: (ObjectSort) -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box {
            AssistChip(
                onClick = { expanded = true },
                label = { Text("Сортування: ${selected.title}") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp)) }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option.title) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
        IconButton(onClick = onToggleDirection) {
            Icon(if (descending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = if (descending) "За спаданням" else "За зростанням")
        }
    }
}
