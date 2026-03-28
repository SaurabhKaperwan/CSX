package com.megix

// Cloudstream Core, Network, Utils, & Logging
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.getAndUnpack

// Network (OkHttp & Java Net)
import java.net.URI
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

// JSON Parsing (Gson, Jackson, Org)
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// Java IO
import java.io.IOException

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

import com.megix.settings.Settings

class Rapid : MegaPlay() {
    override val name = "Rapid"
    override val mainUrl = "https://rapid-cloud.co"
    override val requiresReferer = true
}

open class MegaPlay : ExtractorApi() {
    override val name = "MegaPlay"
    override val mainUrl = "https://megaplay.buzz"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        val mainHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.5",
            "Accept-Encoding" to "gzip, deflate, br, zstd",
            "Origin" to mainUrl,
            "Referer" to "$mainUrl/",
            "Connection" to "keep-alive",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache"
        )

        try {
            val headers = mapOf(
                "Accept" to "*/*",
                "X-Requested-With" to "XMLHttpRequest",
                "Referer" to mainUrl
            )

            val id = url.substringAfterLast("/").substringBefore("?")
                .takeIf { it.isNotBlank() }
                ?: app.get(url, headers = headers)
                    .document
                    .selectFirst("#vidcloud-player")
                    ?.attr("data-id")
                ?: return


            val apiUrl = "$mainUrl/embed-2/v2/e-1/getSources?id=$id"

            val response = app.get(apiUrl, headers = headers)
                .parsedSafe<MegaPlayResponse>()
                ?: return

            val m3u8 = response.sources?.firstOrNull()?.file ?: return

            M3u8Helper.generateM3u8(name, m3u8, mainUrl, headers = mainHeaders)
                .forEach(callback)

            response.tracks?.forEach { track ->
                if (track.kind == "captions" || track.kind == "subtitles") {
                    val file = track.file ?: return@forEach
                    val label = track.label ?: "Unknown"

                    subtitleCallback(
                        newSubtitleFile(label, file)
                    )
                }
            }

        } catch (e: Exception) {
            Log.e("MegaPlay", "Extraction failed: ${e.message}")
        }
    }

    data class MegaPlayResponse(
        val sources: List<Source>?,
        val tracks: List<Track>?,
        val encrypted: Boolean?,
        val intro: Intro?,
        val outro: Outro?,
        val server: Long?
    )

    data class Source(
        val file: String?,
        val type: String?
    )

    data class Track(
        val file: String?,
        val label: String?,
        val kind: String?,
        val default: Boolean?
    )

    data class Intro(
        val start: Long?,
        val end: Long?
    )

    data class Outro(
        val start: Long?,
        val end: Long?
    )
}

