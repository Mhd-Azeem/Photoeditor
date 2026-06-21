package com.photoeditor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PhotoEditorColorScheme = darkColorScheme(
    primary = iOSYellow,
    onPrimary = Black,
    secondary = iOSBlue,
    onSecondary = White,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White,
    surfaceVariant = MediumGray,
    onSurfaceVariant = TextGray,
    outline = SubtleGray
)

@Composable
fun PhotoEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PhotoEditorColorScheme,
        typography = PhotoEditorTypography,
        content = content
    )
}
