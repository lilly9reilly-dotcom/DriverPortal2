package com.driver.portal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.TrendingUp
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

@Composable
fun WalletScreen() {
    val context = LocalContext.current
    val driverName = DriverSession.getDriverName(context).ifEmpty { "غير معروف" }

    var balance by remember { mutableStateOf("0") }
    var trips by remember { mutableStateOf("0") }
    var qty by remember { mutableStateOf("0") }
    var liters by remember { mutableStateOf("0") }

    var loading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    val primary = MaterialTheme.colorScheme.primary
    val primaryDark = MaterialTheme.colorScheme.primaryContainer
    val bgTop = MaterialTheme.colorScheme.background
    val bgBottom = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val textDark = MaterialTheme.colorScheme.onBackground
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(refreshKey, driverName) {
        loading = true
        hasError = false

        try {
            val result = withContext(Dispatchers.IO) {
                val url = com.driver.portal.network.GoogleSheetConfig.execUrl(
                    "wallet",
                    "driverName" to driverName
                )
                URL(url).readText()
            }

            val json = JSONObject(result)

            balance = "%,d".format(json.optLong("profit", 0L))
            trips = json.optLong("trips", 0L).toString()
            qty = "%,d".format(json.optLong("quantity", 0L))
            liters = "%,d".format(json.optLong("liters", 0L))

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
                    BalanceCard(
                        balance = balance,
                        primary = primary,
                        primaryDark = primaryDark
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WalletStatCard(
                            title = "عدد النقلات",
                            value = trips,
                            suffix = "",
                            icon = Icons.Default.Route,
                            accent = primary,
                            modifier = Modifier.weight(1f)
                        )

                        WalletStatCard(
                            title = "مجموع الكمية",
                            value = qty,
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
                            title = "لترات الكاز",
                            value = liters,
                            suffix = "لتر",
                            icon = Icons.Default.LocalGasStation,
                            accent = Color(0xFFEF6C00),
                            modifier = Modifier.weight(1f)
                        )

                        WalletStatCard(
                            title = "حالة المحفظة",
                            value = if ((balance.replace(",", "").toLongOrNull() ?: 0L) > 0L) "ممتازة" else "ضعيفة",
                            suffix = "",
                            icon = Icons.Default.TrendingUp,
                            accent = if ((balance.replace(",", "").toLongOrNull() ?: 0L) > 0) Color(0xFF2E7D32) else Color(0xFFE53935),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SummaryCard(
                        balance = balance,
                        trips = trips,
                        qty = qty,
                        liters = liters,
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
                    text = "متابعة الرصيد والإحصائيات الخاصة بالسائق: $driverName",
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
private fun BalanceCard(
    balance: String,
    primary: Color,
    primaryDark: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(primary, primaryDark)
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }

                Text(
                    text = "الرصيد الحالي",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyLarge
                )

                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = balance,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "دينار",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
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
    balance: String,
    trips: String,
    qty: String,
    liters: String,
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

            WalletLine("الرصيد", "$balance دينار", textDark, textMuted)
            WalletLine("عدد النقلات", trips, textDark, textMuted)
            WalletLine("مجموع الكمية", "$qty طن", textDark, textMuted)
            WalletLine("لترات الكاز", "$liters لتر", textDark, textMuted)
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