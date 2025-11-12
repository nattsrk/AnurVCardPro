package com.anur.vcardpro.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Divider
import androidx.compose.ui.text.font.FontStyle
import java.text.SimpleDateFormat
import java.util.*
import java.nio.charset.Charset
import com.anur.vcardpro.MainActivity
import com.anur.vcardpro.UserSession
import com.anur.vcardpro.model.InsuranceResponse
import com.anur.vcardpro.model.InsurancePolicy
import com.anur.vcardpro.network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope


// Data classes for structured data
data class InsuranceInfo(
    var policyHolder: String = "",
    var age: String = "",
    var insurer: String = "",
    var policyType: String = "",
    var premium: String = "",
    var sumAssured: String = "",
    var policyStart: String = "",
    var policyEnd: String = "",
    var status: String = "Active",
    var contact: String = "",
    var mobile: String = "",
    var policyNumber: String = ""
)

data class ExtractedCardData(
    val personalInfo: Map<String, String> = emptyMap(),
    val emergencyContact: Map<String, String> = emptyMap(),
    val insuranceInfo: List<Map<String, String>> = emptyList(),
    val otherData: Map<String, String> = emptyMap()
)

// New data class for sync comparison
data class SyncComparisonData(
    val cardPolicies: List<Map<String, String>>,
    val backendPolicies: List<InsurancePolicy>,
    val differences: List<String>,
    val needsSync: Boolean
)

