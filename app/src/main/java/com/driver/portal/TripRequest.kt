package com.driver.portal.network

data class TripRequest(

    // نوع العملية (ثابت = trip)
    val action: String = "trip",

    // معلومات الوصل
    val docNumber: String,
    val driverName: String,
    val carNumber: String,

    // التواريخ
    val loadDate: String,
    val unloadDate: String,

    // الكمية
    val quantity: String,
    val liters: String,

    // نوع الملك
    val ownerType: String,

    // المحطة
    val destination: String,

    // المعمل (يترك فارغ في التحميل)
    val factory: String = "",

    // البوجر
    val bojer: String = "",

    // ملاحظات
    val notes: String = "",

    // سعر النقلة
    val price: String,

    // صورة الوصل
    val fileData: String
)