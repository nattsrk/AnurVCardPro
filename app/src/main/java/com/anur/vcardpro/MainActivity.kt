package com.anur.vcardpro

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.anur.vcardpro.model.LoginResponse
import com.anur.vcardpro.network.ApiService
import com.anur.vcardpro.ui.*
import kotlinx.coroutines.delay
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import com.anur.vcardpro.ui.theme.VCardProTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.window.Dialog

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.nio.charset.Charset

class MainActivity : ComponentActivity() {
    // Add this mutable state to hold NFC data
    private val _nfcTag = mutableStateOf<Tag?>(null)
    val nfcTag: State<Tag?> = _nfcTag

    // Mode management properties - keeping these for future use/compatibility
    private var currentNfcMode = NFCMode.NONE
    private var writeCallback: ((String) -> Unit)? = null

    // Define NFC modes - keeping for potential future use
    enum class NFCMode {
        NONE,
        READ_MODE,    // For SCMaster
        WRITE_MODE    // For SCWriter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        UserSession.loadSession(this)

        setContent {
            VCardProTheme {
                VCardApp(activity = this@MainActivity)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent?.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent?.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent?.action) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            println("NFC Tag detected in mode: $currentNfcMode")

            tag?.let {
                when (currentNfcMode) {
                    NFCMode.WRITE_MODE -> {
                        // Handle write operation - call callback directly
                        println("Processing write operation")
                        writeCallback?.invoke("tag_detected")
                    }
                    NFCMode.READ_MODE -> {
                        // Handle read operation for SCMaster
                        println("Processing read operation for SCMaster")
                        _nfcTag.value = it
                    }
                    NFCMode.NONE -> {
                        // Default behavior - treat as read (used by integrated SCMaster)
                        println("No specific mode set, defaulting to read")
                        _nfcTag.value = it
                    }
                }
            }
        }
    }

    // Add function to clear tag after processing
    fun clearNfcTag() {
        _nfcTag.value = null
    }

    // Mode management methods - keeping for compatibility
    fun setReadMode() {
        println("Setting NFC to READ mode")
        currentNfcMode = NFCMode.READ_MODE
    }

    fun setWriteMode(callback: (String) -> Unit) {
        println("Setting NFC to WRITE mode")
        currentNfcMode = NFCMode.WRITE_MODE
        writeCallback = callback
    }

    fun clearNfcMode() {
        println("Clearing NFC mode")
        currentNfcMode = NFCMode.NONE
        writeCallback = null
    }

    // Write functionality - keeping for compatibility/future use
    fun writeToCard(
        tag: Tag,
        fullName: String, phone: String, email: String, organization: String,
        emergencyName: String, emergencyPhone: String, bloodGroup: String,
        policyHolder: String, insurerName: String, policyNumber: String
    ): String {
        return try {
            val records = mutableListOf<NdefRecord>()

            // Create VCard record if personal info exists
            if (fullName.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())) {
                val vCard = buildString {
                    append("BEGIN:VCARD\n")
                    append("VERSION:3.0\n")
                    append("FN:$fullName\n")
                    if (phone.isNotBlank()) append("TEL:$phone\n")
                    if (email.isNotBlank()) append("EMAIL:$email\n")
                    if (organization.isNotBlank()) append("ORG:$organization\n")
                    append("END:VCARD")
                }

                records.add(NdefRecord.createMime("text/vcard", vCard.toByteArray(Charset.forName("UTF-8"))))
            }

            // Create Emergency record
            if (emergencyName.isNotBlank() && emergencyPhone.isNotBlank()) {
                val emergency = buildString {
                    append("EMERGENCY CONTACT INFORMATION\n\n")
                    append("Name: $emergencyName\n")
                    append("Mobile: $emergencyPhone\n")
                    if (bloodGroup.isNotBlank()) append("Blood Group: $bloodGroup\n")
                }

                records.add(NdefRecord.createTextRecord("en", emergency))
            }

            // Create Insurance record
            if (policyHolder.isNotBlank()) {
                val insurance = buildString {
                    append("LIFE INSURANCE INFORMATION\n\n")
                    append("Policyholder: $policyHolder\n")
                    if (insurerName.isNotBlank()) append("Insurer: $insurerName\n")
                    if (policyNumber.isNotBlank()) append("Policy Number: $policyNumber\n")
                }

                records.add(NdefRecord.createTextRecord("en", insurance))
            }

            if (records.isEmpty()) {
                return "No valid data to write"
            }

            // Write to card
            val ndefMessage = NdefMessage(records.toTypedArray())
            val ndef = Ndef.get(tag)

            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return "Card is read-only"
                }

