package com.example.memori

import android.app.Application
import com.example.memori.data.local.AppDatabase
import com.example.memori.data.repository.GameRepository
import com.example.memori.data.repository.PlayerRepository
import com.example.memori.data.repository.AchievementRepository
import java.util.Collections

class App : Application() {

    val cardNames: List<String> = Collections.unmodifiableList(
        listOf(
            "1",
"2",
"3",
"4",
"5",
"6",
"7",
"8",
"9",
"10",
"11",
"12",
"13",
"14",
"15",
"16",
"17",
"18",
"19",
"20",
"21",
"22",
"23",
"24",
"25",
        )
    )

    val database: AppDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
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
    }
}