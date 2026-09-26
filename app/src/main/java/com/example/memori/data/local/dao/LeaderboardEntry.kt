package com.example.memori.data.local.dao

data class LeaderboardEntry(
    val playerId: Long,
    val playerName: String,
    val bestTime: Int,
    val moves: Int
)