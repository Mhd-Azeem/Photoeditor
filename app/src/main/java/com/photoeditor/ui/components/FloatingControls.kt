package com.photoeditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photoeditor.ui.theme.iOSYellow

@Composable
fun FloatingControls(
    isAutoEnhanced: Boolean,
    bwActive: Boolean,
    onAutoEnhance: () -> Unit,
    onToggleBW: () -> Unit,
    onCompareStart: () -> Unit,
    onCompareEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(bottom = 20.dp)
    ) {
        // AUTO pill badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF1A1A1A).copy(alpha = 0.88f))
                .border(0.5.dp, Color(0xFF555555), RoundedCornerShape(7.dp))
                .clickable { onAutoEnhance() }
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = "AUTO",
                color = if (isAutoEnhanced) iOSYellow else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularActionButton(
                icon = Icons.Default.AutoFixHigh,
                isActive = false,
                onClick = onAutoEnhance
            )

            // Compare: press-and-hold shows original, release returns to edited view
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141414).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF666666), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onCompareStart()
                                tryAwaitRelease()
                                onCompareEnd()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = "Hold to compare original",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            CircularActionButton(
                icon = Icons.Default.FilterBAndW,
                isActive = bwActive,
                onClick = onToggleBW
            )
        }
    }
}

@Composable
private fun CircularActionButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color(0xFF141414).copy(alpha = 0.85f))
            .border(
                width = 1.dp,
                color = if (isActive) iOSYellow else Color(0xFF666666),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) iOSYellow else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
