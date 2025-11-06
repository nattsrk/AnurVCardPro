@file:OptIn(ExperimentalMaterial3Api::class)

package com.anur.vcardpro.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anur.vcardpro.model.User
import com.anur.vcardpro.model.UserResponse
import com.anur.vcardpro.network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.anur.vcardpro.UserSession

import androidx.compose.material.icons.filled.Add
@Composable
fun DashboardScreen(
    onProfile: () -> Unit,
    onContacts: () -> Unit,
    onReceived: () -> Unit,
    onVCard: () -> Unit,
    onSCMaster: () -> Unit,
    onLogout: () -> Unit
){
    val context = LocalContext.current
    var user by remember { mutableStateOf<User?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    // Load user data
    LaunchedEffect(Unit) {
        isVisible = true
        loadUserData(context) { userData ->
            user = userData
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF8B5CF6), // Purple
                        Color(0xFFEC4899)  // Pink
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Status bar spacing
            Spacer(modifier = Modifier.height(24.dp))

            // Header Section
            HeaderSection(
                user = user,
                isVisible = isVisible
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Action Cards
            MainActionsSection(
                onProfile = onProfile,
                onContacts = onContacts,
                onReceived = onReceived,
                onVCard = onVCard,
                onSCMaster = onSCMaster,
                isVisible = isVisible
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button
            LogoutSection(onLogout = onLogout, isVisible = isVisible)

            // Bottom spacing
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HeaderSection(user: User?, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(800)
        ) + fadeIn(animationSpec = tween(800))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Avatar and Greeting
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Greeting and Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Good ${getGreeting()}!",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        user?.name ?: "Loading...",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Notification Bell
                IconButton(
                    onClick = { /* TODO: Implement notifications */ }
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // VCard Pro Branding Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.15f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "📱 VCard Pro",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Smart Contact Sharing Platform",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MainActionsSection(
    onProfile: () -> Unit,
    onContacts: () -> Unit,
    onReceived: () -> Unit,
    onVCard: () -> Unit,
    onSCMaster: () -> Unit,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(1000, delayMillis = 400)
        ) + fadeIn(animationSpec = tween(1000, delayMillis = 400))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Main Features",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // First Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedDashboardCard(
                    title = "My Profile",
                    subtitle = "View & edit details",
                    icon = Icons.Filled.Person,
                    backgroundColor = Color(0xFFEEF2FF),
                    iconColor = Color(0xFF667EEA),
                    onClick = onProfile,
                    modifier = Modifier.weight(1f)
                )
                EnhancedDashboardCard(
                    title = "My Contacts",
                    subtitle = "Phone contacts",
                    icon = Icons.Filled.Phone,
                    backgroundColor = Color(0xFFF0FDF4),
                    iconColor = Color(0xFF8B5CF6),
                    onClick = onContacts,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Second Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedDashboardCard(
                    title = "Received\nContacts",
                    subtitle = "Shared with you",
                    icon = Icons.Filled.Email,
                    backgroundColor = Color(0xFFEFF6FF),
                    iconColor = Color(0xFF8B5CF6),
                    onClick = onReceived,
                    modifier = Modifier.weight(1f)
                )
                EnhancedDashboardCard(
                    title = "VCard Page",
                    subtitle = "Share your card",
                    icon = Icons.Filled.Share,
                    backgroundColor = Color(0xFFFFF7ED),
                    iconColor = Color(0xFFFF9800),
                    onClick = onVCard,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Third Row - SC Master + SC Writer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedDashboardCard(
                    title = "SC Master",
                    subtitle = "Read DESFire Cards",
                    icon = Icons.Filled.Star,
                    backgroundColor = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF9C27B0),
                    onClick = onSCMaster,
                    modifier = Modifier.weight(1f)
                )
            }

        }
    }
}

@Composable
fun EnhancedDashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "card_scale"
    )

    Card(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier
            .scale(scale)
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = iconColor
                )

                Column {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF4A5568)
                    )
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
fun LogoutSection(onLogout: () -> Unit, isVisible: Boolean) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(1000, delayMillis = 600)
        ) + fadeIn(animationSpec = tween(1000, delayMillis = 600))
    ) {
        Button(
            onClick = {
                UserSession.clearSession(context)
                onLogout()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF8B5CF6) // PrimaryPurple
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                Icons.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Logout",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Helper Functions
fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Morning"
        in 12..17 -> "Afternoon"
        else -> "Evening"
    }
}

fun loadUserData(context: Context, onResult: (User?) -> Unit) {
    val userId = UserSession.userId

    if (userId != -1) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://vcard.tecgs.com:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)
        api.getUserProfile(userId).enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    onResult(response.body()?.user)
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                onResult(null)
            }
        })
    } else {
        onResult(null)
    }
}