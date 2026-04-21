package com.driver.portal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

@Composable
fun TripsHistoryScreen(driverName: String) {
    var trips by remember { mutableStateOf<List<TripItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }

    val primary = Color(0xFF6C63FF)
    val primaryDark = Color(0xFF4B42D9)
    val bgTop = Color(0xFFF5F3FF)
    val bgBottom = Color(0xFFE9EEFF)

    LaunchedEffect(refreshKey, driverName) {
        loading = true
        hasError = false

        try {
            val result = withContext(Dispatchers.IO) {
                val url = com.driver.portal.network.GoogleSheetConfig.execUrl(
                    "history",
                    "driverName" to driverName
                )

                URL(url).readText()
            }

            val json = JSONObject(result)
            val arr: JSONArray = json.optJSONArray("trips") ?: JSONArray()
            val list = mutableListOf<TripItem>()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                normalizeTripItem(obj)?.let(list::add)
            }

            trips = list
            loading = false

        } catch (e: Exception) {
            e.printStackTrace()
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            HistoryHeaderCard(
                driverName = driverName,
                primary = primary,
                primaryDark = primaryDark,
                onRefresh = { refreshKey++ }
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primary)
                    }
                }

                hasError -> {
                    EmptyStateCard(
                        title = "تعذر تحميل السجل",
                        subtitle = "تحقق من الاتصال بالشبكة ثم أعد المحاولة",
                        buttonText = "إعادة المحاولة",
                        onClick = { refreshKey++ }
                    )
                }

                trips.isEmpty() -> {
                    EmptyStateCard(
                        title = "لا يوجد سجل نقلات",
                        subtitle = "عند إضافة نقلات جديدة ستظهر هنا تلقائيًا",
                        buttonText = "تحديث",
                        onClick = { refreshKey++ }
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(trips) { trip ->
                            TripCard(
                                trip = trip,
                                driverName = driverName,
                                onRefresh = { refreshKey++ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeaderCard(
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "سجل النقلات",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "متابعة جميع النقلات الخاصة بالسائق: $driverName",
                    color = Color.White.copy(alpha = 0.85f),
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
                    Text("تحديث السجل", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TripCard(
    trip: TripItem,
    driverName: String,
    onRefresh: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedIssue by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val reported = trip.status == "reported"
    val accent = if (reported) Color(0xFFE53935) else Color(0xFF10B981)
    val container = if (reported) {
        Color(0xFFFFF3F3)
    } else {
        Color.White
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "وصل #${trip.docNumber.orEmpty()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2430)
                    )

                    StatusBadge(
                        text = if (reported) "تم الإبلاغ عن هذا الوصل" else "الوصل سليم",
                        color = accent,
                        icon = if (reported) Icons.Default.ErrorOutline else Icons.Default.Verified
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF3F1FF)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = trip.quantity.orEmpty()
                                .takeIf { it.isNotBlank() }
                                ?.let { "$it طن" }
                                ?: "غير محدد",
                            color = Color(0xFF6C63FF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider()

            if (trip.carNumber.orEmpty().isNotBlank() || trip.station.orEmpty().isNotBlank()) {
                DataGridRow(
                    leftLabel = "السيارة",
                    leftValue = trip.carNumber.orEmpty(),
                    leftIcon = Icons.Default.LocalShipping,
                    rightLabel = "المحطة",
                    rightValue = trip.station.orEmpty(),
                    rightIcon = Icons.Default.LocationOn
                )
            }

            if (trip.price.orEmpty().isNotBlank() || trip.quantity.orEmpty().isNotBlank()) {
                DataGridRow(
                    leftLabel = "سعر النقلة",
                    leftValue = trip.price.orEmpty(),
                    leftIcon = Icons.Default.Payments,
                    rightLabel = "الكمية",
                    rightValue = trip.quantity.orEmpty(),
                    rightIcon = Icons.Default.Scale
                )
            }

            if (trip.loadDate.orEmpty().isNotBlank() || trip.unloadDate.orEmpty().isNotBlank()) {
                DataGridRow(
                    leftLabel = "تاريخ التحميل",
                    leftValue = trip.loadDate.orEmpty(),
                    leftIcon = Icons.Default.CalendarMonth,
                    rightLabel = "تاريخ التفريغ",
                    rightValue = trip.unloadDate.orEmpty(),
                    rightIcon = Icons.Default.CalendarMonth
                )
            }

            if (trip.date.orEmpty().isNotBlank()) {
                InfoLine(
                    label = "تاريخ الإدخال",
                    value = formatDate(trip.date.orEmpty())
                )
            }

            if (!reported) {
                TextButton(
                    onClick = { showDialog = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "الإبلاغ عن خطأ",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!sending) showDialog = false
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedIssue.isEmpty() || trip.docNumber.isNullOrEmpty() || sending) {
                            return@Button
                        }

                        sending = true

                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                try {
                                    val url = com.driver.portal.network.GoogleSheetConfig.execUrl(
                                        "reportIssue",
                                        "driverName" to driverName,
                                        "docNumber" to trip.docNumber,
                                        "issueType" to selectedIssue
                                    )

                                    URL(url).readText()
                                    true
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    false
                                }
                            }

                            sending = false
                            if (result) {
                                onRefresh()
                                showDialog = false
                            }
                        }
                    },
                    enabled = selectedIssue.isNotEmpty() && !sending
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("إرسال")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { if (!sending) showDialog = false }
                ) {
                    Text("إلغاء")
                }
            },
            title = { Text("تحديد المشكلة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "الكمية غلط",
                        "المحطة غلط",
                        "السعر غلط",
                        "غير ذلك"
                    ).forEach { issue ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedIssue == issue,
                                onClick = { selectedIssue = issue }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(issue)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DataGridRow(
    leftLabel: String,
    leftValue: String,
    leftIcon: androidx.compose.ui.graphics.vector.ImageVector,
    rightLabel: String,
    rightValue: String,
    rightIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DataCell(
            modifier = Modifier.weight(1f),
            label = leftLabel,
            value = leftValue,
            icon = leftIcon
        )

        DataCell(
            modifier = Modifier.weight(1f),
            label = rightLabel,
            value = rightValue,
            icon = rightIcon
        )
    }
}

@Composable
private fun DataCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7F8FC)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6C63FF),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = Color(0xFF6E7582),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = value.ifBlank { "-" },
                color = Color(0xFF1F2430),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF6E7582),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            color = Color(0xFF1F2430),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2430)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6E7582)
            )

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF),
                    contentColor = Color.White
                )
            ) {
                Text(buttonText)
            }
        }
    }
}

