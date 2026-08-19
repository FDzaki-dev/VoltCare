package com.voltcare.app.ui.screens.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltcare.app.data.db.entity.RuleEntity

/**
 * Tab Aturan Cerdas: daftar RuleEntity (Room, sumber sama dg engine evaluasi di
 * BatteryMonitorService, tidak diubah) + form tambah/edit via AlertDialog + toggle
 * aktif/nonaktif + hapus (dgn konfirmasi). Contoh: IF suhu>40C AND charging THEN alarm.
 */
@Composable
fun RulesScreen(viewModel: RulesViewModel = viewModel()) {
    val rules by viewModel.rules.collectAsState()
    var editingRule by remember { mutableStateOf<RuleEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<RuleEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editingRule = null; showForm = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Aturan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Aturan Cerdas", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Contoh: IF suhu > 40C AND charging THEN alarm. Ketuk + untuk tambah aturan baru.",
                style = MaterialTheme.typography.bodyMedium
            )

            if (rules.isEmpty()) {
                Text(
                    "Belum ada aturan. Tambah aturan pertama lewat tombol + di kanan bawah.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rules, key = { it.id }) { rule ->
                        RuleRow(
                            rule = rule,
                            onToggle = { enabled -> viewModel.setEnabled(rule, enabled) },
                            onEdit = { editingRule = rule; showForm = true },
                            onDelete = { pendingDelete = rule }
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        RuleFormDialog(
            existing = editingRule,
            onDismiss = { showForm = false },
            onSave = { label, condition, value, requireCharging, action ->
                viewModel.saveRule(editingRule?.id, label, condition, value, requireCharging, action)
                showForm = false
            }
        )
    }

    pendingDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus \"${rule.label}\"?") },
            text = { Text("Aturan ini akan dihapus permanen dan berhenti dievaluasi.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRule(rule)
                    pendingDelete = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun RuleRow(
    rule: RuleEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val condition = RuleCondition.fromStored(rule.conditionType)
    val action = RuleAction.fromStored(rule.actionType)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(rule.label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
            }
            Text(
                "IF ${condition.label} ${formatValue(rule.conditionValue)}${condition.unit}" +
                    (if (rule.requireCharging) " AND charging" else "") +
                    " THEN ${action.label}",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Hapus") }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RuleFormDialog(
    existing: RuleEntity?,
    onDismiss: () -> Unit,
    onSave: (label: String, condition: RuleCondition, value: Float, requireCharging: Boolean, action: RuleAction) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var condition by remember { mutableStateOf(RuleCondition.fromStored(existing?.conditionType ?: RuleCondition.TEMP_ABOVE.stored)) }
    var valueText by remember { mutableStateOf(existing?.conditionValue?.toString() ?: "") }
    var requireCharging by remember { mutableStateOf(existing?.requireCharging ?: true) }
    var action by remember { mutableStateOf(RuleAction.fromStored(existing?.actionType ?: RuleAction.NOTIFY.stored)) }
    var conditionMenuOpen by remember { mutableStateOf(false) }
    var actionMenuOpen by remember { mutableStateOf(false) }

    val parsedValue = valueText.toFloatOrNull()
    val isValid = label.isNotBlank() && parsedValue != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Tambah Aturan" else "Edit Aturan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nama aturan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = conditionMenuOpen,
                    onExpandedChange = { conditionMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = condition.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kondisi") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionMenuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    DropdownMenu(
                        expanded = conditionMenuOpen,
                        onDismissRequest = { conditionMenuOpen = false }
                    ) {
                        RuleCondition.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { condition = option; conditionMenuOpen = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("Nilai ambang (${condition.unit})") },
                    singleLine = true,
                    isError = valueText.isNotEmpty() && parsedValue == null,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hanya saat charging")
                    Switch(checked = requireCharging, onCheckedChange = { requireCharging = it })
                }

                ExposedDropdownMenuBox(
                    expanded = actionMenuOpen,
                    onExpandedChange = { actionMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = action.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Aksi") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionMenuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    DropdownMenu(
                        expanded = actionMenuOpen,
                        onDismissRequest = { actionMenuOpen = false }
                    ) {
                        RuleAction.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { action = option; actionMenuOpen = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label.trim(), condition, parsedValue ?: 0f, requireCharging, action) },
                enabled = isValid
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun formatValue(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else value.toString()
