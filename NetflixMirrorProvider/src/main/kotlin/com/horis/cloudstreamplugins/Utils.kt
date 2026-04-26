package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.nicehttp.Requests
import com.lagradost.nicehttp.ResponseParser
import kotlin.reflect.KClass
import okhttp3.FormBody
import com.lagradost.nicehttp.NiceResponse
import kotlinx.coroutines.delay
import android.content.Context
import com.lagradost.api.Log
import org.json.JSONObject
import com.lagradost.cloudstream3.APIHolder.unixTime
import kotlinx.coroutines.delay

val JSONParser = object : ResponseParser {
    val mapper: ObjectMapper = jacksonObjectMapper().configure(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
    ).configure(
        JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true
    )

    override fun <T : Any> parse(text: String, kClass: KClass<T>): T {
        return mapper.readValue(text, kClass.java)
    }

    override fun <T : Any> parseSafe(text: String, kClass: KClass<T>): T? {
        return try {
            mapper.readValue(text, kClass.java)
        } catch (e: Exception) {
            null
        }
    }

    override fun writeValueAsString(obj: Any): String {
        return mapper.writeValueAsString(obj)
    }
}

val app = Requests(responseParser = JSONParser).apply {
    defaultHeaders = mapOf("User-Agent" to USER_AGENT)
}

inline fun <reified T : Any> parseJson(text: String): T {
    return JSONParser.parse(text, T::class)
}

inline fun <reified T : Any> tryParseJson(text: String): T? {
    return try {
        return JSONParser.parseSafe(text, T::class)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun convertRuntimeToMinutes(runtime: String): Int {
    var totalMinutes = 0

    val parts = runtime.split(" ")

    for (part in parts) {
        when {
            part.endsWith("h") -> {
                val hours = part.removeSuffix("h").trim().toIntOrNull() ?: 0
                totalMinutes += hours * 60
            }
            part.endsWith("m") -> {
                val minutes = part.removeSuffix("m").trim().toIntOrNull() ?: 0
                totalMinutes += minutes
            }
        }
    }

    return totalMinutes
}

//Bypass (App method)
suspend fun bypass(mainUrl: String): String {

    // Check persistent storage first
    val (savedCookie, savedTimestamp) = NetflixMirrorStorage.getCookie()

    // Return cached cookie if valid (≤15 hours old)
    if (!savedCookie.isNullOrEmpty() && System.currentTimeMillis() - savedTimestamp < 54_000_000) {
        Log.d("NF", "savedCookie: $savedCookie")
        return savedCookie
    }

    val addhash = app.get(
        "$mainUrl/mobile/home?app=1",
        headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0",
            "X-Requested-With" to "app.netmirror.netmirrornew"
        )
    ).document.select("body").attr("data-addhash")

    Log.d("NF", "addhash: $addhash")

    app.get("https://userver.net52.cc/?jjoii=$addhash&a=y&t=${APIHolder.unixTime}")

    val newCookie = try {
        var verifyCheck: String
        var verifyResponse: NiceResponse
        var count = 0
        val requestBody = FormBody.Builder()
            .addEncoded("verify", "$addhash")
            .build()
        do {
            delay(10000)
            verifyResponse = app.post(
                "$mainUrl/mobile/verify2.php",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Linux; Android 12; RMX2117 Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.55 Mobile Safari/537.36 /OS.Gatu v3.0",
                    "X-Requested-With" to "XMLHttpRequest"
                ),
                requestBody = requestBody
            )
            verifyCheck = verifyResponse.text

            Log.d("NF", "verifyCheck: $verifyCheck")

            count++
            if (count > 7) {
                throw Exception("Failed to verify cookie")
            }
        } while (!verifyCheck.contains("\"statusup\":\"All Done\""))
        verifyResponse.cookies["t_hash_t"].orEmpty()
    } catch (e: Exception) {
        // Clear invalid cookie on failure
        NetflixMirrorStorage.clearCookie()
        throw e
    }

    // Persist the new cookie
    if (newCookie.isNotEmpty()) {
        NetflixMirrorStorage.saveCookie(newCookie)
    }

    Log.d("NF", "newCookie: $newCookie")
    return newCookie
}

suspend fun getVideoToken(mainUrl: String, newUrl: String, id: String, cookies: Map<String, String>): String {

    val requestBody = FormBody.Builder().addEncoded("id", id).build()

    val headers = mapOf(
        "Accept" to "*/*",
        "Accept-Language" to "en-US,en;q=0.5",
        "Connection" to "keep-alive",
        "Content-Length" to "11",
        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "Host" to "net22.cc",
        "Origin" to "$mainUrl/",
        "Priority" to "u=0",
        "Referer" to "$mainUrl/home",
        "Sec-Fetch-Dest" to "empty",
        "Sec-Fetch-Mode" to "cors",
        "Sec-Fetch-Site" to "same-origin",
        "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:139.0) Gecko/20100101 Firefox/139.0",
        "X-Requested-With" to "XMLHttpRequest"
    )

    val json = app.post("$mainUrl/play.php", cookies = cookies, requestBody = requestBody, headers = headers).text
    val h = JSONObject(json).getString("h")
    return h.substringAfter("in=")
}
