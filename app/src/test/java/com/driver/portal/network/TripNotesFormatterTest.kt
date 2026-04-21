package com.driver.portal.network

import org.junit.Assert.assertEquals
import org.junit.Test

class TripNotesFormatterTest {

    @Test
    fun merge_appendsFactoryDataToExistingNotes() {
        val merged = TripNotesFormatter.merge(
            baseNotes = "وصول متأخر",
            factoryName = "مستودع التاجي",
            factoryVoucher = "B-42"
        )

        assertEquals(
            "وصول متأخر - اسم المعمل: مستودع التاجي | رقم بوچر المعمل: B-42",
            merged
        )
    }

    @Test
    fun merge_returnsExtrasOnlyWhenBaseNotesAreBlank() {
        val merged = TripNotesFormatter.merge(
            baseNotes = "   ",
            factoryName = "أبو غريب",
            factoryVoucher = ""
        )

        assertEquals("اسم المعمل: أبو غريب", merged)
    }
}
