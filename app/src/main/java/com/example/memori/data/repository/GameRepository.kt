package com.example.memori.data.repository

import com.example.memori.data.local.AppDatabase
import com.example.memori.data.local.dao.LeaderboardEntry
import com.example.memori.data.local.entity.GameResult

class GameRepository(
    private val database: AppDatabase
) {
    private val gameResultDao = database.gameResultDao()

    suspend fun saveResult(result: GameResult): Long {
        require(result.timeSpent >= 0) { "Время игры не может быть отрицательным" }
        require(result.moves >= 0) { "Количество ходов не может быть отрицательным" }
        return gameResultDao.insert(result)
    }

    suspend fun getLeaderboard(difficulty: String): List<LeaderboardEntry> {
        return gameResultDao.getLeaderboard(difficulty)
    }

    suspend fun countWinsForPlayer(playerId: Long): Int {
        return gameResultDao.countWinsForPlayer(playerId)
    }

    suspend fun deleteResultsForPlayer(playerId: Long) {
        gameResultDao.deleteForPlayer(playerId)
    }
}