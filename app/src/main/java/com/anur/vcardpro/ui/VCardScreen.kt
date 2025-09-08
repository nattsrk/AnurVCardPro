package com.anur.vcardpro.ui

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.anur.vcardpro.model.User
import com.anur.vcardpro.model.UserResponse
import com.anur.vcardpro.network.ApiService
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import com.anur.vcardpro.UserSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VCardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var vCardUrl by remember { mutableStateOf("") }
    var webViewLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Load user profile to get VCard URL
    LaunchedEffect(Unit) {
        //val userId = context.getSharedPreferences("VCardApp", Context.MODE_PRIVATE)
          //  .getInt("userId", -1)
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
                        val userData = response.body()?.user
                        userData?.profileSlug?.let { slug ->
                            vCardUrl = "https://vcard.tecgs.com:3000/profile/$slug"
                            Log.d("VCard", "Loading VCard URL: $vCardUrl")
                        } ?: run {
                            errorMessage = "Profile slug not found"
                        }
                    } else {
                        errorMessage = "Failed to load profile"
                    }
                    isLoading = false
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                    errorMessage = "Network error: ${t.message}"
                    isLoading = false
                    Log.e("VCard", "API call failed", t)
                }
            })
        } else {
            errorMessage = "Please login again"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My VCard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (vCardUrl.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                webView?.reload()
                                webViewLoading = true
                            }
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    // Loading profile
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading VCard...")
                        }
                    }
                }

                errorMessage.isNotEmpty() -> {
                    // Error state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                "❌ Error",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onBack) {
                                Text("Go Back")
                            }
                        }
                    }
                }

                vCardUrl.isNotEmpty() -> {
                    // WebView to load VCard URL
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                WebView(context).apply {
                                    webView = this

                                    // Configure WebView
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        setSupportZoom(true)
                                    }

                                    // Set WebView client
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: android.graphics.Bitmap?
                                        ) {
                                            super.onPageStarted(view, url, favicon)
                                            webViewLoading = true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            webViewLoading = false
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            errorCode: Int,
                                            description: String?,
                                            failingUrl: String?
                                        ) {
                                            super.onReceivedError(view, errorCode, description, failingUrl)
                                            webViewLoading = false
                                            Toast.makeText(
                                                context,
                                                "Error loading VCard: $description",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                    // Load the VCard URL
                                    loadUrl(vCardUrl)
                                }
                            }
                        )

                        // Show loading indicator over WebView
                        if (webViewLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Loading VCard...")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}