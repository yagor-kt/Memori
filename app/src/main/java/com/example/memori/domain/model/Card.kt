package com.example.memori.domain.model

data class Card(
    val id: Int,
    val pairId: Int,
    val name: String,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)