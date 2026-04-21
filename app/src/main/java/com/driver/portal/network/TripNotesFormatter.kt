package com.driver.portal.network

object TripNotesFormatter {

    fun merge(baseNotes: String, factoryName: String, factoryVoucher: String): String {
        val extras = buildList {
            factoryName.trim()
                .takeIf { it.isNotEmpty() }
                ?.let { add("اسم المعمل: $it") }
            factoryVoucher.trim()
                .takeIf { it.isNotEmpty() }
                ?.let { add("رقم بوچر المعمل: $it") }
        }.joinToString(" | ")

        val notes = baseNotes.trim()
        if (extras.isEmpty()) return notes
        if (notes.isEmpty()) return extras
        return "$notes - $extras"
    }
}
