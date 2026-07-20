package com.driver.portal

import android.content.Intent
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.driver.portal.network.DocNumberGuard
import com.driver.portal.network.FactoryRequest
import com.driver.portal.network.GoogleSheetConfig
import com.driver.portal.network.TripRepository
import com.driver.portal.network.TripRequest
import com.driver.portal.ui.theme.DriverPortalTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.Calendar
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONObject

data class CompanyDriver(
    val id: String,
    val name: String,
    val phone: String,
    val carNumber: String
)

data class DriverMonthlyStats(
    val trips: Int = 0,
    val tripQuantity: Double = 0.0,
    val liters: Double = 0.0,
    val tripAmount: Double = 0.0,
    val factoryTrips: Int = 0,
    val factoryQuantity: Double = 0.0,
    val factoryAmount: Double = 0.0
) {
    operator fun plus(other: DriverMonthlyStats): DriverMonthlyStats {
        return DriverMonthlyStats(
            trips = trips + other.trips,
            tripQuantity = tripQuantity + other.tripQuantity,
            liters = liters + other.liters,
            tripAmount = tripAmount + other.tripAmount,
            factoryTrips = factoryTrips + other.factoryTrips,
            factoryQuantity = factoryQuantity + other.factoryQuantity,
            factoryAmount = factoryAmount + other.factoryAmount
        )
    }
}

data class DriverTripRecord(
    val docNumber: String,
    val quantity: Double,
    val destination: String,
    val loadDate: String,
    val unloadDate: String,
)

data class DriverMonthlyReport(
    val stats: DriverMonthlyStats = DriverMonthlyStats(),
    val halafayaTrips: List<DriverTripRecord> = emptyList(),
    val factoryTrips: List<DriverTripRecord> = emptyList(),
)

data class CompanyMaintenanceRecord(
    val id: String,
    val carNumber: String,
    val assignedTo: String,
    val maintenanceType: String,
    val dueDate: String,
    val estimatedCost: Double,
    val status: String,
    val notes: String,
    val createdAt: Long,
)

private data class CompanyVersionPolicy(
    val allowed: Boolean,
    val title: String,
    val message: String,
    val supportPhone: String,
    val latestVersionName: String,
)

private data class CompanyActivationPolicy(
    val allowed: Boolean,
    val message: String,
    val reason: String,
    val boundDevice: String,
    val companyId: String,
)

private object CompanyVersionPolicyRepository {
    suspend fun fetch(): CompanyVersionPolicy = withContext(Dispatchers.IO) {
        val raw = URL(
            GoogleSheetConfig.execUrl(
                "companyVersionPolicy",
                "versionCode" to BuildConfig.VERSION_CODE.toString(),
                "versionName" to BuildConfig.VERSION_NAME,
                "packageName" to BuildConfig.APPLICATION_ID,
            )
        ).readText()
        val json = JSONObject(raw)

        CompanyVersionPolicy(
            allowed = json.optBoolean("allowed", false) && json.optBoolean("success", false),
            title = json.optString("title", "تطبيق مدير الحسابات متوقف"),
            message = json.optString("message", "هذه النسخة غير مفعلة حاليًا. يرجى التواصل مع الإدارة."),
            supportPhone = json.optString("supportPhone", "07809830249"),
            latestVersionName = json.optString("latestVersionName", BuildConfig.VERSION_NAME),
        )
    }
}

private object CompanyActivationRepository {
    private fun toHex(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        bytes.forEach { b -> out.append(String.format("%02x", b)) }
        return out.toString()
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return toHex(digest)
    }

    fun buildDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_android_id"
        val raw = "${BuildConfig.APPLICATION_ID}|$androidId"
        return sha256(raw)
    }

    suspend fun verify(context: Context, activationCode: String): CompanyActivationPolicy = withContext(Dispatchers.IO) {
        val code = activationCode.trim().replace(" ", "")
        val raw = URL(
            GoogleSheetConfig.execUrl(
                "companyActivationVerify",
                "activationCode" to code,
                "deviceId" to buildDeviceId(context),
                "appKey" to "company",
                "packageName" to BuildConfig.APPLICATION_ID,
                "versionName" to BuildConfig.VERSION_NAME,
            )
        ).readText()
        val json = JSONObject(raw)
        CompanyActivationPolicy(
            allowed = json.optBoolean("allowed", false) && json.optBoolean("success", false),
            message = json.optString("message", "تعذر التحقق من التفعيل"),
            reason = json.optString("reason", "unknown"),
            boundDevice = json.optString("boundDevice", ""),
            companyId = json.optString("companyId", "").trim(),
        )
    }
}

enum class CompanyReportHalf(val title: String) {
    All("كل الشهر"),
    FirstHalf("1 - 15"),
    SecondHalf("16 - نهاية الشهر");

    fun matchesRecord(record: DriverTripRecord): Boolean {
        // Keep unknown-date records from collapsing report totals to zero when sheet rows are inconsistent.
        val day = companyReportDayFromDate(record.unloadDate.ifBlank { record.loadDate }) ?: return true
        return matchesDay(day)
    }

    private fun matchesDay(day: Int): Boolean = when (this) {
        All -> true
        FirstHalf -> day in 1..15
        SecondHalf -> day in 16..31
    }
}

private fun companyReportDayFromDate(value: String): Int? {
    val normalizedDigits = buildString(value.length) {
        val arabicDigits = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
        )
        value.forEach { append(arabicDigits[it] ?: it) }
    }.trim()

    if (normalizedDigits.isBlank()) return null

    val datePart = normalizedDigits
        .substringBefore('T')
        .substringBefore(' ')
        .replace('/', '-')
        .replace('.', '-')
        .trim()

    val ymd = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$").find(datePart)
    if (ymd != null) {
        val day = ymd.groupValues[3].toIntOrNull()
        if (day != null && day in 1..31) return day
    }

    val dmy = Regex("^(\\d{1,2})-(\\d{1,2})-(\\d{4})$").find(datePart)
    if (dmy != null) {
        val day = dmy.groupValues[1].toIntOrNull()
        if (day != null && day in 1..31) return day
    }

    // Fallback for noisy strings that still contain a date-like tail.
    val tailYmd = Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(datePart)
    if (tailYmd != null) {
        val day = tailYmd.groupValues[3].toIntOrNull()
        if (day != null && day in 1..31) return day
    }

    // Handles Date.toString-like values from Apps Script, e.g.
    // "Sat May 30 2026 11:31:20 GMT+0300 (...)"
    val englishTextual = Regex("^[A-Za-z]{3}\\s+[A-Za-z]{3}\\s+(\\d{1,2})\\s+\\d{4}")
        .find(normalizedDigits)
    if (englishTextual != null) {
        val day = englishTextual.groupValues[1].toIntOrNull()
        if (day != null && day in 1..31) return day
    }

    // Handles Unix timestamps in seconds or milliseconds.
    val epochDigits = normalizedDigits.filter { it.isDigit() }
    if (epochDigits.length == 10 || epochDigits.length == 13) {
        val millis = epochDigits.toLongOrNull()?.let { if (epochDigits.length == 10) it * 1000L else it }
        if (millis != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            val day = cal.get(Calendar.DAY_OF_MONTH)
            if (day in 1..31) return day
        }
    }

    return null
}

private fun filterCompanyMonthlyReport(report: DriverMonthlyReport, half: CompanyReportHalf): DriverMonthlyReport {
    if (half == CompanyReportHalf.All) return report

    val filteredHalafaya = report.halafayaTrips.filter { half.matchesRecord(it) }
    val filteredFactory = report.factoryTrips.filter { half.matchesRecord(it) }

    return report.copy(
        stats = DriverMonthlyStats(
            trips = filteredHalafaya.size,
            tripQuantity = filteredHalafaya.sumOf { it.quantity },
            liters = if (report.stats.tripQuantity > 0.0) {
                report.stats.liters * (filteredHalafaya.sumOf { it.quantity } / report.stats.tripQuantity)
            } else {
                0.0
            },
            tripAmount = if (report.stats.tripQuantity > 0.0) {
                report.stats.tripAmount * (filteredHalafaya.sumOf { it.quantity } / report.stats.tripQuantity)
            } else {
                0.0
            },
            factoryTrips = filteredFactory.size,
            factoryQuantity = filteredFactory.sumOf { it.quantity },
            factoryAmount = if (report.stats.factoryQuantity > 0.0) {
                report.stats.factoryAmount * (filteredFactory.sumOf { it.quantity } / report.stats.factoryQuantity)
            } else {
                0.0
            }
        ),
        halafayaTrips = filteredHalafaya,
        factoryTrips = filteredFactory,
    )
}

object CompanyConstants {
    val STATIONS = listOf(
        "محطة حلفاية",
        "محطة التاجي",
        "محطة الدورة",
        "محطة الرصافة",
        "محطات الشمال",
        "أخرى"
    )
}

object CompanyPrefs {
    private const val PREF = "company_manager_prefs"
    private const val KEY_MANAGER_NAME = "manager_name"
    private const val KEY_MANAGER_PHONE = "manager_phone"
    private const val KEY_MANAGER_PIN = "manager_pin"
    private const val KEY_DRIVERS = "drivers_json"
    private const val KEY_MANAGER_AUTHENTICATED = "manager_authenticated"
    private const val KEY_EXCEL_QTY_ALERT_MIN_TON = "excel_qty_alert_min_ton"
    private const val KEY_EXCEL_QTY_ALERT_MAX_TON = "excel_qty_alert_max_ton"
    private const val KEY_MAINTENANCE_RECORDS = "maintenance_records_json"
    private const val KEY_COMPANY_ACTIVATION_CODE = "company_activation_code"
    private const val KEY_COMPANY_SCOPE_ID = "company_scope_id"

    fun hasManager(context: android.content.Context): Boolean {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        return !p.getString(KEY_MANAGER_NAME, "").isNullOrBlank() &&
            !p.getString(KEY_MANAGER_PIN, "").isNullOrBlank()
    }

    fun saveManager(context: android.content.Context, name: String, phone: String, pin: String) {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        p.edit()
            .putString(KEY_MANAGER_NAME, name)
            .putString(KEY_MANAGER_PHONE, phone)
            .putString(KEY_MANAGER_PIN, pin)
            .putBoolean(KEY_MANAGER_AUTHENTICATED, true)
            .apply()
    }

    fun isAuthenticated(context: android.content.Context): Boolean {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        return p.getBoolean(KEY_MANAGER_AUTHENTICATED, false)
    }

    fun setAuthenticated(context: android.content.Context, value: Boolean) {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        p.edit().putBoolean(KEY_MANAGER_AUTHENTICATED, value).apply()
    }

    fun managerName(context: android.content.Context): String {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        return p.getString(KEY_MANAGER_NAME, "") ?: ""
    }

    fun validatePin(context: android.content.Context, pin: String): Boolean {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        return (p.getString(KEY_MANAGER_PIN, "") ?: "") == pin
    }

    fun loadDrivers(context: android.content.Context): List<CompanyDriver> {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val managerName = managerName(context)
        val managerSpecificKey = if (managerName.isBlank()) KEY_DRIVERS else "${KEY_DRIVERS}_${managerName}"
        fun parseDrivers(json: String): List<CompanyDriver> {
            val arr = JSONArray(json)
            val result = mutableListOf<CompanyDriver>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                result.add(
                    CompanyDriver(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        phone = o.optString("phone"),
                        carNumber = o.optString("carNumber")
                    )
                )
            }
            return result
        }

        return try {
            val managerJson = p.getString(managerSpecificKey, "[]") ?: "[]"
            val managerDrivers = parseDrivers(managerJson)
            if (managerDrivers.isNotEmpty() || managerName.isBlank()) return managerDrivers

            // ترحيل تلقائي: إذا كانت البيانات القديمة موجودة بالمفتاح العام، ننسخها لمفتاح المدير الحالي.
            val legacyJson = p.getString(KEY_DRIVERS, "[]") ?: "[]"
            val legacyDrivers = parseDrivers(legacyJson)
            if (legacyDrivers.isNotEmpty()) {
                p.edit().putString(managerSpecificKey, legacyJson).apply()
                return legacyDrivers
            }
            emptyList()
        } catch (_: Exception) {
            // لا نكسر التطبيق بسبب بيانات محلية تالفة من إصدار سابق.
            p.edit().putString(managerSpecificKey, "[]").apply()
            emptyList()
        }
    }

    fun saveDrivers(context: android.content.Context, drivers: List<CompanyDriver>) {
        val arr = JSONArray()
        drivers.forEach { d ->
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("name", d.name)
                    .put("phone", d.phone)
                    .put("carNumber", d.carNumber)
            )
        }
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val managerName = managerName(context)
        val managerSpecificKey = if (managerName.isBlank()) KEY_DRIVERS else "${KEY_DRIVERS}_${managerName}"
        p.edit().putString(managerSpecificKey, arr.toString()).apply()
    }

    fun loadExcelQtyAlertLimits(context: android.content.Context): Pair<Double, Double> {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val managerName = managerName(context)
        val minKey = if (managerName.isBlank()) KEY_EXCEL_QTY_ALERT_MIN_TON else "${KEY_EXCEL_QTY_ALERT_MIN_TON}_${managerName}"
        val maxKey = if (managerName.isBlank()) KEY_EXCEL_QTY_ALERT_MAX_TON else "${KEY_EXCEL_QTY_ALERT_MAX_TON}_${managerName}"
        val min = p.getString(minKey, null)?.toDoubleOrNull() ?: EXCEL_SUSPICIOUS_QTY_MIN_TON
        val max = p.getString(maxKey, null)?.toDoubleOrNull() ?: EXCEL_SUSPICIOUS_QTY_MAX_TON
        val normalizedMin = min.coerceAtLeast(0.0)
        val normalizedMax = if (max <= normalizedMin) normalizedMin + 1.0 else max
        return normalizedMin to normalizedMax
    }

    fun saveExcelQtyAlertLimits(context: android.content.Context, minTon: Double, maxTon: Double) {
        val normalizedMin = minTon.coerceAtLeast(0.0)
        val normalizedMax = if (maxTon <= normalizedMin) normalizedMin + 1.0 else maxTon
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val managerName = managerName(context)
        val minKey = if (managerName.isBlank()) KEY_EXCEL_QTY_ALERT_MIN_TON else "${KEY_EXCEL_QTY_ALERT_MIN_TON}_${managerName}"
        val maxKey = if (managerName.isBlank()) KEY_EXCEL_QTY_ALERT_MAX_TON else "${KEY_EXCEL_QTY_ALERT_MAX_TON}_${managerName}"
        p.edit()
            .putString(minKey, normalizedMin.toString())
            .putString(maxKey, normalizedMax.toString())
            .apply()
    }

    fun loadMaintenanceRecords(context: android.content.Context): List<CompanyMaintenanceRecord> {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val managerName = managerName(context)
        val managerSpecificKey = if (managerName.isBlank()) KEY_MAINTENANCE_RECORDS else "${KEY_MAINTENANCE_RECORDS}_${managerName}"
        val json = p.getString(managerSpecificKey, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<CompanyMaintenanceRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                result.add(
                    CompanyMaintenanceRecord(
                        id = o.optString("id"),
                        carNumber = o.optString("carNumber"),
                        assignedTo = o.optString("assignedTo"),
                        maintenanceType = o.optString("maintenanceType"),
                        dueDate = o.optString("dueDate"),
                        estimatedCost = o.optDouble("estimatedCost", 0.0),
                        status = o.optString("status"),
                        notes = o.optString("notes"),
                        createdAt = o.optLong("createdAt", 0L),
                    )
                )
            }
            result.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            val managerSpecificKey = if (managerName.isBlank()) KEY_MAINTENANCE_RECORDS else "${KEY_MAINTENANCE_RECORDS}_${managerName}"
            p.edit().putString(managerSpecificKey, "[]").apply()
            emptyList()
        }
    }

    fun saveMaintenanceRecords(context: android.content.Context, records: List<CompanyMaintenanceRecord>) {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("carNumber", r.carNumber)
                    .put("assignedTo", r.assignedTo)
                    .put("maintenanceType", r.maintenanceType)
                    .put("dueDate", r.dueDate)
                    .put("estimatedCost", r.estimatedCost)
                    .put("status", r.status)
                    .put("notes", r.notes)
                    .put("createdAt", r.createdAt)
            )
        }
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        val managerName = managerName(context)
        val managerSpecificKey = if (managerName.isBlank()) KEY_MAINTENANCE_RECORDS else "${KEY_MAINTENANCE_RECORDS}_${managerName}"
        p.edit().putString(managerSpecificKey, arr.toString()).apply()
    }

    fun activationCode(context: android.content.Context): String {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        return p.getString(KEY_COMPANY_ACTIVATION_CODE, "") ?: ""
    }

    fun saveActivationCode(context: android.content.Context, code: String) {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        p.edit().putString(KEY_COMPANY_ACTIVATION_CODE, code.trim()).apply()
    }

    fun companyId(context: android.content.Context): String {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        return p.getString(KEY_COMPANY_SCOPE_ID, "") ?: ""
    }

    fun saveCompanyId(context: android.content.Context, companyId: String) {
        val p = context.getSharedPreferences(PREF, android.content.Context.MODE_PRIVATE)
        p.edit().putString(KEY_COMPANY_SCOPE_ID, companyId.trim().uppercase(Locale.ROOT)).apply()
    }
}

