package com.example.memori.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.memori.R
import com.example.memori.domain.model.Card as MemoryCard
import java.util.Locale

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onExitToMenu: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.status == GameStatus.ERROR -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.errorMessage
                            ?: "Не удалось запустить игру",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onExitToMenu) {
                        Text("В меню")
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp)
                ) {
                    GameHeader(
                        title = state.difficultyTitle,
                        coins = state.coins,
                        timeLeftSeconds = state.timeLeftSeconds,
                        totalTimeSeconds = state.totalTimeSeconds,
                        moves = state.moves,
                        foundPairs = state.foundPairs,
                        totalPairs = state.totalPairs,
                        canPause = !state.isLocked &&
                                (state.status == GameStatus.READY ||
                                        state.status == GameStatus.PLAYING),
                        onPause = viewModel::pauseGame
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(state.columns),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            vertical = 8.dp,
                            horizontal = 2.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = state.cards,
                            key = { it.id }
                        ) { card ->
                            MemoryCardView(
                                card = card,
                                enabled = !state.isLocked &&
                                        (state.status == GameStatus.READY ||
                                                state.status == GameStatus.PLAYING),
                                onClick = { viewModel.onCardTapped(card.id) }
                            )
                        }
                    }

                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    HintButtons(
                        coins = state.coins,
                        enabled = !state.isLocked &&
                                (state.status == GameStatus.READY ||
                                        state.status == GameStatus.PLAYING),
                        onShowPair = viewModel::showPairHint,
                        onAddTime = viewModel::addTimeHint
                    )
                }
            }
        }
    }

    when (state.status) {
        GameStatus.PAUSED -> {
            AlertDialog(
                onDismissRequest = viewModel::resumeGame,
                title = { Text("Пауза") },
                text = { Text("Игра приостановлена.") },
                confirmButton = {
                    Button(onClick = viewModel::resumeGame) {
                        Text("Продолжить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onExitToMenu) {
                        Text("Выйти в меню")
                    }
                }
            )
        }

        GameStatus.WON -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Победа!") },
                text = {
                    Column {
                        Text("Вы нашли все пары.")
                        Text(
                            text = "+${state.rewardCoins} монет",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        if (state.errorMessage != null) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::restartGame) {
                        Text("Ещё раз")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onExitToMenu) {
                        Text("В меню")
                    }
                }
            )
        }

        GameStatus.LOST -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Время вышло") },
                text = {
                    Column {
                        Text("Попробуйте сыграть ещё раз.")
                        if (state.errorMessage != null) {
                            Text(
                                text = state.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = viewModel::restartGame) {
                        Text("Ещё раз")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onExitToMenu) {
                        Text("В меню")
                    }
                }
            )
        }

        GameStatus.READY,
        GameStatus.PLAYING,
        GameStatus.ERROR -> Unit
    }
}

@Composable
private fun GameHeader(
    title: String,
    coins: Int,
    timeLeftSeconds: Int,
    totalTimeSeconds: Int,
    moves: Int,
    foundPairs: Int,
    totalPairs: Int,
    canPause: Boolean,
    onPause: () -> Unit
) {
    val progress = if (totalTimeSeconds > 0) {
        (timeLeftSeconds.toFloat() / totalTimeSeconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Монеты: $coins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = onPause,
                enabled = canPause
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Пауза"
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.weight(1f)
            )

            Text(
                text = formatTime(timeLeftSeconds),
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Ходы: $moves")
            Text("Пары: $foundPairs/$totalPairs")
        }
    }
}

@Composable
private fun MemoryCardView(
    card: MemoryCard,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isFaceUp = card.isFlipped || card.isMatched
    val rotation by animateFloatAsState(
        targetValue = if (isFaceUp) 180f else 0f,
        label = "card_flip"
    )
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(
                enabled = enabled && !isFaceUp,
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation > 90f) {
                AsyncImage(
                    model = "file:///android_asset/cards/${card.name}.png",
                    contentDescription = card.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                CardBack()
            }
        }
    }
}

@Composable
private fun CardBack() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✦",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
private fun HintButtons(
    coins: Int,
    enabled: Boolean,
    onShowPair: () -> Unit,
    onAddTime: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onShowPair,
            enabled = enabled && coins >= 50,
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Показать пару")
                Text(
                    text = "50 монет",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        OutlinedButton(
            onClick = onAddTime,
            enabled = enabled && coins >= 40,
            modifier = Modifier.weight(1f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+15 секунд")
                Text(
                    text = "40 монет",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
}