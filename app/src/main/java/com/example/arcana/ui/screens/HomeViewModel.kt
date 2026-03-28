package com.example.arcana.ui.screens

import androidx.lifecycle.viewModelScope
import com.example.arcana.core.analytics.AnalyticsScreens
import com.example.arcana.core.analytics.AnalyticsTracker
import com.example.arcana.core.analytics.AnalyticsViewModel
import com.example.arcana.core.analytics.Events
import com.example.arcana.core.analytics.Params
import com.example.arcana.core.analytics.annotations.TrackScreen
import com.example.arcana.core.analytics.trackFlow
import com.example.arcana.core.analytics.trackSync
import com.example.arcana.data.model.User
import com.example.arcana.domain.service.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val SYNC_FAILED_MESSAGE = "Sync failed"

@HiltViewModel
@TrackScreen(AnalyticsScreens.HOME)
class HomeViewModel @Inject constructor(
    private val userService: UserService,
    analyticsTracker: AnalyticsTracker
) : AnalyticsViewModel(analyticsTracker) {

    // ============================================
    // Input - Events from UI to ViewModel
    // ============================================
    sealed interface Input {
        data object LoadUsers : Input
        data object SyncData : Input
        data object Refresh : Input
    }

    // ============================================
    // Output - UI State for binding
    // ============================================
    /**
     * Output - Represents the current UI state for binding
     */
    data class Output(
        val users: List<User> = emptyList(),
        val totalUserCount: Int = 0,
        val isLoading: Boolean = false
    )

    // ============================================
    // Effect - One-time events from ViewModel to UI
    // ============================================
    sealed interface Effect {
        data class ShowSnackbar(val message: String) : Effect
    }

    // ============================================
    // Output & Effect Channels
    // ============================================
    private val _output = MutableStateFlow(Output())
    val output: StateFlow<Output> = _output.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        // Screen view automatically tracked via @TrackScreen annotation
        onEvent(Input.LoadUsers)
        onEvent(Input.SyncData)
    }

    // ============================================
    // Event Handler
    // ============================================
    fun onEvent(input: Input) {
        when (input) {
            is Input.LoadUsers -> loadUsers()
            is Input.SyncData -> syncData()
            is Input.Refresh -> refresh()
        }
    }

    // ============================================
    // Private Methods
    // ============================================
    private fun loadUsers() {
        userService.getUsers()
            .trackFlow(
                analyticsTracker = analyticsTracker,
                eventName = Events.PAGE_LOADED,
                params = mapOf(Params.SCREEN_NAME to AnalyticsScreens.HOME),
                trackPerformance = true,
                trackErrors = true,
                onData = { users -> mapOf(Params.ITEM_COUNT to users.size.toString()) }
            )
            .onEach { users ->
                _output.value = _output.value.copy(users = users)
            }
            .catch { _ ->
                viewModelScope.launch {
                    _effect.send(Effect.ShowSnackbar("Error loading users from local source"))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun syncData() {
        viewModelScope.launch {
            _output.value = _output.value.copy(isLoading = true)

            // Use trackSync extension for automatic sync event tracking
            val syncSuccessful = try {
                trackSync(
                    analyticsTracker = analyticsTracker,
                    screenName = AnalyticsScreens.HOME,
                    trigger = "auto"
                ) {
                    userService.syncUsers()
                }
            } catch (error: Exception) {
                Timber.e(error, "Sync failed")
                _effect.send(Effect.ShowSnackbar(SYNC_FAILED_MESSAGE))
                false
            }

            if (!syncSuccessful) {
                _effect.send(Effect.ShowSnackbar(SYNC_FAILED_MESSAGE))
            }

            // Fetch total user count from API
            val totalCount = userService.getTotalUserCount()
            _output.value = _output.value.copy(
                isLoading = false,
                totalUserCount = totalCount
            )
        }
    }

    private fun refresh() {
        // Invalidate cache and reload
        userService.invalidateCache()
        onEvent(Input.LoadUsers)
        onEvent(Input.SyncData)
    }
}
