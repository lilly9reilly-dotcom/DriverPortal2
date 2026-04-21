package com.driver.portal

data class HistoryResponse(
    val success: Boolean,
    val trips: List<TripItem>
)

data class TripItem(
    val docNumber: String?,
    val carNumber: String?,
    val loadDate: String?,
    val unloadDate: String?,
    val quantity: String?,
    val station: String?,
    val price: String?,
    val total: String?,
    val date: String?,
    val status: String? = "ok",
    val imageUrl: String? = null,
    val sendTime: String? = null,
    val notes: String? = null,
    val owner: String? = null,
    val finalQuantity: String? = null,
    val finalAmount: String? = null
)