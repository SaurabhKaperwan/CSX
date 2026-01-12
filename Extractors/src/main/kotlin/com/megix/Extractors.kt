package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject
import okhttp3.FormBody
import okhttp3.*
import java.net.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.lagradost.api.Log
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.VidHidePro
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

val VIDEO_HEADERS = mapOf(
    "User-Agent" to "VLC/3.6.0 LibVLC/3.0.18 (Android)",
    "Accept" to "*/*",
    "Accept-Encoding" to "identity",
    "Connection" to "keep-alive",
    "Range" to "bytes=0-",
    "Icy-MetaData" to "1"
)

fun getIndexQuality(str: String?): Int {
    return Regex("""(\d{3,4})[pP]""").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: Qualities.Unknown.value
}

fun getBaseUrl(url: String): String {
    return URI(url).let {
        "${it.scheme}://${it.host}"
    }
}

suspend fun getLatestUrl(url: String, source: String): String {
    val link = JSONObject(
        app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/urls.json").text
    ).optString(source)
    if(link.isNullOrEmpty()) {
        return getBaseUrl(url)
    }
    return link
}

class Watchadsontape : StreamTape() {
    override var mainUrl: String = "https://watchadsontape.com"
}

class Smoothpre : VidHidePro() {
    override var mainUrl: String = "https://smoothpre.com"
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
        app.get(url).document.select("div.center_it a").amap {
            loadExtractor(it.attr("href"), referer, subtitleCallback, callback)
        }
    }
}

open class XDmovies: ExtractorApi() {
    override val name: String = "XDmovies"
    override val mainUrl: String = "https://link.xdmovies.site"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val location = app.get(url, allowRedirects = false).headers["location"]
        if(location != null) {
            loadExtractor(location, "", subtitleCallback, callback)
        }

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

