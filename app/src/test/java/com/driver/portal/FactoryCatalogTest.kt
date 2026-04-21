package com.driver.portal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FactoryCatalogTest {

    @Test
    fun includesHabibiyaFactoryWithNavigationData() {
        val destination = FactoryCatalog.findByName("حبيبية")

        assertNotNull(destination)
        assertTrue(destination!!.googleQuery.contains("حبيبية"))
        assertTrue(destination.latitude != 0.0)
        assertTrue(destination.longitude != 0.0)
    }

    @Test
    fun namesAreUniqueAndStable() {
        val names = FactoryCatalog.all.map { it.name }

        assertEquals(names.size, names.toSet().size)
        assertFalse(names.contains("مشثل"))
        assertTrue(names.contains("مشعل"))
    }
}
