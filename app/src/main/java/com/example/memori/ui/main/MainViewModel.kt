package com.example.memori.ui.main

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

data class DifficultyOption(
    val id: String,
    val title: String,
    val description: String,
    val rows: Int,
    val columns: Int,
    val timeSeconds: Int,
    val rewardCoins: Int
)

data class MainUiState(
    val player: Player? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface MainEvent {
    data class ShowMessage(val message: String) : MainEvent
    data object SessionExpired : MainEvent
}

class MainViewModel(
    private val application: App
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<MainEvent>(capacity = Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val difficulties: List<DifficultyOption> = listOf(
        DifficultyOption(
            id = "easy",
            title = "Лёгкий",
            description = "4 × 3 карточки · 60 секунд · +30 монет",
            rows = 3,
            columns = 4,
            timeSeconds = 60,
            rewardCoins = 30
        ),
        DifficultyOption(
            id = "medium",
            title = "Средний",
            description = "4 × 4 карточки · 90 секунд · +50 монет",
            rows = 4,
            columns = 4,
            timeSeconds = 90,
            rewardCoins = 50
        ),
        DifficultyOption(
            id = "hard",
            title = "Сложный",
            description = "6 × 4 карточки · 150 секунд · +80 монет",
            rows = 4,
            columns = 6,
            timeSeconds = 150,
            rewardCoins = 80
        ),
        DifficultyOption(
            id = "expert",
            title = "Эксперт",
            description = "6 × 6 карточек · 210 секунд · +120 монет",
            rows = 6,
            columns = 6,
            timeSeconds = 210,
            rewardCoins = 120
        )
    )

    init {
        loadPlayerAndApplyDailyBonus()
    }

    fun refreshPlayer() {
        loadPlayerAndApplyDailyBonus()
    }

    private fun loadPlayerAndApplyDailyBonus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            val playerId = application.preferences.getLong(
                App.CURRENT_PLAYER_ID_KEY,
                INVALID_PLAYER_ID
            )

            if (playerId == INVALID_PLAYER_ID) {
                _uiState.value = MainUiState(
                    isLoading = false,
                    errorMessage = "Игрок не выбран"
                )
                eventChannel.send(MainEvent.SessionExpired)
                return@launch
            }

            val player = runCatching {
                application.playerRepository.getById(playerId)
            }.getOrElse {
                _uiState.value = MainUiState(
                    isLoading = false,
                    errorMessage = "Не удалось загрузить профиль"
                )
                return@launch
            }

            if (player == null) {
                application.preferences.edit()
                    .remove(App.CURRENT_PLAYER_ID_KEY)
                    .apply()

                _uiState.value = MainUiState(
                    isLoading = false,
                    errorMessage = "Игрок не найден"
                )
                eventChannel.send(MainEvent.SessionExpired)
                return@launch
            }

            val now = System.currentTimeMillis()
            val shouldGiveBonus = player.lastBonusDate == 0L ||
                    now - player.lastBonusDate > DAILY_BONUS_INTERVAL_MS

            val updatedPlayer = if (shouldGiveBonus) {
                application.playerRepository.addCoins(player.id, DAILY_BONUS_COINS)
                application.playerRepository.updateBonusDate(player.id, now)

                eventChannel.send(
                    MainEvent.ShowMessage("Ежедневный бонус: +$DAILY_BONUS_COINS монет")
                )

                application.playerRepository.getById(player.id) ?: player.copy(
                    coins = player.coins + DAILY_BONUS_COINS,
                    lastBonusDate = now
                )
            } else {
                player
            }

            _uiState.value = MainUiState(
                player = updatedPlayer,
                isLoading = false
            )
        }
    }

    class Factory(
        private val application: App
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MainViewModel::class.java))
            return MainViewModel(application) as T
        }
    }

    private companion object {
        const val INVALID_PLAYER_ID = -1L
        const val DAILY_BONUS_COINS = 50
        const val DAILY_BONUS_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }
}