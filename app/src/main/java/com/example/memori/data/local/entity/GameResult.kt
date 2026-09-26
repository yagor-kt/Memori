package com.example.memori.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_results",
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playerId"]),
        Index(value = ["difficulty", "won", "timeSpent"])
    ]
)
data class GameResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerId: Long,
    val difficulty: String,
    val timeSpent: Int,
    val moves: Int,
    val won: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)