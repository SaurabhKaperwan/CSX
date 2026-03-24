package com.megix

// Android
import android.util.Base64

// Cloudstream & NiceHttp
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.APIHolder.unixTimeMS
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.nicehttp.NiceResponse

// Coroutines
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Network
import java.net.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl

// JSON & HTML Parsing
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

// Java Utils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.pow
import kotlin.random.Random

// Security & Crypto
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

// Extractors
import com.lagradost.cloudstream3.extractors.VidHidePro
import com.lagradost.cloudstream3.extractors.StreamWishExtractor

// Settings
import com.megix.settings.Settings

class SpecOption(searchTerms: List<String>, val label: String) {
    constructor(term: String, label: String) : this(listOf(term), label)

    val regex = Regex(
        searchTerms.joinToString(
            separator = "|",
            prefix = "(?i)(?<=^|\\W)(?:",
            postfix = ")(?=[^a-zA-Z0-9_+]|$)"
        ) {
            Regex.escape(it)
        }
    )
}

val SPEC_OPTIONS = mapOf(
    "quality" to listOf(
        SpecOption("UHD BluRay", "4K UHD BluRay 💿"),
        SpecOption("BluRay", "BluRay 💿"),
        SpecOption("BluRay REMUX", "BluRay REMUX 💾"),
        SpecOption("BDRip", "BDRip 💿"),
        SpecOption("BRRip", "BRRip 💿"),
        SpecOption("DVD", "DVD Full/ISO 📀"),
        SpecOption("DVDRip", "DVDRip 📀"),
        SpecOption("DVD5", "DVD5 📀"),
        SpecOption("DVD9", "DVD9 📀"),
        SpecOption("HD-DVD", "HD-DVD 📀"),
        SpecOption("WEB-DL", "WEB-DL ☁️"),
        SpecOption("WEBRip", "WEBRip 🌐"),
        SpecOption("HDRip", "HDRip ✨"),
        SpecOption("HDTV", "HDTV 📺"),
        SpecOption("PDTV", "PDTV 📺"),
        SpecOption("SDTV", "SDTV 📺"),
        SpecOption("PPV", "PPV 🎫"),
        SpecOption("SATRip", "SATRip 📡"),
        SpecOption("DSR", "DSRip 📡"),
        SpecOption("TVRip", "TVRip 📺"),
        SpecOption("CAM", "CAM 📹"),
        SpecOption("TeleSync", "TeleSync 📹"),
        SpecOption("TS", "TS 🚫"),
        SpecOption("TC", "TeleCine 🎞️"),
        SpecOption("SCR", "SCR 📼"),
        SpecOption("DVDScr", "DVDScr 📼"),
        SpecOption("R5", "R5 ⁵"),
        SpecOption("VHS", "VHS 📼"),
        SpecOption("LaserDisc", "LaserDisc 💿")
    ),
    "codec" to listOf(
        SpecOption("av1", "AV1 🚀"),
        SpecOption(listOf("x265", "h.265", "hevc"), "HEVC ⚡"),
        SpecOption("vp9", "VP9 🧪"),
        SpecOption("vp8", "VP8 🧪"),
        SpecOption(listOf("x264", "h.264", "H264", "avc"), "H.264 📦"),
        SpecOption("vc-1", "VC-1 📼"),
        SpecOption("mpeg-2", "MPEG-2 🎞️"),
        SpecOption("mpeg-4", "MPEG-4 🎞️"),
        SpecOption("xvid", "XviD 🧩"),
        SpecOption("divx", "DivX 🧩"),
        SpecOption("wmv", "WMV 🪟"),
        SpecOption("theora", "Theora 🦦"),
        SpecOption("realvideo", "RealVideo 🎥"),
        SpecOption("h.263", "H.263 📱")
    ),
    "bitdepth" to listOf(
        SpecOption("12bit", "12bit 🌈"),
        SpecOption("10bit", "10bit 🎨"),
        SpecOption("Hi10P", "Hi10P (10bit) 🎨"),
        SpecOption("8bit", "8bit 🖍️"),
        SpecOption("3D", "3D 👓"),
        SpecOption("SBS", "3D SBS ↔️"),
        SpecOption("OU", "3D Over/Under ↕️"),
        SpecOption("IMAX", "IMAX 🏟️")
    ),
    "audio" to listOf(
        SpecOption("TrueHD", "Dolby TrueHD 🔊"),
        SpecOption("Atmos", "Dolby Atmos 🌌"),
        SpecOption(listOf("DDP5.1", "DDP 5.1"), "DD+ 5.1 🔉"),
        SpecOption("7.1", "7.1 Ch 🔊"),
        SpecOption("5.1", "5.1 Ch 🔉"),
        SpecOption("DTS-HD MA", "DTS-HD MA 🔊"),
        SpecOption("DTS-HD", "DTS-HD 🔊"),
        SpecOption("DTS:X", "DTS:X 🔊"),
        SpecOption("DTS Lossless", "DTS Lossless 🎼"),
        SpecOption("DTS-ES", "DTS-ES 🔉"),
        SpecOption("PCM", "LPCM/PCM 💿"),
        SpecOption("FLAC", "FLAC 🎹"),
        SpecOption("ALAC", "ALAC 🍏"),
        SpecOption("WAV", "WAV 🌊"),
        SpecOption("AIFF", "AIFF 🎼"),
        SpecOption(listOf("AAC2.0", "AAC 2.0"), "AAC 2.0 🎧"),
        SpecOption("DD2.0", "DD 2.0 🎧"),
        SpecOption(listOf("E-AC3", "DD+", "Dolby Digital Plus"), "DD+ 🔉"),
        SpecOption("AC3", "AC3 (Dolby Digital) 🔈"),
        SpecOption("DD5.1", "Dolby Digital 5.1 🔈"),
        SpecOption("DTS", "DTS 🔈"),
        SpecOption("AAC", "AAC 🎧"),
        SpecOption("HE-AAC", "HE-AAC 🎧"),
        SpecOption("OPUS", "Opus 🎙️"),
        SpecOption("VORBIS", "Vorbis 🌀"),
        SpecOption("MP3", "MP3 🎵"),
        SpecOption("WMA", "WMA 🎵"),
        SpecOption("OGG", "OGG 🌀"),
        SpecOption("MP2", "MP2 📻")
    ),
    "hdr" to listOf(
        SpecOption(listOf("DV", "DoVi", "DOLBYVISION", "Dolby Vision"), "Dolby Vision 👁️"),
        SpecOption("HDR10+", "HDR10+ 🔆"),
        SpecOption("HDR10", "HDR10 🔆"),
        SpecOption("HLG", "HLG 📡"),
        SpecOption("HDR", "HDR 🔆"),
        SpecOption("SDR", "SDR 🔅")
    ),
    "language" to listOf(
        SpecOption(listOf("HIN", "Hindi"), "Hindi 🇮🇳"),
        SpecOption("Tamil", "Tamil 🇮🇳"),
        SpecOption("Telugu", "Telugu 🇮🇳"),
        SpecOption("Malayalam", "Malayalam 🇮🇳"),
        SpecOption("Kannada", "Kannada 🇮🇳"),
        SpecOption("Bengali", "Bengali 🇮🇳"),
        SpecOption("Punjabi", "Punjabi 🇮🇳"),
        SpecOption(listOf("ENG", "English"), "English 🇺🇸"),
        SpecOption(listOf("KOR", "Korean"), "Korean 🇰🇷"),
        SpecOption(listOf("JPN", "Japanese"), "Japanese 🇯🇵"),
        SpecOption(listOf("CHN", "Chinese"), "Chinese 🇨🇳"),
        SpecOption("Spanish", "Spanish 🇪🇸"),
        SpecOption("French", "French 🇫🇷"),
        SpecOption("German", "German 🇩🇪"),
        SpecOption("Italian", "Italian 🇮🇹"),
        SpecOption("Russian", "Russian 🇷🇺"),
        SpecOption("Arabic", "Arabic 🇸🇦"),
        SpecOption(listOf("Multi-Audio", "Multi Audio", "Multi.Audio"), "Multi Audio 🌍"),
        SpecOption(listOf("Dual.Audio", "Dual Audio", "Dual"), "Dual Audio 🌗"),
        SpecOption(listOf("Multi-Sub", "MultiSub", "Multi Sub"), "Multi Subs 💬"),
        SpecOption("ESub", "English Subs 🇺🇸")
    )
)

