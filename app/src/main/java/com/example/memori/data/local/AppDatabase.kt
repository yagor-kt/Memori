package com.example.memori.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.memori.data.local.dao.AchievementDao
import com.example.memori.data.local.dao.GameResultDao
import com.example.memori.data.local.dao.PlayerAchievementDao
import com.example.memori.data.local.dao.PlayerDao
import com.example.memori.data.local.entity.Achievement
import com.example.memori.data.local.entity.GameResult
import com.example.memori.data.local.entity.Player
import com.example.memori.data.local.entity.PlayerAchievement

@Database(
    entities = [
        Player::class,
        GameResult::class,
        Achievement::class,
        PlayerAchievement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playerDao(): PlayerDao

    abstract fun gameResultDao(): GameResultDao

    abstract fun achievementDao(): AchievementDao

    abstract fun playerAchievementDao(): PlayerAchievementDao

    companion object {
        private const val DATABASE_NAME = "memori_database"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build().also { database ->
                    instance = database
                }
            }
        }
    }
}