object CompanyStatsRepository {
    // Some historical rows store quantity in kilograms (e.g. 18900 for 18.9 tons).
    private fun normalizeToTons(quantity: Double): Double {
        if (quantity <= 0.0) return 0.0
        return if (quantity in 1000.0..100000.0) quantity / 1000.0 else quantity
    }

    private fun normalizeNumericText(value: String): String {
        if (value.isBlank()) return ""
        val arabicDigits = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
            '٫' to '.', '،' to ',',
        )
        return buildString(value.length) {
            value.forEach { append(arabicDigits[it] ?: it) }
        }
    }

    private fun readNumber(json: JSONObject, key: String): Double {
        if (!json.has(key) || json.isNull(key)) return 0.0
        return try {
            when (val value = json.get(key)) {
                is Number -> value.toDouble()
                is String -> normalizeNumericText(value).replace(",", "").trim().toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
        } catch (_: Exception) {
            0.0
        }
    }

    private fun readNumber(json: JSONObject, vararg keys: String): Double {
        keys.forEach { key ->
            val value = readNumber(json, key)
            if (value != 0.0) return value
        }
        return 0.0
    }

    private fun readText(json: JSONObject, vararg keys: String): String {
        keys.forEach { key ->
            val value = json.optString(key).trim()
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun parseFactoryAmountFromNotes(notes: String): Double {
        if (notes.isBlank()) return 0.0
        val normalized = normalizeNumericText(notes)
        val amountPatterns = listOf(
            Regex("(?:إجمالي|الاجمالي|total)\\s*[:=]\\s*([0-9,]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE),
            Regex("(?:المبلغ|amount)\\s*[:=]\\s*([0-9,]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
        )
        amountPatterns.forEach { pattern ->
            val value = pattern.find(normalized)?.groupValues?.getOrNull(1)
                ?.replace(",", "")
                ?.trim()
                ?.toDoubleOrNull()
            if (value != null && value > 0.0) return value
        }
        return 0.0
    }

    // بعض السجلات القديمة تُخزن المبلغ مضروبًا في 1000 (مثل 669,175,000 بدل 669,175).
    // نعيد المبلغ إلى حجمه الطبيعي عندما يكون سعر الطن الناتج غير منطقي.
    private fun normalizeAmountByQuantity(amount: Double, quantityTon: Double): Double {
        if (amount <= 0.0 || quantityTon <= 0.0) return amount
        var normalized = amount
        var unitRate = normalized / quantityTon
        while (unitRate > 200_000.0 && normalized >= 1000.0) {
            normalized /= 1000.0
            unitRate = normalized / quantityTon
        }
        return normalized
    }

    private fun parseJsonResponseOrThrow(raw: String, action: String): JSONObject {
        val text = raw.trimStart()
        val isHtml = text.startsWith("<!doctype html", ignoreCase = true) || text.startsWith("<html", ignoreCase = true)
        if (isHtml) {
            throw IllegalStateException("الخادم أعاد صفحة HTML بدل JSON لطلب $action")
        }
        if (!text.startsWith("{") && !text.startsWith("[")) {
            throw IllegalStateException("استجابة غير متوقعة من الخادم لطلب $action")
        }
        return JSONObject(text)
    }

    suspend fun fetchDriverMonthlyReport(
        driverName: String,
        month: Int,
        year: Int,
        reportHalf: CompanyReportHalf = CompanyReportHalf.All,
        companyId: String = "",
        activationCode: String = "",
    ): DriverMonthlyReport {
        return withContext(Dispatchers.IO) {
            val m = String.format("%02d", month)
            val y = year.toString()
            val tripSheet = "${y}_${m}"
            val factorySheet = "F_${y}_${m}"

            val maxDay = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)

            val (fromDay, toDay, halfToken) = when (reportHalf) {
                CompanyReportHalf.All -> Triple(1, maxDay, "all")
                CompanyReportHalf.FirstHalf -> Triple(1, 15, "first")
                CompanyReportHalf.SecondHalf -> Triple(16, maxDay, "second")
            }
            val fromDate = String.format("%04d/%02d/%02d", year, month, fromDay)
            val toDate = String.format("%04d/%02d/%02d", year, month, toDay)

            val historyUrl = GoogleSheetConfig.execUrl(
                "history",
                "driverName" to driverName,
                "month" to m,
                "year" to y,
                "sheet" to tripSheet,
                "tripSheet" to tripSheet,
                "factorySheet" to factorySheet,
                "source" to "trip",
                "half" to halfToken,
                "day_from" to fromDay.toString(),
                "day_to" to toDay.toString(),
                "from_date" to fromDate,
                "to_date" to toDate,
                "start_date" to fromDate,
                "end_date" to toDate,
                "companyId" to companyId,
                "activationCode" to activationCode,
            )

            val json = parseJsonResponseOrThrow(URL(historyUrl).readText(), "history")
            val arr = json.optJSONArray("trips") ?: JSONArray()

            var trips = 0
            var tripQuantity = 0.0
            var liters = 0.0
            var tripAmount = 0.0
            var factoryTripsCount = 0
            var factoryQuantity = 0.0
            var factoryAmount = 0.0

            val halafayaTrips = mutableListOf<DriverTripRecord>()
            val factoryTrips = mutableListOf<DriverTripRecord>()

            fun resolveDisplayLoadDate(item: JSONObject): String {
                val preferred = readText(
                    item,
                    "loadDate",
                    "sendTime",
                    "timestamp",
                    "time",
                    "date",
                    "created_at",
                    "createdAt",
                    "fill_date",
                )
                if (preferred.isNotBlank()) return preferred
                return readText(item, "unloadDate", "date", "created_at", "createdAt", "fill_date")
            }

            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val docNum = readText(item, "docNumber")
                if (docNum.isBlank()) continue

                val rawQuantity = readText(item, "quantity")
                val factoryName = readText(item, "factory")
                val destination = readText(item, "destination", "station")
                val sourceType = readText(item, "source", "type")
                val sheetName = readText(item, "sheetName")

                val isFactory =
                    factoryName.isNotBlank() ||
                        destination.contains("معمل", ignoreCase = true) ||
                        sheetName.startsWith("F_", ignoreCase = true) ||
                        sheetName.startsWith("مع_") ||
                        rawQuantity.startsWith("http", ignoreCase = true) ||
                        sourceType.contains("factory", ignoreCase = true)

                if (reportHalf != CompanyReportHalf.All) {
                    val day = listOf(
                        readText(item, "unloadDate"),
                        readText(item, "loadDate"),
                        readText(item, "sendTime"),
                        readText(item, "timestamp"),
                        readText(item, "time"),
                        readText(item, "date"),
                        readText(item, "created_at"),
                        readText(item, "createdAt"),
                        readText(item, "fill_date")
                    ).firstNotNullOfOrNull { companyReportDayFromDate(it) }

                    // Keep undated factory rows in first-half reports to match server behavior.
                    if (day == null) {
                        if (!(isFactory && reportHalf == CompanyReportHalf.FirstHalf)) continue
                    }
                    if (reportHalf == CompanyReportHalf.FirstHalf && day !in 1..15) continue
                    if (reportHalf == CompanyReportHalf.SecondHalf && day !in 16..31) continue
                }

                fun scaleAmountIfQuantityInKg(amount: Double, sourceQuantity: Double): Double {
                    if (amount <= 0.0) return amount
                    if (sourceQuantity < 1000.0) return amount

                    // Apply /1000 only when the stored amount is clearly inflated.
                    // Example inflated row: 669,175,000 with qty 18,850 (kg) => rate 35,500 per kg.
                    // Example normal row:   160,225 with qty 18,850 (kg) => rate 8.5 per kg.
                    var normalized = amount
                    var unitRatePerKg = normalized / sourceQuantity
                    while (unitRatePerKg > 1000.0 && normalized >= 1000.0) {
                        normalized /= 1000.0
                        unitRatePerKg = normalized / sourceQuantity
                    }
                    return normalized
                }

                if (isFactory) {
                    val sourceQuantity = readNumber(item, "quantity", "finalQuantity", "qty")
                    val quantity = normalizeToTons(sourceQuantity)
                    val directAmount = readNumber(item, "price", "amount", "total", "finalAmount", "profit")
                    val notesAmount = parseFactoryAmountFromNotes(readText(item, "notes", "note", "remarks"))
                    val estimatedAmount = quantity * 8500.0
                    val rawLineAmount = when {
                        directAmount > 0.0 -> directAmount
                        notesAmount > 0.0 -> notesAmount
                        else -> estimatedAmount
                    }
                    val scaledLineAmount = scaleAmountIfQuantityInKg(rawLineAmount, sourceQuantity)
                    val lineAmount = normalizeAmountByQuantity(scaledLineAmount, quantity)

                    factoryTripsCount += 1
                    factoryQuantity += quantity
                    factoryAmount += lineAmount
                    factoryTrips += DriverTripRecord(
                        docNumber = docNum,
                        quantity = quantity,
                        destination = factoryName.ifBlank { "معمل" },
                        loadDate = resolveDisplayLoadDate(item),
                        unloadDate = readText(item, "unloadDate", "created_at", "createdAt", "fill_date")
                    )
                } else {
                    val sourceQuantity = readNumber(item, "quantity", "finalQuantity", "qty")
                    val quantity = normalizeToTons(sourceQuantity)
                    val gas = readNumber(item, "liters", "gas")
                    val rawAmount = readNumber(item, "price", "profit", "finalAmount")
                    val scaledAmount = scaleAmountIfQuantityInKg(rawAmount, sourceQuantity)
                    val amount = normalizeAmountByQuantity(scaledAmount, quantity)

                    trips += 1
                    tripQuantity += quantity
                    liters += gas
                    tripAmount += amount

                    halafayaTrips += DriverTripRecord(
                        docNumber = docNum,
                        quantity = quantity,
                        destination = destination.ifBlank { "محطة حلفاية" },
                        loadDate = resolveDisplayLoadDate(item),
                        unloadDate = readText(item, "unloadDate", "created_at", "createdAt", "fill_date")
                    )
                }
            }

            DriverMonthlyReport(
                stats = DriverMonthlyStats(
                    trips = trips,
                    tripQuantity = tripQuantity,
                    liters = liters,
                    tripAmount = tripAmount,
                    factoryTrips = factoryTripsCount,
                    factoryQuantity = factoryQuantity,
                    factoryAmount = factoryAmount
                ),
                halafayaTrips = halafayaTrips,
                factoryTrips = factoryTrips
            )
        }
    }

    suspend fun fetchDriverMonthlyStats(
        driverName: String,
        month: Int,
        year: Int,
        reportHalf: CompanyReportHalf = CompanyReportHalf.All,
        companyId: String = "",
        activationCode: String = "",
    ): DriverMonthlyStats {
        return fetchDriverMonthlyReport(driverName, month, year, reportHalf, companyId, activationCode).stats
    }


}

class CompanyMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DriverPortalTheme {
                CompanyApp()
            }
        }
    }
}

@Composable
private fun CompanyApp() {
    val context = LocalContext.current
    var versionPolicy by remember { mutableStateOf<CompanyVersionPolicy?>(null) }
    var policyError by remember { mutableStateOf<String?>(null) }
    var activationCode by remember { mutableStateOf(CompanyPrefs.activationCode(context)) }
    var activationPolicy by remember { mutableStateOf<CompanyActivationPolicy?>(null) }
    var activationError by remember { mutableStateOf<String?>(null) }
    var activationLoading by remember { mutableStateOf(false) }
    var hasManager by remember { mutableStateOf(CompanyPrefs.hasManager(context)) }
    var authenticated by remember { mutableStateOf(CompanyPrefs.isAuthenticated(context)) }

    LaunchedEffect(Unit) {
        runCatching { CompanyVersionPolicyRepository.fetch() }
            .onSuccess { versionPolicy = it }
            .onFailure { error ->
                policyError = error.message ?: "تعذر التحقق من حالة النسخة"
                versionPolicy = CompanyVersionPolicy(
                    allowed = false,
                    title = "تعذر التحقق من النسخة",
                    message = "يجب توفر اتصال بالإنترنت للتحقق من صلاحية تطبيق مدير الحسابات.",
                    supportPhone = "07809830249",
                    latestVersionName = BuildConfig.VERSION_NAME,
                )
            }
    }

    LaunchedEffect(versionPolicy?.allowed, activationCode) {
        if (versionPolicy?.allowed != true) return@LaunchedEffect
        if (activationCode.isBlank()) {
            activationPolicy = null
            activationError = null
            return@LaunchedEffect
        }

        activationLoading = true
        runCatching { CompanyActivationRepository.verify(context, activationCode) }
            .onSuccess { result ->
                activationPolicy = result
                if (result.companyId.isNotBlank()) {
                    CompanyPrefs.saveCompanyId(context, result.companyId)
                }
                activationError = if (result.allowed) null else result.message
            }
            .onFailure { error ->
                activationPolicy = null
                activationError = error.message ?: "تعذر التحقق من التفعيل"
            }
        activationLoading = false
    }

    val policy = versionPolicy
    if (policy == null) {
        CompanyVersionLoadingScreen()
        return
    }

    if (!policy.allowed) {
        CompanyVersionBlockedScreen(policy = policy, technicalMessage = policyError)
        return
    }

    if (activationLoading) {
        CompanyActivationLoadingScreen()
        return
    }

    val activationAllowed = activationPolicy?.allowed == true
    if (!activationAllowed) {
        CompanyActivationScreen(
            savedCode = activationCode,
            serverMessage = activationError ?: activationPolicy?.message,
            onActivated = { code, result ->
                CompanyPrefs.saveActivationCode(context, code)
                if (result.companyId.isNotBlank()) {
                    CompanyPrefs.saveCompanyId(context, result.companyId)
                }
                activationCode = code
                activationPolicy = result
                activationError = null
            }
        )
        return
    }

    if (!hasManager) {
        if (authenticated) {
            CompanyPrefs.setAuthenticated(context, false)
            authenticated = false
        }
        ManagerSetupScreen(onManagerSaved = { hasManager = true; authenticated = true })
        return
    }

    if (!authenticated) {
        ManagerLoginScreen(managerName = CompanyPrefs.managerName(context), onLoggedIn = { authenticated = true })
        return
    }

    CompanyDashboard(onLogout = {
        CompanyPrefs.setAuthenticated(context, false)
        authenticated = false
    })
}

