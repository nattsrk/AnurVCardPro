package com.anur.vcardpro.model

data class CardReadLog(
    val userId: Int,
    val cardId: String,
    val readAt: String,
    val dataTypes: List<String>
)