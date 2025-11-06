package com.anur.vcardpro.model

import com.google.gson.annotations.SerializedName

data class PolicyProduct(
    @SerializedName("id") val id: Int,
    @SerializedName("productName") val productName: String,
    @SerializedName("productType") val productType: String,
    @SerializedName("insurerName") val insurerName: String,
    @SerializedName("basePremium") val basePremium: Double,
    @SerializedName("baseCoverage") val baseCoverage: Double,
    @SerializedName("description") val description: String,
    @SerializedName("features") val features: List<String>,
    @SerializedName("coverageOptions") val coverageOptions: List<Double>,
    @SerializedName("tenureOptions") val tenureOptions: List<Int>,
    @SerializedName("premiumMultiplier") val premiumMultiplier: Double
)

data class PolicyProductsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("products") val products: List<PolicyProduct>
)

data class PolicyApplicationRequest(
    @SerializedName("userId") val userId: Int,
    @SerializedName("productId") val productId: Int,
    @SerializedName("productName") val productName: String,
    @SerializedName("productType") val productType: String,
    @SerializedName("insurerName") val insurerName: String,
    @SerializedName("selectedCoverage") val selectedCoverage: Double,
    @SerializedName("selectedTenure") val selectedTenure: Int,
    @SerializedName("calculatedPremium") val calculatedPremium: Double
)

data class ApplicationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)