package com.megix

import com.google.gson.JsonParser
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import java.util.Base64
import okhttp3.FormBody
import org.jsoup.nodes.Document
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import java.net.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.api.Log
import com.lagradost.nicehttp.NiceResponse
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.USER_AGENT
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.lagradost.nicehttp.RequestBodyTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import org.jsoup.Jsoup
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.lagradost.cloudstream3.runAllAsync
import kotlin.math.pow
import kotlin.random.Random
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
        mapOf("value" to "SCR", "label" to "SCR"),
        mapOf("value" to "10bit", "label" to "10bit"),
        mapOf("value" to "8bit", "label" to "8bit"),
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

    val fileSizeRegex = """(\d+(?:\.\d+)?\s?(?:MB|GB))""".toRegex(RegexOption.IGNORE_CASE)
    val sizeMatch = fileSizeRegex.find(inputString)
    if (sizeMatch != null) {
        results["size"] = listOf(sizeMatch.groupValues[1])
    }

    return results.toMap()
}

// Helper function to escape regex special characters
fun Regex.escape(input: String): String {
    return input.replace(Regex("[.\\+*?^$()\\[\\]{}|\\\\]"), "\\\\$0")
}

fun buildExtractedTitle(extracted: Map<String, List<String>>): String {
    val orderedCategories = listOf("quality", "codec", "audio", "hdr", "language")

    val specs = orderedCategories
        .flatMap { extracted[it] ?: emptyList() }
        .distinct()
        .joinToString(" ")

    val size = extracted["size"]?.firstOrNull()

    return if (size != null) {
        "$specs [$size]"
    } else {
        specs
    }
}

fun String.getHost(): String {
    return fixTitle(URI(this).host.substringBeforeLast(".").substringAfterLast("."))
}

suspend fun NFBypass(mainUrl: String): String {
    // Check persistent storage first
    val (savedCookie, savedTimestamp) = CineStreamStorage.getCookie()

    // Return cached cookie if valid (≤15 hours old)
    if (!savedCookie.isNullOrEmpty() && System.currentTimeMillis() - savedTimestamp < 54_000_000) {
        return savedCookie
    }

    // Fetch new cookie if expired/missing
    val newCookie = try {
        var verifyCheck: String
        var verifyResponse: NiceResponse
        do {
            verifyResponse = app.post("$mainUrl/tv/p.php")
            verifyCheck = verifyResponse.text
        } while (!verifyCheck.contains("\"r\":\"n\""))
        verifyResponse.cookies["t_hash_t"].orEmpty()
    } catch (e: Exception) {
        // Clear invalid cookie on failure
        CineStreamStorage.clearCookie()
        throw e
    }


    // Persist the new cookie
    if (newCookie.isNotEmpty()) {
        CineStreamStorage.saveCookie(newCookie)
    }
    return newCookie
}

suspend fun makePostRequestCinemaluxe(jsonString: String, url: String, action: String): String {
    val gson = Gson()
    val item = gson.fromJson(jsonString, CinemaluxeItem::class.java)

    val requestBody = "token=${
        URLEncoder.encode(item.token, "UTF-8")
    }&id=${
        item.id
    }&time=${
        item.time
    }&post=${
        URLEncoder.encode(item.post, "UTF-8")
    }&redirect=${
        URLEncoder.encode(item.redirect, "UTF-8")
    }&cacha=${
        URLEncoder.encode(item.cacha, "UTF-8")
    }&new=${
        item.new
    }&link=${
        URLEncoder.encode(item.link, "UTF-8")
    }&action=$action".toRequestBody("application/x-www-form-urlencoded".toMediaType())

    val response = app.post(url, requestBody = requestBody, allowRedirects = false).headers["Location"] ?: ""
    return response
}

