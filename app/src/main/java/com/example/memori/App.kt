package com.example.memori

import android.app.Application
import java.util.Collections

class App : Application() {

    val cardNames: List<String> = Collections.unmodifiableList(
        listOf("bear", "cat", "dog", "fox", "frog", "giraffe", "hedgehog", "lion", "monkey", "owl", "panda", "penguin", "rabbit", "raccoon", "sheep", "tiger", "turtle", "zebra", "elephant", "koala", "parrot", "squirrel", "whale", "wolf")
    )

    /*val database: AppDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppDatabase.getInstance(applicationContext)
    }

    val playerRepository: PlayerRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PlayerRepository(database)
    }

    val gameRepository: GameRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        GameRepository(database)
    }

    val achievementRepository: AchievementRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AchievementRepository(database)
    }

    val preferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
    }

    companion object {
        const val PREFERENCES_NAME = "memori_preferences"
        const val CURRENT_PLAYER_ID_KEY = "current_player_id"
    }*/
}