package com.driver.portal

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.rememberCoroutineScope
import com.driver.portal.ui.theme.DriverPortalTheme
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class GasMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DriverPortalTheme {
                GasApp()
            }
        }
    }
}

private data class GasVehicle(
    val id: String,
    val plateNumber: String,
    val driverName: String,
    val active: Boolean = true,
)

data class GasTransaction(
    val id: String,
    val receiptNumber: String,
    val fillDate: String,
    val fuelType: String,
    val plateNumber: String,
    val driverName: String,
    val liters: Double,
    val pricePerLiter: Double,
    val totalAmount: Double,
    val notes: String,
    val enteredBy: String,
    val createdAt: Long,
)

data class GasSettlement(
    val id: String,
    val amount: Double,
    val notes: String,
    val createdAt: Long,
    val createdBy: String,
)

private data class GasManager(
    val stationName: String,
    val managerName: String,
    val pin: String,
)

private data class GasSummary(
    val totalTransactions: Int = 0,
    val totalLiters: Double = 0.0,
    val totalGasAmount: Double = 0.0,
    val totalSettlements: Double = 0.0,
    val balanceDue: Double = 0.0,
)

private object GasPrefs {
    private const val PREFS = "gas_station_prefs"
    private const val KEY_MANAGER = "manager"
    private const val KEY_AUTH = "authenticated"
    private const val KEY_PRICE = "default_price"
    private const val KEY_PRICE_COMMERCIAL = "default_price_commercial"
    private const val KEY_TRANSACTIONS = "transactions"
    private const val KEY_SETTLEMENTS = "settlements"
    private const val KEY_VEHICLES = "vehicles"

    fun hasManager(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS, 0).contains(KEY_MANAGER)

    fun isAuthenticated(context: android.content.Context): Boolean =
        context.getSharedPreferences(PREFS, 0).getBoolean(KEY_AUTH, false)

    fun setAuthenticated(context: android.content.Context, value: Boolean) {
        context.getSharedPreferences(PREFS, 0).edit().putBoolean(KEY_AUTH, value).apply()
    }

    fun saveManager(context: android.content.Context, manager: GasManager) {
        val json = JSONObject()
            .put("stationName", manager.stationName)
            .put("managerName", manager.managerName)
            .put("pin", manager.pin)
            .toString()
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY_MANAGER, json).apply()
    }

    fun loadManager(context: android.content.Context): GasManager? {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY_MANAGER, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            GasManager(
                stationName = json.optString("stationName"),
                managerName = json.optString("managerName"),
                pin = json.optString("pin")
            )
        }.getOrNull()
    }

    fun saveDefaultPrice(context: android.content.Context, price: Double) {
        context.getSharedPreferences(PREFS, 0).edit().putFloat(KEY_PRICE, price.toFloat()).apply()
    }

    fun loadDefaultPrice(context: android.content.Context): Double =
        context.getSharedPreferences(PREFS, 0).getFloat(KEY_PRICE, 430f).toDouble()

    fun saveCommercialPrice(context: android.content.Context, price: Double) {
        context.getSharedPreferences(PREFS, 0).edit().putFloat(KEY_PRICE_COMMERCIAL, price.toFloat()).apply()
    }

    fun loadCommercialPrice(context: android.content.Context): Double =
        context.getSharedPreferences(PREFS, 0).getFloat(KEY_PRICE_COMMERCIAL, 430f).toDouble()

    fun loadVehicles(context: android.content.Context): List<GasVehicle> {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val raw = prefs.getString(KEY_VEHICLES, null)
        if (raw.isNullOrBlank()) {
            val seeded = listOf(
                GasVehicle(id = UUID.randomUUID().toString(), plateNumber = "0001", driverName = "سائق 1"),
                GasVehicle(id = UUID.randomUUID().toString(), plateNumber = "0002", driverName = "سائق 2"),
                GasVehicle(id = UUID.randomUUID().toString(), plateNumber = "0003", driverName = "سائق 3"),
            )
            saveVehicles(context, seeded)
            return seeded
        }
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        GasVehicle(
                            id = item.optString("id"),
                            plateNumber = item.optString("plateNumber"),
                            driverName = item.optString("driverName"),
                            active = item.optBoolean("active", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveVehicles(context: android.content.Context, vehicles: List<GasVehicle>) {
        val deduped = vehicles
            .distinctBy { vehicleIdentity(it.plateNumber, it.driverName) }
            .mapIndexed { index, vehicle ->
                if (vehicle.id.isBlank()) vehicle.copy(id = UUID.randomUUID().toString()) else vehicle
            }
        val array = JSONArray()
        deduped.forEach { vehicle ->
            array.put(
                JSONObject()
                    .put("id", vehicle.id)
                    .put("plateNumber", vehicle.plateNumber)
                    .put("driverName", vehicle.driverName)
                    .put("active", vehicle.active)
            )
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY_VEHICLES, array.toString()).apply()
    }

    fun loadTransactions(context: android.content.Context): List<GasTransaction> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        GasTransaction(
                            id = item.optString("id"),
                            receiptNumber = item.optString("receiptNumber", item.optString("id")),
                            fillDate = item.optString("fillDate", ""),
                            fuelType = item.optString("fuelType", "اعتيادي"),
                            plateNumber = item.optString("plateNumber"),
                            driverName = item.optString("driverName"),
                            liters = item.optDouble("liters"),
                            pricePerLiter = item.optDouble("pricePerLiter"),
                            totalAmount = item.optDouble("totalAmount"),
                            notes = item.optString("notes"),
                            enteredBy = item.optString("enteredBy"),
                            createdAt = item.optLong("createdAt")
                        )
                    )
                }
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    fun saveTransactions(context: android.content.Context, transactions: List<GasTransaction>) {
        val deduped = transactions.distinctBy { transactionIdentity(it.receiptNumber, it.id) }
        val array = JSONArray()
        deduped.forEach { tx ->
            array.put(
                JSONObject()
                    .put("id", tx.id)
                    .put("receiptNumber", tx.receiptNumber)
                    .put("fillDate", tx.fillDate)
                    .put("fuelType", tx.fuelType)
                    .put("plateNumber", tx.plateNumber)
                    .put("driverName", tx.driverName)
                    .put("liters", tx.liters)
                    .put("pricePerLiter", tx.pricePerLiter)
                    .put("totalAmount", tx.totalAmount)
                    .put("notes", tx.notes)
                    .put("enteredBy", tx.enteredBy)
                    .put("createdAt", tx.createdAt)
            )
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
    }

    fun loadSettlements(context: android.content.Context): List<GasSettlement> {
        val raw = context.getSharedPreferences(PREFS, 0).getString(KEY_SETTLEMENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        GasSettlement(
                            id = item.optString("id"),
                            amount = item.optDouble("amount"),
                            notes = item.optString("notes"),
                            createdAt = item.optLong("createdAt"),
                            createdBy = item.optString("createdBy")
                        )
                    )
                }
            }.sortedByDescending { it.createdAt }
        }.getOrDefault(emptyList())
    }

    fun saveSettlements(context: android.content.Context, settlements: List<GasSettlement>) {
        val array = JSONArray()
        settlements.forEach { settlement ->
            array.put(
                JSONObject()
                    .put("id", settlement.id)
                    .put("amount", settlement.amount)
                    .put("notes", settlement.notes)
                    .put("createdAt", settlement.createdAt)
                    .put("createdBy", settlement.createdBy)
            )
        }
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY_SETTLEMENTS, array.toString()).apply()
    }
}

