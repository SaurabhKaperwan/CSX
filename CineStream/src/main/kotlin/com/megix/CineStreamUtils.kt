package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import okhttp3.FormBody
import org.jsoup.nodes.Document
import java.net.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.api.Log
import com.lagradost.nicehttp.NiceResponse
import kotlinx.coroutines.delay

val SPEC_OPTIONS = mapOf(
    "quality" to listOf(
        mapOf("value" to "BluRay", "label" to "BluRay"),
        mapOf("value" to "BluRay REMUX", "label" to "BluRay REMUX"),
        mapOf("value" to "BRRip", "label" to "BRRip"),
        mapOf("value" to "BDRip", "label" to "BDRip"),
        mapOf("value" to "WEB-DL", "label" to "WEB-DL"),
        mapOf("value" to "HDRip", "label" to "HDRip"),
        mapOf("value" to "DVDRip", "label" to "DVDRip"),
        mapOf("value" to "HDTV", "label" to "HDTV"),
        mapOf("value" to "CAM", "label" to "CAM"),
        mapOf("value" to "TeleSync", "label" to "TeleSync"),
        mapOf("value" to "SCR", "label" to "SCR")
    ),
    "codec" to listOf(
        mapOf("value" to "x264", "label" to "x264"),
        mapOf("value" to "x265", "label" to "x265 (HEVC)"),
        mapOf("value" to "h.264", "label" to "H.264 (AVC)"),
        mapOf("value" to "h.265", "label" to "H.265 (HEVC)"),
        mapOf("value" to "hevc", "label" to "HEVC"),
        mapOf("value" to "avc", "label" to "AVC"),
        mapOf("value" to "mpeg-2", "label" to "MPEG-2"),
        mapOf("value" to "mpeg-4", "label" to "MPEG-4"),
        mapOf("value" to "vp9", "label" to "VP9")
    ),
    "audio" to listOf(
        mapOf("value" to "AAC", "label" to "AAC"),
        mapOf("value" to "AC3", "label" to "AC3 (Dolby Digital)"),
        mapOf("value" to "DTS", "label" to "DTS"),
        mapOf("value" to "DTS-HD MA", "label" to "DTS-HD MA"),
        mapOf("value" to "TrueHD", "label" to "Dolby TrueHD"),
        mapOf("value" to "Atmos", "label" to "Dolby Atmos"),
        mapOf("value" to "DD+", "label" to "DD+"),
        mapOf("value" to "Dolby Digital Plus", "label" to "Dolby Digital Plus"),
        mapOf("value" to "DTS Lossless", "label" to "DTS Lossless")
    ),
    "hdr" to listOf(
        mapOf("value" to "DV", "label" to "Dolby Vision"),
        mapOf("value" to "HDR10+", "label" to "HDR10+"),
        mapOf("value" to "HDR", "label" to "HDR"),
        mapOf("value" to "SDR", "label" to "SDR")
    ),
    "language" to listOf(
        mapOf("value" to "HIN", "label" to "Hindi"),
        mapOf("value" to "Hindi", "label" to "Hindi"),
        mapOf("value" to "Tamil", "label" to "Tamil"),
        mapOf("value" to "ENG", "label" to "English"),
        mapOf("value" to "English", "label" to "English"),
        mapOf("value" to "Korean", "label" to "Korean"),
        mapOf("value" to "KOR", "label" to "Korean"),
        mapOf("value" to "Japanese", "label" to "Japanese"),
        mapOf("value" to "Chinese", "label" to "Chinese"),
        mapOf("value" to "Telugu", "label" to "Telugu"),
    )
)

fun extractSpecs(inputString: String): Map<String, List<String>> {
    val results = mutableMapOf<String, List<String>>()

    SPEC_OPTIONS.forEach { (category, options) ->
        val matches = options.filter { option ->
            val value = option["value"] as String
            val regexPattern = "\\b${Regex.escape(value)}\\b".toRegex(RegexOption.IGNORE_CASE)
            regexPattern.containsMatchIn(inputString)
        }.map { it["label"] as String }

        results[category] = matches
    }

    return results.toMap()
}

// Helper function to escape regex special characters
fun Regex.escape(input: String): String {
    return input.replace(Regex("[.\\+*?^$()\\[\\]{}|\\\\]"), "\\\\$0")
}

fun buildExtractedTitle(extracted: Map<String, List<String>>): String {
    var extractedTitle = ""
    val orderedCategories = listOf("quality", "codec", "audio", "hdr", "language")

    for (category in orderedCategories) {
        val values = extracted[category] ?: emptyList()
        if (values.isNotEmpty()) {
            extractedTitle += values.joinToString(" ") + " "
        }
    }

    return extractedTitle.trim()
}

fun String.getHost(): String {
    return fixTitle(URI(this).host.substringBeforeLast(".").substringAfterLast("."))
}

var NfCookie = ""

