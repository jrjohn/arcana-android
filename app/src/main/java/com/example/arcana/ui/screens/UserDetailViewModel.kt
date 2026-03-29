package com.example.arcana.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.arcana.R
import com.example.arcana.core.analytics.AnalyticsScreens
import com.example.arcana.core.analytics.AnalyticsTracker
import com.example.arcana.core.analytics.AnalyticsViewModel
import com.example.arcana.core.analytics.CrudOperation
import com.example.arcana.core.analytics.Events
import com.example.arcana.core.analytics.Params
import com.example.arcana.core.analytics.trackCrudOperation
import com.example.arcana.core.common.StringProvider
import com.example.arcana.domain.model.User
import com.example.arcana.domain.service.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UserDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userService: UserService,
    private val stringProvider: StringProvider,
    analyticsTracker: AnalyticsTracker
) : AnalyticsViewModel(analyticsTracker) {

    private val userId: Int = checkNotNull(savedStateHandle["userId"])

    // ============================================
    // Input - Events from UI to ViewModel
    // ============================================
    sealed interface Input {
        data class UpdateUser(val user: User) : Input
        data class DeleteUser(val user: User) : Input
    }

    // ============================================
    // Output - UI State for binding
    // ============================================
    data class Output(
        val user: User? = null,
        val isLoading: Boolean = false,
        val isDeleted: Boolean = false
    )

    // ============================================
    // Effect - One-time events from ViewModel to UI
    // ============================================
    sealed interface Effect {
        data class ShowError(val message: String) : Effect
        data class ShowSuccess(val message: String) : Effect
        data object NavigateBack : Effect
    }

    // ============================================
    // Output & Effect Channels
    // ============================================
    private val _output = MutableStateFlow(Output())
    val output: StateFlow<Output> = _output.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        // Use reactive Flow for automatic updates (Optimization #1)
        viewModelScope.launch {
            userService.getUserFlow(userId).collect { user ->
                _output.update { it.copy(user = user, isLoading = false) }
            }
        }
    }

    // ============================================
    // Event Handler
    // ============================================
    fun onEvent(input: Input) {
        when (input) {
            is Input.UpdateUser -> updateUser(input.user)
            is Input.DeleteUser -> deleteUser(input.user)
        }
    }

    // ============================================
    // Private Methods
    // ============================================

    private fun updateUser(user: User) {
        viewModelScope.launch {
            Timber.d("Updating user ${user.id}: name=${user.name}, email=${user.email}, avatar=${user.avatar}")

            // Track user update clicked
            trackEvent(Events.USER_UPDATE_CLICKED, mapOf(
                Params.SCREEN_NAME to AnalyticsScreens.USER_DETAIL,
                Params.USER_ID to user.id.toString()
            ))

            // Use optimistic update for instant UI feedback
            val success = try {
                trackCrudOperation(
                    analyticsTracker = analyticsTracker,
                    operation = CrudOperation.UPDATE,
                    entity = "User",
                    params = mapOf(
                        Params.USER_ID to user.id.toString(),
                        Params.USER_NAME to user.name,
                        Params.USER_EMAIL to user.email,
                        Params.SCREEN_NAME to AnalyticsScreens.USER_DETAIL
                    )
                ) {
                    // updateUser now uses optimistic pattern - UI updates instantly!
                    userService.updateUser(user)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update user ${user.id}")
                false
            }

            if (success) {
                // No need to update local state - getUserFlow() handles it automatically!
                _effect.send(Effect.ShowSuccess(
                    stringProvider.getString(R.string.user_updated_success)
                ))
            } else {
                _effect.send(Effect.ShowError(
                    stringProvider.getString(R.string.user_update_failed)
                ))
            }
        }
    }

    private fun deleteUser(user: User) {
        viewModelScope.launch {
            Timber.d("Deleting user ${user.id}: ${user.name}")

            // Track user delete clicked
            trackEvent(Events.USER_DELETE_CLICKED, mapOf(
                Params.SCREEN_NAME to AnalyticsScreens.USER_DETAIL,
                Params.USER_ID to user.id.toString()
            ))

            // Use optimistic delete for instant UI feedback
            val success = try {
                trackCrudOperation(
                    analyticsTracker = analyticsTracker,
                    operation = CrudOperation.DELETE,
                    entity = "User",
                    params = mapOf(
                        Params.USER_ID to user.id.toString(),
                        Params.USER_NAME to user.name,
                        Params.SCREEN_NAME to AnalyticsScreens.USER_DETAIL
                    )
                ) {
                    // deleteUser now uses optimistic pattern - user disappears instantly!
                    userService.deleteUser(user.id)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete user ${user.id}")
                false
            }

            if (success) {
                _output.update { it.copy(isDeleted = true) }
                _effect.send(Effect.ShowSuccess(
                    stringProvider.getString(R.string.user_deleted_success)
                ))
                _effect.send(Effect.NavigateBack)
                Timber.d("User ${user.id} deleted successfully")
            } else {
                _effect.send(Effect.ShowError(
                    stringProvider.getString(R.string.user_delete_failed)
                ))
            }
        }
    }
}
