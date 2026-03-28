package com.example.arcana.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.arcana.data.model.User
import timber.log.Timber

private const val PLACEHOLDER_AVATAR_URL = "https://via.placeholder.com/150"
private const val INFINITE_SCROLL_DELAY_MS = 1000L
private const val LOAD_MORE_THRESHOLD = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen( // NOSONAR kotlin:S3776
    viewModel: UserViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToUserDetail: (Int) -> Unit = {}
) {
    val uiState by viewModel.output.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var userToUpdate by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var enableInfiniteScroll by remember { mutableStateOf(false) }

    // Handle effects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is UserViewModel.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is UserViewModel.Effect.ShowSuccess -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    // Delay infinite scroll to prevent immediate triggering on first load
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(INFINITE_SCROLL_DELAY_MS)
        enableInfiniteScroll = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("User Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(UserViewModel.Input.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add User")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.allUsers.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading && uiState.users.isNotEmpty(),
                        onRefresh = { viewModel.onEvent(UserViewModel.Input.Refresh) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Summary Header
                            item {
                                SummaryCard(
                                    totalLoaded = uiState.allUsers.size,
                                    currentPage = uiState.currentPage,
                                    totalPages = uiState.totalPages
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            items(
                                items = uiState.allUsers,
                                key = { it.id }
                            ) { user ->
                                UserCard(
                                    user = user,
                                    onClick = { onNavigateToUserDetail(user.id) },
                                    onUpdate = { userToUpdate = user },
                                    onDelete = { userToDelete = user }
                                )
                            }

                            // Loading indicator at the bottom
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }

                    // Scrollbar
                    VerticalScrollbar(
                        listState = listState,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = 4.dp)
                    )
                }

                // Infinite scroll logic
                LaunchedEffect(listState, uiState.isLoadingMore, uiState.currentPage, uiState.totalPages, enableInfiniteScroll) {
                    snapshotFlow {
                        val layoutInfo = listState.layoutInfo
                        val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItemsCount = layoutInfo.totalItemsCount
                        Pair(lastVisibleItemIndex, totalItemsCount)
                    }.collect { (lastVisibleItemIndex, totalItemsCount) ->
                        val shouldLoadMore = lastVisibleItemIndex >= totalItemsCount - LOAD_MORE_THRESHOLD && totalItemsCount > 0
                        if (enableInfiniteScroll && shouldLoadMore && !uiState.isLoadingMore && uiState.currentPage < uiState.totalPages) {
                            Timber.d("Triggering loadNextPage - lastVisible: $lastVisibleItemIndex, total: $totalItemsCount, page: ${uiState.currentPage}/${uiState.totalPages}")
                            viewModel.onEvent(UserViewModel.Input.LoadNextPage)
                        }
                    }
                }
            }
        }
    }

    // Create User Dialog
    if (showAddDialog) {
        UserDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { firstName, lastName, email, avatar ->
                viewModel.onEvent(
                    UserViewModel.Input.CreateUser(
                        User(id = 0, firstName = firstName, lastName = lastName, email = email, avatar = avatar)
                    )
                )
                showAddDialog = false
            }
        )
    }

    // Update User Dialog
    userToUpdate?.let { user ->
        UserDialog(
            user = user,
            onDismiss = { userToUpdate = null },
            onConfirm = { firstName, lastName, email, avatar ->
                viewModel.onEvent(
                    UserViewModel.Input.UpdateUser(
                        user.copy(firstName = firstName, lastName = lastName, email = email, avatar = avatar)
                    )
                )
                userToUpdate = null
            }
        )
    }

    // Delete Confirmation Dialog
    userToDelete?.let { user ->
        ConfirmDialog(
            title = "Delete User",
            message = "Are you sure you want to delete ${user.name}?\n\nThis action cannot be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.onEvent(UserViewModel.Input.DeleteUser(user))
                userToDelete = null
            },
            onDismiss = { userToDelete = null }
        )
    }
}

@Composable
fun UserCard(
    user: User,
    onClick: () -> Unit = {},
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = user.avatar.ifEmpty { PLACEHOLDER_AVATAR_URL },
                contentDescription = "User avatar",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Full Name
                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Email
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                // ID
                Text(
                    text = "ID: ${user.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons
            Row {
                IconButton(onClick = onUpdate) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    totalLoaded: Int,
    currentPage: Int,
    totalPages: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Users Loaded",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$totalLoaded",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Page Progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$currentPage / $totalPages",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun VerticalScrollbar(
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val scrollbarState by remember {
        androidx.compose.runtime.derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
            val itemsHeight = layoutInfo.totalItemsCount * (layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0)

            if (itemsHeight > viewportHeight && layoutInfo.totalItemsCount > 0) {
                val firstVisibleItemIndex = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
                val firstVisibleItemScrollOffset = layoutInfo.visibleItemsInfo.firstOrNull()?.offset ?: 0

                val scrollPercentage = if (itemsHeight > 0) {
                    (firstVisibleItemIndex * (layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0) +
                     kotlin.math.abs(firstVisibleItemScrollOffset)).toFloat() / itemsHeight
                } else 0f

                val scrollbarHeight = if (itemsHeight > 0) {
                    (viewportHeight.toFloat() / itemsHeight * viewportHeight).coerceIn(40f, viewportHeight.toFloat())
                } else 0f

                Triple(true, scrollPercentage * (viewportHeight - scrollbarHeight), scrollbarHeight)
            } else {
                Triple(false, 0f, 0f)
            }
        }
    }

    if (scrollbarState.first) {
        Box(
            modifier = modifier
                .width(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = scrollbarState.second.dp)
                    .width(6.dp)
                    .height(scrollbarState.third.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
        }
    }
}
