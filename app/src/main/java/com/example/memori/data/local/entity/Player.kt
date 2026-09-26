package com.example.memori.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "players",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val coins: Int = 0,
    val lastBonusDate: Long = 0L
)