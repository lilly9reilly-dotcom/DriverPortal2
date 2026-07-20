package com.driver.portal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class GasRemoteReportData(
    val transactions: List<GasTransaction>,
    val settlements: List<GasSettlement>,
)

object GasRemoteRepository {
    private val apiDateTimeFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
    private val isoDateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun requireSuccess(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) return raw

        val json = JSONObject(trimmed)
        if (!json.optBoolean("success", false)) {
            throw IllegalStateException(json.optString("message", "فشل حفظ البيانات"))
        }

        return raw
    }

    private suspend fun postForm(params: Map<String, String>): String = withContext(Dispatchers.IO) {
        val encoded = params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }

        val connection = (URL(GasSheetConfig.GAS_EXEC_ENDPOINT).openConnection() as HttpURLConnection).apply {
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

            val responseStream = if (connection.responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            BufferedReader(responseStream.reader(StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun addTransaction(transaction: GasTransaction, stationName: String): Result<String> = runCatching {
        val params = mapOf(
            "action" to "gas_mvp_add_transaction",
            "row_type" to "TRANSACTION",
            "record_id" to transaction.receiptNumber,
            "created_at" to formatApiDateTime(transaction.createdAt),
            "fill_date" to transaction.fillDate,
            "fuel_type" to transaction.fuelType,
            "station_name" to stationName,
            "plate_number" to transaction.plateNumber,
            "driver_name" to transaction.driverName,
            "liters" to transaction.liters.toString(),
            "price_per_liter" to transaction.pricePerLiter.toString(),
            "total_amount" to transaction.totalAmount.toString(),
            "entered_by" to transaction.enteredBy,
            "notes" to transaction.notes,
            "status" to "ACTIVE"
        )
        val raw = requireSuccess(postForm(params))
        raw
    }

    suspend fun updateTransaction(transaction: GasTransaction, stationName: String, originalReceiptNumber: String): Result<String> = runCatching {
        val params = mapOf(
            "action" to "gas_mvp_update_transaction",
            "row_type" to "TRANSACTION",
            "target_record_id" to originalReceiptNumber,
            "record_id" to transaction.receiptNumber,
            "created_at" to formatApiDateTime(transaction.createdAt),
            "fill_date" to transaction.fillDate,
            "fuel_type" to transaction.fuelType,
            "station_name" to stationName,
            "plate_number" to transaction.plateNumber,
            "driver_name" to transaction.driverName,
            "liters" to transaction.liters.toString(),
            "price_per_liter" to transaction.pricePerLiter.toString(),
            "total_amount" to transaction.totalAmount.toString(),
            "entered_by" to transaction.enteredBy,
            "notes" to transaction.notes,
            "status" to "ACTIVE"
        )
        val raw = requireSuccess(postForm(params))
        raw
    }

    suspend fun addSettlement(settlement: GasSettlement, stationName: String): Result<String> = runCatching {
        val params = mapOf(
            "action" to "gas_mvp_add_settlement",
            "row_type" to "SETTLEMENT",
            "record_id" to settlement.id,
            "created_at" to formatApiDateTime(settlement.createdAt),
            "station_name" to stationName,
            "payment_amount" to settlement.amount.toString(),
            "created_by" to settlement.createdBy,
            "notes" to settlement.notes,
            "status" to "ACTIVE"
        )
        val raw = requireSuccess(postForm(params))
        raw
    }

    suspend fun addVehicle(vehicleId: String, plateNumber: String, driverName: String, stationName: String): Result<String> = runCatching {
        requireSuccess(postForm(
            mapOf(
                "action" to "gas_mvp_add_vehicle",
                "row_type" to "VEHICLE",
                "record_id" to vehicleId,
                "created_at" to formatApiDateTime(System.currentTimeMillis()),
                "station_name" to stationName,
                "plate_number" to plateNumber,
                "driver_name" to driverName,
                "active" to "true",
                "status" to "ACTIVE"
            )
        ))
    }

    suspend fun updateSettings(stationName: String, normalPrice: Double, commercialPrice: Double): Result<String> = runCatching {
        requireSuccess(postForm(
            mapOf(
                "action" to "gas_mvp_update_settings",
                "row_type" to "SETTING",
                "record_id" to "settings",
                "station_name" to stationName,
                "default_price_normal" to normalPrice.toString(),
                "default_price_commercial" to commercialPrice.toString(),
                "status" to "ACTIVE"
            )
        ))
    }

    suspend fun fetchReportData(stationName: String): Result<GasRemoteReportData> = runCatching {
        val raw = requireSuccess(postForm(mapOf("action" to "gas_mvp_list")))
        val json = JSONObject(raw)
        val rows = json.optJSONArray("rows") ?: JSONArray()

        val transactions = buildList {
            for (index in 0 until rows.length()) {
                val item = rows.optJSONObject(index) ?: continue
                if (!isMatchingStation(item, stationName)) continue
                if (!item.optString("status", "ACTIVE").equals("ACTIVE", ignoreCase = true)) continue
                if (!item.optString("row_type").equals("TRANSACTION", ignoreCase = true)) continue

                val receiptNumber = item.optString("record_id").trim().ifBlank { item.optString("receipt_number").trim() }
                add(
                    GasTransaction(
                        id = receiptNumber,
                        receiptNumber = receiptNumber,
                        fillDate = normalizeRemoteFillDate(item.optString("fill_date")),
                        fuelType = item.optString("fuel_type", "اعتيادي"),
                        plateNumber = item.optString("plate_number"),
                        driverName = item.optString("driver_name"),
                        liters = item.optDouble("liters"),
                        pricePerLiter = item.optDouble("price_per_liter"),
                        totalAmount = item.optDouble("total_amount"),
                        notes = item.optString("notes"),
                        enteredBy = item.optString("entered_by"),
                        createdAt = parseRemoteDateTime(item.optString("created_at"))
                    )
                )
            }
        }.distinctBy { transactionKey(it.receiptNumber, it.id) }.sortedByDescending { it.createdAt }

        val settlements = buildList {
            for (index in 0 until rows.length()) {
                val item = rows.optJSONObject(index) ?: continue
                if (!isMatchingStation(item, stationName)) continue
                if (!item.optString("status", "ACTIVE").equals("ACTIVE", ignoreCase = true)) continue
                if (!item.optString("row_type").equals("SETTLEMENT", ignoreCase = true)) continue

                add(
                    GasSettlement(
                        id = item.optString("record_id").ifBlank { parseRemoteDateTime(item.optString("created_at")).toString() },
                        amount = item.optDouble("payment_amount"),
                        notes = item.optString("notes"),
                        createdAt = parseRemoteDateTime(item.optString("created_at")),
                        createdBy = item.optString("created_by")
                    )
                )
            }
        }.sortedByDescending { it.createdAt }

        GasRemoteReportData(transactions = transactions, settlements = settlements)
    }

    private fun formatApiDateTime(value: Long): String =
        apiDateTimeFormat.format(Date(value))

    private fun parseRemoteDateTime(value: String): Long {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return System.currentTimeMillis()

        return runCatching { isoDateTimeFormat.parse(trimmed)?.time }.getOrNull()
            ?: runCatching { apiDateTimeFormat.parse(trimmed)?.time }.getOrNull()
            ?: System.currentTimeMillis()
    }

    private fun normalizeRemoteFillDate(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return ""

        return runCatching { isoDateTimeFormat.parse(trimmed) }.getOrNull()
            ?.let { SimpleDateFormat("yyyy/MM/dd", Locale.US).format(it) }
            ?: trimmed.replace('-', '/').replace('.', '/').take(10)
    }

    private fun isMatchingStation(item: JSONObject, stationName: String): Boolean {
        val remoteStation = item.optString("station_name").trim()
        return remoteStation.isBlank() || remoteStation.equals(stationName.trim(), ignoreCase = true)
    }

    private fun transactionKey(receiptNumber: String, fallbackId: String): String =
        receiptNumber.trim().ifBlank { fallbackId.trim() }.lowercase(Locale.ROOT)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
