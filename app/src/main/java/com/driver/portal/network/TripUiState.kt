package com.driver.portal.network

data class TripUiState(
    val docNumber: String = "",
    val driverName: String = "",
    val carNumber: String = "",
    val loadDate: String = "",
    val unloadDate: String = "",
    val quantity: String = "",
    val liters: String = "",
    val ownerType: String = "",
    val destination: String = "",
    val factoryName: String = "",
    val factoryVoucher: String = "",
    val price: String = "",
    val notes: String = "",
    val imageData: String = ""
) {
    fun updateDocNumber(value: String) = copy(docNumber = value)
    fun updateDriverName(value: String) = copy(driverName = value)
    fun updateCarNumber(value: String) = copy(carNumber = value)
    fun updateQuantity(value: String) = copy(quantity = value)
    fun updateLiters(value: String) = copy(liters = value)
}