open class ZenCloudz : ExtractorApi() {
    override val name = "ZenCloudz"
    override val mainUrl = "https://zencloudz.cc"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to "$mainUrl/",
            "User-Agent" to USER_AGENT
        )

        val response = app.get(url, headers = headers).text

        val regex = Regex("""data:\s*\[null,null,(\{.*?\})\],\s*form:\s*null""")
        val matchResult = regex.find(response) ?: return
        val jsonString = matchResult.groupValues[1]

        val siteData = JSONObject(jsonString).getJSONObject("data")
        val obfuscatedCryptoData = siteData.getJSONObject("obfuscated_crypto_data")
        val obfuscationSeed = siteData.getString("obfuscation_seed")

        val primaryHash = sha256(obfuscationSeed)
        val secondaryHash = sha256(primaryHash)

        val containerField = "cd_${primaryHash.substring(24, 32)}"
        val arrayField = "ad_${primaryHash.substring(32, 40)}"
        val objectField = "od_${primaryHash.substring(40, 48)}"
        val keyField = "kf_${primaryHash.substring(8, 16)}"
        val ivField = "ivf_${primaryHash.substring(16, 24)}"
        val tokenField = "${primaryHash.substring(48, 64)}_${primaryHash.substring(56, 64)}"
        val secondaryKeyField = "${secondaryHash.substring(0, 16)}_${secondaryHash.substring(16, 24)}"

        val containerData = obfuscatedCryptoData.getJSONObject(containerField)
        val arrayData = containerData.getJSONArray(arrayField)
        val objectData = arrayData.getJSONObject(0).getJSONObject(objectField)

        val encryptedKeyB64 = objectData.getString(keyField)
        val ivB64 = objectData.getString(ivField)
        val secondaryKeyB64 = siteData.getString(secondaryKeyField)
        val tokenReference = siteData.getString(tokenField)

        val tokenResponseStr = app.get("$mainUrl/api/m3u8/$tokenReference", headers = headers).text
        val tokenJson = JSONObject(tokenResponseStr)

        val encryptedVideoB64 = tokenJson.getString("video_b64")
        val dynamicKeyB64 = tokenJson.getString("key_frag")

        val encryptedKeyBytes = Base64.decode(encryptedKeyB64, Base64.DEFAULT)
        val secondaryKeyBytes = Base64.decode(secondaryKeyB64, Base64.DEFAULT)
        val dynamicKeyBytes = Base64.decode(dynamicKeyB64, Base64.DEFAULT)
        val ivBytes = Base64.decode(ivB64, Base64.DEFAULT)
        val ciphertextBytes = Base64.decode(encryptedVideoB64, Base64.DEFAULT)

        val sboxSeed = obfuscationSeed.substring(0, 8).toLong(16).toInt()
        val sboxTable = generateSbox(sboxSeed)
        val aesKey = deriveAesKey(encryptedKeyBytes, secondaryKeyBytes, dynamicKeyBytes, sboxTable)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(aesKey, "AES")
        val ivParameterSpec = IvParameterSpec(ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

        val plaintextBytes = cipher.doFinal(ciphertextBytes)
        val videoUrl = String(plaintextBytes, Charsets.UTF_8)

        Log.d("ZenCloudz", "Decrypted URL: $videoUrl")

        callback.invoke(
            newExtractorLink(
                name,
                name,
                videoUrl,
                type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else INFER_TYPE
            ) {
                this.referer = "$mainUrl/"
                this.quality = 1080
            }
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSbox(seed: Int): ByteArray {
        val sbox = ByteArray(256)
        for (i in 0 until 256) {
            sbox[i] = ((i * 37 + seed) and 0xFF).toByte()
        }
        return sbox
    }

    private fun deriveAesKey(keyFragment: ByteArray, secondaryKey: ByteArray, dynamicKey: ByteArray, sbox: ByteArray): ByteArray {
        val length = keyFragment.size
        val aesKey = ByteArray(length)
        for (i in 0 until length) {
            aesKey[i] = (keyFragment[i].toInt() xor
                         secondaryKey[i].toInt() xor
                         dynamicKey[i].toInt() xor
                         sbox[i and 0xFF].toInt()).toByte()
        }
        return aesKey
    }
}

class Streameeeeee : Videostr() {
    override var name = "Streameeeeee"
    override var mainUrl = "https://streameeeeee.site"
}

open class Videostr : ExtractorApi() {
    override val name = "Videostr"
    override val mainUrl = "https://videostr.net"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mainHeaders = mapOf(
            "User-Agent" to USER_AGENT,
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.5",
            "Accept-Encoding" to "gzip, deflate, br, zstd",
            "Origin" to mainUrl,
            "Referer" to "$mainUrl/",
            "Connection" to "keep-alive",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache"
        )

        val headers = mapOf(
            "Accept" to "*/*",
            "X-Requested-With" to "XMLHttpRequest",
            "Referer" to "$mainUrl/",
            "User-Agent" to USER_AGENT
        )

        val response = app.get(url, headers = headers)
        val document = response.document
        val htmlResponse = response.text

        val videoTag = document.selectFirst("[id$=\"-player\"]") ?: return
        val fileId = videoTag.attr("data-id")
        if (fileId.isEmpty()) return

        val tripleRegex = Regex("""\b([a-zA-Z0-9]{16})\b.*?\b([a-zA-Z0-9]{16})\b.*?\b([a-zA-Z0-9]{16})\b""", RegexOption.DOT_MATCHES_ALL)
        val singleRegex = Regex("""\b[a-zA-Z0-9]{48}\b""")

        val tripleMatch = tripleRegex.find(htmlResponse)
        val singleMatch = singleRegex.find(htmlResponse)

        val nonce = if (tripleMatch != null) {
            tripleMatch.groupValues[1] + tripleMatch.groupValues[2] + tripleMatch.groupValues[3]
        } else {
            singleMatch?.value ?: return
        }

        val apiUrl = "$mainUrl/embed-1/v3/e-1/getSources?id=$fileId&_k=$nonce"
        val jsonResponse = app.get(apiUrl, headers = headers).text

        val rootObj = JSONObject(jsonResponse)
        val sourcesArray = rootObj.optJSONArray("sources") ?: return

        if (sourcesArray.length() == 0) return

        val m3u8 =  sourcesArray.optJSONObject(0)?.optString("file") ?: return

        M3u8Helper.generateM3u8(name, m3u8, mainUrl, headers = mainHeaders)
            .forEach(callback)
    }
}

