package com.anur.vcardpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.window.Dialog
import android.widget.Toast
import com.anur.vcardpro.UserSession
import com.anur.vcardpro.model.PolicyProduct
import com.anur.vcardpro.model.PolicyProductsResponse
import com.anur.vcardpro.model.PolicyApplicationRequest
import com.anur.vcardpro.model.ApplicationResponse
import com.anur.vcardpro.network.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.util.Locale
import com.anur.vcardpro.ui.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyNewPolicyScreen(onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Life Insurance", "Health Insurance")

    var lifeProducts by remember { mutableStateOf<List<PolicyProduct>>(emptyList()) }
    var healthProducts by remember { mutableStateOf<List<PolicyProduct>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedProduct by remember { mutableStateOf<PolicyProduct?>(null) }
    var showCustomizeDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Load products on launch
    LaunchedEffect(Unit) {
        loadPolicyProducts { life, health, error ->
            lifeProducts = life
            healthProducts = health
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
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.Black)
            }
            Text(
                "Buy New Policy",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF4CAF50)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        // Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: $errorMessage", color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            isLoading = true
                            loadPolicyProducts { life, health, error ->
                                lifeProducts = life
                                healthProducts = health
                                errorMessage = error
                                isLoading = false
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                val products = if (selectedTab == 0) lifeProducts else healthProducts

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (products.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No products available", color = Color.Gray)
                        }
                    } else {
                        products.forEach { product ->
                            PolicyProductCard(
                                product = product,
                                onClick = {
                                    selectedProduct = product
                                    showCustomizeDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }

    // Customize Dialog
    if (showCustomizeDialog && selectedProduct != null) {
        CustomizePolicyDialog(
            product = selectedProduct!!,
            onDismiss = { showCustomizeDialog = false },
            onApply = { coverage, tenure, premium ->
                submitApplication(
                    context,
                    selectedProduct!!,
                    coverage,
                    tenure,
                    premium
                ) { success, message ->
                    if (success) {
                        Toast.makeText(context, "Policy purchased successfully!", Toast.LENGTH_LONG).show()
                        showCustomizeDialog = false
                    } else {
                        Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun PolicyProductCard(product: PolicyProduct, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    product.productName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748)
                )
                Text(
                    product.productType,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = if (product.productType == "Life") Color(0xFF8B5CF6) else Color(0xFF2196F3),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                product.insurerName,
                fontSize = 14.sp,
                color = Color(0xFF4A5568),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                product.description,
                fontSize = 13.sp,
                color = Color(0xFF718096),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            product.features.take(3).forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        feature,
                        fontSize = 12.sp,
                        color = Color(0xFF4A5568)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pricing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Starting from", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        formatCurrency(product.basePremium),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text("/year", fontSize = 11.sp, color = Color.Gray)
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("Customize & Apply")
                }
            }
        }
    }
}

@Composable
fun CustomizePolicyDialog(
    product: PolicyProduct,
    onDismiss: () -> Unit,
    onApply: (Double, Int, Double) -> Unit
) {
    var selectedCoverage by remember { mutableStateOf(product.baseCoverage) }
    var selectedTenure by remember { mutableStateOf(product.tenureOptions.firstOrNull() ?: 10) }

    val calculatedPremium = remember(selectedCoverage, selectedTenure) {
        calculatePremium(product.basePremium, selectedCoverage, product.baseCoverage, selectedTenure, product.premiumMultiplier)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Customize ${product.productName}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Coverage Selection
                Text("Select Coverage Amount", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                product.coverageOptions.forEach { coverage ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCoverage = coverage }
                            .background(
                                if (selectedCoverage == coverage) Color(0xFFE8F5E9) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCoverage == coverage,
                            onClick = { selectedCoverage = coverage }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(formatCurrency(coverage))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tenure Selection
                Text("Select Policy Tenure", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    product.tenureOptions.forEach { tenure ->
                        FilterChip(
                            selected = selectedTenure == tenure,
                            onClick = { selectedTenure = tenure },
                            label = { Text("$tenure ${if (product.productType == "Life") "years" else "year${if (tenure > 1) "s" else ""}"}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Premium Summary", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Coverage:", color = Color.Gray)
                            Text(formatCurrency(selectedCoverage), fontWeight = FontWeight.Medium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tenure:", color = Color.Gray)
                            Text("$selectedTenure ${if (product.productType == "Life") "years" else "year${if (selectedTenure > 1) "s" else ""}"}", fontWeight = FontWeight.Medium)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Annual Premium:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                formatCurrency(calculatedPremium),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onApply(selectedCoverage, selectedTenure, calculatedPremium)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Buy Policy")
                    }
                }
            }
        }
    }
}

// Helper Functions
fun loadPolicyProducts(callback: (List<PolicyProduct>, List<PolicyProduct>, String?) -> Unit) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://vcard.tecgs.com:3000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(ApiService::class.java)

    var lifeProducts = emptyList<PolicyProduct>()
    var healthProducts = emptyList<PolicyProduct>()
    var completedCalls = 0
    var error: String? = null

    // Fetch Life products
    api.getPolicyProducts("Life").enqueue(object : Callback<PolicyProductsResponse> {
        override fun onResponse(call: Call<PolicyProductsResponse>, response: Response<PolicyProductsResponse>) {
            if (response.isSuccessful && response.body()?.success == true) {
                lifeProducts = response.body()?.products ?: emptyList()
            } else {
                error = "Failed to load Life products"
            }
            completedCalls++
            if (completedCalls == 2) callback(lifeProducts, healthProducts, error)
        }

        override fun onFailure(call: Call<PolicyProductsResponse>, t: Throwable) {
            error = t.message
            completedCalls++
            if (completedCalls == 2) callback(lifeProducts, healthProducts, error)
        }
    })

    // Fetch Health products
    api.getPolicyProducts("Health").enqueue(object : Callback<PolicyProductsResponse> {
        override fun onResponse(call: Call<PolicyProductsResponse>, response: Response<PolicyProductsResponse>) {
            if (response.isSuccessful && response.body()?.success == true) {
                healthProducts = response.body()?.products ?: emptyList()
            } else {
                error = "Failed to load Health products"
            }
            completedCalls++
            if (completedCalls == 2) callback(lifeProducts, healthProducts, error)
        }

        override fun onFailure(call: Call<PolicyProductsResponse>, t: Throwable) {
            error = t.message
            completedCalls++
            if (completedCalls == 2) callback(lifeProducts, healthProducts, error)
        }
    })
}

fun submitApplication(
    context: android.content.Context,
    product: PolicyProduct,
    coverage: Double,
    tenure: Int,
    premium: Double,
    callback: (Boolean, String?) -> Unit
) {
    val userId = UserSession.userId
    if (userId == -1) {
        callback(false, "User not logged in")
        return
    }

    val retrofit = Retrofit.Builder()
        .baseUrl("https://vcard.tecgs.com:3000/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(ApiService::class.java)

    // Generate policy number
    val policyNumber = "POL-${product.productType.take(1)}-${System.currentTimeMillis()}"

    // Calculate dates
    val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val calendar = java.util.Calendar.getInstance()
    calendar.add(java.util.Calendar.YEAR, tenure)
    val endDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(calendar.time)

    // Create request matching /api/insurance/policy endpoint
    val requestBody = mapOf(
        "userId" to userId,
        "policyNumber" to policyNumber,
        "policyType" to product.productType,
        "insurerName" to product.insurerName,
        "premiumAmount" to premium,
        "sumAssured" to coverage,
        "policyStartDate" to currentDate,
        "policyEndDate" to endDate,
        "status" to "Active"
    )

    // Use existing createInsurancePolicy endpoint from SCMasterScreen
    api.createInsurancePolicy(requestBody).enqueue(object : Callback<Map<String, Any>> {
        override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
            if (response.isSuccessful && response.body()?.get("success") == true) {
                callback(true, null)
            } else {
                val message = response.body()?.get("message") as? String ?: "Failed to create policy"
                callback(false, message)
            }
        }

        override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
            callback(false, t.message)
        }
    })
}

fun calculatePremium(
    basePremium: Double,
    selectedCoverage: Double,
    baseCoverage: Double,
    tenure: Int,
    multiplier: Double
): Double {
    val coverageRatio = selectedCoverage / baseCoverage
    val tenureFactor = 1.0 + (tenure * multiplier)
    return basePremium * coverageRatio * tenureFactor
}

