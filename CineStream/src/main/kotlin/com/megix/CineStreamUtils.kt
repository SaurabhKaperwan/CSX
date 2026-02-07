package com.megix

import android.os.Build
import androidx.annotation.RequiresApi
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import java.util.Base64
import okhttp3.*
import okhttp3.FormBody
import org.jsoup.nodes.Document
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import java.net.*
import com.lagradost.api.Log
import com.lagradost.nicehttp.NiceResponse
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
import javax.crypto.Mac
import com.lagradost.cloudstream3.runAllAsync
import kotlin.math.pow
import kotlin.random.Random
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.nio.charset.StandardCharsets
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlin.math.max
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.lagradost.cloudstream3.APIHolder.unixTimeMS
import java.util.regex.Pattern

val M3U8_HEADERS = mapOf(
    "User-Agent" to "Mozilla/5.0 (Android) ExoPlayer",
    "Accept" to "*/*",
    "Accept-Encoding" to "identity",
    "Connection" to "keep-alive",
)

data class SpecOption(val value: String, val label: String)

// 2. Define Options
val SPEC_OPTIONS = mapOf(
    "quality" to listOf(
        // -- Optical / Disk --
        SpecOption("UHD BluRay", "4K UHD BluRay 💿"),
        SpecOption("BluRay", "BluRay 💿"),
        SpecOption("BluRay REMUX", "BluRay REMUX 💾"),
        SpecOption("BDRip", "BDRip 💿"),
        SpecOption("BRRip", "BRRip 💿"),
        SpecOption("DVD", "DVD Full/ISO 📀"),
        SpecOption("DVDRip", "DVDRip 📀"),

        // -- Web --
        SpecOption("WEB-DL", "WEB-DL ☁️"),
        SpecOption("WEBRip", "WEBRip 🌐"),
        SpecOption("WEB", "WEB 🕸️"),
        SpecOption("HDRip", "HDRip ✨"),

        // -- TV / Broadcast --
        SpecOption("HDTV", "HDTV 📺"),
        SpecOption("PDTV", "PDTV 📺"),
        SpecOption("PPV", "PPV 🎫"),

        // -- Low Quality / Pre-release --
        SpecOption("CAM", "CAM 📹"),
        SpecOption("TeleSync", "TeleSync 📹"),
        SpecOption("TS", "TS 🚫"),
        SpecOption("TC", "TeleCine 🎞️"),
        SpecOption("SCR", "SCR 📼"),
        SpecOption("R5", "R5 ⁵")
    ),
    "codec" to listOf(
        // -- Modern --
        SpecOption("av1", "AV1 🚀"),
        SpecOption("x265", "x265 ⚡"),
        SpecOption("h.265", "H.265 (HEVC) ⚡"),
        SpecOption("hevc", "HEVC ⚡"),
        SpecOption("vp9", "VP9 🧪"),

        // -- Standard --
        SpecOption("x264", "x264 📦"),
        SpecOption("h.264", "H.264 (AVC) 📦"),
        SpecOption("avc", "AVC 📦"),

        // -- Legacy --
        SpecOption("vc-1", "VC-1 📼"),
        SpecOption("mpeg-2", "MPEG-2 🎞️"),
        SpecOption("mpeg-4", "MPEG-4 🎞️"),
        SpecOption("xvid", "XviD 🧩"),
        SpecOption("divx", "DivX 🧩")
    ),
    "bitdepth" to listOf(
         SpecOption("10bit", "10bit 🎨"),
         SpecOption("8bit", "8bit 🖍️"),
         SpecOption("12bit", "12bit 🌈"),
         SpecOption("3D", "3D 👓"),
         SpecOption("IMAX", "IMAX 🏟️")
    ),
    "audio" to listOf(
        // -- Surround / Lossless --
        SpecOption("TrueHD", "Dolby TrueHD 🔊"),
        SpecOption("Atmos", "Dolby Atmos 🌌"),
        SpecOption("DTS-HD MA", "DTS-HD MA 🔊"),
        SpecOption("DTS:X", "DTS:X 🔊"),
        SpecOption("DTS Lossless", "DTS Lossless 🎼"),
        SpecOption("FLAC", "FLAC 🎹"),
        SpecOption("PCM", "LPCM/PCM 💿"),

        // -- Standard --
        SpecOption("E-AC3", "E-AC3 (DD+) 🔉"),
        SpecOption("DD+", "DD+ 🔉"),
        SpecOption("Dolby Digital Plus", "Dolby Digital Plus 🔉"),
        SpecOption("AC3", "AC3 (Dolby Digital) 🔈"),
        SpecOption("DTS", "DTS 🔈"),
        SpecOption("AAC", "AAC 🎧"),
        SpecOption("OPUS", "Opus 🎙️"),
        SpecOption("MP3", "MP3 🎵"),
        SpecOption("WMA", "WMA 🎵")
    ),
    "hdr" to listOf(
        SpecOption("DV", "Dolby Vision 👁️"),
        SpecOption("DoVi", "Dolby Vision 👁️"),
        SpecOption("HDR10+", "HDR10+ 🔆"),
        SpecOption("HDR10", "HDR10 🔆"),
        SpecOption("HLG", "HLG 📡"),
        SpecOption("HDR", "HDR 🔆"),
        SpecOption("SDR", "SDR 🔅")
    ),
    "language" to listOf(
        // -- Indian --
        SpecOption("HIN", "Hindi 🇮🇳"),
        SpecOption("Hindi", "Hindi 🇮🇳"),
        SpecOption("TAM", "Tamil 🇮🇳"),
        SpecOption("Tamil", "Tamil 🇮🇳"),
        SpecOption("TEL", "Telugu 🇮🇳"),
        SpecOption("Telugu", "Telugu 🇮🇳"),
        SpecOption("MAL", "Malayalam 🇮🇳"),
        SpecOption("Malayalam", "Malayalam 🇮🇳"),
        SpecOption("KAN", "Kannada 🇮🇳"),
        SpecOption("Kannada", "Kannada 🇮🇳"),
        SpecOption("BEN", "Bengali 🇮🇳"),
        SpecOption("PUN", "Punjabi 🇮🇳"),

        // -- Global --
        SpecOption("ENG", "English 🇺🇸"),
        SpecOption("English", "English 🇺🇸"),
        SpecOption("KOR", "Korean 🇰🇷"),
        SpecOption("Korean", "Korean 🇰🇷"),
        SpecOption("JPN", "Japanese 🇯🇵"),
        SpecOption("Japanese", "Japanese 🇯🇵"),
        SpecOption("CHN", "Chinese 🇨🇳"),
        SpecOption("Chinese", "Chinese 🇨🇳"),
        SpecOption("SPA", "Spanish 🇪🇸"),
        SpecOption("Spanish", "Spanish 🇪🇸"),
        SpecOption("FRE", "French 🇫🇷"),
        SpecOption("French", "French 🇫🇷"),
        SpecOption("GER", "German 🇩🇪"),
        SpecOption("German", "German 🇩🇪"),
        SpecOption("RUS", "Russian 🇷🇺"),
        SpecOption("ITA", "Italian 🇮🇹"),
        SpecOption("POR", "Portuguese 🇵🇹"),
        SpecOption("ARA", "Arabic 🇸🇦"),
        SpecOption("THA", "Thai 🇹🇭"),
        SpecOption("Multi", "Multi-Audio 🌍")
    )
)

