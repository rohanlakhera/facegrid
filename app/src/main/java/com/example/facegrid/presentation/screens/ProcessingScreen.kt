package com.example.facegrid.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.facegrid.domain.model.ProcessingStage
import com.example.facegrid.domain.model.ProgressUpdate
import com.example.facegrid.ui.theme.FaceGridTheme

@Composable
fun ProcessingScreen(
    progress: ProgressUpdate,
    onCancel: () -> Unit = {}
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val targetProgress = processingProgress(progress)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 280),
        label = "processing progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FaceGridBrand()
        Spacer(Modifier.height(54.dp))
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(96.dp),
            strokeWidth = 7.dp,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(30.dp))
        Text(
            processingTitle(progress.stage),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            processingDescription(progress.stage),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "This may take a little while.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(18.dp))
        TextButton(
            onClick = { showCancelDialog = true },
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Cancel")
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel collage generation?") },
            text = {
                Text("The video will stop processing and no collage will be created.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel generation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep processing")
                }
            }
        )
    }
}

private fun processingProgress(progress: ProgressUpdate): Float {
    val phaseProgress = if (progress.total > 0) {
        (progress.current.toFloat() / progress.total).coerceIn(0f, 1f)
    } else {
        0f
    }

    return when (progress.stage) {
        ProcessingStage.EXTRACTING,
        ProcessingStage.DETECTING -> phaseProgress * 0.65f

        ProcessingStage.EMBEDDING -> 0.65f + phaseProgress * 0.25f
        ProcessingStage.CLUSTERING -> 0.90f + phaseProgress * 0.10f
    }
}

private fun processingTitle(stage: ProcessingStage): String = when (stage) {
    ProcessingStage.EXTRACTING,
    ProcessingStage.DETECTING -> "Discovering the moments"

    ProcessingStage.EMBEDDING -> "Recognizing familiar faces"
    ProcessingStage.CLUSTERING -> "Putting it all together"
}

private fun processingDescription(stage: ProcessingStage): String = when (stage) {
    ProcessingStage.EXTRACTING,
    ProcessingStage.DETECTING -> "Looking through your video and finding the people who make it special."

    ProcessingStage.EMBEDDING -> "Comparing appearances so each person gets their own place."
    ProcessingStage.CLUSTERING -> "Bringing each person’s best moments together and giving your portrait story its final shape."
}

@Preview(showBackground = true, showSystemUi = true, name = "Processing screen")
@Composable
private fun ProcessingScreenPreview() {
    FaceGridTheme {
        ProcessingScreen(ProgressUpdate(ProcessingStage.DETECTING, 0, 0, ""))
    }
}