private val SIZE_REGEX = """(\d+(?:\.\d+)?\s?(?:MB|GB))""".toRegex(RegexOption.IGNORE_CASE)
private val CATEGORY_ORDER = listOf("quality", "codec", "bitdepth", "audio", "hdr", "language")

fun getSimplifiedTitle(title: String): String {
    var remainingTitle = title
    val matchedLabels = mutableListOf<String>()

    CATEGORY_ORDER.forEach { category ->
        SPEC_OPTIONS[category].orEmpty().forEach { spec ->
            if (spec.regex.containsMatchIn(remainingTitle)) {
                matchedLabels.add(spec.label)
                remainingTitle = spec.regex.replace(remainingTitle, " ")
            }
        }
    }

    val sizeMatch = SIZE_REGEX.find(title)?.value?.uppercase()
    val size = sizeMatch?.let { "$it 💾" }

    val result = listOfNotNull(
        matchedLabels.distinct().joinToString(" | ").takeIf { it.isNotEmpty() },
        size
    ).joinToString(" | ")

    return if (result.isEmpty()) "" else "\n$result"
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

suspend fun checkPosterAvailable(posterUrl: String? = null): String? {
    if(posterUrl == null) return null
    return try {
        val res = app.head(posterUrl)
        if (res.code == 200) {
            posterUrl
        } else {
            null
        }

    } catch (e: Exception) {
        null
    }
}

suspend fun getTvdbData(tvType: String, imdbId: String? = null): ExtractedMediaData? {
    if (imdbId == null) return null
    val primaryUrl = "https://aiometadata.elfhosted.com/stremio/9197a4a9-2f5b-4911-845e-8704c520bdf7/meta/$tvType/$imdbId.json"
    var jsonText = try {
        app.get(primaryUrl, timeout = 6L).text
    } catch (e: Exception) {
        ""
    }

    if (jsonText.isEmpty()) {
        val fallbackUrl = "https://94c8cb9f702d-tmdb-addon.baby-beamup.club/meta/$tvType/$imdbId.json"
        jsonText = try {
            app.get(fallbackUrl, timeout = 6L).text
        } catch (e: Exception) {
            ""
        }
    }

    if (jsonText.isEmpty()) return null

    val root = JSONObject(jsonText)
    val meta = root.optJSONObject("meta") ?: return null
    val image_proxy = "https://wsrv.nl/?url="
    val posterUrl = meta.optString("poster").takeIf { it.isNotEmpty() }?.let { "$image_proxy$it" }
    val backgroundUrl = meta.optString("background").takeIf { it.isNotEmpty() }?.let { "$image_proxy$it" }
    val logoUrl = meta.optString("logo").takeIf { it.isNotEmpty() }?.let { "$image_proxy$it" }

    val castArray = meta.optJSONObject("app_extras")?.optJSONArray("cast")
    val castList = if (castArray != null) {
        (0 until castArray.length()).mapNotNull { i ->
            val castMember = castArray.optJSONObject(i) ?: return@mapNotNull null

            val name = castMember.optString("name")
            if (name.isNotEmpty() && name != "null") {
                ActorData(
                    Actor(
                        name = name,
                        image = castMember.optString("photo")
                            .takeIf { it.isNotEmpty() && it != "null" }
                            ?.let { "$image_proxy$it" }
                    ),
                    roleString = castMember.optString("character")
                        .takeIf { it.isNotEmpty() && it != "null" }
                )
            } else null
        }
    } else null

    return ExtractedMediaData(castList, posterUrl, backgroundUrl, logoUrl)
}

suspend fun NFBypass(mainUrl: String): String {
    val (savedCookie, savedTimestamp) = Settings.getCookie()

    if (!savedCookie.isNullOrEmpty() && System.currentTimeMillis() - savedTimestamp < 54_000_000) {
        return savedCookie
    }

    val newCookie = try {
        var verifyCheck: String
        var verifyResponse: NiceResponse
        var count = 0

        do {
            verifyResponse = app.post("$mainUrl/tv/p.php")
            verifyCheck = verifyResponse.text
            count++
            if (count > 5) {
                throw Exception("Failed to get cookie after 5 attempts")
            }
        } while (!verifyCheck.contains("\"r\":\"n\""))

        verifyResponse.cookies["t_hash_t"].orEmpty()
    } catch (e: Exception) {
        Settings.clearCookie()
        throw e
    }

    if (newCookie.isNotEmpty()) {
        Settings.saveCookie(newCookie)
    }

    return newCookie
}

suspend fun getNfVideoToken(mainUrl: String, newUrl: String, id: String, cookies: Map<String, String>): String {
    val headers = mapOf(
        "X-Requested-With" to "XMLHttpRequest",
        "Referer" to "$mainUrl/",
    )

    val json = app.post(
        "$mainUrl/play.php",
        headers = headers,
        cookies = cookies,
        data = mapOf("id" to id)
    ).text
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
    try {
        return URI(url).let {
            "${it.scheme}://${it.host}"
        }
    } catch (e: Exception) {
        return url
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

suspend fun <A, B> Iterable<A>.safeAmap(
    concurrency: Int = 5,
    f: suspend (A) -> B?
): List<B> = supervisorScope {
    val semaphore = Semaphore(concurrency)
    map { item ->
        async<B?>(Dispatchers.IO) {
            semaphore.acquire()
            try {
                f(item)
            } catch (e: CancellationException) {
                if (!this@supervisorScope.isActive) throw e
                Log.w("safeAmap", "Item cancelled locally: ${e.message}")
                null
            } catch (e: Throwable) {
                Log.e("safeAmap", "Item failed: ${e.message}")
                null
            } finally {
                semaphore.release()
            }
        }
    }.awaitAll().filterNotNull()
}

suspend fun runLimitedAsync(
    concurrency: Int = 7,
    vararg tasks: suspend () -> Unit
) = supervisorScope {
    val semaphore = Semaphore(concurrency)
    tasks.map { task ->
        async<Unit>(Dispatchers.IO) {
            semaphore.acquire()
            try {
                task()
            } catch (e: CancellationException) {
                if (!this@supervisorScope.isActive) throw e
                Log.w("runLimitedAsync", "Task cancelled locally: ${e.message}")
            } catch (e: Throwable) {
                Log.e("runLimitedAsync", "Task failed: ${e.message}")
            } finally {
                semaphore.release()
            }
        }
    }.awaitAll()
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
    if (str.isNullOrBlank()) return Qualities.Unknown.value

    Regex("""(\d{3,4})[pP]""").find(str)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
        return it
    }

    val lowerStr = str.lowercase()
    return when {
        lowerStr.contains("8k") -> 4320
        lowerStr.contains("4k") -> 2160
        lowerStr.contains("2k") -> 1440
        else -> Qualities.Unknown.value
    }
}

//Dahmer
fun getIndexQualityTags(str: String?, fullTag: Boolean = false): String {
    return if (fullTag) Regex("(?i)(.*)\\.(?:mkv|mp4|avi)").find(str ?: "")?.groupValues?.get(1)
        ?.trim() ?: str ?: "" else Regex("(?i)\\d{3,4}[pP]\\.?(.*?)\\.(mkv|mp4|avi)").find(
        str ?: ""
    )?.groupValues?.getOrNull(1)
        ?.replace(".", " ")?.trim() ?: str ?: ""
}

suspend fun resolveFinalUrl(startUrl: String): String? {
    var currentUrl = startUrl
    var loopCount = 0
    val maxRedirects = 7

    while (loopCount < maxRedirects) {
        try {
            val res = app.head(currentUrl, allowRedirects = false, timeout = 2500L)
            if (res.code == 200 || res.code in 300..399) {
                val location = res.headers.get("Location")
                if(location.isNullOrEmpty()) break
                currentUrl = location
            } else {
                return null
            }
            loopCount++
        } catch (e: Exception) {
            return null
        }
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

    runLimitedAsync( concurrency = 2,
        {
            val link = doc.select("a.btn-info").attr("href")
            val referer = response.url
            val document = app.get(link, referer = referer).document
            document.select("a.button").map {
                callback.invoke(
                    newExtractorLink(
                        source,
                        source.toSansSerifBold() +" $simplifiedTitle",
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
                    "$source[HCloud]".toSansSerifBold() + " $simplifiedTitle",
                    link,
                    ExtractorLinkType.VIDEO,
                ) {
                    this.quality = getIndexQuality(name)
                }
            )
        },
    )
}

//For Extractor new domain
suspend fun getLatestBaseUrl(baseUrl: String, source: String): String {
    return try {
        val dynamicUrls = app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/urls.json")
            .parsedSafe<Map<String, String>>()
        dynamicUrls?.get(source)?.takeIf { it.isNotBlank() } ?: baseUrl
    } catch (e: Exception) {
        baseUrl
    }
}


//Bold String
fun String.toSansSerifBold(): String {
    val builder = StringBuilder()
    for (char in this) {
        val codePoint = when (char) {
            // Mathematical Sans-Serif Bold ranges
            in 'A'..'Z' -> 0x1D5D4 + (char - 'A')
            in 'a'..'z' -> 0x1D5EE + (char - 'a')
            in '0'..'9' -> 0x1D7EC + (char - '0')
            else -> char.code
        }
        builder.append(Character.toChars(codePoint))
    }
    return builder.toString()
}

suspend fun loadSourceNameExtractor(
    source: String,
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
    quality: Int? = null,
    size: String = ""
) = supervisorScope {
    val processLink: (ExtractorLink) -> Unit = { link ->
        launch(Dispatchers.IO) {
            val isDownload = link.source.contains("Download", ignoreCase = true) ||
                             link.url.contains("video-downloads.googleusercontent")
            val simplifiedTitle = getSimplifiedTitle(link.name)
            val combined = if (source.contains("(Combined)")) " (Combined)" else ""
            val fixSize = if (size.isNotEmpty()) " $size" else ""
            val sourceBold = "$source [${link.source}]".toSansSerifBold()
            val newSourceName = if (isDownload) "Download$combined" else "${link.source}$combined"
            val newName = "$sourceBold $simplifiedTitle$fixSize".trim()

            val newLink = newExtractorLink(
                newSourceName,
                newName,
                link.url,
                type = link.type
            ) {
                this.referer = link.referer
                this.quality = quality ?: link.quality
                this.headers = link.headers
                this.extractorData = link.extractorData
            }

            callback(newLink)
        }
    }

    when {
        url.contains("hubcloud.") || url.contains("vcloud.") -> HubCloud().getUrl(url, referer, subtitleCallback, processLink)
        url.contains("gdflix.") || url.contains("gdlink.") -> GDFlix().getUrl(url, referer, subtitleCallback, processLink)
        url.contains("fastdlserver.") -> fastdlserver().getUrl(url, referer, subtitleCallback, processLink)
        url.contains("linksmod.") -> Linksmod().getUrl(url, referer, subtitleCallback, processLink)
        url.contains("hubdrive.") -> Hubdrive().getUrl(url, referer, subtitleCallback, processLink)
        url.contains("gofile.") -> Gofile().getUrl(url, referer, subtitleCallback, processLink)
        url.contains("driveleech.") || url.contains("driveseed.") -> Driveleech().getUrl(url, referer, subtitleCallback, processLink)
        url.contains("howblogs.") -> Howblogs().getUrl(url, referer, subtitleCallback, processLink)
        else -> loadExtractor(url, referer, subtitleCallback, processLink)
    }
}

suspend fun loadCustomExtractor(
    name: String? = null,
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit,
    quality: Int? = null,
    serverName: String = "",
) = supervisorScope {

    val processLink: (ExtractorLink) -> Unit = { link ->
        launch(Dispatchers.IO) {
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

            callback(newLink)
        }
    }

    when {
        serverName.contains("Vidhide", true) -> VidHidePro().getUrl(url, referer, subtitleCallback, processLink)
        serverName.contains("Streamwish", true) -> StreamWishExtractor().getUrl(url, referer, subtitleCallback, processLink)
        else -> loadExtractor(url, referer, subtitleCallback, processLink)
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

//XDM

fun createMouseData(durationMs: Long, phase: Int = 1): Map<String, Any> {
    val duration = maxOf(1000L, durationMs)
    val steps = maxOf(6L, duration / 1000L).toInt()
    return mapOf(
        "eventCount" to (12 + (steps * 2) + phase),
        "moveCount" to (8 + steps + phase),
        "clickCount" to minOf(3, phase + 1),
        "totalDistance" to (180 + (steps * 90)),
        "hasMovement" to true,
        "duration" to duration
    )
}

fun generateRandomFingerprint(): String {
    return UUID.randomUUID().toString().replace("-", "")
}

suspend fun openAndTrackProtectorSocket(
    socketUrl: String, baseUrl: String, cookies: Map<String, String>, bindToken: String, durationMs: Long
): Unit = suspendCancellableCoroutine { cont ->
    val cookieHeader = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    val request = Request.Builder()
        .url(socketUrl)
        .header("User-Agent", USER_AGENT)
        .header("Cookie", cookieHeader)
        .header("Origin", baseUrl)
        .build()

    val scope = CoroutineScope(Dispatchers.IO)
    var startedAt = 0L
    var intervalJob: Job? = null
    var timeoutJob: Job? = null
    var finished = false

    val finish = { error: Exception? ->
        if (!finished) {
            finished = true
            intervalJob?.cancel()
            timeoutJob?.cancel()
            if (error != null && cont.isActive) cont.resumeWithException(error)
            else if (cont.isActive) cont.resume(Unit)
        }
    }

    timeoutJob = scope.launch {
        delay(maxOf(30_000L, durationMs + 10_000L))
        finish(Exception("WebSocket timeout"))
    }

    val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            when {
                text.startsWith("0") -> webSocket.send("40")
                text == "2" -> webSocket.send("3")
                text.startsWith("40") && startedAt == 0L -> {
                    startedAt = System.currentTimeMillis()
                    webSocket.send("""42["bind","$bindToken"]""")
                    webSocket.send("""42["visibility","visible"]""")

                    intervalJob = scope.launch {
                        while (isActive) {
                            delay(1000)
                            val elapsed = maxOf(1000L, System.currentTimeMillis() - startedAt)
                            webSocket.send("""42["heartbeat"]""")
                            webSocket.send("""42["visibility","visible"]""")
                            webSocket.send("""42["mouseActivity",${JSONObject(createMouseData(elapsed, 2))}]""")
                        }
                    }

                    scope.launch {
                        delay(durationMs)
                        finish(null)
                        webSocket.close(1000, "Done")
                    }
                }
            }
        }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = finish(Exception(t))
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = finish(null)
    }

    val webSocket = OkHttpClient().newWebSocket(request, listener)

    cont.invokeOnCancellation {
        finished = true
        intervalJob?.cancel()
        timeoutJob.cancel()
        webSocket.cancel()
    }
}

suspend fun bypassXDM(url: String): String? {

    val link = app.get(url, allowRedirects = false).headers["location"] ?: return null

    if (link.contains("hubcloud")) return link

    val fingerprint = generateRandomFingerprint()
    val visibleMs = 15_000L

    val uri = URI(link)
    val baseUrl = "${uri.scheme}://${uri.host}"
    val socketUrl = "${if (uri.scheme == "https") "wss" else "ws"}://${uri.host}/socket.io/?EIO=4&transport=websocket"

    val id = link.substringAfter("/r/").substringBefore("?").substringBefore("/")

    if (id.isEmpty() || id == link) return null

    var sessionCookies = mapOf<String, String>()

    return try {
        val sessionResponse = app.post(
            "$baseUrl/api/session",
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to link, "Origin" to baseUrl),
            json = mapOf("code" to id, "fingerprint" to fingerprint, "mouseData" to createMouseData(2500, 1))
        )
        sessionCookies = sessionCookies + sessionResponse.cookies

        val sessionData = JSONObject(sessionResponse.text)
        val sessionId = sessionData.optString("sessionId").ifEmpty { return null }
        val sessionToken = sessionData.optString("token").ifEmpty { return null }

        openAndTrackProtectorSocket(socketUrl, baseUrl, sessionCookies, sessionToken, visibleMs)

        val step2Url = "$baseUrl/r/${URLEncoder.encode(id, "UTF-8")}?step=2&sid=${URLEncoder.encode(sessionId, "UTF-8")}"
        val step2Response = app.get(step2Url, headers = mapOf("User-Agent" to USER_AGENT, "Referer" to link), cookies = sessionCookies)
        sessionCookies = sessionCookies + step2Response.cookies

        val rebindResponse = app.post(
            "$baseUrl/api/session/rebind",
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to step2Url, "Origin" to baseUrl),
            cookies = sessionCookies,
            json = mapOf("fingerprint" to fingerprint)
        )
        sessionCookies = sessionCookies + rebindResponse.cookies
        val rebindToken = JSONObject(rebindResponse.text).optString("token").ifEmpty { return null }

        openAndTrackProtectorSocket(socketUrl, baseUrl, sessionCookies, rebindToken, visibleMs)

        val completeResponse = app.post(
            "$baseUrl/api/session/complete",
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to step2Url, "Origin" to baseUrl),
            cookies = sessionCookies,
            json = mapOf(
                "fingerprint" to fingerprint,
                "mouseData" to createMouseData((visibleMs * 2) + 2500, 3),
                "honeypot" to ""
            )
        )
        sessionCookies = sessionCookies + completeResponse.cookies
        val completeToken = JSONObject(completeResponse.text).optString("token").ifEmpty { return null }

        val goUrl = "$baseUrl/go/${URLEncoder.encode(sessionId, "UTF-8")}?t=${URLEncoder.encode(completeToken, "UTF-8")}"
        val goResponse = app.get(
            goUrl,
            headers = mapOf("User-Agent" to USER_AGENT, "Referer" to step2Url),
            cookies = sessionCookies,
            allowRedirects = false
        )

        goResponse.headers["location"] ?: goResponse.headers["Location"]
    } catch (e: Exception) {
        println("Failed to bypass XDM: ${e.message}")
        null
    }
}

suspend fun getAniListInfo(animeId: Int): AnimeInfo? {
    val query = """
        query (${'$'}id: Int) {
            Media (id: ${'$'}id, type: ANIME) {
                title {
                    english
                    romaji
                }
                bannerImage
                description(asHtml: false)
            }
        }
    """.trimIndent()

    val requestData = mapOf(
        "query" to query,
        "variables" to mapOf("id" to animeId)
    )

    val response = app.post(
        "https://graphql.anilist.co",
        json = requestData
    ).parsedSafe<AniListResponse>()

    val media = response?.data?.media ?: return null

    val finalBanner = media.bannerImage?.takeUnless { it.isBlank() || it == "null" }
    val finalTitle = media.title?.english?.takeUnless { it.isBlank() || it == "null" }
    val finaromajiTitle = media.title?.romaji?.takeUnless { it.isBlank() || it == "null" }
    val finalDescription = media.description?.takeUnless { it.isBlank() || it == "null" }

    return AnimeInfo(
        title = finalTitle,
        romajiTitle = finaromajiTitle,
        banner = finalBanner,
        description = finalDescription
    )
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
    """.trimIndent()

    val variables = mapOf(
        "search" to title,
        "sort" to "SEARCH_MATCH",
        "type" to "ANIME",
        "season" to season?.uppercase(),
        "seasonYear" to year,
        "format" to listOf(if (type == TvType.AnimeMovie) "MOVIE" else "TV", "ONA")
    ).filterValues { it != null && it.toString().isNotEmpty() }

    val res = app.post(
        url = anilistAPI,
        json = mapOf(
            "query" to query,
            "variables" to variables
        )
    ).parsedSafe<AniSearch>()?.data?.Page?.media?.firstOrNull()

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

    val url = if (type == TvType.Movie)
        "$tmdbAPI/movie/$tmdbId/images?api_key=$apiKey"
    else
        "$tmdbAPI/tv/$tmdbId/images?api_key=$apiKey"

    val json = runCatching { JSONObject(app.get(url).text) }.getOrNull() ?: return null
    val logos = json.optJSONArray("logos") ?: return null
    if (logos.length() == 0) return null

    val lang = appLangCode?.trim()?.lowercase()?.substringBefore("-")

    fun path(o: JSONObject) = o.optString("file_path")
    fun isSvg(o: JSONObject) = path(o).endsWith(".svg", true)
    fun urlOf(o: JSONObject) = "https://image.tmdb.org/t/p/w500${path(o)}"

    // Language match
    var svgFallback: JSONObject? = null

    for (i in 0 until logos.length()) {
        val logo = logos.optJSONObject(i) ?: continue
        val p = path(logo)
        if (p.isBlank()) continue

        val l = logo.optString("iso_639_1").trim().lowercase()
        if (l == lang) {
            if (!isSvg(logo)) return urlOf(logo)
            if (svgFallback == null) svgFallback = logo
        }
    }
    svgFallback?.let { return urlOf(it) }

    // Highest voted fallback
    var best: JSONObject? = null
    var bestSvg: JSONObject? = null

    fun voted(o: JSONObject) = o.optDouble("vote_average", 0.0) > 0 && o.optInt("vote_count", 0) > 0

    fun better(a: JSONObject?, b: JSONObject): Boolean {
        if (a == null) return true
        val aAvg = a.optDouble("vote_average", 0.0)
        val aCnt = a.optInt("vote_count", 0)
        val bAvg = b.optDouble("vote_average", 0.0)
        val bCnt = b.optInt("vote_count", 0)
        return bAvg > aAvg || (bAvg == aAvg && bCnt > aCnt)
    }

    for (i in 0 until logos.length()) {
        val logo = logos.optJSONObject(i) ?: continue
        if (!voted(logo)) continue

        if (isSvg(logo)) {
            if (better(bestSvg, logo)) bestSvg = logo
        } else {
            if (better(best, logo)) best = logo
        }
    }

    best?.let { return urlOf(it) }
    bestSvg?.let { return urlOf(it) }

    // No language match & no voted logos
    return null
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
    doc.select("tr.infotr").safeAmap { tr ->
        val id = tr.select("button:contains(Info)").attr("id").split("-").getOrNull(1)
        if(id != null) {
            val uid = "uid_${System.currentTimeMillis()}_${
                (Random.nextDouble() * 36.0.pow(9))
                .toLong()
                .toString(36)
                .padStart(9, '0')
            }"

            val postHeaders = mapOf(
                "User-Agent" to USER_AGENT,
                "Referer" to protonmoviesAPI,
                "Content-Type" to "multipart/form-data",
            )

            val idData = app.post(
                "$protonmoviesAPI/ppd.php",
                headers = postHeaders,
                data = mapOf(
                    "downloadid" to id,
                    "token" to "ok",
                    "uid" to uid
                )
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
                loadSourceNameExtractor("Protonmovies", source, "", subtitleCallback, callback)
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

fun getGojoId(jsonString: String, title: String): String? {

    val results = JSONObject(jsonString).getJSONArray("results")

    for( i in 0 until results.length() ) {
        val item = results.getJSONObject(i)

        val titleObject = item.optJSONObject("title") ?: continue

        val englishTitle = titleObject.optString("english", "")
        val romajiTitle = titleObject.optString("romaji", "")

        if (englishTitle.equals(title, ignoreCase = true) ||
            romajiTitle.equals(title, ignoreCase = true)) {
            return item.getString("id")
        }
    }

    // for (i in 0 until results.length()) {
    //     val item = results.getJSONObject(i)
    //     if (item.optInt("anilist_id") == aniId) {
    //         return item.getString("id")
    //     }
    // }

    return null
}

fun getGojoServers(jsonString: String): List<String> {
    val jsonArray = JSONArray(jsonString)
    val ids = mutableListOf<String>()

    for (i in 0 until jsonArray.length()) {
        val id = jsonArray.getJSONObject(i).optString("id")
        if (id.isNotEmpty()) {
            ids.add(id)
        }
    }
    return ids
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
        val serverName = jsonObject.optString("server", "")
        if(serverName != provider) return
        val sourcesArray = jsonObject.optJSONArray("sources") ?: return

        for (i in 0 until sourcesArray.length()) {
            val source = sourcesArray.optJSONObject(i) ?: continue
            val url = source.optString("url").takeIf { it.isNotEmpty() } ?: continue
            val videoType = source.optString("type", "m3u8")
            val quality = source.optString("quality").replace("p", "").toIntOrNull()

            callback.invoke(
                newExtractorLink(
                    "Animetsu [${lang.uppercase()}] [${provider.uppercase()}]",
                    "Animetsu [${lang.uppercase()}] [${provider.uppercase()}]",
                    fixUrl(url, "https://ani.metsu.site/proxy"),
                    type = if (videoType == "video/mp4") ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
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

suspend fun getRedirectLinks(url: String): String {
    fun encode(value: String): String {
        return Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
    }

    fun decode(value: String): String {
        return String(Base64.decode(value, Base64.DEFAULT))
    }

    fun rot13(value: String): String {
        return value.map {
            when (it) {
                in 'A'..'Z' -> 'A' + (it - 'A' + 13) % 26
                in 'a'..'z' -> 'a' + (it - 'a' + 13) % 26
                else -> it
            }
        }.joinToString("")
    }

    return try {
        val doc = app.get(url).text

        val regex = """s\('o','([A-Za-z0-9+/=]+)'|ck\('_wp_http_\d+','([^']+)'""".toRegex()

        val combinedString = regex.findAll(doc)
            .mapNotNull { it.groups[1]?.value ?: it.groups[2]?.value }
            .joinToString("")

        if (combinedString.isEmpty()) return ""

        val decodedString = decode(rot13(decode(decode(combinedString))))
        val jsonObject = JSONObject(decodedString)

        val encodedUrl = decode(jsonObject.optString("o", "")).trim()
        val data = encode(jsonObject.optString("data", "")).trim()
        val wphttp1 = jsonObject.optString("blog_url", "").trim()

        val directLink = if (wphttp1.isNotEmpty() && data.isNotEmpty()) {
            runCatching {
                app.get("$wphttp1?re=$data").document.select("body").text().trim()
            }.getOrDefault("")
        } else {
            ""
        }

        encodedUrl.ifEmpty { directLink }
    } catch (e: Exception) {
        ""
    }
}

