package com.example.facegrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.facegrid.presentation.FaceGridApp
import com.example.facegrid.presentation.FaceGridViewModel
import com.example.facegrid.presentation.FaceGridViewModelFactory
import com.example.facegrid.ui.theme.FaceGridTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val factory = remember { FaceGridViewModelFactory(applicationContext) }
            val faceGridViewModel: FaceGridViewModel = viewModel(factory = factory)
            FaceGridTheme { FaceGridApp(faceGridViewModel) }
        }
    }
}
