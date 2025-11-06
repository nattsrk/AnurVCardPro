package com.anur.vcardpro.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anur.vcardpro.UserSession
import com.anur.vcardpro.model.InsuranceResponse
import com.anur.vcardpro.model.InsuranceClaim
import com.anur.vcardpro.model.ClaimsResponse
import com.anur.vcardpro.model.TimelineStep
import com.anur.vcardpro.network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import android.widget.Toast
import android.util.Log
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceManagerScreen(onBack: () -> Unit) {
    var insuranceData by remember { mutableStateOf<InsuranceResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loadInsuranceData { response: InsuranceResponse?, error: String? ->
            insuranceData = response
            errorMessage = error
            isLoading = false
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF616161))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Insurance Manager",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "User ID: ${UserSession.userId}",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading insurance data...")
                    }
                }
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error Loading Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }

            insuranceData != null -> {
                insuranceData?.policies?.forEach { policy ->
                    EnhancedPolicyCard(policy = policy)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (insuranceData?.policies?.isEmpty() == true) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Insurance Policies",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No policies found for this user")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedPolicyCard(policy: com.anur.vcardpro.model.InsurancePolicy) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Policy Details", "Claim Status")
    val context = LocalContext.current

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Insurance Policy",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = policy.policyNumber,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Download PDF Icon
                    IconButton(
                        onClick = {
                            generateAndDownloadPolicyPDF(context, policy)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text(
                            text = "💾",
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF8B0000)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title, fontSize = 14.sp) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            when (selectedTab) {
                0 -> PolicyDetailsTab(policy = policy)
                1 -> ClaimStatusTab(policyId = policy.id)
            }
        }
    }
}

@Composable
fun PolicyDetailsTab(policy: com.anur.vcardpro.model.InsurancePolicy) {
    Column(modifier = Modifier.padding(16.dp)) {
        InfoRow("Policyholder:", "Natarajan R", Color.White, FontWeight.Bold)
        InfoRow("Policy Number:", policy.policyNumber, Color.White, FontWeight.Bold)
        InfoRow("Policy Type:", policy.policyType, Color.White, FontWeight.Bold)
        InfoRow("Insurer:", policy.insurerName, Color.White, FontWeight.Bold)
        InfoRow("Premium:", "${formatCurrency(policy.premiumAmount)} (Annual)", Color.White, FontWeight.Bold)
        InfoRow("Sum Assured:", "${formatCurrency(policy.sumAssured)}", Color.White, FontWeight.Bold)
        InfoRow("Status:", policy.status, getStatusColor(policy.status), FontWeight.Bold)

        policy.claims?.let { claims ->
            InfoRow("Claims:", "${claims.size} ${if (claims.size == 1) "claim" else "claims"}", Color.White, FontWeight.Bold)
        }
        policy.payments?.let { payments ->
            InfoRow("Payments:", "${payments.size} ${if (payments.size == 1) "payment" else "payments"}", Color.White, FontWeight.Bold)
        }
    }
}

