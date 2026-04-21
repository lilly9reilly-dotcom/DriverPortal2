package com.driver.portal.network

import com.driver.portal.HistoryResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @FormUrlEncoded
    @POST("exec")
    fun login(
        @Field("action") action: String = "login",
        @Field("name") name: String,
        @Field("phone") phone: String,
        @Field("carNumber") carNumber: String
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("exec")
    fun sendGps(
        @Field("action") action: String = "gps",
        @Field("driverName") driverName: String,
        @Field("carNumber") carNumber: String,
        @Field("lat") lat: Double,
        @Field("lng") lng: Double
    ): Call<ResponseBody>

    @Headers("Content-Type: application/json")
    @POST("exec")
    fun sendTrip(
        @Body request: TripRequest
    ): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("exec")
    fun sendFactory(
        @Body request: FactoryRequest
    ): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("exec")
    fun sendMaintenanceRequest(
        @Body request: MaintenanceRequest
    ): Call<ApiResponse>

    @Headers("Content-Type: application/json")
    @POST("exec")
    fun sendIssue(
        @Body request: IssueRequest
    ): Call<ApiResponse>

    @FormUrlEncoded
    @POST("exec")
    fun checkDocNumber(
        @Field("action") action: String = "checkDoc",
        @Field("docNumber") docNumber: String
    ): Call<ApiResponse>

    @GET("exec")
    fun getHistory(
        @Query("action") action: String = "history",
        @Query("driverName") driverName: String
    ): Call<HistoryResponse>

    @GET("exec")
    fun getMaintenanceRequests(
        @Query("action") action: String = "getMaintenance",
        @Query("carNumber") carNumber: String
    ): Call<MaintenanceResponse>

    @FormUrlEncoded
    @POST("exec")
    fun registerDriver(
        @Field("action") action: String = "registerDriver",
        @Field("name") name: String,
        @Field("phone") phone: String,
        @Field("carNumber") carNumber: String
    ): Call<ApiResponse>
}