private fun normalizeTripItem(obj: JSONObject): TripItem? {
    val docNumber = obj.optString("docNumber").cleanField()
    val carNumber = obj.optString("carNumber").cleanField()
    val rawLoadDate = obj.optString("loadDate").cleanField()
    val rawUnloadDate = obj.optString("unloadDate").cleanField()
    val rawQuantity = obj.optString("quantity").cleanField()
    val rawStation = obj.optString("station").cleanField()
    val rawPrice = obj.optString("price").cleanField()
    val rawTotal = obj.optString("total").cleanField()
    val rawDate = obj.optString("date").cleanField()
    val rawStatus = obj.optString("status", "ok").cleanField().ifBlank { "ok" }
    val rawImageUrl = obj.optString("imageUrl").cleanField()
    val rawSendTime = obj.optString("sendTime").cleanField()
    val rawNotes = obj.optString("notes").cleanField()
    val rawOwner = obj.optString("owner").cleanField()
    val rawFinalQuantity = obj.optString("finalQuantity").cleanField()
    val rawFinalAmount = obj.optString("finalAmount").cleanField()

    val station = firstNotBlankNonUrl(
        rawStation,
        rawUnloadDate.takeIf { !looksLikeDateValue(it) },
        rawLoadDate.takeIf { !looksLikeDateValue(it) }
    )

    val quantity = firstNumericValue(rawQuantity, rawFinalQuantity)
    val price = firstNumericValue(rawPrice)
    val total = firstNumericValue(rawTotal, rawFinalAmount)

    val loadDate = rawLoadDate.takeIf { looksLikeDateValue(it) }
    val unloadDate = rawUnloadDate.takeIf { looksLikeDateValue(it) }
    val date = firstDateValue(rawDate, rawSendTime, rawOwner)
    val imageUrl = firstUrlValue(rawImageUrl, rawQuantity, rawLoadDate, rawUnloadDate)

    val meaningfulCount = listOf(station, quantity, price, loadDate, unloadDate)
        .count { !it.isNullOrBlank() }

    if (docNumber.isBlank() || meaningfulCount < 2) {
        return null
    }

    return TripItem(
        docNumber = docNumber,
        carNumber = carNumber,
        loadDate = loadDate,
        unloadDate = unloadDate,
        quantity = quantity,
        station = station,
        price = price,
        total = total,
        date = date,
        status = rawStatus,
        imageUrl = imageUrl,
        sendTime = rawSendTime,
        notes = rawNotes,
        owner = rawOwner,
        finalQuantity = rawFinalQuantity,
        finalAmount = rawFinalAmount
    )
}

