package com.example.facegrid.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.facegrid.presentation.screens.ErrorScreen
import com.example.facegrid.presentation.screens.HomeScreen
import com.example.facegrid.presentation.screens.ProcessingScreen
import com.example.facegrid.presentation.screens.ResultScreen
import com.example.facegrid.presentation.screens.SavedCollagesScreen
import com.example.facegrid.presentation.screens.SavedCollageResultScreen

@Composable
fun FaceGridApp(viewModel: FaceGridViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var lastSavedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        viewModel.processVideo(uri)
    }
    var pendingGalleryAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val legacyStoragePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingGalleryAction?.invoke()
        pendingGalleryAction = null
    }

    fun withGalleryPermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingGalleryAction = action
            legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            action()
        }
    }

    LaunchedEffect(state) {
        val savedUri = (state as? FaceGridUiState.Result)?.savedUri
        if (savedUri != null && savedUri != lastSavedUri) {
            lastSavedUri = savedUri
            snackbarHostState.showSnackbar("Collage saved to your gallery")
        } else if (savedUri == null) {
            lastSavedUri = null
        }
    }

    if (state is FaceGridUiState.Result) {
        BackHandler { viewModel.reset() }
    } else if (state is FaceGridUiState.SavedCollages) {
        BackHandler { viewModel.reset() }
    } else if (state is FaceGridUiState.SavedResult) {
        BackHandler { viewModel.backFromSavedCollage() }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val current = state) {
                is FaceGridUiState.Home -> HomeScreen(
                    savedCollages = current.savedCollages,
                    isLoadingSaved = current.isLoadingSaved,
                    onPick = { videoPicker.launch(arrayOf("video/*")) },
                    onSeeAll = viewModel::showSavedCollages,
                    onOpen = viewModel::showSavedCollage
                )
                is FaceGridUiState.SavedCollages -> SavedCollagesScreen(
                    collages = current.collages,
                    onBack = viewModel::reset,
                    onOpen = viewModel::showSavedCollage
                )
                is FaceGridUiState.SavedResult -> SavedCollageResultScreen(
                    collage = current.collage,
                    onBack = viewModel::backFromSavedCollage,
                    onShare = { shareCollage(context, current.collage.uri) }
                )
                is FaceGridUiState.Processing -> ProcessingScreen(current.progress)
                is FaceGridUiState.Result -> ResultScreen(
                    result = current,
                    onSave = { withGalleryPermission(viewModel::saveCollage) },
                    onShare = {
                        withGalleryPermission {
                            viewModel.shareCollage { uri -> shareCollage(context, uri) }
                        }
                    },
                    onPickAnother = { videoPicker.launch(arrayOf("video/*")) },
                    onBack = viewModel::reset
                )
                is FaceGridUiState.Error -> ErrorScreen(current.message, viewModel::reset)
            }
        }
    }
}

private fun shareCollage(context: Context, uri: android.net.Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share FaceGrid collage"))
}
