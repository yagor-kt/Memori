package com.example.memori.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.memori.data.local.entity.PlayerAchievement

@Dao
interface PlayerAchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(playerAchievement: PlayerAchievement): Long

    @Query("SELECT * FROM player_achievements WHERE playerId = :playerId")
    suspend fun getForPlayer(playerId: Long): List<PlayerAchievement>

    @Query("DELETE FROM player_achievements WHERE playerId = :playerId")
    suspend fun deleteForPlayer(playerId: Long): Int
}