@Composable
private fun CompanyActivationLoadingScreen() {
    CompanyScreenBackground {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ProGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text("جارٍ التحقق من كود التفعيل لهذا الجهاز", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Text("لا يمكن الدخول قبل موافقة الإدارة وربط الكود بجهاز واحد.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CompanyActivationScreen(
    savedCode: String,
    serverMessage: String?,
    onActivated: (String, CompanyActivationPolicy) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember(savedCode) { mutableStateOf(savedCode) }
    var loading by remember { mutableStateOf(false) }
    var message by remember(serverMessage) { mutableStateOf(serverMessage ?: "") }

    CompanyScreenBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "تفعيل تطبيق الشركات",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 28.sp
            )
            Text(
                "أدخل كود التفعيل الصادر من الإدارة. كل كود يعمل على جهاز واحد فقط.",
                color = ProOrange(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )

            ProGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.trim().uppercase(Locale.ROOT) },
                        label = { Text("كود التفعيل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = companyOutlinedTextFieldColors()
                    )

                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    CompanyActionButton(
                        text = if (loading) "جارٍ التحقق..." else "تفعيل الآن",
                        icon = Icons.Default.Person,
                        containerColor = Color(0xFF0EA5E9),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                        onClick = {
                            val normalized = code.trim().replace(" ", "")
                            if (normalized.isBlank()) {
                                message = "يرجى إدخال كود التفعيل"
                                return@CompanyActionButton
                            }
                            loading = true
                            scope.launch {
                                runCatching { CompanyActivationRepository.verify(context, normalized) }
                                    .onSuccess { result ->
                                        if (result.allowed) {
                                            onActivated(normalized, result)
                                        } else {
                                            message = result.message
                                        }
                                    }
                                    .onFailure { err ->
                                        message = err.message ?: "تعذر الاتصال بالخادم"
                                    }
                                loading = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyVersionLoadingScreen() {
    CompanyScreenBackground {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ProGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text("جارٍ التحقق من صلاحية النسخة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Text("لن يتم فتح لوحة الشركات قبل التحقق من حالة التشغيل.", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CompanyVersionBlockedScreen(policy: CompanyVersionPolicy, technicalMessage: String?) {
    CompanyScreenBackground {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            ProGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(policy.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
                    Text(policy.message, color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text("نسختك: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Text("آخر نسخة معتمدة: ${policy.latestVersionName}", color = Color.White.copy(alpha = 0.70f), fontSize = 12.sp, textAlign = TextAlign.Center)
                    Text("للتفعيل: ${policy.supportPhone}", color = Color(0xFF5EEAD4), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.Center)
                    technicalMessage?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerSetupScreen(onManagerSaved: () -> Unit) {
    val context = LocalContext.current
    var managerName by remember { mutableStateOf("") }
    var managerPhone by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    CompanyScreenBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("تهيئة مدير الشركة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 28.sp)
            Text("مدير واحد لإدارة كاملة", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)

            ProGlassCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = Color(0xFF14B8A6).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.45f))
                    ) {
                        Text(
                            "إعداد أولي آمن للتحكم الإداري",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(value = managerName, onValueChange = { managerName = it }, label = { Text("اسم المدير") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                    OutlinedTextField(value = managerPhone, onValueChange = { managerPhone = it.filter { ch -> ch.isDigit() || ch == '+' } }, label = { Text("هاتف المدير") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                    OutlinedTextField(value = pin, onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) }, label = { Text("رمز الدخول (6 أرقام)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                    OutlinedTextField(value = confirmPin, onValueChange = { confirmPin = it.filter { ch -> ch.isDigit() }.take(6) }, label = { Text("تأكيد الرمز") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())

                    CompanyActionButton(
                        text = "حفظ المدير",
                        icon = Icons.Default.Add,
                        containerColor = Color(0xFF14B8A6),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (managerName.trim().isEmpty() || managerPhone.trim().isEmpty() || pin.length < 4 || pin != confirmPin) {
                                Toast.makeText(context, "تحقق من البيانات", Toast.LENGTH_LONG).show()
                            } else {
                                CompanyPrefs.saveManager(context, managerName.trim(), managerPhone.trim(), pin)
                                onManagerSaved()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ManagerLoginScreen(managerName: String, onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }

    CompanyScreenBackground {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("مدير الشركة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 28.sp)
            Text("أهلاً: $managerName", color = ProOrange(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

            ProGlassCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = Color(0xFF38BDF8).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
                    ) {
                        Text(
                            "تسجيل دخول الإدارة",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(value = pin, onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) }, label = { Text("أدخل رمز الدخول") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())

                    CompanyActionButton(
                        text = "دخول",
                        icon = Icons.Default.Person,
                        containerColor = Color(0xFF0EA5E9),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (CompanyPrefs.validatePin(context, pin)) {
                                CompanyPrefs.setAuthenticated(context, true)
                                onLoggedIn()
                            } else {
                                Toast.makeText(context, "الرمز غير صحيح", Toast.LENGTH_LONG).show()
                                pin = ""
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompanyDashboard(onLogout: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var drivers by remember { mutableStateOf(CompanyPrefs.loadDrivers(context)) }
    var maintenanceRecords by remember { mutableStateOf(CompanyPrefs.loadMaintenanceRecords(context)) }

    val tabs = listOf("السواق", "وصل حلفاية", "وصل المعمل", "لوحة السائق", "لوحة الصيانة", "التقارير")

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("نسخة الإدارة", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    Text("لوحة تشغيل الشركات", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                }
            },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            actions = {
                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFF59E0B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("خروج", color = Color.White, fontSize = 12.sp)
                }
            }
        )
    }) { padding ->
        CompanyScreenBackground {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    val active = selectedTab == index
                                    OutlinedButton(
                                        onClick = { selectedTab = index },
                                        modifier = Modifier.height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White,
                                            containerColor = if (active) Color(0xFF2E556E).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.04f)
                                        ),
                                        border = BorderStroke(1.dp, if (active) Color(0xFF63D2C8).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.24f)),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            title,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Visible,
                                            fontSize = 12.sp,
                                            color = if (active) Color.White else Color.White.copy(alpha = 0.86f),
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> CompanyDriversScreen(drivers = drivers) { drivers = it; CompanyPrefs.saveDrivers(context, it) }
                        1 -> CompanyTripScreen(drivers = drivers)
                        2 -> CompanyFactoryScreen(drivers = drivers)
                        3 -> CompanyDriverOverviewScreen(drivers = drivers)
                        4 -> CompanyMaintenanceScreen(
                            drivers = drivers,
                            records = maintenanceRecords,
                            onRecordsChanged = {
                                maintenanceRecords = it
                                CompanyPrefs.saveMaintenanceRecords(context, it)
                            }
                        )
                        5 -> CompanyMonthlyReportScreen(drivers = drivers)
                    }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyMaintenanceScreen(
    drivers: List<CompanyDriver>,
    records: List<CompanyMaintenanceRecord>,
    onRecordsChanged: (List<CompanyMaintenanceRecord>) -> Unit,
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var expandedDriver by remember { mutableStateOf(false) }
    var selectedDriverId by remember { mutableStateOf("") }
    var maintenanceType by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var estimatedCostText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("مجدولة") }
    var expandedStatus by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf("الكل") }

    fun normalizeNumeric(value: String): String {
        val arabicDigits = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
            '٫' to '.', '،' to '.',
        )
        return buildString(value.length) {
            value.forEach { append(arabicDigits[it] ?: it) }
        }
    }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(
            context,
            { _, year, month, day -> onDateSelected("$year-${month + 1}-$day") },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val selectedDriver = drivers.firstOrNull { it.id == selectedDriverId }
    val availableCars = drivers.map { it.carNumber }.distinct()
    val statuses = listOf("مجدولة", "جارية", "مكتملة")

    val filteredRecords = records.filter {
        statusFilter == "الكل" || it.status == statusFilter
    }

    val scheduledCount = records.count { it.status == "مجدولة" }
    val inProgressCount = records.count { it.status == "جارية" }
    val completedCount = records.count { it.status == "مكتملة" }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProGlassCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("لوحة الصيانة", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Text("تسجيل ومتابعة صيانة سيارات الشركة", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                if (drivers.isEmpty()) {
                    Text("أضف السواق أولاً حتى تظهر السيارات في الصيانة", color = Color(0xFFFFD27A), fontSize = 12.sp)
                }

                Box {
                    OutlinedButton(
                        onClick = { expandedDriver = true },
                        enabled = drivers.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            selectedDriver?.let { "${it.name} - ${it.carNumber}" } ?: "اختر السائق/السيارة",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End,
                            color = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = expandedDriver,
                        onDismissRequest = { expandedDriver = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        drivers.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d.name} - ${d.carNumber}", color = Color.White) },
                                onClick = { selectedDriverId = d.id; expandedDriver = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = maintenanceType,
                    onValueChange = { maintenanceType = it },
                    label = { Text("نوع الصيانة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = companyOutlinedTextFieldColors()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("تاريخ الاستحقاق") },
                        modifier = Modifier.weight(1f),
                        colors = companyOutlinedTextFieldColors()
                    )
                    OutlinedButton(
                        onClick = { showDatePicker { dueDate = it } },
                        modifier = Modifier.width(92.dp).height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) { Text("اختر", color = Color.White) }
                }

                OutlinedTextField(
                    value = estimatedCostText,
                    onValueChange = { estimatedCostText = normalizeNumeric(it).filter { ch -> ch.isDigit() || ch == '.' }.take(14) },
                    label = { Text("التكلفة التقديرية") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = companyOutlinedTextFieldColors()
                )

                Box {
                    OutlinedButton(
                        onClick = { expandedStatus = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Text("الحالة: $selectedStatus", color = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedStatus,
                        onDismissRequest = { expandedStatus = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        statuses.forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status, color = Color.White) },
                                onClick = { selectedStatus = status; expandedStatus = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = companyOutlinedTextFieldColors()
                )

                CompanyActionButton(
                    text = "إضافة مهمة صيانة",
                    icon = Icons.Default.Add,
                    containerColor = Color(0xFF14B8A6),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val driver = selectedDriver
                        val cost = estimatedCostText.toDoubleOrNull() ?: 0.0
                        if (driver == null) {
                            Toast.makeText(context, "اختر السائق/السيارة", Toast.LENGTH_LONG).show()
                            return@CompanyActionButton
                        }
                        if (maintenanceType.trim().isBlank() || dueDate.isBlank()) {
                            Toast.makeText(context, "أدخل نوع الصيانة وتاريخ الاستحقاق", Toast.LENGTH_LONG).show()
                            return@CompanyActionButton
                        }

                        val newRecord = CompanyMaintenanceRecord(
                            id = System.currentTimeMillis().toString(),
                            carNumber = driver.carNumber,
                            assignedTo = driver.name,
                            maintenanceType = maintenanceType.trim(),
                            dueDate = dueDate,
                            estimatedCost = cost,
                            status = selectedStatus,
                            notes = notes.trim(),
                            createdAt = System.currentTimeMillis(),
                        )

                        onRecordsChanged((records + newRecord).sortedByDescending { it.createdAt })
                        maintenanceType = ""
                        dueDate = ""
                        estimatedCostText = ""
                        notes = ""
                        selectedStatus = "مجدولة"
                        Toast.makeText(context, "تمت إضافة مهمة الصيانة", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        ProGlassCard {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ملخص الصيانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProMetricCard("مجدولة", scheduledCount.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                    ProMetricCard("جارية", inProgressCount.toString(), Color(0xFF0EA5E9), Modifier.weight(1f))
                    ProMetricCard("مكتملة", completedCount.toString(), Color(0xFF22C55E), Modifier.weight(1f))
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    var expandedFilter by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { expandedFilter = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                    ) {
                        Text("فلتر الحالة: $statusFilter", color = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedFilter,
                        onDismissRequest = { expandedFilter = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        listOf("الكل")
                            .plus(statuses)
                            .forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status, color = Color.White) },
                                    onClick = {
                                        statusFilter = status
                                        expandedFilter = false
                                    }
                                )
                            }
                    }
                }
            }
        }

        if (filteredRecords.isEmpty()) {
            ProGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("لا توجد مهام صيانة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("أضف أول مهمة صيانة للسيارات", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredRecords.forEach { record ->
                    val statusColor = when (record.status) {
                        "مكتملة" -> Color(0xFF22C55E)
                        "جارية" -> Color(0xFF0EA5E9)
                        else -> Color(0xFFF59E0B)
                    }
                    ProGlassCard {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(record.maintenanceType, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("السيارة: ${record.carNumber}", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                                    Text("المسؤول: ${record.assignedTo}", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                                }

                                Surface(
                                    color = statusColor.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.55f))
                                ) {
                                    Text(
                                        record.status,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text("الاستحقاق: ${record.dueDate}", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                            Text("التكلفة التقديرية: ${formatAmountReadable(record.estimatedCost)} د.ع", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                            if (record.notes.isNotBlank()) {
                                Text("ملاحظات: ${record.notes}", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                statuses.forEach { nextStatus ->
                                    val isSelected = record.status == nextStatus
                                    OutlinedButton(
                                        onClick = {
                                            val updated = records.map {
                                                if (it.id == record.id) it.copy(status = nextStatus) else it
                                            }
                                            onRecordsChanged(updated.sortedByDescending { it.createdAt })
                                        },
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White,
                                            containerColor = if (isSelected) statusColor.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.04f)
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) statusColor else Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Text(nextStatus, fontSize = 11.sp)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    onRecordsChanged(records.filterNot { it.id == record.id })
                                    Toast.makeText(context, "تم حذف المهمة", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF8A8A),
                                    containerColor = Color(0xFFFF8A8A).copy(alpha = 0.08f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFF8A8A).copy(alpha = 0.45f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حذف المهمة")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProGlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.22f),
                    Color.White.copy(alpha = 0.06f)
                )
            ),
            shape = RoundedCornerShape(20.dp)
        ),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0E2A39).copy(alpha = 0.64f),
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f),
                    Color.Black.copy(alpha = 0.10f)
                )
            )
        )) {
            content()
        }
    }
}

@Composable
private fun ProBg(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B1F2A),
            Color(0xFF12374A),
            Color(0xFF072333)
        )
    )
}

@Composable
private fun CompanyScreenBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.driver_dash_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC071A27),
                            Color(0xD6102D42),
                            Color(0xE6051B2C)
                        )
                    )
                )
        )

        content()
    }
}

private fun ProBlue() = Color(0xFF0B7A92)
private fun ProOrange() = Color(0xFF38B7C6)
private fun ProPurple() = Color(0xFF5FA8D3)
private fun ProGreen() = Color(0xFF2FBF9D)
private fun ProRed() = Color(0xFFFF6B6B)

@Composable
private fun companyOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.92f),
    disabledTextColor = Color.White.copy(alpha = 0.65f),
    focusedLabelColor = Color(0xFF9BE7DF),
    unfocusedLabelColor = Color.White.copy(alpha = 0.84f),
    focusedBorderColor = Color(0xFF63D2C8),
    unfocusedBorderColor = Color.White.copy(alpha = 0.46f),
    cursorColor = Color.White,
    focusedLeadingIconColor = Color(0xFF9BE7DF),
    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.82f),
    focusedContainerColor = Color.White.copy(alpha = 0.10f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
)

@Composable
private fun companySelectionButtonColors() = ButtonDefaults.outlinedButtonColors(
    contentColor = Color.White,
    containerColor = Color.White.copy(alpha = 0.06f),
    disabledContentColor = Color.White.copy(alpha = 0.55f),
    disabledContainerColor = Color.White.copy(alpha = 0.04f),
)

@Composable
private fun formatDisplayDate(raw: String): String {
    if (raw.isBlank()) return "—"
    // Handle formats: "2026-6-14", "2026-06-14", "14/6/2026", "2026/06/14"
    return try {
        val normalized = raw.trim()

        // Handle long Date.toString-style values, e.g.:
        // Sun Jun 14 2026 00:00:00 GMT+0300 (Arabian Standard Time)
        val cleanedLong = normalized.replace(Regex("\\s*\\(.*\\)$"), "")
        val longMatch = Regex(
            "^[A-Za-z]{3}\\s+([A-Za-z]{3})\\s+(\\d{1,2})\\s+(\\d{4})\\s+\\d{2}:\\d{2}:\\d{2}\\s+GMT[+-]\\d{4}$"
        ).find(cleanedLong)
        if (longMatch != null) {
            val monthMap = mapOf(
                "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4,
                "May" to 5, "Jun" to 6, "Jul" to 7, "Aug" to 8,
                "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12,
            )
            val month = monthMap[longMatch.groupValues[1]]
            val day = longMatch.groupValues[2].toIntOrNull()
            val year = longMatch.groupValues[3].toIntOrNull()
            if (month != null && day != null && year != null) {
                return "$day/$month/$year"
            }
        }

        val parts = when {
            normalized.contains('-') -> normalized.split('-')
            normalized.contains('/') -> normalized.split('/')
            else -> return normalized
        }
        if (parts.size == 3) {
            val (a, b, c) = parts
            // Detect if first part is year (length 4) → yyyy-mm-dd
            if (a.length == 4) {
                val y = a; val m = b.toIntOrNull() ?: return normalized; val d = c.toIntOrNull() ?: return normalized
                "$d/$m/$y"
            } else {
                // dd/mm/yyyy
                val d = a.toIntOrNull() ?: return normalized; val m = b.toIntOrNull() ?: return normalized; val y = c
                "$d/$m/$y"
            }
        } else normalized
    } catch (_: Exception) { raw }
}

@Composable
private fun TripRecordsSection(
    title: String,
    records: List<DriverTripRecord>,
    accent: Color,
    emptyText: String,
) {
    ProGlassCard {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = accent, fontSize = 15.sp)
            if (records.isEmpty()) {
                Text(emptyText, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            } else {
                records.forEachIndexed { index, trip ->
                    if (index > 0) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f), thickness = 1.dp)
                    }
                    // سطر واحد: رقم الوصل | الوجهة | الكمية | تاريخ التحميل
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            // السطر الأول: رقم الوصل والوجهة
                            Text(
                                "${trip.destination.ifBlank { "—" }}  |  وصل ${trip.docNumber.ifBlank { "—" }}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            // السطر الثاني: الكمية + تاريخ التحميل
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "${if (trip.quantity % 1.0 == 0.0) trip.quantity.toLong().toString() else "%.2f".format(trip.quantity)} طن",
                                    color = accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val dateText = formatDisplayDate(trip.loadDate)
                                Text("•", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp)
                                Text(
                                    "التاريخ: $dateText",
                                    color = Color(0xFF7DD3FC),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyDriversScreen(drivers: List<CompanyDriver>, onDriversChanged: (List<CompanyDriver>) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var carNumber by remember { mutableStateOf("") }
    var editingDriverId by remember { mutableStateOf<String?>(null) }
    val driversPrimary = Color(0xFF14B8A6)
    val driversInfo = Color(0xFF38BDF8)
    val driversDanger = Color(0xFFEF4444)

    fun resetForm() {
        editingDriverId = null
        name = ""
        phone = ""
        carNumber = ""
    }

    fun isCarNumberLimitExceededForSave(targetCar: String): Boolean {
        if (targetCar.isBlank()) return false
        val count = drivers.count {
            it.carNumber.equals(targetCar, ignoreCase = true) && it.id != editingDriverId
        }
        return count >= 2
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProGlassCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (editingDriverId == null) "إضافة سائق جديد" else "تعديل بيانات السائق", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Text("إدارة السواق والسيارات المعتمدة للتشغيل", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                Surface(
                    color = driversPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, driversPrimary.copy(alpha = 0.5f))
                ) {
                    Text(
                        "عدد السواق الحالي: ${drivers.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم السائق") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                OutlinedTextField(value = phone, onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' } }, label = { Text("الهاتف") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                OutlinedTextField(value = carNumber, onValueChange = { carNumber = it }, label = { Text("رقم السيارة") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())

                CompanyActionButton(
                    text = if (editingDriverId == null) "حفظ السائق" else "حفظ التعديل",
                    icon = if (editingDriverId == null) Icons.Default.Add else Icons.Default.Edit,
                    containerColor = driversPrimary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val cleanName = name.trim()
                        val cleanPhone = phone.trim()
                        val cleanCar = carNumber.trim()
                        if (cleanName.isEmpty() || cleanPhone.isEmpty() || cleanCar.isEmpty()) {
                            Toast.makeText(context, "املأ جميع الحقول", Toast.LENGTH_LONG).show()
                        } else if (isCarNumberLimitExceededForSave(cleanCar)) {
                            Toast.makeText(context, "لا يمكن تكرار السيارة أكثر من سائقين", Toast.LENGTH_LONG).show()
                        } else {
                            val targetId = editingDriverId
                            if (targetId == null) {
                                onDriversChanged(drivers + CompanyDriver(id = System.currentTimeMillis().toString(), name = cleanName, phone = cleanPhone, carNumber = cleanCar))
                            } else {
                                onDriversChanged(
                                    drivers.map {
                                        if (it.id == targetId) {
                                            it.copy(name = cleanName, phone = cleanPhone, carNumber = cleanCar)
                                        } else {
                                            it
                                        }
                                    }
                                )
                            }
                            resetForm()
                        }
                    }
                )

                if (editingDriverId != null) {
                    OutlinedButton(
                        onClick = { resetForm() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        Text("إلغاء التعديل")
                    }
                }
            }
        }

        if (drivers.isEmpty()) {
            ProGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("لا يوجد سواق", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("ابدأ بإضافة أول سائق مع رقم السيارة", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                drivers.forEach { driver ->
                    ProGlassCard {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                                Text(driver.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text(driver.phone, fontSize = 12.sp, color = driversInfo)
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                ) {
                                    Text(
                                        "رقم السيارة: ${driver.carNumber}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.82f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    editingDriverId = driver.id
                                    name = driver.name
                                    phone = driver.phone
                                    carNumber = driver.carNumber
                                },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = driversInfo,
                                    containerColor = driversInfo.copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(1.dp, driversInfo.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = driversInfo, modifier = Modifier.size(22.dp))
                            }

                            OutlinedButton(
                                onClick = { onDriversChanged(drivers.filterNot { it.id == driver.id }) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = driversDanger,
                                    containerColor = driversDanger.copy(alpha = 0.12f)
                                ),
                                border = BorderStroke(1.dp, driversDanger.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = driversDanger, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyTripScreen(drivers: List<CompanyDriver>) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var expandedDriver by remember { mutableStateOf(false) }
    var selectedDriverId by remember { mutableStateOf("") }
    var docNumber by remember { mutableStateOf("") }
    var station by remember { mutableStateOf("") }
    var loadDate by remember { mutableStateOf("") }
    var unloadDate by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var liters by remember { mutableStateOf("") }
    var ownerType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageData by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var expandedStation by remember { mutableStateOf(false) }
    val tripTonPrice = 35500L
    val tripPrimary = Color(0xFF14B8A6)
    val tripSecondary = Color(0xFF0EA5E9)

    fun normalizeNumeric(value: String): String {
        val arabicDigits = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
        )
        return buildString {
            value.forEach { append(arabicDigits[it] ?: it) }
        }
    }

    val quantityValue = normalizeNumeric(quantity).toDoubleOrNull() ?: 0.0
    val tripTotalAmount = (quantityValue * tripTonPrice).toLong()

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(context, { _, year, month, day -> onDateSelected("$year-${month + 1}-$day") }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            val resized = Bitmap.createScaledBitmap(bitmap, (bitmap.width / 2).coerceAtLeast(1), (bitmap.height / 2).coerceAtLeast(1), true)
            val stream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val bytes = stream.toByteArray()
            imageData = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val stream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = stream.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    imageBitmap = bitmap
                    val resized = Bitmap.createScaledBitmap(bitmap, (bitmap.width / 2).coerceAtLeast(1), (bitmap.height / 2).coerceAtLeast(1), true)
                    val out = ByteArrayOutputStream()
                    resized.compress(Bitmap.CompressFormat.JPEG, 70, out)
                    val bytes = out.toByteArray()
                    imageData = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (_: Exception) {
                Toast.makeText(context, "تعذر تحميل الصورة", Toast.LENGTH_LONG).show()
            }
        }
    }

    val selectedDriver = drivers.firstOrNull { it.id == selectedDriverId }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (drivers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("أضف السواق أولاً", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
            }
            return
        }

        ProGlassCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("وصل حلفاية", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Text("إدخال بيانات نقل المحطات مع صورة الوصل", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                Box {
                    OutlinedButton(
                        onClick = { expandedDriver = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            selectedDriver?.let { it.name } ?: "اختر السائق",
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = expandedDriver,
                        onDismissRequest = { expandedDriver = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        drivers.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d.name} - ${d.carNumber}", color = Color.White) },
                                onClick = { selectedDriverId = d.id; expandedDriver = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = docNumber, onValueChange = { docNumber = it }, label = { Text("رقم وصل حلفاية") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())

                Box {
                    OutlinedButton(
                        onClick = { expandedStation = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Text(if (station.isBlank()) "اختر المحطة" else station, color = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedStation,
                        onDismissRequest = { expandedStation = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        CompanyConstants.STATIONS.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s, color = Color.White) },
                                onClick = { station = s; expandedStation = false }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = loadDate, onValueChange = {}, readOnly = true, label = { Text("التحميل") }, modifier = Modifier.weight(1f), colors = companyOutlinedTextFieldColors())
                    OutlinedButton(
                        onClick = { showDatePicker { loadDate = it } },
                        modifier = Modifier.width(92.dp).height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) { Text("اختر", color = Color.White) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = unloadDate, onValueChange = {}, readOnly = true, label = { Text("التفريغ") }, modifier = Modifier.weight(1f), colors = companyOutlinedTextFieldColors())
                    OutlinedButton(
                        onClick = { showDatePicker { unloadDate = it } },
                        modifier = Modifier.width(92.dp).height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) { Text("اختر", color = Color.White) }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("الكمية") }, modifier = Modifier.weight(1f), singleLine = true, colors = companyOutlinedTextFieldColors())
                    OutlinedTextField(value = liters, onValueChange = { liters = it }, label = { Text("الكاز") }, modifier = Modifier.weight(1f), singleLine = true, colors = companyOutlinedTextFieldColors())
                }

                OutlinedTextField(value = ownerType, onValueChange = { ownerType = it }, label = { Text("المالك") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                OutlinedTextField(value = "%,d".format(tripTonPrice), onValueChange = {}, readOnly = true, label = { Text("سعر الطن") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                OutlinedTextField(value = "%,d".format(tripTotalAmount), onValueChange = {}, readOnly = true, label = { Text("إجمالي الكمية") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth(), colors = companyOutlinedTextFieldColors())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CompanyActionButton(
                        text = "تصوير",
                        icon = Icons.Default.PhotoCamera,
                        containerColor = tripSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { cameraLauncher.launch(null) }
                    )
                    CompanyActionButton(
                        text = "تحميل",
                        icon = Icons.Default.FileUpload,
                        containerColor = ProBlue(),
                        modifier = Modifier.weight(1f),
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                }

                if (imageBitmap != null) {
                    Image(bitmap = imageBitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1F2937)))
                }

                Button(onClick = {
                    val driver = selectedDriver
                    val cleanDoc = DocNumberGuard.normalize(docNumber)
                    val scopeCompanyId = CompanyPrefs.companyId(context)
                    val scopeActivationCode = CompanyPrefs.activationCode(context)
                    if (driver == null) { Toast.makeText(context, "اختر السائق", Toast.LENGTH_LONG).show(); return@Button }
                    if (scopeCompanyId.isBlank() || scopeActivationCode.isBlank()) {
                        Toast.makeText(context, "التفعيل غير مكتمل: تحقق من companyId وكود التفعيل", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (cleanDoc.isBlank() || station.isBlank() || loadDate.isBlank() || quantity.isBlank() || ownerType.isBlank() || imageData.isBlank()) {
                        Toast.makeText(context, "أكمل البيانات المطلوبة", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    sending = true

                    fun submitHalafayaNow() {
                        val trip = TripRequest(
                            docNumber = cleanDoc,
                            driverName = driver.name,
                            carNumber = driver.carNumber,
                            quantity = quantity,
                            loadDate = loadDate,
                            unloadDate = unloadDate,
                            liters = liters,
                            ownerType = ownerType,
                            destination = station,
                            factory = "",
                            bojer = "",
                            notes = notes,
                            price = tripTotalAmount.toString(),
                            companyId = scopeCompanyId,
                            activationCode = scopeActivationCode,
                            fileData = imageData
                        )
                        // Register locally BEFORE sending to prevent duplicates even if send fails
                        DocNumberGuard.markUsed(context, cleanDoc)
                        TripRepository.sendTrip(trip, companyId = scopeCompanyId, activationCode = scopeActivationCode, onSuccess = {
                            sending = false
                            Toast.makeText(context, "تم إرسال الوصل", Toast.LENGTH_LONG).show()
                            docNumber = ""; station = ""; loadDate = ""; unloadDate = ""; quantity = ""; liters = ""; ownerType = ""; notes = ""; imageBitmap = null; imageData = ""
                        }, onError = { sending = false; Toast.makeText(context, it, Toast.LENGTH_LONG).show() })
                    }

                    TripRepository.checkDocNumber(docNumber = cleanDoc, driverName = driver.name, carNumber = driver.carNumber, companyId = scopeCompanyId, activationCode = scopeActivationCode, onResult = { result ->
                        when (result) {
                            TripRepository.DocCheckResult.EXISTS -> { sending = false; Toast.makeText(context, "الوصل موجود", Toast.LENGTH_LONG).show() }
                            TripRepository.DocCheckResult.UNVERIFIED -> {
                                if (DocNumberGuard.isUsedLocally(context, cleanDoc)) {
                                    sending = false
                                    Toast.makeText(context, "رقم الوصل مستخدم مسبقاً (تحقق محلي)", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "تعذر التحقق من السيرفر، تم الاعتماد على الحماية المحلية", Toast.LENGTH_LONG).show()
                                    submitHalafayaNow()
                                }
                            }
                            TripRepository.DocCheckResult.AVAILABLE -> submitHalafayaNow()
                        }
                    })
                }, enabled = !sending, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = tripPrimary)) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White) else {
                        Icon(Icons.Default.LocalShipping, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("إرسال وصل حلفاية", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyFactoryScreen(drivers: List<CompanyDriver>) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var expandedDriver by remember { mutableStateOf(false) }
    var selectedDriverId by remember { mutableStateOf("") }
    var docNumber by remember { mutableStateOf("") }
    var loadDate by remember { mutableStateOf("") }
    var unloadDate by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var factoryName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageData by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val factoryTonPrice = 8500L
    val factoryPrimary = Color(0xFF0EA5E9)
    val factorySecondary = Color(0xFFF59E0B)

    fun normalizeNumeric(value: String): String {
        val arabicDigits = mapOf(
            '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
            '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9'
        )
        return buildString {
            value.forEach { append(arabicDigits[it] ?: it) }
        }
    }

    val quantityValue = normalizeNumeric(quantity).toDoubleOrNull() ?: 0.0
    val factoryTotalAmount = (quantityValue * factoryTonPrice).toLong()
    val factories = remember { FactoryCatalog.all.map { it.name } + "أخرى" }
    var expandedFactory by remember { mutableStateOf(false) }

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        DatePickerDialog(context, { _, year, month, day -> onDateSelected("$year-${month + 1}-$day") }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            imageBitmap = bitmap
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val bytes = stream.toByteArray()
            imageData = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val stream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = stream.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    imageBitmap = bitmap
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                    val bytes = out.toByteArray()
                    imageData = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            } catch (_: Exception) {
                Toast.makeText(context, "تعذر تحميل الصورة", Toast.LENGTH_LONG).show()
            }
        }
    }

    val selectedDriver = drivers.firstOrNull { it.id == selectedDriverId }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (drivers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("أضف السواق أولاً", color = Color.White.copy(alpha = 0.5f), fontSize = 16.sp)
            }
            return
        }

        ProGlassCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("وصل المعمل", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Text("تسجيل وصولات المعامل وتوثيقها بصورة", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                Box {
                    OutlinedButton(
                        onClick = { expandedDriver = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            selectedDriver?.let { it.name } ?: "اختر السائق",
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = expandedDriver,
                        onDismissRequest = { expandedDriver = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        drivers.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d.name} - ${d.carNumber}", color = Color.White) },
                                onClick = { selectedDriverId = d.id; expandedDriver = false }
                            )
                        }
                    }
                }

                OutlinedTextField(value = docNumber, onValueChange = { docNumber = it }, label = { Text("رقم وصل المعمل") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = loadDate, onValueChange = {}, readOnly = true, label = { Text("التحميل") }, modifier = Modifier.weight(1f), colors = companyOutlinedTextFieldColors())
                    OutlinedButton(
                        onClick = { showDatePicker { loadDate = it } },
                        modifier = Modifier.width(92.dp).height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) { Text("اختر", color = Color.White) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = unloadDate, onValueChange = {}, readOnly = true, label = { Text("التفريغ") }, modifier = Modifier.weight(1f), colors = companyOutlinedTextFieldColors())
                    OutlinedButton(
                        onClick = { showDatePicker { unloadDate = it } },
                        modifier = Modifier.width(92.dp).height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) { Text("اختر", color = Color.White) }
                }
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("الكمية") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                Box {
                    OutlinedButton(
                        onClick = { expandedFactory = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Warehouse, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (factoryName.isBlank()) "اختر المعمل" else factoryName, color = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedFactory,
                        onDismissRequest = { expandedFactory = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        factories.forEach { factory ->
                            DropdownMenuItem(
                                text = { Text(factory, color = Color.White) },
                                onClick = { factoryName = factory; expandedFactory = false }
                            )
                        }
                    }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("ملاحظات") }, modifier = Modifier.fillMaxWidth(), colors = companyOutlinedTextFieldColors())
                OutlinedTextField(value = "%,d".format(factoryTonPrice), onValueChange = {}, readOnly = true, label = { Text("سعر الطن") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                OutlinedTextField(value = "%,d".format(factoryTotalAmount), onValueChange = {}, readOnly = true, label = { Text("إجمالي الكمية") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = companyOutlinedTextFieldColors())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CompanyActionButton(
                        text = "تصوير",
                        icon = Icons.Default.PhotoCamera,
                        containerColor = factoryPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { cameraLauncher.launch(null) }
                    )
                    CompanyActionButton(
                        text = "تحميل",
                        icon = Icons.Default.FileUpload,
                        containerColor = factorySecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { galleryLauncher.launch("image/*") }
                    )
                }
                if (imageBitmap != null) {
                    Image(bitmap = imageBitmap!!.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1F2937)))
                }
                Button(onClick = {
                    val driver = selectedDriver
                    val cleanDoc = DocNumberGuard.normalize(docNumber)
                    val scopeCompanyId = CompanyPrefs.companyId(context)
                    val scopeActivationCode = CompanyPrefs.activationCode(context)
                    if (driver == null) { Toast.makeText(context, "اختر السائق", Toast.LENGTH_LONG).show(); return@Button }
                    if (scopeCompanyId.isBlank() || scopeActivationCode.isBlank()) {
                        Toast.makeText(context, "التفعيل غير مكتمل: تحقق من companyId وكود التفعيل", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (cleanDoc.isBlank() || quantity.isBlank() || factoryName.isBlank() || loadDate.isBlank() || unloadDate.isBlank()) {
                        Toast.makeText(context, "أكمل البيانات", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    if (imageData.isBlank()) {
                        Toast.makeText(context, "أضف صورة الوصل", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    sending = true

                    fun submitFactoryNow() {
                        val pricingNote = "سعر الطن: ${factoryTonPrice} | إجمالي: ${factoryTotalAmount}"
                        val mergedNotes = if (notes.isBlank()) pricingNote else "$notes\n$pricingNote"
                        val request = FactoryRequest(docNumber = cleanDoc, driverName = driver.name, carNumber = driver.carNumber, loadDate = loadDate, unloadDate = unloadDate, quantity = quantity, factory = factoryName, fileData = imageData, notes = mergedNotes, companyId = scopeCompanyId, activationCode = scopeActivationCode)
                        // Register locally BEFORE sending to prevent duplicates even if send fails
                        DocNumberGuard.markUsed(context, cleanDoc)
                        TripRepository.sendFactory(request, companyId = scopeCompanyId, activationCode = scopeActivationCode, onSuccess = { sending = false; Toast.makeText(context, "تم إرسال الوصل", Toast.LENGTH_LONG).show(); docNumber = ""; loadDate = ""; unloadDate = ""; quantity = ""; factoryName = ""; notes = ""; imageBitmap = null; imageData = "" }, onError = { sending = false; Toast.makeText(context, it, Toast.LENGTH_LONG).show() })
                    }

                    TripRepository.checkDocNumber(cleanDoc, driverName = driver.name, carNumber = driver.carNumber, companyId = scopeCompanyId, activationCode = scopeActivationCode, onResult = { result ->
                        when (result) {
                            TripRepository.DocCheckResult.EXISTS -> { sending = false; Toast.makeText(context, "الوصل موجود", Toast.LENGTH_LONG).show() }
                            TripRepository.DocCheckResult.UNVERIFIED -> {
                                if (DocNumberGuard.isUsedLocally(context, cleanDoc)) {
                                    sending = false
                                    Toast.makeText(context, "رقم الوصل مستخدم مسبقاً (تحقق محلي)", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "تعذر التحقق من السيرفر، تم الاعتماد على الحماية المحلية", Toast.LENGTH_LONG).show()
                                    submitFactoryNow()
                                }
                            }
                            TripRepository.DocCheckResult.AVAILABLE -> submitFactoryNow()
                        }
                    })
                }, enabled = !sending, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = factoryPrimary)) {
                    if (sending) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White) else {
                        Icon(Icons.Default.Warehouse, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("إرسال وصل المعمل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyDriverOverviewScreen(drivers: List<CompanyDriver>) {
    val scope = rememberCoroutineScope()
    var expandedDriver by remember { mutableStateOf(false) }
    var selectedDriverId by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf(DriverMonthlyReport()) }

    val selectedDriver = drivers.firstOrNull { it.id == selectedDriverId }

    fun loadStats() {
        val driver = selectedDriver ?: return
        loading = true
        val now = Calendar.getInstance()
        val month = now.get(Calendar.MONTH) + 1
        val year = now.get(Calendar.YEAR)
        scope.launch {
            try {
                report = CompanyStatsRepository.fetchDriverMonthlyReport(
                    driverName = driver.name,
                    month = month,
                    year = year,
                    reportHalf = CompanyReportHalf.All,
                    companyId = CompanyPrefs.companyId(context),
                    activationCode = CompanyPrefs.activationCode(context)
                )
            } catch (_: Exception) {
                report = DriverMonthlyReport()
            }
            loading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (drivers.isEmpty()) {
            ProGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("أضف السواق أولاً", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("لا يمكن عرض لوحة السائق بدون بيانات مسجلة", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }
            return
        }

        ProGlassCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("لوحة السائق", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Text("عرض مختصر لحسابات السائق الحالي مع تفاصيل الحلفاية والمعمل", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Box {
                    OutlinedButton(
                        onClick = { expandedDriver = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedDriver?.let { it.name } ?: "اختر السائق", color = Color.White, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                    DropdownMenu(
                        expanded = expandedDriver,
                        onDismissRequest = { expandedDriver = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        drivers.forEach { d ->
                            DropdownMenuItem(
                                text = { Text("${d.name} - ${d.carNumber}", color = Color.White) },
                                onClick = { selectedDriverId = d.id; expandedDriver = false }
                            )
                        }
                    }
                }
                CompanyActionButton(
                    text = "تحديث اللوحة",
                    icon = Icons.Default.Refresh,
                    containerColor = Color(0xFF14B8A6),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { loadStats() }
                )
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ProBlue())
            }
        } else if (selectedDriver != null) {
            val reportTotal = report.stats.tripAmount + report.stats.factoryAmount
            val driverGasCost = report.stats.liters * GAS_PRICE_PER_LITER
            val driverNetAfterGas = reportTotal - driverGasCost
            val driverTripAmountView = amountDisplayAccounting(report.stats.tripAmount)
            val driverFactoryAmountView = amountDisplayAccounting(report.stats.factoryAmount)
            val driverTotalAmountView = amountDisplayAccounting(reportTotal)
            val driverGasCostView = amountDisplayAccounting(driverGasCost)
            val driverGasExpenseView = amountDisplayAccounting(driverGasCost)
            val driverNetAfterGasView = amountDisplayAccounting(driverNetAfterGas)

            ProGlassCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(selectedDriver.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                            Text("رقم السيارة: ${selectedDriver.carNumber}", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                        }

                        Surface(
                            color = Color(0xFF14B8A6).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.5f))
                        ) {
                            Text(
                                "الإجمالي ${driverTotalAmountView.first}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProMetricCard("وصلات حلفاية", report.stats.trips.toString(), Color(0xFF0EA5E9), Modifier.weight(1f))
                        ProMetricCard("وصلات المعمل", report.stats.factoryTrips.toString(), Color(0xFFF59E0B), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProMetricCard("كمية حلفاية", formatReadableNumber(report.stats.tripQuantity), Color(0xFF14B8A6), Modifier.weight(1f))
                        ProMetricCard("كمية المعمل", formatReadableNumber(report.stats.factoryQuantity), Color(0xFFEF4444), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProMetricCard("كمية الكاز (لتر)", formatReadableNumber(report.stats.liters, maxFractionDigits = 0), Color(0xFFF59E0B), Modifier.weight(1f))
                        ProMetricCard("حساب الكاز", driverGasCostView.first, Color(0xFFF97316), Modifier.weight(1f), subtitle = driverGasCostView.second)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProMetricCard("حساب حلفاية", driverTripAmountView.first, Color(0xFF0EA5E9), Modifier.weight(1f), subtitle = driverTripAmountView.second)
                        ProMetricCard("حساب المعمل", driverFactoryAmountView.first, Color(0xFF38BDF8), Modifier.weight(1f), subtitle = driverFactoryAmountView.second)
                    }
                    ProMetricCard("إجمالي الحساب الكلي الحقيقي", driverTotalAmountView.first, Color(0xFF14B8A6), Modifier.fillMaxWidth(), subtitle = driverTotalAmountView.second)
                    ProMetricCard("إجمالي حساب الكاز الكلي", driverGasExpenseView.first, Color(0xFF22C55E), Modifier.fillMaxWidth(), subtitle = driverGasExpenseView.second)
                    ProMetricCard("الحساب الكلي بعد استقطاع الكاز", driverNetAfterGasView.first, Color(0xFF16A34A), Modifier.fillMaxWidth(), subtitle = driverNetAfterGasView.second)
                }
            }

            TripRecordsSection(
                title = "وصلات حلفاية (${report.halafayaTrips.size})",
                records = report.halafayaTrips,
                accent = ProBlue(),
                emptyText = "لا توجد وصلات حلفاية لهذا السائق في هذا الشهر"
            )

            TripRecordsSection(
                title = "وصلات المعمل (${report.factoryTrips.size})",
                records = report.factoryTrips,
                accent = ProOrange(),
                emptyText = "لا توجد وصلات معمل لهذا السائق في هذا الشهر"
            )
        }
    }
}

@Composable
private fun CompanyMonthlyReportScreen(drivers: List<CompanyDriver>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val now = Calendar.getInstance()
    val savedQtyAlertLimits = remember { CompanyPrefs.loadExcelQtyAlertLimits(context) }
    val shakeOffsetX = remember { Animatable(0f) }
    var monthText by remember { mutableStateOf(String.format("%02d", now.get(Calendar.MONTH) + 1)) }
    var yearText by remember { mutableStateOf(now.get(Calendar.YEAR).toString()) }
    var qtyAlertMinText by remember {
        mutableStateOf(
            if (savedQtyAlertLimits.first % 1.0 == 0.0) savedQtyAlertLimits.first.toLong().toString() else savedQtyAlertLimits.first.toString()
        )
    }
    var qtyAlertMaxText by remember {
        mutableStateOf(
            if (savedQtyAlertLimits.second % 1.0 == 0.0) savedQtyAlertLimits.second.toLong().toString() else savedQtyAlertLimits.second.toString()
        )
    }
    var selectedHalf by remember { mutableStateOf(CompanyReportHalf.All) }
    var searchText by remember { mutableStateOf("") }
    var expandedSort by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf("total_desc") }
    var loading by remember { mutableStateOf(false) }
    var perDriver by remember { mutableStateOf<Map<String, DriverMonthlyStats>>(emptyMap()) }
    var perDriverReports by remember { mutableStateOf<Map<String, DriverMonthlyReport>>(emptyMap()) }
    var reportDrivers by remember { mutableStateOf<List<CompanyDriver>>(drivers) }
    var total by remember { mutableStateOf(DriverMonthlyStats()) }
    var autoSaveStatusIcon by remember { mutableStateOf("...") }
    var autoSaveStatusText by remember { mutableStateOf("الحفظ التلقائي: جاهز") }
    var autoSaveStatusColor by remember { mutableStateOf(Color.White.copy(alpha = 0.78f)) }

    val reportPrimary = Color(0xFF14B8A6)
    val reportSecondary = Color(0xFF38BDF8)
    val reportWarning = Color(0xFFF59E0B)
    val reportInfo = Color(0xFF0EA5E9)

    val totalAccountValue = total.tripAmount + total.factoryAmount

    fun parseAlertNumber(input: String): Double? {
        val normalizedDigits = buildString(input.length) {
            val arabicDigits = mapOf(
                '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
                '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
                '٫' to '.', '،' to '.',
            )
            input.forEach { append(arabicDigits[it] ?: it) }
        }.trim()
        return normalizedDigits.toDoubleOrNull()
    }

    fun sanitizeAlertInput(input: String): String {
        val normalized = buildString(input.length) {
            val arabicDigits = mapOf(
                '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
                '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
                '٫' to '.', '،' to '.',
            )
            input.forEach { append(arabicDigits[it] ?: it) }
        }
        var dotSeen = false
        val filtered = StringBuilder(normalized.length)
        normalized.forEach { ch ->
            when {
                ch.isDigit() -> filtered.append(ch)
                ch == '.' && !dotSeen -> {
                    filtered.append(ch)
                    dotSeen = true
                }
            }
        }
        val candidate = filtered.toString().take(12)
        if (candidate.endsWith('.')) return candidate
        val parsed = candidate.toDoubleOrNull() ?: return candidate
        return if (parsed > EXCEL_ALERT_INPUT_MAX_TON) {
            EXCEL_ALERT_INPUT_MAX_TON.toInt().toString()
        } else {
            candidate
        }
    }

    fun loadReport() {
        val month = monthText.toIntOrNull()
        val year = yearText.toIntOrNull()
        if (month == null || year == null || month !in 1..12) { Toast.makeText(context, "بيانات غير صحيحة", Toast.LENGTH_LONG).show(); return }
        loading = true
        scope.launch {
            fun DriverMonthlyStats.hasAnyValue(): Boolean {
                return trips > 0 || factoryTrips > 0 ||
                    tripQuantity > 0.0 || factoryQuantity > 0.0 ||
                    liters > 0.0 || tripAmount > 0.0 || factoryAmount > 0.0
            }

            val reportDriverList = drivers
            reportDrivers = reportDriverList

            if (reportDriverList.isEmpty()) {
                perDriver = emptyMap()
                perDriverReports = emptyMap()
                total = DriverMonthlyStats()
                loading = false
                Toast.makeText(context, "لا توجد سواق محفوظين. أضف السواق أولاً", Toast.LENGTH_LONG).show()
                return@launch
            }

            val map = mutableMapOf<String, DriverMonthlyStats>()
            val detailsMap = mutableMapOf<String, DriverMonthlyReport>()
            var aggregate = DriverMonthlyStats()
            var failedFetchCount = 0
            reportDriverList.forEach { driver ->
                try {
                    val serverReport = CompanyStatsRepository.fetchDriverMonthlyReport(driver.name, month, year, selectedHalf)
                    val locallyFiltered = filterCompanyMonthlyReport(serverReport, selectedHalf)
                    val effectiveReport = when {
                        selectedHalf == CompanyReportHalf.All -> serverReport
                        locallyFiltered.stats.hasAnyValue() -> locallyFiltered
                        !serverReport.stats.hasAnyValue() -> locallyFiltered
                        else -> serverReport
                    }
                    map[driver.id] = effectiveReport.stats
                    detailsMap[driver.id] = effectiveReport
                    aggregate += effectiveReport.stats
                } catch (_: Exception) {
                    failedFetchCount += 1
                    map[driver.id] = DriverMonthlyStats()
                    detailsMap[driver.id] = DriverMonthlyReport()
                }
            }
            perDriver = map
            perDriverReports = detailsMap
            total = aggregate
            loading = false
            if (failedFetchCount > 0) {
                val message = if (failedFetchCount == reportDriverList.size) {
                    "فشل جلب بيانات التقرير من السيرفر. تحقق من رابط API."
                } else {
                    "تعذر جلب بيانات بعض السواق (${formatReadableNumber(failedFetchCount, maxFractionDigits = 0)})"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun triggerInvalidInputShake() {
        scope.launch {
            shakeOffsetX.stop()
            shakeOffsetX.snapTo(0f)
            repeat(3) {
                shakeOffsetX.animateTo(-6f, animationSpec = tween(35))
                shakeOffsetX.animateTo(6f, animationSpec = tween(70))
            }
            shakeOffsetX.animateTo(0f, animationSpec = tween(35))
        }
    }

    LaunchedEffect(qtyAlertMinText, qtyAlertMaxText) {
        autoSaveStatusIcon = "..."
        autoSaveStatusText = "الحفظ التلقائي: جاري التحقق..."
        autoSaveStatusColor = Color(0xFF9AD1FF)
        delay(700)
        val minValue = parseAlertNumber(qtyAlertMinText)
        val maxValue = parseAlertNumber(qtyAlertMaxText)
        if (minValue == null || maxValue == null) {
            autoSaveStatusIcon = "!"
            autoSaveStatusText = "الحفظ التلقائي: إدخال غير صالح"
            autoSaveStatusColor = Color(0xFFFFB3B3)
        } else if (minValue > EXCEL_ALERT_INPUT_MAX_TON || maxValue > EXCEL_ALERT_INPUT_MAX_TON) {
            autoSaveStatusIcon = "!"
            autoSaveStatusText = "الحفظ التلقائي: الحد الأقصى المسموح لكل قيمة هو ${EXCEL_ALERT_INPUT_MAX_TON.toInt()} طن"
            autoSaveStatusColor = Color(0xFFFFB3B3)
        } else if (maxValue <= minValue) {
            autoSaveStatusIcon = "!"
            autoSaveStatusText = "الحفظ التلقائي: الحد الأعلى يجب أن يكون أكبر من الأدنى"
            autoSaveStatusColor = Color(0xFFFFB3B3)
        } else {
            val (savedMin, savedMax) = CompanyPrefs.loadExcelQtyAlertLimits(context)
            if (abs(savedMin - minValue) > 1e-9 || abs(savedMax - maxValue) > 1e-9) {
                autoSaveStatusIcon = "..."
                autoSaveStatusText = "الحفظ التلقائي: جاري الحفظ..."
                autoSaveStatusColor = Color(0xFF9AD1FF)
                CompanyPrefs.saveExcelQtyAlertLimits(context, minValue, maxValue)
                autoSaveStatusIcon = "✓"
                autoSaveStatusText = "الحفظ التلقائي: تم الحفظ تلقائيًا"
                autoSaveStatusColor = Color(0xFF7CFFB2)
            } else {
                autoSaveStatusIcon = "✓"
                autoSaveStatusText = "الحفظ التلقائي: لا توجد تغييرات"
                autoSaveStatusColor = Color.White.copy(alpha = 0.74f)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProGlassCard {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("التقرير الشهري", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                Text("لوحة تشغيل وتقارير السواق والمعامل", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = monthText, onValueChange = { monthText = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("الشهر") }, modifier = Modifier.weight(1f), singleLine = true, colors = companyOutlinedTextFieldColors())
                    OutlinedTextField(value = yearText, onValueChange = { yearText = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("السنة") }, modifier = Modifier.weight(1f), singleLine = true, colors = companyOutlinedTextFieldColors())
                }
                Text("فترة التقرير", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CompanyReportHalf.entries.forEach { half ->
                        OutlinedButton(
                            onClick = { selectedHalf = half },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedHalf == half) reportPrimary.copy(alpha = 0.18f) else Color.Transparent,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, if (selectedHalf == half) reportPrimary else Color.White.copy(alpha = 0.28f))
                        ) {
                            Text(half.title)
                        }
                    }
                }
                val saveMinValue = parseAlertNumber(qtyAlertMinText)
                val saveMaxValue = parseAlertNumber(qtyAlertMaxText)
                val isMinExceeded = saveMinValue != null && saveMinValue > EXCEL_ALERT_INPUT_MAX_TON
                val isMaxExceeded = saveMaxValue != null && saveMaxValue > EXCEL_ALERT_INPUT_MAX_TON
                val isSaveInputValid = saveMinValue != null && saveMaxValue != null && !isMinExceeded && !isMaxExceeded && saveMaxValue > saveMinValue
                val isMinNumericError = saveMinValue == null
                val isMaxNumericError = saveMaxValue == null
                val isRangeOrderError = saveMinValue != null && saveMaxValue != null && !isMinExceeded && !isMaxExceeded && saveMaxValue <= saveMinValue
                val minFieldError = when {
                    isMinNumericError -> "أدخل رقمًا صحيحًا للحد الأدنى"
                    isMinExceeded -> "الحد الأقصى المسموح ${EXCEL_ALERT_INPUT_MAX_TON.toInt()} طن"
                    isRangeOrderError -> "يجب أن يكون أقل من الحد الأعلى"
                    else -> null
                }
                val maxFieldError = when {
                    isMaxNumericError -> "أدخل رقمًا صحيحًا للحد الأعلى"
                    isMaxExceeded -> "الحد الأقصى المسموح ${EXCEL_ALERT_INPUT_MAX_TON.toInt()} طن"
                    isRangeOrderError -> "يجب أن يكون أكبر من الحد الأدنى"
                    else -> null
                }
                val minErrorColor = when {
                    isMinNumericError -> Color(0xFFFFB3B3)
                    isMinExceeded -> Color(0xFFFF8A8A)
                    isRangeOrderError -> Color(0xFFFFD27A)
                    else -> Color(0xFFFFB3B3)
                }
                val maxErrorColor = when {
                    isMaxNumericError -> Color(0xFFFFB3B3)
                    isMaxExceeded -> Color(0xFFFF8A8A)
                    isRangeOrderError -> Color(0xFFFFD27A)
                    else -> Color(0xFFFFB3B3)
                }
                val minErrorIcon = when {
                    isRangeOrderError -> "<>"
                    isMinExceeded -> "^"
                    else -> "!"
                }
                val maxErrorIcon = when {
                    isRangeOrderError -> "<>"
                    isMaxExceeded -> "^"
                    else -> "!"
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().offset(x = shakeOffsetX.value.dp)
                ) {
                    OutlinedTextField(
                        value = qtyAlertMinText,
                        onValueChange = { qtyAlertMinText = sanitizeAlertInput(it) },
                        label = { Text("حد التنبيه الأدنى (طن)") },
                        isError = minFieldError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            if (minFieldError != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(minErrorIcon, color = minErrorColor, fontWeight = FontWeight.Bold)
                                    Text(minFieldError, color = minErrorColor)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = companyOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = qtyAlertMaxText,
                        onValueChange = { qtyAlertMaxText = sanitizeAlertInput(it) },
                        label = { Text("حد التنبيه الأعلى (طن)") },
                        isError = maxFieldError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            if (maxFieldError != null) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(maxErrorIcon, color = maxErrorColor, fontWeight = FontWeight.Bold)
                                    Text(maxFieldError, color = maxErrorColor)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = companyOutlinedTextFieldColors()
                    )
                }
                val saveDisabledReason = when {
                    saveMinValue == null || saveMaxValue == null -> "سبب التعطيل: أدخل قيمًا رقمية صحيحة"
                    isMinExceeded || isMaxExceeded -> "سبب التعطيل: الحد الأقصى لكل قيمة هو ${EXCEL_ALERT_INPUT_MAX_TON.toInt()} طن"
                    saveMaxValue <= saveMinValue -> "سبب التعطيل: الحد الأعلى يجب أن يكون أكبر من الحد الأدنى"
                    else -> ""
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f).height(48.dp)) {
                        OutlinedButton(
                            enabled = isSaveInputValid,
                            onClick = {
                                if (saveMinValue == null || saveMaxValue == null) {
                                    Toast.makeText(context, "حدود التنبيه غير صحيحة", Toast.LENGTH_LONG).show()
                                } else if (isMinExceeded || isMaxExceeded) {
                                    Toast.makeText(context, "الحد الأقصى لكل قيمة هو ${EXCEL_ALERT_INPUT_MAX_TON.toInt()} طن", Toast.LENGTH_LONG).show()
                                } else if (saveMaxValue <= saveMinValue) {
                                    Toast.makeText(context, "الحد الأعلى يجب أن يكون أكبر من الحد الأدنى", Toast.LENGTH_LONG).show()
                                } else {
                                    CompanyPrefs.saveExcelQtyAlertLimits(context, saveMinValue, saveMaxValue)
                                    val (savedMin, savedMax) = CompanyPrefs.loadExcelQtyAlertLimits(context)
                                    qtyAlertMinText = if (savedMin % 1.0 == 0.0) savedMin.toLong().toString() else savedMin.toString()
                                    qtyAlertMaxText = if (savedMax % 1.0 == 0.0) savedMax.toLong().toString() else savedMax.toString()
                                    autoSaveStatusIcon = "✓"
                                    autoSaveStatusText = "الحفظ التلقائي: تم الحفظ يدويًا"
                                    autoSaveStatusColor = Color(0xFF7CFFB2)
                                    Toast.makeText(context, "تم حفظ حدود تنبيه Excel", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            colors = companySelectionButtonColors(),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                        ) {
                            Text("حفظ الحدود", color = Color.White)
                        }
                        if (!isSaveInputValid) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        triggerInvalidInputShake()
                                        Toast.makeText(context, saveDisabledReason, Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            CompanyPrefs.saveExcelQtyAlertLimits(
                                context = context,
                                minTon = EXCEL_SUSPICIOUS_QTY_MIN_TON,
                                maxTon = EXCEL_SUSPICIOUS_QTY_MAX_TON
                            )
                            qtyAlertMinText = if (EXCEL_SUSPICIOUS_QTY_MIN_TON % 1.0 == 0.0) EXCEL_SUSPICIOUS_QTY_MIN_TON.toLong().toString() else EXCEL_SUSPICIOUS_QTY_MIN_TON.toString()
                            qtyAlertMaxText = if (EXCEL_SUSPICIOUS_QTY_MAX_TON % 1.0 == 0.0) EXCEL_SUSPICIOUS_QTY_MAX_TON.toLong().toString() else EXCEL_SUSPICIOUS_QTY_MAX_TON.toString()
                            autoSaveStatusIcon = "✓"
                            autoSaveStatusText = "الحفظ التلقائي: تمت إعادة القيم الافتراضية"
                            autoSaveStatusColor = Color(0xFF7CFFB2)
                            Toast.makeText(context, "تمت إعادة القيم الافتراضية", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                    ) {
                        Text("إعادة الافتراضي", color = Color.White)
                    }
                }
                if (!isSaveInputValid) {
                    Text(
                        text = saveDisabledReason,
                        color = Color(0xFFFFB3B3),
                        fontSize = 11.sp
                    )
                }
                val previewMin = parseAlertNumber(qtyAlertMinText)
                val previewMax = parseAlertNumber(qtyAlertMaxText)
                val isPreviewRangeValid = previewMin != null && previewMax != null && previewMin <= EXCEL_ALERT_INPUT_MAX_TON && previewMax <= EXCEL_ALERT_INPUT_MAX_TON && previewMax > previewMin
                val previewRule = if (isPreviewRangeValid) {
                    "قاعدة تنبيه Excel الحالية: <= ${formatReadableNumber(previewMin, maxFractionDigits = 2)} أو > ${formatReadableNumber(previewMax, maxFractionDigits = 2)} طن"
                } else if (previewMin != null && previewMax != null && (previewMin > EXCEL_ALERT_INPUT_MAX_TON || previewMax > EXCEL_ALERT_INPUT_MAX_TON)) {
                    "قاعدة تنبيه Excel الحالية: الحد الأقصى لكل قيمة هو ${EXCEL_ALERT_INPUT_MAX_TON.toInt()} طن"
                } else if (previewMin != null && previewMax != null) {
                    "قاعدة تنبيه Excel الحالية: الحد الأعلى يجب أن يكون أكبر من الحد الأدنى"
                } else {
                    "قاعدة تنبيه Excel الحالية: أدخل قيمًا رقمية صحيحة للحد الأدنى والأعلى"
                }
                val previewColor = if (isPreviewRangeValid) {
                    Color(0xFF7CFFB2)
                } else {
                    Color(0xFFFFB3B3)
                }
                Text(
                    text = previewRule,
                    color = previewColor,
                    fontSize = 12.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = autoSaveStatusIcon,
                        color = autoSaveStatusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = autoSaveStatusText,
                        color = autoSaveStatusColor,
                        fontSize = 11.sp
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "أمثلة إدخال الحدود",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "الحد الأقصى لكل قيمة: ${EXCEL_ALERT_INPUT_MAX_TON.toInt()} طن",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = "صحيح: 5 | 12.5 | ٦٠٫٢٥",
                        color = Color(0xFF7CFFB2),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "غير صحيح: 5..2 | 1,2,3 | abc",
                        color = Color(0xFFFFB3B3),
                        fontSize = 11.sp
                    )
                }
                OutlinedTextField(value = searchText, onValueChange = { searchText = it }, label = { Text("بحث عن السائق") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, colors = companyOutlinedTextFieldColors())

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { expandedSort = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = companySelectionButtonColors(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Text(
                            when (sortMode) {
                                "total_desc" -> "ترتيب: الأعلى حساباً"
                                "halafaya_qty_desc" -> "ترتيب: الأعلى كمية حلفاية"
                                "factory_qty_desc" -> "ترتيب: الأعلى كمية معمل"
                                else -> "ترتيب: الاسم"
                            },
                            color = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = expandedSort,
                        onDismissRequest = { expandedSort = false },
                        modifier = Modifier.background(Color(0xFF12313D).copy(alpha = 0.96f))
                    ) {
                        DropdownMenuItem(
                            text = { Text("الأعلى حساباً", color = Color.White) },
                            onClick = { sortMode = "total_desc"; expandedSort = false }
                        )
                        DropdownMenuItem(
                            text = { Text("الأعلى كمية حلفاية", color = Color.White) },
                            onClick = { sortMode = "halafaya_qty_desc"; expandedSort = false }
                        )
                        DropdownMenuItem(
                            text = { Text("الأعلى كمية معمل", color = Color.White) },
                            onClick = { sortMode = "factory_qty_desc"; expandedSort = false }
                        )
                        DropdownMenuItem(
                            text = { Text("الاسم", color = Color.White) },
                            onClick = { sortMode = "name_asc"; expandedSort = false }
                        )
                    }
                }

                Button(
                    onClick = { loadReport() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = reportPrimary)
                ) {
                    Icon(Icons.Default.Summarize, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تحميل التقرير", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ProBlue())
            }
        } else if (perDriver.isNotEmpty()) {
            val reportSourceDrivers = if (reportDrivers.isNotEmpty()) reportDrivers else drivers
            val reportPeriodLabel = buildString {
                append("$monthText/$yearText")
                if (selectedHalf != CompanyReportHalf.All) {
                    append(" - ")
                    append(selectedHalf.title)
                }
            }
            val activeStats = reportSourceDrivers.mapNotNull { d -> perDriver[d.id] }
            val allTripsCount = (total.trips + total.factoryTrips).coerceAtLeast(1)
            val averageQtyPerTrip = (total.tripQuantity + total.factoryQuantity) / allTripsCount
            val averageAmountPerDriver = if (activeStats.isNotEmpty()) totalAccountValue / activeStats.size else 0.0
            val totalTripAmountView = amountDisplayAccounting(total.tripAmount)
            val totalFactoryAmountView = amountDisplayAccounting(total.factoryAmount)
            val totalAmountView = amountDisplayAccounting(totalAccountValue)
            val averageAmountView = amountDisplayAccounting(averageAmountPerDriver)
            val totalGasCost = total.liters * GAS_PRICE_PER_LITER
            val totalNetAfterGas = totalAccountValue - totalGasCost
            val totalGasCostView = amountDisplayAccounting(totalGasCost)
            val totalGasExpenseView = amountDisplayAccounting(totalGasCost)
            val totalNetAfterGasView = amountDisplayAccounting(totalNetAfterGas)

            val topDriver = reportSourceDrivers.maxByOrNull { d ->
                val s = perDriver[d.id] ?: DriverMonthlyStats()
                s.tripAmount + s.factoryAmount
            }
            val topDriverTotal = topDriver?.let { d ->
                val s = perDriver[d.id] ?: DriverMonthlyStats()
                s.tripAmount + s.factoryAmount
            } ?: 0.0

            ProGlassCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("الملخص العام", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProMetricCard("وصلات حلفاية", total.trips.toString(), reportInfo, Modifier.weight(1f))
                        ProMetricCard("وصلات المعمل", total.factoryTrips.toString(), reportWarning, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProMetricCard("كمية حلفاية", formatReadableNumber(total.tripQuantity), reportPrimary, Modifier.weight(1f))
                        ProMetricCard("كمية المعمل", formatReadableNumber(total.factoryQuantity), Color(0xFFEF4444), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProMetricCard("كمية الكاز (لتر)", formatReadableNumber(total.liters, maxFractionDigits = 0), reportWarning, Modifier.weight(1f))
                        ProMetricCard("حساب الكاز", totalGasCostView.first, Color(0xFFF97316), Modifier.weight(1f), subtitle = totalGasCostView.second)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProMetricCard("حساب حلفاية", totalTripAmountView.first, reportInfo, Modifier.weight(1f), subtitle = totalTripAmountView.second)
                        ProMetricCard("حساب المعمل", totalFactoryAmountView.first, reportSecondary, Modifier.weight(1f), subtitle = totalFactoryAmountView.second)
                    }
                    ProMetricCard("إجمالي الحساب الكلي الحقيقي", totalAmountView.first, reportPrimary, Modifier.fillMaxWidth(), subtitle = totalAmountView.second)
                    ProMetricCard("إجمالي حساب الكاز الكلي", totalGasExpenseView.first, Color(0xFF22C55E), Modifier.fillMaxWidth(), subtitle = totalGasExpenseView.second)
                    ProMetricCard("الحساب الكلي بعد استقطاع الكاز", totalNetAfterGasView.first, Color(0xFF16A34A), Modifier.fillMaxWidth(), subtitle = totalNetAfterGasView.second)
                    Text(
                        "تحقق الكاز: ${formatReadableNumber(total.liters, maxFractionDigits = 0)} × ${formatAmountReadable(GAS_PRICE_PER_LITER)} = ${totalGasExpenseView.first} (بدون آخر 3 أصفار)",
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 11.sp
                    )
                }
            }

            ProGlassCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مؤشرات احترافية", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    Text("متوسط الكمية لكل وصلة: ${formatReadableNumber(averageQtyPerTrip)} طن", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    Text(
                        "متوسط الحساب لكل سائق (بدون آخر 3 أصفار): ${averageAmountView.first}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                    Text(
                        "سعر لتر الكاز المعتمد: ${formatAmountReadable(GAS_PRICE_PER_LITER)} د.ع",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                    Text("أفضل سائق حسب الحساب: ${topDriver?.name ?: "-"} (${formatAmountReadable(topDriverTotal)} د.ع)", color = Color.White.copy(alpha = 0.96f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            ProGlassCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تصدير ومشاركة التقرير", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CompanyActionButton(
                            text = "طباعة PDF",
                            icon = Icons.Default.Print,
                            containerColor = reportInfo,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val month = monthText.toIntOrNull()
                                val year = yearText.toIntOrNull()
                                if (month != null && year != null) {
                                    val file = createCompanyMonthlyPdf(context, month, year, reportPeriodLabel, reportSourceDrivers, perDriver, total)
                                    Toast.makeText(context, "تم إنشاء ملف PDF", Toast.LENGTH_SHORT).show()
                                    ReportUtils.printPdf(context, file)
                                }
                            }
                        )

                        CompanyActionButton(
                            text = "مشاركة PDF",
                            icon = Icons.Default.PictureAsPdf,
                            containerColor = reportPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val month = monthText.toIntOrNull()
                                val year = yearText.toIntOrNull()
                                if (month != null && year != null) {
                                    val file = createCompanyMonthlyPdf(context, month, year, reportPeriodLabel, reportSourceDrivers, perDriver, total)
                                    shareFile(
                                        context = context,
                                        file = file,
                                        mimeType = "application/pdf",
                                        chooserTitle = "مشاركة التقرير PDF"
                                    )
                                }
                            }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CompanyActionButton(
                            text = "واتساب",
                            icon = Icons.Default.Share,
                            containerColor = reportWarning,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val month = monthText.toIntOrNull()
                                val year = yearText.toIntOrNull()
                                if (month != null && year != null) {
                                    val file = createCompanyMonthlyPdf(context, month, year, reportPeriodLabel, reportSourceDrivers, perDriver, total)
                                    shareFile(
                                        context = context,
                                        file = file,
                                        mimeType = "application/pdf",
                                        chooserTitle = "إرسال عبر واتساب",
                                        targetPackage = "com.whatsapp"
                                    )
                                }
                            }
                        )

                        CompanyActionButton(
                            text = "تصدير Excel",
                            icon = Icons.Default.TableChart,
                            containerColor = reportSecondary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val month = monthText.toIntOrNull()
                                val year = yearText.toIntOrNull()
                                if (month != null && year != null) {
                                    val (savedMin, savedMax) = CompanyPrefs.loadExcelQtyAlertLimits(context)
                                    val file = createCompanyMonthlyXlsx(
                                        context = context,
                                        month = month,
                                        year = year,
                                        periodLabel = reportPeriodLabel,
                                        drivers = reportSourceDrivers,
                                        statsMap = perDriver,
                                        reportsMap = perDriverReports,
                                        total = total,
                                        suspiciousQtyMinTon = savedMin,
                                        suspiciousQtyMaxTon = savedMax
                                    )
                                    shareFile(
                                        context = context,
                                        file = file,
                                        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        chooserTitle = "مشاركة ملف Excel (.xlsx)"
                                    )
                                }
                            }
                        )
                    }
                }
            }

            val filteredDrivers = reportSourceDrivers.filter { searchText.isEmpty() || it.name.contains(searchText, ignoreCase = true) }
            val sortedDrivers = when (sortMode) {
                "halafaya_qty_desc" -> filteredDrivers.sortedByDescending { d -> (perDriver[d.id] ?: DriverMonthlyStats()).tripQuantity }
                "factory_qty_desc" -> filteredDrivers.sortedByDescending { d -> (perDriver[d.id] ?: DriverMonthlyStats()).factoryQuantity }
                "name_asc" -> filteredDrivers.sortedBy { it.name }
                else -> filteredDrivers.sortedByDescending { d ->
                    val s = perDriver[d.id] ?: DriverMonthlyStats()
                    s.tripAmount + s.factoryAmount
                }
            }

            Text("تفاصيل السواق (${sortedDrivers.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sortedDrivers.forEach { driver ->
                    val stats = perDriver[driver.id] ?: DriverMonthlyStats()
                    val report = perDriverReports[driver.id] ?: DriverMonthlyReport()
                    val driverAmountView = amountDisplay(stats.tripAmount + stats.factoryAmount)
                    val driverGasCost = stats.liters * GAS_PRICE_PER_LITER
                    val driverNetAfterGas = (stats.tripAmount + stats.factoryAmount) - driverGasCost
                    ProGlassCard {
                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val driverTotal = stats.tripAmount + stats.factoryAmount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(driver.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    Text("رقم السيارة: ${driver.carNumber}", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                                }
                                Surface(
                                    color = reportPrimary.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, reportPrimary.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        "الإجمالي ${driverAmountView.first}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Text("حلفاية: ${stats.trips} وصلات | الكمية: ${formatReadableNumber(stats.tripQuantity)} طن | الحساب: ${formatAmountReadable(stats.tripAmount)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                            Text("المعمل: ${stats.factoryTrips} وصلات | الكمية: ${formatReadableNumber(stats.factoryQuantity)} طن | الحساب: ${formatAmountReadable(stats.factoryAmount)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            Text("الكاز: ${formatReadableNumber(stats.liters, maxFractionDigits = 0)} لتر | حساب الكاز: ${formatAmountReadable(driverGasCost)} | الصافي: ${formatAmountReadable(driverNetAfterGas)}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))

                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                            Text("وصلات حلفاية (${report.halafayaTrips.size})", color = ProBlue(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (report.halafayaTrips.isEmpty()) {
                                Text("لا توجد وصلات حلفاية", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            } else {
                                report.halafayaTrips.forEach { trip ->
                                    val loadDateText = formatDisplayDate(trip.loadDate)
                                    val quantityText = if (trip.quantity % 1.0 == 0.0) trip.quantity.toLong().toString() else formatReadableNumber(trip.quantity)
                                    Text(
                                        "• ${trip.docNumber} | ${trip.destination} | ${quantityText} طن | ${loadDateText}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Text("وصلات المعمل (${report.factoryTrips.size})", color = ProOrange(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (report.factoryTrips.isEmpty()) {
                                Text("لا توجد وصلات معمل", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                            } else {
                                report.factoryTrips.forEach { trip ->
                                    val loadDateText = formatDisplayDate(trip.loadDate)
                                    val quantityText = if (trip.quantity % 1.0 == 0.0) trip.quantity.toLong().toString() else formatReadableNumber(trip.quantity)
                                    Text(
                                        "• ${trip.docNumber} | ${trip.destination} | ${quantityText} طن | ${loadDateText}",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            ProGlassCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("لا توجد بيانات تقرير", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("اختر الشهر والسنة ثم اضغط تحميل التقرير", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ProMetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier, subtitle: String? = null) {
    ProGlassCard(modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.96f), fontSize = 12.sp)
            HorizontalDivider(color = color.copy(alpha = 0.65f), thickness = 1.dp, modifier = Modifier.fillMaxWidth(0.5f))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CompanyActionButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = Color.White)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

private fun createCompanyMonthlyPdf(context: android.content.Context, month: Int, year: Int, periodLabel: String, drivers: List<CompanyDriver>, statsMap: Map<String, DriverMonthlyStats>, total: DriverMonthlyStats): File {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas
    val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
    val textPaint = Paint().apply { textSize = 12f }
    val totalAmount = total.tripAmount + total.factoryAmount
    val totalGasCost = total.liters * GAS_PRICE_PER_LITER
    val totalNetAfterGas = totalAmount - totalGasCost
    val totalTripAmountView = amountDisplayAccounting(total.tripAmount)
    val totalFactoryAmountView = amountDisplayAccounting(total.factoryAmount)
    val totalAmountView = amountDisplayAccounting(totalAmount)
    val totalGasCostView = amountDisplayAccounting(totalGasCost)
    val totalGasExpenseView = amountDisplayAccounting(totalGasCost)
    val totalNetAfterGasView = amountDisplayAccounting(totalNetAfterGas)
    var y = 40
    canvas.drawText("تقرير الشركة الشهري - $periodLabel", 40f, y.toFloat(), titlePaint)
    y += 28
    canvas.drawText("إجمالي وصلات حلفاية: ${formatReadableNumber(total.trips)}", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("إجمالي وصلات المعمل: ${formatReadableNumber(total.factoryTrips)}", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("كمية حلفاية: ${formatReadableNumber(total.tripQuantity)} طن", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("كمية المعمل: ${formatReadableNumber(total.factoryQuantity)} طن", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("كمية الكاز: ${formatReadableNumber(total.liters, maxFractionDigits = 0)} لتر", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("حساب حلفاية: ${compactWithFull(totalTripAmountView)} د.ع", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("حساب المعمل: ${compactWithFull(totalFactoryAmountView)} د.ع", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("حساب الكاز: ${compactWithFull(totalGasCostView)} د.ع", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("إجمالي الحساب الكلي الحقيقي: ${compactWithFull(totalAmountView)} د.ع", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("إجمالي حساب الكاز الكلي: ${compactWithFull(totalGasExpenseView)} د.ع", 40f, y.toFloat(), textPaint)
    y += 18
    canvas.drawText("الحساب الكلي بعد استقطاع الكاز: ${compactWithFull(totalNetAfterGasView)} د.ع", 40f, y.toFloat(), textPaint)
    y += 28
    canvas.drawText("تفاصيل السواق", 40f, y.toFloat(), titlePaint)
    y += 20
    drivers.forEach { driver ->
        val stats = statsMap[driver.id] ?: DriverMonthlyStats()
        val gasCost = stats.liters * GAS_PRICE_PER_LITER
        val netAfterGas = (stats.tripAmount + stats.factoryAmount) - gasCost
        val tripAmountView = amountDisplayAccounting(stats.tripAmount)
        val factoryAmountView = amountDisplayAccounting(stats.factoryAmount)
        val rowTotalAmountView = amountDisplayAccounting(stats.tripAmount + stats.factoryAmount)
        val gasCostView = amountDisplayAccounting(gasCost)
        val rowGasExpenseView = amountDisplayAccounting(gasCost)
        val rowNetAfterGasView = amountDisplayAccounting(netAfterGas)
        if (y > 790) return@forEach
        canvas.drawText("${driver.name} (${driver.carNumber})", 40f, y.toFloat(), textPaint)
        y += 15
        canvas.drawText("حلفاية:${formatReadableNumber(stats.trips)} | معمل:${formatReadableNumber(stats.factoryTrips)} | كمية ح:${formatReadableNumber(stats.tripQuantity)} | كمية م:${formatReadableNumber(stats.factoryQuantity)} | كاز:${formatReadableNumber(stats.liters, maxFractionDigits = 0)}", 50f, y.toFloat(), textPaint)
        y += 15
        canvas.drawText("حساب ح:${compactWithFull(tripAmountView)} | حساب م:${compactWithFull(factoryAmountView)} | حساب كاز:${compactWithFull(gasCostView)} | الإجمالي:${compactWithFull(rowTotalAmountView)} | صرفيات كاز:${compactWithFull(rowGasExpenseView)} | صافي بعد كاز:${compactWithFull(rowNetAfterGasView)}", 50f, y.toFloat(), textPaint)
        y += 18
    }
    pdf.finishPage(page)
    val safePeriod = periodLabel.replace("/", "-").replace(" ", "_")
    val file = File(context.cacheDir, "company_report_${safePeriod}.pdf")
    pdf.writeTo(FileOutputStream(file))
    pdf.close()
    return file
}

private fun formatReadableNumber(value: Number, maxFractionDigits: Int = 2): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }
    val pattern = if (maxFractionDigits <= 0) "#,##0" else "#,##0.${"#".repeat(maxFractionDigits)}"
    val formatter = DecimalFormat(pattern, symbols)
    formatter.isGroupingUsed = true
    return formatter.format(value.toDouble())
}

private fun formatAmountReadable(value: Number): String = formatReadableNumber(value, maxFractionDigits = 0)

private fun formatCompactAmountReadable(value: Number): String {
    val amount = value.toDouble()
    val absAmount = abs(amount)
    fun compactPart(divisor: Double): String = formatReadableNumber(amount / divisor, maxFractionDigits = 1)

    return when {
        absAmount >= 1_000_000_000.0 -> "${compactPart(1_000_000_000.0)} مليار"
        absAmount >= 1_000_000.0 -> "${compactPart(1_000_000.0)} مليون"
        absAmount >= 1_000.0 -> "${compactPart(1_000.0)} ألف"
        else -> formatAmountReadable(amount)
    }
}

private fun amountDisplay(value: Number): Pair<String, String?> {
    val full = formatAmountReadable(value)
    val compact = formatCompactAmountReadable(value)
    return if (full == compact) compact to null else compact to full
}

private fun amountDisplayAccounting(value: Number): Pair<String, String?> {
    val scaled = (value.toDouble() / 1000.0).roundToLong()
    return formatAmountReadable(scaled) to null
}

private const val EXCEL_SUSPICIOUS_QTY_MIN_TON = 0.0
private const val EXCEL_SUSPICIOUS_QTY_MAX_TON = 100.0
private const val EXCEL_ALERT_INPUT_MAX_TON = 9999.0
private const val GAS_PRICE_PER_LITER = 430.0

private fun compactWithFull(display: Pair<String, String?>): String {
    val compact = display.first
    val full = display.second
    return if (full == null) compact else "$compact ($full)"
}

private fun createCompanyMonthlyXlsx(
    context: android.content.Context,
    month: Int,
    year: Int,
    periodLabel: String,
    drivers: List<CompanyDriver>,
    statsMap: Map<String, DriverMonthlyStats>,
    reportsMap: Map<String, DriverMonthlyReport>,
    total: DriverMonthlyStats,
    suspiciousQtyMinTon: Double = EXCEL_SUSPICIOUS_QTY_MIN_TON,
    suspiciousQtyMaxTon: Double = EXCEL_SUSPICIOUS_QTY_MAX_TON,
): File {
    data class XCell(val value: String, val isNumeric: Boolean, val styleIndex: Int? = null)
    data class XSheet(
        val name: String,
        val rows: List<List<XCell>>,
        val freezeTopRows: Int = 0,
        val autoFilterRef: String? = null
    )

    fun excelNumber(value: Double, scale: Int = 3): String {
        val rounded = BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
        return rounded.toPlainString()
    }

    fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun colName(index: Int): String {
        var i = index
        val out = StringBuilder()
        while (i > 0) {
            val rem = (i - 1) % 26
            out.append(('A'.code + rem).toChar())
            i = (i - 1) / 26
        }
        return out.reverse().toString()
    }

    fun sheetXml(rows: List<List<XCell>>, freezeTopRows: Int = 0, autoFilterRef: String? = null): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        if (freezeTopRows > 0) {
            val topRow = freezeTopRows + 1
            sb.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"")
                .append(freezeTopRows)
                .append("\" topLeftCell=\"A")
                .append(topRow)
                .append("\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        }
        sb.append("<sheetData>")

        rows.forEachIndexed { rowIndex, row ->
            val excelRow = rowIndex + 1
            sb.append("<row r=\"").append(excelRow).append("\">")
            row.forEachIndexed { colIndex, cell ->
                val ref = colName(colIndex + 1) + excelRow
                val styleAttr = cell.styleIndex?.let { " s=\"$it\"" } ?: ""
                if (cell.isNumeric) {
                    val n = cell.value.toDoubleOrNull() ?: 0.0
                    val printable = if (n % 1.0 == 0.0) n.toLong().toString() else n.toString()
                    sb.append("<c r=\"").append(ref).append("\"").append(styleAttr).append("><v>").append(printable).append("</v></c>")
                } else {
                    sb.append("<c r=\"").append(ref).append("\"").append(styleAttr).append(" t=\"inlineStr\"><is><t>")
                        .append(escapeXml(cell.value))
                        .append("</t></is></c>")
                }
            }
            sb.append("</row>")
        }

        sb.append("</sheetData>")
        if (!autoFilterRef.isNullOrBlank()) {
            sb.append("<autoFilter ref=\"").append(autoFilterRef).append("\"/>")
        }
        sb.append("</worksheet>")
        return sb.toString()
    }

    fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    fun safeSheetName(name: String): String {
        val cleaned = name
            .replace(Regex("[\\\\/*?:\\[\\]]"), "_")
            .trim()
            .ifBlank { "ورقة" }
        return if (cleaned.length <= 31) cleaned else cleaned.take(31)
    }

    fun uniqueSheetName(base: String, used: MutableSet<String>): String {
        var candidate = safeSheetName(base)
        if (candidate !in used) {
            used += candidate
            return candidate
        }

        var index = 2
        while (true) {
            val suffix = "_$index"
            val trimmed = candidate.take((31 - suffix.length).coerceAtLeast(1))
            val next = "$trimmed$suffix"
            if (next !in used) {
                used += next
                return next
            }
            index += 1
        }
    }

    fun dateSortKey(value: String): Long {
        val digits = value.filter { it.isDigit() }
        return digits.toLongOrNull() ?: 0L
    }

    fun isSuspiciousQty(qty: Double): Boolean = qty <= suspiciousQtyMinTon || qty > suspiciousQtyMaxTon

    fun voucherRowStyle(baseStyle: Int, qty: Double): Int = if (isSuspiciousQty(qty)) 5 else baseStyle

    val header = listOf(
        "السائق", "رقم السيارة", "وصلات حلفاية", "كمية حلفاية (طن)", "حساب حلفاية (د.ع)",
        "وصلات المعمل", "كمية المعمل (طن)", "حساب المعمل (د.ع)", "الإجمالي (د.ع)"
    )

    val summaryRows = mutableListOf<List<XCell>>()
    summaryRows += listOf(XCell("الفترة", false), XCell(periodLabel, false))
    summaryRows += header.map { XCell(it, false, styleIndex = 1) }

    drivers.forEach { driver ->
        val stats = statsMap[driver.id] ?: DriverMonthlyStats()
        val totalAmount = (stats.tripAmount + stats.factoryAmount).toLong()
        summaryRows += listOf(
            XCell(driver.name, false),
            XCell(driver.carNumber, false),
            XCell(stats.trips.toString(), true),
            XCell(excelNumber(stats.tripQuantity), true),
            XCell(stats.tripAmount.toLong().toString(), true),
            XCell(stats.factoryTrips.toString(), true),
            XCell(excelNumber(stats.factoryQuantity), true),
            XCell(stats.factoryAmount.toLong().toString(), true),
            XCell(totalAmount.toString(), true)
        )
    }

    val grandTotal = (total.tripAmount + total.factoryAmount).toLong()
    summaryRows += listOf(
        XCell("", false),
        XCell("المجموع العام", false),
        XCell(total.trips.toString(), true),
        XCell(excelNumber(total.tripQuantity), true),
        XCell(total.tripAmount.toLong().toString(), true),
        XCell(total.factoryTrips.toString(), true),
        XCell(excelNumber(total.factoryQuantity), true),
        XCell(total.factoryAmount.toLong().toString(), true),
        XCell(grandTotal.toString(), true)
    )

    val totalTripAmountView = amountDisplay(total.tripAmount)
    val totalFactoryAmountView = amountDisplay(total.factoryAmount)
    val totalAmountView = amountDisplay(total.tripAmount + total.factoryAmount)
    summaryRows += listOf(
        XCell("ملخص مختصر عربي", false),
        XCell("", false),
        XCell("", false),
        XCell("", false),
        XCell(compactWithFull(totalTripAmountView), false),
        XCell("", false),
        XCell("", false),
        XCell(compactWithFull(totalFactoryAmountView), false),
        XCell(compactWithFull(totalAmountView), false)
    )

    val sheets = mutableListOf<XSheet>()
    val usedSheetNames = mutableSetOf<String>()
    sheets += XSheet(name = uniqueSheetName("الملخص العام", usedSheetNames), rows = summaryRows)

    drivers.forEachIndexed { index, driver ->
        val stats = statsMap[driver.id] ?: DriverMonthlyStats()
        val report = reportsMap[driver.id] ?: DriverMonthlyReport()
        val rows = mutableListOf<List<XCell>>()
        val driverTotal = stats.tripAmount + stats.factoryAmount

        rows += listOf(XCell("تقرير السائق", false), XCell(driver.name, false))
        rows += listOf(XCell("رقم السيارة", false), XCell(driver.carNumber, false))
        rows += listOf(XCell("الفترة", false), XCell(periodLabel, false))
        rows += listOf(
            XCell("معيار تنبيه الكمية", false),
            XCell(
                "<= ${formatReadableNumber(suspiciousQtyMinTon, maxFractionDigits = 0)} أو > ${formatReadableNumber(suspiciousQtyMaxTon, maxFractionDigits = 0)} طن",
                false
            )
        )
        val allTrips = (report.halafayaTrips + report.factoryTrips)
        val datedTrips = allTrips
            .map { trip ->
                val dateLabel = trip.unloadDate.ifBlank { trip.loadDate }
                val key = dateSortKey(dateLabel)
                dateLabel to key
            }
            .filter { (_, key) -> key > 0L }
        val newestDate = datedTrips.maxByOrNull { it.second }?.first ?: "-"
        val oldestDate = datedTrips.minByOrNull { it.second }?.first ?: "-"
        rows += listOf(XCell("نطاق التواريخ", false), XCell("من $oldestDate إلى $newestDate", false))
        rows += listOf(XCell("", false))
        rows += listOf(
            XCell("إجمالي حلفاية", false), XCell(formatAmountReadable(stats.tripAmount), true),
            XCell("إجمالي المعمل", false), XCell(formatAmountReadable(stats.factoryAmount), true),
            XCell("الإجمالي", false), XCell(formatAmountReadable(driverTotal), true)
        )
        rows += listOf(XCell("", false))
        rows += listOf(
            XCell("النوع", false, styleIndex = 1),
            XCell("رقم الوصل", false, styleIndex = 1),
            XCell("الوجهة", false, styleIndex = 1),
            XCell("تاريخ التحميل", false, styleIndex = 1),
            XCell("تاريخ التفريغ", false, styleIndex = 1),
            XCell("الكمية (طن)", false, styleIndex = 1)
        )

        val sortedHalafayaTrips = report.halafayaTrips.sortedWith(
            compareByDescending<DriverTripRecord> { dateSortKey(it.unloadDate.ifBlank { it.loadDate }) }
                .thenByDescending { dateSortKey(it.loadDate) }
                .thenByDescending { it.docNumber.toLongOrNull() ?: 0L }
        )
        val sortedFactoryTrips = report.factoryTrips.sortedWith(
            compareByDescending<DriverTripRecord> { dateSortKey(it.unloadDate.ifBlank { it.loadDate }) }
                .thenByDescending { dateSortKey(it.loadDate) }
                .thenByDescending { it.docNumber.toLongOrNull() ?: 0L }
        )

        sortedHalafayaTrips.forEach { trip ->
            val rowStyle = voucherRowStyle(baseStyle = 2, qty = trip.quantity)
            rows += listOf(
                XCell("حلفاية", false, styleIndex = rowStyle),
                XCell(trip.docNumber, false, styleIndex = rowStyle),
                XCell(trip.destination, false, styleIndex = rowStyle),
                XCell(trip.loadDate, false, styleIndex = rowStyle),
                XCell(trip.unloadDate, false, styleIndex = rowStyle),
                XCell(excelNumber(trip.quantity), true, styleIndex = rowStyle)
            )
        }

        sortedFactoryTrips.forEach { trip ->
            val rowStyle = voucherRowStyle(baseStyle = 3, qty = trip.quantity)
            rows += listOf(
                XCell("معمل", false, styleIndex = rowStyle),
                XCell(trip.docNumber, false, styleIndex = rowStyle),
                XCell(trip.destination, false, styleIndex = rowStyle),
                XCell(trip.loadDate, false, styleIndex = rowStyle),
                XCell(trip.unloadDate, false, styleIndex = rowStyle),
                XCell(excelNumber(trip.quantity), true, styleIndex = rowStyle)
            )
        }

        if (sortedHalafayaTrips.isNotEmpty() || sortedFactoryTrips.isNotEmpty()) {
            val halafayaQtySum = sortedHalafayaTrips.sumOf { it.quantity }
            val factoryQtySum = sortedFactoryTrips.sumOf { it.quantity }
            val totalQtySum = halafayaQtySum + factoryQtySum

            rows += listOf(XCell("", false))
            rows += listOf(
                XCell("مجموع حلفاية", false, styleIndex = 4),
                XCell("عدد الوصولات", false, styleIndex = 4),
                XCell(sortedHalafayaTrips.size.toString(), true, styleIndex = 4),
                XCell("", false, styleIndex = 4),
                XCell("", false, styleIndex = 4),
                XCell(excelNumber(halafayaQtySum), true, styleIndex = 4)
            )
            rows += listOf(
                XCell("مجموع المعمل", false, styleIndex = 4),
                XCell("عدد الوصولات", false, styleIndex = 4),
                XCell(sortedFactoryTrips.size.toString(), true, styleIndex = 4),
                XCell("", false, styleIndex = 4),
                XCell("", false, styleIndex = 4),
                XCell(excelNumber(factoryQtySum), true, styleIndex = 4)
            )
            rows += listOf(
                XCell("الإجمالي", false, styleIndex = 4),
                XCell("", false, styleIndex = 4),
                XCell((sortedHalafayaTrips.size + sortedFactoryTrips.size).toString(), true, styleIndex = 4),
                XCell("", false, styleIndex = 4),
                XCell("", false, styleIndex = 4),
                XCell(excelNumber(totalQtySum), true, styleIndex = 4)
            )
        }

        if (sortedHalafayaTrips.isEmpty() && sortedFactoryTrips.isEmpty()) {
            rows += listOf(XCell("لا توجد وصولات لهذا السائق في الفترة المحددة", false))
        }

        val baseName = "${driver.name}_${index + 1}"
        val tableStartRow = 8
        val tableEndRow = rows.size.coerceAtLeast(tableStartRow)
        sheets += XSheet(
            name = uniqueSheetName(baseName, usedSheetNames),
            rows = rows,
            freezeTopRows = tableStartRow - 1,
            autoFilterRef = "A${tableStartRow}:F${tableEndRow}"
        )
    }

    val safePeriod = periodLabel.replace("/", "-").replace(" ", "_")
    val file = File(context.cacheDir, "تقرير_الشركة_${safePeriod}.xlsx")
    ZipOutputStream(FileOutputStream(file)).use { zip ->
        val sheetOverrides = buildString {
            sheets.indices.forEach { i ->
                append("    <Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>\n")
            }
        }.trimEnd()

        writeZipEntry(
            zip,
            "[Content_Types].xml",
            """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    $sheetOverrides
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>
            """.trimIndent()
        )

        writeZipEntry(
            zip,
            "_rels/.rels",
            """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>
            """.trimIndent()
        )

        writeZipEntry(
            zip,
            "xl/workbook.xml",
            buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                appendLine("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                appendLine("    <sheets>")
                sheets.forEachIndexed { i, sheet ->
                    val name = sheet.name
                    appendLine("        <sheet name=\"${escapeXml(name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
                }
                appendLine("    </sheets>")
                appendLine("</workbook>")
            }
        )

        val workbookRels = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            appendLine("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
            sheets.indices.forEach { i ->
                appendLine("    <Relationship Id=\"rId${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>")
            }
            appendLine("    <Relationship Id=\"rId${sheets.size + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
            appendLine("</Relationships>")
        }

        writeZipEntry(
            zip,
            "xl/_rels/workbook.xml.rels",
            workbookRels
        )

        writeZipEntry(
            zip,
            "xl/styles.xml",
            """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
    <fills count="6">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF2A4154"/><bgColor indexed="64"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF1D4D55"/><bgColor indexed="64"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF4D3C1D"/><bgColor indexed="64"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF244B39"/><bgColor indexed="64"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF5A2631"/><bgColor indexed="64"/></patternFill></fill>
    </fills>
    <borders count="1"><border/></borders>
    <cellStyleXfs count="1"><xf/></cellStyleXfs>
    <cellXfs count="6">
        <xf xfId="0" fontId="0" fillId="0" borderId="0"/>
        <xf xfId="0" fontId="0" fillId="1" borderId="0" applyFill="1"/>
        <xf xfId="0" fontId="0" fillId="2" borderId="0" applyFill="1"/>
        <xf xfId="0" fontId="0" fillId="3" borderId="0" applyFill="1"/>
        <xf xfId="0" fontId="0" fillId="4" borderId="0" applyFill="1"/>
        <xf xfId="0" fontId="0" fillId="5" borderId="0" applyFill="1"/>
    </cellXfs>
    <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>
            """.trimIndent()
        )

        sheets.forEachIndexed { i, sheet ->
            writeZipEntry(
                zip,
                "xl/worksheets/sheet${i + 1}.xml",
                sheetXml(rows = sheet.rows, freezeTopRows = sheet.freezeTopRows, autoFilterRef = sheet.autoFilterRef)
            )
        }
    }

    return file
}

private fun shareFile(
    context: android.content.Context,
    file: File,
    mimeType: String,
    chooserTitle: String,
    targetPackage: String? = null
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (!targetPackage.isNullOrBlank()) {
            `package` = targetPackage
        }
    }

    try {
        if (targetPackage.isNullOrBlank()) {
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } else {
            context.startActivity(intent)
        }
    } catch (_: ActivityNotFoundException) {
        val msg = if (targetPackage == "com.whatsapp") "واتساب غير مثبت على الجهاز" else "تعذر فتح المشاركة"
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