internal fun sanitizeTripForDisplay(trip: TripItem): TripItem {
    val station = firstNotBlankNonUrl(
        trip.station.cleanField(),
        trip.unloadDate.cleanField().takeIf { !looksLikeDateValue(it) },
        trip.loadDate.cleanField().takeIf { !looksLikeDateValue(it) }
    )

    return trip.copy(
        station = station,
        quantity = firstNumericValue(trip.quantity.cleanField(), trip.finalQuantity.cleanField()),
        price = firstNumericValue(trip.price.cleanField()),
        total = firstNumericValue(trip.total.cleanField(), trip.finalAmount.cleanField()),
        loadDate = trip.loadDate.cleanField().takeIf { looksLikeDateValue(it) },
        unloadDate = trip.unloadDate.cleanField().takeIf { looksLikeDateValue(it) },
        date = firstDateValue(trip.date.cleanField(), trip.sendTime.cleanField(), trip.owner.cleanField()),
        imageUrl = firstUrlValue(
            trip.imageUrl.cleanField(),
            trip.quantity.cleanField(),
            trip.loadDate.cleanField(),
            trip.unloadDate.cleanField()
        )
    )
}

private fun String?.cleanField(): String = this.orEmpty().trim().takeUnless {
    it.equals("null", ignoreCase = true)
} ?: ""

private fun looksLikeUrl(value: String): Boolean {
    val clean = value.cleanField().lowercase()
    return clean.startsWith("http://") || clean.startsWith("https://") || clean.contains("drive.google.com")
}

private fun looksLikeDateValue(value: String): Boolean {
    val clean = value.cleanField()
    return clean.contains("GMT") || clean.contains("/") || clean.contains("-") || clean.contains("Apr") || clean.contains("202")
}

private fun firstDateValue(vararg candidates: String?): String {
    return candidates.firstOrNull { !it.cleanField().isNullOrBlank() && looksLikeDateValue(it.cleanField()) }
        .cleanField()
}

private fun firstUrlValue(vararg candidates: String?): String {
    return candidates.firstOrNull { looksLikeUrl(it.cleanField()) }.cleanField()
}

private fun firstNumericValue(vararg candidates: String?): String {
    return candidates.firstOrNull {
        val clean = it.cleanField()
        clean.isNotBlank() &&
            clean != "0" &&
            clean != "0.0" &&
            !looksLikeUrl(clean) &&
            clean.any(Char::isDigit) &&
            !looksLikeDateValue(clean)
    }.cleanField()
}

private fun firstNotBlankNonUrl(vararg candidates: String?): String {
    return candidates.firstOrNull {
        val clean = it.cleanField()
        clean.isNotBlank() && !looksLikeUrl(clean)
    }.cleanField()
}

fun formatDate(raw: String): String {
    if (raw.isBlank()) return "-"

    return try {
        val parser = java.text.SimpleDateFormat(
            "EEE MMM dd yyyy HH:mm:ss",
            java.util.Locale.ENGLISH
        )

        val value = if (raw.length >= 24) raw.substring(0, 24) else raw
        val date = parser.parse(value)

        val formatter = java.text.SimpleDateFormat(
            "yyyy/MM/dd - HH:mm",
            java.util.Locale("ar")
        )

        if (date != null) formatter.format(date) else raw
    } catch (e: Exception) {
        raw
    }
}