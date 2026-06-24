package com.photoeditor.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photoeditor.data.model.AdjustmentType
import com.photoeditor.data.model.AspectRatio
import com.photoeditor.data.model.CropState
import com.photoeditor.data.model.EditState
import com.photoeditor.data.model.EditTab
import com.photoeditor.data.model.FilterType
import com.photoeditor.data.repository.ImageRepository
import com.photoeditor.filters.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditViewModel(
    application: Application,
    private val imageUri: Uri
) : AndroidViewModel(application) {

    private val repository = ImageRepository(application)

    private val _sourceBitmap = MutableStateFlow<Bitmap?>(null)
    val sourceBitmap: StateFlow<Bitmap?> = _sourceBitmap.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _editState = MutableStateFlow(EditState())
    val editState: StateFlow<EditState> = _editState.asStateFlow()

    private val _selectedTab = MutableStateFlow(EditTab.ADJUST)
    val selectedTab: StateFlow<EditTab> = _selectedTab.asStateFlow()

    private val _selectedAdjustment = MutableStateFlow<AdjustmentType?>(null)
    val selectedAdjustment: StateFlow<AdjustmentType?> = _selectedAdjustment.asStateFlow()

    private val _filterPreviews = MutableStateFlow<Map<FilterType, Bitmap>>(emptyMap())
    val filterPreviews: StateFlow<Map<FilterType, Bitmap>> = _filterPreviews.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _savedUri = MutableStateFlow<Uri?>(null)
    val savedUri: StateFlow<Uri?> = _savedUri.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var processingJob: Job? = null
    private var historyJob: Job? = null
    private val editHistory = mutableListOf<EditState>()
    private var historyIndex = -1

    init {
        loadImage()
        _selectedAdjustment.value = AdjustmentType.EXPOSURE
    }

    private fun loadImage() {
        viewModelScope.launch {
            _isLoading.value = true
            val bitmap = repository.loadBitmap(imageUri)
            if (bitmap != null) {
                _sourceBitmap.value = bitmap
                _previewBitmap.value = bitmap
                pushHistory(EditState())
                generateFilterPreviews(bitmap)
            } else {
                _errorMessage.value = "Failed to load image"
            }
            _isLoading.value = false
        }
    }

    private fun generateFilterPreviews(source: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            val previews = mutableMapOf<FilterType, Bitmap>()
            FilterType.entries.forEach { filterType ->
                previews[filterType] = ImageProcessor.processFilterPreview(source, filterType, 100f)
            }
            _filterPreviews.value = previews
        }
    }

    fun updateAdjustment(type: AdjustmentType, value: Float) {
        _editState.update { it.withAdjustment(type, value) }
        scheduleProcessing()
        scheduleHistoryPush()
    }

    fun resetAdjustment(type: AdjustmentType) {
        _editState.update { it.withAdjustment(type, 0f) }
        scheduleProcessing()
        scheduleHistoryPush()
    }

    fun selectAdjustment(type: AdjustmentType?) {
        _selectedAdjustment.value = type
    }

    fun selectTab(tab: EditTab) {
        _selectedTab.value = tab
        when (tab) {
            EditTab.ADJUST -> {
                if (_selectedAdjustment.value == null) {
                    _selectedAdjustment.value = AdjustmentType.EXPOSURE
                }
            }
            else -> _selectedAdjustment.value = null
        }
    }

    fun selectFilter(filterType: FilterType) {
        _editState.update { it.copy(selectedFilter = filterType) }
        scheduleProcessing()
        scheduleHistoryPush()
    }

    fun updateFilterIntensity(intensity: Float) {
        _editState.update { it.copy(filterIntensity = intensity) }
        scheduleProcessing()
        scheduleHistoryPush()
    }

    fun updateCrop(crop: CropState) {
        _editState.update { it.copy(cropState = crop) }
    }

    fun updateAspectRatio(ratio: AspectRatio) {
        _editState.update { it.copy(cropState = it.cropState.copy(aspectRatio = ratio)) }
    }

    fun updateRotation(rotation: Float) {
        _editState.update { it.copy(cropState = it.cropState.copy(rotation = rotation)) }
        scheduleHistoryPush()
    }

    fun resetCrop() {
        _editState.update { it.copy(cropState = CropState()) }
        scheduleHistoryPush()
    }

    fun undo() {
        if (historyIndex > 0) {
            historyJob?.cancel()
            historyIndex--
            _editState.value = editHistory[historyIndex]
            updateCanUndoRedo()
            scheduleProcessing(immediate = true)
        }
    }

    fun redo() {
        if (historyIndex < editHistory.size - 1) {
            historyJob?.cancel()
            historyIndex++
            _editState.value = editHistory[historyIndex]
            updateCanUndoRedo()
            scheduleProcessing(immediate = true)
        }
    }

    fun autoEnhance() {
        val source = _sourceBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val suggestions = ImageProcessor.autoEnhance(source)
            _editState.update { current ->
                current.copy(
                    brightness = suggestions["brightness"] ?: current.brightness,
                    contrast = suggestions["contrast"] ?: current.contrast,
                    warmth = suggestions["warmth"] ?: current.warmth
                )
            }
            scheduleProcessing(immediate = true)
            scheduleHistoryPush()
        }
    }

    fun revertToOriginal() {
        _editState.value = EditState()
        _previewBitmap.value = _sourceBitmap.value
        pushHistory(EditState())
    }

    fun saveImage() {
        val source = _sourceBitmap.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val processed = withContext(Dispatchers.Default) {
                ImageProcessor.process(source, _editState.value)
            }
            val uri = repository.saveToGallery(processed)
            _savedUri.value = uri
            _isSaving.value = false
        }
    }

    private fun scheduleHistoryPush() {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            delay(800)
            pushHistory(_editState.value)
        }
    }

    private fun scheduleProcessing(immediate: Boolean = false) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            if (!immediate) delay(50)
            val source = _sourceBitmap.value ?: return@launch
            val state = _editState.value
            val processed = withContext(Dispatchers.Default) {
                val previewWidth = minOf(source.width, 480)
                val scale = previewWidth.toFloat() / source.width
                val previewHeight = (source.height * scale).toInt()
                val scaledSource = if (scale < 1f) {
                    Bitmap.createScaledBitmap(source, previewWidth, previewHeight, true)
                } else source
                ImageProcessor.process(scaledSource, state)
            }
            _previewBitmap.value = processed
        }
    }

    private fun pushHistory(state: EditState) {
        if (historyIndex < editHistory.size - 1) {
            editHistory.subList(historyIndex + 1, editHistory.size).clear()
        }
        if (editHistory.isEmpty() || editHistory.last() != state) {
            editHistory.add(state)
            historyIndex = editHistory.size - 1
        }
        updateCanUndoRedo()
    }

    private fun updateCanUndoRedo() {
        _canUndo.value = historyIndex > 0
        _canRedo.value = historyIndex < editHistory.size - 1
    }
}
