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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.facegrid.R
import com.example.facegrid.presentation.FaceGridUiState
import com.example.facegrid.presentation.preview.PreviewData
import com.example.facegrid.ui.theme.FaceGridTheme

@Composable
fun ResultScreen(
    result: FaceGridUiState.Result,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onPickAnother: () -> Unit,
    onBack: () -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 70.dp, bottom = 22.dp)
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                "Your story",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${result.output.identities.size} people · ${result.output.identities.sumOf { it.appearanceCount }} moments",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            result.output.videoName?.let { name ->
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (result.output.usesFallbackEmbedding) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "The face-embedding model could not be loaded. Identity grouping may be inaccurate.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(18.dp))
            Image(
                bitmap = result.output.collage.asImageBitmap(),
                contentDescription = "FaceGrid story collage",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp)),
                contentScale = ContentScale.FillWidth
            )
            Spacer(Modifier.height(18.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (result.savedUri == null) "Save to gallery" else "Saved")
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Share")
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPickAnother,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Create another collage")
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            background,
                            background.copy(0.95f),
                            background.copy(0.85f),
                            background.copy(0.45f),
                            Color.Transparent
                        )
                    )
                )
                .padding(vertical = 16.dp, horizontal = 5.dp)
        ) {
            Row(verticalAlignment = CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "go back",
                        modifier = Modifier.size(26.dp)
                    )
                }
                FaceGridBrand()
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
            onPickAnother = {},
            onBack = {})
    }
}
