package com.photoeditor.ui.components.filters

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photoeditor.data.model.EditState
import com.photoeditor.data.model.FilterType
import com.photoeditor.ui.theme.SubtleGray
import com.photoeditor.ui.theme.TextGray
import com.photoeditor.ui.theme.iOSYellow
import kotlin.math.roundToInt

@Composable
fun FiltersPanel(
    editState: EditState,
    filterPreviews: Map<FilterType, Bitmap>,
    onFilterSelected: (FilterType) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Filter name + intensity label (centered, above the dial)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = editState.selectedFilter.displayName.uppercase(),
                color = if (editState.selectedFilter != FilterType.ORIGINAL) Color.White else SubtleGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            if (editState.selectedFilter != FilterType.ORIGINAL) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${editState.filterIntensity.roundToInt()}%",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        }

        // Filter thumbnail carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(FilterType.entries) { filterType ->
                FilterThumbnail(
                    filterType = filterType,
                    previewBitmap = filterPreviews[filterType],
                    isSelected = editState.selectedFilter == filterType,
                    onClick = { onFilterSelected(filterType) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun FilterThumbnail(
    filterType: FilterType,
    previewBitmap: Bitmap?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isSelected) 2.5.dp else 0.dp,
                    color = if (isSelected) iOSYellow else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .background(Color(0xFF1C1C1E)),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap.asImageBitmap(),
                    contentDescription = filterType.displayName,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(RoundedCornerShape(if (isSelected) 7.5.dp else 10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .background(Color(0xFF2C2C2E), RoundedCornerShape(10.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = filterType.displayName,
            color = if (isSelected) iOSYellow else TextGray,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
