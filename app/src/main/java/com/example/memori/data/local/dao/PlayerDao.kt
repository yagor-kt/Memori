package com.example.memori.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.memori.data.local.entity.Player

@Dao
interface PlayerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(player: Player): Long

    @Query("SELECT * FROM players WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Player?

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Player?

    @Query("UPDATE players SET coins = :coins WHERE id = :playerId")
    suspend fun updateCoins(playerId: Long, coins: Int): Int

    @Query("UPDATE players SET coins = coins + :amount WHERE id = :playerId")
    suspend fun addCoins(playerId: Long, amount: Int): Int

    @Query("UPDATE players SET lastBonusDate = :date WHERE id = :playerId")
    suspend fun updateBonusDate(playerId: Long, date: Long): Int

    @Query(
        """
        UPDATE players
        SET coins = 0, lastBonusDate = 0
        WHERE id = :playerId
        """
    )
    suspend fun resetProgress(playerId: Long): Int
}