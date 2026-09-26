package com.example.memori.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "achievements",
    indices = [
        Index(value = ["code"], unique = true)
    ]
)
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val title: String,
    val description: String,
    val rewardCoins: Int
)