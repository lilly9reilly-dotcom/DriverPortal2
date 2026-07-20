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
import java.util.Calendar
import java.util.Locale

private enum class DriverReportsHalf(val title: String, val token: String, val fromDay: Int, val toDay: Int) {
    All("الشهر الكامل", "all", 1, 31),
    FirstHalf("1 - 15", "first", 1, 15),
    SecondHalf("16 - نهاية الشهر", "second", 16, 31)
}

@Composable
fun ReportsScreen() {
    val tonRateDinar = 35_500.0
    val context    = LocalContext.current
    val driverName = DriverSession.getDriverName(context).ifEmpty { "غير معروف" }
    val carNumber  = DriverSession.getCarNumber(context).ifEmpty { "-" }

    var trips by remember { mutableStateOf("0") }
    var loads by remember { mutableStateOf("0") }
    var quantity by remember { mutableStateOf("0") }
    var factoryQuantity by remember { mutableStateOf("0") }
    var liters by remember { mutableStateOf("0") }
    var finalQty by remember { mutableStateOf("0") }
    var tripAmount by remember { mutableStateOf("0") }
    var factoryAmount by remember { mutableStateOf("0") }
    var profit by remember { mutableStateOf("0") }
    var maintenance by remember { mutableStateOf("0") }
    var net by remember { mutableStateOf("0") }
    var distance by remember { mutableStateOf("0") }
    var exportHeaders by remember { mutableStateOf(listOf<String>()) }
    var exportRows by remember { mutableStateOf(listOf<List<String>>()) }

    var selectedHalf by remember { mutableStateOf(DriverReportsHalf.All) }
    var periodLabel by remember { mutableStateOf("") }

    var loading     by remember { mutableStateOf(true) }
    var hasError    by remember { mutableStateOf(false) }
    var refreshKey  by remember { mutableStateOf(0) }

    fun readNumber(json: JSONObject, key: String): Double {
        if (!json.has(key) || json.isNull(key)) return 0.0
        return try {
            when (val value = json.get(key)) {
                is Number -> value.toDouble()
                is String -> value.replace(",", "").trim().toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }

    fun readNumber(json: JSONObject, vararg keys: String): Double {
        for (key in keys) {
            val value = readNumber(json, key)
            if (value != 0.0) return value
        }
        return 0.0
    }

    fun formatQty(value: Double): String {
        val rounded = kotlin.math.round(value * 1000.0) / 1000.0
        return if (rounded % 1.0 == 0.0) {
            "%,d".format(rounded.toLong())
        } else {
            "%,.3f".format(rounded)
        }
    }

    fun normalizeToTons(value: Double): Double {
        return if (kotlin.math.abs(value) >= 1000.0) value / 1000.0 else value
    }

    fun normalizeArabicText(value: String): String {
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
    }

    fun extractDayFromAnyDate(value: String): Int {
        val text = value.trim()
        if (text.isBlank()) return 0

        val ymd = Regex("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").find(text)
        if (ymd != null) {
            return ymd.groupValues[3].toIntOrNull() ?: 0
        }

        val eng = Regex("\\b[A-Za-z]{3}\\s+[A-Za-z]{3}\\s+(\\d{1,2})\\s+\\d{4}\\b").find(text)
        if (eng != null) {
            return eng.groupValues[1].toIntOrNull() ?: 0
        }

        return 0
    }

    LaunchedEffect(refreshKey, driverName, selectedHalf) {
        loading  = true
        hasError = false
        try {
            val calendar = Calendar.getInstance()
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val year = calendar.get(Calendar.YEAR).toString()
            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val rangeFrom = selectedHalf.fromDay
            val rangeTo = if (selectedHalf == DriverReportsHalf.SecondHalf) maxDay else minOf(selectedHalf.toDay, maxDay)
            val monthKey = "${year}_${month}"
            periodLabel = "من $rangeFrom/$month إلى $rangeTo/$month/$year"

            val result = withContext(Dispatchers.IO) {
                val url = com.driver.portal.network.GoogleSheetConfig.execUrl(
                    "getAllReceiptsData",
                    "companyId" to com.driver.portal.network.DriverScopeConfig.COMPANY_ID,
                    "activationCode" to com.driver.portal.network.DriverScopeConfig.ACTIVATION_CODE,
                    "deviceId" to com.driver.portal.network.DriverScopeConfig.DEVICE_ID,
                    "packageName" to com.driver.portal.network.DriverScopeConfig.PACKAGE_NAME,
                    "month" to monthKey
                )
                URL(url).readText()
            }

            val json = JSONObject(result)
            val arr = json.optJSONArray("data") ?: org.json.JSONArray()
            val rowsForExport = mutableListOf<List<String>>()
            val headersForExport = listOf(
                "رقم الوصل",
                "اسم السائق",
                "رقم السيارة",
                "المصدر",
                "الوجهة",
                "المعمل",
                "تاريخ التحميل",
                "تاريخ التفريغ",
                "الكمية",
                "الكمية النهائية",
                "لترات الكاز",
                "الكروة",
                "سعر النقلة المذكور",
                "سعر النقل المحتسب",
                "الملف",
                "الفترة",
                "الملاحظات"
            )

            var halafayaTripsCount = 0
            var factoryTripsCount = 0
            var halafayaQty = 0.0
            var factoryQtyTotal = 0.0
            var litersTotal = 0.0
            var halafayaAmountTotal = 0.0
            var factoryAmountTotal = 0.0
            var accountingRevenue = 0.0

            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val docNum = item.optString("docNumber").trim()
                if (docNum.isBlank()) continue

                val source = item.optString("source").trim().lowercase()
                val station = item.optString("station").ifBlank { item.optString("destination") }.trim()
                val destination = item.optString("destination").trim()
                val factory = item.optString("factory").trim()
                val sheetName = item.optString("sheetName").trim().lowercase()
                val period = item.optInt("period15", 0)

                if (selectedHalf != DriverReportsHalf.All) {
                    if (period == 1 && selectedHalf == DriverReportsHalf.SecondHalf) continue
                    if (period == 2 && selectedHalf == DriverReportsHalf.FirstHalf) continue
                    if (period == 0) {
                        val rowDay = extractDayFromAnyDate(
                            item.optString("unloadDate").ifBlank {
                                item.optString("loadDate").ifBlank { item.optString("timestamp") }
                            }
                        )
                        if (rowDay <= 0 || rowDay < rangeFrom || rowDay > rangeTo) continue
                    }
                }

                val rowCar = item.optString("carNumber").trim()
                if (rowCar != carNumber) continue

                val isFactoryRow =
                    sheetName.startsWith("f_") ||
                    source.contains("factory") ||
                        station.contains("معمل") ||
                        destination.contains("معمل") ||
                        factory.contains("معمل")

                val grossQty = readNumber(item, "quantity", "qty")
                val qtyTons = normalizeToTons(grossQty)
                val finalAmount = qtyTons * tonRateDinar

                if (isFactoryRow && qtyTons <= 0.0) continue
                if (!isFactoryRow && qtyTons <= 0.0) continue

                accountingRevenue += finalAmount

                rowsForExport.add(
                    listOf(
                        docNum,
                        item.optString("driverName"),
                        item.optString("carNumber"),
                        item.optString("source"),
                        station.ifBlank { destination },
                        factory,
                        item.optString("loadDate"),
                        item.optString("unloadDate"),
                        formatQty(qtyTons),
                        formatQty(qtyTons),
                        readNumber(item, "liters", "gas").toLong().toString(),
                        readNumber(item, "kroa", "driverfare", "fare").toLong().toString(),
                        finalAmount.toLong().toString(),
                        finalAmount.toLong().toString(),
                        item.optString("sheetName"),
                        period.toString(),
                        item.optString("notes")
                    )
                )

                if (isFactoryRow) {
                    factoryTripsCount++
                    factoryQtyTotal += qtyTons
                    factoryAmountTotal += finalAmount
                } else {
                    halafayaTripsCount++
                    halafayaQty += qtyTons
                    litersTotal += readNumber(item, "liters", "gas")
                    halafayaAmountTotal += finalAmount
                }
            }

            val grossTotalQty = halafayaQty + factoryQtyTotal
            val finalQtyVal = grossTotalQty
            val gasCostVal = litersTotal * 430.0
            val netAfterGasVal = accountingRevenue - gasCostVal

            trips = halafayaTripsCount.toString()
            loads = factoryTripsCount.toString()
            quantity = formatQty(halafayaQty)
            factoryQuantity = formatQty(factoryQtyTotal)
            liters = "%,d".format(litersTotal.toLong())
            finalQty = formatQty(finalQtyVal)
            tripAmount = "%,d".format(halafayaAmountTotal.toLong())
            factoryAmount = "%,d".format(factoryAmountTotal.toLong())
            profit = "%,d".format(accountingRevenue.toLong())
            maintenance = "%,d".format(gasCostVal.toLong())
            net = "%,d".format(netAfterGasVal.toLong())
            distance = formatQty(finalQtyVal)
            exportHeaders = headersForExport
            exportRows = rowsForExport

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

                    Text(
                        text = "الفترة: ${selectedHalf.title}",
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "نطاق التاريخ: $periodLabel",
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DriverReportsHalf.entries.forEach { half ->
                    OutlinedButton(
                        onClick = { selectedHalf = half },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedHalf == half) Color(0xFF6C63FF).copy(alpha = 0.12f) else Color.White,
                            contentColor = if (selectedHalf == half) Color(0xFF4B42D9) else Color(0xFF6E7582)
                        )
                    ) {
                        Text(half.title, maxLines = 1)
                    }
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
                        ReportCard("نقلات حلفاية", trips, Color(0xFF5B4FD3), Modifier.weight(1f))
                        ReportCard("نقلات المعمل", loads, Color(0xFF1976D2), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("كمية حلفاية", "$quantity طن", Color(0xFF00897B), Modifier.weight(1f))
                        ReportCard("كمية المعمل", "$factoryQuantity طن", Color(0xFF43A047), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("الكمية النهائية", "$finalQty طن", Color(0xFF00796B), Modifier.fillMaxWidth())
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("حساب حلفاية", "$tripAmount د.ع", Color(0xFF1565C0), Modifier.weight(1f))
                        ReportCard("حساب المعمل", "$factoryAmount د.ع", Color(0xFF2E7D32), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("إجمالي الحساب", "$profit د.ع", Color(0xFF00695C), Modifier.weight(1f))
                        ReportCard("حساب الكاز", "$maintenance د.ع", Color(0xFFEF6C00), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportCard("الصافي بعد الكاز", "$net د.ع", Color(0xFF1B5E20), Modifier.weight(1f))
                        ReportCard("لترات الكاز", "$liters لتر", Color(0xFF6A1B9A), Modifier.weight(1f))
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                            modifier = Modifier.weight(1f),
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
                            modifier = Modifier.weight(1f),
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
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val file = ReportUtils.generateReportXlsx(
                                    context = context,
                                    driverName = driverName,
                                    carNumber = carNumber,
                                    periodLabel = periodLabel,
                                    summaryRows = listOf(
                                        "نقلات حلفاية" to trips,
                                        "نقلات المعمل" to loads,
                                        "كمية حلفاية" to "$quantity طن",
                                        "كمية المعمل" to "$factoryQuantity طن",
                                        "الكمية النهائية" to "$finalQty طن",
                                        "حساب حلفاية" to "$tripAmount د.ع",
                                        "حساب المعمل" to "$factoryAmount د.ع",
                                        "إجمالي الحساب" to "$profit د.ع",
                                        "حساب الكاز" to "$maintenance د.ع",
                                        "الصافي بعد الكاز" to "$net د.ع",
                                        "لترات الكاز" to "$liters لتر"
                                    ),
                                    tripHeaders = exportHeaders,
                                    tripRows = exportRows
                                )
                                ReportUtils.shareXlsx(context, file)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B8F5A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تصدير Excel", fontWeight = FontWeight.Bold)
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
                            modifier = Modifier.weight(1f),
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