@Composable
fun SCMasterScreen(activity: MainActivity, onBack: () -> Unit) {
    var currentMode by remember { mutableStateOf("READ") }

    // Read mode states
    var nfcStatus by remember { mutableStateOf("Initializing NFC...") }
    var cardDetected by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf("") }
    var cardDataStatus by remember { mutableStateOf("") }
    var extractedData by remember { mutableStateOf<ExtractedCardData?>(null) }

    // Write mode states - Card Personalization
    var writeStatus by remember { mutableStateOf("Ready to write") }
    var isWriting by remember { mutableStateOf(false) }

    // Write mode states - Data Sync
    var backendInsuranceData by remember { mutableStateOf<InsuranceResponse?>(null) }
    var isLoadingBackend by remember { mutableStateOf(false) }
    var backendError by remember { mutableStateOf<String?>(null) }
    var syncComparison by remember { mutableStateOf<SyncComparisonData?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf("") }


    // Write mode states - Card Personalization
    var vCardSlug by remember {
        mutableStateOf("")  // ✅ Start empty
    }
    // Write mode - Personal Info
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var jobTitle by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    // Write mode - Emergency Contact
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var emergencyLocation by remember { mutableStateOf("") }
    var emergencyRelationship by remember { mutableStateOf("") }


    // Write mode - Insurance List (Multiple Records) - MUTABLE
    var insuranceList by remember {
        mutableStateOf(mutableListOf(
            InsuranceInfo(
                policyHolder = "",
                age = "",
                insurer = "",
                policyType = "",
                premium = "",
                sumAssured = "",
                policyStart = "",
                policyEnd = "",
                status = "Active",
                contact = "",
                mobile = "",
                policyNumber = "POL/${System.currentTimeMillis()}"
            )
        ))
    }


    val context = LocalContext.current
    val nfcTag by activity.nfcTag

    // NEW: Function to fetch backend insurance data
    fun fetchBackendInsuranceData() {
        val userId = UserSession.userId
        if (userId == -1) {
            backendError = "User not logged in"
            return
        }

        isLoadingBackend = true
        backendError = null

        val retrofit = Retrofit.Builder()
            .baseUrl("https://vcard.tecgs.com:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)
        api.getUserInsurance(userId).enqueue(object : Callback<InsuranceResponse> {
            override fun onResponse(call: Call<InsuranceResponse>, response: Response<InsuranceResponse>) {
                isLoadingBackend = false
                if (response.isSuccessful && response.body()?.success == true) {
                    backendInsuranceData = response.body()
                    // Compare with card data if available
                    extractedData?.let { cardData ->
                        syncComparison = compareCardAndBackendData(cardData.insuranceInfo, response.body()?.policies ?: emptyList())
                    }
                } else {
                    backendError = "Failed to load backend data: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<InsuranceResponse>, t: Throwable) {
                isLoadingBackend = false
                backendError = "Network error: ${t.message}"
            }
        })
    }

    // Helper functions for sync
    fun parsePremiumFromString(premium: String): Double {
        return premium.replace("₹", "").replace(",", "")
            .replace("(Annual)", "").trim()
            .toDoubleOrNull() ?: 0.0
    }

    fun parseSumAssuredFromString(sumAssured: String): Double {
        return sumAssured.replace("₹", "").replace(",", "")
            .replace("(1 Crore)", "").trim()
            .toDoubleOrNull() ?: 0.0
    }
    // NEW: Function to sync card data to backend (upload card policies to backend)
    fun syncCardToBackend() {
        val comparison = syncComparison ?: return
        val userId = UserSession.userId

        if (userId == -1) {
            syncStatus = "Error: User not logged in"
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        isSyncing = true
        syncStatus = "Starting sync from card to backend..."

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://vcard.tecgs.com:3000/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        // Upload card-only policies to backend
        val cardOnlyPolicies = comparison.cardPolicies.filter { cardPolicy ->
            comparison.backendPolicies.none { it.policyNumber == cardPolicy["Policy Number"] }
        }

        if (cardOnlyPolicies.isEmpty()) {
            isSyncing = false
            syncStatus = "No new policies to sync from card"
            Toast.makeText(context, "No new policies to sync from card", Toast.LENGTH_SHORT).show()
            return
        }

        syncStatus = "Uploading ${cardOnlyPolicies.size} policies to backend..."

        var successCount = 0
        var failCount = 0
        val totalToSync = cardOnlyPolicies.size

        // Upload each card-only policy to backend
        cardOnlyPolicies.forEachIndexed { index, cardPolicy ->
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val calendar = java.util.Calendar.getInstance()

            // Extract tenure from policy or default to 10 years
            val tenureString = cardPolicy["Policy End"]?.substringBefore("/") ?: "10"
            val tenure = tenureString.toIntOrNull() ?: 10
            calendar.add(java.util.Calendar.YEAR, tenure)
            val endDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)

            val requestBody = mapOf(
                "userId" to userId,
                "policyNumber" to (cardPolicy["Policy Number"] ?: ""),
                "policyType" to (cardPolicy["Policy Type"] ?: ""),
                "insurerName" to (cardPolicy["Insurer"] ?: ""),
                "premiumAmount" to parsePremiumFromString(cardPolicy["Premium"] ?: "0"),
                "sumAssured" to parseSumAssuredFromString(cardPolicy["Sum Assured"] ?: "0"),
                "policyStartDate" to (cardPolicy["Policy Start"] ?: currentDate),
                "policyEndDate" to (cardPolicy["Policy End"] ?: endDate),
                "status" to (cardPolicy["Status"] ?: "Active")
            )

            api.createInsurancePolicy(requestBody).enqueue(object : retrofit2.Callback<Map<String, Any>> {
                override fun onResponse(
                    call: retrofit2.Call<Map<String, Any>>,
                    response: retrofit2.Response<Map<String, Any>>
                ) {
                    if (response.isSuccessful && response.body()?.get("success") == true) {
                        successCount++
                        syncStatus = "Synced ${successCount}/${totalToSync} card policies to backend..."
                        android.util.Log.d("SYNC", "Policy ${cardPolicy["Policy Number"]} synced to backend")
                    } else {
                        failCount++
                        val errorMsg = response.body()?.get("message") as? String ?: "Unknown error"
                        syncStatus = "Synced ${successCount}/${totalToSync}, Failed: $failCount"
                        android.util.Log.e("SYNC", "Policy sync failed: $errorMsg")
                    }

                    // Check if all uploads completed
                    if (successCount + failCount == totalToSync) {
                        isSyncing = false

                        syncStatus = "Sync complete! $successCount policies synced to backend"
                        Toast.makeText(
                            context,
                            "Successfully synced $successCount policies to backend!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<Map<String, Any>>, t: Throwable) {
                    failCount++
                    android.util.Log.e("SYNC", "Network error: ${t.message}", t)

                    if (successCount + failCount == totalToSync) {
                        isSyncing = false
                        syncStatus = "Sync failed: ${t.message}"
                        Toast.makeText(
                            context,
                            "Network error: ${t.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
        }
    }

    // NEW: Function to sync backend policies to card (write missing policies to card)
    fun syncBackendToCard() {
        val comparison = syncComparison ?: return

        // Identify backend-only policies (policies in backend but not in card)
        val backendOnlyPolicies = comparison.backendPolicies.filter { backendPolicy ->
            comparison.cardPolicies.none { it["Policy Number"] == backendPolicy.policyNumber }
        }

        if (backendOnlyPolicies.isEmpty()) {
            syncStatus = "No missing policies to sync to card"
            Toast.makeText(context, "No missing policies to sync to card", Toast.LENGTH_SHORT).show()
            return
        }

        syncStatus = "Found ${backendOnlyPolicies.size} policies to sync to card. Tap card to write them."
        isSyncing = true
        Toast.makeText(context, "Please tap your NFC card to write missing policies", Toast.LENGTH_SHORT).show()
    }

    fun syncBackendPoliciesToCard(tag: Tag): String {
        val comparison = syncComparison ?: return "No sync data available"

        val backendOnlyPolicies = comparison.backendPolicies.filter { backendPolicy ->
            comparison.cardPolicies.none { it["Policy Number"] == backendPolicy.policyNumber }
        }

        if (backendOnlyPolicies.isEmpty()) {
            return "No policies to sync to card"
        }

        return try {
            // 🔹 USE ALREADY-READ CARD DATA FROM READ MODE
            // User MUST read card in READ mode before syncing
            val existingData = extractedData ?: return "Please read the card first in READ mode before syncing"

            // Verify we have card data
            if (existingData.personalInfo.isEmpty() &&
                existingData.emergencyContact.isEmpty() &&
                existingData.insuranceInfo.isEmpty()) {
                return "No card data found. Please read the card in READ mode first."
            }

            // Create records from existing data
            val records = mutableListOf<NdefRecord>()

            // 1. VCard URL Record (preserve existing or use default)
            val vCardUrl = "https://vcard.tecgs.com:3000/profile/${vCardSlug}"
            records.add(NdefRecord.createUri(vCardUrl))

            // 2. VCard Record (preserve existing personal info)
            if (existingData.personalInfo.isNotEmpty()) {
                val vCard = buildString {
                    append("BEGIN:VCARD\n")
                    append("VERSION:3.0\n")
                    append("FN:${existingData.personalInfo["Full Name"] ?: "Unknown"}\n")
                    existingData.personalInfo["Phone"]?.let { append("TEL:$it\n") }
                    existingData.personalInfo["Email"]?.let { append("EMAIL:$it\n") }
                    existingData.personalInfo["Organization"]?.let { append("ORG:$it\n") }
                    existingData.personalInfo["Job Title"]?.let { append("TITLE:$it\n") }
                    existingData.personalInfo["Address"]?.let { append("ADR:$it\n") }
                    append("END:VCARD")
                }
                records.add(NdefRecord.createMime("text/vcard", vCard.toByteArray(Charset.forName("UTF-8"))))
            }

            // 3. Emergency Contact Record (preserve existing)
            if (existingData.emergencyContact.isNotEmpty()) {
                val emergency = buildString {
                    append("EMERGENCY CONTACT INFORMATION\n\n")
                    append("Name: ${existingData.emergencyContact["Name"] ?: ""}\n")
                    append("Mobile: ${existingData.emergencyContact["Mobile"] ?: ""}\n")
                    existingData.emergencyContact["Blood Group"]?.let { append("Blood Group: $it\n") }
                    existingData.emergencyContact["Location"]?.let { append("Location: $it\n") }
                    existingData.emergencyContact["Relationship"]?.let { append("Relationship: $it\n") }
                }
                records.add(NdefRecord.createTextRecord("en", emergency))
            }

            // 4. Existing Insurance Records (preserve card policies)
            existingData.insuranceInfo.forEach { policy ->
                val insuranceData = buildString {
                    append("INSURANCE INFORMATION - POLICY\n\n")
                    append("Policyholder: ${policy["Policyholder"] ?: ""}\n")
                    policy["Age"]?.let { append("Age: $it\n") }
                    policy["Insurer"]?.let { append("Insurer: $it\n") }
                    policy["Policy Type"]?.let { append("Policy Type: $it\n") }
                    policy["Premium"]?.let { append("Premium: $it\n") }
                    policy["Sum Assured"]?.let { append("Sum Assured: $it\n") }
                    policy["Policy Start"]?.let { append("Policy Start: $it\n") }
                    policy["Policy End"]?.let { append("Policy End: $it\n") }
                    append("Status: ${policy["Status"] ?: "Active"}\n")
                    policy["Contact"]?.let { append("Contact: $it\n") }
                    policy["Mobile"]?.let { append("Mobile: $it\n") }
                    policy["Policy Number"]?.let { append("Policy Number: $it\n") }
                }
                records.add(NdefRecord.createTextRecord("en", insuranceData))
            }

            // 5. Add missing backend policies
            backendOnlyPolicies.forEach { backendPolicy ->
                val insuranceData = buildString {
                    append("INSURANCE INFORMATION - POLICY\n\n")
                    append("Policyholder: ${UserSession.userName}\n")
                    append("Insurer: ${backendPolicy.insurerName}\n")
                    append("Policy Type: ${backendPolicy.policyType}\n")
                    append("Premium: ₹${backendPolicy.premiumAmount}\n")
                    append("Sum Assured: ₹${backendPolicy.sumAssured}\n")
                    append("Policy Start: ${backendPolicy.policyStartDate}\n")
                    append("Policy End: ${backendPolicy.policyEndDate}\n")
                    append("Status: ${backendPolicy.status}\n")
                    append("Policy Number: ${backendPolicy.policyNumber}\n")
                }
                records.add(NdefRecord.createTextRecord("en", insuranceData))
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
                    return "Data too large for card (${ndefMessage.toByteArray().size} bytes > ${ndef.maxSize} bytes)"
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                "Successfully synced ${backendOnlyPolicies.size} new policies! Total: ${existingData.insuranceInfo.size + backendOnlyPolicies.size} policies on card"
            } else {
                val ndefFormatable = NdefFormatable.get(tag)
                if (ndefFormatable != null) {
                    ndefFormatable.connect()
                    ndefFormatable.format(ndefMessage)
                    ndefFormatable.close()
                    "Card formatted and synced with ${backendOnlyPolicies.size} new policies!"
                } else {
                    "Card is not NDEF compatible"
                }
            }
        } catch (e: Exception) {
            "Sync error: ${e.message}"
        }
    }

    // Handle NFC tag detection
    LaunchedEffect(nfcTag) {
        nfcTag?.let { tag ->
            if (currentMode == "READ") {                                    // 1 OPEN {
                val result = handleCardDetection(tag)
                cardDataStatus = result.first
                extractedData = result.second
                cardDetected = true
                lastTapTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                // ADDED: Update syncComparison with fresh card data if backend data exists
                backendInsuranceData?.let { backend ->                      // 2 OPEN {
                    syncComparison = compareCardAndBackendData(
                        extractedData?.insuranceInfo ?: emptyList(),
                        backend.policies ?: emptyList()
                    )
                }                                                           // 2 CLOSE }
            }                                                               // 1 CLOSE }
            else if (currentMode == "WRITE") {
                if (isWriting) {
                    // Card Personalization - Write data to card
                    writeStatus = "Card detected! Writing structured data..."
                    val result = writeStructuredDataToCard(
                        tag, vCardSlug,
                        fullName, phone, email, organization, jobTitle, address,
                        emergencyName, emergencyPhone, bloodGroup, emergencyLocation, emergencyRelationship,
                        insuranceList
                    )

                    writeStatus = result
                    isWriting = false

                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()

                    kotlinx.coroutines.delay(3000)
                    writeStatus = "Ready to write again"
                } else if (isSyncing) {
                    // Data Sync - Write missing policies to card
                    writeStatus = "Card detected! Syncing missing policies..."
                    val result = syncBackendPoliciesToCard(tag)

                    writeStatus = result
                    isSyncing = false

                    Toast.makeText(context, result, Toast.LENGTH_LONG).show()

                    kotlinx.coroutines.delay(3000)
                    writeStatus = "Ready to sync again"
                }
            }
            activity.clearNfcTag()
        }
    }

    // NFC setup
    DisposableEffect(Unit) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        if (nfcAdapter == null) {
            nfcStatus = "NFC not supported on this device"
        } else if (!nfcAdapter.isEnabled) {
            nfcStatus = "Please enable NFC in Settings"
        } else {
            nfcStatus = "Tap your DESFire card to read data"
            val intent = Intent(activity, activity.javaClass).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                activity, 0, intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val intentFilters = arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            )
            val techList = arrayOf(arrayOf(Ndef::class.java.name))
            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFilters, techList)
        }
        onDispose {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            nfcAdapter?.disableForegroundDispatch(activity)
        }
    }

    // Auto-fetch backend data when entering WRITE mode
    LaunchedEffect(currentMode) {
        if (currentMode == "WRITE") {
            fetchBackendInsuranceData()
        }
    }

// Auto-populate vCardSlug from card data when available
    LaunchedEffect(extractedData) {
        extractedData?.let { data ->
            // Extract slug from card's Website URL
            val websiteUrl = data.otherData["Website"] ?: ""
            if (websiteUrl.isNotEmpty() && vCardSlug.isEmpty()) {
                // Extract slug from URL like "https://vcard.tecgs.com:3000/profile/sri-123"
                val slug = websiteUrl.substringAfterLast("/")
                if (slug.isNotEmpty()) {
                    vCardSlug = slug  // ✅ Use existing slug from card!
                }
            }
        }
    }
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }
        }

        // Title and Mode Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SC Master - Enhanced with Sync",
                    fontSize = 20.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mode toggle buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { currentMode = "READ" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == "READ") Color(0xFFFF9800) else Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Read Card", color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { currentMode = "WRITE" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentMode == "WRITE") Color(0xFF616161) else Color.Gray
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Write Card", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (currentMode == "READ") nfcStatus else writeStatus,
                    fontSize = 14.sp,
                    color = Color.Black
                )

                if (currentMode == "READ" && cardDetected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Card detected at: $lastTapTime",
                        fontSize = 12.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cardDataStatus,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }

            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on mode
        when (currentMode) {
            "READ" -> {
                extractedData?.let { data ->
                    // Personal Information - Grey
                    if (data.personalInfo.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF616161))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "👤 Personal Information",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                data.personalInfo.forEach { (key, value) ->
                                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Text(
                                            text = "$key:",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.weight(0.35f)
                                        )
                                        Text(
                                            text = value,
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            modifier = Modifier.weight(0.65f)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Emergency Contact - Red to Black gradient
                    if (data.emergencyContact.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFF8B0000), Color(0xFF000000))
                                        )
                                    )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🚨 Emergency Contact",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    data.emergencyContact.forEach { (key, value) ->
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                            Text(
                                                text = "$key:",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.weight(0.35f)
                                            )
                                            Text(
                                                text = value,
                                                fontSize = 14.sp,
                                                color = Color.White,
                                                modifier = Modifier.weight(0.65f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // NEW: Insurance Information with Backend Comparison
                    if (data.insuranceInfo.isNotEmpty() || backendInsuranceData?.policies?.isNotEmpty() == true) {
                        // Card Insurance Data
                        if (data.insuranceInfo.isNotEmpty()) {
                            Text(
                                text = "📱 CARD INSURANCE DATA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            data.insuranceInfo.forEachIndexed { index, insurancePolicy ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF616161))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "🏥 Card Policy ${index + 1}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        insurancePolicy.forEach { (key, value) ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                                Text(
                                                    text = "$key:",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.weight(0.35f)
                                                )
                                                Text(
                                                    text = value,
                                                    fontSize = 14.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.weight(0.65f)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Backend Insurance Data
                        if (backendInsuranceData?.policies?.isNotEmpty() == true) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "🌐 BACKEND INSURANCE DATA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            backendInsuranceData?.policies?.forEachIndexed { index, policy ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "🏥 Backend Policy ${index + 1}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        listOf(
                                            "Policy Number" to policy.policyNumber,
                                            "Policy Type" to policy.policyType,
                                            "Insurer" to policy.insurerName,
                                            "Status" to policy.status,
                                            "Premium" to "₹${policy.premiumAmount}",
                                            "Sum Assured" to "₹${policy.sumAssured}"
                                        ).forEach { (key, value) ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                                Text(
                                                    text = "$key:",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = 0.9f),
                                                    modifier = Modifier.weight(0.35f)
                                                )
                                                Text(
                                                    text = value,
                                                    fontSize = 14.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.weight(0.65f)
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Backend Error Display
                        if (backendError != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "⚠️ Backend Error",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                    Text(
                                        text = backendError ?: "Unknown error",
                                        fontSize = 14.sp,
                                        color = Color(0xFF666666)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { fetchBackendInsuranceData() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                    }

                    // VCard Information - URL Only
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF000000), Color(0xFF8B0000))
                                    )
                                )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "📇 VCard Information",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Text(
                                        text = "Website:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.weight(0.35f)
                                    )
                                    Text(
                                        text = data.otherData["Website"] ?: "No URL found on card",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        modifier = Modifier.weight(0.65f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "WRITE" -> {
                Column {
                    // Card Personalization Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF424242))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🎴 CARD PERSONALIZATION",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Write static data to personalize your card",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    // Data Sync Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🔄 DATA SYNC",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Synchronize insurance policies between card and backend",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Data Sync UI
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Backend Data Loading
                            if (isLoadingBackend) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading backend data...", fontSize = 12.sp, color = Color.Gray)
                                }
                            }


                            // Sync Status
                            if (syncStatus.isNotEmpty()) {
                                Text(
                                    text = syncStatus,
                                    fontSize = 12.sp,
                                    color = Color.Blue,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
// Around line 1113-1125 (after the current LOAD BACKEND DATA button)

// Load Backend Data Button (if not loaded)
                            if (backendInsuranceData == null && !isLoadingBackend) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { fetchBackendInsuranceData() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                ) {
                                    Text("LOAD BACKEND DATA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }

// 🔹 ADD THIS NEW CODE HERE 🔹
// Refresh button (always show when data is loaded)
                            if (backendInsuranceData != null && !isLoadingBackend) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        backendInsuranceData = null
                                        backendError = null
                                        syncComparison = null
                                        fetchBackendInsuranceData()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                ) {
                                    Text("🔄 REFRESH BACKEND DATA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Sync Comparison and Buttons
                            syncComparison?.let { comparison ->
                                if (comparison.needsSync) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6))  // ✅ Purple theme
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "🔄 SYNC REQUIRED",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White  // ✅ Changed to white
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            comparison.differences.forEach { difference ->
                                                Text(
                                                    text = "• $difference",
                                                    fontSize = 12.sp,
                                                    color = Color.White  // ✅ Changed to white
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))

                                            // ✅ ONLY ONE BUTTON NOW - SYNC TO CARD
                                            Button(
                                                onClick = { syncBackendToCard() },
                                                enabled = !isSyncing,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),  // Pink accent
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("📥 SYNC TO CARD", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1BEE7))  // ✅ Light purple
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "✅ DATA IN SYNC",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B5CF6)  // Purple
                                            )
                                            Text(
                                                text = "Card and backend data are synchronized",
                                                fontSize = 14.sp,
                                                color = Color(0xFF666666)
                                            )
                                        }
                                    }
                                }
                            }

                            // Load Backend Data Button (if not loaded)
                            if (backendInsuranceData == null && !isLoadingBackend) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { fetchBackendInsuranceData() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                                ) {
                                    Text("LOAD BACKEND DATA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    // Card Personalization Form
                    EnhancedWriteFormSection(
                        vCardSlug = vCardSlug,
                        onVCardSlugChange = { vCardSlug = it },
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        email = email,
                        onEmailChange = { email = it },
                        organization = organization,
                        onOrganizationChange = { organization = it },
                        jobTitle = jobTitle,
                        onJobTitleChange = { jobTitle = it },
                        address = address,
                        onAddressChange = { address = it },
                        emergencyName = emergencyName,
                        onEmergencyNameChange = { emergencyName = it },
                        emergencyPhone = emergencyPhone,
                        onEmergencyPhoneChange = { emergencyPhone = it },
                        bloodGroup = bloodGroup,
                        onBloodGroupChange = { bloodGroup = it },
                        emergencyLocation = emergencyLocation,
                        onEmergencyLocationChange = { emergencyLocation = it },
                        emergencyRelationship = emergencyRelationship,
                        onEmergencyRelationshipChange = { emergencyRelationship = it },
                        insuranceList = insuranceList,
                        onInsuranceListChange = { insuranceList = it },
                        isWriting = isWriting,
                        onWriteClick = {
                            if (hasValidStructuredData(fullName, phone, email, emergencyName, emergencyPhone, insuranceList)) {
                                isWriting = true
                                writeStatus = "Tap your card to write structured data..."
                                Toast.makeText(context, "Please tap your NFC card now", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please fill at least one complete section", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))


                }
            }
        }
    }
}

@Composable
fun EnhancedWriteFormSection(
    vCardSlug: String,
    onVCardSlugChange: (String) -> Unit,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    organization: String,
    onOrganizationChange: (String) -> Unit,
    jobTitle: String,
    onJobTitleChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    emergencyName: String,
    onEmergencyNameChange: (String) -> Unit,
    emergencyPhone: String,
    onEmergencyPhoneChange: (String) -> Unit,
    bloodGroup: String,
    onBloodGroupChange: (String) -> Unit,
    emergencyLocation: String,
    onEmergencyLocationChange: (String) -> Unit,
    emergencyRelationship: String,
    onEmergencyRelationshipChange: (String) -> Unit,
    insuranceList: MutableList<InsuranceInfo>,
    onInsuranceListChange: (MutableList<InsuranceInfo>) -> Unit,
    isWriting: Boolean,
    onWriteClick: () -> Unit
) {
    Column {
        // VCard URL Section (First)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF616161))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📇 VCard URL", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "https://vcard.tecgs.com:3000/profile/",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = vCardSlug,
                        onValueChange = onVCardSlugChange,
                        label = { Text("Slug", color = Color.White) },
                        modifier = Modifier.weight(0.4f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Complete URL: https://vcard.tecgs.com:3000/profile/$vCardSlug",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Info Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF616161))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("👤 Personal Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Full Name", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        label = { Text("Phone", color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email", color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = organization,
                        onValueChange = onOrganizationChange,
                        label = { Text("Organization", color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = onJobTitleChange,
                        label = { Text("Job Title", color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = onAddressChange,
                    label = { Text("Address", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency Contact Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF616161))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🚨 Emergency Contact", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = emergencyName,
                        onValueChange = onEmergencyNameChange,
                        label = { Text("Emergency Contact Name", color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = emergencyPhone,
                        onValueChange = onEmergencyPhoneChange,
                        label = { Text("Emergency Phone", color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = onBloodGroupChange,
                        label = { Text("Blood Group", color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = emergencyLocation,
                        onValueChange = onEmergencyLocationChange,
                        label = { Text("Location", color = Color.White) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emergencyRelationship,
                    onValueChange = onEmergencyRelationshipChange,
                    label = { Text("Relationship", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Insurance Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF616161))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏥 Insurance Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Button(
                        onClick = {
                            val newList = insuranceList.toMutableList()
                            newList.add(InsuranceInfo())
                            onInsuranceListChange(newList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Insurance", tint = Color(0xFF616161))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Policy", color = Color(0xFF616161))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Display all insurance policies
                insuranceList.forEachIndexed { index, insurance ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Policy ${index + 1}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (insuranceList.size > 1) {
                                    IconButton(
                                        onClick = {
                                            val newList = insuranceList.toMutableList()
                                            newList.removeAt(index)
                                            onInsuranceListChange(newList)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Insurance form fields
                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = insurance.policyHolder,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(policyHolder = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Policyholder Name", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = insurance.age,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(age = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Age", color = Color.White) },
                                    modifier = Modifier.weight(0.5f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = insurance.insurer,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(insurer = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Insurer Name", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = insurance.policyType,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(policyType = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Policy Type", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = insurance.premium,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(premium = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Premium", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = insurance.sumAssured,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(sumAssured = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Sum Assured", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = insurance.policyStart,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(policyStart = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Policy Start", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = insurance.policyEnd,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(policyEnd = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Policy End", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = insurance.contact,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(contact = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Contact Email", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = insurance.mobile,
                                    onValueChange = {
                                        val newList = insuranceList.toMutableList()
                                        newList[index] = newList[index].copy(mobile = it)
                                        onInsuranceListChange(newList)
                                    },
                                    label = { Text("Mobile", color = Color.White) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = insurance.policyNumber,
                                onValueChange = {
                                    val newList = insuranceList.toMutableList()
                                    newList[index] = newList[index].copy(policyNumber = it)
                                    onInsuranceListChange(newList)
                                },
                                label = { Text("Policy Number", color = Color.White) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    if (index < insuranceList.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color.White.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Write Button
        Button(
            onClick = onWriteClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isWriting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            if (isWriting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Waiting for Card...")
            } else {
                Text("Write Structured Data to Smart Card", fontSize = 16.sp)
            }
        }
    }
}

// Enhanced structured data writing function
private fun writeStructuredDataToCard(
    tag: Tag,
    vCardSlug: String,
    fullName: String, phone: String, email: String, organization: String, jobTitle: String, address: String,
    emergencyName: String, emergencyPhone: String, bloodGroup: String, emergencyLocation: String, emergencyRelationship: String,
    insuranceList: MutableList<InsuranceInfo>
): String {
    return try {
        val records = mutableListOf<NdefRecord>()

        // 1. VCard URL Record (First)
        if (vCardSlug.isNotBlank()) {
            val vCardUrl = "https://vcard.tecgs.com:3000/profile/$vCardSlug"
            records.add(NdefRecord.createUri(vCardUrl))
        }

        // 2. VCard Record (Personal Details)
        if (fullName.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())) {
            val vCard = buildString {
                append("BEGIN:VCARD\n")
                append("VERSION:3.0\n")
                append("FN:$fullName\n")
                if (phone.isNotBlank()) append("TEL:$phone\n")
                if (email.isNotBlank()) append("EMAIL:$email\n")
                if (organization.isNotBlank()) append("ORG:$organization\n")
                if (jobTitle.isNotBlank()) append("TITLE:$jobTitle\n")
                if (address.isNotBlank()) append("ADR:$address\n")
                append("END:VCARD")
            }
            records.add(NdefRecord.createMime("text/vcard", vCard.toByteArray(Charset.forName("UTF-8"))))
        }

        // 3. Emergency Contact Record
        if (emergencyName.isNotBlank() && emergencyPhone.isNotBlank()) {
            val emergency = buildString {
                append("EMERGENCY CONTACT INFORMATION\n\n")
                append("Name: $emergencyName\n")
                append("Mobile: $emergencyPhone\n")
                if (bloodGroup.isNotBlank()) {
                    append("Blood Group: $bloodGroup")
                    if (bloodGroup.equals("O+", ignoreCase = true)) {
                        append(" (Universal Donor)")
                    }
                    append("\n")
                }
                if (emergencyLocation.isNotBlank()) append("Location: $emergencyLocation\n")
                if (emergencyRelationship.isNotBlank()) append("Relationship: $emergencyRelationship\n")
            }
            records.add(NdefRecord.createTextRecord("en", emergency))
        }

        // 4. Multiple Insurance Records
        insuranceList.forEachIndexed { index, insurance ->
            if (insurance.policyHolder.isNotBlank()) {
                val insuranceData = buildString {
                    append("INSURANCE INFORMATION - POLICY ${index + 1}\n\n")
                    append("Policyholder: ${insurance.policyHolder}\n")
                    if (insurance.age.isNotBlank()) append("Age: ${insurance.age}\n")
                    if (insurance.insurer.isNotBlank()) append("Insurer: ${insurance.insurer}\n")
                    if (insurance.policyType.isNotBlank()) append("Policy Type: ${insurance.policyType}\n")
                    if (insurance.premium.isNotBlank()) append("Premium: ${insurance.premium}\n")
                    if (insurance.sumAssured.isNotBlank()) append("Sum Assured: ${insurance.sumAssured}\n")
                    if (insurance.policyStart.isNotBlank()) append("Policy Start: ${insurance.policyStart}\n")
                    if (insurance.policyEnd.isNotBlank()) append("Policy End: ${insurance.policyEnd}\n")
                    append("Status: ${insurance.status}\n")
                    if (insurance.contact.isNotBlank()) append("Contact: ${insurance.contact}\n")
                    if (insurance.mobile.isNotBlank()) append("Mobile: ${insurance.mobile}\n")
                    if (insurance.policyNumber.isNotBlank()) append("Policy Number: ${insurance.policyNumber}\n")
                }
                records.add(NdefRecord.createTextRecord("en", insuranceData))
            }
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
            "Successfully written ${records.size} structured records to card!"
        } else {
            val ndefFormatable = NdefFormatable.get(tag)
            if (ndefFormatable != null) {
                ndefFormatable.connect()
                ndefFormatable.format(ndefMessage)
                ndefFormatable.close()
                "Card formatted and written with ${records.size} structured records!"
            } else {
                "Card is not NDEF compatible"
            }
        }
    } catch (e: Exception) {
        "Write error: ${e.message}"
    }
}

// Enhanced card detection for multiple insurance records
private fun handleCardDetection(tag: Tag): Pair<String, ExtractedCardData?> {
    return try {
        val tagId = tag.id.joinToString(":") { "%02x".format(it) }
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            return "DESFire card detected but no NDEF support. ID: $tagId" to null
        }
        ndef.connect()
        val ndefMessage = ndef.ndefMessage
        ndef.close()
        if (ndefMessage == null) {
            return "Card is empty - no data records" to null
        }
        val recordCount = ndefMessage.records.size
        val extractedData = extractStructuredDataFromRecords(ndefMessage.records)
        val result = "Found $recordCount records - Structured data extracted successfully!"
        result to extractedData
    } catch (e: Exception) {
        "Error reading card: ${e.message}" to null
    }
}

// Enhanced data extraction for multiple insurance records
private fun extractStructuredDataFromRecords(records: Array<android.nfc.NdefRecord>): ExtractedCardData {
    val personalInfo = mutableMapOf<String, String>()
    val emergencyContact = mutableMapOf<String, String>()
    val insuranceInfoList = mutableListOf<Map<String, String>>()
    val otherData = mutableMapOf<String, String>()

    records.forEachIndexed { index, record ->
        try {
            val tnf = record.tnf
            val type = String(record.type)
            val payload = record.payload

            when {
                // TEXT RECORDS
                tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && type == "T" -> {
                    val textRecord = parseTextRecord(payload)
                    when {
                        textRecord.contains("INSURANCE INFORMATION", ignoreCase = true) -> {
                            val insuranceData = mutableMapOf<String, String>()
                            parseInsuranceTextData(textRecord, insuranceData)
                            insuranceInfoList.add(insuranceData)
                        }
                        textRecord.contains("EMERGENCY", ignoreCase = true) -> {
                            parseEmergencyTextData(textRecord, emergencyContact)
                        }
                        else -> {
                            otherData["Text Record $index"] = textRecord
                        }
                    }
                }
                // URI RECORDS
                tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && type == "U" -> {
                    val uriRecord = parseUriRecord(payload)
                    otherData["Website"] = uriRecord
                }
                // VCARD RECORDS
                tnf == android.nfc.NdefRecord.TNF_MIME_MEDIA && type.contains("vcard", ignoreCase = true) -> {
                    val vCardData = String(payload, Charsets.UTF_8)
                    parseVCardData(vCardData, personalInfo)
                    parseVCardForDisplay(vCardData, otherData)
                }
                else -> {
                    val dataString = try {
                        String(payload, Charsets.UTF_8)
                    } catch (e: Exception) {
                        payload.joinToString(" ") { "%02x".format(it) }
                    }
                    otherData["Record $index (TNF:$tnf Type:$type)"] = dataString.take(100)
                }
            }
        } catch (e: Exception) {
            otherData["Record $index Error"] = e.message ?: "Unknown error"
        }
    }

    return ExtractedCardData(
        personalInfo = personalInfo,
        emergencyContact = emergencyContact,
        insuranceInfo = insuranceInfoList,
        otherData = otherData
    )
}

private fun parseInsuranceTextData(textData: String, insuranceInfo: MutableMap<String, String>) {
    val lines = textData.split("\n", "\r\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
    lines.forEach { line ->
        when {
            line.contains("Policyholder:", ignoreCase = true) -> insuranceInfo["Policyholder"] = line.substringAfter(":").trim()
            line.contains("Age:", ignoreCase = true) -> insuranceInfo["Age"] = line.substringAfter(":").trim()
            line.contains("Insurer:", ignoreCase = true) -> insuranceInfo["Insurer"] = line.substringAfter(":").trim()
            line.contains("Policy Type:", ignoreCase = true) -> insuranceInfo["Policy Type"] = line.substringAfter(":").trim()
            line.contains("Premium:", ignoreCase = true) -> insuranceInfo["Premium"] = line.substringAfter(":").trim()
            line.contains("Sum Assured:", ignoreCase = true) -> insuranceInfo["Sum Assured"] = line.substringAfter(":").trim()
            line.contains("Policy Start:", ignoreCase = true) -> insuranceInfo["Policy Start"] = line.substringAfter(":").trim()
            line.contains("Policy End:", ignoreCase = true) -> insuranceInfo["Policy End"] = line.substringAfter(":").trim()
            line.contains("Status:", ignoreCase = true) -> insuranceInfo["Status"] = line.substringAfter(":").trim()
            line.contains("Contact:", ignoreCase = true) -> insuranceInfo["Contact"] = line.substringAfter(":").trim()
            line.contains("Mobile:", ignoreCase = true) -> insuranceInfo["Mobile"] = line.substringAfter(":").trim()
            line.contains("Policy Number:", ignoreCase = true) -> insuranceInfo["Policy Number"] = line.substringAfter(":").trim()
        }
    }
}

private fun parseEmergencyTextData(textData: String, emergencyContact: MutableMap<String, String>) {
    val lines = textData.split("\n", "\r\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
    lines.forEach { line ->
        when {
            line.contains("Name:", ignoreCase = true) -> emergencyContact["Name"] = line.substringAfter(":").trim()
            line.contains("Blood Group:", ignoreCase = true) -> emergencyContact["Blood Group"] = line.substringAfter(":").trim()
            line.contains("Mobile:", ignoreCase = true) -> emergencyContact["Mobile"] = line.substringAfter(":").trim()
            line.contains("Location:", ignoreCase = true) -> emergencyContact["Location"] = line.substringAfter(":").trim()
            line.contains("Relationship:", ignoreCase = true) -> emergencyContact["Relationship"] = line.substringAfter(":").trim()
        }
    }
}

private fun parseVCardData(vCardData: String, personalInfo: MutableMap<String, String>) {
    val lines = vCardData.split("\n", "\r\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
    lines.forEach { line ->
        when {
            line.startsWith("FN:", ignoreCase = true) -> personalInfo["Full Name"] = line.substringAfter(":").trim()
            line.startsWith("N:", ignoreCase = true) -> personalInfo["Name"] = line.substringAfter(":").trim()
            line.startsWith("TEL:", ignoreCase = true) -> personalInfo["Phone"] = line.substringAfter(":").trim()
            line.startsWith("EMAIL:", ignoreCase = true) -> personalInfo["Email"] = line.substringAfter(":").trim()
            line.startsWith("ORG:", ignoreCase = true) -> personalInfo["Organization"] = line.substringAfter(":").trim()
            line.startsWith("TITLE:", ignoreCase = true) -> personalInfo["Job Title"] = line.substringAfter(":").trim()
            line.startsWith("ADR:", ignoreCase = true) -> personalInfo["Address"] = line.substringAfter(":").trim()
        }
    }
}

private fun parseVCardForDisplay(vCardData: String, otherData: MutableMap<String, String>) {
    val lines = vCardData.split("\n", "\r\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
    lines.forEach { line ->
        when {
            line.startsWith("VERSION:", ignoreCase = true) -> otherData["VCard Version"] = line.substringAfter(":").trim()
            line.startsWith("FN:", ignoreCase = true) -> otherData["Full Name"] = line.substringAfter(":").trim()
            line.startsWith("ORG:", ignoreCase = true) -> otherData["Organization"] = line.substringAfter(":").trim()
            line.startsWith("TEL:", ignoreCase = true) -> otherData["Phone Number"] = line.substringAfter(":").trim()
            line.startsWith("EMAIL:", ignoreCase = true) -> otherData["Email Address"] = line.substringAfter(":").trim()
            line.startsWith("URL:", ignoreCase = true) -> otherData["Website"] = line.substringAfter(":").trim()
            line.startsWith("ADR:", ignoreCase = true) -> otherData["Address"] = line.substringAfter(":").trim()
            line.startsWith("TITLE:", ignoreCase = true) -> otherData["Job Title"] = line.substringAfter(":").trim()
            line.startsWith("NOTE:", ignoreCase = true) -> otherData["Notes"] = line.substringAfter(":").trim()
        }
    }
}

private fun parseTextRecord(payload: ByteArray): String {
    return try {
        val languageCodeLength = payload[0].toInt() and 0x3F
        String(payload, 1 + languageCodeLength, payload.size - 1 - languageCodeLength, Charsets.UTF_8)
    } catch (e: Exception) {
        "Text parse error: ${e.message}"
    }
}

private fun parseUriRecord(payload: ByteArray): String {
    return try {
        val uriPrefixes = arrayOf(
            "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
            "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://",
            "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://",
            "urn:", "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://",
            "btgoep://", "tcpobex://", "irdaobex://", "file://", "urn:epc:id:",
            "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
        )

        val prefixIndex = payload[0].toInt() and 0xFF
        val prefix = if (prefixIndex < uriPrefixes.size) uriPrefixes[prefixIndex] else ""
        val uriSuffix = String(payload, 1, payload.size - 1, Charsets.UTF_8)

        "$prefix$uriSuffix"
    } catch (e: Exception) {
        "URI parse error: ${e.message}"
    }
}

private fun hasValidStructuredData(
    fullName: String, phone: String, email: String,
    emergencyName: String, emergencyPhone: String,
    insuranceList: MutableList<InsuranceInfo>
): Boolean {
    val hasPersonal = fullName.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())
    val hasEmergency = emergencyName.isNotBlank() && emergencyPhone.isNotBlank()
    val hasInsurance = insuranceList.any { it.policyHolder.isNotBlank() }

    return hasPersonal || hasEmergency || hasInsurance
}

// Helper function to compare card vs backend data (outside composable)
private fun compareCardAndBackendData(cardPolicies: List<Map<String, String>>, backendPolicies: List<InsurancePolicy>): SyncComparisonData {
    val differences = mutableListOf<String>()

    // Check for policies in backend but not in card
    backendPolicies.forEach { backendPolicy ->
        val found = cardPolicies.any { cardPolicy ->
            cardPolicy["Policy Number"] == backendPolicy.policyNumber
        }
        if (!found) {
            differences.add("Policy ${backendPolicy.policyNumber} exists in backend but not in card")
        }
    }

    // Check for policies in card but not in backend
    cardPolicies.forEach { cardPolicy ->
        val policyNumber = cardPolicy["Policy Number"]
        val found = backendPolicies.any { it.policyNumber == policyNumber }
        if (!found && !policyNumber.isNullOrEmpty()) {
            differences.add("Policy $policyNumber exists in card but not in backend")
        }
    }

    // Check for data differences in matching policies
    cardPolicies.forEach { cardPolicy ->
        val cardPolicyNumber = cardPolicy["Policy Number"]
        backendPolicies.find { it.policyNumber == cardPolicyNumber }?.let { backendPolicy ->
            if (cardPolicy["Status"] != backendPolicy.status) {
                differences.add("Policy $cardPolicyNumber: Status differs (Card: ${cardPolicy["Status"]}, Backend: ${backendPolicy.status})")
            }
            if (cardPolicy["Insurer"] != backendPolicy.insurerName) {
                differences.add("Policy $cardPolicyNumber: Insurer differs")
            }
        }
    }

    return SyncComparisonData(
        cardPolicies = cardPolicies,
        backendPolicies = backendPolicies,
        differences = differences,
        needsSync = differences.isNotEmpty()
    )
}