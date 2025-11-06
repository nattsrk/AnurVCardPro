package com.anur.vcardpro.model

import com.google.gson.annotations.SerializedName

// Insurance Response Models - UPDATED to match your API
data class InsuranceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("userId") val userId: String,  // API returns userId as string
    @SerializedName("policies") val policies: List<InsurancePolicy>?
)

// UPDATED: InsurancePolicy to match your actual API response
data class InsurancePolicy(
    @SerializedName("id") val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("policyNumber") val policyNumber: String,
    @SerializedName("policyType") val policyType: String,
    @SerializedName("insurerName") val insurerName: String,
    @SerializedName("premiumAmount") val premiumAmount: Double,
    @SerializedName("sumAssured") val sumAssured: Double,
    @SerializedName("policyStartDate") val policyStartDate: String,
    @SerializedName("policyEndDate") val policyEndDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("claims") val claims: List<InsuranceClaim>? = null,
    @SerializedName("payments") val payments: List<Payment>? = null
)

// NEW: Payment model to match your API response
data class Payment(
    @SerializedName("id") val id: Int,
    @SerializedName("policyId") val policyId: Int,
    @SerializedName("paymentDate") val paymentDate: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("paymentMode") val paymentMode: String,
    @SerializedName("transactionId") val transactionId: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("policy_id") val policy_id: Int? = null  // Seems to be duplicate field in API
)

// Keep existing PolicyResponse for backwards compatibility
data class PolicyResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("policy") val policy: Policy?,
    @SerializedName("claims") val claims: List<PolicyClaim>?
)

// Keep existing Policy model for backwards compatibility
data class Policy(
    @SerializedName("id") val id: Int,
    @SerializedName("policyNumber") val policyNumber: String,
    @SerializedName("type") val type: String,
    @SerializedName("premium") val premium: Double,
    @SerializedName("coverageAmount") val coverageAmount: Double,
    @SerializedName("status") val status: String
)

// Keep existing PolicyClaim for backwards compatibility
data class PolicyClaim(
    @SerializedName("id") val id: Int,
    @SerializedName("claimNumber") val claimNumber: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("status") val status: String,
    @SerializedName("submittedDate") val submittedDate: String
)