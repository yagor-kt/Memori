package com.example.memori.data.local.dao

data class UnlockedAchievement(
    val achievementId: Long,
    val code: String,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val unlockedAt: Long
)