package com.driver.portal

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driver.portal.network.TripFormScreen
import com.driver.portal.network.FactoryFormScreen
import com.driver.portal.ui.theme.DriverPortalTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

class MainActivity : ComponentActivity() {

    private val PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkGPS()
        checkPermissions()

        if (DriverSession.isLoggedIn(this)) {
            startLocationService(
                DriverSession.getDriverName(this),
                DriverSession.getCarNumber(this)
            )
        }

        setContent {
            DriverPortalTheme {
                val context = LocalContext.current

                var driverName by remember {
                    mutableStateOf(
                        if (DriverSession.isLoggedIn(context))
                            DriverSession.getDriverName(context)
                        else null
                    )
                }

                var selectedTab by remember { mutableStateOf(0) }
                var currentLat  by remember { mutableStateOf(33.2348) }
                var currentLng  by remember { mutableStateOf(44.5302) }

                if (driverName == null) {
                    DriverSetupScreen(
                        onSave = {
                            driverName = DriverSession.getDriverName(context)
                            startLocationService(
                                DriverSession.getDriverName(context),
                                DriverSession.getCarNumber(context)
                            )
                        }
                    )
                } else {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            DriverBottomBar(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            when (selectedTab) {
                                0 -> TripFormScreen()
                                1 -> FactoryFormScreen()
                                2 -> DriverMapScreen(
                                    driverName = DriverSession.getDriverName(context),
                                    carNumber  = DriverSession.getCarNumber(context)
                                )
                                3 -> DriverDashboardScreen(
                                    driverName = DriverSession.getDriverName(context),
                                    onLogout = {
                                        DriverSession.logout(context)
                                        stopService(Intent(context, LocationForegroundService::class.java))
                                        driverName = null
                                    },
                                    onOpenMap = { lat, lng ->
                                        currentLat = lat
                                        currentLng = lng
                                        selectedTab = 2
                                    }
                                )
                                4 -> MoreScreen(
                                    onOpenHistory       = { selectedTab = 5 },
                                    onOpenMaintenance   = { selectedTab = 6 },
                                    onOpenWallet        = { selectedTab = 7 },
                                    onOpenProfile       = { selectedTab = 8 },
                                    onOpenReports       = { selectedTab = 9 },
                                    onOpenCommunication = { selectedTab = 10 }
                                )
                                5 -> TripsHistoryScreen(
                                    driverName = DriverSession.getDriverName(context)
                                )
                                6 -> MaintenanceScreen()
                                7 -> WalletScreen()
                                8 -> ProfileScreen(
                                    onLogout = {
                                        DriverSession.logout(context)
                                        stopService(Intent(context, LocationForegroundService::class.java))
                                        driverName = null
                                    }
                                )
                                9 -> ReportsScreen()
                                10 -> DriverCommunicationScreen(
                                    driverName = DriverSession.getDriverName(context)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkGPS() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsEnabled) {
            AlertDialog.Builder(this)
                .setTitle("تشغيل الموقع")
                .setMessage("يجب تشغيل GPS حتى يعمل التتبع")
                .setPositiveButton("تشغيل") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun checkPermissions() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                PERMISSION_CODE
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationService(driverName: String, carNumber: String) {
        val intent = Intent(this, LocationForegroundService::class.java)
        intent.putExtra("driverName", driverName)
        intent.putExtra("carNumber",  carNumber)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

// ══════════════════════════════════════════════
// القائمة السفلية الاحترافية
// ══════════════════════════════════════════════

data class BottomNavItem(
    val label: String,
    val icon:  ImageVector,
    val tab:   Int
)

@Composable
fun DriverBottomBar(
    selectedTab:   Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        BottomNavItem("وصل",     Icons.Default.Description,  0),
        BottomNavItem("المعمل",  Icons.Default.Factory,      1),
        BottomNavItem("الخريطة", Icons.Default.LocationOn,   2),
        BottomNavItem("اللوحة",  Icons.Default.Dashboard,    3),
        BottomNavItem("المزيد",  Icons.Default.Menu,         4)
    )

    val primary = MaterialTheme.colorScheme.primary
    val primaryDark = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(primary, primaryDark)
                    )
                )
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedTab == item.tab
                val bgColor = if (isSelected)
                    Color.White.copy(alpha = 0.22f)
                else
                    Color.Transparent

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .noRippleClickable { onTabSelected(item.tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(if (isSelected) 26.dp else 22.dp)
                    )

                    Text(
                        text       = item.label,
                        color      = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                        fontSize   = if (isSelected) 12.sp else 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    // مؤشر النشاط
                    Box(
                        modifier = Modifier
                            .size(width = 20.dp, height = 3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isSelected) Color.White
                                else Color.Transparent
                            )
                    )
                }
            }
        }
    }
}

// extension لإزالة تأثير الموجة مع الحفاظ على النقر
fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = this.then(
    Modifier.clickable(
        indication        = null,
        interactionSource = MutableInteractionSource(),
        onClick           = onClick
    )
)