package com.anur.vcardpro.model

data class ReceivedContactResponse(
    val success: Boolean,
    val contacts: List<ContactShare>
)

data class ContactShare(
    val id: Int,
    val sharedByUserId: Int,
    val name: String?,
    val viewerEmail: String?,
    val viewerPhone: String?,
    val shareMethod: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val referrer: String?,
    val location: String?,
    val notes: String?,
    val visitorContactData: String?, // JSON string
    val createdAt: String,
    val updatedAt: String
)
