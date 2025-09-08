package com.anur.vcardpro.model

data class PhoneContact(
    val id: Long,
    val name: String,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val photoUri: String? = null
) {
    // Get first letter for alphabetical grouping
    val firstLetter: String
        get() = name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"

    // Get initials for avatar
    val initials: String
        get() = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("").take(2)

    // Get primary phone number
    val primaryPhone: String?
        get() = phoneNumbers.firstOrNull()

    // Get primary email
    val primaryEmail: String?
        get() = emails.firstOrNull()
}