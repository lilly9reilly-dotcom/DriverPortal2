package com.driver.portal.network

import android.content.Context
import java.util.Locale

object DocNumberGuard {

    private const val PREF_NAME = "doc_guard"
    private const val KEY_USED_DOCS = "used_doc_numbers"

    fun normalize(raw: String): String {
        val trimmed = raw.trim().replace("\\s+".toRegex(), "")
        if (trimmed.isBlank()) return ""

        val normalizedDigits = buildString(trimmed.length) {
            for (ch in trimmed) {
                append(
                    when (ch) {
                        '٠' -> '0'
                        '١' -> '1'
                        '٢' -> '2'
                        '٣' -> '3'
                        '٤' -> '4'
                        '٥' -> '5'
                        '٦' -> '6'
                        '٧' -> '7'
                        '٨' -> '8'
                        '٩' -> '9'
                        else -> ch
                    }
                )
            }
        }

        return normalizedDigits.lowercase(Locale.ROOT)
    }

    fun isUsedLocally(context: Context, rawDocNumber: String): Boolean {
        val normalized = normalize(rawDocNumber)
        if (normalized.isBlank()) return false

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val used = prefs.getStringSet(KEY_USED_DOCS, emptySet()) ?: emptySet()
        return used.contains(normalized)
    }

    fun markUsed(context: Context, rawDocNumber: String) {
        val normalized = normalize(rawDocNumber)
        if (normalized.isBlank()) return

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val used = (prefs.getStringSet(KEY_USED_DOCS, emptySet()) ?: emptySet()).toMutableSet()
        used.add(normalized)
        prefs.edit().putStringSet(KEY_USED_DOCS, used).apply()
    }
}