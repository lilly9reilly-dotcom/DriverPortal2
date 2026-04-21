package com.driver.portal

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

@Composable
fun ReportsScreen() {
    val context    = LocalContext.current
    val driverName = DriverSession.getDriverName(context).ifEmpty { "غير معروف" }
    val carNumber  = DriverSession.getCarNumber(context).ifEmpty { "-" }

    var trips       by remember { mutableStateOf("0") }
    var quantity    by remember { mutableStateOf("0") }
    var liters      by remember { mutableStateOf("0") }
    var profit      by remember { mutableStateOf("0") }
    var maintenance by remember { mutableStateOf("0") }
    var net         by remember { mutableStateOf("0") }
    var distance    by remember { mutableStateOf("0") }
    var loads       by remember { mutableStateOf("0") }
    var loading     by remember { mutableStateOf(true) }
    var hasError    by remember { mutableStateOf(false) }
    var refreshKey  by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey, driverName) {
        loading  = true
        hasError = false
        try {
            val result = withContext(Dispatchers.IO) {
                val url = com.driver.portal.network.GoogleSheetConfig.execUrl(
                    "wallet",
                    "driverName" to driverName
                )
                URL(url).readText()
            }
            val json    = JSONObject(result)
            trips       = json.optLong("trips",       0L).toString()
            loads       = json.optLong("trips",       0L).toString()
            quantity    = "%,d".format(json.optLong("quantity",    0L))
            liters      = "%,d".format(json.optLong("liters",      0L))
            profit      = "%,d".format(json.optLong("profit",      0L))
            maintenance = "%,d".format(json.optLong("maintenance", 0L))
            net         = "%,d".format(json.optLong("netProfit",   0L))
            distance    = "0"
            loading     = false
        } catch (e: Exception) {
            loading  = false
            hasError = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF4F4FF), Color(0xFFE8EEFF))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Header ──────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6C63FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "التقارير",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "تقرير السائق: $driverName",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "رقم السيارة: $carNumber",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // ── Content ─────────────────────────────────
            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF6C63FF))
                    }
                }

                hasError -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "تعذر تحميل التقرير",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2430)
                            )
                            Button(
                                onClick = { refreshKey++ },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("الرحلات",  trips,              Color(0xFF5B4FD3), Modifier.weight(1f))
                        ReportCard("الوصولات", loads,              Color(0xFF1976D2), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("الكمية",  "$quantity طن",     Color(0xFF00897B), Modifier.weight(1f))
                        ReportCard("الكاز",   "$liters لتر",      Color(0xFFEF6C00), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("الربح",   "$profit د.ع",      Color(0xFF2E7D32), Modifier.weight(1f))
                        ReportCard("الصيانة", "$maintenance د.ع", Color(0xFFE53935), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("الصافي",  "$net د.ع",         Color(0xFF6A1B9A), Modifier.weight(1f))
                        ReportCard("المسافة", "$distance كم",     Color(0xFF00838F), Modifier.weight(1f))
                    }
                }
            }

            // ── أزرار التقرير ────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إجراءات التقرير",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            ReportUtils.generateReportPdf(
                                context     = context,
                                driverName  = driverName,
                                carNumber   = carNumber,
                                trips       = trips,
                                loads       = loads,
                                quantity    = quantity,
                                liters      = liters,
                                profit      = profit,
                                maintenance = maintenance,
                                net         = net,
                                distance    = distance
                            )
                            Toast.makeText(context, "تم إنشاء PDF", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6C63FF),
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنشاء ملف PDF", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val file = ReportUtils.generateReportPdf(
                                context     = context,
                                driverName  = driverName,
                                carNumber   = carNumber,
                                trips       = trips,
                                loads       = loads,
                                quantity    = quantity,
                                liters      = liters,
                                profit      = profit,
                                maintenance = maintenance,
                                net         = net,
                                distance    = distance
                            )
                            ReportUtils.sharePdf(context, file)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00897B),
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاركة التقرير", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val file = ReportUtils.generateReportPdf(
                                context     = context,
                                driverName  = driverName,
                                carNumber   = carNumber,
                                trips       = trips,
                                loads       = loads,
                                quantity    = quantity,
                                liters      = liters,
                                profit      = profit,
                                maintenance = maintenance,
                                net         = net,
                                distance    = distance
                            )
                            ReportUtils.printPdf(context, file)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF6C00),
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("طباعة التقرير", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// ── ReportCard ───────────────────────────────────
@Composable
fun ReportCard(
    title:    String,
    value:    String,
    accent:   Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier.height(120.dp),
        shape     = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text     = title,
                    color    = accent,
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text       = value,
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = accent
            )
        }
    }
}