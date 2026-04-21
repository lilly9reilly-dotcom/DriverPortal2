package com.driver.portal.network

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object TripRepository {

    // ========================================
    // إرسال وصل التحميل
    // ========================================
    fun sendTrip(
        trip: TripRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val request = trip.copy(action = "trip")

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
        onError: (String) -> Unit
    ) {

        val request = factory.copy(action = "factory")

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
    fun checkDocNumber(
        docNumber: String,
        onResult: (Boolean) -> Unit
    ) {
        RetrofitClient.instance.checkDocNumber("checkDoc", docNumber)
            .enqueue(object : Callback<ApiResponse> {

                override fun onResponse(
                    call: Call<ApiResponse>,
                    response: Response<ApiResponse>
                ) {
                    if (response.isSuccessful) {
                        val message = response.body()?.message
                        if (message == "EXISTS") {
                            onResult(true)
                        } else {
                            onResult(false)
                        }
                    } else {
                        onResult(false)
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    onResult(false)
                }
            })
    }
}