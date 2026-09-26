package com.example.memori.ui.settings

import android.widget.ImageButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.memori.R
import com.example.memori.data.local.dao.LeaderboardEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SettingsTab {
    PROFILE,
    ACHIEVEMENTS,
    LEADERBOARD
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onChangePlayer: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(SettingsTab.PROFILE.ordinal) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.ChangePlayer -> onChangePlayer()
                is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SettingsHeader(
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == SettingsTab.PROFILE.ordinal,
                        onClick = { selectedTab = SettingsTab.PROFILE.ordinal },
                        text = { Text(stringResource(R.string.settings_profile)) }
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.ACHIEVEMENTS.ordinal,
                        onClick = { selectedTab = SettingsTab.ACHIEVEMENTS.ordinal },
                        text = { Text(stringResource(R.string.settings_achievements)) }
                    )
                    Tab(
                        selected = selectedTab == SettingsTab.LEADERBOARD.ordinal,
                        onClick = { selectedTab = SettingsTab.LEADERBOARD.ordinal },
                        text = { Text(stringResource(R.string.settings_leaderboard)) }
                    )
                }

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                when (selectedTab) {
                    SettingsTab.PROFILE.ordinal -> ProfileTab(
                        playerName = state.player?.name.orEmpty(),
                        coins = state.player?.coins ?: 0,
                        isResetting = state.isResetting,
                        onChangePlayer = viewModel::switchPlayer,
                        onResetProgress = { showResetDialog = true }
                    )

                    SettingsTab.ACHIEVEMENTS.ordinal -> AchievementsTab(
                        achievements = state.achievements
                    )

                    SettingsTab.LEADERBOARD.ordinal -> LeaderboardTab(
                        selectedDifficulty = state.selectedDifficulty,
                        leaderboard = state.leaderboard,
                        onDifficultySelected = viewModel::selectDifficulty
                    )

                    else -> Unit
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_progress_title)) },
            text = { Text(stringResource(R.string.reset_progress_confirmation)) },
            confirmButton = {
                Button(
                    enabled = !state.isResetting,
                    onClick = {
                        showResetDialog = false
                        viewModel.resetProgress()
                    }
                ) {
                    Text(stringResource(R.string.reset_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }
}

@Composable
private fun SettingsHeader(
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null
            )
        }

        Text(
            text = stringResource(R.string.settings_title),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ProfileTab(
    playerName: String,
    coins: Int,
    isResetting: Boolean,
    onChangePlayer: () -> Unit,
    onResetProgress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.profile_name, playerName),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.coins_balance, coins),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        OutlinedButton(
            onClick = onChangePlayer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.change_player))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onResetProgress,
            enabled = !isResetting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isResetting) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(R.string.reset_progress_button))
            }
        }
    }
}

@Composable
private fun AchievementsTab(
    achievements: List<AchievementUiModel>
) {
    if (achievements.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_achievements))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = achievements,
            key = { it.achievement.id }
        ) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isUnlocked) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.achievement.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(
                                if (item.isUnlocked) {
                                    R.string.achievement_unlocked
                                } else {
                                    R.string.achievement_locked
                                }
                            ),
                            color = if (item.isUnlocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Text(
                        text = item.achievement.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(
                            R.string.achievement_reward,
                            item.achievement.rewardCoins
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )

                    item.unlockedAt?.let { timestamp ->
                        Text(
                            text = stringResource(
                                R.string.achievement_date,
                                formatDate(timestamp)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardTab(
    selectedDifficulty: String,
    leaderboard: List<LeaderboardEntry>,
    onDifficultySelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SettingsViewModel.DIFFICULTIES.forEach { difficulty ->
                val selected = difficulty == selectedDifficulty

                if (selected) {
                    Button(onClick = { onDifficultySelected(difficulty) }) {
                        Text(SettingsViewModel.difficultyTitle(difficulty))
                    }
                } else {
                    OutlinedButton(onClick = { onDifficultySelected(difficulty) }) {
                        Text(SettingsViewModel.difficultyTitle(difficulty))
                    }
                }
            }
        }

        if (leaderboard.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.leaderboard_empty),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = leaderboard,
                    key = { it.playerId }
                ) { entry ->
                    LeaderboardRow(
                        place = leaderboard.indexOf(entry) + 1,
                        entry = entry
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    place: Int,
    entry: LeaderboardEntry
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.leaderboard_place, place),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.playerName,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    textAlign = TextAlign.End
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.leaderboard_time, formatTime(entry.bestTime)))
                Text(stringResource(R.string.leaderboard_moves, entry.moves))
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU"))
        .format(Date(timestamp))
}

private fun formatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    return String.format(
        Locale.ROOT,
        "%02d:%02d",
        safeSeconds / 60,
        safeSeconds % 60
    )
}