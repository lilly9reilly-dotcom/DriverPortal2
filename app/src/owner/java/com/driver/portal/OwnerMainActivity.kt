package com.driver.portal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driver.portal.network.GoogleSheetConfig
import com.driver.portal.ui.theme.DriverPortalTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Calendar
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OwnerMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DriverPortalTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    OwnerRootScreen()
                }
            }
        }
    }
}

private enum class OwnerRole(val title: String) {
    SuperAdmin("مدير عام"),
    ReadOnly("مراقب قراءة")
}

private const val OWNER_ADMIN_TRANSPORT_URL =
    "https://script.google.com/macros/s/AKfycbwCreVvebaAN7C4W2OZu6ura7cza42P2lIssNt4sVBv1raDqZkQYY-ZZyNNcl9_iynhAw/exec?page=admin"

private object OwnerAuthStore {
    private const val PREF = "owner_auth_prefs"
    private const val KEY_SUPER_PIN_HASH = "super_pin_hash"
    private const val KEY_READONLY_PIN_HASH = "readonly_pin_hash"
    private const val KEY_AUTH_ROLE = "auth_role"
    private const val KEY_IS_AUTH = "is_auth"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCK_UNTIL_MS = "lock_until_ms"
    private const val MAX_ATTEMPTS = 5
    private const val LOCK_DURATION_MS = 5 * 60 * 1000L

    private fun prefs(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun isConfigured(context: Context): Boolean {
        val p = prefs(context)
        return p.getString(KEY_SUPER_PIN_HASH, null)?.isNotBlank() == true
    }

    fun configure(context: Context, superPin: String, readOnlyPin: String) {
        prefs(context).edit()
            .putString(KEY_SUPER_PIN_HASH, hashPin(superPin))
            .putString(KEY_READONLY_PIN_HASH, if (readOnlyPin.isBlank()) hashPin(superPin) else hashPin(readOnlyPin))
            .putBoolean(KEY_IS_AUTH, false)
            .putString(KEY_AUTH_ROLE, "")
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCK_UNTIL_MS, 0L)
            .apply()
    }

    fun currentRole(context: Context): OwnerRole? {
        if (!prefs(context).getBoolean(KEY_IS_AUTH, false)) return null
        return when (prefs(context).getString(KEY_AUTH_ROLE, "")) {
            OwnerRole.SuperAdmin.name -> OwnerRole.SuperAdmin
            OwnerRole.ReadOnly.name -> OwnerRole.ReadOnly
            else -> null
        }
    }

    fun setAuthenticated(context: Context, role: OwnerRole?) {
        prefs(context).edit()
            .putBoolean(KEY_IS_AUTH, role != null)
            .putString(KEY_AUTH_ROLE, role?.name ?: "")
            .apply()
    }

    fun lockRemainingMs(context: Context): Long {
        val until = prefs(context).getLong(KEY_LOCK_UNTIL_MS, 0L)
        return (until - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun verifyPin(context: Context, pin: String): OwnerRole? {
        if (lockRemainingMs(context) > 0) return null
        val p = prefs(context)
        val input = hashPin(pin)
        val superHash = p.getString(KEY_SUPER_PIN_HASH, "") ?: ""
        val readHash = p.getString(KEY_READONLY_PIN_HASH, "") ?: ""

        val matchedRole = when {
            input == superHash -> OwnerRole.SuperAdmin
            input == readHash -> OwnerRole.ReadOnly
            else -> null
        }

        if (matchedRole != null) {
            p.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCK_UNTIL_MS, 0L).apply()
            return matchedRole
        }

        val nextAttempts = p.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val editor = p.edit().putInt(KEY_FAILED_ATTEMPTS, nextAttempts)
        if (nextAttempts >= MAX_ATTEMPTS) {
            editor.putLong(KEY_LOCK_UNTIL_MS, System.currentTimeMillis() + LOCK_DURATION_MS)
            editor.putInt(KEY_FAILED_ATTEMPTS, 0)
        }
        editor.apply()
        return null
    }

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.trim().toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

@Composable
private fun OwnerRootScreen() {
    val context = LocalContext.current
    var configured by remember { mutableStateOf(OwnerAuthStore.isConfigured(context)) }
    var role by remember { mutableStateOf(OwnerAuthStore.currentRole(context)) }

    if (!configured) {
        OwnerSetupScreen(
            onConfigured = {
                configured = true
                role = null
            }
        )
        return
    }

    if (role == null) {
        OwnerLoginScreen(
            onLoggedIn = {
                role = it
            }
        )
        return
    }

    OwnerDashboardScreen(
        role = role ?: OwnerRole.ReadOnly,
        onLogout = {
            OwnerAuthStore.setAuthenticated(context, null)
            role = null
        }
    )
}

@Composable
private fun OwnerSetupScreen(onConfigured: () -> Unit) {
    val context = LocalContext.current
    var superPin by remember { mutableStateOf("") }
    var superPinConfirm by remember { mutableStateOf("") }
    var readOnlyPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF071426), Color(0xFF0B233E))))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("تهيئة تطبيق المسؤول الرئيسي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("ضبط صلاحيات مدير عام ومراقب قراءة", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)

        OutlinedTextField(
            value = superPin,
            onValueChange = { superPin = it.filter { ch -> ch.isDigit() }.take(8) },
            label = { Text("PIN المدير العام") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = superPinConfirm,
            onValueChange = { superPinConfirm = it.filter { ch -> ch.isDigit() }.take(8) },
            label = { Text("تأكيد PIN المدير العام") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = readOnlyPin,
            onValueChange = { readOnlyPin = it.filter { ch -> ch.isDigit() }.take(8) },
            label = { Text("PIN المراقب (اختياري)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (superPin.length < 4 || superPin != superPinConfirm) {
                    Toast.makeText(context, "تحقق من PIN المدير العام", Toast.LENGTH_LONG).show()
                    return@Button
                }
                if (readOnlyPin.isNotBlank() && readOnlyPin.length < 4) {
                    Toast.makeText(context, "PIN المراقب يجب أن يكون 4 أرقام على الأقل", Toast.LENGTH_LONG).show()
                    return@Button
                }
                OwnerAuthStore.configure(context, superPin, readOnlyPin)
                Toast.makeText(context, "تمت التهيئة بنجاح", Toast.LENGTH_LONG).show()
                onConfigured()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1492E6))
        ) {
            Text("حفظ إعدادات الصلاحيات")
        }
    }
}

@Composable
private fun OwnerLoginScreen(onLoggedIn: (OwnerRole) -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var lockRemainingMs by remember { mutableStateOf(OwnerAuthStore.lockRemainingMs(context)) }

    LaunchedEffect(lockRemainingMs) {
        if (lockRemainingMs > 0) {
            kotlinx.coroutines.delay(1000)
            lockRemainingMs = OwnerAuthStore.lockRemainingMs(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF071426), Color(0xFF0B233E))))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("دخول المسؤول الرئيسي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Text("أدخل PIN لتحديد الصلاحية تلقائيًا", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)

        if (lockRemainingMs > 0) {
            val sec = (lockRemainingMs / 1000L).coerceAtLeast(0L)
            Text("الحساب مقفل مؤقتًا: ${sec} ثانية", color = Color(0xFFFBBF24), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(8) },
            label = { Text("PIN") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (OwnerAuthStore.lockRemainingMs(context) > 0) {
                    lockRemainingMs = OwnerAuthStore.lockRemainingMs(context)
                    return@Button
                }
                val role = OwnerAuthStore.verifyPin(context, pin)
                if (role == null) {
                    lockRemainingMs = OwnerAuthStore.lockRemainingMs(context)
                    pin = ""
                    Toast.makeText(context, "PIN غير صحيح", Toast.LENGTH_LONG).show()
                    return@Button
                }
                OwnerAuthStore.setAuthenticated(context, role)
                Toast.makeText(context, "تم الدخول بصلاحية: ${role.title}", Toast.LENGTH_SHORT).show()
                onLoggedIn(role)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1492E6))
        ) {
            Text("دخول")
        }
    }
}

private data class OwnerSystemSnapshot(
    val monthKey: String,
    val activeDrivers: Int,
    val totalReceipts: Int,
    val halafayaReceipts: Int,
    val factoryReceipts: Int,
    val totalQuantityTon: Double,
)

private data class OwnerAlert(
    val level: String,
    val title: String,
    val details: String,
)

private data class OwnerAuditEntry(
    val timestamp: String,
    val action: String,
    val result: String,
)

private data class OwnerHealthStatus(
    val coreApiOk: Boolean,
    val coreApiLatencyMs: Long,
    val adminPageOk: Boolean,
    val adminPageLatencyMs: Long,
    val lastSnapshotAt: String,
)

private data class OwnerAppServiceStatus(
    val appName: String,
    val serviceName: String,
    val ok: Boolean,
    val latencyMs: Long,
    val details: String,
)

private data class OwnerAdminOpResult(
    val title: String,
    val success: Boolean,
    val message: String,
    val timestamp: String,
)

