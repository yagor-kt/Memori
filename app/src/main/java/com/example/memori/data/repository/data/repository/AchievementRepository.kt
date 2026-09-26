package com.example.memori.data.repository.data.repository

import androidx.room.withTransaction
import com.example.memori.data.local.AppDatabase
import com.example.memori.data.local.dao.UnlockedAchievement
import com.example.memori.data.local.entity.Achievement
import com.example.memori.data.local.entity.PlayerAchievement
import com.example.memori.data.repository.AchievementDefinitions

class AchievementRepository(
    private val database: AppDatabase
) {
    private val achievementDao = database.achievementDao()
    private val playerAchievementDao = database.playerAchievementDao()
    private val playerDao = database.playerDao()

    suspend fun ensureDefaultAchievements() {
        achievementDao.insertAll(AchievementDefinitions.all)
    }

    suspend fun getAll(): List<Achievement> {
        ensureDefaultAchievements()
        return achievementDao.getAll()
    }

    suspend fun getUnlockedForPlayer(playerId: Long): List<UnlockedAchievement> {
        ensureDefaultAchievements()
        return achievementDao.getUnlockedForPlayer(playerId)
    }

    /**
     * Выдаёт достижение и начисляет награду только при первой выдаче.
     * Возвращает достижение, если оно было выдано этим вызовом, иначе null.
     */
    suspend fun unlockIfNotAlreadyEarned(
        playerId: Long,
        code: String
    ): Achievement? {
        ensureDefaultAchievements()

        return database.withTransaction {
            val achievement = achievementDao.getByCode(code) ?: return@withTransaction null

            val insertedId = playerAchievementDao.insert(
                PlayerAchievement(
                    playerId = playerId,
                    achievementId = achievement.id
                )
            )

            if (insertedId == -1L) {
                return@withTransaction null
            }

            playerDao.addCoins(playerId, achievement.rewardCoins)
            achievement
        }
    }

    suspend fun getPlayerAchievements(playerId: Long): List<PlayerAchievement> {
        return playerAchievementDao.getForPlayer(playerId)
    }
}