//Thanks to https://github.com/yogesh-hacker/MediaVanced
open class Gofile : ExtractorApi() {
    override val name = "Gofile"
    override val mainUrl = "https://gofile.io"
    override val requiresReferer = false
    private val mainApi = "https://api.gofile.io"
    private val browserLanguage = "en-GB"
    private val secret = "5d4f7g8sd45fsd"

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val id = Regex("/(?:\\?c=|d/)([\\da-zA-Z-]+)").find(url)?.groupValues?.get(1) ?: return

        val token = app.post(
            "$mainApi/accounts",
        ).parsedSafe<AccountResponse>()?.data?.token ?: return

        val currentTimeSeconds = System.currentTimeMillis() / 1000
        val interval = (currentTimeSeconds / 14400).toString()
        val message = listOf(USER_AGENT, browserLanguage, token, interval, secret).joinToString("::")
        val hashedToken = sha256(message)

        val headers = mapOf(
            "Referer" to "$mainUrl/",
            "User-Agent" to USER_AGENT,
            "Authorization" to "Bearer $token",
            "X-BL" to browserLanguage,
            "X-Website-Token" to hashedToken
        )

        val parsedResponse = app.get(
            "$mainApi/contents/$id?contentFilter=&page=1&pageSize=1000&sortField=name&sortDirection=1",
            headers = headers
        ).parsedSafe<GofileResponse>()

        val childrenMap = parsedResponse?.data?.children ?: return

        for ((_, file) in childrenMap) {
            if (file.link.isNullOrEmpty() || file.type != "file") continue
            val fileName = file.name ?: ""
            val size = file.size ?: 0L
            val formattedSize = formatBytes(size)

            callback.invoke(
                newExtractorLink(
                    "Gofile",
                    "[Gofile] $fileName [$formattedSize]",
                    file.link,
                    ExtractorLinkType.VIDEO
                ) {
                    this.quality = getQuality(fileName)
                    this.headers = mapOf("Cookie" to "accountToken=$token")
                }
            )
        }
    }

    private fun getQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024L * 1024 * 1024 -> "%.2f MB".format(bytes.toDouble() / (1024 * 1024))
            else -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
        }
    }

    data class AccountResponse(
        @param:JsonProperty("data") val data: AccountData? = null
    )

    data class AccountData(
        @param:JsonProperty("token") val token: String? = null
    )

    data class GofileResponse(
        @param:JsonProperty("data") val data: GofileData? = null
    )

    data class GofileData(
        @param:JsonProperty("children") val children: Map<String, GofileFile>? = null
    )

    data class GofileFile(
        @param:JsonProperty("type") val type: String? = null,
        @param:JsonProperty("name") val name: String? = null,
        @param:JsonProperty("link") val link: String? = null,
        @param:JsonProperty("size") val size: Long? = 0L
    )
}

class Wootly : ExtractorApi() {
    override var name = "Wootly"
    override var mainUrl = "https://www.wootly.ch"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val iframe = app.get(url).document.selectFirst("iframe")?.attr("src") ?: return

        val iframeHtml = app.post(
            iframe,
            headers = mapOf("Referer" to url),
            data = mapOf("qdfx" to "1")
        ).text

        val vdRegex = Regex("""var\s+vd\s*=\s*["']([^"']+)["']""")
        val tkRegex = Regex("""tk\s*=\s*["']([^"']+)["']""")

        val vd = vdRegex.find(iframeHtml)?.groupValues?.get(1)
        val tk = tkRegex.find(iframeHtml)?.groupValues?.get(1)

        if (vd.isNullOrBlank() || tk.isNullOrBlank()) {
            return
        }

        val streamUrl = app.get(
            "https://web.wootly.ch/grabm?t=$tk&id=$vd",
            headers = mapOf("Referer" to iframe)
        ).text.trim()