private data class OwnerLiveVehicle(
    val driverName: String,
    val carNumber: String,
    val lat: Double,
    val lng: Double,
    val status: String,
    val speedKmh: Double,
    val lastUpdate: String,
)

private data class OwnerTimelineEvent(
    val timestampMs: Long,
    val timestampText: String,
    val source: String,
    val title: String,
    val details: String,
    val level: String,
)

private data class OwnerSafetyThresholds(
    val highSpeedKmh: Double,
    val staleMinutes: Int,
)

private object OwnerAuditLogStore {
    private const val PREF = "owner_admin_prefs"
    private const val KEY_AUDIT_LOG = "owner_admin_audit_log"
    private const val MAX_ITEMS = 50

    fun load(context: Context): List<OwnerAuditEntry> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_AUDIT_LOG, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    add(
                        OwnerAuditEntry(
                            timestamp = item.optString("timestamp"),
                            action = item.optString("action"),
                            result = item.optString("result"),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun append(context: Context, action: String, result: String) {
        val existing = load(context).toMutableList()
        existing.add(
            0,
            OwnerAuditEntry(
                timestamp = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.US)
                    .format(java.util.Date()),
                action = action,
                result = result,
            )
        )
        val trimmed = existing.take(MAX_ITEMS)
        val arr = JSONArray()
        trimmed.forEach { entry ->
            arr.put(
                JSONObject()
                    .put("timestamp", entry.timestamp)
                    .put("action", entry.action)
                    .put("result", entry.result)
            )
        }
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUDIT_LOG, arr.toString()).apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUDIT_LOG, "[]").apply()
    }
}

private object OwnerSafetyPrefs {
    private const val PREF = "owner_safety_prefs"
    private const val KEY_SPEED_LIMIT_KMH = "speed_limit_kmh"
    private const val KEY_STALE_MINUTES = "stale_minutes"
    private const val DEFAULT_SPEED_LIMIT_KMH = 110
    private const val DEFAULT_STALE_MINUTES = 15

    fun defaults(): OwnerSafetyThresholds {
        return OwnerSafetyThresholds(
            highSpeedKmh = DEFAULT_SPEED_LIMIT_KMH.toDouble(),
            staleMinutes = DEFAULT_STALE_MINUTES,
        )
    }

    fun load(context: Context): OwnerSafetyThresholds {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val speed = prefs.getInt(KEY_SPEED_LIMIT_KMH, DEFAULT_SPEED_LIMIT_KMH).coerceIn(60, 180)
        val stale = prefs.getInt(KEY_STALE_MINUTES, DEFAULT_STALE_MINUTES).coerceIn(5, 120)
        return OwnerSafetyThresholds(
            highSpeedKmh = speed.toDouble(),
            staleMinutes = stale,
        )
    }

    fun save(context: Context, thresholds: OwnerSafetyThresholds) {
        val speed = thresholds.highSpeedKmh.toInt().coerceIn(60, 180)
        val stale = thresholds.staleMinutes.coerceIn(5, 120)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SPEED_LIMIT_KMH, speed)
            .putInt(KEY_STALE_MINUTES, stale)
            .apply()
    }
}

private fun buildOwnerAlerts(snapshot: OwnerSystemSnapshot?, error: String?): List<OwnerAlert> {
    val alerts = mutableListOf<OwnerAlert>()

    if (!error.isNullOrBlank()) {
        alerts += OwnerAlert(
            level = "critical",
            title = "فشل قراءة النظام",
            details = error
        )
        return alerts
    }

    if (snapshot == null) return alerts

    if (snapshot.activeDrivers == 0) {
        alerts += OwnerAlert(
            level = "critical",
            title = "لا يوجد سواق نشطون",
            details = "تحقق من مزامنة بيانات السواق أو استجابة endpoint drivers"
        )
    }
    if (snapshot.totalReceipts == 0) {
        alerts += OwnerAlert(
            level = "warning",
            title = "لا توجد وصولات للشهر الحالي",
            details = "قد يكون السبب انقطاع إدخال البيانات أو خطأ في قراءة getAllReceiptsData"
        )
    }
    if (snapshot.factoryReceipts == 0 && snapshot.halafayaReceipts > 0) {
        alerts += OwnerAlert(
            level = "warning",
            title = "وصولات المعمل = 0",
            details = "تحقق من تصنيف وصولات المعمل أو توقف إدخالها"
        )
    }
    if (snapshot.totalQuantityTon <= 0.0 && snapshot.totalReceipts > 0) {
        alerts += OwnerAlert(
            level = "warning",
            title = "كميات غير مقروءة",
            details = "يوجد وصولات لكن إجمالي الكميات صفر، راجع حقول quantity/finalQuantity/qty"
        )
    }

    if (alerts.isEmpty()) {
        alerts += OwnerAlert(
            level = "ok",
            title = "لا توجد تنبيهات حرجة",
            details = "النظام يعمل ضمن المؤشرات المتوقعة"
        )
    }

    return alerts
}

private suspend fun fetchEndpointHealth(url: String): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
    val start = System.currentTimeMillis()
    return@withContext try {
        val response = URL(url).readText()
        val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        (response.isNotBlank()) to elapsed
    } catch (_: Exception) {
        false to (System.currentTimeMillis() - start).coerceAtLeast(1L)
    }
}

private suspend fun fetchPostHealth(url: String, params: Map<String, String>): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
    val start = System.currentTimeMillis()
    val encoded = params.entries.joinToString("&") { (k, v) ->
        "${URLEncoder.encode(k, StandardCharsets.UTF_8.toString())}=${URLEncoder.encode(v, StandardCharsets.UTF_8.toString())}"
    }
    return@withContext try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 15000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }
        try {
            OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                writer.write(encoded)
            }
            val responseStream = if (connection.responseCode in 200..299) connection.inputStream else (connection.errorStream ?: connection.inputStream)
            val body = BufferedReader(responseStream.reader(StandardCharsets.UTF_8)).use { it.readText() }
            val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
            (body.isNotBlank() && !body.contains("Exception", ignoreCase = true)) to elapsed
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        false to (System.currentTimeMillis() - start).coerceAtLeast(1L)
    }
}

private suspend fun fetchOwnerAppServicesMatrix(monthKey: String): List<OwnerAppServiceStatus> {
    val standardDriversUrl = GoogleSheetConfig.execUrl("drivers")
    val companyMonthUrl = GoogleSheetConfig.execUrl("getAllReceiptsData", "month" to monthKey)
    val gasExecUrl = GoogleSheetConfig.EXEC_ENDPOINT

    val (stdOk, stdLatency) = fetchEndpointHealth(standardDriversUrl)
    val (cmpOk, cmpLatency) = fetchEndpointHealth(companyMonthUrl)
    val (gasOk, gasLatency) = fetchPostHealth(gasExecUrl, mapOf("action" to "gas_mvp_list"))

    return listOf(
        OwnerAppServiceStatus(
            appName = "Standard",
            serviceName = "drivers",
            ok = stdOk,
            latencyMs = stdLatency,
            details = if (stdOk) "تغذية السواق متاحة" else "تعذر قراءة قائمة السواق"
        ),
        OwnerAppServiceStatus(
            appName = "Company",
            serviceName = "getAllReceiptsData",
            ok = cmpOk,
            latencyMs = cmpLatency,
            details = if (cmpOk) "بيانات وصولات الشهر متاحة" else "تعذر قراءة وصولات الشهر"
        ),
        OwnerAppServiceStatus(
            appName = "Gas",
            serviceName = "gas_mvp_list",
            ok = gasOk,
            latencyMs = gasLatency,
            details = if (gasOk) "بيانات الوقود متاحة" else "تعذر قراءة بيانات الوقود"
        ),
    )
}

private suspend fun fetchOwnerLiveVehicles(): List<OwnerLiveVehicle> = withContext(Dispatchers.IO) {
    val url = GoogleSheetConfig.execUrl("drivers")
    val raw = URL(url).readText()
    val json = parseJsonObject(raw)
    val arr = sequenceOf("drivers", "data", "rows", "items")
        .mapNotNull { key -> json.optJSONArray(key) }
        .firstOrNull() ?: JSONArray()

    val result = mutableListOf<OwnerLiveVehicle>()
    for (i in 0 until arr.length()) {
        val item = arr.optJSONObject(i) ?: continue
        val driver = readJsonText(item, "driver", "driverName", "name")
        if (driver.isBlank()) continue

        val lat = item.optDouble("lat", Double.NaN)
        val lng = item.optDouble("lng", Double.NaN)
        if (!lat.isFinite() || !lng.isFinite()) continue

        result += OwnerLiveVehicle(
            driverName = driver,
            carNumber = readJsonText(item, "carNumber", "car", "plateNumber"),
            lat = lat,
            lng = lng,
            status = readJsonText(item, "status", "state").ifBlank { "offline" },
            speedKmh = readJsonNumber(item, "speed", "kmh", "velocity", "v"),
            lastUpdate = readJsonText(item, "updatedAt", "lastUpdate", "time", "timestamp", "sendTime", "created_at"),
        )
    }

    result
        .distinctBy { "${it.driverName.lowercase()}|${it.carNumber.lowercase()}" }
        .sortedWith(compareByDescending<OwnerLiveVehicle> { it.status.equals("online", ignoreCase = true) }.thenBy { it.driverName })
}

