package com.example.facegrid.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.facegrid.ui.theme.FaceGridTheme

@Composable
fun PickVideoScreen(onPick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text("FaceGrid", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Turn a portrait video into a shareable people collage.", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(14.dp))
        Text(
            "Everything is processed on-device. Choose a video from your device to detect appearances, group identities, and select the best frame for each person.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(34.dp))
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Choose portrait video") }
        Spacer(Modifier.height(18.dp))
        Text(
            "Best results come from clear, well-lit videos with visible faces.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Pick video screen")
@Composable
private fun PickVideoScreenPreview() {
    FaceGridTheme { PickVideoScreen(onPick = {}) }
}
