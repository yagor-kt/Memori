package com.example.memori.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memori.App
import com.example.memori.data.local.entity.Player
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val name: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface LoginEvent {
    data object LoginSucceeded : LoginEvent
}

class LoginViewModel(
    private val application: App
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<LoginEvent>(capacity = Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        restoreSession()
    }

    fun onNameChanged(name: String) {
        if (name.length <= MAX_NAME_LENGTH) {
            _uiState.value = _uiState.value.copy(
                name = name,
                errorMessage = null
            )
        }
    }

    fun login() {
        val name = _uiState.value.name.trim()

        if (name.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Введите имя"
            )
            return
        }

        if (name.length > MAX_NAME_LENGTH) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Имя должно содержать не более 20 символов"
            )
            return
        }

        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            runCatching {
                application.playerRepository.loginOrCreate(name)
            }.onSuccess { player ->
                saveCurrentPlayer(player)
                _uiState.value = _uiState.value.copy(isLoading = false)
                eventChannel.send(LoginEvent.LoginSucceeded)
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Не удалось войти. Попробуйте ещё раз."
                )
            }
        }
    }

    private fun restoreSession() {
        val savedPlayerId = application.preferences
            .getLong(App.CURRENT_PLAYER_ID_KEY, INVALID_PLAYER_ID)

        if (savedPlayerId == INVALID_PLAYER_ID) return

        viewModelScope.launch {
            val player = runCatching {
                application.playerRepository.getById(savedPlayerId)
            }.getOrNull()

            if (player != null) {
                eventChannel.send(LoginEvent.LoginSucceeded)
            } else {
                application.preferences.edit()
                    .remove(App.CURRENT_PLAYER_ID_KEY)
                    .apply()
            }
        }
    }

    private fun saveCurrentPlayer(player: Player) {
        application.preferences.edit()
            .putLong(App.CURRENT_PLAYER_ID_KEY, player.id)
            .apply()
    }

    class Factory(
        private val application: App
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(LoginViewModel::class.java))
            return LoginViewModel(application) as T
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 20
        const val INVALID_PLAYER_ID = -1L
    }
}