package com.example.memori.data.repository

import com.example.memori.App
import com.example.memori.data.local.entity.Achievement

class AchievementChecker(
    private val application: App
) {
    suspend fun checkAfterGame(
        playerId: Long,
        difficulty: String,
        won: Boolean,
        timeSpent: Int,
        moves: Int
    ): List<Achievement> {
        if (!won) return emptyList()

        val unlocked = mutableListOf<Achievement>()

        application.achievementRepository.ensureDefaultAchievements()

        application.achievementRepository.unlockIfNotAlreadyEarned(
            playerId = playerId,
            code = AchievementDefinitions.FIRST_WIN
        )?.let(unlocked::add)

        val totalPairs = difficultyTotalPairs(difficulty)
        if (totalPairs > 0 && moves == totalPairs) {
            application.achievementRepository.unlockIfNotAlreadyEarned(
                playerId = playerId,
                code = AchievementDefinitions.NO_MISTAKES
            )?.let(unlocked::add)
        }

        val timeLimit = difficultyTimeLimit(difficulty)
        if (timeLimit > 0 && timeSpent < timeLimit / 2) {
            application.achievementRepository.unlockIfNotAlreadyEarned(
                playerId = playerId,
                code = AchievementDefinitions.SPEEDRUN
            )?.let(unlocked::add)
        }

        val winCount = application.gameRepository.countWinsForPlayer(playerId)
        if (winCount >= COLLECTOR_WIN_COUNT) {
            application.achievementRepository.unlockIfNotAlreadyEarned(
                playerId = playerId,
                code = AchievementDefinitions.COLLECTOR
            )?.let(unlocked::add)
        }

        return unlocked
    }

    private fun difficultyTotalPairs(difficulty: String): Int {
        return when (difficulty) {
            "easy" -> 6
            "medium" -> 8
            "hard" -> 12
            "expert" -> 18
            else -> 0
        }
    }

    private fun difficultyTimeLimit(difficulty: String): Int {
        return when (difficulty) {
            "easy" -> 60
            "medium" -> 90
            "hard" -> 150
            "expert" -> 210
            else -> 0
        }
    }

    private companion object {
        const val COLLECTOR_WIN_COUNT = 10
    }
}