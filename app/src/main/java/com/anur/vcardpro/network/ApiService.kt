package com.anur.vcardpro.network

import com.anur.vcardpro.model.ReceivedContactResponse
import com.anur.vcardpro.model.UserResponse
import com.anur.vcardpro.model.LoginResponse
import com.anur.vcardpro.model.InsuranceResponse
import com.anur.vcardpro.model.PolicyResponse
import com.anur.vcardpro.model.ClaimsResponse  // SINGLE IMPORT - removed duplicate
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

import com.anur.vcardpro.model.PolicyProductsResponse
import com.anur.vcardpro.model.PolicyApplicationRequest
import com.anur.vcardpro.model.ApplicationResponse

interface ApiService {

    // Existing user endpoints
    @POST("api/users")
    fun createUser(@Body body: Map<String, String>): Call<UserResponse>

    @GET("api/users/{id}")
    fun getUserProfile(@Path("id") id: Int): Call<UserResponse>

    @POST("api/admin/login")
    fun adminLogin(@Body body: Map<String, String>): Call<LoginResponse>
    @POST("api/insurance/policy/{policyId}/claims")
    fun createClaim(
        @Path("policyId") policyId: Int,
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): Call<Map<String, @JvmSuppressWildcards Any>>
    @GET("api/received/{userId}")
    fun getReceivedContacts(@Path("userId") userId: Int): Call<ReceivedContactResponse>

    // Insurance Manager endpoints
    // Create New policy
    @POST("api/insurance/policy")
    fun createInsurancePolicy(
        @Body requestBody: Map<String, @JvmSuppressWildcards Any>
    ): Call<Map<String, @JvmSuppressWildcards Any>>
    @GET("api/insurance/user/{userId}")
    fun getUserInsurance(@Path("userId") userId: Int): Call<InsuranceResponse>

    @GET("api/insurance/policy/{policyId}")
    fun getPolicyDetails(@Path("policyId") policyId: Int): Call<PolicyResponse>


    @GET("api/policy-products/products")
    fun getPolicyProducts(@Query("type") type: String? = null): Call<PolicyProductsResponse>

    @GET("api/policy-products/products/{id}")
    fun getPolicyProduct(@Path("id") productId: Int): Call<Map<String, Any>>

    @POST("api/policy-products/applications")
    fun submitPolicyApplication(@Body request: PolicyApplicationRequest): Call<ApplicationResponse>

    @GET("api/policy-products/applications/user/{userId}")
    fun getUserApplications(@Path("userId") userId: Int): Call<Map<String, Any>>

    // CLAIMS ENDPOINTS - FIXED: Removed duplicate getPolicyClaims
    @GET("api/insurance/claims/{policyId}")
    fun getPolicyClaims(@Path("policyId") policyId: Int): Call<ClaimsResponse>

    @GET("api/claims")
    fun getUserClaims(
        @Query("userId") userId: Int,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Call<ClaimsResponse>

    @GET("api/claims/{id}")
    fun getClaimById(
        @Path("id") claimId: Int,
        @Query("userId") userId: Int
    ): Call<Map<String, Any>>

    @GET("api/claims/stats/{userId}")
    fun getClaimStats(@Path("userId") userId: Int): Call<Map<String, Any>>
}