        if (streamUrl.isBlank() || !streamUrl.startsWith("http")) return

        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                url = streamUrl,
                type = ExtractorLinkType.VIDEO,
            ) {
                this.referer = referer ?: ""
            }
        )
    }
}

class GDLink : GDFlix() {
    override var mainUrl = "https://gdlink.*"
}

class GDFlixApp: GDFlix() {
    override var mainUrl = "https://new.gdflix.*"
}

class GdFlix1: GDFlix() {
    override var mainUrl = "https://new1.gdflix.*"
}

class GdFlix2: GDFlix() {
    override var mainUrl = "https://*.gdflix.*"
}

class GDFlixNet : GDFlix() {
    override var mainUrl = "https://new16.gdflix.*"
}

open class GDFlix : ExtractorApi() {
    override val name = "GDFlix"
    override val mainUrl = "https://gdflix.*"
    override val requiresReferer = false

    private suspend fun CFType(url: String): List<String> {
        val types = listOf("1", "2")
        val downloadLinks = mutableListOf<String>()

        types.safeAmap { t ->
            try {
                val document = app.get(url + "?type=$t").document
                val links = document.select("a.btn-success").mapNotNull { it.attr("href") }
                downloadLinks.addAll(links)
            } catch (e: Exception) {
                Log.d("Error", e.toString())
            }
        }
        return downloadLinks
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        var baseUrl = getBaseUrl(url)
        val latestBaseUrl = getLatestBaseUrl(baseUrl, "gdflix")

        var newUrl = url

        if(baseUrl != latestBaseUrl) {
            newUrl = url.replace(baseUrl, latestBaseUrl)
            baseUrl = latestBaseUrl
        }

        val document = app.get(newUrl).document
        val fileName = document.select("ul > li.list-group-item:contains(Name)").text()
            .substringAfter("Name : ").orEmpty()
        val fileSize = document.select("ul > li.list-group-item:contains(Size)").text()
            .substringAfter("Size : ").orEmpty()
        val quality = getIndexQuality(fileName)

        suspend fun myCallback(link: String, server: String = "") {
            callback.invoke(
                newExtractorLink(
                    "${name}${server}",
                    "${name}${server} ${fileName}[${fileSize}]",
                    link,
                    ExtractorLinkType.VIDEO
                ) {
                    this.quality = quality
                }
            )
        }


        document.select("div.text-center a").safeAmap { anchor ->
            val text = anchor.select("a").text()
            val link = anchor.attr("href")

            when {
                text.contains("FSL V2") -> { myCallback(link, "[FSL V2]") }

                text.contains("DIRECT DL") -> { myCallback(link, "[Direct]") }

                text.contains("DIRECT SERVER") -> { myCallback(link, "[Direct]") }

                text.contains("CLOUD DOWNLOAD [R2]") -> { myCallback(link, "[Cloud]") }

                text.contains("FAST CLOUD") -> {

                    val dlink = app.get(baseUrl + link)
                        .document
                        .select("div.card-body a")
                        .attr("href")
                    if(dlink == "") return@safeAmap
                    myCallback(dlink, "[FAST CLOUD]")
                }

                link.contains("pixeldra") -> {
                    val baseUrlLink = getBaseUrl(link)
                    val finalURL = if (link.contains("download", true)) link
                    else "$baseUrlLink/api/file/${link.substringAfterLast("/")}?download"
                    myCallback(finalURL, "[Pixeldrain]")
                }

                Settings.allowDownloadLinks && text.contains("Instant DL") -> {
                    try {
                        val instantLink = app.get(link, allowRedirects = false)
                            .headers["location"]?.substringAfter("url=").orEmpty()
                        myCallback(instantLink, "[Instant Download]")

                    } catch (e: Exception) {
                        Log.d("Instant DL", e.toString())
                    }
                }

                text.contains("GoFile") -> {
                    try {
                        app.get(link).document
                            .select(".row .row a").safeAmap { gofileAnchor ->
                                val link = gofileAnchor.attr("href")
                                if (link.contains("gofile")) {
                                    loadExtractor(link, "", subtitleCallback, callback)
                                }
                            }
                    } catch (e: Exception) {
                        Log.d("Gofile", e.toString())
                    }
                }

                else -> {
                    Log.d("Error", "No Server matched")
                }
            }
        }

        //Cloudflare backup links
        try {
            val sources = CFType(newUrl.replace("file", "wfile"))

            sources.safeAmap { source ->
                val redirectUrl = resolveFinalUrl(source) ?: return@safeAmap
                myCallback(redirectUrl, "[CF]")
            }
        } catch (e: Exception) {
            Log.d("CF", e.toString())
        }
    }
}

