package com.driver.portal.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import org.json.JSONObject

object TripRepository {

    enum class DocCheckResult {
        EXISTS,
        AVAILABLE,
        UNVERIFIED
    }

    // ========================================
    // إرسال وصل التحميل
    // ========================================
    fun sendTrip(
        trip: TripRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        companyId: String = "",
        activationCode: String = ""
    ) {

        val request = trip.copy(
            action = "trip",
            companyId = companyId.trim().ifEmpty { DriverScopeConfig.COMPANY_ID },
            activationCode = activationCode.trim().ifEmpty { DriverScopeConfig.ACTIVATION_CODE },
            deviceId = DriverScopeConfig.DEVICE_ID,
            packageName = DriverScopeConfig.PACKAGE_NAME
        )

        RetrofitClient.instance.sendTrip(request)
            .enqueue(object : Callback<ApiResponse> {

                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body?.success == true) {
                            onSuccess()
                        } else {
                            onError(body?.message ?: "فشل في الحفظ")
                        }
                    } else {
                        onError("فشل في الاستجابة")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    onError("خطأ إنترنت")
                }
            })
    }

    // ========================================
    // إرسال وصل المعمل
    // ========================================
    fun sendFactory(
        factory: FactoryRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        companyId: String = "",
        activationCode: String = ""
    ) {

        val request = factory.copy(
            action = "factory",
            companyId = companyId.trim().ifEmpty { DriverScopeConfig.COMPANY_ID },
            activationCode = activationCode.trim().ifEmpty { DriverScopeConfig.ACTIVATION_CODE },
            deviceId = DriverScopeConfig.DEVICE_ID,
            packageName = DriverScopeConfig.PACKAGE_NAME
        )

        RetrofitClient.instance.sendFactory(request)
            .enqueue(object : Callback<ApiResponse> {

                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val body = response.body()

                        if (body?.success == true) {
                            onSuccess()
                        } else {
                            onError(body?.message ?: "فشل في إرسال وصل المعمل")
                        }
                    } else {
                        onError("فشل في الاستجابة")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    onError("خطأ إنترنت")
                }
            })
    }

    // ========================================
    // فحص رقم الوصل
    // ========================================
    private fun normalizeCheckText(raw: String): String {
        return raw
            .trim()
            .replace("\\s+".toRegex(), " ")
            .lowercase(Locale.ROOT)
    }

    private fun containsAny(text: String, words: List<String>): Boolean {
        return words.any { text.contains(it) }
    }

    private fun parseDocCheck(rawBody: String): DocCheckResult {
        val availableWords = listOf(
            "not_exists", "not exists", "not found", "available", "new", "ok",
            "not used", "not duplicate", "does not exist",
            "غير موجود", "متاح", "جديد", "غير مستخدم", "غير مكرر"
        )
        val duplicateWords = listOf(
            "duplicate", "already used", "used before",
            "مكرر", "موجود مسبق", "مستخدم مسبق", "تم استخدامه"
        )

        // أولوية لقراءة JSON الحقيقي من الخادم: {"success":true,"exists":false}
        try {
            val json = JSONObject(rawBody)

            if (json.has("exists") && !json.isNull("exists")) {
                return if (json.optBoolean("exists", false)) {
                    DocCheckResult.EXISTS
                } else {
                    DocCheckResult.AVAILABLE
                }
            }

            if (json.has("isExists") && !json.isNull("isExists")) {
                return if (json.optBoolean("isExists", false)) {
                    DocCheckResult.EXISTS
                } else {
                    DocCheckResult.AVAILABLE
                }
            }

            val message = normalizeCheckText(json.optString("message", ""))
            if (containsAny(message, availableWords)) return DocCheckResult.AVAILABLE
            if (containsAny(message, duplicateWords)) return DocCheckResult.EXISTS

            val success = if (json.has("success") && !json.isNull("success")) {
                json.optBoolean("success", false)
            } else {
                false
            }

            if (success) return DocCheckResult.AVAILABLE
        } catch (_: Exception) {
            // ليس JSON، نكمل على فحص النص الخام
        }

        val text = normalizeCheckText(rawBody)
        if (containsAny(text, availableWords)) return DocCheckResult.AVAILABLE

        // لا نعتبر "exists" وحدها دليلاً؛ فقط الصيغ الصريحة مثل exists:true
        if (text.contains("\"exists\":true") || text.contains("isexists:true")) {
            return DocCheckResult.EXISTS
        }
        if (containsAny(text, duplicateWords)) return DocCheckResult.EXISTS

        return DocCheckResult.UNVERIFIED
    }

    fun checkDocNumber(
        docNumber: String,
        driverName: String = "",
        carNumber: String = "",
        onResult: (DocCheckResult) -> Unit,
        companyId: String = "",
        activationCode: String = ""
    ) {
        RetrofitClient.instance.checkDocNumberRaw(
            action = "checkDoc",
            docNumber = docNumber,
            driverName = driverName,
            carNumber = carNumber,
            companyId = companyId.trim().ifEmpty { DriverScopeConfig.COMPANY_ID },
            activationCode = activationCode.trim().ifEmpty { DriverScopeConfig.ACTIVATION_CODE },
            deviceId = DriverScopeConfig.DEVICE_ID,
            packageName = DriverScopeConfig.PACKAGE_NAME
        ).enqueue(object : Callback<okhttp3.ResponseBody> {

                override fun onResponse(
                    call: Call<okhttp3.ResponseBody>,
                    response: Response<okhttp3.ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        val raw = response.body()?.string().orEmpty()
                        onResult(parseDocCheck(raw))
                    } else {
                        val rawError = response.errorBody()?.string().orEmpty()
                        if (rawError.isNotBlank()) {
                            onResult(parseDocCheck(rawError))
                        } else {
                            onResult(DocCheckResult.UNVERIFIED)
                        }
                    }
                }

                override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                    onResult(DocCheckResult.UNVERIFIED)
                }
            })
    }
}