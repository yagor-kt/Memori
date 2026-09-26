package com.example.memori.data.repository

import com.example.memori.data.local.entity.Achievement

object AchievementDefinitions {

    const val FIRST_WIN = "FIRST_WIN"
    const val NO_MISTAKES = "NO_MISTAKES"
    const val SPEEDRUN = "SPEEDRUN"
    const val COLLECTOR = "COLLECTOR"

    val all: List<Achievement> = listOf(
        Achievement(
            code = FIRST_WIN,
            title = "Первая победа",
            description = "Пройти любой уровень",
            rewardCoins = 100
        ),
        Achievement(
            code = NO_MISTAKES,
            title = "Без ошибок",
            description = "Пройти уровень без неверных пар",
            rewardCoins = 150
        ),
        Achievement(
            code = SPEEDRUN,
            title = "Скоростной",
            description = "Пройти уровень быстрее половины отведённого времени",
            rewardCoins = 200
        ),
        Achievement(
            code = COLLECTOR,
            title = "Коллекционер",
            description = "Победить 10 раз",
            rewardCoins = 300
        )
    )
}