open class fastdlserver : ExtractorApi() {
    override val name = "fastdlserver"
    override var mainUrl = "https://fastdlserver.*"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val location = app.get(url, allowRedirects = false).headers["location"]
        if (location != null) {
            loadExtractor(location, "", subtitleCallback, callback)
        }
    }
}

class Linksmod : ExtractorApi() {
    override val name = "Linksmod"
    override var mainUrl = "https://linksmod.*"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).document

        document.select("div .view-well > a").safeAmap {
            val link = it.attr("href")
            loadExtractor(link, "", subtitleCallback, callback)
        }
    }
}

open class Hubdrive : ExtractorApi() {
    override val name = "Hubdrive"
    override val mainUrl = "https://hubdrive.*"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val href = app.get(url).document.select(".btn.btn-primary.btn-user.btn-success1.m-1").attr("href")
        loadExtractor(href, "", subtitleCallback, callback)
    }
}

class Driveseed : Driveleech() {
    override val name: String = "Driveseed"
    override val mainUrl: String = "https://driveseed.*"
}

open class Driveleech : ExtractorApi() {
    override val name: String = "Driveleech"
    override val mainUrl: String = "https://driveleech.*"
    override val requiresReferer = false

    private suspend fun CFType(url: String): List<String> {
        val types = listOf("1", "2")
        val downloadLinks = mutableListOf<String>()

        types.map { t ->
            val document = app.get(url + "?type=$t").document
            val links = document.select("a.btn-success").mapNotNull { it.attr("href") }
            downloadLinks.addAll(links)
        }
        return downloadLinks
    }

    private suspend fun resumeCloudLink(baseUrl: String, url: String): String? {
        val resumeCloudUrl = baseUrl + url
        val document = app.get(resumeCloudUrl).document
        val link = document.selectFirst("a.btn-success")?.attr("href")
        return link
    }

    private suspend fun resumeBot(url : String): String {
        val resumeBotResponse = app.get(url)
        val resumeBotDoc = resumeBotResponse.document.toString()
        val ssid = resumeBotResponse.cookies["PHPSESSID"]
        val resumeBotToken = Regex("formData\\.append\\('token', '([a-f0-9]+)'\\)").find(resumeBotDoc)?.groups?.get(1)?.value
        val resumeBotPath = Regex("fetch\\('/download\\?id=([a-zA-Z0-9/+]+)'").find(resumeBotDoc)?.groups?.get(1)?.value
        val resumeBotBaseUrl = url.split("/download")[0]
        val requestBody = FormBody.Builder()
            .addEncoded("token", "$resumeBotToken")
            .build()

        val jsonResponse = app.post(resumeBotBaseUrl + "/download?id=" + resumeBotPath,
            requestBody = requestBody,
            headers = mapOf(
                "Accept" to "*/*",
                "Origin" to resumeBotBaseUrl,
                "Sec-Fetch-Site" to "same-origin"
            ),
            cookies = mapOf("PHPSESSID" to "$ssid"),
            referer = url
        ).text
        val jsonObject = JSONObject(jsonResponse)
        val link = jsonObject.getString("url")
        return link
    }

