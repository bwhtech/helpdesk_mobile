package io.github.kaulith.helpdeskanalytics.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterCondition
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterFieldType
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterOperator
import io.github.kaulith.helpdeskanalytics.domain.model.filter.FilterableField
import io.github.kaulith.helpdeskanalytics.domain.model.filter.IS_NOT_SET
import io.github.kaulith.helpdeskanalytics.domain.model.filter.IS_SET
import io.github.kaulith.helpdeskanalytics.domain.model.filter.Timespan
import io.github.kaulith.helpdeskanalytics.domain.model.filter.defaultValueFor
import io.github.kaulith.helpdeskanalytics.ui.theme.FrappeRadius
import io.github.kaulith.helpdeskanalytics.ui.theme.Spacing
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterSheet(
    fields: List<FilterableField<T>>,
    conditions: List<FilterCondition<T>>,
    onApply: (List<FilterCondition<T>>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rows by remember { mutableStateOf(conditions) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.base)
                .padding(bottom = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (rows.isNotEmpty()) {
                    TextButton(onClick = { rows = emptyList() }) { Text("Clear all") }
                }
            }

            if (rows.isEmpty()) {
                Text(
                    text = "Empty, add a field to filter by",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.sm),
                )
            }

            rows.forEachIndexed { index, condition ->
                ConditionRow(
                    prefix = if (index == 0) "Where" else "And",
                    fields = fields,
                    condition = condition,
                    onChange = { updated -> rows = rows.toMutableList().also { it[index] = updated } },
                    onRemove = { rows = rows.toMutableList().also { it.removeAt(index) } },
                )
            }

            OutlinedButton(
                onClick = { rows = rows + newCondition(fields) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("Add filter")
            }

            Button(
                onClick = { onApply(rows); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apply") }
        }
    }
}

private fun <T> newCondition(fields: List<FilterableField<T>>): FilterCondition<T> {
    val field = fields.first()
    val op = field.defaultOperator
    return FilterCondition(field, op, defaultValueFor(field, op))
}

@Composable
private fun <T> ConditionRow(
    prefix: String,
    fields: List<FilterableField<T>>,
    condition: FilterCondition<T>,
    onChange: (FilterCondition<T>) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = prefix,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Close, contentDescription = "Remove filter",
                    modifier = Modifier.size(18.dp))
            }
        }
        DropdownField(
            label = condition.field.label,
            options = fields.map { it.label },
            onSelectedIndex = { i ->
                val field = fields[i]
                val op = field.defaultOperator
                onChange(condition.copy(field = field, operator = op, value = defaultValueFor(field, op)))
            },
        )
        val operators = FilterOperator.forType(condition.field.type)
        DropdownField(
            label = condition.operator.label,
            options = operators.map { it.label },
            onSelectedIndex = { i ->
                val op = operators[i]
                onChange(condition.copy(operator = op, value = defaultValueFor(condition.field, op)))
            },
        )
        ValueControl(condition = condition, onValueChange = { onChange(condition.copy(value = it)) })
    }
}

@Composable
private fun <T> ValueControl(condition: FilterCondition<T>, onValueChange: (String) -> Unit) {
    val field = condition.field
    val op = condition.operator
    when {
        op == FilterOperator.IS -> DropdownField(
            label = if (condition.value == IS_NOT_SET) "Not Set" else "Set",
            options = listOf("Set", "Not Set"),
            onSelectedIndex = { onValueChange(if (it == 0) IS_SET else IS_NOT_SET) },
        )

        field.type == FilterFieldType.DATE && op == FilterOperator.TIMESPAN -> DropdownField(
            label = Timespan.entries.find { it.name == condition.value }?.label ?: "Select",
            options = Timespan.entries.map { it.label },
            onSelectedIndex = { onValueChange(Timespan.entries[it].name) },
        )

        field.type == FilterFieldType.DATE && op == FilterOperator.BETWEEN -> {
            val parts = condition.value.split(",")
            val from = parts.getOrNull(0)?.toLongOrNull()
            val to = parts.getOrNull(1)?.toLongOrNull()
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DateButton(millis = from, placeholder = "From", modifier = Modifier.weight(1f)) {
                    onValueChange("$it,${to ?: ""}")
                }
                DateButton(millis = to, placeholder = "To", modifier = Modifier.weight(1f)) {
                    onValueChange("${from ?: ""},$it")
                }
            }
        }

        field.type == FilterFieldType.DATE -> DateButton(
            millis = condition.value.toLongOrNull(),
            placeholder = "Select date",
            modifier = Modifier.fillMaxWidth(),
        ) { onValueChange(it.toString()) }

        field.type == FilterFieldType.SELECT &&
            op in listOf(FilterOperator.EQUALS, FilterOperator.NOT_EQUALS) -> DropdownField(
            label = condition.value.ifBlank { "Select" },
            options = field.options,
            onSelectedIndex = { onValueChange(field.options[it]) },
        )

        else -> OutlinedTextField(
            value = condition.value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(valuePlaceholder(op)) },
        )
    }
}

private fun valuePlaceholder(op: FilterOperator): String = when (op) {
    FilterOperator.IN, FilterOperator.NOT_IN -> "value 1, value 2"
    FilterOperator.LIKE, FilterOperator.NOT_LIKE -> "contains..."
    else -> "value"
}

@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    onSelectedIndex: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = FrappeRadius.md,
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null,
                modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { i, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelectedIndex(i); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateButton(
    millis: Long?,
    placeholder: String,
    modifier: Modifier = Modifier,
    onPicked: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { showPicker = true }, modifier = modifier, shape = FrappeRadius.md) {
        Text(millis?.let { formatDate(it) } ?: placeholder)
    }
    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onPicked)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

private fun formatDate(millis: Long): String =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
