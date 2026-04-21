package com.driver.portal

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

@Composable
fun DriverDashboardScreen(
    driverName: String,
    onLogout: () -> Unit,
    onOpenMap: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var trips by remember { mutableStateOf("0") }
    var qty by remember { mutableStateOf("0") }
    var gas by remember { mutableStateOf("0") }
    var salary by remember { mutableStateOf("0") }

    var loading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    var latitude by remember { mutableStateOf("0.0") }
    var longitude by remember { mutableStateOf("0.0") }
    var lastLat by remember { mutableStateOf(0.0) }
    var lastLng by remember { mutableStateOf(0.0) }
    var totalDistance by remember { mutableStateOf(0.0) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var locationCallback by remember { mutableStateOf<LocationCallback?>(null) }
    var locationActive by remember { mutableStateOf(false) }
    var locationStatusText by remember { mutableStateOf("غير محدث") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        locationStatusText = if (granted) {
            "تم منح صلاحية الموقع"
        } else {
            "لم يتم منح صلاحية الموقع"
        }
    }

    fun readNumber(json: JSONObject, key: String): Double {
        if (!json.has(key) || json.isNull(key)) return 0.0

        return try {
            when (val value = json.get(key)) {
                is Number -> value.toDouble()
                is String -> value
                    .replace(",", "")
                    .replace("د.ع", "")
                    .trim()
                    .toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    fun loadDashboard() {
        loading = true
        hasError = false

        scope.launch(Dispatchers.IO) {
            try {
                val url = com.driver.portal.network.GoogleSheetConfig.execUrl(
                    "wallet",
                    "driverName" to driverName
                )

                val result = URL(url).readText()
                val json = JSONObject(result)

                val tripsValue = readNumber(json, "trips")
                val qtyValue = readNumber(json, "quantity")
                val gasValue = readNumber(json, "liters")

                val salaryValue = readNumber(json, "profit")

                withContext(Dispatchers.Main) {
                    trips = tripsValue.toInt().toString()
                    qty = "%,d".format(qtyValue.toLong())
                    gas = "%,d".format(gasValue.toLong())
                    salary = "%,d".format(salaryValue.toLong())
                    loading = false
                    hasError = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading = false
                    hasError = true
                }
            }
        }
    }

    fun stopGpsUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        locationActive = false
    }

    @SuppressLint("MissingPermission")
    fun startGpsUpdates() {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        if (locationCallback != null) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            4000L
        )
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return

                val newLat = location.latitude
                val newLng = location.longitude

                if (lastLat != 0.0 && lastLng != 0.0) {
                    val results = FloatArray(1)
                    Location.distanceBetween(lastLat, lastLng, newLat, newLng, results)
                    if (results[0] > 5f) {
                        totalDistance += (results[0] / 1000.0)
                    }
                }

                lastLat = newLat
                lastLng = newLng
                latitude = String.format("%.6f", newLat)
                longitude = String.format("%.6f", newLng)
                locationStatusText = "الموقع محدث"
                locationActive = true
            }
        }

        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(
            request,
            callback,
            Looper.getMainLooper()
        )
    }

    LaunchedEffect(driverName, refreshKey) {
        loadDashboard()
    }

    DisposableEffect(Unit) {
        onDispose {
            stopGpsUpdates()
        }
    }

    val carNumber = remember { DriverSession.getCarNumber(context) }
    val phoneNumber = remember { DriverSession.getDriverPhone(context) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryDark = MaterialTheme.colorScheme.primaryContainer
    val backgroundTop = MaterialTheme.colorScheme.background
    val backgroundBottom = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    val textDark = MaterialTheme.colorScheme.onBackground
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val infoColor = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundTop, backgroundBottom)
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
            DriverHeaderCard(
                driverName = driverName,
                phone = phoneNumber,
                carNumber = carNumber,
                isOnline = true,
                primaryColor = primaryColor,
                primaryDark = primaryDark,
                onRefresh = { refreshKey++ }
            )

            when {
                loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }

                hasError -> {
                    DashboardErrorCard(
                        onRetry = { refreshKey++ }
                    )
                }

                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            icon = Icons.Default.Route,
                            title = "النقلات",
                            value = trips,
                            accent = primaryColor,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = Icons.Default.Scale,
                            title = "الكمية",
                            value = qty,
                            accent = Color(0xFF00897B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            icon = Icons.Default.LocalGasStation,
                            title = "الكاز",
                            value = gas,
                            accent = Color(0xFFEF6C00),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            icon = Icons.Default.MonetizationOn,
                            title = "الربح",
                            value = salary,
                            accent = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            LocationStatusCard(
                latitude = latitude,
                longitude = longitude,
                totalDistance = totalDistance,
                statusText = locationStatusText,
                isActive = locationActive,
                hasPermission = hasLocationPermission,
                primaryColor = primaryColor,
                textDark = textDark,
                textMuted = textMuted,
                onRefreshLocation = { startGpsUpdates() }
            )

            FilledTonalButton(
                onClick = {
                    if (lastLat == 0.0 && lastLng == 0.0) {
                        locationStatusText = "لم يتم تحديد الموقع بعد"
                    } else {
                        onOpenMap(lastLat, lastLng)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = infoColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "فتح الخريطة",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DriverHeaderCard(
    driverName: String,
    phone: String,
    carNumber: String,
    isOnline: Boolean,
    primaryColor: Color,
    primaryDark: Color,
    onRefresh: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(primaryColor, primaryDark)
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مرحبًا بك",
                            color = Color.White.copy(alpha = 0.90f),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = driverName,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }
                }

                InfoChipRow(
                    icon = Icons.Default.Phone,
                    text = "الهاتف: $phone",
                    color = Color.White
                )

                InfoChipRow(
                    icon = Icons.Default.DirectionsCar,
                    text = "السيارة: $carNumber",
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF00E676) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOnline) "متصل" else "غير متصل",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    FilledTonalButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChipRow(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(146.dp),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2430),
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun LocationStatusCard(
    latitude: String,
    longitude: String,
    totalDistance: Double,
    statusText: String,
    isActive: Boolean,
    hasPermission: Boolean,
    primaryColor: Color,
    textDark: Color,
    textMuted: Color,
    onRefreshLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = primaryColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الموقع الحالي",
                        color = textDark,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when {
                            !hasPermission -> "الموقع يحتاج صلاحية"
                            isActive -> statusText
                            else -> "لم يبدأ التتبع بعد"
                        },
                        color = when {
                            !hasPermission -> Color(0xFFE53935)
                            isActive -> Color(0xFF10B981)
                            else -> textMuted
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider()

            DataLine(
                label = "Latitude",
                value = latitude,
                textDark = textDark,
                textMuted = textMuted
            )

            DataLine(
                label = "Longitude",
                value = longitude,
                textDark = textDark,
                textMuted = textMuted
            )

            DataLine(
                label = "المسافة المقطوعة",
                value = "${String.format("%.2f", totalDistance)} km",
                textDark = textDark,
                textMuted = textMuted
            )

            Button(
                onClick = onRefreshLocation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 15.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تحديث الموقع",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DashboardErrorCard(
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "تعذر تحميل بيانات اللوحة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2430)
            )

            Text(
                text = "تحقق من الإنترنت أو من استجابة Google Apps Script ثم أعد المحاولة",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6E7582)
            )

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("إعادة المحاولة")
            }
        }
    }
}

@Composable
private fun DataLine(
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