private fun parseOwnerTimestampMillis(raw: String): Long? {
    if (raw.isBlank()) return null
    val cleaned = raw.trim().substringBefore(" GMT").substringBefore(" (").trim()

    val patterns = listOf(
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    patterns.forEach { pattern ->
        val parser = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
        if (pattern.endsWith("'Z'")) {
            parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val parsed = runCatching { parser.parse(cleaned)?.time }.getOrNull()
        if (parsed != null) return parsed
    }

    val englishParser = java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", java.util.Locale.US)
    val english = runCatching { englishParser.parse(cleaned)?.time }.getOrNull()
    if (english != null) return english

    return null
}

private fun buildOwnerTimelineEvents(
    auditLog: List<OwnerAuditEntry>,
    liveVehicles: List<OwnerLiveVehicle>,
    appServices: List<OwnerAppServiceStatus>,
    highSpeedLimitKmh: Double,
    staleLimitMinutes: Int,
): List<OwnerTimelineEvent> {
    val events = mutableListOf<OwnerTimelineEvent>()
    val nowMs = System.currentTimeMillis()
    val nowText = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(nowMs))

    auditLog.forEach { entry ->
        events += OwnerTimelineEvent(
            timestampMs = parseOwnerTimestampMillis(entry.timestamp) ?: nowMs,
            timestampText = entry.timestamp,
            source = "Admin",
            title = entry.action,
            details = entry.result,
            level = if (entry.result.contains("فشل")) "critical" else "ok",
        )
    }

    liveVehicles.forEach { vehicle ->
        val ts = parseOwnerTimestampMillis(vehicle.lastUpdate) ?: nowMs
        val isStale = (nowMs - ts) > (staleLimitMinutes * 60 * 1000L)
        val speedPart = if (vehicle.speedKmh > 0.0) " | ${String.format("%.1f", vehicle.speedKmh)} كم/س" else ""
        events += OwnerTimelineEvent(
            timestampMs = ts,
            timestampText = if (vehicle.lastUpdate.isNotBlank()) vehicle.lastUpdate else nowText,
            source = "Tracking",
            title = "${vehicle.driverName} (${vehicle.carNumber.ifBlank { "بدون رقم" }})",
            details = "الحالة: ${vehicle.status}$speedPart",
            level = when {
                vehicle.speedKmh >= highSpeedLimitKmh -> "warning"
                isStale -> "warning"
                vehicle.status.equals("online", ignoreCase = true) -> "ok"
                else -> "warning"
            },
        )
    }

    appServices.forEach { service ->
        events += OwnerTimelineEvent(
            timestampMs = nowMs,
            timestampText = nowText,
            source = service.appName,
            title = service.serviceName,
            details = "${if (service.ok) "UP" else "DOWN"} | ${service.latencyMs}ms",
            level = if (service.ok) "ok" else "critical",
        )
    }

    return events
        .sortedByDescending { it.timestampMs }
        .take(25)
}

private suspend fun runOwnerAdminOperation(action: String, vararg params: Pair<String, String>): OwnerAdminOpResult {
    val startedAt = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.US)
        .format(java.util.Date())
    val url = GoogleSheetConfig.execUrl(action, *params)
    return withContext(Dispatchers.IO) {
        try {
            val raw = URL(url).readText().trim()
            val parsed = runCatching { JSONObject(raw) }.getOrNull()
            val success = parsed?.optBoolean("success", false)
                ?: (!raw.contains("Exception", ignoreCase = true) && !raw.contains("error", ignoreCase = true))
            val message = parsed?.optString("message")
                ?.takeIf { it.isNotBlank() }
                ?: raw.take(350)
            OwnerAdminOpResult(
                title = action,
                success = success,
                message = message,
                timestamp = startedAt,
            )
        } catch (e: Exception) {
            OwnerAdminOpResult(
                title = action,
                success = false,
                message = e.message ?: "فشل تنفيذ العملية",
                timestamp = startedAt,
            )
        }
    }
}

private suspend fun fetchOwnerHealthStatus(lastSnapshotAt: String): OwnerHealthStatus {
    val coreUrl = GoogleSheetConfig.execUrl("getAvailableMonths")
    val adminUrl = GoogleSheetConfig.ADMIN_PAGE_URL
    val (coreOk, coreLatency) = fetchEndpointHealth(coreUrl)
    val (adminOk, adminLatency) = fetchEndpointHealth(adminUrl)

    return OwnerHealthStatus(
        coreApiOk = coreOk,
        coreApiLatencyMs = coreLatency,
        adminPageOk = adminOk,
        adminPageLatencyMs = adminLatency,
        lastSnapshotAt = lastSnapshotAt,
    )
}

private fun parseJsonObject(raw: String): JSONObject {
    val text = raw.trimStart()
    if (text.startsWith("{")) return JSONObject(text)
    if (text.startsWith("[")) return JSONObject().put("data", JSONArray(text)).put("success", true)
    throw IllegalStateException("Invalid JSON payload")
}

private fun readJsonText(json: JSONObject, vararg keys: String): String {
    keys.forEach { key ->
        val value = json.optString(key).trim()
        if (value.isNotBlank()) return value
    }
    return ""
}

private fun readJsonNumber(json: JSONObject, vararg keys: String): Double {
    keys.forEach { key ->
        if (!json.has(key) || json.isNull(key)) return@forEach
        val value = json.opt(key)
        val parsed = when (value) {
            is Number -> value.toDouble()
            is String -> value.replace(",", "").trim().toDoubleOrNull()
            else -> null
        }
        if (parsed != null) return parsed
    }
    return 0.0
}

private fun normalizeToTonsSafe(quantity: Double): Double {
    if (quantity <= 0.0) return 0.0
    return if (quantity in 1000.0..100000.0) quantity / 1000.0 else quantity
}

private data class OwnerXlsxSheet(
    val name: String,
    val rows: List<List<String>>,
)

private enum class OwnerXlsxExportMode {
    Full,
    Executive,
}

private enum class OwnerDashboardTab(val title: String) {
    Overview("نظرة عامة"),
    LiveTracking("التتبع المباشر"),
    Alerts("التنبيهات"),
    AdminOps("العمليات الإدارية"),
    Timeline("السجل الزمني"),
    Reports("التقارير والتصدير"),
    Settings("الإعدادات"),
    Account("الصلاحيات والحساب"),
}

private fun ownerSanitizeXmlText(value: String): String {
    return buildString(value.length) {
        value.forEach { ch ->
            val allowed = ch == '\n' || ch == '\r' || ch == '\t' || ch.code >= 0x20
            if (!allowed) return@forEach
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}

private fun ownerXlsxColumnName(index: Int): String {
    var x = index
    val sb = StringBuilder()
    while (x > 0) {
        val rem = (x - 1) % 26
        sb.append(('A'.code + rem).toChar())
        x = (x - 1) / 26
    }
    return sb.reverse().toString()
}

private fun ownerBuildWorksheetXml(rows: List<List<String>>): String {
    val body = StringBuilder()
    rows.forEachIndexed { rowIndex, row ->
        val rowNum = rowIndex + 1
        body.append("<row r=\"").append(rowNum).append("\">")
        row.forEachIndexed { colIndex, cell ->
            val cellRef = ownerXlsxColumnName(colIndex + 1) + rowNum
            val text = ownerSanitizeXmlText(cell)
            body.append("<c r=\"").append(cellRef).append("\" t=\"inlineStr\"><is><t>")
                .append(text)
                .append("</t></is></c>")
        }
        body.append("</row>")
    }

    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
        "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" " +
        "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
        "<sheetData>${body}</sheetData></worksheet>"
}

private fun ownerWriteZipEntry(zip: ZipOutputStream, name: String, content: String) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(content.toByteArray(StandardCharsets.UTF_8))
    zip.closeEntry()
}

private fun writeOwnerDashboardXlsx(output: OutputStream, sheets: List<OwnerXlsxSheet>) {
    ZipOutputStream(output).use { zip ->
        val contentTypes = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
            append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
            append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
            append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
            append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
            sheets.indices.forEach { i ->
                append("<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ")
                append("ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
            }
            append("</Types>")
        }

        val rootRels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

        val workbook = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
            append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
            append("<sheets>")
            sheets.forEachIndexed { i, sheet ->
                val safeName = ownerSanitizeXmlText(sheet.name)
                append("<sheet name=\"").append(safeName).append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>")
            }
            append("</sheets></workbook>")
        }

        val workbookRels = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
            sheets.indices.forEach { i ->
                append("<Relationship Id=\"rId").append(i + 1)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" ")
                    .append("Target=\"worksheets/sheet").append(i + 1).append(".xml\"/>")
            }
            append("</Relationships>")
        }

        val styles = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
            "<fonts count=\"1\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
            "<fills count=\"1\"><fill><patternFill patternType=\"none\"/></fill></fills>" +
            "<borders count=\"1\"><border/></borders>" +
            "<cellStyleXfs count=\"1\"><xf/></cellStyleXfs>" +
            "<cellXfs count=\"1\"><xf xfId=\"0\"/></cellXfs>" +
            "</styleSheet>"

        ownerWriteZipEntry(zip, "[Content_Types].xml", contentTypes)
        ownerWriteZipEntry(zip, "_rels/.rels", rootRels)
        ownerWriteZipEntry(zip, "xl/workbook.xml", workbook)
        ownerWriteZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRels)
        ownerWriteZipEntry(zip, "xl/styles.xml", styles)
        sheets.forEachIndexed { i, sheet ->
            ownerWriteZipEntry(zip, "xl/worksheets/sheet${i + 1}.xml", ownerBuildWorksheetXml(sheet.rows))
        }
    }
}

