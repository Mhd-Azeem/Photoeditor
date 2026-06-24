package com.photoeditor.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.photoeditor.data.model.AdjustmentType
import com.photoeditor.data.model.EditTab
import com.photoeditor.data.model.config
import com.photoeditor.ui.components.DialRuler
import com.photoeditor.ui.components.EditBottomBar
import com.photoeditor.ui.components.EditTopBar
import com.photoeditor.ui.components.FloatingControls
import com.photoeditor.ui.components.ImagePreview
import com.photoeditor.ui.components.adjust.AdjustPanel
import com.photoeditor.ui.components.crop.CropPanel
import com.photoeditor.ui.components.filters.FiltersPanel
import com.photoeditor.ui.theme.DarkGray
import com.photoeditor.ui.theme.iOSYellow
import com.photoeditor.viewmodel.EditViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditScreen(
    viewModel: EditViewModel,
    onDismiss: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    val previewBitmap by viewModel.previewBitmap.collectAsState()
    val sourceBitmap by viewModel.sourceBitmap.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val selectedAdjustment by viewModel.selectedAdjustment.collectAsState()
    val filterPreviews by viewModel.filterPreviews.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val savedUri by viewModel.savedUri.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    var showRevertDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var bwActive by remember { mutableStateOf(false) }
    var isComparing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(savedUri) {
        if (savedUri != null) onDismiss()
    }

    val dialValue: Float = when (selectedTab) {
        EditTab.ADJUST  -> selectedAdjustment?.let { editState.getAdjustmentValue(it) } ?: 0f
        EditTab.FILTERS -> editState.filterIntensity
        EditTab.CROP    -> editState.cropState.rotation
    }
    val dialRange: ClosedFloatingPointRange<Float> = when (selectedTab) {
        EditTab.ADJUST  -> selectedAdjustment?.config()?.let { it.minValue..it.maxValue } ?: -100f..100f
        EditTab.FILTERS -> 0f..100f
        EditTab.CROP    -> -45f..45f
    }

    fun onDialChange(v: Float) {
        when (selectedTab) {
            EditTab.ADJUST  -> selectedAdjustment?.let { viewModel.updateAdjustment(it, v) }
            EditTab.FILTERS -> viewModel.updateFilterIntensity(v)
            EditTab.CROP    -> viewModel.updateRotation(v)
        }
    }

    if (showRevertDialog) {
        AlertDialog(
            onDismissRequest = { showRevertDialog = false },
            title = { Text("Revert to Original", color = Color.White) },
            text = { Text("All edits will be removed. This cannot be undone.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { viewModel.revertToOriginal(); showRevertDialog = false }) {
                    Text("Revert", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertDialog = false }) {
                    Text("Cancel", color = iOSYellow)
                }
            },
            containerColor = DarkGray
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?", color = Color.White) },
            text = { Text("Your edits will be lost.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onDismiss() }) {
                    Text("Discard", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing", color = iOSYellow)
                }
            },
            containerColor = DarkGray
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        EditTopBar(
            selectedTab = selectedTab,
            canUndo = canUndo,
            canRedo = canRedo,
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onMore = { if (editState.isModified()) showRevertDialog = true },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = iOSYellow,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                ImagePreview(
                    bitmap = if (isComparing) sourceBitmap else previewBitmap,
                    showCropOverlay = selectedTab == EditTab.CROP,
                    cropState = editState.cropState,
                    onCropChanged = { viewModel.updateCrop(it) },
                    modifier = Modifier.fillMaxSize()
                )

                if (selectedTab == EditTab.ADJUST) {
                    FloatingControls(
                        isAutoEnhanced = false,
                        bwActive = bwActive,
                        onAutoEnhance = { viewModel.autoEnhance() },
                        onToggleBW = {
                            bwActive = !bwActive
                            viewModel.updateAdjustment(
                                AdjustmentType.BW_INTENSITY,
                                if (bwActive) 100f else 0f
                            )
                        },
                        onCompareStart = { isComparing = true },
                        onCompareEnd = { isComparing = false },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "panel"
        ) { tab ->
            when (tab) {
                EditTab.ADJUST -> AdjustPanel(
                    editState = editState,
                    selectedAdjustment = selectedAdjustment,
                    onAdjustmentSelected = { viewModel.selectAdjustment(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                EditTab.FILTERS -> FiltersPanel(
                    editState = editState,
                    filterPreviews = filterPreviews,
                    onFilterSelected = { viewModel.selectFilter(it) },
                    onIntensityChanged = { viewModel.updateFilterIntensity(it) },
                    modifier = Modifier.fillMaxWidth()
                )
                EditTab.CROP -> CropPanel(
                    cropState = editState.cropState,
                    onCropChanged = { viewModel.updateCrop(it) },
                    onAspectRatioChanged = { viewModel.updateAspectRatio(it) },
                    onRotationChanged = { viewModel.updateRotation(it) },
                    onReset = { viewModel.resetCrop() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        DialRuler(
            value = dialValue,
            onValueChange = ::onDialChange,
            valueRange = dialRange,
            modifier = Modifier.fillMaxWidth()
        )

        EditBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { viewModel.selectTab(it) },
            onCancel = {
                if (editState.isModified()) showDiscardDialog = true
                else onDismiss()
            },
            onDone = { viewModel.saveImage() },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        )
    }

    if (isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = iOSYellow)
        }
    }
}

private fun shareImage(context: android.content.Context, bitmap: Bitmap) {
    try {
        val cacheDir = File(context.cacheDir, "images").also { it.mkdirs() }
        val file = File(cacheDir, "share_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Image"))
    } catch (_: Exception) {}
}
