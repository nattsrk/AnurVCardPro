package com.anur.vcardpro.model

import com.google.gson.annotations.SerializedName

// RENAMED: Claim -> InsuranceClaim to avoid conflicts
data class InsuranceClaim(
    @SerializedName("id") val id: Int,
    @SerializedName("policyId") val policyId: Int,  // Add this field
    @SerializedName("claimNumber") val claimNumber: String?,
    @SerializedName("claimType") val claimType: String?,  // Add this field
    @SerializedName("claimAmount") val amount: Double?,  // Change from "amount" to "claimAmount"
    @SerializedName("claimStatus") val status: String?,  // Change from "status" to "claimStatus"
    @SerializedName("description") val description: String?,
    @SerializedName("claimDate") val submittedDate: String?,  // Change from "submittedDate" to "claimDate"
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?
)

// Claims API Response
data class ClaimsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("policyId") val policyId: Int,  // Change String to Int
    @SerializedName("claims") val claims: List<InsuranceClaim>
)

// Timeline step for progress display
data class TimelineStep(
    @SerializedName("status") val status: String,
    @SerializedName("date") val date: String?,
    @SerializedName("description") val description: String,
    @SerializedName("isCompleted") val isCompleted: Boolean,
    @SerializedName("isActive") val isActive: Boolean
)