private enum class GasTab(val title: String) {
    Transactions("التعبئة"),
    Ledger("الحركات"),
    Settlements("التسديدات"),
    Reports("التقارير"),
    Settings("الإعدادات")
}

@Composable
private fun GasApp() {
    val context = LocalContext.current
    var hasManager by remember { mutableStateOf(GasPrefs.hasManager(context)) }
    var authenticated by remember { mutableStateOf(GasPrefs.isAuthenticated(context)) }

    if (!hasManager) {
        GasSetupScreen(onSaved = {
            hasManager = true
            authenticated = true
        })
        return
    }

    if (!authenticated) {
        GasLoginScreen(onLoggedIn = { authenticated = true })
        return
    }

    GasDashboard(onLogout = {
        GasPrefs.setAuthenticated(context, false)
        authenticated = false
    })
}

@Composable
private fun GasSetupScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    var stationName by remember { mutableStateOf("محطة الكاز") }
    var managerName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    GasScreenBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("تهيئة تطبيق الكاز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text("نسخة مستقلة وآمنة لإدارة تعبئة الكاز والتسديدات", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            GasCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = stationName, onValueChange = { stationName = it }, label = { Text("اسم المحطة") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = gasTextFieldColors())
                    OutlinedTextField(value = managerName, onValueChange = { managerName = it }, label = { Text("اسم المسؤول") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = gasTextFieldColors())
                    OutlinedTextField(value = pin, onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) }, label = { Text("رمز الدخول") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), colors = gasTextFieldColors())
                    Button(
                        onClick = {
                            if (stationName.isBlank() || managerName.isBlank() || pin.length < 4) {
                                Toast.makeText(context, "أكمل بيانات التهيئة", Toast.LENGTH_SHORT).show()
                            } else {
                                GasPrefs.saveManager(context, GasManager(stationName.trim(), managerName.trim(), pin))
                                GasPrefs.setAuthenticated(context, true)
                                GasPrefs.saveDefaultPrice(context, 430.0)
                                GasPrefs.saveCommercialPrice(context, 430.0)
                                onSaved()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GasPrimary)
                    ) {
                        Icon(Icons.Default.LocalGasStation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("بدء نظام الكاز")
                    }
                }
            }
        }
    }
}

