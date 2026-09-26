package com.example.memori.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "player_achievements",
    primaryKeys = ["playerId", "achievementId"],
    foreignKeys = [
        ForeignKey(
            entity = Player::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Achievement::class,
            parentColumns = ["id"],
            childColumns = ["achievementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["achievementId"])
    ]
)
data class PlayerAchievement(
    val playerId: Long,
    val achievementId: Long,
    val unlockedAt: Long = System.currentTimeMillis()
)