// 3. Extraction Logic
fun extractSpecs(inputString: String): Map<String, List<String>> {
    val results = mutableMapOf<String, List<String>>()

    SPEC_OPTIONS.forEach { (category, options) ->
        val matches = options.filter { option ->
            // Escape special chars (like dots in "h.264") and use word boundaries (\b)
            val escapedValue = Pattern.quote(option.value)
            val regexPattern = "\\b$escapedValue\\b".toRegex(RegexOption.IGNORE_CASE)
            regexPattern.containsMatchIn(inputString)
        }.map { it.label }

        if (matches.isNotEmpty()) {
            results[category] = matches
        }
    }

    // Regex for file size (e.g. 1.4GB, 500MB)
    val fileSizeRegex = """(\d+(?:\.\d+)?\s?(?:MB|GB))""".toRegex(RegexOption.IGNORE_CASE)
    val sizeMatch = fileSizeRegex.find(inputString)
    if (sizeMatch != null) {
        results["size"] = listOf(sizeMatch.groupValues[1])
    }

    return results.toMap()
}

// 4. Formatting Logic (Using Pipe Separator)
fun buildExtractedTitle(extracted: Map<String, List<String>>): String {
    // Define preferred order of categories
    val orderedCategories = listOf("quality", "codec", "bitdepth", "audio", "hdr", "language")

    // Flatten lists, remove duplicates, join with " | "
    val specs = orderedCategories
        .flatMap { extracted[it] ?: emptyList() }
        .distinct()
        .joinToString(" | ")

    val size = extracted["size"]?.firstOrNull()

    return when {
        // If both specs and size exist, separate them with " | "
        size != null && specs.isNotEmpty() -> "\n$specs | $size 💾"

        // Only size
        size != null -> "$size 💾"

        // Only specs
        specs.isNotEmpty() -> "\n$specs"

        // Nothing found
        else -> ""
    }
}

