package com.driver.portal.network

data class MaintenanceRequest(
    val action: String = "saveMaintenance",
    val driver: String,
    val vehicle: String,
    val problem: String,
    val price: Double,
    val companyId: String = DriverScopeConfig.COMPANY_ID,
    val activationCode: String = DriverScopeConfig.ACTIVATION_CODE,
    val deviceId: String = DriverScopeConfig.DEVICE_ID,
    val packageName: String = DriverScopeConfig.PACKAGE_NAME
)
