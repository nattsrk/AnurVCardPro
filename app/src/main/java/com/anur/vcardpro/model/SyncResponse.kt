package com.anur.vcardpro.model

data class SyncResponse(
    val success: Boolean,
    val syncedAt: String,
    val message: String
)