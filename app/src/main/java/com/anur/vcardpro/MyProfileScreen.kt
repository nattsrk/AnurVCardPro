package com.anur.vcardpro.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anur.vcardpro.model.User
import com.anur.vcardpro.model.UserResponse
import com.anur.vcardpro.network.ApiService
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

import com.anur.vcardpro.UserSession
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isVisible by remember { mutableStateOf(false) }

    // Load user data
    LaunchedEffect(Unit) {
        //val userId = context.getSharedPreferences("VCardApp", Context.MODE_PRIVATE)
          //  .getInt("userId", -1)

        val userId = UserSession.userId
        Log.d("Profile", "User ID from SharedPrefs = $userId")

        if (userId != -1) {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://vcard.tecgs.com:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(ApiService::class.java)
            api.getUserProfile(userId).enqueue(object : Callback<UserResponse> {
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        user = response.body()?.user
                        isVisible = true
                    } else {
                        Log.e("Profile", "Failed: ${response.message()}")
                    }
                    isLoading = false
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    Log.e("Profile", "Error: ${t.message}")
                    isLoading = false
                }
            })
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Edit profile */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            user?.let { userData ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(bottom = 16.dp)
                ) {
                    // Hero Profile Section
                    ProfileHeroSection(
                        user = userData,
                        isVisible = isVisible
                    )

                    // Contact Information Section
                    ContactInfoSection(
                        user = userData,
                        context = context,
                        isVisible = isVisible
                    )

                    // Professional Information Section
                    ProfessionalInfoSection(
                        user = userData,
                        context = context,
                        isVisible = isVisible
                    )

                    // Profile Statistics Section
                    ProfileStatsSection(isVisible = isVisible)
                }
            } ?: run {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Unable to load profile", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeroSection(user: User, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFFFC1C1),
                                    Color(0xFFFF8A80)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = user.name
                        .split(" ")
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString("")
                        .take(2)

                    Text(
                        text = initials,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Name and Title
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                user.company?.let { company ->
                    Text(
                        text = company,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF8B5CF6).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Active",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8B5CF6),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactInfoSection(
    user: User,
    context: Context,
    isVisible: Boolean
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(600, delayMillis = 200)) + fadeIn(animationSpec = tween(600, delayMillis = 200))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SectionHeader("Contact Information", Icons.Filled.Phone)

            Spacer(modifier = Modifier.height(12.dp))

            // Email
            ProfileInfoCard(
                icon = Icons.Filled.Email,
                title = "Email",
                value = user.email,
                subtitle = "Primary",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${user.email}")
                    }
                    context.startActivity(intent)
                }
            )

            // Phone
            user.phone?.let { phone ->
                Spacer(modifier = Modifier.height(4.dp))
                ProfileInfoCard(
                    icon = Icons.Filled.Phone,
                    title = "Phone",
                    value = phone,
                    subtitle = "Mobile",
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun ProfessionalInfoSection(user: User, context: Context, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(600, delayMillis = 400)) + fadeIn(animationSpec = tween(600, delayMillis = 400))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SectionHeader("Professional", Icons.Filled.Person)

            Spacer(modifier = Modifier.height(8.dp))

            // Company
            user.company?.let { company ->
                ProfileInfoCard(
                    icon = Icons.Filled.Star,
                    title = "Organization",
                    value = company,
                    subtitle = "Current workplace"
                )
            }

            // LinkedIn
            user.linkedin?.let { linkedin ->
                Spacer(modifier = Modifier.height(8.dp))
                ProfileInfoCard(
                    icon = Icons.Filled.Share,
                    title = "LinkedIn",
                    value = "View Profile",
                    subtitle = "Professional network",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedin))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileStatsSection(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(600, delayMillis = 600)) + fadeIn(animationSpec = tween(600, delayMillis = 600))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SectionHeader("Activity", Icons.Filled.Star)

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem("23", "Contacts\nShared", Color(0xFF8B5CF6))
                    StatItem("7", "Received", Color(0xFF8B5CF6))
                    StatItem("156", "Profile\nViews", Color(0xFFFF9800))
                }
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProfileInfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                subtitle?.let { sub ->
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action indicator
            if (onClick != null) {
                Text(
                    "›",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}