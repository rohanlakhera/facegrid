package com.example.facegrid.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.facegrid.data.gallery.MediaStoreGalleryRepository
import com.example.facegrid.data.processing.VideoProcessor
import com.example.facegrid.data.repository.VideoProcessingRepositoryImpl
import com.example.facegrid.domain.model.ProcessingResult
import com.example.facegrid.domain.model.ProcessingStage
import com.example.facegrid.domain.model.ProgressUpdate
import com.example.facegrid.domain.model.SavedCollage
import com.example.facegrid.domain.usecase.ProcessVideoUseCase
import com.example.facegrid.domain.usecase.SaveCollageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed interface FaceGridUiState {
    data class Home(
        val savedCollages: List<SavedCollage> = emptyList(),
        val isLoadingSaved: Boolean = true
    ) : FaceGridUiState

    data class SavedCollages(val collages: List<SavedCollage>) : FaceGridUiState
    data class SavedResult(
        val collage: SavedCollage,
        val previousCollages: List<SavedCollage>? = null
    ) : FaceGridUiState

    data class Processing(val progress: ProgressUpdate) : FaceGridUiState
    data class Result(val output: ProcessingResult, val savedUri: Uri? = null) : FaceGridUiState
    data class Error(val message: String) : FaceGridUiState
}

class FaceGridViewModel(
    private val processVideo: ProcessVideoUseCase,
    private val saveCollage: SaveCollageUseCase,
    private val galleryRepository: com.example.facegrid.domain.repository.GalleryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<FaceGridUiState>(FaceGridUiState.Home())
    val state: StateFlow<FaceGridUiState> = _state.asStateFlow()

    init {
        refreshSavedCollages()
    }

    fun processVideo(uri: Uri) {
        _state.value = FaceGridUiState.Processing(
            ProgressUpdate(ProcessingStage.EXTRACTING, 0, 0, "Preparing video")
        )
        viewModelScope.launch {
            try {
                val result = processVideo(uri) { progress ->
                    _state.value = FaceGridUiState.Processing(progress)
                }
                delay(320.milliseconds)
                _state.value = FaceGridUiState.Result(result)
            } catch (error: Throwable) {
                _state.value =
                    FaceGridUiState.Error(error.message ?: "Could not process this video")
            }
        }
    }

    fun saveCollage() {
        val current = _state.value as? FaceGridUiState.Result ?: return
        viewModelScope.launch {
            runCatching { saveCollage(current.output.collage) }
                .onSuccess { uri -> _state.value = current.copy(savedUri = uri) }
                .onFailure { error ->
                    _state.value = FaceGridUiState.Error(error.message ?: "Could not save collage")
                }
        }
    }

    fun shareCollage(onReady: (Uri) -> Unit) {
        val current = _state.value as? FaceGridUiState.Result ?: return
        current.savedUri?.let {
            onReady(it)
            return
        }
        viewModelScope.launch {
            runCatching { saveCollage(current.output.collage) }
                .onSuccess { uri ->
                    _state.value = current.copy(savedUri = uri)
                    onReady(uri)
                }
                .onFailure { error ->
                    _state.value =
                        FaceGridUiState.Error(error.message ?: "Could not prepare collage")
                }
        }
    }

    fun reset() {
        _state.value = FaceGridUiState.Home(isLoadingSaved = true)
        refreshSavedCollages()
    }

    fun showSavedCollages() {
        val current = _state.value as? FaceGridUiState.Home ?: return
        _state.value = FaceGridUiState.SavedCollages(current.savedCollages)
    }

    fun showSavedCollage(collage: SavedCollage) {
        val previousCollages = (_state.value as? FaceGridUiState.SavedCollages)?.collages
        _state.value = FaceGridUiState.SavedResult(collage, previousCollages)
    }

    fun backFromSavedCollage() {
        val current = _state.value as? FaceGridUiState.SavedResult ?: return
        current.previousCollages?.let {
            _state.value = FaceGridUiState.SavedCollages(it)
        } ?: reset()
    }

    private fun refreshSavedCollages() {
        viewModelScope.launch {
            val saved = runCatching { galleryRepository.listSaved() }.getOrDefault(emptyList())
            _state.value = FaceGridUiState.Home(savedCollages = saved, isLoadingSaved = false)
        }
    }
}

class FaceGridViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val appContext = context.applicationContext

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FaceGridViewModel::class.java)) {
            val videoRepository = VideoProcessingRepositoryImpl(VideoProcessor(appContext))
            val galleryRepository = MediaStoreGalleryRepository(appContext)
            return FaceGridViewModel(
                processVideo = ProcessVideoUseCase(videoRepository),
                saveCollage = SaveCollageUseCase(galleryRepository),
                galleryRepository = galleryRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
