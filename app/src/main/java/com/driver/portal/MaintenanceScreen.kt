package com.driver.portal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.driver.portal.network.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MaintenanceScreen() {
    val context = LocalContext.current

    val driverName = remember { DriverSession.getDriverName(context) }
    val carNumber = remember { DriverSession.getCarNumber(context) }

    val problems = remember { mutableStateListOf<String>() }
    var currentProblem by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    var maintenanceRequests by remember { mutableStateOf<List<MaintenanceItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    fun loadMaintenance() {
        loading = true
        errorMessage = null
        maintenanceRequests = emptyList()

        RetrofitClient.instance.getMaintenanceRequests(
            carNumber = carNumber
        ).enqueue(object : Callback<MaintenanceResponse> {

            override fun onResponse(
                call: Call<MaintenanceResponse>,
                response: Response<MaintenanceResponse>
            ) {
                loading = false

                if (response.isSuccessful) {
                    val filtered = response.body()?.requests
                        ?.filter {
                            it.vehicle.orEmpty().trim() == carNumber.trim()
                        }
                        ?.sortedByDescending { it.requestDate.orEmpty() }

                    maintenanceRequests = filtered ?: emptyList()
                } else {
                    errorMessage = "تعذر تحميل بيانات الصيانة"
                }
            }

            override fun onFailure(call: Call<MaintenanceResponse>, t: Throwable) {
                loading = false
                errorMessage = "فشل تحميل الصيانة"
            }
        })
    }

    LaunchedEffect(refreshKey) {
        loadMaintenance()
    }

    val totalCost = maintenanceRequests.sumOf {
        it.cost?.toDoubleOrNull() ?: 0.0
    }

    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    val monthlyTotal = maintenanceRequests
        .filter { (it.requestDate ?: "").startsWith(currentMonth) }
        .sumOf { it.cost?.toDoubleOrNull() ?: 0.0 }

    val primary = Color(0xFF5B4FD3)
    val primaryDark = Color(0xFF4338CA)
    val bgTop = Color(0xFFF5F3FF)
    val bgBottom = Color(0xFFE8EDFF)
    val textDark = Color(0xFF1F2430)
    val textMuted = Color(0xFF6E7582)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgTop, bgBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(primary, primaryDark)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "نظام الصيانة",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "السائق: $driverName",
                            color = Color.White.copy(alpha = 0.95f)
                        )

                        Text(
                            text = "السيارة: $carNumber",
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "إجمالي الصيانة",
                    value = "${totalCost.toInt()} د.ع",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "هذا الشهر",
                    value = "${monthlyTotal.toInt()} د.ع",
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إضافة صيانة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )

                    OutlinedTextField(
                        value = currentProblem,
                        onValueChange = { currentProblem = it },
                        label = { Text("العطل") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1F2430),
                            unfocusedTextColor = Color(0xFF1F2430),
                            focusedLabelColor = Color(0xFF5B4FD3),
                            unfocusedLabelColor = Color(0xFF6E7582),
                            cursorColor = Color(0xFF5B4FD3),
                            focusedBorderColor = Color(0xFF5B4FD3),
                            unfocusedBorderColor = Color(0xFFD0D5DD),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            if (currentProblem.isNotBlank()) {
                                problems.add(currentProblem.trim())
                                currentProblem = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة العطل")
                    }

                    if (problems.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF7F8FC)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                problems.forEachIndexed { index, item ->
                                    Text("🔧 ${index + 1}- $item", color = textDark)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("تكلفة الصيانة") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1F2430),
                            unfocusedTextColor = Color(0xFF1F2430),
                            focusedLabelColor = Color(0xFF5B4FD3),
                            unfocusedLabelColor = Color(0xFF6E7582),
                            cursorColor = Color(0xFF5B4FD3),
                            focusedBorderColor = Color(0xFF5B4FD3),
                            unfocusedBorderColor = Color(0xFFD0D5DD),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            val priceNumber = price.toDoubleOrNull()

                            if (problems.isEmpty() || priceNumber == null) {
                                Toast.makeText(context, "تحقق من البيانات", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            sending = true

                            val request = MaintenanceRequest(
                                driver = driverName,
                                vehicle = carNumber,
                                problem = problems.joinToString(" | "),
                                price = priceNumber
                            )

                            RetrofitClient.instance.sendMaintenanceRequest(request)
                                .enqueue(object : Callback<ApiResponse> {

                                    override fun onResponse(
                                        call: Call<ApiResponse>,
                                        response: Response<ApiResponse>
                                    ) {
                                        sending = false

                                        if (response.isSuccessful) {
                                            Toast.makeText(
                                                context,
                                                "تم إرسال طلب الصيانة",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            problems.clear()
                                            currentProblem = ""
                                            price = ""
                                            refreshKey++
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "تعذر إرسال طلب الصيانة",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }

                                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                                        sending = false
                                        Toast.makeText(
                                            context,
                                            "فشل إرسال طلب الصيانة",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !sending
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("إرسال طلب الصيانة")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الصيانة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )

                FilledTonalButton(
                    onClick = { refreshKey++ },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحديث")
                }
            }

            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(errorMessage ?: "", color = Color.Red)
                            Button(
                                onClick = { refreshKey++ },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }

                maintenanceRequests.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("لا توجد طلبات صيانة لهذه السيارة", color = textMuted)
                        }
                    }
                }

                else -> {
                    maintenanceRequests.forEach { request ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = request.vehicle ?: carNumber,
                                            fontWeight = FontWeight.Bold,
                                            color = textDark
                                        )
                                    }

                                    Text(
                                        text = "${request.cost ?: "0"} د.ع",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                request.problem.orEmpty()
                                    .split("|")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .forEach {
                                        Text("🔧 $it", color = textDark)
                                    }

                                Text(
                                    text = "📅 ${(request.requestDate ?: "").take(10)}",
                                    color = textMuted
                                )

                                if (!request.status.isNullOrBlank()) {
                                    Text(
                                        text = "الحالة: ${request.status}",
                                        color = primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color(0xFF6E7582),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2430)
            )
        }
    }
}