    private suspend fun instantLink(finallink: String): String? {
        val link = app.get(finallink, allowRedirects = false).headers["location"]
        return link?.substringAfter("?url=")
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val baseUrl = getBaseUrl(url)
        val document = if(url.contains("r?key=")) {
            val temp = app.get(url).document.selectFirst("script")?.data()?.substringAfter("replace(\"")?.substringBefore("\")") ?: ""
            app.get(baseUrl + temp).document
        }
        else {
            app.get(url).document
        }

        val fileName = document.select("ul > li.list-group-item:contains(Name)").text().substringAfter("Name : ")
        val fileSize = document.select("ul > li.list-group-item:contains(Size)").text().substringAfter("Size : ")
        val quality = getIndexQuality(fileName)

        suspend fun myCallback(link: String, server: String = "") {
            callback.invoke(
                newExtractorLink(
                    "${name}${server}",
                    "${name}${server} ${fileName}[${fileSize}]",
                    link,
                    ExtractorLinkType.VIDEO
                ) {
                    this.quality = quality
                }
            )
        }

        document.select("div.text-center > a").safeAmap { element ->
            val text = element.text()
            val href = element.attr("href")

            when {

                text.contains("Cloud Download") -> { myCallback(href, "[Cloud]") }

                Settings.allowDownloadLinks && text.contains("Instant Download") -> {
                    try{
                        val instant = instantLink(href) ?: return@safeAmap
                        myCallback(instant, "[Instant(Download)]")
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }

                text.contains("Resume Worker Bot") -> {
                    try{
                        val resumeLink = resumeBot(href)
                        myCallback(resumeLink, "[ResumeBot]")
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }

                }

                text.contains("Direct Links") -> {
                    try {
                        val link = baseUrl + href
                        CFType(link).forEach {
                            myCallback(it, "[CF]")
                        }
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }

                text.contains("Resume Cloud") -> {
                    try {
                        val resumeCloud = resumeCloudLink(baseUrl, href) ?: return@safeAmap
                        myCallback(resumeCloud, "[ResumeCloud]")
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }

                text.contains("gofile") -> {
                    loadExtractor(href, "", subtitleCallback, callback)
                }

                else -> {
                    Log.d("Error", "No Server matched")
                }
            }
        }
    }
}

class Howblogs : ExtractorApi() {
    override val name: String = "Howblogs"
    override val mainUrl: String = "https://howblogs.*"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        app.get(url).document.select("div.center_it a").safeAmap {
            loadExtractor(it.attr("href"), referer, subtitleCallback, callback)
        }
    }
}

class VCloud : HubCloud() {
    override val name: String = "V-Cloud"
    override val mainUrl: String = "https://vcloud.*"
}

open class HubCloud : ExtractorApi() {
    override val name: String = "Hub-Cloud"
    override val mainUrl: String = "https://hubcloud.*"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        var baseUrl = getBaseUrl(url)

        val latestBaseUrl = if(url.contains("hubcloud")) {
            getLatestBaseUrl(baseUrl, "hubcloud")
        } else {
            getLatestBaseUrl(baseUrl, "vcloud")
        }

        var newUrl = url

        if(baseUrl != latestBaseUrl) {
            newUrl = url.replace(baseUrl, latestBaseUrl)
            baseUrl = latestBaseUrl
        }

        val doc = app.get(newUrl).document

        var link = if(newUrl.contains("/video/")) {
            doc.selectFirst("div.vd > center > a") ?. attr("href") ?: ""
        }
        else {
            val scriptTag = doc.selectFirst("script:containsData(url)")?.toString() ?: ""
            Regex("var url = '([^']*)'").find(scriptTag) ?. groupValues ?. get(1) ?: ""
        }

        if(!link.startsWith("https://")) link = baseUrl + link

        val document = app.get(link).document
        val header = document.select("div.card-header").text()
        val size = document.select("i#size").text()
        val quality = getIndexQuality(header)

        suspend fun myCallback( link: String, server: String = "") {
            callback.invoke(
                newExtractorLink(
                    "${name}${server}",
                    "${name}${server} ${header}[${size}]",
                    link,
                    ExtractorLinkType.VIDEO
                ) {
                    this.quality = quality
                }
            )
        }

        document.select("h2 a.btn").safeAmap {
            val link = it.attr("href")
            val text = it.text()

            if (text.contains("FSL Server")) myCallback(link, "[FSL Server]")
            else if (text.contains("FSLv2")) myCallback(link, "[FSLv2 Server]")
            else if (text.contains("Mega Server")) myCallback(link, "[Mega Server]")
            else if (text.contains("Download File")) myCallback(link)
            else if (link.contains("pixeldra")) {
                val baseUrlLink = getBaseUrl(link)
                val finalURL = if (link.contains("download", true)) link
                else "$baseUrlLink/api/file/${link.substringAfterLast("/")}?download"
                myCallback(finalURL, "[Pixeldrain]")
            }
            else if (Settings.allowDownloadLinks && text.contains("Server : 10Gbps")) {
                var redirectUrl = resolveFinalUrl(link) ?: return@safeAmap
                if(redirectUrl.contains("link=")) redirectUrl = redirectUrl.substringAfter("link=")
                myCallback(redirectUrl, "[Download]")
            }
            else { Log.d("Error", "No Server matched") }
        }
    }
}

open class SuperVideo : ExtractorApi() {
    override val name = "SuperVideo"
    override val mainUrl = "https://supervideo.cc"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url.replace("tv","cc"), referer = referer)
        val script =
            res.document.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data()
        val unpacked = getAndUnpack(script ?: return)
        val m3u8 = Regex("file:\"(.*?m3u8.*?)").find(unpacked)?.groupValues?.getOrNull(1) ?:""
        M3u8Helper.generateM3u8(
            this.name,
            m3u8,
            referer = "$mainUrl/",
        ).forEach(callback)
    }
}