val languageMap = mapOf(
    "Afrikaans" to listOf("af", "afr"),
    "Albanian" to listOf("sq", "sqi"),
    "Amharic" to listOf("am", "amh"),
    "Arabic" to listOf("ar", "ara"),
    "Armenian" to listOf("hy", "hye"),
    "Azerbaijani" to listOf("az", "aze"),
    "Basque" to listOf("eu", "eus"),
    "Belarusian" to listOf("be", "bel"),
    "Bengali" to listOf("bn", "ben"),
    "Bosnian" to listOf("bs", "bos"),
    "Bulgarian" to listOf("bg", "bul"),
    "Catalan" to listOf("ca", "cat"),
    "Chinese" to listOf("zh", "zho"),
    "Croatian" to listOf("hr", "hrv"),
    "Czech" to listOf("cs", "ces"),
    "Danish" to listOf("da", "dan"),
    "Dutch" to listOf("nl", "nld"),
    "English" to listOf("en", "eng"),
    "Estonian" to listOf("et", "est"),
    "Filipino" to listOf("tl", "tgl", "fil"),
    "Finnish" to listOf("fi", "fin"),
    "French" to listOf("fr", "fra"),
    "Galician" to listOf("gl", "glg"),
    "Georgian" to listOf("ka", "kat"),
    "German" to listOf("de", "deu", "ger"),
    "Greek" to listOf("el", "ell"),
    "Gujarati" to listOf("gu", "guj"),
    "Hebrew" to listOf("he", "heb"),
    "Hindi" to listOf("hi", "hin"),
    "Hungarian" to listOf("hu", "hun"),
    "Icelandic" to listOf("is", "isl"),
    "Indonesian" to listOf("id", "ind"),
    "Italian" to listOf("it", "ita"),
    "Japanese" to listOf("ja", "jpn"),
    "Kannada" to listOf("kn", "kan"),
    "Kazakh" to listOf("kk", "kaz"),
    "Korean" to listOf("ko", "kor"),
    "Latvian" to listOf("lv", "lav"),
    "Lithuanian" to listOf("lt", "lit"),
    "Macedonian" to listOf("mk", "mkd"),
    "Malay" to listOf("ms", "msa"),
    "Malayalam" to listOf("ml", "mal"),
    "Maltese" to listOf("mt", "mlt"),
    "Marathi" to listOf("mr", "mar"),
    "Mongolian" to listOf("mn", "mon"),
    "Nepali" to listOf("ne", "nep"),
    "Norwegian" to listOf("no", "nor"),
    "Persian" to listOf("fa", "fas"),
    "Polish" to listOf("pl", "pol"),
    "Portuguese" to listOf("pt", "por"),
    "Punjabi" to listOf("pa", "pan"),
    "Romanian" to listOf("ro", "ron"),
    "Russian" to listOf("ru", "rus"),
    "Serbian" to listOf("sr", "srp"),
    "Sinhala" to listOf("si", "sin"),
    "Slovak" to listOf("sk", "slk"),
    "Slovenian" to listOf("sl", "slv"),
    "Spanish" to listOf("es", "spa"),
    "Swahili" to listOf("sw", "swa"),
    "Swedish" to listOf("sv", "swe"),
    "Tamil" to listOf("ta", "tam"),
    "Telugu" to listOf("te", "tel"),
    "Thai" to listOf("th", "tha"),
    "Turkish" to listOf("tr", "tur"),
    "Ukrainian" to listOf("uk", "ukr"),
    "Urdu" to listOf("ur", "urd"),
    "Uzbek" to listOf("uz", "uzb"),
    "Vietnamese" to listOf("vi", "vie"),
    "Welsh" to listOf("cy", "cym"),
    "Yiddish" to listOf("yi", "yid")
)

fun getLanguage(language: String?): String? {

    language ?: return null

    var normalizedLang = if(language.contains("-")) {
        language.substringBefore("-")
    } else if(language.contains(" ")) {
        language.substringBefore(" ")
    } else if(language.contains("CR_")) {
        language.substringAfter("CR_")
    } else {
        language
    }

    if(normalizedLang.isBlank()) {
        normalizedLang =  language
    }

    val tag = languageMap.entries.find { entry ->
        entry.value.contains(normalizedLang)
    }?.key

    if(tag == null) {
        return normalizedLang
    }
    return tag
}

fun String.getHost(): String {
    return fixTitle(URI(this).host.substringBeforeLast(".").substringAfterLast("."))
}

