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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.photoeditor.data.model.EditTab
import com.photoeditor.ui.components.EditTabBar
import com.photoeditor.ui.components.EditTopBar
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

    var showRevertDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(savedUri) {
        if (savedUri != null) {
            onDismiss()
        }
    }

    if (showRevertDialog) {
        AlertDialog(
            onDismissRequest = { showRevertDialog = false },
            title = { Text("Revert to Original", color = Color.White) },
            text = { Text("This will remove all edits. This action cannot be undone.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.revertToOriginal()
                    showRevertDialog = false
                }) { Text("Revert", color = Color.Red) }
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
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDismiss()
                }) { Text("Discard", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing", color = iOSYellow)
                }
            },
            containerColor = DarkGray
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            EditTopBar(
                isModified = editState.isModified(),
                onCancel = {
                    if (editState.isModified()) showDiscardDialog = true
                    else onDismiss()
                },
                onDone = { viewModel.saveImage() },
                onRevert = { if (editState.isModified()) showRevertDialog = true },
                onShare = {
                    previewBitmap?.let { bitmap -> shareImage(context, bitmap) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            )

            // Image preview (takes remaining space between bars)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = iOSYellow)
                } else {
                    ImagePreview(
                        bitmap = previewBitmap,
                        showCropOverlay = selectedTab == EditTab.CROP,
                        cropState = editState.cropState,
                        onCropChanged = { viewModel.updateCrop(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Bottom editing panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .navigationBarsPadding()
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        EditTab.ADJUST -> AdjustPanel(
                            editState = editState,
                            selectedAdjustment = selectedAdjustment,
                            onAdjustmentSelected = { viewModel.selectAdjustment(it) },
                            onValueChanged = { type, value -> viewModel.updateAdjustment(type, value) },
                            onReset = { viewModel.resetAdjustment(it) },
                            onAutoEnhance = { viewModel.autoEnhance() },
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

                // Tab bar
                EditTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = iOSYellow)
            }
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
