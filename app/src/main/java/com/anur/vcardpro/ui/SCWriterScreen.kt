package com.anur.vcardpro.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import android.widget.Toast
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import com.anur.vcardpro.MainActivity
import java.nio.charset.Charset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SCWriterScreen(activity: MainActivity, onBack: () -> Unit) {
    // Simple data states
    var fullName by remember { mutableStateOf("Test Name") }
    var phone by remember { mutableStateOf("1234567890") }
    var email by remember { mutableStateOf("test@example.com") }
    var organization by remember { mutableStateOf("Test Org") }

    var emergencyName by remember { mutableStateOf("Emergency Contact") }
    var emergencyPhone by remember { mutableStateOf("0987654321") }
    var bloodGroup by remember { mutableStateOf("O+") }

    var policyHolder by remember { mutableStateOf("Policy Holder") }
    var insurerName by remember { mutableStateOf("Test Insurance") }
    var policyNumber by remember { mutableStateOf("POL123456") }

    var writeStatus by remember { mutableStateOf("Ready to write") }
    var isWriting by remember { mutableStateOf(false) }
    var cardDetected by remember { mutableStateOf(false) }
    var currentTag by remember { mutableStateOf<Tag?>(null) }

    val context = LocalContext.current

    // Watch for NFC tag changes directly
    val nfcTag by activity.nfcTag

    LaunchedEffect(nfcTag) {
        if (isWriting && nfcTag != null) {
            cardDetected = true
            currentTag = nfcTag
            writeStatus = "Card detected! Writing data..."

            // Write directly to the tag
            val result = writeDirectlyToCard(
                nfcTag!!, fullName, phone, email, organization,
                emergencyName, emergencyPhone, bloodGroup,
                policyHolder, insurerName, policyNumber
            )

            writeStatus = result
            isWriting = false

            if (result.contains("success", ignoreCase = true)) {
                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
            }

            // Clear the tag after processing
            activity.clearNfcTag()

            // Reset after delay
            kotlinx.coroutines.delay(3000)
            cardDetected = false
            currentTag = null
            writeStatus = "Ready to write again"
        }
    }

    // Set write mode when entering this screen
    LaunchedEffect(Unit) {
        activity.setWriteMode { /* callback not needed, using LaunchedEffect above */ }
    }

    // Clear mode when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            activity.clearNfcMode()
        }
    }

    // Purple to pink gradient background matching dashboard
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
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                activity.clearNfcMode()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black)
            }
            Text("SC Writer", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status Card - Changes color based on state
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    cardDetected -> Color(0xFF8B5CF6) // Green when detected
                    isWriting -> Color(0xFFFF9800) // Orange when writing
                    else -> Color(0xFF8B5CF6) // Blue when ready
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = writeStatus,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                if (cardDetected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Card Present - Writing in Progress!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                if (isWriting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Info Section - RED background as requested
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Personal Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
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

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = { Text("Organization", color = Color.White) },
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

        // Emergency Contact Section - RED background
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Emergency Contact", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emergencyName,
                    onValueChange = { emergencyName = it },
                    label = { Text("Emergency Contact Name", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    label = { Text("Emergency Phone", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bloodGroup,
                    onValueChange = { bloodGroup = it },
                    label = { Text("Blood Group", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("A+, B-, O+, etc.", color = Color.White.copy(alpha = 0.7f)) },
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

        // Insurance Section - RED background
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Insurance Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = policyHolder,
                    onValueChange = { policyHolder = it },
                    label = { Text("Policyholder Name", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = insurerName,
                    onValueChange = { insurerName = it },
                    label = { Text("Insurer Name", color = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.7f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = policyNumber,
                    onValueChange = { policyNumber = it },
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

        Spacer(modifier = Modifier.height(24.dp))

        // Write Button
        Button(
            onClick = {
                if (hasValidData(fullName, phone, email, emergencyName, emergencyPhone, policyHolder)) {
                    isWriting = true
                    writeStatus = "Tap your card to write data..."
                    cardDetected = false
                    Toast.makeText(context, "Please tap your NFC card now", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Please fill at least one complete section", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isWriting,
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    cardDetected -> Color(0xFF8B5CF6) // Green when card detected
                    else -> Color(0xFFD32F2F) // Red otherwise
                }
            )
        ) {
            if (isWriting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Waiting for Card...")
            } else {
                Text("Write to Smart Card", fontSize = 16.sp)
            }
        }
    }
}

// Direct write function that actually works
private fun writeDirectlyToCard(
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
                return "Data too large for card (${ndefMessage.toByteArray().size} bytes > ${ndef.maxSize} bytes)"
            }

            ndef.writeNdefMessage(ndefMessage)
            ndef.close()
            "Successfully written ${records.size} records to card!"
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

// Simple validation
private fun hasValidData(
    fullName: String, phone: String, email: String,
    emergencyName: String, emergencyPhone: String,
    policyHolder: String
): Boolean {
    val hasPersonal = fullName.isNotBlank() && (phone.isNotBlank() || email.isNotBlank())
    val hasEmergency = emergencyName.isNotBlank() && emergencyPhone.isNotBlank()
    val hasInsurance = policyHolder.isNotBlank()

    return hasPersonal || hasEmergency || hasInsurance
}