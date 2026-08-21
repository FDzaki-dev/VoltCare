package com.voltcare.app.ui.screens.rules

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
    var showPreset by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<RuleEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editingRule = null; showForm = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah Aturan")
            }
        }
    ) { padding ->
        // Fix (audit UX, terverifikasi - pola identik dgn DrainScreen Batch 55): sebelumnya
        // Column(fillMaxSize) TIDAK scroll berisi LazyColumn bersarang tanpa weight - kalau
        // daftar aturan berkembang bisa ke-clip di bawah tanpa cara scroll. Diganti SATU
        // LazyColumn datar (item{} utk header, items(rules){} utk daftar) - 0 logic disentuh.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Aturan Cerdas", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Text(
                    "Contoh: IF suhu > 40C AND charging THEN alarm. Ketuk + untuk tambah aturan baru.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                // Batch 42 (Pending #11): shortcut 1-tap, tidak perlu isi form lengkap 5 field
                // buat kasus paling umum "alarm kalau charging kelewat batas".
                TextButton(onClick = { showPreset = true }) {
                    Text("+ Preset Cepat: Alarm Batas Charge")
                }
            }

            if (rules.isEmpty()) {
                item {
                    Text(
                        "Belum ada aturan. Tambah aturan pertama lewat tombol + di kanan bawah.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
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

    if (showForm) {
        RuleFormDialog(
            existing = editingRule,
            onDismiss = { showForm = false },
            onSave = { label, condition, value, requireCharging, action, alarmSoundUri, alarmLoop ->
                viewModel.saveRule(editingRule?.id, label, condition, value, requireCharging, action, alarmSoundUri, alarmLoop)
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

    if (showPreset) {
        ChargeLimitPresetDialog(
            onDismiss = { showPreset = false },
            onSave = { percent ->
                viewModel.saveChargeLimitPreset(percent)
                showPreset = false
            }
        )
    }
}

/**
 * Batch 42 (Pending #11): dialog minimal khusus preset "Alarm Batas Charge" — cuma 1 field
 * (persen ambang), beda dari [RuleFormDialog] yang minta 5 field lengkap. `requireCharging`
 * & `actionType` sudah tetap (true / ALARM) sesuai definisi preset, tidak perlu dipilih user.
 */
@Composable
private fun ChargeLimitPresetDialog(
    onDismiss: () -> Unit,
    onSave: (percent: Float) -> Unit
) {
    var percentText by remember { mutableStateOf("80") }
    val parsedPercent = percentText.toFloatOrNull()
    val isValid = parsedPercent != null && parsedPercent in 1f..100f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preset: Alarm Batas Charge") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Alarm otomatis berbunyi saat baterai sedang di-charge dan melewati " +
                        "persentase ini. Cocok untuk kebiasaan cabut charger tepat waktu.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = percentText,
                    onValueChange = { percentText = it },
                    label = { Text("Batas persen (1-100)") },
                    singleLine = true,
                    isError = percentText.isNotEmpty() && !isValid,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedPercent?.let(onSave) },
                enabled = isValid
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
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
    onSave: (label: String, condition: RuleCondition, value: Float, requireCharging: Boolean, action: RuleAction, alarmSoundUri: String?, alarmLoop: Boolean) -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var condition by remember { mutableStateOf(RuleCondition.fromStored(existing?.conditionType ?: RuleCondition.TEMP_ABOVE.stored)) }
    var valueText by remember { mutableStateOf(existing?.conditionValue?.toString() ?: "") }
    // Fix (Batch 63, root cause bug "alarm gak ke-trigger"): default lama SELALU true, jebakan
    // untuk kondisi PERCENT_BELOW (baterai lemah) - "lemah SAAT charging" nyaris mustahil terjadi
    // bareng, jadi alarm kelihatan "gak pernah nyala" padahal ambang sudah lama terlewati.
    // Default kini kontekstual: PERCENT_BELOW -> false, kondisi lain tetap true (perilaku lama).
    var requireCharging by remember {
        mutableStateOf(existing?.requireCharging ?: (condition != RuleCondition.PERCENT_BELOW))
    }
    var action by remember { mutableStateOf(RuleAction.fromStored(existing?.actionType ?: RuleAction.NOTIFY.stored)) }
    // Pending Queue #26 (RESOLVED): custom nada alarm via RingtoneManager.ACTION_RINGTONE_PICKER.
    var alarmSoundUri by remember { mutableStateOf(existing?.alarmSoundUri) }
    var alarmLoop by remember { mutableStateOf(existing?.alarmLoop ?: false) }
    var conditionMenuOpen by remember { mutableStateOf(false) }
    var actionMenuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Batch #60 (RESOLVED): tampilkan judul nada alarm asli, bukan generik "Custom terpilih".
    var alarmSoundTitle by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(alarmSoundUri) {
        alarmSoundTitle = alarmSoundUri?.let { uriStr ->
            runCatching {
                RingtoneManager.getRingtone(context, Uri.parse(uriStr))?.getTitle(context)
            }.getOrNull()
        }
    }
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        alarmSoundUri = uri?.toString()
    }

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
                // Fix (Batch 63): peringatan eksplisit untuk kombinasi kontradiktif yang jadi
                // root cause "alarm gak ke-trigger walaupun ambang batas terpenuhi" - baterai
                // lemah SAAT charging nyaris tidak pernah terjadi bersamaan.
                if (condition == RuleCondition.PERCENT_BELOW && requireCharging) {
                    Text(
                        "⚠ Kombinasi ini nyaris tidak pernah terpenuhi: baterai jarang \"lemah\" " +
                            "SAAT sedang di-charge. Matikan switch di atas kalau aturan ini untuk " +
                            "peringatan baterai lemah biasa (tidak charging).",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.voltcare.app.ui.theme.VcAmber
                    )
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

                if (action == RuleAction.ALARM) {
                    TextButton(onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            alarmSoundUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                        }
                        ringtonePickerLauncher.launch(intent)
                    }) {
                        Text(
                            if (alarmSoundUri == null) "Pilih Nada Alarm (default sistem)"
                            else "Nada Alarm: ${alarmSoundTitle ?: "Custom"} ✓"
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Ulangi terus sampai dimatikan manual")
                        Switch(checked = alarmLoop, onCheckedChange = { alarmLoop = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(label.trim(), condition, parsedValue ?: 0f, requireCharging, action, alarmSoundUri, alarmLoop) },
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
