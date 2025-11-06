package com.anur.vcardpro.model

// Data models for writing to Smart Cards
data class WriteableCardData(
    val personalInfo: WriteablePersonalInfo = WriteablePersonalInfo(),
    val emergencyContact: WriteableEmergencyContact = WriteableEmergencyContact(),
    val insuranceInfo: WriteableInsuranceInfo = WriteableInsuranceInfo(),
    val vCardInfo: WriteableVCardInfo = WriteableVCardInfo()
)

data class WriteablePersonalInfo(
    val fullName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val email: String = "",
    val organization: String = "",
    val jobTitle: String = "",
    val address: String = "",
    val website: String = "",
    val notes: String = ""
) {
    fun isValid(): Boolean {
        return fullName.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())
    }

    fun toVCardString(): String {
        val vCard = StringBuilder()
        vCard.append("BEGIN:VCARD\n")
        vCard.append("VERSION:3.0\n")
        if (fullName.isNotBlank()) vCard.append("FN:$fullName\n")
        if (firstName.isNotBlank() || lastName.isNotBlank()) {
            vCard.append("N:$lastName;$firstName;;;\n")
        }
        if (phone.isNotBlank()) vCard.append("TEL:$phone\n")
        if (email.isNotBlank()) vCard.append("EMAIL:$email\n")
        if (organization.isNotBlank()) vCard.append("ORG:$organization\n")
        if (jobTitle.isNotBlank()) vCard.append("TITLE:$jobTitle\n")
        if (address.isNotBlank()) vCard.append("ADR:;;$address;;;;\n")
        if (website.isNotBlank()) vCard.append("URL:$website\n")
        if (notes.isNotBlank()) vCard.append("NOTE:$notes\n")
        vCard.append("END:VCARD")
        return vCard.toString()
    }
}

data class WriteableEmergencyContact(
    val name: String = "",
    val bloodGroup: String = "",
    val mobile: String = "",
    val location: String = "",
    val relationship: String = "",
    val alternateContact: String = "",
    val medicalConditions: String = "",
    val allergies: String = ""
) {
    fun isValid(): Boolean {
        return name.isNotBlank() && mobile.isNotBlank()
    }

    fun toTextRecord(): String {
        val emergency = StringBuilder()
        emergency.append("EMERGENCY CONTACT INFORMATION\n\n")
        if (name.isNotBlank()) emergency.append("Name: $name\n")
        if (bloodGroup.isNotBlank()) emergency.append("Blood Group: $bloodGroup\n")
        if (mobile.isNotBlank()) emergency.append("Mobile: $mobile\n")
        if (location.isNotBlank()) emergency.append("Location: $location\n")
        if (relationship.isNotBlank()) emergency.append("Relationship: $relationship\n")
        if (alternateContact.isNotBlank()) emergency.append("Alternate Contact: $alternateContact\n")
        if (medicalConditions.isNotBlank()) emergency.append("Medical Conditions: $medicalConditions\n")
        if (allergies.isNotBlank()) emergency.append("Allergies: $allergies\n")
        return emergency.toString()
    }
}

data class WriteableInsuranceInfo(
    val policyholderName: String = "",
    val age: String = "",
    val insurerName: String = "",
    val policyType: String = "",
    val policyNumber: String = "",
    val premium: String = "",
    val sumAssured: String = "",
    val policyStart: String = "",
    val policyEnd: String = "",
    val status: String = "",
    val contactNumber: String = "",
    val mobile: String = ""
) {
    fun isValid(): Boolean {
        return policyholderName.isNotBlank() && insurerName.isNotBlank() && policyNumber.isNotBlank()
    }

    fun toTextRecord(): String {
        val insurance = StringBuilder()
        insurance.append("LIFE INSURANCE INFORMATION\n\n")
        if (policyholderName.isNotBlank()) insurance.append("Policyholder: $policyholderName\n")
        if (age.isNotBlank()) insurance.append("Age: $age\n")
        if (insurerName.isNotBlank()) insurance.append("Insurer: $insurerName\n")
        if (policyType.isNotBlank()) insurance.append("Policy Type: $policyType\n")
        if (policyNumber.isNotBlank()) insurance.append("Policy Number: $policyNumber\n")
        if (premium.isNotBlank()) insurance.append("Premium: $premium\n")
        if (sumAssured.isNotBlank()) insurance.append("Sum Assured: $sumAssured\n")
        if (policyStart.isNotBlank()) insurance.append("Policy Start: $policyStart\n")
        if (policyEnd.isNotBlank()) insurance.append("Policy End: $policyEnd\n")
        if (status.isNotBlank()) insurance.append("Status: $status\n")
        if (contactNumber.isNotBlank()) insurance.append("Contact: $contactNumber\n")
        if (mobile.isNotBlank()) insurance.append("Mobile: $mobile\n")
        return insurance.toString()
    }
}

data class WriteableVCardInfo(
    val version: String = "3.0",
    val customFields: MutableMap<String, String> = mutableMapOf()
) {
    fun addCustomField(key: String, value: String) {
        if (key.isNotBlank() && value.isNotBlank()) {
            customFields[key] = value
        }
    }

    fun toCustomVCard(personalInfo: WriteablePersonalInfo): String {
        val vCard = StringBuilder()
        vCard.append(personalInfo.toVCardString())

        // Add custom fields before END:VCARD
        if (customFields.isNotEmpty()) {
            val lines = vCard.toString().split("\n").toMutableList()
            val endIndex = lines.indexOfLast { it.trim() == "END:VCARD" }
            if (endIndex != -1) {
                customFields.forEach { (key, value) ->
                    lines.add(endIndex, "$key:$value")
                }
                return lines.joinToString("\n")
            }
        }

        return vCard.toString()
    }
}

// Validation utility
object CardDataValidator {
    fun validateEmail(email: String): Boolean {
        return email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
    }

    fun validatePhone(phone: String): Boolean {
        return phone.matches(Regex("^[+]?[0-9\\s-()]{7,15}$"))
    }

    fun validateBloodGroup(bloodGroup: String): Boolean {
        val validGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        return bloodGroup.uppercase() in validGroups
    }

    fun validateUrl(url: String): Boolean {
        return url.matches(Regex("^https?://[\\w\\-.]+(:\\d+)?(/.*)?$"))
    }
}