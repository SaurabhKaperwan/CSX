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

data class VerifyUrl(
    val url: String
)

suspend fun bypass(mainUrl : String): String {
    val homePageDocument = app.get("${mainUrl}/mobile/home").document
    val addHash          = homePageDocument.select("body").attr("data-addhash")
    val time             = homePageDocument.select("body").attr("data-time")

    var verificationUrl  = "https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/NF.json"
    verificationUrl      = app.get(verificationUrl).parsed<VerifyUrl>().url.replace("###", addHash)
    // val hashDigits       = addHash.filter { it.isDigit() }
    // val first16Digits    = hashDigits.take(16)
    // app.get("${verificationUrl}&t=0.${first16Digits}")
    app.get(verificationUrl + "&t=${time}")

    var verifyCheck: String
    var verifyResponse: NiceResponse

    do {
        delay(1000)
        val requestBody = FormBody.Builder().add("verify", addHash).build()
        verifyResponse  = app.post("${mainUrl}/mobile/verify2.php", requestBody = requestBody)
        verifyCheck     = verifyResponse.text
    } while (!verifyCheck.contains("\"statusup\":\"All Done\""))

    return verifyResponse.cookies["t_hash_t"].orEmpty()
}
