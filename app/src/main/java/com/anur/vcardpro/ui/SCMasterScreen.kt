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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.NdefMessage
import android.nfc.tech.Ndef
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import java.text.SimpleDateFormat
import java.util.*
import com.anur.vcardpro.MainActivity
import com.anur.vcardpro.ui.theme.*

// Simplified data classes - VCard only
data class ExtractedCardData(
    val vCardUrl: String = "",
    val personalInfo: Map<String, String> = emptyMap(),
    val emergencyContact: Map<String, String> = emptyMap(),
    val otherData: Map<String, String> = emptyMap()
)

@Composable
fun SCMasterScreen(activity: MainActivity, onBack: () -> Unit) {
    var nfcStatus by remember { mutableStateOf("Initializing NFC...") }
    var cardDetected by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf("") }
    var cardDataStatus by remember { mutableStateOf("") }
    var extractedData by remember { mutableStateOf<ExtractedCardData?>(null) }

    val context = LocalContext.current
    val nfcTag by activity.nfcTag

    // Handle NFC tag detection
    LaunchedEffect(nfcTag) {
        nfcTag?.let { tag ->
            val result = handleCardDetection(tag)
            cardDataStatus = result.first
            extractedData = result.second
            cardDetected = true
            lastTapTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
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
            nfcStatus = "Ready to read card - Tap your NFC card"
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
                    tint = Color.White
                )
            }
        }

        // Title
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📇 VCard Reader",
                    fontSize = 24.sp,
                    color = PrimaryPurple,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = nfcStatus,
                    fontSize = 14.sp,
                    color = Color.Black
                )

                if (cardDetected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✅ Card detected at: $lastTapTime",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
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

        // Display card data
        extractedData?.let { data ->
            // VCard URL
            if (data.vCardUrl.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔗 VCard Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = data.vCardUrl,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Personal Information
            if (data.personalInfo.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GrayDark)

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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$key:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.weight(0.4f)
                                )
                                Text(
                                    text = value,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Emergency Contact
            if (data.emergencyContact.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ErrorRed)

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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$key:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.weight(0.4f)
                                )
                                Text(
                                    text = value,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Insurance Info Section - formatted display
            if (data.otherData.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(DeepPurple, DeepPurple) // Purple → Blue gradient
                            )
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📋 Insurance Policies",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Format each policy entry cleanly
                        data.otherData.entries.forEachIndexed { index, entry ->
                            val policyNumber = String.format("%02d", index + 1)
                            val lines = entry.value.split("\n", "\r\n", "\r")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }

                            // Card for each individual policy
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Insurance Policy $policyNumber",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Each line like "Policy Holder: NATARAJAN"
                                    lines.forEach { line ->
                                        if (":" in line) {
                                            val (label, value) = line.split(":", limit = 2)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${label.trim()}:",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextWhite.copy(alpha = 0.9f),
                                                    modifier = Modifier.weight(0.4f)
                                                )
                                                Text(
                                                    text = value.trim(),
                                                    fontSize = 14.sp,
                                                    color = TextWhite,
                                                    modifier = Modifier.weight(0.6f)
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = line,
                                                fontSize = 14.sp,
                                                color = TextWhite,
                                                modifier = Modifier.padding(vertical = 2.dp)
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

        // Instructions when no card detected
        if (!cardDetected) {
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📱",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap Your NFC Card",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple

                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hold your card near the back of your phone to read VCard information",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

// Card detection handler
private fun handleCardDetection(tag: Tag): Pair<String, ExtractedCardData?> {
    return try {
        val tagId = tag.id.joinToString(":") { "%02x".format(it) }
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            return "Card detected but no NDEF support. ID: $tagId" to null
        }
        ndef.connect()
        val ndefMessage = ndef.ndefMessage
        ndef.close()
        if (ndefMessage == null) {
            return "Card is empty - no data records" to null
        }
        val recordCount = ndefMessage.records.size
        val extractedData = extractDataFromRecords(ndefMessage.records)
        val result = "✅ Found $recordCount records - Data extracted successfully!"
        result to extractedData
    } catch (e: Exception) {
        "❌ Error reading card: ${e.message}" to null
    }
}

private fun extractDataFromRecords(records: Array<android.nfc.NdefRecord>): ExtractedCardData {
    var vCardUrl = ""
    val personalInfo = mutableMapOf<String, String>()
    val emergencyContact = mutableMapOf<String, String>()
    val otherData = mutableMapOf<String, String>()

    var textRecordCounter = 0 // 👈 Track only TEXT record numbers

    records.forEachIndexed { index, record ->
        try {
            val tnf = record.tnf
            val type = String(record.type)
            val payload = record.payload

            when {
                // URI RECORDS (VCard URL)
                tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && type == "U" -> {
                    vCardUrl = parseUriRecord(payload)
                }

                // VCARD RECORDS
                tnf == android.nfc.NdefRecord.TNF_MIME_MEDIA && type.contains("vcard", ignoreCase = true) -> {
                    val vCardData = String(payload, Charsets.UTF_8)
                    parseVCardData(vCardData, personalInfo)
                }

                // TEXT RECORDS
                tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN && type == "T" -> {
                    val textRecord = parseTextRecord(payload)

                    when {
                        textRecord.contains("EMERGENCY", ignoreCase = true) -> {
                            parseEmergencyTextData(textRecord, emergencyContact)
                        }
                        else -> {
                            textRecordCounter++ // 👈 Increment only for non-emergency TEXT records
                            otherData["Policy $textRecordCounter"] = textRecord.take(200)
                        }
                    }
                }


                // Other record types (optional)
                else -> {
                    val dataString = try {
                        String(payload, Charsets.UTF_8)
                    } catch (e: Exception) {
                        payload.joinToString(" ") { "%02x".format(it) }
                    }
                    if (dataString.length < 200) {
                        otherData["Record ${index + 1}"] = dataString
                    }
                }
            }
        } catch (e: Exception) {
            otherData["Record ${index + 1} Error"] = e.message ?: "Unknown error"
        }
    }

    return ExtractedCardData(
        vCardUrl = vCardUrl,
        personalInfo = personalInfo,
        emergencyContact = emergencyContact,
        otherData = otherData
    )
}

// Parse VCard data
private fun parseVCardData(vCardData: String, personalInfo: MutableMap<String, String>) {
    val lines = vCardData.split("\n", "\r\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
    lines.forEach { line ->
        when {
            line.startsWith("FN:", ignoreCase = true) ->
                personalInfo["Full Name"] = line.substringAfter(":").trim()
            line.startsWith("TEL:", ignoreCase = true) ->
                personalInfo["Phone"] = line.substringAfter(":").trim()
            line.startsWith("EMAIL:", ignoreCase = true) ->
                personalInfo["Email"] = line.substringAfter(":").trim()
            line.startsWith("ORG:", ignoreCase = true) ->
                personalInfo["Organization"] = line.substringAfter(":").trim()
            line.startsWith("TITLE:", ignoreCase = true) ->
                personalInfo["Job Title"] = line.substringAfter(":").trim()
            line.startsWith("ADR:", ignoreCase = true) ->
                personalInfo["Address"] = line.substringAfter(":").trim()
        }
    }
}

// Parse emergency contact text data
private fun parseEmergencyTextData(textData: String, emergencyContact: MutableMap<String, String>) {
    val lines = textData.split("\n", "\r\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
    lines.forEach { line ->
        when {
            line.contains("Name:", ignoreCase = true) ->
                emergencyContact["Name"] = line.substringAfter(":").trim()
            line.contains("Blood Group:", ignoreCase = true) ->
                emergencyContact["Blood Group"] = line.substringAfter(":").trim()
            line.contains("Mobile:", ignoreCase = true) ->
                emergencyContact["Mobile"] = line.substringAfter(":").trim()
            line.contains("Location:", ignoreCase = true) ->
                emergencyContact["Location"] = line.substringAfter(":").trim()
            line.contains("Relationship:", ignoreCase = true) ->
                emergencyContact["Relationship"] = line.substringAfter(":").trim()
        }
    }
}

// Parse text record
private fun parseTextRecord(payload: ByteArray): String {
    return try {
        val languageCodeLength = payload[0].toInt() and 0x3F
        String(payload, 1 + languageCodeLength, payload.size - 1 - languageCodeLength, Charsets.UTF_8)
    } catch (e: Exception) {
        "Text parse error: ${e.message}"
    }
}

// Parse URI record
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