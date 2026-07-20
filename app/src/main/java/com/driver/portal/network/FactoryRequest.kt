package com.driver.portal.network

data class FactoryRequest(
    val action: String = "factory",
    val docNumber: String,      // A رقم الوصل
    val driverName: String,     // B اسم السائق
    val carNumber: String,      // C رقم السيارة
    val loadDate: String,       // D تاريخ تحميل
    val unloadDate: String,     // E تاريخ تفريغ
    val quantity: String,       // F الكمية
    val owner: String = "",    // G المالك
    val factory: String,        // G اسم المعمل
    val fileData: String,       // H صورة
    val notes: String = "",    // I الملاحظات
    val companyId: String = "",
    val activationCode: String = "",
    val deviceId: String = "",
    val packageName: String = ""
)