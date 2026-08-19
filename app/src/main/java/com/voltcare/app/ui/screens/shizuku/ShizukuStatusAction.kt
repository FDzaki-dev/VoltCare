package com.voltcare.app.ui.screens.shizuku

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltcare.app.R
import com.voltcare.app.ui.theme.VcAmber
import com.voltcare.app.ui.theme.VcGreen
import com.voltcare.app.ui.theme.VcTextSecondary
import com.voltcare.app.util.ShizukuManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ShizukuStatusAction (Batch 26 - Pending Queue #17: Shizuku UI Wiring)
 *
 * Lanjutan langsung ShizukuManager.kt (Batch 23, engine-only). Entry point non-invasif:
 * ikon status + AlertDialog, dipasang di NavGraph.kt (overlay Dashboard, TopStart) supaya
 * DashboardScreen.kt TIDAK perlu disentuh — pola sama persis dengan UpdateCheckAction
 * (TopEnd, Batch 21).
 *
 * Self-contained: listener binder/permission didaftarkan di sini (bukan di
 * VoltCareApplication.kt) supaya batch ini tetap 3 file sesuai Strict Micro-Batching Rule.
 * Registrasi listener level-Application (utk lifecycle di luar layar Dashboard) di-queue
 * terpisah jika terbukti dibutuhkan.
 */
class ShizukuStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ShizukuManager.currentState())
    val uiState: StateFlow<ShizukuManager.State> = _uiState.asStateFlow()

    init {
        refresh()
        ShizukuManager.addBinderListeners(onReceived = { refresh() }, onDead = { refresh() })
        ShizukuManager.addPermissionResultListener { refresh() }
    }

    fun refresh() {
        _uiState.value = ShizukuManager.currentState()
    }

    fun requestPermission() {
        ShizukuManager.requestPermission()
    }
}

@Composable
fun ShizukuStatusAction(viewModel: ShizukuStatusViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Re-cek status tiap kali Composable ini masuk komposisi (mis. balik dari app Shizuku).
    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { }
    }

    val tint = when (state) {
        ShizukuManager.State.Ready -> VcGreen
        ShizukuManager.State.PermissionDenied -> VcAmber
        else -> VcTextSecondary
    }

    IconButton(onClick = { showDialog = true }) {
        Icon(
            Icons.Filled.AdminPanelSettings,
            contentDescription = stringResource(R.string.shizuku_status_action),
            tint = tint
        )
    }

    if (showDialog) {
        val (titleRes, bodyRes) = when (state) {
            ShizukuManager.State.NotInstalled -> R.string.shizuku_not_installed_title to R.string.shizuku_not_installed_body
            ShizukuManager.State.NotRunning -> R.string.shizuku_not_running_title to R.string.shizuku_not_running_body
            ShizukuManager.State.PermissionDenied -> R.string.shizuku_permission_denied_title to R.string.shizuku_permission_denied_body
            ShizukuManager.State.Ready -> R.string.shizuku_ready_title to R.string.shizuku_ready_body
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(titleRes)) },
            text = { Text(stringResource(bodyRes)) },
            confirmButton = {
                if (state == ShizukuManager.State.PermissionDenied || state == ShizukuManager.State.NotRunning) {
                    TextButton(onClick = {
                        viewModel.requestPermission()
                        showDialog = false
                    }) { Text(stringResource(R.string.shizuku_request_permission)) }
                } else {
                    TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.shizuku_close)) }
                }
            },
            dismissButton = {
                if (state == ShizukuManager.State.PermissionDenied || state == ShizukuManager.State.NotRunning) {
                    TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.shizuku_close)) }
                }
            }
        )
    }
}