fun cinemaOSGenerateHash(tmdbId: Int?, imdbId: String?, season: Int?, episode: Int?): String {
    val primary = "a7f3b9c2e8d4f1a6b5c9e2d7f4a8b3c6e1d9f7a4b2c8e5d3f9a6b4c1e7d2f8a5"
    val secondary = "d3f8a5b2c9e6d1f7a4b8c5e2d9f3a6b1c7e4d8f2a9b5c3e7d4f1a8b6c2e9d5f3"

    var message = "tmdbId:$tmdbId|imdbId:$imdbId"

    if (season != null && episode != null) {
        message += "|seasonId:$season|episodeId:$episode"
    }
    val firstHash = calculateHmacSha256(message, primary)
    return calculateHmacSha256(firstHash, secondary)
}

private fun calculateHmacSha256(data: String, key: String): String {
    val algorithm = "HmacSHA256"
    val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), algorithm)
    val mac = Mac.getInstance(algorithm)
    mac.init(secretKeySpec)

    val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

fun cinemaOSDecryptResponse(e: CinemaOSReponseData?): String? {
    if (e == null) return null

    if (e.encrypted.isEmpty() || e.cin.isEmpty() || e.mao.isEmpty() || e.salt.isEmpty()) {
        return null
    }

    val passwordStr = "a1b2c3d4e4f6477658455678901477567890abcdef1234567890abcdef123456"

    val ivBytes = hexStringToByteArray(e.cin)
    val authTagBytes = hexStringToByteArray(e.mao)
    val encryptedBytes = hexStringToByteArray(e.encrypted)
    val saltBytes = hexStringToByteArray(e.salt)

    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(passwordStr.toCharArray(), saltBytes, 100000, 256)
    val tmp = factory.generateSecret(spec)
    val key = SecretKeySpec(tmp.encoded, "AES")

    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val gcmSpec = GCMParameterSpec(128, ivBytes)
    cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

    val decryptedBytes = cipher.doFinal(encryptedBytes + authTagBytes)
    return String(decryptedBytes, Charsets.UTF_8)
}

