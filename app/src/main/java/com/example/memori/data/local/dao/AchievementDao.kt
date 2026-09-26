package com.example.memori.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.memori.data.local.entity.Achievement

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY id ASC")
    suspend fun getAll(): List<Achievement>

    @Query(
        """
        SELECT
            a.id AS achievementId,
            a.code AS code,
            a.title AS title,
            a.description AS description,
            a.rewardCoins AS rewardCoins,
            pa.unlockedAt AS unlockedAt
        FROM achievements AS a
        INNER JOIN player_achievements AS pa ON pa.achievementId = a.id
        WHERE pa.playerId = :playerId
        ORDER BY pa.unlockedAt DESC
        """
    )
    suspend fun getUnlockedForPlayer(playerId: Long): List<UnlockedAchievement>

    @Query("SELECT * FROM achievements WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): Achievement?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<Achievement>): List<Long>
}