                if (ndefMessage.toByteArray().size > ndef.maxSize) {
                    ndef.close()
                    return "Data too large for card"
                }

                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                "Successfully written ${records.size} records!"
            } else {
                // Try to format
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    ndefFormatable.connect()
                    ndefFormatable.format(ndefMessage)
                    ndefFormatable.close()
                    "Card formatted and written with ${records.size} records!"
                } else {
                    "Card is not NDEF compatible"
                }
            }

        } catch (e: Exception) {
            "Write error: ${e.message}"
        }
    }
}

@Composable
fun VCardApp(activity: MainActivity){
    var currentScreen by remember { mutableStateOf("splash") }

    when (currentScreen) {
        "splash" -> SplashScreen {
            currentScreen = if (UserSession.isLoggedIn()) {
                "dashboard"
            } else {
                "login"
            }
        }
        "login" -> LoginScreen { currentScreen = "dashboard" }
        "dashboard" -> DashboardScreen(
            onProfile = { currentScreen = "profile" },
            onContacts = { currentScreen = "contacts" },
            onReceived = { currentScreen = "received" },
            onVCard = { currentScreen = "vcard" },
            onSCMaster = { currentScreen = "scmaster" },
            onBuyNewPolicy = { currentScreen = "buy_new_policy" },  // ✅ NEW
            onInsuranceManager = { currentScreen = "insurance_manager" },
            onLogout = {
                UserSession.clearSession(activity)
                currentScreen = "login"
            }
        )
        "profile" -> MyProfileScreen { currentScreen = "dashboard" }
        "contacts" -> ContactsScreen { currentScreen = "dashboard" }
        "received" -> ReceivedScreen { currentScreen = "dashboard" }
        "vcard" -> VCardScreen { currentScreen = "dashboard" }
        "scmaster" -> SCMasterScreen(
            activity = activity,
            onBack = { currentScreen = "dashboard" }
        )
        "buy_new_policy" -> BuyNewPolicyScreen(
            onBack = { currentScreen = "dashboard" }
        )
        "insurance_manager" -> InsuranceManagerScreen { currentScreen = "dashboard" }
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF8B5CF6), // Purple
                        Color(0xFFEC4899)  // Pink
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📱", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(30.dp))
            Text("Anur VCard", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Smart Contact Sharing", fontSize = 18.sp, color = Color.White.copy(alpha = 0.9f))

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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var manualUserId by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
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
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ANUR Logo Placeholder (logo file not found in drawable directory)
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "ANUR",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    "Onewealth Card",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

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

                OutlinedTextField(
                    value = manualUserId,
                    onValueChange = { manualUserId = it },
                    label = { Text("Emp Code") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter user ID: 1, 2, 3, etc.") }
                )

                // Terms & Conditions Checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF8B5CF6),
                            uncheckedColor = Color.White.copy(alpha = 0.7f),
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        "I accept the ",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        "Terms & Conditions",
                        fontSize = 12.sp,
                        color = Color(0xFFEC4899),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { showTermsDialog = true }
                    )
                }

                Button(
                    onClick = {
                        if (username.isNotEmpty() && password.isNotEmpty() && manualUserId.isNotEmpty() && termsAccepted) {
                            val userIdInt = manualUserId.toIntOrNull()
                            if (userIdInt == null || userIdInt <= 0) {
                                Toast.makeText(context, "Please enter a valid User ID", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

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
                                            UserSession.userId = userId
                                            UserSession.userName = userName
                                            UserSession.userEmail = userEmail
                                            UserSession.saveSession(context)
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
                            if (!termsAccepted) {
                                Toast.makeText(context, "Please accept the Terms & Conditions", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
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

        // Terms & Conditions Dialog
        if (showTermsDialog) {
            Dialog(onDismissRequest = { showTermsDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            "Terms & Conditions",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            "This application is a Proof of Concept (POC) developed for demonstration purposes only. *The concepts, ideas, and intellectual property (IP) presented in this app may not be copied, replicated, or used without explicit written permission.\n\n" +
                        "By using this application, you acknowledge that:\n\n" +
                                "1. This is a demonstration/prototype application\n" +
                                "2. All concepts and ideas are protected intellectual property\n" +
                                "3. This app is intended for authorized employees only\n" +
                                "4. Any unauthorized use, copying, or distribution is prohibited\n" +
                                "5. This application should not be considered production-ready and is provided 'as-is' for evaluation purposes only.\n",
                        fontSize = 14.sp,
                            color = Color.White,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showTermsDialog = false }) {
                                Text("Close", color = Color(0xFF8B5CF6))
                            }
                        }
                    }
                }
            }
        }
    }
}