@Composable
private fun GasLoginScreen(onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { GasPrefs.loadManager(context) }
    var pin by remember { mutableStateOf("") }

    GasScreenBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(manager?.stationName ?: "تطبيق الكاز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text("دخول مسؤول المحطة", color = Color.White.copy(alpha = 0.82f), fontSize = 13.sp)
            GasCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = pin, onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) }, label = { Text("الرمز") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), colors = gasTextFieldColors())
                    Button(
                        onClick = {
                            if (manager != null && pin == manager.pin) {
                                GasPrefs.setAuthenticated(context, true)
                                onLoggedIn()
                            } else {
                                Toast.makeText(context, "رمز غير صحيح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GasPrimary)
                    ) {
                        Text("دخول")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GasDashboard(onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(GasTab.Transactions) }
    var transactions by remember { mutableStateOf(GasPrefs.loadTransactions(context)) }
    var settlements by remember { mutableStateOf(GasPrefs.loadSettlements(context)) }
    var vehicles by remember { mutableStateOf(GasPrefs.loadVehicles(context)) }
    var defaultPrice by remember { mutableStateOf(GasPrefs.loadDefaultPrice(context)) }
    var commercialPrice by remember { mutableStateOf(GasPrefs.loadCommercialPrice(context)) }
    val manager = remember { GasPrefs.loadManager(context) }

    fun refreshAll() {
        transactions = GasPrefs.loadTransactions(context)
        settlements = GasPrefs.loadSettlements(context)
        vehicles = GasPrefs.loadVehicles(context)
        defaultPrice = GasPrefs.loadDefaultPrice(context)
        commercialPrice = GasPrefs.loadCommercialPrice(context)
    }

    GasScreenBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(manager?.stationName ?: "تطبيق الكاز", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("منظومة مستقلة للمحطة بدون المساس بنظام الشركة", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GasTabRow(selectedTab = selectedTab, onSelected = { selectedTab = it })
                when (selectedTab) {
                    GasTab.Transactions -> GasTransactionScreen(
                        stationName = manager?.stationName.orEmpty(),
                        managerName = manager?.managerName.orEmpty(),
                        transactions = transactions,
                        vehicles = vehicles,
                        defaultPrice = defaultPrice,
                        commercialPrice = commercialPrice,
                        onSaveTransaction = { tx ->
                            val candidateIdentity = vehicleIdentity(tx.plateNumber, tx.driverName)
                            if (vehicles.none { vehicleIdentity(it.plateNumber, it.driverName) == candidateIdentity }) {
                                val updatedVehicles = vehicles + GasVehicle(UUID.randomUUID().toString(), tx.plateNumber, tx.driverName)
                                GasPrefs.saveVehicles(context, updatedVehicles)
                            }
                            GasPrefs.saveTransactions(context, listOf(tx) + transactions)
                            refreshAll()
                            Toast.makeText(context, "تم حفظ تعبئة الكاز", Toast.LENGTH_SHORT).show()
                        }
                    )
                    GasTab.Ledger -> GasLedgerScreen(
                        stationName = manager?.stationName.orEmpty(),
                        managerName = manager?.managerName.orEmpty(),
                        transactions = transactions,
                        onUpdateTransaction = { previous, updated ->
                            val previousKey = transactionIdentity(previous.receiptNumber, previous.id)
                            val updatedList = transactions.map { tx ->
                                if (transactionIdentity(tx.receiptNumber, tx.id) == previousKey) updated else tx
                            }.sortedByDescending { it.createdAt }
                            val updatedVehicleIdentity = vehicleIdentity(updated.plateNumber, updated.driverName)
                            if (vehicles.none { vehicleIdentity(it.plateNumber, it.driverName) == updatedVehicleIdentity }) {
                                GasPrefs.saveVehicles(context, vehicles + GasVehicle(UUID.randomUUID().toString(), updated.plateNumber, updated.driverName))
                            }
                            GasPrefs.saveTransactions(context, updatedList)
                            refreshAll()
                        }
                    )
                    GasTab.Settlements -> GasSettlementScreen(
                        stationName = manager?.stationName.orEmpty(),
                        managerName = manager?.managerName.orEmpty(),
                        settlements = settlements,
                        onSaveSettlement = { settlement ->
                            GasPrefs.saveSettlements(context, listOf(settlement) + settlements)
                            refreshAll()
                            Toast.makeText(context, "تم تسجيل التسديد", Toast.LENGTH_SHORT).show()
                        }
                    )
                    GasTab.Reports -> GasReportsScreen(
                        stationName = manager?.stationName.orEmpty(),
                        transactions = transactions,
                        settlements = settlements,
                        vehicles = vehicles
                    )
                    GasTab.Settings -> GasSettingsScreen(
                        manager = manager,
                        defaultPrice = defaultPrice,
                        commercialPrice = commercialPrice,
                        vehicles = vehicles,
                        onSavePrices = { normal, commercial ->
                            GasPrefs.saveDefaultPrice(context, normal)
                            GasPrefs.saveCommercialPrice(context, commercial)
                            refreshAll()
                        },
                        onDeleteVehicle = { vehicleId ->
                            GasPrefs.saveVehicles(context, vehicles.filterNot { it.id == vehicleId })
                            refreshAll()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GasTabRow(selectedTab: GasTab, onSelected: (GasTab) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GasTab.entries.forEach { tab ->
            val active = tab == selectedTab
            OutlinedButton(
                onClick = { onSelected(tab) },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active) GasPrimary.copy(alpha = 0.16f) else GasPanel.copy(alpha = 0.75f),
                    contentColor = if (active) Color.White else Color.White.copy(alpha = 0.86f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (active) GasPrimary else Color.White.copy(alpha = 0.12f))
            ) {
                Text(tab.title)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GasTransactionScreen(
    stationName: String,
    managerName: String,
    transactions: List<GasTransaction>,
    vehicles: List<GasVehicle>,
    defaultPrice: Double,
    commercialPrice: Double,
    onSaveTransaction: (GasTransaction) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expandedVehicle by remember { mutableStateOf(false) }
    var selectedVehicleId by remember { mutableStateOf(vehicles.firstOrNull()?.id.orEmpty()) }
    var receiptNumber by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf(vehicles.firstOrNull()?.driverName.orEmpty()) }
    var plateNumber by remember { mutableStateOf(vehicles.firstOrNull()?.plateNumber.orEmpty()) }
    var fillDate by remember { mutableStateOf(formatDateOnly(System.currentTimeMillis())) }
    var isCommercialFuel by remember { mutableStateOf(false) }
    var litersText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf(formatPlainNumber(defaultPrice)) }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(isCommercialFuel, defaultPrice, commercialPrice) {
        priceText = formatPlainNumber(if (isCommercialFuel) commercialPrice else defaultPrice)
    }

    val liters = litersText.toDoubleOrNull() ?: 0.0
    val price = priceText.toDoubleOrNull() ?: defaultPrice
    val total = liters * price

    LaunchedEffect(selectedVehicleId, vehicles) {
        val vehicle = vehicles.firstOrNull { it.id == selectedVehicleId }
        if (vehicle != null) {
            plateNumber = vehicle.plateNumber
            driverName = vehicle.driverName
        }
    }

    GasCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("تعبئة جديدة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("رقم الوصل هو المرجع الأساسي لمنع التكرار ومراجعة حساب المحطة", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
            Text("رقم السيارة مسموح يتكرر مع سائق مختلف", color = Color.White.copy(alpha = 0.66f), fontSize = 11.sp)
            OutlinedTextField(value = receiptNumber, onValueChange = { receiptNumber = sanitizeReceiptNumber(it) }, label = { Text("رقم الوصل") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = gasTextFieldColors())
            OutlinedTextField(value = driverName, onValueChange = { driverName = it }, label = { Text("اسم السائق") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = gasTextFieldColors())
            OutlinedTextField(value = plateNumber, onValueChange = { plateNumber = it }, label = { Text("رقم السيارة") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = gasTextFieldColors())
            OutlinedTextField(value = fillDate, onValueChange = { fillDate = sanitizeDateInput(it) }, label = { Text("تاريخ الوصل (YYYY/MM/DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = gasTextFieldColors())
            if (vehicles.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = expandedVehicle, onExpandedChange = { expandedVehicle = !expandedVehicle }) {
                    OutlinedTextField(
                        value = vehicles.firstOrNull { it.id == selectedVehicleId }?.let { "${it.plateNumber} - ${it.driverName}" }.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اختيار سريع من السيارات المحفوظة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicle) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = gasTextFieldColors()
                    )
                    DropdownMenu(expanded = expandedVehicle, onDismissRequest = { expandedVehicle = false }) {
                        vehicles.forEach { vehicle ->
                            DropdownMenuItem(
                                text = { Text("${vehicle.plateNumber} - ${vehicle.driverName}") },
                                onClick = {
                                    selectedVehicleId = vehicle.id
                                    expandedVehicle = false
                                }
                            )
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { isCommercialFuel = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (!isCommercialFuel) GasPrimary.copy(alpha = 0.18f) else Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (!isCommercialFuel) GasPrimary else Color.White.copy(alpha = 0.35f))
                ) {
                    Text("كاز اعتيادي")
                }
                OutlinedButton(
                    onClick = { isCommercialFuel = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isCommercialFuel) GasWarning.copy(alpha = 0.18f) else Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isCommercialFuel) GasWarning else Color.White.copy(alpha = 0.35f))
                ) {
                    Text("كاز تجاري")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = litersText, onValueChange = { litersText = sanitizeDecimal(it) }, label = { Text("اللترات") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = gasTextFieldColors())
                OutlinedTextField(value = priceText, onValueChange = { priceText = sanitizeDecimal(it) }, label = { Text("سعر اللتر") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = gasTextFieldColors())
            }
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth(), colors = gasTextFieldColors())
            GasSummaryMetric(title = "المبلغ الكلي", value = formatAmountReadable(total), subtitle = "${if (isCommercialFuel) "تجاري" else "اعتيادي"} | ${formatReadableNumber(liters, 0)} × ${formatAmountReadable(price)}")
            Button(
                onClick = {
                    if (isSaving) return@Button
                    val normalizedReceipt = sanitizeReceiptNumber(receiptNumber)
                    val normalizedPlate = plateNumber.trim()
                    val normalizedDriver = driverName.trim()
                    val duplicateReceipt = transactions.any { transactionIdentity(it.receiptNumber, it.id) == normalizedReceipt.lowercase(Locale.ROOT) }
                    if (normalizedReceipt.isBlank() || normalizedDriver.isBlank() || normalizedPlate.isBlank() || fillDate.length < 8 || liters <= 0.0 || price <= 0.0) {
                        Toast.makeText(context, "أكمل بيانات التعبئة", Toast.LENGTH_SHORT).show()
                    } else if (normalizedReceipt.length < 3) {
                        Toast.makeText(context, "رقم الوصل يجب أن يكون أوضح (3 خانات على الأقل)", Toast.LENGTH_SHORT).show()
                    } else if (duplicateReceipt) {
                        Toast.makeText(context, "رقم الوصل محفوظ مسبقًا", Toast.LENGTH_SHORT).show()
                    } else {
                        val transaction = GasTransaction(
                            id = normalizedReceipt,
                            receiptNumber = normalizedReceipt,
                            fillDate = fillDate,
                            fuelType = if (isCommercialFuel) "تجاري" else "اعتيادي",
                            plateNumber = normalizedPlate,
                            driverName = normalizedDriver,
                            liters = liters,
                            pricePerLiter = price,
                            totalAmount = total,
                            notes = notes.trim(),
                            enteredBy = managerName,
                            createdAt = System.currentTimeMillis()
                        )
                        isSaving = true
                        scope.launch {
                            val remote = GasRemoteRepository.addTransaction(transaction, stationName)
                            remote.onSuccess {
                                onSaveTransaction(transaction)
                                receiptNumber = ""
                                selectedVehicleId = ""
                                driverName = ""
                                plateNumber = ""
                                fillDate = formatDateOnly(System.currentTimeMillis())
                                isCommercialFuel = false
                                litersText = ""
                                priceText = formatPlainNumber(defaultPrice)
                                notes = ""
                                Toast.makeText(context, "تم حفظ جميع البيانات وإرسالها إلى Google Sheet", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "فشل الإرسال إلى Google Sheet", Toast.LENGTH_LONG).show()
                            }
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GasPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSaving) "جارٍ الحفظ..." else "حفظ جميع البيانات")
            }
        }
    }
}

@Composable
private fun GasLedgerScreen(
    stationName: String,
    managerName: String,
    transactions: List<GasTransaction>,
    onUpdateTransaction: (GasTransaction, GasTransaction) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editingTransaction by remember { mutableStateOf<GasTransaction?>(null) }
    var isUpdating by remember { mutableStateOf(false) }

    GasCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("آخر الحركات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("يمكن لمسؤول المحطة تعديل أي وصل محفوظ ثم حفظه مباشرة", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            if (transactions.isEmpty()) {
                Text("لا توجد تعبئات محفوظة بعد", color = Color.White.copy(alpha = 0.72f))
            } else {
                transactions.take(20).forEach { tx ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GasListRow(
                            title = "وصل ${tx.receiptNumber.ifBlank { tx.id }} | ${tx.driverName.ifBlank { "بدون اسم" }}",
                            value = "${tx.plateNumber} | ${formatReadableNumber(tx.liters, 0)} لتر | ${formatAmountReadable(tx.pricePerLiter)} سعر",
                            subtitle = "${tx.fillDate} | ${tx.fuelType} | ${formatAmountReadable(tx.totalAmount)} د.ع"
                        )
                        OutlinedButton(
                            onClick = { editingTransaction = tx },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تعديل الوصل")
                        }
                    }
                }
            }
        }
    }

    editingTransaction?.let { target ->
        var receiptNumber by remember(target) { mutableStateOf(target.receiptNumber) }
        var driverName by remember(target) { mutableStateOf(target.driverName) }
        var plateNumber by remember(target) { mutableStateOf(target.plateNumber) }
        var fillDate by remember(target) { mutableStateOf(target.fillDate) }
        var litersText by remember(target) { mutableStateOf(formatPlainNumber(target.liters)) }
        var priceText by remember(target) { mutableStateOf(formatPlainNumber(target.pricePerLiter)) }
        var notes by remember(target) { mutableStateOf(target.notes) }
        var isCommercialFuel by remember(target) { mutableStateOf(target.fuelType.contains("تجاري")) }

        AlertDialog(
            onDismissRequest = {
                if (!isUpdating) editingTransaction = null
            },
            title = { Text("تعديل الوصل") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = receiptNumber, onValueChange = { receiptNumber = sanitizeReceiptNumber(it) }, label = { Text("رقم الوصل") }, singleLine = true, colors = gasTextFieldColors())
                    OutlinedTextField(value = driverName, onValueChange = { driverName = it }, label = { Text("اسم السائق") }, singleLine = true, colors = gasTextFieldColors())
                    OutlinedTextField(value = plateNumber, onValueChange = { plateNumber = it }, label = { Text("رقم السيارة") }, singleLine = true, colors = gasTextFieldColors())
                    OutlinedTextField(value = fillDate, onValueChange = { fillDate = sanitizeDateInput(it) }, label = { Text("تاريخ الوصل (YYYY/MM/DD)") }, singleLine = true, colors = gasTextFieldColors())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isCommercialFuel = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isCommercialFuel) GasPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (!isCommercialFuel) GasPrimary else Color.White.copy(alpha = 0.35f))
                        ) {
                            Text("اعتيادي")
                        }
                        OutlinedButton(
                            onClick = { isCommercialFuel = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isCommercialFuel) GasWarning.copy(alpha = 0.18f) else Color.Transparent,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isCommercialFuel) GasWarning else Color.White.copy(alpha = 0.35f))
                        ) {
                            Text("تجاري")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = litersText,
                            onValueChange = { litersText = sanitizeDecimal(it) },
                            label = { Text("اللترات") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = gasTextFieldColors()
                        )
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = sanitizeDecimal(it) },
                            label = { Text("سعر اللتر") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = gasTextFieldColors()
                        )
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, colors = gasTextFieldColors())
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isUpdating) return@TextButton
                        val normalizedReceipt = sanitizeReceiptNumber(receiptNumber)
                        val normalizedDriver = driverName.trim()
                        val normalizedPlate = plateNumber.trim()
                        val liters = litersText.toDoubleOrNull() ?: 0.0
                        val price = priceText.toDoubleOrNull() ?: 0.0
                        val duplicate = transactions.any {
                            it.createdAt != target.createdAt &&
                                transactionIdentity(it.receiptNumber, it.id) == normalizedReceipt.lowercase(Locale.ROOT)
                        }

                        if (stationName.isBlank()) {
                            Toast.makeText(context, "تعذر تحديد اسم المحطة", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (normalizedReceipt.isBlank() || normalizedDriver.isBlank() || normalizedPlate.isBlank() || fillDate.length < 8 || liters <= 0.0 || price <= 0.0) {
                            Toast.makeText(context, "أكمل بيانات الوصل", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (normalizedReceipt.length < 3) {
                            Toast.makeText(context, "رقم الوصل يجب أن يكون أوضح (3 خانات على الأقل)", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (duplicate) {
                            Toast.makeText(context, "رقم الوصل مستخدم في وصل آخر", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        val updated = target.copy(
                            id = normalizedReceipt,
                            receiptNumber = normalizedReceipt,
                            fillDate = fillDate,
                            fuelType = if (isCommercialFuel) "تجاري" else "اعتيادي",
                            plateNumber = normalizedPlate,
                            driverName = normalizedDriver,
                            liters = liters,
                            pricePerLiter = price,
                            totalAmount = liters * price,
                            notes = notes.trim(),
                            enteredBy = managerName
                        )

                        isUpdating = true
                        scope.launch {
                            GasRemoteRepository.updateTransaction(
                                transaction = updated,
                                stationName = stationName,
                                originalReceiptNumber = target.receiptNumber
                            ).onSuccess {
                                onUpdateTransaction(target, updated)
                                editingTransaction = null
                                Toast.makeText(context, "تم تعديل الوصل بنجاح", Toast.LENGTH_SHORT).show()
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "فشل تعديل الوصل", Toast.LENGTH_LONG).show()
                            }
                            isUpdating = false
                        }
                    }
                ) {
                    Text(if (isUpdating) "جارٍ الحفظ..." else "حفظ التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (!isUpdating) editingTransaction = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun GasSettlementScreen(
    stationName: String,
    managerName: String,
    settlements: List<GasSettlement>,
    onSaveSettlement: (GasSettlement) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GasCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تسديد جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedTextField(value = amountText, onValueChange = { amountText = sanitizeDecimal(it) }, label = { Text("المبلغ") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = gasTextFieldColors())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth(), colors = gasTextFieldColors())
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (amount <= 0.0) {
                            Toast.makeText(context, "أدخل مبلغًا صحيحًا", Toast.LENGTH_SHORT).show()
                        } else {
                            val settlement = GasSettlement(
                                    id = UUID.randomUUID().toString(),
                                    amount = amount,
                                    notes = notes.trim(),
                                    createdAt = System.currentTimeMillis(),
                                    createdBy = managerName
                                )
                            isSaving = true
                            scope.launch {
                                val remote = GasRemoteRepository.addSettlement(settlement, stationName)
                                remote.onSuccess {
                                    onSaveSettlement(settlement)
                                    amountText = ""
                                    notes = ""
                                    Toast.makeText(context, "تم إرسال التسديد إلى Google Sheet", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "فشل إرسال التسديد إلى Google Sheet", Toast.LENGTH_LONG).show()
                                }
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GasAccent)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "جارٍ الإرسال..." else "تسجيل التسديد")
                }
            }
        }

        GasCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("آخر التسديدات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (settlements.isEmpty()) {
                    Text("لا توجد تسديدات محفوظة", color = Color.White.copy(alpha = 0.72f))
                } else {
                    settlements.take(20).forEach { settlement ->
                        GasListRow(
                            title = formatAmountReadable(settlement.amount),
                            value = settlement.notes.ifBlank { "بدون ملاحظات" },
                            subtitle = "${formatGasDate(settlement.createdAt)} | ${settlement.createdBy.ifBlank { "-" }}"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GasReportsScreen(
    stationName: String,
    transactions: List<GasTransaction>,
    settlements: List<GasSettlement>,
    vehicles: List<GasVehicle>,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var remoteTransactions by remember { mutableStateOf<List<GasTransaction>?>(null) }
    var remoteSettlements by remember { mutableStateOf<List<GasSettlement>?>(null) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }
    var selectedHalf by remember { mutableStateOf(GasReportHalf.AllMonth) }

    fun syncReports() {
        if (isSyncing || stationName.isBlank()) return
        isSyncing = true
        syncMessage = null
        scope.launch {
            GasRemoteRepository.fetchReportData(stationName).onSuccess { remote ->
                remoteTransactions = remote.transactions
                remoteSettlements = remote.settlements
                GasPrefs.saveTransactions(context, remote.transactions)
                GasPrefs.saveSettlements(context, remote.settlements)
                syncMessage = "تم تحديث التقارير من Google Sheet"
            }.onFailure {
                syncMessage = it.message ?: "تعذر جلب بيانات التقارير من Google Sheet"
            }
            isSyncing = false
        }
    }

    LaunchedEffect(stationName) {
        syncReports()
    }

    val reportTransactions = remoteTransactions ?: transactions
    val reportSettlements = remoteSettlements ?: settlements

    val monthKeys = remember(reportTransactions, reportSettlements) {
        buildList {
            add("الكل")
            val txMonths = reportTransactions.mapNotNull { monthKeyFromFillDate(it.fillDate) }
            val settlementMonths = reportSettlements.map { monthKeyFromMillis(it.createdAt) }
            addAll((txMonths + settlementMonths).distinct().sortedDescending())
        }
    }
    var selectedMonth by remember(monthKeys) { mutableStateOf(monthKeys.firstOrNull() ?: "الكل") }

    LaunchedEffect(monthKeys) {
        if (selectedMonth !in monthKeys) selectedMonth = monthKeys.firstOrNull() ?: "الكل"
    }

    val monthFilteredTransactions = remember(reportTransactions, selectedMonth) {
        if (selectedMonth == "الكل") reportTransactions
        else reportTransactions.filter { monthKeyFromFillDate(it.fillDate) == selectedMonth }
    }
    val monthFilteredSettlements = remember(reportSettlements, selectedMonth) {
        if (selectedMonth == "الكل") reportSettlements
        else reportSettlements.filter { monthKeyFromMillis(it.createdAt) == selectedMonth }
    }

    val filteredTransactions = remember(monthFilteredTransactions, selectedHalf) {
        monthFilteredTransactions.filter { selectedHalf.matchesDate(it.fillDate) }
    }
    val filteredSettlements = remember(monthFilteredSettlements, selectedHalf) {
        monthFilteredSettlements.filter { selectedHalf.matchesMillis(it.createdAt) }
    }

    val summary = remember(filteredTransactions, filteredSettlements) {
        computeGasSummary(filteredTransactions, filteredSettlements)
    }
    val normalTransactions = remember(filteredTransactions) {
        filteredTransactions.filter { it.fuelType.trim().contains("اعتيادي") }
    }
    val commercialTransactions = remember(filteredTransactions) {
        filteredTransactions.filter { it.fuelType.trim().contains("تجاري") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GasCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الملخص المحاسبي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Button(
                    onClick = { syncReports() },
                    enabled = !isSyncing && stationName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GasAccent)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSyncing) "جارٍ تحديث التقارير من Google Sheet..." else "تحديث التقارير من Google Sheet")
                }
                OutlinedButton(
                    onClick = { syncReports() },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحديث الصفحة")
                }
                syncMessage?.let {
                    Text(it, color = if (it.startsWith("تم")) GasSuccess else GasWarning, fontSize = 12.sp)
                }
                Text("شهر التقرير", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    monthKeys.forEach { month ->
                        val selected = month == selectedMonth
                        OutlinedButton(
                            onClick = { selectedMonth = month },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) GasPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) GasPrimary else Color.White.copy(alpha = 0.35f))
                        ) {
                            Text(month)
                        }
                    }
                }
                Text("نصف الشهر", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GasReportHalf.entries.forEach { half ->
                        val selected = half == selectedHalf
                        OutlinedButton(
                            onClick = { selectedHalf = half },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) GasPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) GasPrimary else Color.White.copy(alpha = 0.35f))
                        ) {
                            Text(half.title)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GasMetricCard("عدد التعبئات", summary.totalTransactions.toString(), GasPrimary, Modifier.weight(1f))
                    GasMetricCard(
                        "عدد السيارات",
                        filteredTransactions.distinctBy { it.plateNumber.trim().lowercase(Locale.ROOT) }.size.toString(),
                        GasAccent,
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GasMetricCard("إجمالي اللترات", formatReadableNumber(summary.totalLiters, 0), GasWarning, Modifier.weight(1f))
                    GasMetricCard("إجمالي الكاز", amountDisplayAccounting(summary.totalGasAmount).first, GasDanger, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GasMetricCard("إجمالي التسديدات", amountDisplayAccounting(summary.totalSettlements).first, GasAccent, Modifier.weight(1f))
                    GasMetricCard("الرصيد المستحق", amountDisplayAccounting(summary.balanceDue).first, GasSuccess, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GasMetricCard(
                        "اعتيادي",
                        "${formatReadableNumber(normalTransactions.sumOf { it.liters }, 0)} لتر",
                        GasPrimary,
                        Modifier.weight(1f)
                    )
                    GasMetricCard(
                        "تجاري",
                        "${formatReadableNumber(commercialTransactions.sumOf { it.liters }, 0)} لتر",
                        GasWarning,
                        Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GasMetricCard(
                        "مبلغ الاعتيادي",
                        amountDisplayAccounting(normalTransactions.sumOf { it.totalAmount }).first,
                        GasPrimary,
                        Modifier.weight(1f)
                    )
                    GasMetricCard(
                        "مبلغ التجاري",
                        amountDisplayAccounting(commercialTransactions.sumOf { it.totalAmount }).first,
                        GasWarning,
                        Modifier.weight(1f)
                    )
                }
                val monthLabel = buildString {
                    append(if (selectedMonth == "الكل") "كامل البيانات" else selectedMonth)
                    if (selectedHalf != GasReportHalf.AllMonth) {
                        append(" - ")
                        append(selectedHalf.title)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            runCatching {
                                val file = createGasMonthlyPdf(
                                    context = context,
                                    monthLabel = monthLabel,
                                    transactions = filteredTransactions,
                                    settlements = filteredSettlements,
                                    summary = summary,
                                    normalLiters = normalTransactions.sumOf { it.liters },
                                    commercialLiters = commercialTransactions.sumOf { it.liters },
                                    normalAmount = normalTransactions.sumOf { it.totalAmount },
                                    commercialAmount = commercialTransactions.sumOf { it.totalAmount }
                                )
                                ReportUtils.printPdf(context, file)
                            }.onFailure {
                                Toast.makeText(context, "تعذر إنشاء تقرير PDF", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GasAccent)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("طباعة PDF")
                    }
                    Button(
                        onClick = {
                            runCatching {
                                val file = createGasMonthlyPdf(
                                    context = context,
                                    monthLabel = monthLabel,
                                    transactions = filteredTransactions,
                                    settlements = filteredSettlements,
                                    summary = summary,
                                    normalLiters = normalTransactions.sumOf { it.liters },
                                    commercialLiters = commercialTransactions.sumOf { it.liters },
                                    normalAmount = normalTransactions.sumOf { it.totalAmount },
                                    commercialAmount = commercialTransactions.sumOf { it.totalAmount }
                                )
                                ReportUtils.sharePdf(context, file)
                            }.onFailure {
                                Toast.makeText(context, "تعذر مشاركة تقرير PDF", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GasPrimary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("مشاركة PDF")
                    }
                }
                OutlinedButton(
                    onClick = {
                        shareGasReportText(
                            context = context,
                            monthLabel = monthLabel,
                            summary = summary,
                            normalLiters = normalTransactions.sumOf { it.liters },
                            commercialLiters = commercialTransactions.sumOf { it.liters },
                            normalAmount = normalTransactions.sumOf { it.totalAmount },
                            commercialAmount = commercialTransactions.sumOf { it.totalAmount }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة ملخص التقرير")
                }
                OutlinedButton(
                    onClick = {
                        runCatching {
                            val file = createGasMonthlyXlsx(
                                context = context,
                                monthLabel = monthLabel,
                                transactions = filteredTransactions
                            )
                            shareGasFile(
                                context = context,
                                file = file,
                                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                chooserTitle = "مشاركة تقرير Excel (XLSX)"
                            )
                        }.onFailure {
                            Toast.makeText(context, "تعذر تصدير تقرير Excel", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تصدير Excel (XLSX)")
                }
            }
        }

        GasCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("كشف حسب السيارة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (filteredTransactions.isEmpty()) {
                    Text("لا توجد بيانات كافية لعرض الكشف", color = Color.White.copy(alpha = 0.72f))
                } else {
                    filteredTransactions.groupBy { it.plateNumber }.entries.sortedByDescending { (_, list) -> list.sumOf { it.totalAmount } }.forEach { (plate, list) ->
                        val liters = list.sumOf { it.liters }
                        val amount = list.sumOf { it.totalAmount }
                        GasListRow(
                            title = plate,
                            value = "${formatReadableNumber(liters, 0)} لتر | ${amountDisplayAccounting(amount).first}",
                            subtitle = "${list.size} تعبئة"
                        )
                    }
                }
            }
        }
    }
}

private enum class GasReportHalf(val title: String) {
    AllMonth("كل الشهر"),
    FirstHalf("1 - 15"),
    SecondHalf("16 - نهاية الشهر");

    fun matchesDate(fillDate: String): Boolean {
        val day = dayFromDateText(fillDate) ?: return this == AllMonth
        return matchesDay(day)
    }

    fun matchesMillis(value: Long): Boolean {
        val day = SimpleDateFormat("dd", Locale.US).format(Date(value)).toIntOrNull() ?: return this == AllMonth
        return matchesDay(day)
    }

    private fun matchesDay(day: Int): Boolean = when (this) {
        AllMonth -> true
        FirstHalf -> day in 1..15
        SecondHalf -> day in 16..31
    }
}

@Composable
private fun GasSettingsScreen(
    manager: GasManager?,
    defaultPrice: Double,
    commercialPrice: Double,
    vehicles: List<GasVehicle>,
    onSavePrices: (Double, Double) -> Unit,
    onDeleteVehicle: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var normalPriceText by remember { mutableStateOf(formatPlainNumber(defaultPrice)) }
    var commercialPriceText by remember { mutableStateOf(formatPlainNumber(commercialPrice)) }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GasCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الإعدادات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("المحطة: ${manager?.stationName ?: "-"}", color = Color.White.copy(alpha = 0.82f))
                Text("المسؤول: ${manager?.managerName ?: "-"}", color = Color.White.copy(alpha = 0.82f))
                OutlinedTextField(value = normalPriceText, onValueChange = { normalPriceText = sanitizeDecimal(it) }, label = { Text("سعر اللتر الاعتيادي") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = gasTextFieldColors())
                OutlinedTextField(value = commercialPriceText, onValueChange = { commercialPriceText = sanitizeDecimal(it) }, label = { Text("سعر اللتر التجاري") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), colors = gasTextFieldColors())
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        val normal = normalPriceText.toDoubleOrNull() ?: 0.0
                        val commercial = commercialPriceText.toDoubleOrNull() ?: 0.0
                        if (normal <= 0.0 || commercial <= 0.0) {
                            Toast.makeText(context, "أدخل الأسعار بشكل صحيح", Toast.LENGTH_SHORT).show()
                        } else {
                            isSaving = true
                            scope.launch {
                                val remote = GasRemoteRepository.updateSettings(manager?.stationName.orEmpty(), normal, commercial)
                                remote.onSuccess {
                                    onSavePrices(normal, commercial)
                                    Toast.makeText(context, "تم تحديث أسعار الكاز", Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, "فشل تحديث الأسعار على Google Sheet", Toast.LENGTH_LONG).show()
                                }
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GasPrimary)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "جارٍ الإرسال..." else "حفظ الإعدادات")
                }

            }
        }
        GasCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("السيارات المحلية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (vehicles.isEmpty()) {
                    Text("لا توجد سيارات محفوظة", color = Color.White.copy(alpha = 0.72f))
                } else {
                    vehicles.forEach { vehicle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vehicle.plateNumber, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(vehicle.driverName, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
                            }
                            TextButton(onClick = { onDeleteVehicle(vehicle.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFFB4B4))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("حذف", color = Color(0xFFFFB4B4))
                            }
                        }
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GasMetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GasPanelElevated.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, textAlign = TextAlign.Center)
            HorizontalDivider(color = color.copy(alpha = 0.6f), thickness = 1.dp, modifier = Modifier.fillMaxWidth(0.5f))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun GasSummaryMetric(title: String, value: String, subtitle: String) {
    Surface(
        color = GasPanelElevated.copy(alpha = 0.94f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GasPrimary.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
            HorizontalDivider(color = GasPrimary.copy(alpha = 0.65f), thickness = 1.dp, modifier = Modifier.fillMaxWidth(0.5f))
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun GasListRow(title: String, value: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White.copy(alpha = 0.84f), fontSize = 12.sp)
        Text(subtitle, color = GasPrimary.copy(alpha = 0.88f), fontSize = 11.sp)
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
    }
}

@Composable
private fun GasCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        tonalElevation = 10.dp,
        shadowElevation = 12.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            GasPanelElevated.copy(alpha = 0.98f),
                            GasPanel.copy(alpha = 0.94f)
                        )
                    ),
                    RoundedCornerShape(26.dp)
                )
                .padding(18.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun GasScreenBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(GasInk, Color(0xFF102633), Color(0xFF173847))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(GasAccent.copy(alpha = 0.18f), Color.Transparent),
                        radius = 700f
                    )
                )
                .align(Alignment.TopCenter)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(30.dp))
        )
        content()
    }
}

@Composable
private fun gasTextFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.92f),
    focusedLabelColor = GasPrimary,
    unfocusedLabelColor = Color.White.copy(alpha = 0.82f),
    cursorColor = Color.White,
    focusedBorderColor = GasPrimary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.16f),
    focusedContainerColor = GasPanel.copy(alpha = 0.92f),
    unfocusedContainerColor = GasPanel.copy(alpha = 0.78f)
)

private fun computeGasSummary(transactions: List<GasTransaction>, settlements: List<GasSettlement>): GasSummary {
    val totalLiters = transactions.sumOf { it.liters }
    val totalGasAmount = transactions.sumOf { it.totalAmount }
    val totalSettlements = settlements.sumOf { it.amount }
    return GasSummary(
        totalTransactions = transactions.size,
        totalLiters = totalLiters,
        totalGasAmount = totalGasAmount,
        totalSettlements = totalSettlements,
        balanceDue = totalGasAmount - totalSettlements
    )
}

private fun sanitizeDecimal(input: String): String {
    var dotSeen = false
    val builder = StringBuilder()
    input.forEach { ch ->
        when {
            ch.isDigit() -> builder.append(ch)
            (ch == '.' || ch == '٫' || ch == '،') && !dotSeen -> {
                builder.append('.')
                dotSeen = true
            }
        }
    }
    return builder.toString()
}

private fun sanitizeReceiptNumber(input: String): String = buildString {
    input.forEach { ch ->
        if (ch.isLetterOrDigit() || ch == '-' || ch == '/' || ch == '_') append(ch)
    }
}.trim().uppercase(Locale.ROOT)

private fun sanitizeDateInput(input: String): String {
    val digitsAndSlash = buildString {
        input.forEach { ch ->
            if (ch.isDigit() || ch == '/') append(ch)
        }
    }.take(10)
    if (digitsAndSlash.length <= 4) return digitsAndSlash
    if (digitsAndSlash.length <= 7) {
        return digitsAndSlash.substring(0, 4) + "/" + digitsAndSlash.substring(4).replace("/", "")
    }
    val compact = digitsAndSlash.replace("/", "")
    val y = compact.take(4)
    val m = compact.drop(4).take(2)
    val d = compact.drop(6).take(2)
    return listOf(y, m, d).filter { it.isNotBlank() }.joinToString("/")
}

private fun formatDateOnly(value: Long): String =
    SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(value))

private fun monthKeyFromMillis(value: Long): String =
    SimpleDateFormat("yyyy/MM", Locale.US).format(Date(value))

private fun monthKeyFromFillDate(fillDate: String): String? {
    val normalized = fillDate.trim().replace('-', '/').replace('.', '/')
    val match = Regex("^(\\d{4})/(\\d{1,2})").find(normalized) ?: return null
    val year = match.groupValues[1]
    val month = match.groupValues[2].padStart(2, '0')
    return "$year/$month"
}

private fun dayFromDateText(value: String): Int? {
    val normalized = value.trim().replace('-', '/').replace('.', '/')
    val match = Regex("^\\d{4}/\\d{1,2}/(\\d{1,2})").find(normalized) ?: return null
    return match.groupValues[1].toIntOrNull()
}

private fun formatGasDate(value: Long): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(value))

private fun createGasMonthlyPdf(
    context: android.content.Context,
    monthLabel: String,
    transactions: List<GasTransaction>,
    settlements: List<GasSettlement>,
    summary: GasSummary,
    normalLiters: Double,
    commercialLiters: Double,
    normalAmount: Double,
    commercialAmount: Double,
): File {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint().apply {
        textSize = 22f
        isFakeBoldText = true
    }
    val textPaint = Paint().apply { textSize = 14f }

    var y = 50
    canvas.drawText("Gas Station Report", 185f, y.toFloat(), titlePaint)
    y += 35
    canvas.drawText("Month: $monthLabel", 40f, y.toFloat(), textPaint)
    y += 24
    canvas.drawText("Transactions: ${summary.totalTransactions}", 40f, y.toFloat(), textPaint)
    y += 20
    canvas.drawText("Total Liters: ${formatReadableNumber(summary.totalLiters, 0)}", 40f, y.toFloat(), textPaint)
    y += 20
    canvas.drawText("Normal Liters: ${formatReadableNumber(normalLiters, 0)}", 40f, y.toFloat(), textPaint)
    y += 20
    canvas.drawText("Commercial Liters: ${formatReadableNumber(commercialLiters, 0)}", 40f, y.toFloat(), textPaint)
    y += 20
    canvas.drawText("Normal Amount: ${formatAmountReadable(normalAmount)} IQD", 40f, y.toFloat(), textPaint)
    y += 20
    canvas.drawText("Commercial Amount: ${formatAmountReadable(commercialAmount)} IQD", 40f, y.toFloat(), textPaint)
    y += 20
    canvas.drawText("Settlements: ${formatAmountReadable(summary.totalSettlements)} IQD", 40f, y.toFloat(), textPaint)
    y += 20
    canvas.drawText("Balance Due: ${formatAmountReadable(summary.balanceDue)} IQD", 40f, y.toFloat(), textPaint)
    y += 32
    canvas.drawText("Latest Transactions:", 40f, y.toFloat(), titlePaint)
    y += 26

    transactions.take(12).forEach { tx ->
        val line = "${tx.receiptNumber} | ${tx.driverName} | ${tx.plateNumber} | ${formatReadableNumber(tx.liters, 0)} L | ${formatAmountReadable(tx.totalAmount)}"
        canvas.drawText(line.take(86), 40f, y.toFloat(), textPaint)
        y += 18
    }

    y += 16
    canvas.drawText("Settlements Count: ${settlements.size}", 40f, y.toFloat(), textPaint)

    pdf.finishPage(page)
    val file = File(context.cacheDir, "gas_report_${monthLabel.replace('/', '_')}.pdf")
    pdf.writeTo(FileOutputStream(file))
    pdf.close()
    return file
}

private fun shareGasReportText(
    context: android.content.Context,
    monthLabel: String,
    summary: GasSummary,
    normalLiters: Double,
    commercialLiters: Double,
    normalAmount: Double,
    commercialAmount: Double,
) {
    val reportText = buildString {
        appendLine("تقرير محطة الكاز")
        appendLine("الفترة: $monthLabel")
        appendLine("عدد التعبئات: ${summary.totalTransactions}")
        appendLine("إجمالي اللترات: ${formatReadableNumber(summary.totalLiters, 0)}")
        appendLine("الاعتيادي: ${formatReadableNumber(normalLiters, 0)} لتر | ${formatAmountReadable(normalAmount)} د.ع")
        appendLine("التجاري: ${formatReadableNumber(commercialLiters, 0)} لتر | ${formatAmountReadable(commercialAmount)} د.ع")
        appendLine("إجمالي التسديدات: ${formatAmountReadable(summary.totalSettlements)} د.ع")
        appendLine("الرصيد المستحق: ${formatAmountReadable(summary.balanceDue)} د.ع")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, reportText)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة ملخص التقرير"))
}

private fun createGasMonthlyXlsx(
    context: android.content.Context,
    monthLabel: String,
    transactions: List<GasTransaction>,
): File {
    val file = File(context.cacheDir, "gas_report_${monthLabel.replace('/', '_')}.xlsx")
    val headers = listOf(
        "رقم الوصل",
        "تاريخ الوصل",
        "اسم السائق",
        "رقم السيارة",
        "نوع الكاز",
        "اللترات",
        "سعر اللتر",
        "المبلغ الكلي",
        "ملاحظات"
    )

    val sheetXml = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")

        append("<row r=\"1\">")
        headers.forEachIndexed { index, header ->
            val cellRef = "${excelColumnName(index)}1"
            append("<c r=\"$cellRef\" t=\"inlineStr\"><is><t>${escapeXml(header)}</t></is></c>")
        }
        append("</row>")

        transactions.forEachIndexed { txIndex, tx ->
            val rowNumber = txIndex + 2
            append("<row r=\"$rowNumber\">")
            appendInlineCell(0, rowNumber, tx.receiptNumber)
            appendInlineCell(1, rowNumber, tx.fillDate)
            appendInlineCell(2, rowNumber, tx.driverName)
            appendInlineCell(3, rowNumber, tx.plateNumber)
            appendInlineCell(4, rowNumber, tx.fuelType)
            appendNumberCell(5, rowNumber, tx.liters)
            appendNumberCell(6, rowNumber, tx.pricePerLiter)
            appendNumberCell(7, rowNumber, tx.totalAmount)
            appendInlineCell(8, rowNumber, tx.notes)
            append("</row>")
        }

        append("</sheetData></worksheet>")
    }

    val contentTypesXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
        </Types>
    """.trimIndent()

    val rootRelsXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    val workbookXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>
                <sheet name="Gas Report" sheetId="1" r:id="rId1"/>
            </sheets>
        </workbook>
    """.trimIndent()

    val workbookRelsXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
        </Relationships>
    """.trimIndent()

    ZipOutputStream(file.outputStream()).use { zip ->
        fun writeEntry(path: String, content: String) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        writeEntry("[Content_Types].xml", contentTypesXml)
        writeEntry("_rels/.rels", rootRelsXml)
        writeEntry("xl/workbook.xml", workbookXml)
        writeEntry("xl/_rels/workbook.xml.rels", workbookRelsXml)
        writeEntry("xl/worksheets/sheet1.xml", sheetXml)
    }

    return file
}

private fun StringBuilder.appendInlineCell(columnIndex: Int, rowNumber: Int, value: String) {
    val cellRef = "${excelColumnName(columnIndex)}$rowNumber"
    append("<c r=\"$cellRef\" t=\"inlineStr\"><is><t>${escapeXml(value)}</t></is></c>")
}

private fun StringBuilder.appendNumberCell(columnIndex: Int, rowNumber: Int, value: Double) {
    val cellRef = "${excelColumnName(columnIndex)}$rowNumber"
    append("<c r=\"$cellRef\"><v>${value.toString()}</v></c>")
}

private fun excelColumnName(index: Int): String {
    var value = index
    val result = StringBuilder()
    do {
        result.insert(0, ('A'.code + (value % 26)).toChar())
        value = (value / 26) - 1
    } while (value >= 0)
    return result.toString()
}

private fun escapeXml(value: String): String {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private fun shareGasFile(
    context: android.content.Context,
    file: File,
    mimeType: String,
    chooserTitle: String,
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}

private fun formatPlainNumber(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

private fun formatReadableNumber(value: Number, maxFractionDigits: Int = 2): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val pattern = if (maxFractionDigits <= 0) "#,##0" else "#,##0.${"#".repeat(maxFractionDigits)}"
    return DecimalFormat(pattern, symbols).format(value.toDouble())
}

private fun formatAmountReadable(value: Number): String = formatReadableNumber(value, 0)

private fun amountDisplayAccounting(value: Number): Pair<String, String?> {
    val scaled = kotlin.math.round(value.toDouble() / 1000.0).toLong()
    return formatAmountReadable(scaled) to null
}

private fun vehicleIdentity(plateNumber: String, driverName: String): String =
    "${plateNumber.trim().lowercase(Locale.ROOT)}|${driverName.trim().lowercase(Locale.ROOT)}"

private fun transactionIdentity(receiptNumber: String, fallbackId: String): String =
    receiptNumber.trim().ifBlank { fallbackId.trim() }.lowercase(Locale.ROOT)

private val GasInk = Color(0xFF08131A)
private val GasPanel = Color(0xFF112732)
private val GasPanelElevated = Color(0xFF183441)
private val GasPrimary = Color(0xFFC8A24A)
private val GasAccent = Color(0xFF56B7C9)
private val GasWarning = Color(0xFFE1B75A)
private val GasDanger = Color(0xFFD97A57)
private val GasSuccess = Color(0xFF5FC28E)