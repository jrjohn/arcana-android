package com.example.arcana.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.arcana.ui.theme.ArcanaAmber
import com.example.arcana.ui.theme.ArcanaBackground
import com.example.arcana.ui.theme.ArcanaBackgroundLight
import com.example.arcana.ui.theme.ArcanaCyan
import com.example.arcana.ui.theme.ArcanaGlow
import com.example.arcana.ui.theme.ArcanaGold
import com.example.arcana.ui.theme.ArcanaIndigo
import com.example.arcana.ui.theme.ArcanaPurple
import com.example.arcana.ui.theme.ArcanaViolet

/** A navigation entry on the home screen; NavGraph passes one per feature (tools/new-feature.sh). */
data class HomeEntry(val label: String, val onClick: () -> Unit)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToUserCrud: () -> Unit,
    featureEntries: List<HomeEntry> = emptyList()
) {
    val uiState by viewModel.output.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ArcanaBackground,
                        ArcanaIndigo,
                        ArcanaPurple
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative elements
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 50.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ArcanaViolet.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomStart)
                .padding(bottom = 100.dp, start = 30.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ArcanaCyan.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = ArcanaGold,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ArcanaGlow
                )
            } else {
                // App title
                Text(
                    text = "✨ Arcana ✨",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = ArcanaGold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Mystical User Management",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ArcanaGlow
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Stats card
                Column(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    ArcanaBackgroundLight.copy(alpha = 0.7f),
                                    ArcanaIndigo.copy(alpha = 0.5f)
                                )
                            ),
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total Users",
                        style = MaterialTheme.typography.titleMedium,
                        color = ArcanaCyan
                    )
                    Text(
                        text = "${uiState.totalUserCount}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = ArcanaGold,
                        fontSize = 56.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Loaded: ${uiState.users.size} users",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArcanaGlow
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Action button
                Button(
                    onClick = onNavigateToUserCrud,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(ArcanaAmber, ArcanaGold)
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ArcanaIndigo
                    )
                    Text(
                        text = " Manage Users",
                        color = ArcanaIndigo,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                featureEntries.forEach { entry ->
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = entry.onClick) {
                        Text(text = entry.label, color = ArcanaGlow)
                    }
                }
            }
        }
    }
}
