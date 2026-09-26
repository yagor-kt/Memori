package com.example.memori.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memori.App
import com.example.memori.data.local.dao.LeaderboardEntry
import com.example.memori.data.local.entity.Achievement
import com.example.memori.data.local.entity.Player
import com.example.memori.data.repository.AchievementRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class AchievementUiModel(
    val achievement: Achievement,
    val unlockedAt: Long?
) {
    val isUnlocked: Boolean
        get() = unlockedAt != null
}

data class SettingsUiState(
    val player: Player? = null,
    val achievements: List<AchievementUiModel> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val selectedDifficulty: String = "easy",
    val isLoading: Boolean = true,
    val isResetting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface SettingsEvent {
    data object ChangePlayer : SettingsEvent
    data class ShowMessage(val message: String) : SettingsEvent
}

class SettingsViewModel(
    private val application: App
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        loadSettings()
    }

    fun selectDifficulty(difficulty: String) {
        if (difficulty !in DIFFICULTIES) return

        _uiState.value = _uiState.value.copy(selectedDifficulty = difficulty)
        loadLeaderboard(difficulty)
    }

    fun switchPlayer() {
        application.preferences.edit()
            .remove(App.CURRENT_PLAYER_ID_KEY)
            .apply()

        viewModelScope.launch {
            eventChannel.send(SettingsEvent.ChangePlayer)
        }
    }

    fun resetProgress() {
        val player = _uiState.value.player ?: return
        if (_uiState.value.isResetting) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isResetting = true,
                errorMessage = null
            )

            runCatching {
                application.playerRepository.resetProgress(player.id)
            }.onSuccess {
                eventChannel.send(SettingsEvent.ShowMessage("Прогресс сброшен"))
                loadSettings()
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    errorMessage = "Не удалось сбросить прогресс"
                )
            }
        }
    }

    fun refresh() {
        loadSettings()
    }

    private fun loadSettings() {
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
                _uiState.value = _uiState.value.copy(
                    player = null,
                    isLoading = false
                )
                eventChannel.send(SettingsEvent.ChangePlayer)
                return@launch
            }

            runCatching {
                application.achievementRepository.ensureDefaultAchievements()

                val player = application.playerRepository.getById(playerId)
                    ?: error("Игрок не найден")

                val achievements = application.achievementRepository.getAll()
                val unlocked = application.achievementRepository
                    .getUnlockedForPlayer(playerId)
                    .associateBy { it.achievementId }

                val achievementModels = achievements.map { achievement ->
                    AchievementUiModel(
                        achievement = achievement,
                        unlockedAt = unlocked[achievement.id]?.unlockedAt
                    )
                }

                val difficulty = _uiState.value.selectedDifficulty
                val leaderboard = application.gameRepository
                    .getLeaderboard(difficulty)

                SettingsData(
                    player = player,
                    achievements = achievementModels,
                    leaderboard = leaderboard,
                    difficulty = difficulty
                )
            }.onSuccess { data ->
                _uiState.value = SettingsUiState(
                    player = data.player,
                    achievements = data.achievements,
                    leaderboard = data.leaderboard,
                    selectedDifficulty = data.difficulty,
                    isLoading = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isResetting = false,
                    errorMessage = "Не удалось загрузить настройки"
                )
            }
        }
    }

    private fun loadLeaderboard(difficulty: String) {
        viewModelScope.launch {
            runCatching {
                application.gameRepository.getLeaderboard(difficulty)
            }.onSuccess { entries ->
                _uiState.value = _uiState.value.copy(
                    leaderboard = entries,
                    errorMessage = null
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    leaderboard = emptyList(),
                    errorMessage = "Не удалось загрузить таблицу лидеров"
                )
            }
        }
    }

    private data class SettingsData(
        val player: Player,
        val achievements: List<AchievementUiModel>,
        val leaderboard: List<LeaderboardEntry>,
        val difficulty: String
    )

    class Factory(
        private val application: App
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(SettingsViewModel::class.java))
            return SettingsViewModel(application) as T
        }
    }

    companion object {
        const val INVALID_PLAYER_ID = -1L

        val DIFFICULTIES = listOf(
            "easy",
            "medium",
            "hard",
            "expert"
        )

        fun difficultyTitle(difficulty: String): String {
            return when (difficulty) {
                "easy" -> "Лёгкий"
                "medium" -> "Средний"
                "hard" -> "Сложный"
                "expert" -> "Эксперт"
                else -> difficulty
            }
        }
    }
}