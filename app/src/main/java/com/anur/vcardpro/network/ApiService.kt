package com.anur.vcardpro.network
import com.anur.vcardpro.model.ReceivedContactResponse
import com.anur.vcardpro.model.UserResponse
import com.anur.vcardpro.model.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    @POST("api/users")
    fun createUser(@Body body: Map<String, String>): Call<UserResponse>

    @GET("api/users/{id}")
    fun getUserProfile(@Path("id") id: Int): Call<UserResponse>

    @POST("api/admin/login")
    fun adminLogin(@Body body: Map<String, String>): Call<LoginResponse>

    // ✅ FIXED: Add the missing /api/ prefix
    @GET("api/received/{userId}")
    fun getReceivedContacts(@Path("userId") userId: Int): Call<ReceivedContactResponse>
}