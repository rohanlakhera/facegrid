package com.example.facegrid.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.facegrid.presentation.preview.PreviewData
import com.example.facegrid.presentation.FaceGridUiState
import com.example.facegrid.ui.theme.FaceGridTheme

@Composable
fun ResultScreen(
    result: FaceGridUiState.Result,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onPickAnother: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("Your grid", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${result.output.identities.size} people · ${result.output.identities.sumOf { it.appearanceCount }} appearances", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (result.output.usesFallbackEmbedding) {
            Spacer(Modifier.height(8.dp))
            Text(
                "The face-embedding model could not be loaded. Identity grouping may be inaccurate.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(18.dp))
        Image(
            bitmap = result.output.collage.asImageBitmap(),
            contentDescription = "FaceGrid collage",
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.FillWidth
        )
        Spacer(Modifier.height(18.dp))
        result.output.identities.forEach { identity ->
            IdentityRow(identity.id, identity.appearanceCount)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSave, modifier = Modifier.weight(1f)) { Text(if (result.savedUri == null) "Save" else "Saved") }
            OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) { Text("Share") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onPickAnother, modifier = Modifier.fillMaxWidth()) { Text("Choose another video") }
    }
}

@Composable
private fun IdentityRow(id: Int, appearances: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text("$id", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Person $id", fontWeight = FontWeight.SemiBold)
                Text("$appearances appearance${if (appearances == 1) "" else "s"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Result screen")
@Composable
private fun ResultScreenPreview() {
    FaceGridTheme {
        ResultScreen(
            result = PreviewData.result(),
            onSave = {},
            onShare = {},
            onPickAnother = {}
        )
    }
}
