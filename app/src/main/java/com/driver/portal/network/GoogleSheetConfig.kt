package com.driver.portal.network

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GoogleSheetConfig {

    private const val SCRIPT_ROOT =
        "https://script.google.com/macros/s/AKfycbw-3wKRuKImCvvB4ip3PGokDP18yJz6HDW2QylDmvQGxAbyn8Wq-FIlHQ9ms-i7wlCEQA/"

    const val BASE_URL: String = SCRIPT_ROOT
    const val EXEC_ENDPOINT: String = SCRIPT_ROOT + "exec"

    fun execUrl(action: String, vararg params: Pair<String, String>): String {
        val query = buildList {
            add("action=${encode(action)}")
            params.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    add("${encode(key)}=${encode(value)}")
                }
            }
        }.joinToString("&")

        return "$EXEC_ENDPOINT?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