private suspend fun exportOwnerDashboardAsXlsx(
    context: Context,
    targetUri: Uri,
    timelineEvents: List<OwnerTimelineEvent>,
    auditLog: List<OwnerAuditEntry>,
    kpiRows: List<List<String>>,
    liveVehicleRows: List<List<String>>,
    serviceRows: List<List<String>>,
    exportMode: OwnerXlsxExportMode,
) = withContext(Dispatchers.IO) {
    val timelineRows = buildList {
        add(listOf("Timestamp", "Source", "Title", "Details", "Level"))
        timelineEvents.forEach { event ->
            add(listOf(event.timestampText, event.source, event.title, event.details, event.level))
        }
    }

    val auditRows = buildList {
        add(listOf("Timestamp", "Action", "Result"))
        auditLog.forEach { item ->
            add(listOf(item.timestamp, item.action, item.result))
        }
    }

    val sheets = when (exportMode) {
        OwnerXlsxExportMode.Executive -> listOf(
            OwnerXlsxSheet(name = "KPIs", rows = kpiRows),
            OwnerXlsxSheet(name = "ServicesHistory", rows = serviceRows),
        )
        OwnerXlsxExportMode.Full -> listOf(
            OwnerXlsxSheet(name = "Timeline", rows = timelineRows),
            OwnerXlsxSheet(name = "AuditLog", rows = auditRows),
            OwnerXlsxSheet(name = "KPIs", rows = kpiRows),
            OwnerXlsxSheet(name = "LiveVehicles", rows = liveVehicleRows),
            OwnerXlsxSheet(name = "ServicesHistory", rows = serviceRows),
        )
    }

    val output = context.contentResolver.openOutputStream(targetUri)
        ?: error("تعذر فتح الملف للكتابة")
    output.use {
        writeOwnerDashboardXlsx(it, sheets)
    }
}

private suspend fun exportOwnerDashboardAsXlsxFile(
    outputFile: File,
    timelineEvents: List<OwnerTimelineEvent>,
    auditLog: List<OwnerAuditEntry>,
    kpiRows: List<List<String>>,
    liveVehicleRows: List<List<String>>,
    serviceRows: List<List<String>>,
    exportMode: OwnerXlsxExportMode,
) = withContext(Dispatchers.IO) {
    val timelineRows = buildList {
        add(listOf("Timestamp", "Source", "Title", "Details", "Level"))
        timelineEvents.forEach { event ->
            add(listOf(event.timestampText, event.source, event.title, event.details, event.level))
        }
    }

    val auditRows = buildList {
        add(listOf("Timestamp", "Action", "Result"))
        auditLog.forEach { item ->
            add(listOf(item.timestamp, item.action, item.result))
        }
    }

    val sheets = when (exportMode) {
        OwnerXlsxExportMode.Executive -> listOf(
            OwnerXlsxSheet(name = "KPIs", rows = kpiRows),
            OwnerXlsxSheet(name = "ServicesHistory", rows = serviceRows),
        )
        OwnerXlsxExportMode.Full -> listOf(
            OwnerXlsxSheet(name = "Timeline", rows = timelineRows),
            OwnerXlsxSheet(name = "AuditLog", rows = auditRows),
            OwnerXlsxSheet(name = "KPIs", rows = kpiRows),
            OwnerXlsxSheet(name = "LiveVehicles", rows = liveVehicleRows),
            OwnerXlsxSheet(name = "ServicesHistory", rows = serviceRows),
        )
    }

    outputFile.outputStream().use {
        writeOwnerDashboardXlsx(it, sheets)
    }
}

private suspend fun fetchOwnerSystemSnapshot(): OwnerSystemSnapshot = withContext(Dispatchers.IO) {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH) + 1
    val year = now.get(Calendar.YEAR)
    val monthKey = String.format("%04d_%02d", year, month)

    val driversUrl = GoogleSheetConfig.execUrl("drivers")
    val driversJson = parseJsonObject(URL(driversUrl).readText())
    val driversCandidates = sequenceOf("drivers", "data", "rows", "items")
        .mapNotNull { key -> driversJson.optJSONArray(key) }
        .firstOrNull() ?: JSONArray()

    val activeDrivers = buildSet {
        for (i in 0 until driversCandidates.length()) {
            val item = driversCandidates.optJSONObject(i) ?: continue
            val name = readJsonText(item, "driverName", "driver", "name")
            val car = readJsonText(item, "carNumber", "car", "plateNumber")
            val key = "${name.lowercase()}|${car.lowercase()}"
            if (name.isNotBlank()) add(key)
        }
    }.size

    val receiptsUrl = GoogleSheetConfig.execUrl("getAllReceiptsData", "month" to monthKey)
    val receiptsJson = parseJsonObject(URL(receiptsUrl).readText())
    val receipts = sequenceOf("data", "rows", "items", "trips")
        .mapNotNull { key -> receiptsJson.optJSONArray(key) }
        .firstOrNull() ?: JSONArray()

    var halafaya = 0
    var factory = 0
    var totalQuantityTon = 0.0
    for (i in 0 until receipts.length()) {
        val item = receipts.optJSONObject(i) ?: continue
        val sourceType = readJsonText(item, "source", "type")
        val sheetName = readJsonText(item, "sheetName")
        val destination = readJsonText(item, "destination", "station")
        val factoryName = readJsonText(item, "factory")

        val isFactory =
            sourceType.contains("factory", ignoreCase = true) ||
                sheetName.startsWith("F_") ||
                sheetName.startsWith("مع_") ||
                factoryName.isNotBlank() ||
                destination.contains("معمل", ignoreCase = true)

        if (isFactory) factory++ else halafaya++
        totalQuantityTon += normalizeToTonsSafe(readJsonNumber(item, "quantity", "finalQuantity", "qty"))
    }

    OwnerSystemSnapshot(
        monthKey = monthKey,
        activeDrivers = activeDrivers,
        totalReceipts = receipts.length(),
        halafayaReceipts = halafaya,
        factoryReceipts = factory,
        totalQuantityTon = totalQuantityTon,
    )
}