    private suspend fun CFType1(url: String): List<String> {
        val document = app.get(url+"?type=1").document
        val links = document.select("a.btn-success").mapNotNull { it.attr("href") }
        return links
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

        val fileName = document.select("ul > li.list-group-item:contains(Name)").text().substringAfter("Name : ") ?: ""
        val fileSize = document.select("ul > li.list-group-item:contains(Size)").text().substringAfter("Size : ") ?: ""
        val quality = getIndexQuality(fileName)

        document.select("div.text-center > a").amap { element ->
            val text = element.text()
            val href = element.attr("href")
            when {
                text.contains("Cloud Download") -> {
                    try{
                        callback.invoke(
                            newExtractorLink(
                                "$name Cloud",
                                "$name[Cloud] $fileName[$fileSize]",
                                href,
                            ) {
                                this.quality = quality
                                this.headers = VIDEO_HEADERS
                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }
                text.contains("Instant Download") -> {
                    try{
                        val instant = instantLink(href) ?: return@amap
                        callback.invoke(
                            newExtractorLink(
                                "$name Instant(Download)",
                                "$name[Instant(Download)] $fileName[$fileSize]",
                                instant,
                            ) {
                                this.quality = quality
                                this.headers = VIDEO_HEADERS
                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }
                text.contains("Resume Worker Bot") -> {
                    try{
                        val resumeLink = resumeBot(href)
                        callback.invoke(
                            newExtractorLink(
                                "$name ResumeBot",
                                "$name[ResumeBot] $fileName[$fileSize]",
                                resumeLink,
                            ) {
                                this.quality = quality
                                this.headers = VIDEO_HEADERS
                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }

                }
                text.contains("Direct Links") -> {
                    try {
                        val link = baseUrl + href
                        CFType1(link).forEach {
                            callback.invoke(
                                newExtractorLink(
                                    "$name CF Type1",
                                    "$name[CF Type1] $fileName[$fileSize]",
                                    it,
                                ) {
                                    this.quality = quality
                                    this.headers = VIDEO_HEADERS
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }
                text.contains("Resume Cloud") -> {
                    try {
                        val resumeCloud = resumeCloudLink(baseUrl, href) ?: return@amap
                        callback.invoke(
                            newExtractorLink(
                                "$name ResumeCloud",
                                "$name[ResumeCloud] $fileName[$fileSize]",
                                resumeCloud,
                            ) {
                                this.quality = quality
                                this.headers = VIDEO_HEADERS
                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }

                text.contains("gofile") -> {
                    Gofile().getUrl(href, "", subtitleCallback, callback)
                }
                else -> {
                    Log.d("Error", "No Server matched")
                }
            }
        }
    }
}

open class VCloud : ExtractorApi() {
    override val name: String = "V-Cloud"
    override val mainUrl: String = "https://vcloud.*"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        var href = url
        if (href.contains("api/index.php"))
        {
            href=app.get(url).document.selectFirst("div.main h4 a")?.attr("href") ?:""
        }
        val doc = app.get(href).document
        val scriptTag = doc.selectFirst("script:containsData(url)")?.toString() ?:""
        val urlValue = Regex("var url = '([^']*)'").find(scriptTag) ?. groupValues ?. get(1) ?: ""
        if (urlValue.isNotEmpty()) {
            val document = app.get(urlValue).document
            val div = document.selectFirst("div.card-body")
            val header = document.select("div.card-header").text() ?: ""
            val size = document.select("i#size").text() ?: ""
            val quality = getIndexQuality(header)

            div?.select("h2 a.btn")?.forEach {
                val link = it.attr("href")
                val text = it.text()
                if (text.contains("FSL Server")) {
                    callback.invoke(
                        newExtractorLink(
                            "$name[FSL Server]",
                            "$name[FSL Server] $header[$size]",
                            link,
                        ) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                } else if (text.contains("FSLv2")) {
                    callback.invoke(
                        newExtractorLink(
                            "$name[FSLv2 Server]",
                            "$name[FSLv2 Server] $header[$size]",
                            link,
                        ) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }
                else if (text.contains("[Server : 1]")) {
                    callback.invoke(
                        newExtractorLink(
                            "$name",
                            "$name $header[$size]",
                            link,
                        ) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }
                else if(text.contains("BuzzServer")) {
                    val dlink = app.get("$link/download", referer = link, allowRedirects = false).headers["hx-redirect"] ?: ""
                    val baseUrl = getBaseUrl(link)
                    if(dlink != "") {
                        callback.invoke(
                            newExtractorLink(
                                "$name[BuzzServer]",
                                "$name[BuzzServer] $header[$size]",
                                baseUrl + dlink,
                            ) {
                                this.quality = quality
                                this.headers = VIDEO_HEADERS
                            }
                        )
                    }
                }

                else if (link.contains("pixeldra")) {
                    val baseUrlLink = getBaseUrl(link)
                    val finalURL = if (link.contains("download", true)) link
                    else "$baseUrlLink/api/file/${link.substringAfterLast("/")}?download"

                    callback.invoke(
                        newExtractorLink(
                            "Pixeldrain",
                            "Pixeldrain $header[$size]",
                            finalURL,
                        ) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }
                else if (text.contains("Server : 10Gbps")) {
                    val dlink = app.get(link).document.select("a#vd").attr("href")
                    callback.invoke(
                        newExtractorLink(
                            "$name[Download]",
                            "$name[Download] $header[$size]",
                            dlink,
                        ) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }
                else
                {
                    if(!link.contains(".zip") && (link.contains(".mkv") || link.contains(".mp4"))) {
                        callback.invoke(
                            newExtractorLink(
                                "$name",
                                "$name $header[$size]",
                                link,
                            ) {
                                this.quality = quality
                                this.headers = VIDEO_HEADERS
                            }
                        )
                    }
                }
            }
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
        val latestUrl = getLatestUrl(url, "hubcloud")
        val baseUrl = getBaseUrl(url)
        val newUrl = url.replace(baseUrl, latestUrl)
        val doc = app.get(newUrl).document
        var link = if(newUrl.contains("drive")) {
            val scriptTag = doc.selectFirst("script:containsData(url)")?.toString() ?: ""
            Regex("var url = '([^']*)'").find(scriptTag) ?. groupValues ?. get(1) ?: ""
        }
        else {
            doc.selectFirst("div.vd > center > a") ?. attr("href") ?: ""
        }

        if(!link.startsWith("https://")) {
            link = latestUrl + link
        }

        val document = app.get(link).document
        val div = document.selectFirst("div.card-body")
        val header = document.select("div.card-header").text() ?: ""
        val size = document.select("i#size").text() ?: ""
        val quality = getIndexQuality(header)

        div?.select("h2 a.btn")?.forEach {
            val link = it.attr("href")
            val text = it.text()

            if (text.contains("FSL Server"))
            {
                callback.invoke(
                    newExtractorLink(
                        "$name[FSL Server]",
                        "$name[FSL Server] $header[$size]",
                        link,
                    ) {
                        this.quality = quality
                        this.headers = VIDEO_HEADERS
                    }
                )
            }
            else if (text.contains("FSLv2"))
            {
                callback.invoke(
                    newExtractorLink(
                        "$name[FSLv2 Server]",
                        "$name[FSLv2 Server] $header[$size]",
                        link,
                    ) {
                        this.quality = quality
                        this.headers = VIDEO_HEADERS
                    }
                )
            }
            else if (text.contains("[Mega Server]"))
            {
                callback.invoke(
                    newExtractorLink(
                        "$name[Mega Server]",
                        "$name[Mega Server] $header[$size]",
                        link,
                    ) {
                        this.quality = quality
                        this.headers = VIDEO_HEADERS
                    }
                )
            }
            else if (text.contains("Download File")) {
                callback.invoke(
                    newExtractorLink(
                        "$name",
                        "$name $header[$size]",
                        link,
                    ) {
                        this.quality = quality
                        this.headers = VIDEO_HEADERS
                    }
                )
            }
            else if(text.contains("BuzzServer")) {
                val dlink = app.get("$link/download", referer = link, allowRedirects = false).headers["hx-redirect"] ?: ""
                val baseUrl = getBaseUrl(link)
                if(dlink != "") {
                    callback.invoke(
                        newExtractorLink(
                            "$name[BuzzServer]",
                            "$name[BuzzServer] $header[$size]",
                            baseUrl + dlink,
                        ) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }
            }

            else if (link.contains("pixeldra")) {
                val baseUrlLink = getBaseUrl(link)
                val finalURL = if (link.contains("download", true)) link
                else "$baseUrlLink/api/file/${link.substringAfterLast("/")}?download"

                callback.invoke(
                    newExtractorLink(
                        "Pixeldrain",
                        "Pixeldrain $header[$size]",
                        finalURL,
                    ) {
                        this.quality = quality
                        this.headers = VIDEO_HEADERS
                    }
                )
            }
            else if (text.contains("Server : 10Gbps")) {
                val dlink = app.get(link).document.select("a#vd").attr("href")
                callback.invoke(
                    newExtractorLink(
                        "$name[Download]",
                        "$name[Download] $header[$size]",
                        dlink,
                    ) {
                        this.quality = quality
                        this.headers = VIDEO_HEADERS
                    }
                )
            }
            else
            {
                if(!link.contains(".zip") && (link.contains(".mkv") || link.contains(".mp4"))) {
                    callback.invoke(
                        newExtractorLink(
                            "$name",
                            "$name $header[$size]",
                            link,
                        ) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }
            }
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

class GDLink : GDFlix() {
    override var mainUrl = "https://gdlink.*"
}

class GDFlixNet : GDFlix() {
    override var mainUrl = "https://new12.gdflix.*"
}

open class GDFlix : ExtractorApi() {
    override val name = "GDFlix"
    override val mainUrl = "https://gdflix.*"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val latestUrl = getLatestUrl(url, "gdflix")
        val baseUrl = getBaseUrl(url)
        val newUrl = url.replace(baseUrl, latestUrl)
        val document = app.get(newUrl).document
        val fileName = document.select("ul > li.list-group-item:contains(Name)").text()
            .substringAfter("Name : ").orEmpty()
        val fileSize = document.select("ul > li.list-group-item:contains(Size)").text()
            .substringAfter("Size : ").orEmpty()
        val quality = getIndexQuality(fileName)

        document.select("div.text-center a").forEach { anchor ->
            val text = anchor.select("a").text()
            val link = anchor.attr("href")

            when {
                text.contains("DIRECT DL") -> {
                    callback.invoke(
                        newExtractorLink("GDFlix[Direct]", "GDFlix[Direct] $fileName[$fileSize]", link) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }

                text.contains("DIRECT SERVER") -> {
                    callback.invoke(
                        newExtractorLink("GDFlix[Direct]", "GDFlix[Direct] $fileName[$fileSize]", link) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }

                text.contains("CLOUD DOWNLOAD [R2]") -> {
                    callback.invoke(
                        newExtractorLink("GDFlix[Cloud]", "GDFlix[Cloud] $fileName[$fileSize]", link) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }

                link.contains("pixeldra") -> {
                    val baseUrlLink = getBaseUrl(link)
                    val finalURL = if (link.contains("download", true)) link
                        else "$baseUrlLink/api/file/${link.substringAfterLast("/")}?download"
                    callback.invoke(
                        newExtractorLink(
                            "Pixeldrain",
                            "GDFlix[Pixeldrain] $fileName[$fileSize]",
                            finalURL
                        ) {
                            this.quality = quality
                            this.headers = VIDEO_HEADERS
                        }
                    )
                }

                text.contains("Index Links") -> {
                    try {
                        app.get("$latestUrl$link").document
                            .select("a.btn.btn-outline-info").amap { btn ->
                                val serverUrl = latestUrl + btn.attr("href")
                                app.get(serverUrl).document
                                    .select("div.mb-4 > a").amap { sourceAnchor ->
                                        val source = sourceAnchor.attr("href")
                                        callback.invoke(
                                            newExtractorLink("GDFlix[Index]", "GDFlix[Index] $fileName[$fileSize]", source) {
                                                this.quality = quality
                                                this.headers = VIDEO_HEADERS
                                            }
                                        )
                                    }
                            }
                    } catch (e: Exception) {
                        Log.d("Index Links", e.toString())
                    }
                }

                text.contains("DRIVEBOT") -> {
                    try {
                        val driveLink = link
                        val id = driveLink.substringAfter("id=").substringBefore("&")
                        val doId = driveLink.substringAfter("do=").substringBefore("==")
                        val baseUrls = listOf("https://drivebot.sbs", "https://indexbot.site")

                        baseUrls.amap { baseUrl ->
                            val indexbotLink = "$baseUrl/download?id=$id&do=$doId"
                            val indexbotResponse = app.get(indexbotLink, timeout = 100L)

                            if (indexbotResponse.isSuccessful) {
                                val cookiesSSID = indexbotResponse.cookies["PHPSESSID"]
                                val indexbotDoc = indexbotResponse.document

                                val token = Regex("""formData\.append\('token', '([a-f0-9]+)'\)""")
                                    .find(indexbotDoc.toString())?.groupValues?.get(1).orEmpty()

                                val postId = Regex("""fetch\('/download\?id=([a-zA-Z0-9/+]+)'""")
                                    .find(indexbotDoc.toString())?.groupValues?.get(1).orEmpty()

                                val requestBody = FormBody.Builder()
                                    .add("token", token)
                                    .build()

                                val headers = mapOf("Referer" to indexbotLink)
                                val cookies = mapOf("PHPSESSID" to "$cookiesSSID")

                                var downloadLink = app.post(
                                    "$baseUrl/download?id=$postId",
                                    requestBody = requestBody,
                                    headers = headers,
                                    cookies = cookies,
                                    timeout = 100L
                                ).text.let {
                                    Regex("url\":\"(.*?)\"").find(it)?.groupValues?.get(1)?.replace("\\", "").orEmpty()
                                }

                                callback.invoke(
                                    newExtractorLink("GDFlix[DriveBot]", "GDFlix[DriveBot] $fileName[$fileSize]", downloadLink) {
                                        this.referer = baseUrl
                                        this.quality = quality
                                        this.headers = VIDEO_HEADERS
                                    }
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("DriveBot", e.toString())
                    }
                }

                text.contains("Instant DL") -> {
                    try {
                        val instantLink = link
                        val link = app.get(instantLink, allowRedirects = false)
                            .headers["location"]?.substringAfter("url=").orEmpty()

                        callback.invoke(
                            newExtractorLink("GDFlix[Instant Download]", "GDFlix[Instant Download] $fileName[$fileSize]", link) {
                                this.quality = quality
                                this.headers = VIDEO_HEADERS
                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Instant DL", e.toString())
                    }
                }
                text.contains("GoFile") -> {
                    try {
                        app.get(link).document
                            .select(".row .row a").amap { gofileAnchor ->
                                val link = gofileAnchor.attr("href")
                                if (link.contains("gofile")) {
                                    Gofile().getUrl(link, "", subtitleCallback, callback)
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

        // Cloudflare backup links
        // try {
        //     val types = listOf("type=1", "type=2")
        //     types.map { type ->
        //         val source = app.get("${newUrl.replace("file", "wfile")}?$type")
        //             .document.select("a.btn-success").attr("href")

        //         if (source.isNotEmpty()) {
        //             callback.invoke(
        //                 newExtractorLink("GDFlix[CF]", "GDFlix[CF] $fileName[$fileSize]", source) {
        //                     this.quality = getIndexQuality(fileName)
        //                 }
        //             )
        //         }
        //     }
        // } catch (e: Exception) {
        //     Log.d("CF", e.toString())
        // }
    }
}


class Gofile : ExtractorApi() {
    override val name = "Gofile"
    override val mainUrl = "https://gofile.io"
    override val requiresReferer = false
    private val mainApi = "https://api.gofile.io"

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
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
            callback.invoke(
                newExtractorLink(
                    "Gofile",
                    "Gofile $fileName[$formattedSize]",
                    link,
                ) {
                    this.quality = getQuality(fileName)
                    this.headers = VIDEO_HEADERS + mapOf(
                        "Cookie" to "accountToken=$token"
                    )
                }
            )
        }
    }

    private fun getQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }
}