class Kwik : ExtractorApi() {
    override val name            = "Kwik"
    override val mainUrl         = "https://kwik.cx"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val res = app.get(url, referer = url)
        val script =
            res.document.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data()
        val unpacked = getAndUnpack(script ?: return)
        val m3u8 =Regex("source=\\s*'(.*?m3u8.*?)'").find(unpacked)?.groupValues?.getOrNull(1) ?:""
        callback.invoke(
            newExtractorLink(
                name,
                name,
                m3u8,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = url
            }
        )
    }
}

class Pahe : ExtractorApi() {
    override val name = "Pahe"
    override val mainUrl = "https://pahe.win"
    override val requiresReferer = true
    private val kwikParamsRegex = Regex("""\("(\w+)",\d+,"(\w+)",(\d+),(\d+),\d+\)""")
    private val kwikDUrl = Regex("action=\"([^\"]+)\"")
    private val kwikDToken = Regex("value=\"([^\"]+)\"")
    private val client = OkHttpClient()

    private fun decrypt(fullString: String, key: String, v1: Int, v2: Int): String {
        val keyIndexMap = key.withIndex().associate { it.value to it.index }
        val sb = StringBuilder()
        var i = 0
        val toFind = key[v2]

        while (i < fullString.length) {
            val nextIndex = fullString.indexOf(toFind, i)
            val decodedCharStr = buildString {
                for (j in i until nextIndex) {
                    append(keyIndexMap[fullString[j]] ?: -1)
                }
            }

            i = nextIndex + 1

            val decodedChar = (decodedCharStr.toInt(v2) - v1).toChar()
            sb.append(decodedChar)
        }

        return sb.toString()
    }

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val noRedirects = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        val initialRequest = Request.Builder()
            .url("$url/i")
            .get()
            .build()

        val kwikUrl = "https://" + noRedirects.newCall(initialRequest).execute()
                        .header("location")!!.substringAfterLast("https://")

        val fContentRequest = Request.Builder()
            .url(kwikUrl)
            .header("referer", "https://kwik.cx/")
            .header("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
            .get()
            .build()

        val fContent = client.newCall(fContentRequest).execute()
        val fContentString = fContent.body.string()

        val (fullString, key, v1, v2) = kwikParamsRegex.find(fContentString)!!.destructured
        val decrypted = decrypt(fullString, key, v1.toInt(), v2.toInt())

        val uri = kwikDUrl.find(decrypted)!!.destructured.component1()
        val tok = kwikDToken.find(decrypted)!!.destructured.component1()

        val noRedirectClient = OkHttpClient().newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .cookieJar(client.cookieJar)
            .build()

        var code = 419
        var tries = 0
        var content: Response? = null

        while (code != 302 && tries < 20) {
            val formBody = FormBody.Builder()
                .add("_token", tok)
                .build()

            val postRequest = Request.Builder()
                .url(uri)
                .header("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36")
                .header("referer", fContent.request.url.toString())
                .header("cookie",  fContent.headers("set-cookie").firstOrNull().toString())
                .post(formBody)
                .build()

            content = noRedirectClient.newCall(postRequest).execute()
            code = content.code
            tries++
        }

        val location = content?.header("location").toString()
        content?.close()

        callback.invoke(
            newExtractorLink(
                name,
                name,
                location,
            ) {
                this.referer = "https://kwik.cx/"
            }
        )
    }
}

class Akamaicdn : ExtractorApi() {
    override val name = "Akamaicdn"
    override val mainUrl = "https://molop.art"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers= mapOf("user-agent" to "okhttp/4.12.0")
        val res = app.get(url, referer = referer, headers = headers).document
        val sniffScript = res.selectFirst("script:containsData(sniff\\()")
            ?.data()
            ?.substringAfter("sniff(")
            ?.substringBefore(");") ?: return

