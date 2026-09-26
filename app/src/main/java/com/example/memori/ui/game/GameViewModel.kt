package com.example.memori.ui.game

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.memori.App
import com.example.memori.data.local.entity.GameResult
import com.example.memori.domain.model.Card
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameStatus {
    READY,
    PLAYING,
    PAUSED,
    WON,
    LOST,
    ERROR
}

data class GameUiState(
    val difficulty: String,
    val difficultyTitle: String,
    val rows: Int,
    val columns: Int,
    val rewardCoins: Int,
    val cards: List<Card> = emptyList(),
    val moves: Int = 0,
    val foundPairs: Int = 0,
    val totalPairs: Int = 0,
    val timeLeftSeconds: Int = 0,
    val totalTimeSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val coins: Int = 0,
    val status: GameStatus = GameStatus.READY,
    val isLocked: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val resultSaved: Boolean = false
) {
    val isWon: Boolean
        get() = status == GameStatus.WON

    val isLost: Boolean
        get() = status == GameStatus.LOST
}

class GameViewModel(
    private val application: App,
    difficulty: String
) : ViewModel() {

    private val difficultyConfig = DIFFICULTIES[difficulty]

    private val _uiState = MutableStateFlow(
        if (difficultyConfig == null) {
            GameUiState(
                difficulty = difficulty,
                difficultyTitle = "Неизвестная сложность",
                rows = 0,
                columns = 0,
                rewardCoins = 0,
                status = GameStatus.ERROR,
                isLoading = false,
                errorMessage = "Не удалось определить сложность игры"
            )
        } else {
            GameUiState(
                difficulty = difficulty,
                difficultyTitle = difficultyConfig.title,
                rows = difficultyConfig.rows,
                columns = difficultyConfig.columns,
                rewardCoins = difficultyConfig.rewardCoins,
                totalPairs = difficultyConfig.rows * difficultyConfig.columns / 2,
                timeLeftSeconds = difficultyConfig.timeSeconds,
                totalTimeSeconds = difficultyConfig.timeSeconds
            )
        }
    )

    val uiState = _uiState.asStateFlow()

    private var playerId: Long? = null
    private var timerJob: Job? = null
    private var firstCardId: Int? = null
    private var hasStarted = false
    private var resultSaveStarted = false

    init {
        if (difficultyConfig != null) {
            prepareNewGame()
            loadPlayer()
        }
    }

    fun onCardTapped(cardId: Int) {
        val state = _uiState.value

        if (state.status != GameStatus.READY && state.status != GameStatus.PLAYING) return
        if (state.isLoading || state.isLocked) return

        val tappedCard = state.cards.firstOrNull { it.id == cardId } ?: return
        if (tappedCard.isFlipped || tappedCard.isMatched) return

        if (!hasStarted) {
            hasStarted = true
            _uiState.update { it.copy(status = GameStatus.PLAYING) }
            startTimer()
        }

        val currentFirstCardId = firstCardId

        if (currentFirstCardId == null) {
            firstCardId = cardId
            _uiState.update { current ->
                current.copy(
                    cards = current.cards.map { card ->
                        if (card.id == cardId) card.copy(isFlipped = true) else card
                    }
                )
            }
            return
        }

        val firstCard = state.cards.firstOrNull { it.id == currentFirstCardId } ?: run {
            firstCardId = null
            onCardTapped(cardId)
            return
        }

        firstCardId = null

        val flippedCards = state.cards.map { card ->
            if (card.id == firstCard.id || card.id == cardId) {
                card.copy(isFlipped = true)
            } else {
                card
            }
        }

        val isPair = firstCard.pairId == tappedCard.pairId

        _uiState.update { current ->
            current.copy(
                cards = flippedCards,
                moves = current.moves + 1,
                isLocked = !isPair
            )
        }

        if (isPair) {
            val matchedCards = flippedCards.map { card ->
                if (card.id == firstCard.id || card.id == cardId) {
                    card.copy(isMatched = true)
                } else {
                    card
                }
            }

            val updated = _uiState.value.copy(
                cards = matchedCards,
                foundPairs = _uiState.value.foundPairs + 1,
                isLocked = false
            )
            _uiState.value = updated

            if (updated.foundPairs == updated.totalPairs) {
                finishGame(won = true)
            }
        } else {
            viewModelScope.launch {
                delay(MISMATCH_DELAY_MS)

                if (_uiState.value.status == GameStatus.PLAYING) {
                    _uiState.update { current ->
                        current.copy(
                            cards = current.cards.map { card ->
                                if (card.id == firstCard.id || card.id == cardId) {
                                    card.copy(isFlipped = false)
                                } else {
                                    card
                                }
                            },
                            isLocked = false
                        )
                    }
                    vibrate()
                }
            }
        }
    }

    fun showPairHint() {
        val state = _uiState.value
        if (!canUseHint(state)) return

        val player = playerId ?: return
        val hintPair = state.cards
            .filter { !it.isMatched && !it.isFlipped }
            .groupBy { it.pairId }
            .values
            .firstOrNull { it.size == 2 }
            ?: return

        viewModelScope.launch {
            val current = _uiState.value
            if (!canUseHint(current) || current.coins < PAIR_HINT_COST) return@launch

            _uiState.update { it.copy(isLocked = true) }

            runCatching {
                application.playerRepository.updateCoins(
                    playerId = player,
                    coins = current.coins - PAIR_HINT_COST
                )
            }.onFailure {
                _uiState.update { stateNow ->
                    stateNow.copy(
                        isLocked = false,
                        errorMessage = "Не удалось списать монеты за подсказку"
                    )
                }
                return@launch
            }

            val hintedIds = hintPair.map { it.id }.toSet()

            _uiState.update { stateNow ->
                stateNow.copy(
                    coins = stateNow.coins - PAIR_HINT_COST,
                    cards = stateNow.cards.map { card ->
                        if (card.id in hintedIds) card.copy(isFlipped = true) else card
                    },
                    errorMessage = null
                )
            }

            delay(PAIR_HINT_DURATION_MS)

            if (_uiState.value.status == GameStatus.READY ||
                _uiState.value.status == GameStatus.PLAYING
            ) {
                _uiState.update { stateNow ->
                    stateNow.copy(
                        cards = stateNow.cards.map { card ->
                            if (card.id in hintedIds && !card.isMatched) {
                                card.copy(isFlipped = false)
                            } else {
                                card
                            }
                        },
                        isLocked = false
                    )
                }
            }
        }
    }

    fun addTimeHint() {
        val state = _uiState.value
        if (!canUseHint(state)) return

        val player = playerId ?: return

        viewModelScope.launch {
            val current = _uiState.value
            if (!canUseHint(current) || current.coins < EXTRA_TIME_COST) return@launch

            _uiState.update { it.copy(isLocked = true) }

            runCatching {
                application.playerRepository.updateCoins(
                    playerId = player,
                    coins = current.coins - EXTRA_TIME_COST
                )
            }.onFailure {
                _uiState.update { stateNow ->
                    stateNow.copy(
                        isLocked = false,
                        errorMessage = "Не удалось списать монеты за дополнительное время"
                    )
                }
                return@launch
            }

            _uiState.update { stateNow ->
                stateNow.copy(
                    coins = stateNow.coins - EXTRA_TIME_COST,
                    timeLeftSeconds = stateNow.timeLeftSeconds + EXTRA_TIME_SECONDS,
                    totalTimeSeconds = stateNow.totalTimeSeconds + EXTRA_TIME_SECONDS,
                    isLocked = false,
                    errorMessage = null
                )
            }
        }
    }

    fun pauseGame() {
        val state = _uiState.value
        if (state.status != GameStatus.READY && state.status != GameStatus.PLAYING) return
        if (state.isLocked) return

        _uiState.update { it.copy(status = GameStatus.PAUSED) }
    }

    fun resumeGame() {
        if (_uiState.value.status != GameStatus.PAUSED) return

        if (hasStarted) {
            _uiState.update { it.copy(status = GameStatus.PLAYING) }
            startTimer()
        } else {
            _uiState.update { it.copy(status = GameStatus.READY) }
        }
    }

    fun restartGame() {
        timerJob?.cancel()
        timerJob = null
        firstCardId = null
        hasStarted = false
        resultSaveStarted = false

        prepareNewGame()

        val player = playerId
        if (player == null) {
            loadPlayer()
        }
    }

    private fun prepareNewGame() {
        val config = difficultyConfig ?: return
        val pairsCount = config.rows * config.columns / 2

        if (application.cardNames.size < pairsCount) {
            _uiState.update {
                it.copy(
                    status = GameStatus.ERROR,
                    isLoading = false,
                    errorMessage = "Недостаточно изображений для выбранной сложности"
                )
            }
            return
        }

        val selectedNames = application.cardNames
            .shuffled()
            .take(pairsCount)

        val deck = selectedNames
            .flatMapIndexed { pairId, name ->
                listOf(
                    Card(id = pairId * 2, pairId = pairId, name = name),
                    Card(id = pairId * 2 + 1, pairId = pairId, name = name)
                )
            }
            .shuffled()

        _uiState.update { current ->
            current.copy(
                cards = deck,
                moves = 0,
                foundPairs = 0,
                totalPairs = pairsCount,
                timeLeftSeconds = config.timeSeconds,
                totalTimeSeconds = config.timeSeconds,
                elapsedSeconds = 0,
                status = GameStatus.READY,
                isLocked = false,
                isLoading = true,
                errorMessage = null,
                resultSaved = false
            )
        }
    }

    private fun loadPlayer() {
        viewModelScope.launch {
            val savedPlayerId = application.preferences.getLong(
                App.CURRENT_PLAYER_ID_KEY,
                INVALID_PLAYER_ID
            )

            if (savedPlayerId == INVALID_PLAYER_ID) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        status = GameStatus.ERROR,
                        errorMessage = "Игрок не выбран. Вернитесь в меню и войдите снова."
                    )
                }
                return@launch
            }

            val player = runCatching {
                application.playerRepository.getById(savedPlayerId)
            }.getOrNull()

            if (player == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        status = GameStatus.ERROR,
                        errorMessage = "Не удалось загрузить профиль игрока"
                    )
                }
                return@launch
            }

            playerId = player.id
            _uiState.update {
                it.copy(
                    coins = player.coins,
                    isLoading = false
                )
            }
        }
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)

                val current = _uiState.value
                if (current.status != GameStatus.PLAYING) break

                if (current.timeLeftSeconds <= 1) {
                    _uiState.update {
                        it.copy(
                            timeLeftSeconds = 0,
                            elapsedSeconds = it.elapsedSeconds + 1,
                            status = GameStatus.LOST
                        )
                    }
                    finishGame(won = false)
                    break
                }

                _uiState.update {
                    it.copy(
                        timeLeftSeconds = it.timeLeftSeconds - 1,
                        elapsedSeconds = it.elapsedSeconds + 1
                    )
                }
            }
        }
    }

    private fun finishGame(won: Boolean) {
        if (resultSaveStarted) return
        resultSaveStarted = true

        timerJob?.cancel()
        timerJob = null

        _uiState.update {
            it.copy(
                status = if (won) GameStatus.WON else GameStatus.LOST,
                isLocked = true
            )
        }

        viewModelScope.launch {
            val current = _uiState.value
            val currentPlayerId = playerId

            if (currentPlayerId == null) {
                _uiState.update {
                    it.copy(
                        isLocked = false,
                        resultSaved = false,
                        errorMessage = "Результат не сохранён: игрок не найден"
                    )
                }
                return@launch
            }

            var saveError: String? = null

            if (won) {
                runCatching {
                    application.playerRepository.addCoins(
                        currentPlayerId,
                        current.rewardCoins
                    )
                }.onFailure {
                    saveError = "Не удалось начислить награду за победу"
                }

                if (saveError == null) {
                    _uiState.update {
                        it.copy(coins = it.coins + current.rewardCoins)
                    }
                }
            }

            runCatching {
                application.gameRepository.saveResult(
                    GameResult(
                        playerId = currentPlayerId,
                        difficulty = current.difficulty,
                        timeSpent = current.elapsedSeconds,
                        moves = current.moves,
                        won = won
                    )
                )
            }.onFailure {
                saveError = "Не удалось сохранить результат игры"
            }

            _uiState.update {
                it.copy(
                    isLocked = false,
                    resultSaved = saveError == null,
                    errorMessage = saveError
                )
            }
        }
    }

    private fun canUseHint(state: GameUiState): Boolean {
        return !state.isLoading &&
                !state.isLocked &&
                (state.status == GameStatus.READY || state.status == GameStatus.PLAYING)
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val duration = MISMATCH_VIBRATION_MS

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                        as? VibratorManager
                manager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(
                        duration,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE)
                        as? Vibrator

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(
                            duration,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    vibrator?.vibrate(duration)
                }
            }
        }
    }

    class Factory(
        private val application: App,
        private val difficulty: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameViewModel::class.java))
            return GameViewModel(application, difficulty) as T
        }
    }

    private data class DifficultyConfig(
        val title: String,
        val rows: Int,
        val columns: Int,
        val timeSeconds: Int,
        val rewardCoins: Int
    )

    private companion object {
        const val INVALID_PLAYER_ID = -1L

        const val PAIR_HINT_COST = 50
        const val PAIR_HINT_DURATION_MS = 1_000L

        const val EXTRA_TIME_COST = 40
        const val EXTRA_TIME_SECONDS = 15

        const val MISMATCH_DELAY_MS = 800L
        const val MISMATCH_VIBRATION_MS = 60L

        val DIFFICULTIES = mapOf(
            "easy" to DifficultyConfig(
                title = "Лёгкий",
                rows = 3,
                columns = 4,
                timeSeconds = 60,
                rewardCoins = 30
            ),
            "medium" to DifficultyConfig(
                title = "Средний",
                rows = 4,
                columns = 4,
                timeSeconds = 90,
                rewardCoins = 50
            ),
            "hard" to DifficultyConfig(
                title = "Сложный",
                rows = 4,
                columns = 6,
                timeSeconds = 150,
                rewardCoins = 80
            ),
            "expert" to DifficultyConfig(
                title = "Эксперт",
                rows = 6,
                columns = 6,
                timeSeconds = 210,
                rewardCoins = 120
            )
        )
    }
}