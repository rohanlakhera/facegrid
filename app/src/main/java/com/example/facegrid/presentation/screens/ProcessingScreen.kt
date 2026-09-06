package com.example.facegrid.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.facegrid.domain.model.ProcessingStage
import com.example.facegrid.domain.model.ProgressUpdate
import com.example.facegrid.ui.theme.FaceGridTheme

@Composable
fun ProcessingScreen(progress: ProgressUpdate) {
    val fraction = if (progress.total <= 0) 0f else (progress.current.toFloat() / progress.total).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxSize().padding(28.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(24.dp))
        Text("Building your grid", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(progress.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(stageLabel(progress.stage), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private fun stageLabel(stage: ProcessingStage): String = when (stage) {
    ProcessingStage.EXTRACTING -> "Reading video frames"
    ProcessingStage.DETECTING -> "Detecting faces"
    ProcessingStage.EMBEDDING -> "Creating face embeddings"
    ProcessingStage.CLUSTERING -> "Grouping people"
    ProcessingStage.RENDERING -> "Creating collage"
}

@Preview(showBackground = true, showSystemUi = true, name = "Processing screen")
@Composable
private fun ProcessingScreenPreview() {
    FaceGridTheme {
        ProcessingScreen(
            ProgressUpdate(
                stage = ProcessingStage.DETECTING,
                current = 120,
                total = 600,
                message = "Detecting faces: frame 120/600"
            )
        )
    }
}
