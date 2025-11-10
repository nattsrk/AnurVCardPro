package com.anur.vcardpro.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("company") val company: String? = null,
    @SerializedName("linkedin") val linkedin: String? = null,
    @SerializedName("profileUrl") val profileUrl: String? = null,
    @SerializedName("profileSlug") val profileSlug: String? = null
)

data class UserResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("user") val user: User
)
