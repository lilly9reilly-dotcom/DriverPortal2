package com.driver.portal.network

data class IssueRequest(
    val action: String = "reportIssue",
    val driverName: String,
    val docNumber: String,
    val issueType: String,
    val note: String,
    val companyId: String = DriverScopeConfig.COMPANY_ID,
    val activationCode: String = DriverScopeConfig.ACTIVATION_CODE,
    val deviceId: String = DriverScopeConfig.DEVICE_ID,
    val packageName: String = DriverScopeConfig.PACKAGE_NAME
)