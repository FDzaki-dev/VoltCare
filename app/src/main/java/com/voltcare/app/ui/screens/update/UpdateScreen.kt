package com.voltcare.app.ui.screens.update

import android.app.Application
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voltcare.app.R
import com.voltcare.app.util.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** State UI in-app updater. Nama meniru pola UpdateManager.DownloadResult (tidak diubah). */
sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data class Available(val info: UpdateManager.UpdateInfo) : UpdateUiState()
    data class Downloading(val info: UpdateManager.UpdateInfo, val percent: Int) : UpdateUiState()
    data class ReadyToInstall(val file: File) : UpdateUiState()
    data class Failed(val message: String) : UpdateUiState()
}

/**
 * ViewModel in-app updater. Murni orkestrasi ke [UpdateManager] (Batch 19/20, tidak disentuh
 * di batch ini) — cek rilis, download dengan progress, lalu trigger install.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdate() {
        _uiState.value = UpdateUiState.Checking
        viewModelScope.launch {
            _uiState.value = when (val result = UpdateManager.checkForUpdate(getApplication())) {
                is UpdateManager.UpdateCheckResult.Available -> UpdateUiState.Available(result.info)
                is UpdateManager.UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateManager.UpdateCheckResult.CheckFailed -> UpdateUiState.Failed(result.reason)
            }
        }
    }

    fun startDownload(info: UpdateManager.UpdateInfo) {
        _uiState.value = UpdateUiState.Downloading(info, 0)
        viewModelScope.launch {
            when (val result = UpdateManager.downloadUpdate(getApplication(), info) { percent ->
                _uiState.value = UpdateUiState.Downloading(info, percent)
            }) {
                is UpdateManager.DownloadResult.Success -> _uiState.value = UpdateUiState.ReadyToInstall(result.file)
                is UpdateManager.DownloadResult.Failed -> _uiState.value = UpdateUiState.Failed(result.message)
            }
        }
    }

    fun dismiss() {
        _uiState.value = UpdateUiState.Idle
    }
}

/**
 * Entry point non-invasif: tombol ikon "Cek Update" + AlertDialog hasil, dipasang di
 * NavGraph.kt (overlay Dashboard) supaya DashboardScreen.kt tidak perlu disentuh.
 */
@Composable
fun UpdateCheckAction(viewModel: UpdateViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    IconButton(onClick = { viewModel.checkForUpdate() }) {
        Icon(Icons.Filled.SystemUpdate, contentDescription = stringResource(R.string.update_check_action))
    }

    when (val s = state) {
        is UpdateUiState.Idle -> Unit
        is UpdateUiState.Checking -> AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text(stringResource(R.string.update_checking_title)) },
            text = { CircularProgressIndicator() }
        )
        is UpdateUiState.UpToDate -> AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            confirmButton = { TextButton(onClick = { viewModel.dismiss() }) { Text(stringResource(R.string.update_close)) } },
            title = { Text(stringResource(R.string.update_up_to_date_title)) },
            text = { Text(stringResource(R.string.update_up_to_date_body)) }
        )
        is UpdateUiState.Available -> AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            confirmButton = {
                TextButton(onClick = { viewModel.startDownload(s.info) }) { Text(stringResource(R.string.update_download)) }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismiss() }) { Text(stringResource(R.string.update_close)) } },
            title = { Text(stringResource(R.string.update_available_title, s.info.latestVersionName)) },
            text = { Text(s.info.releaseNotes.ifBlank { stringResource(R.string.update_no_notes) }) }
        )
        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { Text(stringResource(R.string.update_downloading_title, s.percent)) },
            text = { LinearProgressIndicator(progress = s.percent / 100f, modifier = Modifier.fillMaxWidth()) }
        )
        is UpdateUiState.ReadyToInstall -> AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            confirmButton = {
                TextButton(onClick = {
                    if (UpdateManager.canRequestInstallPackages(context)) {
                        UpdateManager.installApk(context, s.file)
                        viewModel.dismiss()
                    } else {
                        context.startActivity(UpdateManager.installPermissionSettingsIntent(context))
                    }
                }) { Text(stringResource(R.string.update_install)) }
            },
            dismissButton = { TextButton(onClick = { viewModel.dismiss() }) { Text(stringResource(R.string.update_close)) } },
            title = { Text(stringResource(R.string.update_ready_title)) },
            text = { Text(stringResource(R.string.update_ready_body)) }
        )
        is UpdateUiState.Failed -> AlertDialog(
            onDismissRequest = { viewModel.dismiss() },
            confirmButton = { TextButton(onClick = { viewModel.dismiss() }) { Text(stringResource(R.string.update_close)) } },
            title = { Text(stringResource(R.string.update_failed_title)) },
            text = { Text(s.message) }
        )
    }
}
