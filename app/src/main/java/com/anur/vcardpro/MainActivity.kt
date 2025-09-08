package com.anur.vcardpro

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anur.vcardpro.model.LoginResponse
import com.anur.vcardpro.network.ApiService
import com.anur.vcardpro.ui.*
import kotlinx.coroutines.delay
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import com.anur.vcardpro.UserSession

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐ Load session when app starts
        UserSession.loadSession(this)

        setContent {
            var currentScreen by remember { mutableStateOf("splash") }

            when (currentScreen) {
                "splash" -> SplashScreen {
                    // ⭐ Check if user is already logged in
                    currentScreen = if (UserSession.isLoggedIn()) {
                        "dashboard"  // Auto-login to dashboard
                    } else {
                        "login"      // Show login screen
                    }
                }
                "login" -> LoginScreen { currentScreen = "dashboard" }
                "dashboard" -> DashboardScreen(
                    onProfile = { currentScreen = "profile" },
                    onContacts = { currentScreen = "contacts" },
                    onReceived = { currentScreen = "received" },
                    onVCard = { currentScreen = "vcard" },
                    onLogout = {
                        // ⭐ Clear persistent session
                        UserSession.clearSession(this@MainActivity)
                        currentScreen = "login"
                        // Handle widget click navigation
                        intent.getStringExtra("navigate_to")?.let { destination ->
                            if (destination == "received") {
                                currentScreen = "received"
                            }
                        }
                    }
                )
                "profile" -> MyProfileScreen { currentScreen = "dashboard" }
                "contacts" -> ContactsScreen { currentScreen = "dashboard" }
                "received" -> ReceivedScreen { currentScreen = "dashboard" }
                "vcard" -> VCardScreen { currentScreen = "dashboard" }
            }
        }
    }
}

@Composable
fun SplashScreen(onNext: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onNext()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFFFF0000), Color.Black))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📱", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(30.dp))
            Text("Anur VCard", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Smart Contact Sharing", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f))

            // ⭐ Show auto-login status
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                if (UserSession.userId != -1) "Welcome back!" else "Please login",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
   // var username by remember { mutableStateOf("sathi@onewealth.com") }
   // var password by remember { mutableStateOf("sat123") }
   // var manualUserId by remember { mutableStateOf("11") } // ⭐ NEW: Manual User ID override

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var manualUserId by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2))))
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome Back", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(40.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )

                // ⭐ NEW: Manual User ID field for demo
                OutlinedTextField(
                    value = manualUserId,
                    onValueChange = { manualUserId = it },
                    label = { Text("Emp Code") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter user ID: 1, 2, 3, etc.") }
                )

                Button(
                    onClick = {
                        if (username.isNotEmpty() && password.isNotEmpty() && manualUserId.isNotEmpty()) {
                            val userIdInt = manualUserId.toIntOrNull()
                            if (userIdInt == null || userIdInt <= 0) {
                                Toast.makeText(context, "Please enter a valid User ID", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // ⭐ SAME API CALL AS BEFORE
                            val retrofit = Retrofit.Builder()
                                .baseUrl("https://vcard.tecgs.com:3000/")
                                .addConverterFactory(GsonConverterFactory.create())
                                .build()

                            val api = retrofit.create(ApiService::class.java)
                            val loginData = mapOf(
                                "email" to username,
                                "password" to password
                            )

                            api.adminLogin(loginData).enqueue(object : Callback<LoginResponse> {
                                override fun onResponse(
                                    call: Call<LoginResponse>,
                                    response: Response<LoginResponse>
                                ) {
                                    if (response.isSuccessful && response.body()?.message == "Login successful") {
                                        val userId = userIdInt
                                        val userName = response.body()?.user?.name ?: username.substringBefore("@")
                                        val userEmail = response.body()?.user?.email ?: username

                                        if (userId != -1) {
                                            // ⭐ Save to session AND persist it
                                            UserSession.userId = userId
                                            UserSession.userName = userName
                                            UserSession.userEmail = userEmail
                                            UserSession.saveSession(context) // ⭐ Save to SharedPreferences
                                        }

                                        Toast.makeText(context, "Login successful! Using User ID: $userId", Toast.LENGTH_SHORT).show()
                                        onLogin()
                                    }
                                }

                                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                    Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        } else {
                            Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))

                ) {
                    Text("Login", color = Color.White, fontSize = 16.sp)
                }

                Text(
                    "Enter User ",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}