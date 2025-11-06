package com.anur.vcardpro.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anur.vcardpro.UserSession
import com.anur.vcardpro.model.ContactShare
import com.anur.vcardpro.model.ReceivedContactResponse
import com.anur.vcardpro.model.VisitorContactData
import com.anur.vcardpro.network.ApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun ReceivedScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val contacts = remember { mutableStateListOf<ContactShare>() }
    val coroutineScope = rememberCoroutineScope()

    val gson = GsonBuilder()
        .setLenient()
        .create()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://vcard.tecgs.com:3000/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api = retrofit.create(ApiService::class.java)
    val userId = UserSession.userId

    LaunchedEffect(Unit) {
        if (userId != -1) {
            api.getReceivedContacts(userId).enqueue(object : Callback<ReceivedContactResponse> {
                override fun onResponse(
                    call: Call<ReceivedContactResponse>,
                    response: Response<ReceivedContactResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.contacts?.let {
                            contacts.clear()
                            contacts.addAll(it)
                        }
                    } else {
                        Toast.makeText(context, "Failed to get contacts", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ReceivedContactResponse>, t: Throwable) {
                    Toast.makeText(context, "API call failed: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        } else {
            Toast.makeText(context, "User ID not found", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Received Contacts") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No contacts received.")
            }
        } else {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(contacts) { contact ->
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFEC4899), // Pink
                                            Color(0xFF3B82F6)  // Blue
                                        )
                                    )
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val gson = Gson()
                                val visitorData = contact.visitorContactData?.let {
                                    try {
                                        gson.fromJson(it, VisitorContactData::class.java)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                val name = visitorData?.name ?: "Unknown"
                                val email = contact.viewerEmail ?: visitorData?.email ?: "N/A"
                                val phone = contact.viewerPhone ?: visitorData?.phone ?: "N/A"
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Name",
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone",
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = phone,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