        val cleaned = sniffScript.replace(Regex("\\[.*?\\]"), "")
        val regex = Regex("\"(.*?)\"")
        val args = regex.findAll(cleaned).map { it.groupValues[1].trim() }.toList()
        val token = args.lastOrNull().orEmpty()
        val m3u8 = "$mainUrl/m3u8/${args[1]}/${args[2]}/master.txt?s=1&cache=1&plt=$token"
        M3u8Helper.generateM3u8(name, m3u8, mainUrl, headers = headers).forEach(callback)
    }
}


class Rapidshare : MegaUp() {
    override var mainUrl = "https://rapidshare.cc"
    override val requiresReferer = true
}

class Fourspromax : MegaUp() {
    override var mainUrl = "https://4spromax.site"
    override val requiresReferer = true
}

class MegaUpTwoTwo : MegaUp() {
    override var mainUrl = "https://megaup22.online"
    override val requiresReferer = true
}

class Rapidairmax : MegaUp() {
    override var mainUrl = "https://rapidairmax.site"
    override val requiresReferer = true
}

//Thanks to https://github.com/AzartX47/EncDecEndpoints
open class MegaUp : ExtractorApi() {
    override var name = "MegaUp"
    override var mainUrl = "https://megaup.live"
    override val requiresReferer = true

    companion object {
        private val HEADERS = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0",
                "Accept" to "text/html, *//*; q=0.01",
                "Accept-Language" to "en-US,en;q=0.5",
                "Sec-GPC" to "1",
                "Sec-Fetch-Dest" to "empty",
                "Sec-Fetch-Mode" to "cors",
                "Sec-Fetch-Site" to "same-origin",
                "Priority" to "u=0",
                "Pragma" to "no-cache",
                "Cache-Control" to "no-cache",
                "referer" to "https://animekai.to/",
        )
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mediaUrl = url.replace("/e/", "/media/").replace("/e2/", "/media/")
        val displayName = referer ?: this.name

        val encodedResult = app.get(mediaUrl, headers = HEADERS)
        .parsedSafe<AnimeKaiResponse>()
        ?.result

        if (encodedResult == null) return

        val body = """
        {
        "text": "$encodedResult",
        "agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:134.0) Gecko/20100101 Firefox/134.0"
        }
        """.trimIndent()
            .trim()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val m3u8Data=app.post("https://enc-dec.app/api/dec-mega", requestBody = body).text
        if (m3u8Data.isBlank()) {
            Log.d("Phisher", "Encoded result is null or empty")
            return
        }

        try {
            val root = JSONObject(m3u8Data)
            val result = root.optJSONObject("result")
            if (result == null) {
                Log.d("Error:", "No 'result' object in M3U8 JSON")
                return
            }

            val sources = result.optJSONArray("sources") ?: JSONArray()
            if (sources.length() > 0) {
                val firstSourceObj = sources.optJSONObject(0)
                val m3u8File = when {
                    firstSourceObj != null -> firstSourceObj.optString("file").takeIf { it.isNotBlank() }
                    else -> {
                        val maybeString = sources.optString(0)
                        maybeString.takeIf { it.isNotBlank() }
                    }
                }
                if (m3u8File != null) {
                    M3u8Helper.generateM3u8(displayName, m3u8File, mainUrl).forEach(callback)
                } else {
                    Log.d("Error:", "No 'file' found in first source")
                }
            } else {
                Log.d("Error:", "No sources found in M3U8 data")
            }

            val tracks = result.optJSONArray("tracks") ?: JSONArray()
            for (i in 0 until tracks.length()) {
                val trackObj = tracks.optJSONObject(i) ?: continue
                val label = trackObj.optString("label").trim().takeIf { it.isNotEmpty() }
                val file = trackObj.optString("file").takeIf { it.isNotBlank() }
                if (label != null && file != null) {
                    subtitleCallback(newSubtitleFile(getLanguage(label) ?: label, file))
                }
            }
        } catch (_: JSONException) {
            Log.e("Error", "Failed to parse M3U8 JSON")
        }
      }

    data class AnimeKaiResponse(
        @param:JsonProperty("status") val status: Int,
        @param:JsonProperty("result") val result: String
    )

}
