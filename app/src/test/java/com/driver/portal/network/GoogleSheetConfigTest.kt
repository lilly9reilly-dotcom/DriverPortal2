package com.driver.portal.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSheetConfigTest {

    @Test
    fun execUrl_encodesArabicAndSpacesSafely() {
        val url = GoogleSheetConfig.execUrl(
            action = "wallet",
            "driverName" to "علي حسين مسلم",
            "docNumber" to "12 34"
        )

        assertTrue(url.startsWith(GoogleSheetConfig.EXEC_ENDPOINT))
        assertTrue(url.contains("action=wallet"))
        assertTrue(url.contains("driverName="))
        assertFalse(url.contains("driverName=علي حسين مسلم"))
        assertTrue(url.contains("docNumber=12+34") || url.contains("docNumber=12%2034"))
    }

    @Test
    fun execUrl_reusesSingleStableEndpoint() {
        val url = GoogleSheetConfig.execUrl("route", "driverName" to "سائق تجريبي")

        assertTrue(url.startsWith(GoogleSheetConfig.EXEC_ENDPOINT))
        assertTrue(url.contains("action=route"))
        assertFalse(url.contains(" "))
    }
}
