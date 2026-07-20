package com.driver.portal

import com.driver.portal.network.GoogleSheetConfig

object GasSheetConfig {
    const val GAS_SHEET_ID: String = "1a7r3rXY7dPyUjKCdvNopK2Y9ufKYhda0o6DCYBukv2o"
    const val GAS_TRANSACTIONS_GID: String = "1253581071"

    const val MVP_SHEET_NAME: String = "GasStation_DB"

    val SHEET_URL: String = "https://docs.google.com/spreadsheets/d/$GAS_SHEET_ID/edit"
    val MVP_SHEET_URL: String = "$SHEET_URL?gid=$GAS_TRANSACTIONS_GID#gid=$GAS_TRANSACTIONS_GID"

    // This points to the same Apps Script backend root. Gas actions are isolated by action names.
    val GAS_EXEC_ENDPOINT: String = GoogleSheetConfig.EXEC_ENDPOINT
}