@Composable
fun ClaimStatusTab(policyId: Int) {
    var claims by remember { mutableStateOf<List<InsuranceClaim>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showTimeline by remember { mutableStateOf<InsuranceClaim?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Function to refresh claims data
    fun refreshClaims() {
        isLoading = true
        loadClaimsForPolicy(policyId) { claimsData: List<InsuranceClaim>?, error: String? ->
            claims = claimsData
            errorMessage = error
            isLoading = false
        }
    }

    LaunchedEffect(policyId) {
        refreshClaims()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        when {
            isLoading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading claims...")
                    }
                }
            }

            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error Loading Claims",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = errorMessage ?: "Unknown error", fontSize = 12.sp, color = Color(0xFF666666))
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { refreshClaims() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }

            claims.isNullOrEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Claims Filed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This policy has no active or historical claims",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Text("File New Claim")
                        }
                    }
                }
            }

            else -> {
                // Add "File New Claim" button at the top when there are existing claims
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Claims (${claims?.size ?: 0})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        ),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("+ New Claim", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                claims?.forEach { claim ->
                    ClaimCard(claim = claim)

                    if (showTimeline?.id == claim.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ClaimStatusTimeline(claim = claim)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            showTimeline = if (showTimeline?.id == claim.id) null else claim
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text(
                            if (showTimeline?.id == claim.id) "Hide Progress" else "Show Progress"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // Show claim creation dialog
    if (showCreateDialog) {
        ClaimCreationDialog(
            policyId = policyId,
            onDismiss = { showCreateDialog = false },
            onClaimCreated = {
                refreshClaims() // Refresh the claims list after successful creation
            }
        )
    }
}

@Composable
private fun ClaimCard(claim: InsuranceClaim) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = claim.claimNumber ?: "Unknown Claim",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Filed: ${formatDate(claim.submittedDate ?: "")}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                StatusBadge(status = claim.status ?: "Unknown")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Amount: ${formatCurrency(claim.amount ?: 0.0)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )

            claim.description?.let { description ->
                if (description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val backgroundColor = Color(getClaimStatusColor(status))

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = backgroundColor
        )
    }
}

@Composable
private fun ClaimStatusTimeline(claim: InsuranceClaim) {
    val timeline = createClaimTimeline(claim)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Progress Timeline",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            timeline.forEachIndexed { index, step ->
                TimelineItem(
                    step = step,
                    isLast = index == timeline.lastIndex
                )

                if (index < timeline.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(step: TimelineStep, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            step.isCompleted -> Color(0xFF4CAF50)
                            step.isActive -> Color(0xFFFF9800)
                            else -> Color(0xFFE0E0E0)
                        }
                    )
            )

            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .width(2.dp)
                        .height(40.dp)
                ) {
                    drawLine(
                        color = Color(0xFFE0E0E0),
                        start = Offset(size.width / 2, 0f),
                        end = Offset(size.width / 2, size.height),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.status,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (step.isActive) Color(0xFFFF9800) else Color.Black
            )

            Text(
                text = step.description,
                fontSize = 12.sp,
                color = Color.Gray
            )

            step.date?.let { date ->
                Text(
                    text = formatDate(date),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimCreationDialog(
    policyId: Int,
    onDismiss: () -> Unit,
    onClaimCreated: () -> Unit
) {
    var claimAmount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedClaimType by remember { mutableStateOf("Medical") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val claimTypes = listOf("Medical", "Accident", "Surgery", "Dental", "Emergency", "Other")
    val context = LocalContext.current

    // Simple submit function
    fun submitClaim() {
        Log.e("CLAIM_DEBUG", "submitClaim function called")

        if (claimAmount.isEmpty() || description.isEmpty()) {
            errorMessage = "Please fill all fields"
            Log.e("CLAIM_DEBUG", "Validation failed: empty fields")
            return
        }

        val amount = claimAmount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            errorMessage = "Please enter valid amount"
            Log.e("CLAIM_DEBUG", "Validation failed: invalid amount")
            return
        }

        Log.e("CLAIM_DEBUG", "Starting API call with policyId=$policyId, amount=$amount")
        isSubmitting = true
        errorMessage = ""

        // Create the API call
        val retrofit = Retrofit.Builder()
            .baseUrl("https://vcard.tecgs.com:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)
        val requestBody = mapOf(
            "claimType" to selectedClaimType,
            "claimAmount" to amount,
            "description" to description,
            "claimDate" to getCurrentDate(),
            "claimStatus" to "Pending"
        )

        Log.e("CLAIM_DEBUG", "Request body: $requestBody")

        api.createClaim(policyId, requestBody).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                Log.e("CLAIM_DEBUG", "API Response received - Code: ${response.code()}")
                isSubmitting = false

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.e("CLAIM_DEBUG", "Success response: $body")
                    Toast.makeText(context, "Claim submitted successfully!", Toast.LENGTH_SHORT).show()
                    onClaimCreated()
                    onDismiss()
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("CLAIM_DEBUG", "API Error: $error")
                    errorMessage = "Failed to submit claim: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("CLAIM_DEBUG", "API Failure: ${t.message}")
                isSubmitting = false
                errorMessage = "Network error: ${t.message}"
            }
        })
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "File New Claim",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Policy ID Display
                Text(
                    text = "Policy ID: $policyId",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                // Error message
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                // Claim Type Dropdown
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedClaimType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Claim Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        claimTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedClaimType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Claim Amount Input
                OutlinedTextField(
                    value = claimAmount,
                    onValueChange = {
                        claimAmount = it
                        errorMessage = "" // Clear error when user types
                    },
                    label = { Text("Claim Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = claimAmount.isNotEmpty() && claimAmount.toDoubleOrNull() == null
                )

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        errorMessage = "" // Clear error when user types
                    },
                    label = { Text("Description") },
                    placeholder = { Text("Describe your claim...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    android.util.Log.e("CLAIM_DEBUG", "Button clicked!")
                    if (claimAmount.isNotEmpty() && description.isNotEmpty()) {
                        val amount = claimAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            isSubmitting = true

                            createNewClaim(
                                policyId = policyId,
                                claimType = selectedClaimType,  // Make sure this variable exists in your dialog
                                amount = amount,
                                description = description,
                                onResult = { success, message ->
                                    Log.e("CLAIM_DEBUG", "API Result: $success, $message")
                                    isSubmitting = false
                                    if (success) {
                                        Toast.makeText(context, "Claim submitted successfully!", Toast.LENGTH_SHORT).show()
                                        onClaimCreated()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Error: $message", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                },
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Submit Claim")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isSubmitting) onDismiss() }
            ) {
                Text("Cancel")
            }
        }
    )
}

// Helper function for date
private fun getCurrentDate(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = Color.Black, valueWeight: FontWeight = FontWeight.Normal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = valueWeight,
            modifier = Modifier.weight(0.6f)
        )
    }
}

// HELPER FUNCTIONS
private fun createClaimTimeline(claim: InsuranceClaim): List<TimelineStep> {
    val allSteps = listOf("Pending", "Approved", "Paid")
    val currentStatus = claim.status
    val currentIndex = allSteps.indexOf(currentStatus)

    return allSteps.mapIndexed { index, status ->
        TimelineStep(
            status = status,
            date = if (index <= currentIndex) claim.submittedDate else null,
            description = getStatusDescription(status),
            isCompleted = index < currentIndex,
            isActive = index == currentIndex
        )
    }
}

private fun getStatusDescription(status: String): String {
    return when (status) {
        "Pending" -> "Claim submitted and under review"
        "Approved" -> "Claim approved by insurance team"
        "Rejected" -> "Claim has been rejected"
        "Paid" -> "Settlement amount has been paid"
        "Cancelled" -> "Claim was cancelled"
        else -> "Status update"
    }
}

private fun getClaimStatusColor(status: String): Long {
    return when (status.lowercase()) {
        "pending" -> 0xFFFF9800
        "approved" -> 0xFF4CAF50
        "rejected" -> 0xFFF44336
        "paid" -> 0xFF2196F3
        "cancelled" -> 0xFF757575
        else -> 0xFF757575
    }
}

private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "active" -> Color(0xFF4CAF50)
        "inactive", "expired", "cancelled" -> Color(0xFFFF5722)
        else -> Color.Black
    }
}

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(amount)
}

private fun formatDate(dateString: String): String {
    return try {
        dateString.substringBefore('T').replace('-', '/')
    } catch (e: Exception) {
        dateString
    }
}

fun loadInsuranceData(onResult: (InsuranceResponse?, String?) -> Unit) {
    val userId = UserSession.userId

    if (userId == -1) {
        onResult(null, "User not logged in")
        return
    }

    val retrofit = Retrofit.Builder()
        .baseUrl("https://vcard.tecgs.com:3000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(ApiService::class.java)
    api.getUserInsurance(userId).enqueue(object : Callback<InsuranceResponse> {
        override fun onResponse(call: Call<InsuranceResponse>, response: Response<InsuranceResponse>) {
            if (response.isSuccessful && response.body()?.success == true) {
                onResult(response.body(), null)
            } else {
                onResult(null, "Failed to load insurance data: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<InsuranceResponse>, t: Throwable) {
            onResult(null, "Network error: ${t.message}")
        }
    })
}

fun loadClaimsForPolicy(policyId: Int, onResult: (List<InsuranceClaim>?, String?) -> Unit) {
    println("DEBUG: Loading claims for policy ID: $policyId")

    val retrofit = Retrofit.Builder()
        .baseUrl("https://vcard.tecgs.com:3000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(ApiService::class.java)
    api.getPolicyClaims(policyId).enqueue(object : Callback<ClaimsResponse> {
        override fun onResponse(call: Call<ClaimsResponse>, response: Response<ClaimsResponse>) {
            println("DEBUG: API Response Code: ${response.code()}")

            if (response.isSuccessful && response.body()?.success == true) {
                onResult(response.body()!!.claims, null)
            } else {
                onResult(null, "Failed to load claims: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<ClaimsResponse>, t: Throwable) {
            println("DEBUG: API call failed with exception: ${t.message}")
            onResult(null, "Network error: ${t.message}")
        }
    })
}
fun createNewClaim(
    policyId: Int,
    claimType: String,
    amount: Double,
    description: String,
    onResult: (Boolean, String?) -> Unit
) {
    Log.e("CLAIM_DEBUG", "createNewClaim called with policyId=$policyId, type=$claimType, amount=$amount")

    try {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://vcard.tecgs.com:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(ApiService::class.java)

        val requestBody = mapOf(
            "claimType" to claimType,
            "claimAmount" to amount,
            "description" to description,
            "claimDate" to getCurrentDate(),
            "claimStatus" to "Pending"
        )

        Log.e("CLAIM_DEBUG", "Request body: $requestBody")

        api.createClaim(policyId, requestBody).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                Log.e("CLAIM_DEBUG", "API Response received - Code: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.e("CLAIM_DEBUG", "Success response: $body")
                    onResult(true, "Claim submitted successfully!")
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("CLAIM_DEBUG", "API Error: $error")
                    onResult(false, "Failed to submit claim: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Log.e("CLAIM_DEBUG", "API Failure: ${t.message}")
                onResult(false, "Network error: ${t.message}")
            }
        })

    } catch (e: Exception) {
        Log.e("CLAIM_DEBUG", "Exception in createNewClaim setup: ${e.message}")
        onResult(false, "Setup error: ${e.localizedMessage}")
    }
}

// PDF Generation and Download Functions
fun generateAndDownloadPolicyPDF(context: android.content.Context, policy: com.anur.vcardpro.model.InsurancePolicy) {
    try {
        // Create PDF document
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = android.graphics.Paint()
        val titlePaint = android.graphics.Paint()

        // Set up paints
        titlePaint.textSize = 24f
        titlePaint.isFakeBoldText = true
        paint.textSize = 14f

        var yPosition = 80f

        // Title
        canvas.drawText("INSURANCE POLICY DETAILS", 50f, yPosition, titlePaint)
        yPosition += 60f

        // Policy Information
        canvas.drawText("Policy Number: ${policy.policyNumber}", 50f, yPosition, paint)
        yPosition += 30f

        canvas.drawText("Policy Type: ${policy.policyType}", 50f, yPosition, paint)
        yPosition += 30f

        canvas.drawText("Insurer: ${policy.insurerName}", 50f, yPosition, paint)
        yPosition += 30f

        canvas.drawText("Premium: ${formatCurrency(policy.premiumAmount)} (Annual)", 50f, yPosition, paint)
        yPosition += 30f

        canvas.drawText("Sum Assured: ${formatCurrency(policy.sumAssured)}", 50f, yPosition, paint)
        yPosition += 30f

        canvas.drawText("Status: ${policy.status}", 50f, yPosition, paint)
        yPosition += 30f

        canvas.drawText("Policy Start Date: ${policy.policyStartDate}", 50f, yPosition, paint)
        yPosition += 30f

        canvas.drawText("Policy End Date: ${policy.policyEndDate}", 50f, yPosition, paint)
        yPosition += 60f

        // Claims Section
        val claimsTitlePaint = android.graphics.Paint()
        claimsTitlePaint.textSize = 18f
        claimsTitlePaint.isFakeBoldText = true

        canvas.drawText("CLAIMS HISTORY", 50f, yPosition, claimsTitlePaint)
        yPosition += 40f

        policy.claims?.let { claims ->
            if (claims.isNotEmpty()) {
                claims.forEachIndexed { index, claim ->
                    canvas.drawText("Claim ${index + 1}:", 50f, yPosition, paint)
                    yPosition += 25f

                    canvas.drawText("  Claim Number: ${claim.claimNumber ?: "N/A"}", 70f, yPosition, paint)
                    yPosition += 20f

                    canvas.drawText("  Amount: ${formatCurrency(claim.amount ?: 0.0)}", 70f, yPosition, paint)
                    yPosition += 20f

                    canvas.drawText("  Status: ${claim.status ?: "Unknown"}", 70f, yPosition, paint)
                    yPosition += 20f

                    canvas.drawText("  Submitted: ${formatDate(claim.submittedDate ?: "")}", 70f, yPosition, paint)
                    yPosition += 20f

                    claim.description?.let { desc ->
                        if (desc.isNotEmpty()) {
                            canvas.drawText("  Description: $desc", 70f, yPosition, paint)
                            yPosition += 20f
                        }
                    }

                    yPosition += 20f // Extra space between claims
                }
            } else {
                canvas.drawText("No claims filed for this policy", 50f, yPosition, paint)
                yPosition += 30f
            }
        } ?: run {
            canvas.drawText("No claims information available", 50f, yPosition, paint)
            yPosition += 30f
        }

        // Footer
        val footerPaint = android.graphics.Paint()
        footerPaint.textSize = 10f
        footerPaint.color = android.graphics.Color.GRAY

        canvas.drawText("Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}", 50f, 800f, footerPaint)
        canvas.drawText("User ID: ${UserSession.userId}", 50f, 820f, footerPaint)

        pdfDocument.finishPage(page)

        // Save PDF to file
        val fileName = "Policy_${policy.policyNumber}_${System.currentTimeMillis()}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        // Show download notification
        Toast.makeText(context, "PDF downloaded to Downloads folder", Toast.LENGTH_LONG).show()

        // Open the PDF
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PDF", "Could not open PDF viewer: ${e.message}")
            Toast.makeText(context, "PDF saved but could not open viewer", Toast.LENGTH_SHORT).show()
        }

    } catch (e: Exception) {
        Log.e("PDF", "Error generating PDF: ${e.message}", e)
        Toast.makeText(context, "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

