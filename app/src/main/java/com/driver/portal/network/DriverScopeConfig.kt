package com.driver.portal.network

import com.driver.portal.BuildConfig

/**
 * Company activation scope shared by the driver app and Apps Script.
 * Always attach these on protected actions (trip/factory/history/wallet/...).
 */
object DriverScopeConfig {
    const val COMPANY_ID = "COMP-001"
    const val ACTIVATION_CODE = "CMP-260704184724-6391"
    const val DEVICE_ID = "desktop-1783285154572-3MZUOKK1"
    val PACKAGE_NAME: String = BuildConfig.APPLICATION_ID

    /** Query/form pairs for GoogleSheetConfig.execUrl */
    fun asQueryPairs(): Array<Pair<String, String>> = arrayOf(
        "companyId" to COMPANY_ID,
        "activationCode" to ACTIVATION_CODE,
        "deviceId" to DEVICE_ID,
        "packageName" to PACKAGE_NAME
    )
}