fun hexStringToByteArray(hex: String): ByteArray {
    require(hex.length % 2 == 0) { "Hex string must have even length" }
    return hex.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun parseCinemaOSSources(jsonString: String): List<Map<String, String>> {
    val json = JSONObject(jsonString)
    val sourcesObject = json.getJSONObject("sources")
    val sourcesList = mutableListOf<Map<String, String>>()

    val keys = sourcesObject.keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val source = sourcesObject.getJSONObject(key)

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
                sourceMap["quality"] = qualityKey

                sourcesList.add(sourceMap)
            }
        } else {
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

fun decryptVidzeeUrl(encryptedUrl: String, secret: String): String? {
    return try {
        val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
        val decodedString = String(decodedBytes, Charsets.UTF_8)
        val parts = decodedString.split(":", limit = 2)
        if (parts.size < 2) return null
        val ivB64 = parts[0]
        val ciphertextB64 = parts[1]
        val iv = Base64.decode(ivB64, Base64.DEFAULT)
        val ciphertext = Base64.decode(ciphertextB64, Base64.DEFAULT)
        val key = secret.padEnd(32, '\u0000').toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(key, "AES")
        val ivParameterSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
        val decryptedData = cipher.doFinal(ciphertext)
        String(decryptedData, Charsets.UTF_8)
    } catch (e: Exception) {
        null
    }
}

suspend fun getUpcloud(
    iframeUrl: String,
    referer: String,
    callback: (ExtractorLink) -> Unit
) {
    val html = app.get(iframeUrl, referer = referer).text
    val sourceEncoded = Regex("""var\s+source\s*=\s*"([^"]+)\"""")
        .find(html)?.groupValues?.get(1) ?: return
    val sourceUrl = JSONObject("""{"v":"$sourceEncoded"}""").optString("v")
    val domain    = sourceUrl.toHttpUrl().host
    val embedType = sourceUrl.toHttpUrl().pathSegments.getOrNull(0) ?: return
    val iframeHeaders = mapOf(
        "User-Agent"       to USER_AGENT,
        "Referer"          to "https://$domain/",
        "X-Requested-With" to "XMLHttpRequest"
    )
    val htmlSource = app.get(sourceUrl, headers = iframeHeaders).text
    val videoId = Regex("""<title>File\s+#([A-Za-z0-9]+)\s*-""")
        .find(htmlSource)?.groupValues?.get(1) ?: return
    val nonce = Regex("""\b[a-zA-Z0-9]{48}\b""").find(htmlSource)?.value
        ?: run {
            val m = Regex("""\b([a-zA-Z0-9]{16})\b.*?\b([a-zA-Z0-9]{16})\b.*?\b([a-zA-Z0-9]{16})\b""")
                .find(htmlSource) ?: return
            m.groupValues.drop(1).joinToString("")
        }
    val api = "https://$domain/$embedType/v3/e-1/getSources?id=$videoId&_k=$nonce"
    val streamsData = runCatching { JSONObject(app.get(api, headers = iframeHeaders).text) }.getOrNull() ?: return
    val sources  = streamsData.optJSONArray("sources") ?: return

    for (i in 0 until sources.length()) {
        val streamUrl = sources.getJSONObject(i).optString("file").ifEmpty { return }
        callback.invoke(
            newExtractorLink(
                "VidsrcCC[UpCloud]",
                "VidsrcCC[UpCloud]",
                streamUrl,
                ExtractorLinkType.M3U8
            ) {
                this.headers = iframeHeaders
                this.quality = 1080
            }
        )
    }
}

fun getVidrockUrlEncode(itemId: String): String {
    val passphrase = "x7k9mPqT2rWvY8zA5bC3nF6hJ2lK4mN9"
    val keyBytes = passphrase.toByteArray(Charsets.UTF_8)
    val ivBytes = keyBytes.copyOfRange(0, 16)
    val secretKey = SecretKeySpec(keyBytes, "AES")
    val ivSpec = IvParameterSpec(ivBytes)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
    val encryptedBytes = cipher.doFinal(itemId.toByteArray(Charsets.UTF_8))
    val base64Encoded = android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
    val urlEncoded = URLEncoder.encode(base64Encoded, "UTF-8").replace("%2F", "/")
    return urlEncoded
}

//Xpass

fun extractXpassBackups(html: String): List<Pair<String, String>> {
    val raw = Regex("""var backups=(\[.*?]);""", RegexOption.DOT_MATCHES_ALL)
        .find(html)?.groupValues?.get(1) ?: return emptyList()
    val array = JSONArray(raw)
    return (0 until array.length()).mapNotNull { i ->
        val obj  = array.getJSONObject(i)
        val name = obj.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val url  = obj.optString("url").takeIf  { it.isNotBlank() } ?: return@mapNotNull null
        Pair(name, url)
    }
}
