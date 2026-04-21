package com.driver.portal

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class FactoryDestination(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val googleQuery: String
)

object FactoryCatalog {

    val all: List<FactoryDestination> = listOf(
        FactoryDestination("معمل الحبيبية", 33.3443, 44.4960, "معمل غاز الحبيبية بغداد"),
        FactoryDestination("مستودع التاجي", 33.5384, 44.2461, "مستودع غاز التاجي بغداد"),
        FactoryDestination("معمل أبو غريب", 33.3053, 44.0686, "معمل غاز أبو غريب بغداد"),
        FactoryDestination("معمل الغزالية", 33.3248, 44.2708, "معمل غاز الغزالية بغداد"),
        FactoryDestination("معمل العامرية", 33.2972, 44.2263, "معمل غاز العامرية بغداد"),
        FactoryDestination("معمل الصمود", 33.3674, 44.4201, "معمل غاز الصمود بغداد"),
        FactoryDestination("معمل الزعفرانية", 33.2557, 44.5157, "معمل غاز الزعفرانية بغداد"),
        FactoryDestination("مشعل", 33.3518, 44.4512, "معمل غاز المشتل بغداد"),
        FactoryDestination("معمل النهضة", 33.3448, 44.4303, "معمل غاز النهضة بغداد"),
        FactoryDestination("معمل الرصافة", 33.3406, 44.4009, "معمل غاز الرصافة بغداد"),
        FactoryDestination("معمل النهروان", 33.2333, 44.5937, "معمل غاز النهروان بغداد"),
        FactoryDestination("معمل الحسينية", 33.4388, 44.3756, "معمل غاز الحسينية بغداد"),
        FactoryDestination("معمل طارق", 33.3777, 44.4023, "معمل غاز طارق بغداد"),
        FactoryDestination("معمل باب الشام", 33.4765, 44.2998, "معمل غاز باب الشام بغداد"),
        FactoryDestination("معمل كسرة وعطش", 33.3932, 44.4920, "معمل غاز كسرة وعطش بغداد")
    )

    private val aliases = mapOf(
        "حبيبية" to "معمل الحبيبية",
        "الحبيبية" to "معمل الحبيبية",
        "معمل حبيبية" to "معمل الحبيبية",
        "أبو غريب" to "معمل أبو غريب",
        "غزالية" to "معمل الغزالية",
        "عامرية" to "معمل العامرية",
        "صمود" to "معمل الصمود",
        "زعفرانية" to "معمل الزعفرانية",
        "مشثل" to "مشعل",
        "مشعل" to "مشعل",
        "المشتل" to "مشعل",
        "مشتل" to "مشعل",
        "نهضة" to "معمل النهضة",
        "رصافة" to "معمل الرصافة",
        "نهروان" to "معمل النهروان",
        "حسينية" to "معمل الحسينية",
        "طارق" to "معمل طارق",
        "باب الشام" to "معمل باب الشام",
        "بوب الشام" to "معمل باب الشام",
        "كسرة وعطش" to "معمل كسرة وعطش"
    )

    fun normalizeName(name: String): String {
        val trimmed = name.trim()
        return aliases[trimmed] ?: trimmed
    }

    fun findByName(name: String): FactoryDestination? {
        val normalized = normalizeName(name)
        return all.firstOrNull {
            it.name == normalized ||
                it.name.contains(normalized) ||
                normalized.contains(it.name.removePrefix("معمل "))
        }
    }

    fun openInGoogleMaps(context: Context, destination: FactoryDestination) {
        val encodedLabel = URLEncoder.encode(destination.googleQuery, StandardCharsets.UTF_8.toString())
        val appUri = Uri.parse("google.navigation:q=${destination.latitude},${destination.longitude}&mode=d")
        val webUri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=${destination.latitude},${destination.longitude}&destination_place_id=&travelmode=driving&dir_action=navigate&query=$encodedLabel"
        )

        val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(appIntent)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(webIntent)
            } catch (_: Exception) {
                Toast.makeText(context, "تعذر فتح خرائط Google", Toast.LENGTH_LONG).show()
            }
        }
    }
}
