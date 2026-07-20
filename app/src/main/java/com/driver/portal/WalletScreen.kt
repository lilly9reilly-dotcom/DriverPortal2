package com.driver.portal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Calendar

@Composable
fun WalletScreen() {
    val context = LocalContext.current
    val driverName = DriverSession.getDriverName(context).ifEmpty { "غير معروف" }

    var totalKroa by remember { mutableStateOf("0") }
    var totalTrips by remember { mutableStateOf("0") }
    var loadedQty by remember { mutableStateOf("0") }
    var halafayaTrips by remember { mutableStateOf("0") }
    var factoryTrips by remember { mutableStateOf("0") }
    var rasafaTrips by remember { mutableStateOf("0") }
    var douraTrips by remember { mutableStateOf("0") }

    var loading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    val primary = MaterialTheme.colorScheme.primary
    val primaryDark = MaterialTheme.colorScheme.primaryContainer
    val bgTop = MaterialTheme.colorScheme.background
    val bgBottom = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val textDark = MaterialTheme.colorScheme.onBackground
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant

    fun normalizeArabicText(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
    }

    fun formatQty(value: Double): String {
        val rounded = (value * 1000.0).toLong() / 1000.0
        val whole = rounded.toLong().toDouble() == rounded
        return if (whole) {
            "%,d".format(rounded.toLong())
        } else {
            "%,.3f".format(rounded)
        }
    }

    fun readNumber(obj: JSONObject, vararg keys: String): Double {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)
            val number = when (value) {
                is Number -> value.toDouble()
                else -> value?.toString().orEmpty()
                    .replace("٠", "0")
                    .replace("١", "1")
                    .replace("٢", "2")
                    .replace("٣", "3")
                    .replace("٤", "4")
                    .replace("٥", "5")
                    .replace("٦", "6")
                    .replace("٧", "7")
                    .replace("٨", "8")
                    .replace("٩", "9")
                    .replace(",", "")
                    .filter { it.isDigit() || it == '.' || it == '-' }
                    .toDoubleOrNull()
            } ?: 0.0
            if (number > 0.0) return number
        }
        return 0.0
    }

    fun normalizeDriverKroa(value: Double): Double {
        if (value <= 0.0) return 0.0
        // Guard against accidental use of trip amount as driver fare.
        return if (value > 10000.0) 0.0 else value
    }

    LaunchedEffect(refreshKey, driverName) {
        loading = true
        hasError = false

        try {
            val result = withContext(Dispatchers.IO) {
                val calendar = Calendar.getInstance()
                val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
                val year = calendar.get(Calendar.YEAR).toString()
                val monthKey = "${year}_${month}"
                val url = com.driver.portal.network.GoogleSheetConfig.execUrl(
                    "getAllReceiptsData",
                    "month" to monthKey
                )
                URL(url).readText()
            }

            val json = JSONObject(result)
            val rows = json.optJSONArray("data")

            var kroaSum = 0.0
            var tripsCount = 0
            var qtySum = 0.0
            var halafayaCount = 0
            var factoryCount = 0
            var rasafaCount = 0
            var douraCount = 0

            if (rows != null) {
                for (i in 0 until rows.length()) {
                    val item = rows.optJSONObject(i) ?: continue

                    val rowDriver = normalizeArabicText(item.optString("driverName"))
                    if (rowDriver != normalizeArabicText(driverName)) continue

                    val destinationRaw = item.optString("destination").ifBlank { item.optString("station") }
                    val destination = normalizeArabicText(destinationRaw)
                    val sheetName = item.optString("sheetName").lowercase()

                    val qtyValue = readNumber(item, "quantity", "qty")
                    val kroaValue = readNumber(
                        item,
                        "kroa",
                        "driverFare",
                        "tripPrice",
                        "fare",
                        "storedPrice"
                    )

                    tripsCount += 1
                    qtySum += qtyValue
                    kroaSum += normalizeDriverKroa(kroaValue)

                    val isFactory = sheetName.startsWith("f_") || destination.contains("معمل")
                    val isHalafaya = destination.contains("حلفاي")
                    val isRasafa = destination.contains("رصاف") || destination.contains("بصاف")
                    val isDoura = destination.contains("دور")

                    if (isFactory) factoryCount += 1
                    if (isHalafaya) halafayaCount += 1
                    if (isRasafa) rasafaCount += 1
                    if (isDoura) douraCount += 1
                }
            }

            totalKroa = "%,d".format(kroaSum.toLong())
            totalTrips = tripsCount.toString()
            loadedQty = formatQty(qtySum)
            halafayaTrips = halafayaCount.toString()
            factoryTrips = factoryCount.toString()
            rasafaTrips = rasafaCount.toString()
            douraTrips = douraCount.toString()

            loading = false

        } catch (e: Exception) {
            loading = false
            hasError = true
        }
    }

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
            WalletHeaderCard(
                driverName = driverName,
                primary = primary,
                primaryDark = primaryDark,
                onRefresh = { refreshKey++ }
            )

            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primary)
                    }
                }

                hasError -> {
                    WalletErrorCard(
                        onRetry = { refreshKey++ }
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletStatCard(
                            title = "مجموع الكروة",
                            value = totalKroa,
                            suffix = "دينار",
                            icon = Icons.Default.Payments,
                            accent = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )

                        WalletStatCard(
                            title = "الكمية المحملة",
                            value = loadedQty,
                            suffix = "طن",
                            icon = Icons.Default.Scale,
                            accent = Color(0xFF00897B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletStatCard(
                            title = "إجمالي النقلات",
                            value = totalTrips,
                            suffix = "",
                            icon = Icons.Default.Route,
                            accent = primary,
                            modifier = Modifier.weight(1f)
                        )

                        WalletStatCard(
                            title = "نقلات حلفاية",
                            value = halafayaTrips,
                            suffix = "",
                            icon = Icons.Default.LocalShipping,
                            accent = Color(0xFF1565C0),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletStatCard(
                            title = "نقلات المعامل",
                            value = factoryTrips,
                            suffix = "",
                            icon = Icons.Default.Warehouse,
                            accent = Color(0xFF6A1B9A),
                            modifier = Modifier.weight(1f)
                        )

                        WalletStatCard(
                            title = "نقلات الرصافة",
                            value = rasafaTrips,
                            suffix = "",
                            icon = Icons.Default.Route,
                            accent = Color(0xFF00838F),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletStatCard(
                            title = "نقلات الدورة",
                            value = douraTrips,
                            suffix = "",
                            icon = Icons.Default.Route,
                            accent = Color(0xFFEF6C00),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.weight(1f))
                    }

                    SummaryCard(
                        totalKroa = totalKroa,
                        totalTrips = totalTrips,
                        loadedQty = loadedQty,
                        halafayaTrips = halafayaTrips,
                        factoryTrips = factoryTrips,
                        rasafaTrips = rasafaTrips,
                        douraTrips = douraTrips,
                        textDark = textDark,
                        textMuted = textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WalletHeaderCard(
    driverName: String,
    primary: Color,
    primaryDark: Color,
    onRefresh: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
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
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "محفظة السائق",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "تقرير السائق المباشر: الكروة، الكمية، وعدد النقلات لكل وجهة ($driverName)",
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium
                )

                FilledTonalButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحديث المحفظة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WalletStatCard(
    title: String,
    value: String,
    suffix: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(148.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(24.dp)
                )
            }

            Text(
                text = title,
                color = Color(0xFF6E7582),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = if (suffix.isBlank()) value else "$value $suffix",
                color = Color(0xFF1F2430),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryCard(
    totalKroa: String,
    totalTrips: String,
    loadedQty: String,
    halafayaTrips: String,
    factoryTrips: String,
    rasafaTrips: String,
    douraTrips: String,
    textDark: Color,
    textMuted: Color
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = Color(0xFF6C63FF)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ملخص المحفظة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textDark
                )
            }

            WalletLine("مجموع الكروة", "$totalKroa دينار", textDark, textMuted)
            WalletLine("الكمية المحملة", "$loadedQty طن", textDark, textMuted)
            WalletLine("إجمالي النقلات", totalTrips, textDark, textMuted)
            WalletLine("نقلات حلفاية", halafayaTrips, textDark, textMuted)
            WalletLine("نقلات المعامل", factoryTrips, textDark, textMuted)
            WalletLine("نقلات الرصافة", rasafaTrips, textDark, textMuted)
            WalletLine("نقلات الدورة", douraTrips, textDark, textMuted)
        }
    }
}

@Composable
private fun WalletLine(
    label: String,
    value: String,
    textDark: Color,
    textMuted: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = textMuted,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = textDark,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun WalletErrorCard(
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
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
                text = "تعذر تحميل المحفظة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2430)
            )

            Text(
                text = "تحقق من الاتصال بالشبكة ثم أعد المحاولة",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6E7582)
            )

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF),
                    contentColor = Color.White
                )
            ) {
                Text("إعادة المحاولة")
            }
        }
    }
}