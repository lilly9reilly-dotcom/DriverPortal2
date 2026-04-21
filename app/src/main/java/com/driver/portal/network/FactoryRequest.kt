package com.driver.portal.network

data class FactoryRequest(
    val action: String = "factory",
    val docNumber: String,
    val driverName: String,
    val carNumber: String,
    val quantity: String,
    val factory: String,
    val unloadDate: String,
    val fileData: String
)