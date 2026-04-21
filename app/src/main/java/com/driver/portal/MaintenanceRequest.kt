package com.driver.portal.network

data class MaintenanceRequest(
    val action: String = "saveMaintenance",
    val driver: String,
    val vehicle: String,
    val problem: String,
    val price: Double
)