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

    private val _aiProgress = MutableStateFlow<Float?>(null)
    val aiProgress = _aiProgress.asStateFlow()
    private val _aiStatus = MutableStateFlow("")
    val aiStatus = _aiStatus.asStateFlow()
    private val _aiBefore = MutableStateFlow<Bitmap?>(null)
    val aiBefore = _aiBefore.asStateFlow()
    private val _aiResult = MutableStateFlow<Bitmap?>(null)
    val aiResult = _aiResult.asStateFlow()
    private val _aiExporting = MutableStateFlow(false)
    val aiExporting = _aiExporting.asStateFlow()
    private val aiCancelled = java.util.concurrent.atomic.AtomicBoolean(false)
    private var aiJob: Job? = null

    fun enhance4K() {
        if (aiJob?.isActive == true || _aiResult.value != null) return
        val source = _sourceBitmap.value ?: return
        val state = _editState.value
        aiCancelled.set(false)
        _aiProgress.value = 0f
        _aiStatus.value = "Rendering current edits…"
        aiJob = viewModelScope.launch {
            var before: Bitmap? = null
            var result: Bitmap? = null
            try {
                before = withContext(Dispatchers.Default) {
                    val factor = minOf(1.0, 960.0 / maxOf(source.width, source.height))
                    val normalized = Bitmap.createScaledBitmap(source,
                        (source.width * factor).toInt().coerceAtLeast(1),
                        (source.height * factor).toInt().coerceAtLeast(1), true)
                    try { ImageProcessor.process(normalized, state) }
                    finally { if (normalized !== source) normalized.recycle() }
                }
                result = com.photoeditor.ai.OfflineUpscaler(getApplication()).enhance(requireNotNull(before), aiCancelled) { value, status ->
                    _aiProgress.value = value
                    _aiStatus.value = status
                }
                if (aiCancelled.get()) throw kotlinx.coroutines.CancellationException()
                _aiBefore.value = before
                _aiResult.value = result
                before = null; result = null
            } catch (_: kotlinx.coroutines.CancellationException) {
                _aiStatus.value = "Cancelled"
            } catch (_: OutOfMemoryError) {
                _errorMessage.value = "Not enough memory for AI enhancement. Close other apps and try again."
            } catch (failure: Throwable) {
                _errorMessage.value = "AI enhancement failed: ${failure.message ?: "native engine unavailable"}. No resized substitute was produced."
            } finally {
                if (before !== source) before?.recycle()
                result?.recycle()
                _aiProgress.value = null
            }
        }
    }

    fun cancelEnhancement() {
        aiCancelled.set(true)
        _aiStatus.value = "Cancelling after the current tile…"
    }

    fun dismissAIResult() {
        if (_aiExporting.value) return
        // Compose may still be drawing the previous image this frame; let bitmap GC release it.
        _aiBefore.value = null
        _aiResult.value = null
    }

    fun exportAIResult() {
        val result = _aiResult.value ?: return
        if (_aiExporting.value) return
        viewModelScope.launch {
            _aiExporting.value = true
            try {
                val uri = repository.saveToGallery(result, "AZ_4K_${System.currentTimeMillis()}")
                if (uri == null) _errorMessage.value = "Failed to export enhanced image. You can retry."
                else { _aiStatus.value = "Saved to Pictures/PhotoEditor"; dismissAIResultAfterExport() }
            } finally { _aiExporting.value = false }
        }
    }

    private fun dismissAIResultAfterExport() { _aiBefore.value = null; _aiResult.value = null }
    fun clearError() { _errorMessage.value = null }
    override fun onCleared() { aiCancelled.set(true); super.onCleared() }

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
    private var cachedPreviewSource: Bitmap? = null
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
                cachedPreviewSource = withContext(Dispatchers.Default) {
                    val factor = minOf(1f, 720f / maxOf(bitmap.width, bitmap.height))
                    if (factor < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt().coerceAtLeast(1), (bitmap.height * factor).toInt().coerceAtLeast(1), true) else bitmap
                }
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
        scheduleHistoryPush()
    }

    fun updateAspectRatio(ratio: AspectRatio) {
        _editState.update { current ->
            val crop = current.cropState
            val source = _sourceBitmap.value
            val imageRatio = if (source != null) source.width.toFloat() / source.height else 1f
            val target = when (ratio) {
                AspectRatio.FREE -> null
                AspectRatio.ORIGINAL -> imageRatio
                else -> ratio.ratio
            }
            if (target == null) current.copy(cropState = crop.copy(aspectRatio = ratio))
            else {
                val normalized = target / imageRatio
                val width = crop.right - crop.left
                val height = crop.bottom - crop.top
                val newWidth = minOf(width, height * normalized)
                val newHeight = minOf(height, newWidth / normalized)
                val cx = (crop.left + crop.right) / 2f
                val cy = (crop.top + crop.bottom) / 2f
                current.copy(cropState = crop.copy(left = cx - newWidth / 2f, right = cx + newWidth / 2f, top = cy - newHeight / 2f, bottom = cy + newHeight / 2f, aspectRatio = ratio))
            } }
        scheduleHistoryPush()
    }

    fun updateRotation(rotation: Float) {
        _editState.update { it.copy(cropState = it.cropState.copy(rotation = rotation)) }
        scheduleProcessing()
        scheduleHistoryPush()
    }

    fun resetCrop() {
        _editState.update { it.copy(cropState = CropState()) }
        scheduleProcessing(immediate = true)
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
        historyJob?.cancel()
        processingJob?.cancel()
        val originalState = EditState()
        _editState.value = originalState
        _previewBitmap.value = _sourceBitmap.value

        // Revert is intentionally destructive, matching the confirmation dialog.
        editHistory.clear()
        historyIndex = -1
        pushHistory(originalState)
    }

    fun saveImage() {
        val source = _sourceBitmap.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val processed = withContext(Dispatchers.Default) {
                ImageProcessor.process(source, _editState.value)
            }
            val uri = repository.saveToGallery(processed)
            if (uri != null) {
                _savedUri.value = uri
            } else {
                _errorMessage.value = "Failed to save image"
            }
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
                val previewSource = cachedPreviewSource ?: source
                val previewState = state.copy(cropState = state.cropState.copy(left = 0f, top = 0f, right = 1f, bottom = 1f))
                ImageProcessor.process(previewSource, previewState)
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
