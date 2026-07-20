package com.driver.portal.network

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object GoogleSheetConfig {

    private const val API_SCRIPT_ROOT =
        "https://script.google.com/macros/s/AKfycbwCreVvebaAN7C4W2OZu6ura7cza42P2lIssNt4sVBv1raDqZkQYY-ZZyNNcl9_iynhAw/"
    private const val ADMIN_SCRIPT_ROOT = API_SCRIPT_ROOT

    const val BASE_URL: String = API_SCRIPT_ROOT
    const val EXEC_ENDPOINT: String = API_SCRIPT_ROOT + "exec"
    const val GPS_EXEC_ENDPOINT: String = EXEC_ENDPOINT
    const val ADMIN_PAGE_URL: String = ADMIN_SCRIPT_ROOT + "exec?page=admin"

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

    fun gpsExecUrl(action: String, vararg params: Pair<String, String>): String {
        val query = buildList {
            add("action=${encode(action)}")
            params.forEach { (key, value) ->
                if (value.isNotBlank()) {
                    add("${encode(key)}=${encode(value)}")
                }
            }
        }.joinToString("&")

        return "$GPS_EXEC_ENDPOINT?$query"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
