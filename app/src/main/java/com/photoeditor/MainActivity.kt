package com.photoeditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.photoeditor.ui.navigation.PhotoEditorNavGraph
import com.photoeditor.ui.theme.PhotoEditorTheme
import com.photoeditor.update.AppUpdater

class MainActivity : ComponentActivity() {

    private lateinit var appUpdater: AppUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        appUpdater = AppUpdater(this)

        setContent {
            PhotoEditorTheme {
                PhotoEditorNavGraph()
            }
        }

        appUpdater.checkForUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (::appUpdater.isInitialized) {
            appUpdater.installPendingIfAllowed()
        }
    }

    override fun onDestroy() {
        if (::appUpdater.isInitialized) {
            appUpdater.dispose()
        }
        super.onDestroy()
    }
}
