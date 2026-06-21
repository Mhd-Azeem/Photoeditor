package com.photoeditor.ui.navigation

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photoeditor.ui.screens.EditScreen
import com.photoeditor.ui.screens.HomeScreen
import com.photoeditor.viewmodel.EditViewModel
import com.photoeditor.viewmodel.EditViewModelFactory

@Composable
fun PhotoEditorNavGraph() {
    val navController = rememberNavController()
    var selectedUriString by rememberSaveable { mutableStateOf<String?>(null) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onImageSelected = { uri ->
                    selectedUriString = uri.toString()
                    navController.navigate("edit")
                }
            )
        }
        composable("edit") {
            val uriString = selectedUriString
            if (uriString != null) {
                val context = LocalContext.current
                val viewModel: EditViewModel = viewModel(
                    factory = EditViewModelFactory(
                        application = context.applicationContext as Application,
                        imageUri = Uri.parse(uriString)
                    )
                )
                EditScreen(
                    viewModel = viewModel,
                    onDismiss = { navController.navigateUp() }
                )
            }
        }
    }
}
