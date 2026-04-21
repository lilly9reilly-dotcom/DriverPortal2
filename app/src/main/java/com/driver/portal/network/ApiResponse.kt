package com.driver.portal.network

data class DriverStats(
    val trips: Int? = 0,
    val qty: Double? = 0.0,
    val gas: Double? = 0.0,
    val netSalary: Double? = 0.0
)

data class ApiResponse(
    val success: Boolean,
    val message: String?,
    val driver: String?,
    val carNumber: String?,
    val newDriver: Boolean?
)