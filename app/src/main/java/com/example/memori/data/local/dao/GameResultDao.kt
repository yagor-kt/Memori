package com.example.memori.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.memori.data.local.entity.GameResult

@Dao
interface GameResultDao {

    @Insert
    suspend fun insert(result: GameResult): Long

    @Query(
        """
        SELECT
            p.id AS playerId,
            p.name AS playerName,
            MIN(gr.timeSpent) AS bestTime,
            (
                SELECT gr2.moves
                FROM game_results AS gr2
                WHERE gr2.playerId = p.id
                  AND gr2.difficulty = :difficulty
                  AND gr2.won = 1
                  AND gr2.timeSpent = (
                      SELECT MIN(gr3.timeSpent)
                      FROM game_results AS gr3
                      WHERE gr3.playerId = p.id
                        AND gr3.difficulty = :difficulty
                        AND gr3.won = 1
                  )
                ORDER BY gr2.timestamp ASC, gr2.id ASC
                LIMIT 1
            ) AS moves
        FROM game_results AS gr
        INNER JOIN players AS p ON p.id = gr.playerId
        WHERE gr.difficulty = :difficulty
          AND gr.won = 1
        GROUP BY p.id, p.name
        ORDER BY bestTime ASC, p.name COLLATE NOCASE ASC
        LIMIT 10
        """
    )
    suspend fun getLeaderboard(difficulty: String): List<LeaderboardEntry>

    @Query("Select count(*) FROM game_results where playerId = :playerId and won = :isWon")
    suspend fun countWinsForPlayer(playerId: Long, isWon: Boolean = true): Int

    @Query("DELETE FROM game_results WHERE playerId = :playerId")
    suspend fun deleteForPlayer(playerId: Long): Int
}