suspend fun NFBypass(mainUrl : String): String {
    if(NfCookie != "") {
        return NfCookie
    }
    val homePageDocument = app.get("${mainUrl}/home").document
    val addHash          = homePageDocument.select("body").attr("data-addhash")
    val time             = homePageDocument.select("body").attr("data-time")

    var verificationUrl  = "https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/NF.json"
    verificationUrl      = app.get(verificationUrl).parsed<NFVerifyUrl>().url.replace("###", addHash)
    // val hashDigits       = addHash.filter { it.isDigit() }
    // val first16Digits    = hashDigits.take(16)
    // app.get("${verificationUrl}&t=0.${first16Digits}")
    app.get(verificationUrl + "&t=${time}")

    var verifyCheck: String
    var verifyResponse: NiceResponse
    var tries = 0

    do {
        delay(1000)
        tries++
        val requestBody = FormBody.Builder().add("verify", addHash).build()
        verifyResponse  = app.post("${mainUrl}/verify2.php", requestBody = requestBody)
        verifyCheck     = verifyResponse.text
    } while (!verifyCheck.contains("\"statusup\":\"All Done\"") || tries < 7)

    return verifyResponse.cookies["t_hash_t"].orEmpty()
}

suspend fun cinemaluxeBypass(url: String): String {
    val document = app.get(url, allowRedirects = true).document.toString()
    val encodeUrl = Regex("""link":"([^"]+)""").find(document) ?. groupValues ?. get(1) ?: ""
    return base64Decode(encodeUrl)
}

fun getFirstCharacterOrZero(input: String): String {
    val firstChar = input[0]
    return if (!firstChar.isLetter()) {
        "0"
    } else {
        firstChar.toString()
    }
}

fun getBaseUrl(url: String): String {
    return URI(url).let {
        "${it.scheme}://${it.host}"
    }
}

fun String?.createSlug(): String? {
    return this?.filter { it.isWhitespace() || it.isLetterOrDigit() }
        ?.trim()
        ?.replace("\\s+".toRegex(), "-")
        ?.lowercase()
}

suspend fun extractMdrive(url: String): List<String> {
    val doc = app.get(url).document
    return doc.select("a")
        .mapNotNull { it.attr("href").takeIf { href ->
            href.contains(Regex("hubcloud|gdflix", RegexOption.IGNORE_CASE))
        }}
}


fun getEpisodeSlug(
    season: Int? = null,
    episode: Int? = null,
): Pair<String, String> {
    return if (season == null && episode == null) {
        "" to ""
    } else {
        (if (season!! < 10) "0$season" else "$season") to (if (episode!! < 10) "0$episode" else "$episode")
    }
}

fun getIndexQuality(str: String?): Int {
    return Regex("""(\d{3,4})[pP]""").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
        ?: Qualities.Unknown.value
}

suspend fun loadSourceNameExtractor(
    source: String,
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
    quality: Int? = null,
) {
    loadExtractor(url, referer, subtitleCallback) { link ->
        val extracted = extractSpecs(link.name)
        val extractedSpecs = buildExtractedTitle(extracted)
        callback.invoke(
            ExtractorLink(
                "$source[${link.source}]",
                "$source[${link.source}] $extractedSpecs",
                link.url,
                link.referer,
                quality ?: link.quality ,
                link.type,
                link.headers,
                link.extractorData
            )
        )
    }
}

suspend fun loadCustomTagExtractor(
        tag: String? = null,
        url: String,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        quality: Int? = null,
) {
    loadExtractor(url, referer, subtitleCallback) { link ->
        callback.invoke(
            ExtractorLink(
                link.source,
                "${link.name} $tag",
                link.url,
                link.referer,
                quality ?: link.quality,
                link.type,
                link.headers,
                link.extractorData
            )
        )
    }
}

suspend fun loadCustomExtractor(
    name: String? = null,
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
    quality: Int? = null,
) {
    loadExtractor(url, referer, subtitleCallback) { link ->
        callback.invoke(
            ExtractorLink(
                name ?: link.source,
                name ?: link.name,
                link.url,
                link.referer,
                when {
                    link.name == "VidSrc" -> Qualities.P1080.value
                    else -> quality ?: link.quality
                },
                link.type,
                link.headers,
                link.extractorData
            )
        )
    }
}

fun fixUrl(url: String, domain: String): String {
    if (url.startsWith("http")) {
        return url
    }
    if (url.isEmpty()) {
        return ""
    }

    val startsWithNoHttp = url.startsWith("//")
    if (startsWithNoHttp) {
        return "https:$url"
    } else {
        if (url.startsWith('/')) {
            return domain + url
        }
        return "$domain/$url"
    }
}

suspend fun bypassHrefli(url: String): String? {
    fun Document.getFormUrl(): String {
        return this.select("form#landing").attr("action")
    }

    fun Document.getFormData(): Map<String, String> {
        return this.select("form#landing input").associate { it.attr("name") to it.attr("value") }
    }

    val host = getBaseUrl(url)
    var res = app.get(url).document
    var formUrl = res.getFormUrl()
    var formData = res.getFormData()

    res = app.post(formUrl, data = formData).document
    formUrl = res.getFormUrl()
    formData = res.getFormData()

    res = app.post(formUrl, data = formData).document
    val skToken = res.selectFirst("script:containsData(?go=)")?.data()?.substringAfter("?go=")
        ?.substringBefore("\"") ?: return null
    val driveUrl = app.get(
        "$host?go=$skToken", cookies = mapOf(
            skToken to "${formData["_wp_http2"]}"
        )
    ).document.selectFirst("meta[http-equiv=refresh]")?.attr("content")?.substringAfter("url=")
    val path = app.get(driveUrl ?: return null).text.substringAfter("replace(\"")
        .substringBefore("\")")
    if (path == "/404") return null
    return fixUrl(path, getBaseUrl(driveUrl))
}

