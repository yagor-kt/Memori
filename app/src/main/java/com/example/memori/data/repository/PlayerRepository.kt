package com.example.memori.data.repository

import androidx.room.withTransaction
import com.example.memori.data.local.AppDatabase
import com.example.memori.data.local.entity.Player

class PlayerRepository(
    private val database: AppDatabase
) {
    private val playerDao = database.playerDao()

    suspend fun loginOrCreate(name: String): Player {
        require(name.isNotBlank()) { "Имя игрока не должно быть пустым" }
        require(name.length <= 20) { "Имя игрока должно содержать не более 20 символов" }

        return database.withTransaction {
            playerDao.getByName(name)?.let { existingPlayer ->
                return@withTransaction existingPlayer
            }

            playerDao.insert(
                Player(
                    name = name,
                    coins = 0,
                    lastBonusDate = 0L
                )
            )

            playerDao.getByName(name)
                ?: error("Не удалось создать или загрузить игрока")
        }
    }

    suspend fun getById(playerId: Long): Player? {
        return playerDao.getById(playerId)
    }

    suspend fun getByName(name: String): Player? {
        return playerDao.getByName(name)
    }

    suspend fun updateCoins(playerId: Long, coins: Int) {
        require(coins >= 0) { "Баланс монет не может быть отрицательным" }
        playerDao.updateCoins(playerId, coins)
    }

    suspend fun addCoins(playerId: Long, amount: Int) {
        require(amount >= 0) { "Нельзя добавить отрицательное количество монет" }
        playerDao.addCoins(playerId, amount)
    }

    suspend fun updateBonusDate(playerId: Long, date: Long) {
        playerDao.updateBonusDate(playerId, date)
    }

    suspend fun resetProgress(playerId: Long) {
        database.withTransaction {
            database.gameResultDao().deleteForPlayer(playerId)
            database.playerAchievementDao().deleteForPlayer(playerId)
            playerDao.resetProgress(playerId)
        }
    }
}