suspend fun cinemaluxeBypass(url: String): String {
    val text = app.get(url).text
    val encodeUrl = Regex("""link":"([^"]+)""").find(text)?.groupValues?.get(1) ?: ""
    if(encodeUrl.isNotEmpty()) {
        return base64Decode(encodeUrl.replace("\\/", "/"))
    }
    val postUrl =
        """\"soralink_ajaxurl":"([^"]+)\"""".toRegex().find(text)?.groupValues?.get(1)
    val jsonData =
        """var\s+item\s*=\s*(\{.*?\});""".toRegex(RegexOption.DOT_MATCHES_ALL)
        .find(text)?.groupValues?.get(1)
    val soraLink =
        """\"soralink_z"\s*:\s*"([^"]+)\"""".toRegex().find(text)?.groupValues?.get(1)

    if(postUrl != null && jsonData != null && soraLink != null) {
        return makePostRequestCinemaluxe(jsonData, postUrl, soraLink)
    }
    return url
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
            href.contains(Regex("hubcloud|gdflix|gdlink", RegexOption.IGNORE_CASE))
        }}
}

suspend fun loadNameExtractor(
    name: String? = null,
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
    quality: Int,
) {
    callback.invoke(
        newExtractorLink(
            name ?: "",
            name ?: "",
            url,
            type = if (url.contains("m3u8")) ExtractorLinkType.M3U8 else INFER_TYPE
        ) {
            this.referer = referer ?: ""
            this.quality = quality
        }
    )
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

suspend fun getHindMoviezLinks(
    source: String,
    url: String,
    callback: (ExtractorLink) -> Unit
) {
    val response = app.get(url)
    val doc = response.document
    val name = doc.select("div.container p:contains(Name:)").text().substringAfter("Name: ") ?: ""
    val fileSize = doc.select("div.container p:contains(Size:)").text().substringAfter("Size: ") ?: ""
    val extracted = extractSpecs(name)
    val extractedSpecs = buildExtractedTitle(extracted)

    runAllAsync(
        {
            val link = doc.select("a.btn-info").attr("href")
            val referer = response.url
            val document = app.get(link, referer = referer).document
            document.select("a.button").map {
                callback.invoke(
                    newExtractorLink(
                        source,
                        "$source $extractedSpecs[$fileSize]",
                        it.attr("href"),
                        ExtractorLinkType.VIDEO,
                    ) {
                        this.quality = getIndexQuality(name)
                    }
                )
            }
        },
        {
            val link = doc.select("a.btn-dark").attr("href")
            callback.invoke(
                newExtractorLink(
                    "$source[HCloud]",
                    "$source[HCloud] $extractedSpecs[$fileSize]",
                    link,
                    ExtractorLinkType.VIDEO,
                ) {
                    this.quality = getIndexQuality(name)
                }
            )
        },
    )
}

suspend fun loadSourceNameExtractor(
    source: String,
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
    quality: Int? = null,
) {
    val scope = CoroutineScope(Dispatchers.Default + Job())

    loadExtractor(url, referer, subtitleCallback) { link ->
        if (!link.source.contains("Download")) {
            scope.launch {
                val extracted = extractSpecs(link.name)
                val extractedSpecs = buildExtractedTitle(extracted)
                val combined = if(source.contains("(Combined)")) " (Combined)" else ""
                val newLink = newExtractorLink(
                    "${link.source}$combined",
                    "$source[${link.source}] $extractedSpecs",
                    link.url,
                    type = link.type
                ) {
                    this.referer = link.referer
                    this.quality = quality ?: link.quality
                    this.headers = link.headers
                    this.extractorData = link.extractorData
                }
                callback.invoke(newLink)
            }
        }
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
    val scope = CoroutineScope(Dispatchers.Default + Job())

    loadExtractor(url, referer, subtitleCallback) { link ->
        scope.launch {
            val newLink = newExtractorLink(
                link.source,
                "${link.name} $tag",
                link.url,
                link.type
            ) {
                this.quality = quality ?: link.quality
                this.referer = link.referer
                this.headers = link.headers
                this.extractorData = link.extractorData
            }
            callback.invoke(newLink)
        }
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
    // Define a scope for the coroutine
    val scope = CoroutineScope(Dispatchers.Default + Job())

    loadExtractor(url, referer, subtitleCallback) { link ->
        scope.launch {
            val newLink = newExtractorLink(
                name ?: link.source,
                name ?: link.name,
                link.url,
                type = link.type
            ) {
                this.quality = quality ?: link.quality
                this.referer = link.referer
                this.headers = link.headers
                this.extractorData = link.extractorData
            }
            callback.invoke(newLink)
        }
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


suspend fun convertTmdbToAnimeId(
    title: String?,
    year: Int?,
    airedDate: String?,
    type: TvType
): AniIds {
    val sAiredDate = airedDate?.split("-")
    val airedYear = sAiredDate?.firstOrNull()?.toIntOrNull()
    val airedSeason = getSeason(sAiredDate?.get(1)?.toIntOrNull())

    return if (type == TvType.AnimeMovie) {
        tmdbToAnimeId(title, year, "", type)
    } else {
        tmdbToAnimeId(title, airedYear, airedSeason, type)
    }
}

suspend fun tmdbToAnimeId(title: String?, year: Int?, season: String?, type: TvType): AniIds {
    val anilistAPI = "https://graphql.anilist.co"
    val query = """
        query (
          ${'$'}page: Int = 1
          ${'$'}search: String
          ${'$'}sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC]
          ${'$'}type: MediaType
          ${'$'}season: MediaSeason
          ${'$'}seasonYear: Int
          ${'$'}format: [MediaFormat]
        ) {
          Page(page: ${'$'}page, perPage: 20) {
            media(
              search: ${'$'}search
              sort: ${'$'}sort
              type: ${'$'}type
              season: ${'$'}season
              seasonYear: ${'$'}seasonYear
              format_in: ${'$'}format
            ) {
              id
              idMal
            }
          }
        }
    """.trimIndent().trim()

    val variables = mapOf(
        "search" to title,
        "sort" to "SEARCH_MATCH",
        "type" to "ANIME",
        "season" to season?.uppercase(),
        "seasonYear" to year,
        "format" to listOf(if (type == TvType.AnimeMovie) "MOVIE" else "TV", "ONA")
    ).filterValues { value -> value != null && value.toString().isNotEmpty() }
    val data = mapOf(
        "query" to query,
        "variables" to variables
    ).toJson().toRequestBody(RequestBodyTypes.JSON.toMediaTypeOrNull())
    val res = app.post(anilistAPI, requestBody = data)
        .parsedSafe<AniSearch>()?.data?.Page?.media?.firstOrNull()
    return AniIds(res?.id, res?.idMal)
}


fun getSeason(month: Int?): String? {
    val seasons = arrayOf(
        "Winter", "Winter", "Spring", "Spring", "Spring", "Summer",
        "Summer", "Summer", "Fall", "Fall", "Fall", "Winter"
    )
    if (month == null) return null
    return seasons[month - 1]
}

suspend fun gofileExtractor(
    source: String,
    url: String,
    referer: String?,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
) {
    val mainUrl = "https://gofile.io"
    val mainApi = "https://api.gofile.io"
    val headers = mapOf(
        "User-Agent" to USER_AGENT,
        "Origin" to mainUrl,
        "Referer" to mainUrl,
    )
    //val res = app.get(url)
    val id = Regex("/(?:\\?c=|d/)([\\da-zA-Z-]+)").find(url)?.groupValues?.get(1) ?: return
    val genAccountRes = app.post("$mainApi/accounts", headers = headers).text
    val jsonResp = JSONObject(genAccountRes)
    val token = jsonResp.getJSONObject("data").getString("token") ?: return

    val globalRes = app.get("$mainUrl/dist/js/global.js", headers = headers).text
    val wt = Regex("""appdata\.wt\s*=\s*[\"']([^\"']+)[\"']""").find(globalRes)?.groupValues?.get(1) ?: return

    val response = app.get("$mainApi/contents/$id?wt=$wt",
        headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Origin" to mainUrl,
            "Referer" to mainUrl,
            "Authorization" to "Bearer $token",
        )
    ).text

    val jsonResponse = JSONObject(response)
    val data = jsonResponse.getJSONObject("data")
    val children = data.getJSONObject("children")
    val oId = children.keys().next()
    val link = children.getJSONObject(oId).getString("link")
    val fileName = children.getJSONObject(oId).getString("name")
    val size = children.getJSONObject(oId).getLong("size")
    val formattedSize = if (size < 1024L * 1024 * 1024) {
        val sizeInMB = size.toDouble() / (1024 * 1024)
        "%.2f MB".format(sizeInMB)
    } else {
        val sizeInGB = size.toDouble() / (1024 * 1024 * 1024)
        "%.2f GB".format(sizeInGB)
    }

    if(link != null) {
        val extracted = extractSpecs(fileName)
        val extractedSpecs = buildExtractedTitle(extracted)
        callback.invoke(
            newExtractorLink(
                "Gofile",
                "$source[Gofile] $extractedSpecs[$formattedSize]",
                link,
            ) {
                this.quality = getIndexQuality(fileName)
                this.headers = mapOf(
                    "Cookie" to "accountToken=$token"
                )
            }
        )
    }
}

fun String?.createPlayerSlug(): String? {
    return this?.replace(Regex("[^A-Za-z0-9]+"), " ")?.replace(Regex("\\s+"), " ")?.trim() // Replace spaces with hyphens
}

fun getPlayer4UQuality (quality :String) : Int
{
    return when (quality) {
        "4K", "2160P" -> Qualities.P2160.value
        "FHD", "1080P" -> Qualities.P1080.value
        "HQ", "HD", "720P","DVDRIP","TVRIP","HDTC","PREDVD" -> Qualities.P720.value
        "480P" -> Qualities.P480.value
        "360P","CAM" -> Qualities.P360.value
        "DS" -> Qualities.P144.value
        "SD" -> Qualities.P480.value
        "WEBRIP" -> Qualities.P720.value
        "BLURAY", "BRRIP" -> Qualities.P1080.value
        "HDRIP" -> Qualities.P1080.value
        "TS" -> Qualities.P480.value
        "R5" -> Qualities.P480.value
        "SCR" -> Qualities.P480.value
        "TC" -> Qualities.P480.value
        else -> Qualities.Unknown.value
    }
}

suspend fun getPlayer4uUrl(
    name: String,
    selectedQuality: Int,
    url: String,
    referer: String?,
    callback: (ExtractorLink) -> Unit
) {
    val response = app.get(url, referer = referer)
    var script = getAndUnpack(response.text).takeIf { it.isNotEmpty() }
        ?: response.document.selectFirst("script:containsData(sources:)")?.data()

    if (script == null) {
        val iframeUrl = Regex("""<iframe src="(.*?)"""").find(response.text)?.groupValues?.getOrNull(1) ?: return
        val iframeResponse = app.get(iframeUrl, referer = null, headers = mapOf("Accept-Language" to "en-US,en;q=0.5"))
        script = getAndUnpack(iframeResponse.text).takeIf { it.isNotEmpty() } ?: return
    }
    val m3u8 = Regex("\"hls2\":\\s*\"(.*?m3u8.*?)\"").find(script)?.groupValues?.getOrNull(1).orEmpty()
    callback.invoke(
        newExtractorLink(
            "Player4U",
            name,
            m3u8,
            type = ExtractorLinkType.M3U8
        ) {
            this.quality = selectedQuality
        }
    )

}

fun decryptBase64BlowfishEbc(base64Encrypted: String, key: String): String {
    try {
        val encryptedBytes =  base64DecodeArray(base64Encrypted)
        val secretKeySpec = SecretKeySpec(key.toByteArray(), "Blowfish")
        val cipher = Cipher.getInstance("Blowfish/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes)
    } catch (e: Exception) {
        e.printStackTrace()
        return "Decryption failed: ${e.message}"
    }
}

// Decrypt Links using Blowfish
fun decryptLinks(data: String): List<String> {
    val key = data.substring(data.length - 10)
    val ct = data.substring(0, data.length - 10)
    val pt = decryptBase64BlowfishEbc(ct, key)
    return pt.chunked(5)
}

suspend fun generateMagnetLink(url: String, hash: String?): String {
    val response = app.get(url)
    val trackerList = response.text.trim().split("\n")

    // Build the magnet link
    return buildString {
        append("magnet:?xt=urn:btih:$hash")
        trackerList.forEach { tracker ->
            if (tracker.isNotBlank()) {
                append("&tr=").append(tracker.trim())
            }
        }
    }
}

suspend fun getProtonEmbed(
    text: String,
    protonmoviesAPI: String,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
) {
    val regex = """([^\"]*strm\.json)""".toRegex()
    val match = regex.find(text)

    if (match != null) {
        val url = match.groupValues[1]
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to protonmoviesAPI
        )
        val json = app.get(protonmoviesAPI + url, headers = headers).text
        JSONObject(json).getJSONObject("ppd")?.getJSONObject("mixdrop.ag")?.optString("link")?.let {
            val source = it.replace("/f/", "/e/").replace("mxdrop.to", "mixdrop.ps")
            loadSourceNameExtractor("Protonmovies", source, "", subtitleCallback, callback)
        }
    }
}

suspend fun getProtonStream(
    doc: Document,
    protonmoviesAPI: String,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
) {
    doc.select("tr.infotr").amap { tr ->
        val id = tr.select("button:contains(Info)").attr("id").split("-").getOrNull(1)
        if(id != null) {
            val uid = "uid_${System.currentTimeMillis()}_${
                (Random.nextDouble() * 36.0.pow(9))
                .toLong()
                .toString(36)
                .padStart(9, '0')
            }"

            val requestBody = FormBody.Builder()
                .add("downloadid", id)
                .add("token", "ok")
                .add("uid", uid)
                .build()

            val postHeaders = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to protonmoviesAPI,
                "Content-Type" to "multipart/form-data",
            )
            val idData = app.post(
                "$protonmoviesAPI/ppd.php",
                headers = postHeaders,
                requestBody = requestBody
            ).text

            val headers = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to protonmoviesAPI
            )

            val idRes = app.post(
                "$protonmoviesAPI/tmp/$idData",
                headers = headers
            ).text

            JSONObject(idRes).getJSONObject("ppd")?.getJSONObject("gofile.io")?.optString("link")?.let {
                val source = it.replace("\\/", "/")
                gofileExtractor("Protonmovies", source, "", subtitleCallback, callback)
            }
        }
    }
}

fun decodeHtml(encodedArray: Array<String>): String {
    val joined = encodedArray.joinToString("")

    val unescaped = joined
        .replace("\\\"", "\"")
        .replace("\\'", "'")

    val cleaned = unescaped
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\r", "\r")

    val decoded = cleaned
        .replace("&quot;", "\"")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")

    return decoded
}

fun decodeMeta(document: Document): Document? {
    val scriptContent = document.selectFirst("script:containsData(decodeURIComponent)")?.data().toString()
    val splitByEqual = scriptContent.split(" = ")
    if (splitByEqual.size > 1) {
        val partAfterEqual = splitByEqual[1]
        val trimmed = partAfterEqual.split("protomovies")[0].trim()
        val sliced = if (trimmed.isNotEmpty()) trimmed.dropLast(1) else ""
        val jsonArray = JSONArray(sliced)
        val htmlString = decodeHtml(Array(jsonArray.length()) { i -> jsonArray.getString(i) })
        val decodedDoc = Jsoup.parse(htmlString)
        return decodedDoc
    }
    return null
}

//Madplay
fun parseMadplayServerInfo(jsonString: String): List<MadplayServerInfo> {
    val gson = Gson()
    val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
    val jsonList: List<Map<String, Any>> = gson.fromJson(jsonString, listType)

    return jsonList.map { item ->
        MadplayServerInfo(
            tvurl = item["tvurl"] as String,
            movieurl = item["movieurl"] as String,
            server = item["server"] as String
        )
    }
}

fun evpKDF(password: ByteArray, salt: ByteArray, keySize: Int, ivSize: Int): Pair<ByteArray, ByteArray> {
    val totalSize = keySize + ivSize
    val derived = ByteArray(totalSize)
    var block: ByteArray? = null
    var offset = 0

    while (offset < totalSize) {
        val hasher = MessageDigest.getInstance("MD5")
        if (block != null) hasher.update(block)
        hasher.update(password)
        hasher.update(salt)
        block = hasher.digest()

        val len = Math.min(block.size, totalSize - offset)
        System.arraycopy(block, 0, derived, offset, len)
        offset += len
    }

    val key = derived.copyOfRange(0, keySize)
    val iv = derived.copyOfRange(keySize, totalSize)
    return Pair(key, iv)
}

fun decryptOpenSSLAES(base64Cipher: String, passphrase: String): String {
    val cipherData = android.util.Base64.decode(base64Cipher, android.util.Base64.DEFAULT)

    // OpenSSL prefix: "Salted__" + 8 bytes salt
    val prefix = cipherData.copyOfRange(0, 8).toString(Charsets.US_ASCII)
    if (prefix != "Salted__") throw IllegalArgumentException("Invalid OpenSSL format")

    val salt = cipherData.copyOfRange(8, 16)
    val ciphertext = cipherData.copyOfRange(16, cipherData.size)

    val (key, iv) = evpKDF(passphrase.toByteArray(Charsets.UTF_8), salt, 32, 16)

    val secretKey = SecretKeySpec(key, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

    val decrypted = cipher.doFinal(ciphertext)
    return String(decrypted, Charsets.UTF_8)
}

//Allanime
fun decrypthex(inputStr: String): String {
    val hexString = if (inputStr.startsWith("-")) {
        inputStr.substringAfterLast("-")
    } else {
        inputStr
    }

    val bytes = ByteArray(hexString.length / 2) { i ->
        val hexByte = hexString.substring(i * 2, i * 2 + 2)
        (hexByte.toInt(16) and 0xFF).toByte()
    }

    return bytes.joinToString("") { (it.toInt() xor 56).toChar().toString() }
}

suspend fun getM3u8Qualities(
    m3u8Link: String,
    referer: String,
    qualityName: String,
): List<ExtractorLink> {
    return M3u8Helper.generateM3u8(
        qualityName,
        m3u8Link,
        referer
    )
}

fun String.fixUrlPath(): String {
    return if (this.contains(".json?")) "https://allanime.day" + this
    else "https://allanime.day" + URI(this).path + ".json?" + URI(this).query
}

fun fixSourceUrls(url: String, source: String?): String? {
    return if (source == "Ak" || url.contains("/player/vitemb")) {
        tryParseJson<AkIframe>(base64Decode(url.substringAfter("=")))?.idUrl
    } else {
        url.replace(" ", "%20")
    }
}

fun extractKatStreamingLinks(html: String, episode: Int? = null): Map<String, String> {
    val result = mutableMapOf<String, String>()

    fun getMatch(pattern: String): String? {
        val matches = pattern.toRegex().findAll(html).toList()
        return when {
            episode != null && matches.size >= episode -> matches[episode - 1].groupValues[1]
            matches.isNotEmpty() -> matches[0].groupValues[1]
            else -> null
        }
    }

    val streamtapeId = getMatch("""streamtape_res:"([^"]+)\"""")
    val streamwishId = getMatch("""streamwish_res:"([^"]+)\"""")
    val vgembedId = getMatch("""vgembedstream_res:"([^"]+)\"""")

    val streamtapeBase = """streamtape_res:\{link:"([^"]+)\"""".toRegex().find(html)?.groupValues?.get(1)
    val streamwishBase = """streamwish_res:\{link:"([^"]+)\"""".toRegex().find(html)?.groupValues?.get(1)
    val vgembedBase = """vgembedstream_res:\{link:"([^"]+)\"""".toRegex().find(html)?.groupValues?.get(1)

    if (streamtapeId != null && streamtapeBase != null) {
        result["streamtape"] = streamtapeBase + streamtapeId
    }
    if (streamwishId != null && streamwishBase != null) {
        result["streamwish"] = streamwishBase + streamwishId
    }
    if (vgembedId != null && vgembedBase != null) {
        result["vgembedstream"] = vgembedBase + vgembedId
    }

    return result
}

fun getGojoServers(jsonString: String): List<Pair<String, Boolean>> {
    val result = mutableListOf<Pair<String, Boolean>>()

    try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val id = obj.optString("id", null) ?: continue
            val hasDub = obj.optBoolean("hasDub", false)
            result.add(Pair(id, hasDub))
        }
    } catch (e: Exception) {
        println("Error parsing Gojo servers: ${e.message}")
    }

    return result
}


suspend fun getGojoStreams(
    json: String,
    lang: String,
    provider: String,
    gojoBaseAPI: String,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
) {
    try {
        val jsonObject = JSONObject(json)
        val sourcesArray = jsonObject.optJSONArray("sources") ?: return

        for (i in 0 until sourcesArray.length()) {
            val source = sourcesArray.optJSONObject(i) ?: continue
            val rawUrl = source.optString("url", null) ?: continue

            val fullUrl = rawUrl.substringAfterLast("https:", "")
            if (fullUrl.isBlank()) continue

            val url = "https:$fullUrl"
            val videoType = source.optString("type", "m3u8")
            val quality = source.optString("quality").replace("p", "").toIntOrNull()

            callback.invoke(
                newExtractorLink(
                    "Gojo [${lang.uppercase()}] [${provider.uppercase()}]",
                    "Gojo [${lang.uppercase()}] [${provider.uppercase()}]",
                    url,
                    type = if (videoType == "mp4") ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                ) {
                    this.quality = quality ?: Qualities.P1080.value
                    this.referer = gojoBaseAPI
                }
            )
        }

        val subtitles = jsonObject.optJSONArray("subtitles") ?: return

        for (i in 0 until subtitles.length()) {
            val item = subtitles.optJSONObject(i) ?: continue
            val url = item.optString("url", null) ?: continue
            val lang = item.optString("lang", null) ?: continue
            subtitleCallback.invoke(
                SubtitleFile(
                    lang,
                    url
                )
            )
        }
    } catch (e: Exception) {
        println("Error parsing Gojo streams for $provider [$lang]: ${e.message}")
    }
}


suspend fun getSoaperLinks(
        soaperAPI: String,
        url: String,
        type: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
) {
    val headers = mapOf(
        "Referer" to soaperAPI,
        "Origin" to soaperAPI
    )

    val document = app.get(url, headers = headers).document
    val eId = document.select("#hId").attr("value")
    val hIsW = document.select("#hIsW").attr("value")
    val data = mapOf(
        "pass" to eId,
        "param" to "",
        "extra" to "1",
        "e2" to hIsW,
        "server" to "0",
    )

    val res = app.post("$soaperAPI/home/index/Get${type}InfoAjax", data = data, headers = headers).text
    val json = JSONObject(res)
    val videoPath = json.getString("val").replace("\\/", "/")
    val videoUrl = soaperAPI + videoPath

    callback.invoke(
        newExtractorLink(
            "Soaper",
            "Soaper",
            videoUrl,
            ExtractorLinkType.M3U8
        ) {
            this.referer = url
            this.quality = Qualities.P1080.value
        }
    )

    val subs = json.getJSONArray("subs")

    for (i in 0 until subs.length()) {
        val sub = subs.getJSONObject(i)
        val name = sub.getString("name")
        val path = sub.getString("path").replace("\\/", "/")
        val subUrl = soaperAPI + path
        subtitleCallback.invoke(
            SubtitleFile(
                name,
                subUrl
            )
        )
    }
}

suspend fun getRedirectLinks(url: String): String {
    fun encode(value: String): String {
        return Base64.getEncoder().encodeToString(value.toByteArray())
    }

    fun pen(value: String): String {
        return value.map {
            when (it) {
                in 'A'..'Z' -> ((it - 'A' + 13) % 26 + 'A'.code).toChar()
                in 'a'..'z' -> ((it - 'a' + 13) % 26 + 'a'.code).toChar()
                else -> it
            }
        }.joinToString("")
    }

    val doc = app.get(url).toString()
    val regex = "s\\('o','([A-Za-z0-9+/=]+)'|ck\\('_wp_http_\\d+','([^']+)'".toRegex()
    val combinedString = buildString {
        regex.findAll(doc).forEach { matchResult ->
            val extractedValue = matchResult.groups[1]?.value ?: matchResult.groups[2]?.value
            if (!extractedValue.isNullOrEmpty()) append(extractedValue)
        }
    }
    return try {
        val decodedString = base64Decode(pen(base64Decode(base64Decode(combinedString))))
        val jsonObject = JSONObject(decodedString)
        val encodedurl = base64Decode(jsonObject.optString("o", "")).trim()
        val data = encode(jsonObject.optString("data", "")).trim()
        val wphttp1 = jsonObject.optString("blog_url", "").trim()
        val directlink = runCatching {
            app.get("$wphttp1?re=$data".trim()).document.select("body").text().trim()
        }.getOrDefault("").trim()

        encodedurl.ifEmpty { directlink }
    } catch (e: Exception) {
        Log.e("Error:", "Error processing links $e")
        ""
    }
}

