package com.driver.portal.network

data class MaintenanceResponse(
    val success: Boolean? = null,
    val requests: List<MaintenanceItem> = emptyList()
)

data class MaintenanceItem(
    val requestId: String? = null,
    val driver: String? = null,
    val vehicle: String = "",
    val problem: String = "",
    val status: String? = null,
    val requestDate: String? = null,
    val repairDate: String? = null,
    val type: String? = null,
    val cost: String? = null,
    val location: String? = null,
    val notes: String? = null
)