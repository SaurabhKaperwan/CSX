package com.megix

import android.os.Build
import androidx.annotation.RequiresApi
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.base64Decode
import java.util.Base64
import org.jsoup.nodes.Document
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import java.net.*
import com.lagradost.api.Log
import com.lagradost.nicehttp.NiceResponse
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.nicehttp.RequestBodyTypes

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

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

suspend fun runLimitedAsync(
    concurrency: Int = 5,
    vararg tasks: suspend () -> Unit
) = coroutineScope {
    val semaphore = Semaphore(concurrency)

    tasks.map { task ->
        async(Dispatchers.IO) {
            semaphore.withPermit {
                try {
                    task()
                } catch (e: Exception) {
                    // Log error but continue
                    Log.e("runLimitedAsync", "Task failed: ${e.message}")
                }
            }
        }
    }.awaitAll()
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
        var count = 0
        do {
            verifyResponse = app.post("$mainUrl/tv/p.php")
            verifyCheck = verifyResponse.text
            count++
            if (count > 5) {
                throw Exception("Failed to get cookie")
            }
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

    runAllAsync(
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
) {
    // 1. Anchor the entire extraction process to the Cloudstream lifecycle
    coroutineScope {
        val scope = this

        val processLink: (ExtractorLink) -> Unit = { link ->
            // 2. Launch inside the anchored scope, NOT a detached IO thread!
            scope.launch {
                val isDownload = link.source.contains("Download") ||
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
                    // Added a fallback to the original referer just in case the link dropped it!
                    this.referer = link.referer ?: referer ?: ""
                    this.quality = quality ?: link.quality
                    this.headers = link.headers
                    this.extractorData = link.extractorData
                }

                callback.invoke(newLink)
            }
        }

        if (url.contains("hubcloud.") || url.contains("vcloud.")) {
            HubCloud().getUrl(url, referer, subtitleCallback, processLink)
        } else if(url.contains("gofile.")) {
            Gofile().getUrl(url, referer, subtitleCallback, processLink)
        } else if(url.contains("gdflix.") || url.contains("gdlink.")) {
            GDFlix().getUrl(url, referer, subtitleCallback, processLink)
        } else if(url.contains("fastdlserver.")) {
            fastdlserver().getUrl(url, referer, subtitleCallback, processLink)
        } else if(url.contains("linksmod.")) {
            Linksmod().getUrl(url, referer, subtitleCallback, processLink)
        } else if(url.contains("hubdrive.")) {
            Hubdrive().getUrl(url, referer, subtitleCallback, processLink)
        } else if(url.contains("driveleech.") || url.contains("driveseed.")) {
            Driveleech().getUrl(url, referer, subtitleCallback, processLink)
        } else if(url.contains("howblogs")) {
            Howblogs().getUrl(url, referer, subtitleCallback, processLink)
        } else {
            loadExtractor(url, referer, subtitleCallback, processLink)
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
    coroutineScope {
        val scope = this

        loadExtractor(url, referer, subtitleCallback) { link ->
            scope.launch {
                val newLink = newExtractorLink(
                    name ?: link.source,
                    name ?: link.name,
                    link.url,
                    type = link.type
                ) {
                    this.quality = quality ?: link.quality
                    this.referer = link.referer ?: referer ?: ""
                    this.headers = link.headers
                    this.extractorData = link.extractorData
                }

                callback.invoke(newLink)
            }
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
// suspend fun multiDecrypt(text : String, source: String) : String? {
//     val headers = mapOf(
//         "Content-Type" to "application/json",
//         "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
//         "Accept" to "application/json",
//         "Accept-Language" to "en-US,en;q=0.9'",
//     )

//     val jsonBody = mapOf(
//         "text" to text
//     )

//     val response = app.post(
//         "https://enc-dec.app/api/$source",
//         headers = headers,
//         json = jsonBody
//     )

//     if(response.isSuccessful) {
//         val json = response.text
//         return JSONObject(json).getString("result")
//     }
//     return null
// }

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
suspend fun bypassXDM(url: String): String? {
    val link = app.get(
        url,
        allowRedirects = false,
        timeout = 600L
    ).headers["location"] ?: return null

    if(link.contains("hubcloud")) return link

    val baseUrl = getBaseUrl(link)
    val id = link.substringAfterLast("/")

    if (id.isEmpty()) return null

    val responseText = app.post(
        "$baseUrl/api/session",
        json = mapOf("code" to id)
    ).text

    val json = try {
        JSONObject(responseText)
    } catch (e: Exception) {
        return null
    }
    val sessionId = json.optString("sessionId")
    val token = json.optString("token")

    if (sessionId.isEmpty() || token.isEmpty()) return null

    val source = app.get(
        "$baseUrl/go/$sessionId?t=$token",
        timeout = 600L,
        allowRedirects = false
    ).headers["location"] ?: return null

    return source
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

    return AnimeInfo(finalTitle, finalBanner)
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
    doc.select("tr.infotr").amap { tr ->
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

fun getGojoId(jsonString: String, aniId: Int): String? {
    val results = JSONObject(jsonString).getJSONArray("results")

    for (i in 0 until results.length()) {
        val item = results.getJSONObject(i)
        if (item.optInt("anilist_id") == aniId) {
            return item.getString("id")
        }
    }
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