//get Cast Data
suspend fun parseCastData(tvType: String, imdbId: String? = null): List<ActorData>? {
    return if (tvType != "anime") {
        try {
            val url = "https://aiometadata.elfhosted.com/stremio/9197a4a9-2f5b-4911-845e-8704c520bdf7/meta/$tvType/$imdbId.json"
            val json = app.get(url, timeout = 6L).text
            val gson = Gson()
            val data = gson.fromJson(json, TmdbResponse::class.java)
            data.meta?.appExtras?.cast?.mapNotNull { castMember ->
                if (castMember.name != null) {
                    ActorData(
                        Actor(
                            name = castMember.name,
                            image = castMember.photo
                        ),
                        roleString = castMember.character
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    } else {
        null
    }
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

suspend fun getNfVideoToken(mainUrl: String, newUrl: String, id: String, cookies: Map<String, String>): String {
    val requestBody = FormBody.Builder().add("id", id).build()
    val headers = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to "$mainUrl/",
    )
    val json = app.post("$mainUrl/play.php", cookies = cookies, requestBody = requestBody, headers = headers).text
    val h = JSONObject(json).getString("h")

    val headers2 = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language" to "en-GB,en;q=0.9",
        "Connection" to "keep-alive",
        "Host" to "net52.cc",
        "Referer" to "$mainUrl/",
        "sec-ch-ua" to "\"Chromium\";v=\"142\", \"Brave\";v=\"142\", \"Not_A Brand\";v=\"99\"",
        "sec-ch-ua-mobile" to "?0",
        "sec-ch-ua-platform" to "\"Linux\"",
        "Sec-Fetch-Dest" to "iframe",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "cross-site",
        "Sec-Fetch-Storage-Access" to "none",
        "Sec-Fetch-User" to "?1",
        "Sec-GPC" to "1",
        "Upgrade-Insecure-Requests" to "1",
        "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"
    )
    val document = app.get("$newUrl/play.php?id=$id&$h", headers = headers2).document
    val token = document.select("body").attr("data-h")
    return token
}

fun buildMagnetString(stream: TorrentioStream): String {
    val trackersString = stream.sources
        ?.asSequence()
        ?.filter { it.startsWith("tracker:", ignoreCase = true) }
        ?.map { it.substringAfter("tracker:").trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        ?.joinToString("") { "&tr=$it" }
        ?: ""
    return "magnet:?xt=urn:btih:${stream.infoHash}&dn=${stream.infoHash}$trackersString&index=${stream.fileIdx}"
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

fun getDate(): TmdbDate {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()

    // Today
    val today = formatter.format(calendar.time)

    // Next week
    calendar.add(Calendar.WEEK_OF_YEAR, 1)
    val nextWeek = formatter.format(calendar.time)

    // Last week's Monday
    calendar.time = Date()
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.add(Calendar.WEEK_OF_YEAR, -1)
    val lastWeekStart = formatter.format(calendar.time)

    // Start of current month
    calendar.time = Date()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val monthStart = formatter.format(calendar.time)

    return TmdbDate(today, nextWeek, lastWeekStart, monthStart)
}

fun isUpcoming(dateString: String?): Boolean {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateTime = dateString?.let { format.parse(it)?.time } ?: return false
        unixTimeMS < dateTime
    } catch (t: Throwable) {
        //logError(t)
        false
    }
}

fun getKisskhTitle(str: String?): String? {
    return str?.replace(Regex("[^a-zA-Z\\d]"), "-")
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

//Dahmer
fun getIndexQuality(str: String?): Int {
    return Regex("""(\d{3,4})[pP]""").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
        ?: Qualities.Unknown.value
}

fun getIndexQualityTags(str: String?, fullTag: Boolean = false): String {
    return if (fullTag) Regex("(?i)(.*)\\.(?:mkv|mp4|avi)").find(str ?: "")?.groupValues?.get(1)
        ?.trim() ?: str ?: "" else Regex("(?i)\\d{3,4}[pP]\\.?(.*?)\\.(mkv|mp4|avi)").find(
        str ?: ""
    )?.groupValues?.getOrNull(1)
        ?.replace(".", " ")?.trim() ?: str ?: ""
}

suspend fun resolveFinalUrl(startUrl: String): String {
    var currentUrl = startUrl
    var loopCount = 0
    val maxRedirects = 5

    while (loopCount < maxRedirects) {
        val location = app.head(currentUrl, allowRedirects = false).headers.get("Location")

        if(location.isNullOrEmpty()) {
            break
        }
        currentUrl = location
        loopCount++
    }

    return currentUrl
}

fun String.encodeUrl(): String {
    val url = URL(this)
    val uri = URI(url.protocol, url.userInfo, url.host, url.port, url.path, url.query, url.ref)
    return uri.toURL().toString()
}

suspend fun getHindMoviezLinks(
    source: String,
    url: String,
    callback: (ExtractorLink) -> Unit
) {
    val response = app.get(url)
    val doc = response.document
    val name = doc.select("div.container p:contains(Name:)").text().substringAfter("Name: ")
    val fileSize = doc.select("div.container p:contains(Size:)").text().substringAfter("Size: ")
    val simplifiedTitle = getSimplifiedTitle(name + fileSize)

    runAllAsync(
        {
            val link = doc.select("a.btn-info").attr("href")
            val referer = response.url
            val document = app.get(link, referer = referer).document
            document.select("a.button").map {
                callback.invoke(
                    newExtractorLink(
                        source,
                        "$source $simplifiedTitle",
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
                    "$source[HCloud] $simplifiedTitle",
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
    size: String = "",
) {
    val scope = CoroutineScope(Dispatchers.Default + Job())

    loadExtractor(url, referer, subtitleCallback) { link ->
        scope.launch {
            val isDownload = if(link.source.contains("Download")
                || link.url.contains("video-downloads.googleusercontent")
            ){ true } else { false }

            if(isDownload) return@launch

            val simplifiedTitle = getSimplifiedTitle(link.name)
            val combined = if(source.contains("(Combined)")) " (Combined)" else ""
            val fixSize = if(size.isNotEmpty()) " $size" else ""
            val newLink = newExtractorLink(
                if(isDownload) "Download${combined}" else "${link.source}$combined",
                "$source [${link.source}] $simplifiedTitle $fixSize",
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

fun getSimplifiedTitle(title: String) : String {
    val extracted = extractSpecs(title)
    return buildExtractedTitle(extracted)
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

//Anizip
fun getEpAnizipId(json: String, ep: Int): Int? {
    val gson = Gson()
    val root = gson.fromJson(json, Anizip::class.java)
    val episode = root.episodes?.get(ep.toString())
    val anidbEid = episode?.anidbEid
    return anidbEid
}

// --- Converts bytes → readable GB/MB ---
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "-"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        else -> String.format("%.2f KB", bytes / kb)
    }
}

//Xprime
suspend fun multiDecrypt(text : String, source: String) : String? {
    val headers = mapOf(
        "Content-Type" to "application/json",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Accept" to "application/json",
        "Accept-Language" to "en-US,en;q=0.9'",
    )

    val jsonBody = """{"text":"$text"}"""
    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

    val response = app.post(
        "https://enc-dec.app/api/$source",
        headers = headers,
        requestBody = requestBody
    )

    if(response.isSuccessful) {
        val json = response.text
        return JSONObject(json).getString("result")
    }
    return null
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

fun getAniListInfo(animeId: Int): AnimeInfo? {
    val client = OkHttpClient()

    val query = """
        query (${"$"}id: Int) {
            Media (id: ${"$"}id, type: ANIME) {
                title {
                    english
                    romaji
                }
                bannerImage
            }
        }
    """.trimIndent()

    val jsonBody = JSONObject()
    jsonBody.put("query", query)
    jsonBody.put("variables", JSONObject().put("id", animeId))

    val request = Request.Builder()
        .url("https://graphql.anilist.co")
        .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null

        val responseBody = response.body.string()
        val json = JSONObject(responseBody)
        val media = json.optJSONObject("data")?.optJSONObject("Media") ?: return null
        val rawBanner = media.optString("bannerImage")
        val finalBanner = rawBanner.takeUnless { it.isBlank() || it == "null" }
        val titleObj = media.optJSONObject("title")
        val english = titleObj?.optString("english")
        //val romaji = titleObj?.optString("romaji")
        val finalTitle = english?.takeUnless { it.isBlank() || it == "null" }
        return AnimeInfo(finalTitle, finalBanner)
    }
}

suspend fun convertTmdbToAnimeId(
    title: String?,
    date: String?,
    airedDate: String?,
    type: TvType
): AniIds {
    val sDate = date?.split("-")
    val sAiredDate = airedDate?.split("-")

    val year = sDate?.firstOrNull()?.toIntOrNull()
    val airedYear = sAiredDate?.firstOrNull()?.toIntOrNull()
    val season = getSeason(sDate?.get(1)?.toIntOrNull())
    val airedSeason = getSeason(sAiredDate?.get(1)?.toIntOrNull())

    return if (type == TvType.AnimeMovie) {
        tmdbToAnimeId(title, airedYear, "", type)
    } else {
        tmdbToAnimeId(title, year, season, type)
    }
}

suspend fun convertImdbToAnimeId(
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

suspend fun fetchTmdbLogoUrl(
    tmdbAPI: String,
    apiKey: String,
    type: TvType,
    tmdbId: Int?,
    appLangCode: String?
): String? {

    if (tmdbId == null) return null

    val appLang = appLangCode
        ?.substringBefore("-")
        ?.lowercase()

    val url = if (type == TvType.Movie) {
        "$tmdbAPI/movie/$tmdbId/images?api_key=$apiKey"
    } else {
        "$tmdbAPI/tv/$tmdbId/images?api_key=$apiKey"
    }

    val json = runCatching { JSONObject(app.get(url).text) }.getOrNull()
        ?: return null

    val logos = json.optJSONArray("logos") ?: return null
    if (logos.length() == 0) return null

    fun logoUrlAt(i: Int): String = "https://image.tmdb.org/t/p/w500${logos.getJSONObject(i).optString("file_path")}"

    if (!appLang.isNullOrBlank()) {
        for (i in 0 until logos.length()) {
            val logo = logos.optJSONObject(i) ?: continue
            if (logo.optString("iso_639_1") == appLang) {
                return logoUrlAt(i)
            }
        }
    }

    for (i in 0 until logos.length()) {
        val logo = logos.optJSONObject(i) ?: continue
        if (logo.optString("iso_639_1") == "en") {
            return logoUrlAt(i)
        }
    }

    return logoUrlAt(0)
}

suspend fun filepressExtractor(
    source: String,
    filepressID : String,
    referer: String?,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
) {
    val filepressBaseUrl = JSONObject(
        app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/urls.json").text
    ).optString("filepress")

    if(filepressBaseUrl.isNullOrEmpty()) return

    val client = OkHttpClient()
    val request = Request.Builder()
        .url("$filepressBaseUrl/api/file/get/$filepressID")
        .addHeader("Referer", filepressBaseUrl)
        .get()
        .build()

    val response = client.newCall(request).execute()
    val responseBody = response.body.string()
    val json = JSONObject(responseBody)
    if(json.optBoolean("status") == false) return
    val data = json.optJSONObject("data")

    val size = data?.optString("size")?.toLongOrNull() ?: 0

    val formattedSize = if (size < 1024L * 1024 * 1024) {
        val sizeInMB = size.toDouble() / (1024 * 1024)
        "%.2f MB".format(sizeInMB)
    } else {
        val sizeInGB = size.toDouble() / (1024 * 1024 * 1024)
        "%.2f GB".format(sizeInGB)
    }

    val fileName = data?.optString("name") ?: ""
    val simplifiedTitle = getSimplifiedTitle(fileName + formattedSize)

    val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    val payload1 = JSONObject().apply {
        put("id", filepressID)
        put("method", "indexDownlaod")
        put("captchaValue", JSONObject.NULL)
    }

    val request1 = Request.Builder()
        .url("$filepressBaseUrl/api/file/downlaod/")
        .post(payload1.toString().toRequestBody(jsonMediaType))
        .addHeader("Referer", filepressBaseUrl)
        .build()

    val response1 = client.newCall(request1).execute()
    val responseBody1 = response1.body.string()

    val json1 = JSONObject(responseBody1)
    val status = json1.optBoolean("status")

    if (status) {
        val filepressToken = json1.optJSONObject("data")?.toString() ?: json1.optString("data")
        val payload2 = JSONObject().apply {
            put("id", filepressToken)
            put("method", "indexDownlaod")
            put("captchaValue", JSONObject.NULL)
        }

        val request2 = Request.Builder()
            .url("$filepressBaseUrl/api/file/downlaod2/")
            .post(payload2.toString().toRequestBody(jsonMediaType))
            .addHeader("Referer", filepressBaseUrl)
            .build()

        val response2 = client.newCall(request2).execute()
        val responseBody2 = response2.body.string()
        val json2 = JSONObject(responseBody2)
        val finalLink = json2.optJSONArray("data")?.optString(0) ?: return

        callback.invoke(
            newExtractorLink(
                "Filepress",
                "$source [Filepress] $simplifiedTitle",
                finalLink,
                ExtractorLinkType.VIDEO
            ) {
                this.quality = getIndexQuality(fileName)
                this.referer = filepressBaseUrl
            }
        )
    }
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
        "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
        "Origin" to mainUrl,
        "Referer" to mainUrl,
    )
    val id = url.substringAfter("d/").substringBefore("/")

    val genAccountRes = app.post("$mainApi/accounts", headers = headers).text
    val jsonResp = JSONObject(genAccountRes)
    val token = jsonResp.getJSONObject("data").getString("token") ?: return
    val globalRes = app.get("$mainUrl/dist/js/config.js", headers = headers).text
    val wt = Regex("""appdata\.wt\s*=\s*[\"']([^\"']+)[\"']""").find(globalRes)?.groupValues?.get(1) ?: return

    val response = app.get("$mainApi/contents/$id?cache=true&sortField=createTime&sortDirection=1",
        headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Origin" to mainUrl,
            "Referer" to mainUrl,
            "Authorization" to "Bearer $token",
            "X-Website-Token" to wt
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
        val simplifiedTitle = getSimplifiedTitle(fileName + formattedSize)
        callback.invoke(
            newExtractorLink(
                "Gofile",
                "$source [Gofile] $simplifiedTitle",
                link,
                ExtractorLinkType.VIDEO
            ) {
                this.quality = getIndexQuality(fileName)
                this.headers = mapOf(
                    "Cookie" to "accountToken=$token"
                )
            }
        )
    }
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
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Referer" to protonmoviesAPI
        )
        val json = app.get(protonmoviesAPI + url, headers = headers).text

        JSONObject(json).getJSONObject("ppd").optJSONObject("mixdrop.ag")?.optString("link")?.takeIf { it.isNotEmpty() }?.let {
            val source = it.replace("mxdrop.to", "mixdrop.ps")
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

            val ppd = JSONObject(idRes).getJSONObject("ppd")

            ppd.optJSONObject("gofile.io")?.optString("link")?.takeIf { it.isNotEmpty() }?.let {
                val source = it.replace("\\/", "/")
                gofileExtractor("Protonmovies", source, "", subtitleCallback, callback)
            }

            ppd.optJSONObject("filepress")?.optString("link")?.takeIf { it.isNotEmpty() }?.let {
                filepressExtractor("Protonmovies", it, "", subtitleCallback, callback)
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

// fun evpKDF(password: ByteArray, salt: ByteArray, keySize: Int, ivSize: Int): Pair<ByteArray, ByteArray> {
//     val totalSize = keySize + ivSize
//     val derived = ByteArray(totalSize)
//     var block: ByteArray? = null
//     var offset = 0

//     while (offset < totalSize) {
//         val hasher = MessageDigest.getInstance("MD5")
//         if (block != null) hasher.update(block)
//         hasher.update(password)
//         hasher.update(salt)
//         block = hasher.digest()

//         val len = Math.min(block.size, totalSize - offset)
//         System.arraycopy(block, 0, derived, offset, len)
//         offset += len
//     }

//     val key = derived.copyOfRange(0, keySize)
//     val iv = derived.copyOfRange(keySize, totalSize)
//     return Pair(key, iv)
// }

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
            val id = obj.optString("id").takeIf { it.isNotEmpty() } ?: continue
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
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "$gojoBaseAPI/",
            "Origin" to gojoBaseAPI
        )

        val jsonObject = JSONObject(json)
        val sourcesArray = jsonObject.optJSONArray("sources") ?: return

        for (i in 0 until sourcesArray.length()) {
            val source = sourcesArray.optJSONObject(i) ?: continue
            val url = source.optString("url").takeIf { it.isNotEmpty() } ?: continue
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
                    this.headers = headers
                }
            )
        }

        val subtitles = jsonObject.optJSONArray("subtitles") ?: return

        for (i in 0 until subtitles.length()) {
            val item = subtitles.optJSONObject(i) ?: continue
            val url = item.optString("url").takeIf { it.isNotEmpty() } ?: continue
            val lang = item.optString("lang").takeIf { it.isNotEmpty() } ?: continue

            subtitleCallback.invoke(
                newSubtitleFile(
                    getLanguage(lang) ?: lang,
                    url
                )
            )
        }
    } catch (e: Exception) {
        println("Error parsing Gojo streams for $provider [$lang]: ${e.message}")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
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


fun getVideoQuality(string: String?): Int {
    return Regex("(\\d{3,4})[pP]").find(string ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: Qualities.Unknown.value
}

// fun generateHashedString(): String {
//     val s = "a8f7e9c2d4b6a1f3e8c9d2t4a7f6e9c2d4z6a1f3e8c9d2b4a7f5e9c2d4b6a1f3"
//     val a = "2"
//     val algorithm = "HmacSHA512"
//     val keySpec = SecretKeySpec(s.toByteArray(StandardCharsets.UTF_8), algorithm)
//     val mac = Mac.getInstance(algorithm)
//     mac.init(keySpec)

//     val input = "crypto_rotation_v${a}_seed_2025"
//     val hmacBytes = mac.doFinal(input.toByteArray(StandardCharsets.UTF_8))
//     val hex = hmacBytes.joinToString("") { "%02x".format(it) }

//     val repeated = hex.repeat(3)
//     val result = repeated.substring(0, max(s.length, 128))

//     return result
// }

fun cinemaOSGenerateHash(t: CinemaOsSecretKeyRequest,isSeries: Boolean): String {
    val primary = "a7f3b9c2e8d4f1a6b5c9e2d7f4a8b3c6e1d9f7a4b2c8e5d3f9a6b4c1e7d2f8a5"
    val secondary = "d3f8a5b2c9e6d1f7a4b8c5e2d9f3a6b1c7e4d8f2a9b5c3e7d4f1a8b6c2e9d5f3"


    // Create content identifier string
    val contentString = createContentString(t)

    // First HMAC with primary key
    val firstHash = calculateHmacSha256(contentString, primary)

    // Second HMAC with secondary key
    return calculateHmacSha256(firstHash, secondary)

}


private fun createContentString(info: CinemaOsSecretKeyRequest): String {
    val parts = mutableListOf<String>()

    info.tmdbId.takeIf { it.isNotEmpty() }?.let { parts.add("tmdbId:$it") }
    info.imdbId.takeIf { it.isNotEmpty() }?.let { parts.add("imdbId:$it") }
    info.seasonId.takeIf { it.isNotEmpty() }?.let { parts.add("seasonId:$it") }
    info.episodeId.takeIf { it.isNotEmpty() }?.let { parts.add("episodeId:$it") }

    return parts.joinToString("|")
}

private fun calculateHmacSha256(data: String, key: String): String {
    val algorithm = "HmacSHA256"
    val secretKeySpec = SecretKeySpec(key.toByteArray(), algorithm)
    val mac = Mac.getInstance(algorithm)
    mac.init(secretKeySpec)

    val bytes = mac.doFinal(data.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// Helper function to convert byte array to hex string
// fun bytesToHex(bytes: ByteArray): String {
//     val hexChars = CharArray(bytes.size * 2)
//     for (i in bytes.indices) {
//         val v = bytes[i].toInt() and 0xFF
//         hexChars[i * 2] = "0123456789abcdef"[v ushr 4]
//         hexChars[i * 2 + 1] = "0123456789abcdef"[v and 0x0F]
//     }
//     return String(hexChars)
// }

fun cinemaOSDecryptResponse(e: CinemaOSReponseData?): Any {
    val encrypted = e?.encrypted
    val cin = e?.cin
    val mao = e?.mao
    val salt = e?.salt

    val keyBytes =  "a1b2c3d4e4f6477658455678901477567890abcdef1234567890abcdef123456".toByteArray()
    val ivBytes = hexStringToByteArray(cin.toString())
    val authTagBytes = hexStringToByteArray(mao.toString())
    val encryptedBytes =hexStringToByteArray(encrypted.toString())
    val saltBytes = hexStringToByteArray(salt.toString())

    // Derive key with PBKDF2-HMAC-SHA256
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(keyBytes.map { it.toInt().toChar() }.toCharArray(), saltBytes, 100000, 256)
    val tmp = factory.generateSecret(spec)
    val key = SecretKeySpec(tmp.encoded, "AES")

    // AES-256-GCM decrypt
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val gcmSpec = GCMParameterSpec(128, ivBytes)
    cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)
    val decryptedBytes = cipher.doFinal(encryptedBytes + authTagBytes)
    val decryptedData = String(decryptedBytes)

    return decryptedData // Use your JSON parser
}


// Helper function to convert hex string to byte array
fun hexStringToByteArray(hex: String): ByteArray {
    val len = hex.length
    require(len % 2 == 0) { "Hex string must have even length" }

    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

fun parseCinemaOSSources(jsonString: String): List<Map<String, String>> {
    val json = JSONObject(jsonString)
    val sourcesObject = json.getJSONObject("sources")
    val sourcesList = mutableListOf<Map<String, String>>()

    val keys = sourcesObject.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val source = sourcesObject.getJSONObject(key)

        // Check if source has "qualities" object
        if (source.has("qualities")) {
            val qualities = source.getJSONObject("qualities")
            val qualityKeys = qualities.keys()

            while (qualityKeys.hasNext()) {
                val qualityKey = qualityKeys.next()
                val qualityObj = qualities.getJSONObject(qualityKey)

                val sourceMap = mutableMapOf<String, String>()
                sourceMap["server"] = source.optString("server", key)
                sourceMap["url"] = qualityObj.optString("url", "")
                sourceMap["type"] = qualityObj.optString("type", "")
                sourceMap["speed"] = source.optString("speed", "")
                sourceMap["bitrate"] = source.optString("bitrate", "")
                sourceMap["quality"] = qualityKey // Use the quality key (e.g., "480", "720")

                sourcesList.add(sourceMap)
            }
        } else {
            // Regular source with direct URL
            val sourceMap = mutableMapOf<String, String>()
            sourceMap["server"] = source.optString("server", key)
            sourceMap["url"] = source.optString("url", "")
            sourceMap["type"] = source.optString("type", "")
            sourceMap["speed"] = source.optString("speed", "")
            sourceMap["bitrate"] = source.optString("bitrate", "")
            sourceMap["quality"] = source.optString("quality", "")

            sourcesList.add(sourceMap)
        }
    }

    return sourcesList
}

/** Encodes input using Base64 with custom character mapping. */
// fun customEncode(input: String): String {
//     val src = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
//     val dst = "TuzHOxl7b0RW9o_1FPV3eGfmL4Z5pD8cahBQr2U-6yvEYwngXCdJjANtqKIMiSks"
//     val transMap = src.zip(dst).toMap()
//     val base64 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//         Base64.getEncoder().encodeToString(input.toByteArray())
//             .replace("+", "-")
//             .replace("/", "_")
//             .replace("=", "")
//     } else {
//         android.util.Base64.encodeToString(input.toByteArray(), android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
//     }
//     return base64.map { char -> transMap[char] ?: char }.joinToString("")
// }

/** Extracts data using regex pattern */
//  fun extractData(pattern: String, input: String): String {
//     val regex = Regex(pattern)
//     val match = regex.find(input)
//     return match?.groups?.get(1)?.value ?: throw Exception("Pattern not found: $pattern")
// }

/** Performs AES encryption */
//  fun aesEncrypt(data: String): String {
//     val aesKey = hexStringToByteArray("034bcfc6275541ff4059bffb23d6d1d23ea49b55f79ea730ac540d1213a61339")
//     val aesIv = hexStringToByteArray("a2e7ad865464f12105e9df84f5bdabed")

//     val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
//     cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(aesIv))

//     val encryptedData = cipher.doFinal(data.toByteArray())
//     return bytesToHex(encryptedData)
// }

/** Performs XOR operation */
//  fun xorOperation(input: String): String {
//     val xorKey = hexStringToByteArray("aaa27e7e3cff888285")
//     val result = StringBuilder()

//     for (i in input.indices) {
//         val char = input[i]
//         val xorByte = xorKey[i % xorKey.size].toInt()
//         result.append((char.code xor xorByte).toChar())
//     }

//     return result.toString()
// }

// fun parseServers(jsonString: String): List<TripleOneMoviesServer> {
//     val servers = mutableListOf<TripleOneMoviesServer>()
//     try {
//         val jsonArray = JSONArray(jsonString)
//         for (i in 0 until jsonArray.length()) {
//             val jsonObject = jsonArray.getJSONObject(i)
//             val server = TripleOneMoviesServer(
//                 name = jsonObject.getString("name"),
//                 description = jsonObject.getString("description"),
//                 image = jsonObject.getString("image"),
//                 data = jsonObject.getString("data")
//             )
//             servers.add(server)
//         }
//     } catch (e: Exception) {
//         Log.e("salman731", "Manual parsing failed: ${e.message}")
//     }
//     return servers
// }

//  fun customEncode(input: ByteArray): String {
//     val sourceChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
//     val targetChars = "yfhNJUs1-djqrDczw08Mk7CeQF4AvWltRGO3ao5Ypn9HKPBbEVSi_X2Zg6IuLmTx"

//     val translationMap = sourceChars.zip(targetChars).toMap()
//     val encoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//         Base64.getUrlEncoder().withoutPadding().encodeToString(input)
//     } else {
//         TODO("VERSION.SDK_INT < O")
//     }

//     return encoded.map { char ->
//         translationMap[char] ?: char
//     }.joinToString("")
// }


/**
 * Utility function to convert hex string to byte array
 */
//  fun hexStringToByteArray2(hex: String): ByteArray {
//     val result = ByteArray(hex.length / 2)
//     for (i in hex.indices step 2) {
//         val value = hex.substring(i, i + 2).toInt(16)
//         result[i / 2] = value.toByte()
//     }
//     return result
// }

/**
 * PKCS7 padding implementation
 */
//  fun padData(data: ByteArray, blockSize: Int): ByteArray {
//     val padding = blockSize - (data.size % blockSize)
//     val result = ByteArray(data.size + padding)
//     System.arraycopy(data, 0, result, 0, data.size)
//     for (i in data.size until result.size) {
//         result[i] = padding.toByte()
//     }
//     return result
// }

/**
 * PBKDF2 key derivation using Bouncy Castle
 */
// fun derivePbkdf2Key(password: String, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
//    val generator = PKCS5S2ParametersGenerator(SHA256Digest())
//    generator.init(password.toByteArray(Charsets.UTF_8), salt, iterations)
//    return (generator.generateDerivedParameters(keyLength * 8) as KeyParameter).key
//}

// fun derivePbkdf2Key(
//     password: String,
//     salt: ByteArray,
//     iterations: Int,
//     keyLength: Int
// ): ByteArray {
//     val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
//     val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength * 8)
//     return factory.generateSecret(spec).encoded
// }

/**
 * Remove PKCS7 padding
 */
// fun unpadData(data: ByteArray): ByteArray {
//     val padding = data[data.size - 1].toInt() and 0xFF
//     if (padding < 1 || padding > data.size) {
//         return data
//     }
//     return data.copyOf(data.size - padding)
// }

// fun hasHost(url: String): Boolean {
//     return try {
//         val host = URL(url).host
//         !host.isNullOrEmpty()
//     } catch (e: Exception) {
//         false
//     }
// }
