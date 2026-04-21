package com.driver.portal

data class DriverLocation(
    val driver: String,
    val carNumber: String,
    val lat: Double,
    val lng: Double,
    val status: String
)