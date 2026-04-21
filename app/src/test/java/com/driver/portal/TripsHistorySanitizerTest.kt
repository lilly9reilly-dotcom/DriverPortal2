package com.driver.portal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripsHistorySanitizerTest {

    @Test
    fun sanitizeTripForDisplay_hidesRawDriveLinkAndKeepsUsefulFields() {
        val rawTrip = TripItem(
            docNumber = "1223",
            carNumber = "506070",
            loadDate = "1052",
            unloadDate = "نهروان",
            quantity = "https://drive.google.com/file/d/abc/view",
            station = "",
            price = "",
            total = "",
            date = "",
            status = "ok",
            imageUrl = "",
            sendTime = "",
            notes = "",
            owner = "٢٠٢٦-٠٤-١٧",
            finalQuantity = "0",
            finalAmount = "0"
        )

        val sanitized = sanitizeTripForDisplay(rawTrip)

        assertEquals("نهروان", sanitized.station)
        assertTrue(sanitized.quantity.isNullOrBlank())
        assertTrue(sanitized.loadDate.isNullOrBlank())
        assertTrue(sanitized.imageUrl.orEmpty().contains("drive.google.com"))
    }

    @Test
    fun formatDate_formatsBackendEnglishDateSafely() {
        val formatted = formatDate("Fri Apr 17 2026 01:05:31 GMT+0300 (التوقيت العربي الرسمي)")

        assertTrue(formatted.isNotBlank())
        assertFalse(formatted.contains("GMT"))
    }
}