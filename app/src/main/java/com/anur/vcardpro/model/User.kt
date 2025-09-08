package com.anur.vcardpro.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String?,
    val company: String?,
    val linkedin: String?,
    val profileUrl: String?,
    val profileSlug: String?
)

data class UserResponse(
    val success: Boolean,
    val user: User
)