@Composable
private fun OwnerMetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2D4C))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun OwnerDashboardScreen(role: OwnerRole, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dashboardUrl = OWNER_ADMIN_TRANSPORT_URL
    var refreshKey by remember { mutableStateOf(0) }
    var loadingSnapshot by remember { mutableStateOf(true) }
    var snapshotError by remember { mutableStateOf<String?>(null) }
    var snapshot by remember { mutableStateOf<OwnerSystemSnapshot?>(null) }
    var alerts by remember { mutableStateOf<List<OwnerAlert>>(emptyList()) }
    var auditLog by remember { mutableStateOf(OwnerAuditLogStore.load(context)) }
    var healthLoading by remember { mutableStateOf(true) }
    var health by remember { mutableStateOf<OwnerHealthStatus?>(null) }
    var servicesLoading by remember { mutableStateOf(true) }
    var appServices by remember { mutableStateOf<List<OwnerAppServiceStatus>>(emptyList()) }
    var opLoading by remember { mutableStateOf(false) }
    var adminOpResult by remember { mutableStateOf<OwnerAdminOpResult?>(null) }
    var liveLoading by remember { mutableStateOf(true) }
    var liveVehicles by remember { mutableStateOf<List<OwnerLiveVehicle>>(emptyList()) }
    var liveError by remember { mutableStateOf<String?>(null) }
    var liveSearchText by remember { mutableStateOf("") }
    var liveOnlineOnly by remember { mutableStateOf(false) }
    var liveSortMode by remember { mutableStateOf("recent") }
    var timelineLoading by remember { mutableStateOf(true) }
    var safetyThresholds by remember { mutableStateOf(OwnerSafetyPrefs.load(context)) }
    var safetySpeedInput by remember { mutableStateOf(safetyThresholds.highSpeedKmh.toInt().toString()) }
    var safetyStaleInput by remember { mutableStateOf(safetyThresholds.staleMinutes.toString()) }
    var safetySaveError by remember { mutableStateOf<String?>(null) }
    var exportingXlsx by remember { mutableStateOf(false) }
    var exportMode by remember { mutableStateOf(OwnerXlsxExportMode.Full) }
    val tabItems = remember {
        listOf(
            OwnerDashboardTab.Overview,
            OwnerDashboardTab.LiveTracking,
            OwnerDashboardTab.Alerts,
            OwnerDashboardTab.AdminOps,
            OwnerDashboardTab.Timeline,
            OwnerDashboardTab.Reports,
            OwnerDashboardTab.Settings,
            OwnerDashboardTab.Account,
        )
    }
    var selectedTab by remember { mutableStateOf(OwnerDashboardTab.Overview) }

    val timelineEvents = remember(auditLog, liveVehicles, appServices, safetyThresholds) {
        buildOwnerTimelineEvents(
            auditLog = auditLog,
            liveVehicles = liveVehicles,
            appServices = appServices,
            highSpeedLimitKmh = safetyThresholds.highSpeedKmh,
            staleLimitMinutes = safetyThresholds.staleMinutes,
        )
    }

    val exportXlsxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            exportingXlsx = true
            runCatching {
                val nowMs = System.currentTimeMillis()
                val onlineCount = liveVehicles.count { it.status.equals("online", ignoreCase = true) }
                val highSpeedCount = liveVehicles.count { it.speedKmh >= safetyThresholds.highSpeedKmh }
                val staleCount = liveVehicles.count {
                    val t = parseOwnerTimestampMillis(it.lastUpdate)
                    t != null && (nowMs - t) > (safetyThresholds.staleMinutes * 60 * 1000L)
                }
                val servicesUp = appServices.count { it.ok }
                val servicesDown = appServices.size - servicesUp
                val exportAt = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(nowMs))

                val kpiRows = buildList {
                    add(listOf("Metric", "Value"))
                    add(listOf("ExportedAt", exportAt))
                    add(listOf("MonthKey", snapshot?.monthKey ?: "N/A"))
                    add(listOf("ActiveDrivers", (snapshot?.activeDrivers ?: 0).toString()))
                    add(listOf("TotalReceipts", (snapshot?.totalReceipts ?: 0).toString()))
                    add(listOf("TotalQuantityTon", String.format(java.util.Locale.US, "%.2f", snapshot?.totalQuantityTon ?: 0.0)))
                    add(listOf("VisibleVehicles", liveVehicles.size.toString()))
                    add(listOf("OnlineVehicles", onlineCount.toString()))
                    add(listOf("HighSpeedVehicles", highSpeedCount.toString()))
                    add(listOf("StaleLocationVehicles", staleCount.toString()))
                    add(listOf("HighSpeedLimitKmh", safetyThresholds.highSpeedKmh.toInt().toString()))
                    add(listOf("StaleLimitMinutes", safetyThresholds.staleMinutes.toString()))
                    add(listOf("ServicesUp", servicesUp.toString()))
                    add(listOf("ServicesDown", servicesDown.toString()))
                    add(listOf("AlertsCount", alerts.size.toString()))
                }

                val liveVehicleRows = buildList {
                    add(listOf("DriverName", "CarNumber", "Status", "SpeedKmh", "LastUpdate", "Lat", "Lng", "HighSpeed", "StaleLocation"))
                    liveVehicles.forEach { v ->
                        val parsed = parseOwnerTimestampMillis(v.lastUpdate)
                        val stale = parsed != null && (nowMs - parsed) > (safetyThresholds.staleMinutes * 60 * 1000L)
                        val highSpeed = v.speedKmh >= safetyThresholds.highSpeedKmh
                        add(
                            listOf(
                                v.driverName,
                                v.carNumber,
                                v.status,
                                String.format(java.util.Locale.US, "%.1f", v.speedKmh),
                                v.lastUpdate,
                                String.format(java.util.Locale.US, "%.6f", v.lat),
                                String.format(java.util.Locale.US, "%.6f", v.lng),
                                if (highSpeed) "YES" else "NO",
                                if (stale) "YES" else "NO",
                            )
                        )
                    }
                }

                val serviceRows = buildList {
                    add(listOf("ExportedAt", "AppName", "ServiceName", "Status", "LatencyMs", "Details"))
                    if (appServices.isEmpty()) {
                        add(listOf(exportAt, "N/A", "N/A", "UNKNOWN", "0", "No services snapshot available"))
                    } else {
                        appServices.forEach { svc ->
                            add(
                                listOf(
                                    exportAt,
                                    svc.appName,
                                    svc.serviceName,
                                    if (svc.ok) "UP" else "DOWN",
                                    svc.latencyMs.toString(),
                                    svc.details,
                                )
                            )
                        }
                    }
                }

                exportOwnerDashboardAsXlsx(
                    context = context,
                    targetUri = uri,
                    timelineEvents = timelineEvents,
                    auditLog = auditLog,
                    kpiRows = kpiRows,
                    liveVehicleRows = liveVehicleRows,
                    serviceRows = serviceRows,
                    exportMode = exportMode,
                )
            }.onSuccess {
                val modeText = if (exportMode == OwnerXlsxExportMode.Executive) "Executive" else "Full"
                OwnerAuditLogStore.append(context, "تصدير XLSX", "نجاح ($modeText)")
                auditLog = OwnerAuditLogStore.load(context)
                Toast.makeText(context, "تم حفظ ملف XLSX بنجاح", Toast.LENGTH_SHORT).show()
            }.onFailure {
                val modeText = if (exportMode == OwnerXlsxExportMode.Executive) "Executive" else "Full"
                OwnerAuditLogStore.append(context, "تصدير XLSX", "فشل ($modeText): ${it.message ?: "Unknown"}")
                auditLog = OwnerAuditLogStore.load(context)
                Toast.makeText(context, "فشل تصدير XLSX", Toast.LENGTH_SHORT).show()
            }
            exportingXlsx = false
        }
    }

    fun nowText(): String {
        return java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
    }

    LaunchedEffect(refreshKey) {
        loadingSnapshot = true
        healthLoading = true
        servicesLoading = true
        liveLoading = true
        timelineLoading = true
        snapshotError = null
        liveError = null
        val now = Calendar.getInstance()
        var monthKeyForChecks = String.format("%04d_%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
        runCatching { fetchOwnerSystemSnapshot() }
            .onSuccess {
                snapshot = it
                monthKeyForChecks = it.monthKey
                alerts = buildOwnerAlerts(it, null)
                OwnerAuditLogStore.append(context, "تحديث ملخص النظام", "نجاح")
                runCatching { fetchOwnerHealthStatus(nowText()) }
                    .onSuccess {
                        health = it
                        OwnerAuditLogStore.append(context, "فحص صحة النظام", "نجاح")
                    }
                    .onFailure {
                        OwnerAuditLogStore.append(context, "فحص صحة النظام", "فشل")
                    }
            }
            .onFailure {
                val msg = it.message ?: "تعذر تحميل ملخص النظام"
                snapshotError = msg
                alerts = buildOwnerAlerts(null, msg)
                OwnerAuditLogStore.append(context, "تحديث ملخص النظام", "فشل: $msg")
                runCatching { fetchOwnerHealthStatus(nowText()) }
                    .onSuccess {
                        health = it
                        OwnerAuditLogStore.append(context, "فحص صحة النظام", "نجاح جزئي")
                    }
                    .onFailure {
                        OwnerAuditLogStore.append(context, "فحص صحة النظام", "فشل")
                    }
            }
        runCatching { fetchOwnerAppServicesMatrix(monthKeyForChecks) }
            .onSuccess {
                appServices = it
                val up = it.count { s -> s.ok }
                OwnerAuditLogStore.append(context, "مراقبة الأنظمة الثلاثة", "نجاح ($up/${it.size})")
            }
            .onFailure {
                appServices = emptyList()
                OwnerAuditLogStore.append(context, "مراقبة الأنظمة الثلاثة", "فشل")
            }
        runCatching { fetchOwnerLiveVehicles() }
            .onSuccess {
                liveVehicles = it
                OwnerAuditLogStore.append(context, "تحديث تتبع السواق", "نجاح (${it.size})")
            }
            .onFailure {
                liveVehicles = emptyList()
                liveError = it.message ?: "تعذر تحميل بيانات التتبع"
                OwnerAuditLogStore.append(context, "تحديث تتبع السواق", "فشل")
            }
        val refreshedAudit = OwnerAuditLogStore.load(context)
        auditLog = refreshedAudit
        loadingSnapshot = false
        healthLoading = false
        servicesLoading = false
        liveLoading = false
        timelineLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071426),
                        Color(0xFF0B233E)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "مركز المسؤول العام",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        Text(
            text = "لوحة تحكم شاملة لمتابعة المركبات والحسابات وإدارة النظام بالكامل",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("الصلاحية: ${role.title}", color = Color(0xFF93C5FD), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { onLogout() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text("خروج")
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ScrollableTabRow(
                selectedTabIndex = tabItems.indexOf(selectedTab).coerceAtLeast(0),
                containerColor = Color(0xFF0E253B),
                contentColor = Color.White,
            ) {
                tabItems.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                tab.title,
                                color = if (selectedTab == tab) Color(0xFF8AD2FF) else Color.White.copy(alpha = 0.78f),
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }
        }

        if (selectedTab == OwnerDashboardTab.Overview) {
        OwnerInfoCard(
            title = "صلاحيات الإدارة",
            body = "هذه النسخة مخصصة للمسؤول العام فقط وتعرض الداشبورد المباشر للنظام من المصدر المركزي.",
            icon = Icons.Default.Security
        )

        OwnerInfoCard(
            title = "متابعة المركبات",
            body = "يمكنك متابعة تحرك السيارات وما يحدث في النظام لحظة بلحظة عبر لوحة الأدمن.",
            icon = Icons.Default.DirectionsCar
        )

        OwnerInfoCard(
            title = "المحاسبة والجرد",
            body = "تظهر مؤشرات الحسابات، التقارير، وحالة البيانات في نفس واجهة التحكم.",
            icon = Icons.Default.Analytics
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF103050))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ملخص النظام المباشر",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                if (loadingSnapshot) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color(0xFF8AD2FF), strokeWidth = 2.dp, modifier = Modifier.height(18.dp).width(18.dp))
                        Text("جاري تحميل الإحصائيات...", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                } else if (snapshotError != null) {
                    Text(snapshotError ?: "تعذر تحميل البيانات", color = Color(0xFFFFB3B3), fontSize = 12.sp)
                } else {
                    val s = snapshot
                    if (s != null) {
                        Text("الشهر: ${s.monthKey}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OwnerMetricCard("السواق النشطون", s.activeDrivers.toString(), Color(0xFF93C5FD), Modifier.weight(1f))
                            OwnerMetricCard("إجمالي الوصولات", s.totalReceipts.toString(), Color(0xFF67E8F9), Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OwnerMetricCard("وصلات حلفاية", s.halafayaReceipts.toString(), Color(0xFF22D3EE), Modifier.weight(1f))
                            OwnerMetricCard("وصلات المعمل", s.factoryReceipts.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                        }
                        OwnerMetricCard(
                            "إجمالي الكميات (طن)",
                            if (s.totalQuantityTon % 1.0 == 0.0) s.totalQuantityTon.toLong().toString() else String.format("%.2f", s.totalQuantityTon),
                            Color(0xFF34D399),
                            Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        }

        if (selectedTab == OwnerDashboardTab.Overview) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2D4C))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("صحة النظام", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (healthLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color(0xFF8AD2FF), strokeWidth = 2.dp, modifier = Modifier.height(18.dp).width(18.dp))
                        Text("جاري فحص الخدمات...", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                } else {
                    val h = health
                    if (h != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OwnerMetricCard(
                                "API الرئيسي",
                                if (h.coreApiOk) "متاح (${h.coreApiLatencyMs}ms)" else "متعطل",
                                if (h.coreApiOk) Color(0xFF34D399) else Color(0xFFF87171),
                                Modifier.weight(1f)
                            )
                            OwnerMetricCard(
                                "لوحة الأدمن",
                                if (h.adminPageOk) "متاحة (${h.adminPageLatencyMs}ms)" else "متعطلة",
                                if (h.adminPageOk) Color(0xFF22D3EE) else Color(0xFFF87171),
                                Modifier.weight(1f)
                            )
                        }
                        OwnerMetricCard(
                            "آخر مزامنة ناجحة",
                            h.lastSnapshotAt,
                            Color(0xFF93C5FD),
                            Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("تعذر حساب صحة النظام", color = Color(0xFFFFB3B3), fontSize = 12.sp)
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2842))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مراقبة التطبيقات الثلاثة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (servicesLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color(0xFF8AD2FF), strokeWidth = 2.dp, modifier = Modifier.height(18.dp).width(18.dp))
                        Text("جاري فحص Standard / Company / Gas ...", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                } else if (appServices.isEmpty()) {
                    Text("تعذر تحميل حالة التطبيقات", color = Color(0xFFFFB3B3), fontSize = 12.sp)
                } else {
                    val upCount = appServices.count { it.ok }
                    OwnerMetricCard(
                        "الخدمات المتاحة",
                        "$upCount/${appServices.size}",
                        if (upCount == appServices.size) Color(0xFF34D399) else Color(0xFFFBBF24),
                        Modifier.fillMaxWidth()
                    )
                    appServices.forEach { service ->
                        val tone = if (service.ok) Color(0xFF34D399) else Color(0xFFF87171)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = tone.copy(alpha = 0.14f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "${service.appName} • ${service.serviceName} • ${if (service.ok) "UP" else "DOWN"}",
                                    color = tone,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "الزمن: ${service.latencyMs}ms | ${service.details}",
                                    color = Color.White.copy(alpha = 0.86f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        if (selectedTab == OwnerDashboardTab.AdminOps) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2E45))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("عمليات المسؤول", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "أوامر إدارية مباشرة من التطبيق (مفعلة للمدير العام فقط)",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = role == OwnerRole.SuperAdmin && !opLoading,
                        onClick = {
                            opLoading = true
                            OwnerAuditLogStore.append(context, "تشغيل systemHealthCheck", "جاري التنفيذ")
                            auditLog = OwnerAuditLogStore.load(context)
                            scope.launch {
                                val result = runOwnerAdminOperation("systemHealthCheck")
                                adminOpResult = result
                                opLoading = false
                                OwnerAuditLogStore.append(
                                    context,
                                    "تشغيل systemHealthCheck",
                                    if (result.success) "نجاح" else "فشل"
                                )
                                auditLog = OwnerAuditLogStore.load(context)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                    ) {
                        Text("فحص شامل")
                    }

                    Button(
                        enabled = role == OwnerRole.SuperAdmin && !opLoading,
                        onClick = {
                            opLoading = true
                            OwnerAuditLogStore.append(context, "تشغيل createSystemBackup", "جاري التنفيذ")
                            auditLog = OwnerAuditLogStore.load(context)
                            scope.launch {
                                val result = runOwnerAdminOperation("createSystemBackup")
                                adminOpResult = result
                                opLoading = false
                                OwnerAuditLogStore.append(
                                    context,
                                    "تشغيل createSystemBackup",
                                    if (result.success) "نجاح" else "فشل"
                                )
                                auditLog = OwnerAuditLogStore.load(context)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Text("نسخة احتياطية")
                    }
                }

                Button(
                    enabled = role == OwnerRole.SuperAdmin && !opLoading,
                    onClick = {
                        opLoading = true
                        OwnerAuditLogStore.append(context, "تشغيل cleanupEmptySupportSheets (dry-run)", "جاري التنفيذ")
                        auditLog = OwnerAuditLogStore.load(context)
                        scope.launch {
                            val result = runOwnerAdminOperation("cleanupEmptySupportSheets", "dryRun" to "true")
                            adminOpResult = result
                            opLoading = false
                            OwnerAuditLogStore.append(
                                context,
                                "تشغيل cleanupEmptySupportSheets (dry-run)",
                                if (result.success) "نجاح" else "فشل"
                            )
                            auditLog = OwnerAuditLogStore.load(context)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                ) {
                    Text("تنظيف تجريبي آمن (بدون حذف فعلي)")
                }

                if (opLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color(0xFF93C5FD), strokeWidth = 2.dp, modifier = Modifier.height(18.dp).width(18.dp))
                        Text("جاري تنفيذ العملية...", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }

                val op = adminOpResult
                if (op != null) {
                    val tone = if (op.success) Color(0xFF34D399) else Color(0xFFF87171)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = tone.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("العملية: ${op.title}", color = tone, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("الوقت: ${op.timestamp}", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                            Text(op.message, color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        }

        if (selectedTab == OwnerDashboardTab.LiveTracking) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12314C))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تتبع السواق المباشر", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (liveLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color(0xFF8AD2FF), strokeWidth = 2.dp, modifier = Modifier.height(18.dp).width(18.dp))
                        Text("جاري تحميل مواقع السواق...", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                } else if (!liveError.isNullOrBlank()) {
                    Text(liveError ?: "تعذر تحميل بيانات التتبع", color = Color(0xFFFFB3B3), fontSize = 12.sp)
                } else if (liveVehicles.isEmpty()) {
                    Text("لا توجد مواقع سواق متاحة حالياً", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                } else {
                    val nowMs = System.currentTimeMillis()
                    val onlineCount = liveVehicles.count { it.status.equals("online", ignoreCase = true) }
                    val highSpeedCount = liveVehicles.count { it.speedKmh >= safetyThresholds.highSpeedKmh }
                    val staleCount = liveVehicles.count {
                        val t = parseOwnerTimestampMillis(it.lastUpdate)
                        t != null && (nowMs - t) > (safetyThresholds.staleMinutes * 60 * 1000L)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2438))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("إعدادات حدود السلامة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                "الحد الحالي: سرعة ${safetyThresholds.highSpeedKmh.toInt()} كم/س | تأخر ${safetyThresholds.staleMinutes} دقيقة",
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp
                            )

                            if (role == OwnerRole.SuperAdmin) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = safetySpeedInput,
                                        onValueChange = {
                                            safetySpeedInput = it.filter { ch -> ch.isDigit() }.take(3)
                                            safetySaveError = null
                                        },
                                        label = { Text("حد السرعة") },
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = safetyStaleInput,
                                        onValueChange = {
                                            safetyStaleInput = it.filter { ch -> ch.isDigit() }.take(3)
                                            safetySaveError = null
                                        },
                                        label = { Text("حد التأخر (دقيقة)") },
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val speed = safetySpeedInput.toIntOrNull()?.coerceIn(60, 180)
                                            val stale = safetyStaleInput.toIntOrNull()?.coerceIn(5, 120)
                                            if (speed == null || stale == null) {
                                                safetySaveError = "أدخل قيماً صحيحة: السرعة 60-180 والتأخر 5-120"
                                                return@Button
                                            }

                                            val updated = OwnerSafetyThresholds(speed.toDouble(), stale)
                                            safetyThresholds = updated
                                            OwnerSafetyPrefs.save(context, updated)
                                            safetySaveError = null
                                            OwnerAuditLogStore.append(context, "تحديث حدود السلامة", "سرعة=${speed} | تأخر=${stale}")
                                            auditLog = OwnerAuditLogStore.load(context)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                                    ) {
                                        Text("حفظ الحدود")
                                    }
                                    Button(
                                        onClick = {
                                            val defaults = OwnerSafetyPrefs.defaults()
                                            safetyThresholds = defaults
                                            safetySpeedInput = defaults.highSpeedKmh.toInt().toString()
                                            safetyStaleInput = defaults.staleMinutes.toString()
                                            OwnerSafetyPrefs.save(context, defaults)
                                            safetySaveError = null
                                            OwnerAuditLogStore.append(context, "إعادة حدود السلامة الافتراضية", "نجاح")
                                            auditLog = OwnerAuditLogStore.load(context)
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                    ) {
                                        Text("افتراضي")
                                    }
                                }
                            } else {
                                Text("صلاحية القراءة فقط: لا يمكن تعديل الحدود", color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp)
                            }

                            if (!safetySaveError.isNullOrBlank()) {
                                Text(safetySaveError ?: "", color = Color(0xFFFFB3B3), fontSize = 11.sp)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OwnerMetricCard("مرئيون", liveVehicles.size.toString(), Color(0xFF22D3EE), Modifier.weight(1f))
                        OwnerMetricCard("Online", onlineCount.toString(), Color(0xFF34D399), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OwnerMetricCard("سرعة عالية", highSpeedCount.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                        OwnerMetricCard("تحديث متأخر", staleCount.toString(), Color(0xFFF87171), Modifier.weight(1f))
                    }

                    OutlinedTextField(
                        value = liveSearchText,
                        onValueChange = { liveSearchText = it },
                        label = { Text("بحث باسم السائق أو رقم السيارة") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { liveOnlineOnly = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (!liveOnlineOnly) Color(0xFF0EA5E9) else Color(0xFF334155))
                        ) { Text("الكل") }
                        Button(
                            onClick = { liveOnlineOnly = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (liveOnlineOnly) Color(0xFF16A34A) else Color(0xFF334155))
                        ) { Text("Online فقط") }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { liveSortMode = "recent" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (liveSortMode == "recent") Color(0xFF0284C7) else Color(0xFF334155))
                        ) { Text("ترتيب: الأحدث") }
                        Button(
                            onClick = { liveSortMode = "speed" },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (liveSortMode == "speed") Color(0xFFB45309) else Color(0xFF334155))
                        ) { Text("ترتيب: السرعة") }
                    }

                    val filteredVehicles = liveVehicles
                        .asSequence()
                        .filter { !liveOnlineOnly || it.status.equals("online", ignoreCase = true) }
                        .filter {
                            liveSearchText.isBlank() ||
                                it.driverName.contains(liveSearchText, ignoreCase = true) ||
                                it.carNumber.contains(liveSearchText, ignoreCase = true)
                        }
                        .let { seq ->
                            when (liveSortMode) {
                                "speed" -> seq.sortedByDescending { it.speedKmh }
                                else -> seq.sortedByDescending { parseOwnerTimestampMillis(it.lastUpdate) ?: 0L }
                            }
                        }
                        .toList()

                    filteredVehicles.take(12).forEach { vehicle ->
                        val isOnline = vehicle.status.equals("online", ignoreCase = true)
                        val tone = if (isOnline) Color(0xFF34D399) else Color(0xFFFBBF24)
                        val lastUpdateMs = parseOwnerTimestampMillis(vehicle.lastUpdate)
                        val isStale = lastUpdateMs != null && (nowMs - lastUpdateMs) > (safetyThresholds.staleMinutes * 60 * 1000L)
                        val isHighSpeed = vehicle.speedKmh >= safetyThresholds.highSpeedKmh
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = tone.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "${vehicle.driverName} • ${vehicle.carNumber.ifBlank { "بدون رقم" }}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "الحالة: ${vehicle.status}",
                                    color = tone,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                )
                                if (vehicle.speedKmh > 0.0) {
                                    Text(
                                        "السرعة: ${String.format("%.1f", vehicle.speedKmh)} كم/س",
                                        color = if (isHighSpeed) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.86f),
                                        fontSize = 11.sp
                                    )
                                }
                                if (vehicle.lastUpdate.isNotBlank()) {
                                    Text(
                                        "آخر تحديث: ${vehicle.lastUpdate}",
                                        color = if (isStale) Color(0xFFF87171) else Color.White.copy(alpha = 0.80f),
                                        fontSize = 10.sp
                                    )
                                }
                                if (isHighSpeed) {
                                    Text(
                                        "تنبيه: سرعة عالية (≥ ${safetyThresholds.highSpeedKmh.toInt()} كم/س)",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (isStale) {
                                    Text(
                                        "تنبيه: تحديث الموقع متأخر (> ${safetyThresholds.staleMinutes} دقيقة)",
                                        color = Color(0xFFF87171),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${vehicle.lat},${vehicle.lng}"
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                                ) {
                                    Text("فتح موقع السائق")
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        if (selectedTab == OwnerDashboardTab.Alerts) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF102A43))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("التنبيهات الحرجة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                alerts.forEach { alert ->
                    val tone = when (alert.level) {
                        "critical" -> Color(0xFFF87171)
                        "warning" -> Color(0xFFFBBF24)
                        else -> Color(0xFF34D399)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = tone.copy(alpha = 0.14f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(alert.title, color = tone, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(alert.details, color = Color.White.copy(alpha = 0.88f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        }

        if (selectedTab == OwnerDashboardTab.Timeline) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12263A))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Timeline لحظي موحّد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (timelineLoading) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color(0xFF8AD2FF), strokeWidth = 2.dp, modifier = Modifier.height(18.dp).width(18.dp))
                        Text("جاري تجهيز الأحداث...", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    }
                } else if (timelineEvents.isEmpty()) {
                    Text("لا توجد أحداث حالياً", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                } else {
                    timelineEvents.forEach { event ->
                        val tone = when (event.level) {
                            "critical" -> Color(0xFFF87171)
                            "warning" -> Color(0xFFFBBF24)
                            else -> Color(0xFF34D399)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = tone.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "${event.source} • ${event.title}",
                                    color = tone,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(event.details, color = Color.White.copy(alpha = 0.90f), fontSize = 11.sp)
                                Text(event.timestampText, color = Color.White.copy(alpha = 0.70f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        }

        if (selectedTab == OwnerDashboardTab.Reports) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2842))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("سجل عمليات المسؤول", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Button(
                        enabled = role == OwnerRole.SuperAdmin,
                        onClick = {
                            OwnerAuditLogStore.clear(context)
                            OwnerAuditLogStore.append(context, "مسح سجل العمليات", "تم المسح")
                            auditLog = OwnerAuditLogStore.load(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                    ) {
                        Text("مسح", fontSize = 11.sp)
                    }
                }

                if (auditLog.isEmpty()) {
                    Text("لا توجد عمليات مسجلة", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                } else {
                    auditLog.take(8).forEach { entry ->
                        Text(
                            "• ${entry.timestamp} | ${entry.action} | ${entry.result}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = !exportingXlsx,
                        onClick = {
                            exportMode = OwnerXlsxExportMode.Full
                            val now = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                            exportXlsxLauncher.launch("owner_dashboard_full_$now.xlsx")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1))
                    ) {
                        Text(if (exportingXlsx) "جاري التصدير..." else "Full Export XLSX", fontSize = 11.sp)
                    }

                    Button(
                        enabled = !exportingXlsx,
                        onClick = {
                            exportMode = OwnerXlsxExportMode.Executive
                            val now = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date())
                            exportXlsxLauncher.launch("owner_dashboard_exec_$now.xlsx")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        Text(if (exportingXlsx) "جاري التصدير..." else "Executive Export XLSX", fontSize = 11.sp)
                    }
                }

                Button(
                    enabled = !exportingXlsx,
                    onClick = {
                        scope.launch {
                            exportingXlsx = true
                            runCatching {
                                val nowMs = System.currentTimeMillis()
                                val onlineCount = liveVehicles.count { it.status.equals("online", ignoreCase = true) }
                                val highSpeedCount = liveVehicles.count { it.speedKmh >= safetyThresholds.highSpeedKmh }
                                val staleCount = liveVehicles.count {
                                    val t = parseOwnerTimestampMillis(it.lastUpdate)
                                    t != null && (nowMs - t) > (safetyThresholds.staleMinutes * 60 * 1000L)
                                }
                                val servicesUp = appServices.count { it.ok }
                                val servicesDown = appServices.size - servicesUp
                                val exportAt = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(nowMs))
                                val nowName = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.US).format(java.util.Date(nowMs))

                                val kpiRows = buildList {
                                    add(listOf("Metric", "Value"))
                                    add(listOf("ExportedAt", exportAt))
                                    add(listOf("MonthKey", snapshot?.monthKey ?: "N/A"))
                                    add(listOf("ActiveDrivers", (snapshot?.activeDrivers ?: 0).toString()))
                                    add(listOf("TotalReceipts", (snapshot?.totalReceipts ?: 0).toString()))
                                    add(listOf("TotalQuantityTon", String.format(java.util.Locale.US, "%.2f", snapshot?.totalQuantityTon ?: 0.0)))
                                    add(listOf("VisibleVehicles", liveVehicles.size.toString()))
                                    add(listOf("OnlineVehicles", onlineCount.toString()))
                                    add(listOf("HighSpeedVehicles", highSpeedCount.toString()))
                                    add(listOf("StaleLocationVehicles", staleCount.toString()))
                                    add(listOf("HighSpeedLimitKmh", safetyThresholds.highSpeedKmh.toInt().toString()))
                                    add(listOf("StaleLimitMinutes", safetyThresholds.staleMinutes.toString()))
                                    add(listOf("ServicesUp", servicesUp.toString()))
                                    add(listOf("ServicesDown", servicesDown.toString()))
                                    add(listOf("AlertsCount", alerts.size.toString()))
                                }

                                val liveVehicleRows = buildList {
                                    add(listOf("DriverName", "CarNumber", "Status", "SpeedKmh", "LastUpdate", "Lat", "Lng", "HighSpeed", "StaleLocation"))
                                    liveVehicles.forEach { v ->
                                        val parsed = parseOwnerTimestampMillis(v.lastUpdate)
                                        val stale = parsed != null && (nowMs - parsed) > (safetyThresholds.staleMinutes * 60 * 1000L)
                                        val highSpeed = v.speedKmh >= safetyThresholds.highSpeedKmh
                                        add(
                                            listOf(
                                                v.driverName,
                                                v.carNumber,
                                                v.status,
                                                String.format(java.util.Locale.US, "%.1f", v.speedKmh),
                                                v.lastUpdate,
                                                String.format(java.util.Locale.US, "%.6f", v.lat),
                                                String.format(java.util.Locale.US, "%.6f", v.lng),
                                                if (highSpeed) "YES" else "NO",
                                                if (stale) "YES" else "NO",
                                            )
                                        )
                                    }
                                }

                                val serviceRows = buildList {
                                    add(listOf("ExportedAt", "AppName", "ServiceName", "Status", "LatencyMs", "Details"))
                                    if (appServices.isEmpty()) {
                                        add(listOf(exportAt, "N/A", "N/A", "UNKNOWN", "0", "No services snapshot available"))
                                    } else {
                                        appServices.forEach { svc ->
                                            add(
                                                listOf(
                                                    exportAt,
                                                    svc.appName,
                                                    svc.serviceName,
                                                    if (svc.ok) "UP" else "DOWN",
                                                    svc.latencyMs.toString(),
                                                    svc.details,
                                                )
                                            )
                                        }
                                    }
                                }

                                val shareFile = File(context.cacheDir, "owner_dashboard_exec_share_$nowName.xlsx")
                                exportOwnerDashboardAsXlsxFile(
                                    outputFile = shareFile,
                                    timelineEvents = timelineEvents,
                                    auditLog = auditLog,
                                    kpiRows = kpiRows,
                                    liveVehicleRows = liveVehicleRows,
                                    serviceRows = serviceRows,
                                    exportMode = OwnerXlsxExportMode.Executive,
                                )

                                val shareUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    shareFile,
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "مشاركة التقرير التنفيذي"))
                            }.onSuccess {
                                OwnerAuditLogStore.append(context, "Quick Share XLSX", "نجاح (Executive)")
                                auditLog = OwnerAuditLogStore.load(context)
                            }.onFailure {
                                OwnerAuditLogStore.append(context, "Quick Share XLSX", "فشل: ${it.message ?: "Unknown"}")
                                auditLog = OwnerAuditLogStore.load(context)
                                Toast.makeText(context, "فشل مشاركة التقرير التنفيذي", Toast.LENGTH_SHORT).show()
                            }
                            exportingXlsx = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Text(if (exportingXlsx) "جاري التصدير..." else "Quick Share Report (Executive)", fontSize = 11.sp)
                }
            }
        }
        }

        if (selectedTab == OwnerDashboardTab.AdminOps) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    OwnerAuditLogStore.append(context, "طلب تحديث الداشبورد", "تم التنفيذ")
                    auditLog = OwnerAuditLogStore.load(context)
                    refreshKey += 1
                },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1492E6))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تحديث الداشبورد")
            }

            Button(
                onClick = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(dashboardUrl))
                        context.startActivity(intent)
                        OwnerAuditLogStore.append(context, "فتح لوحة الأدمن خارجياً", "نجاح")
                        auditLog = OwnerAuditLogStore.load(context)
                    }.onFailure {
                        OwnerAuditLogStore.append(context, "فتح لوحة الأدمن خارجياً", "فشل")
                        auditLog = OwnerAuditLogStore.load(context)
                        Toast.makeText(context, "تعذر فتح المتصفح", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2EAE76))
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("فتح لوحة الأدمن")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E253B))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("لوحة الأدمن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "تم إلغاء العرض المدمج داخل التطبيق للحفاظ على مظهر احترافي. استخدم الزر لفتح اللوحة مباشرة.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }

        }

        if (selectedTab == OwnerDashboardTab.Settings) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C2438))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إعدادات حدود السلامة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "الحد الحالي: سرعة ${safetyThresholds.highSpeedKmh.toInt()} كم/س | تأخر ${safetyThresholds.staleMinutes} دقيقة",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 11.sp
                    )

                    if (role == OwnerRole.SuperAdmin) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = safetySpeedInput,
                                onValueChange = {
                                    safetySpeedInput = it.filter { ch -> ch.isDigit() }.take(3)
                                    safetySaveError = null
                                },
                                label = { Text("حد السرعة") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = safetyStaleInput,
                                onValueChange = {
                                    safetyStaleInput = it.filter { ch -> ch.isDigit() }.take(3)
                                    safetySaveError = null
                                },
                                label = { Text("حد التأخر (دقيقة)") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val speed = safetySpeedInput.toIntOrNull()?.coerceIn(60, 180)
                                    val stale = safetyStaleInput.toIntOrNull()?.coerceIn(5, 120)
                                    if (speed == null || stale == null) {
                                        safetySaveError = "أدخل قيماً صحيحة: السرعة 60-180 والتأخر 5-120"
                                        return@Button
                                    }
                                    val updated = OwnerSafetyThresholds(speed.toDouble(), stale)
                                    safetyThresholds = updated
                                    OwnerSafetyPrefs.save(context, updated)
                                    safetySaveError = null
                                    OwnerAuditLogStore.append(context, "تحديث حدود السلامة", "سرعة=${speed} | تأخر=${stale}")
                                    auditLog = OwnerAuditLogStore.load(context)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                            ) {
                                Text("حفظ الحدود")
                            }
                            Button(
                                onClick = {
                                    val defaults = OwnerSafetyPrefs.defaults()
                                    safetyThresholds = defaults
                                    safetySpeedInput = defaults.highSpeedKmh.toInt().toString()
                                    safetyStaleInput = defaults.staleMinutes.toString()
                                    OwnerSafetyPrefs.save(context, defaults)
                                    safetySaveError = null
                                    OwnerAuditLogStore.append(context, "إعادة حدود السلامة الافتراضية", "نجاح")
                                    auditLog = OwnerAuditLogStore.load(context)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                            ) {
                                Text("افتراضي")
                            }
                        }
                    } else {
                        Text("صلاحية القراءة فقط: لا يمكن تعديل الحدود", color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp)
                    }

                    if (!safetySaveError.isNullOrBlank()) {
                        Text(safetySaveError ?: "", color = Color(0xFFFFB3B3), fontSize = 11.sp)
                    }
                }
            }
        }

        if (selectedTab == OwnerDashboardTab.Account) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102A43))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الصلاحيات والحساب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("الدور الحالي: ${role.title}", color = Color(0xFF93C5FD), fontSize = 12.sp)
                    Text("الوصول إلى العمليات الحساسة مرتبط بصلاحية المدير العام.", color = Color.White.copy(alpha = 0.82f), fontSize = 11.sp)
                    Button(
                        onClick = { onLogout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text("تسجيل الخروج")
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerInfoCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2D4C))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = Color(0xFF8AD2FF))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(body, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }
    }
}

