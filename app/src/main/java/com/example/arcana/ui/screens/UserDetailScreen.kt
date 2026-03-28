package com.example.arcana.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.arcana.ui.theme.ArcanaGold
import com.example.arcana.ui.theme.ArcanaIndigo
import com.example.arcana.ui.theme.ArcanaPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PLACEHOLDER_AVATAR_URL = "https://via.placeholder.com/150"
private const val NOT_AVAILABLE = "N/A"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(
    viewModel: UserDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.output.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // Handle effects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserDetailViewModel.Effect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is UserDetailViewModel.Effect.ShowSuccess -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is UserDetailViewModel.Effect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    val user = uiState.user ?: return

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit User")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete User",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArcanaIndigo,
                    titleContentColor = ArcanaGold,
                    navigationIconContentColor = ArcanaGold,
                    actionIconContentColor = ArcanaGold
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                ArcanaIndigo,
                                ArcanaPurple.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Avatar at the top
                AsyncImage(
                    model = user.avatar.ifEmpty { PLACEHOLDER_AVATAR_URL },
                    contentDescription = "User avatar",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Name
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = ArcanaGold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // User ID
            Text(
                text = "ID: ${user.id}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // User Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ArcanaGold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // First Name
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "First Name",
                        value = user.firstName.ifEmpty { NOT_AVAILABLE }
                    )

                    // Last Name
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "Last Name",
                        value = user.lastName.ifEmpty { NOT_AVAILABLE }
                    )

                    // Full Name
                    DetailRow(
                        icon = Icons.Default.Person,
                        label = "Full Name",
                        value = user.name
                    )

                    // Email
                    DetailRow(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = user.email.ifEmpty { NOT_AVAILABLE }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Metadata",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ArcanaGold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Version
                    MetadataRow(
                        label = "Version",
                        value = user.version.toString()
                    )

                    // Updated At
                    MetadataRow(
                        label = "Last Updated",
                        value = formatTimestamp(user.updatedAt)
                    )

                    // Avatar URL
                    MetadataRow(
                        label = "Avatar URL",
                        value = user.avatar.ifEmpty { NOT_AVAILABLE }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Edit User Dialog
    if (showEditDialog) {
        UserDialog(
            user = user,
            onDismiss = { showEditDialog = false },
            onConfirm = { firstName, lastName, email, avatar ->
                viewModel.onEvent(
                    UserDetailViewModel.Input.UpdateUser(
                        user.copy(
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            avatar = avatar
                        )
                    )
                )
                showEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete User",
            message = "Are you sure you want to delete ${user.name}?\n\nThis action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.onEvent(UserDetailViewModel.Input.DeleteUser(user))
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ArcanaIndigo,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
