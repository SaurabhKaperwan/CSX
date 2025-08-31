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

fun getIndexQuality(str: String?): Int {
    return Regex("""(\d{3,4})[pP]""").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: Qualities.Unknown.value
}

class Watchadsontape : StreamTape() {
    override var mainUrl: String = "https://watchadsontape.com"
}

class Smoothpre : VidHidePro() {
    override var mainUrl: String = "https://smoothpre.com"
}

class Howblogs : ExtractorApi() {
    override val name: String = "Howblogs"
    override val mainUrl: String = "https://howblogs.xyz"
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

class Ziddiflix: Vifix() {
    override val name: String = "Ziddiflix"
    override val mainUrl: String = "https://ziddiflix.com"
    override val requiresReferer = false
}

open class Vifix: ExtractorApi() {
    override val name: String = "Vifix"
    override val mainUrl: String = "https://vifix.site"
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

class Linkstore : ExtractorApi() {
    override val name: String = "Linkstore"
    override val mainUrl: String = "https://linkstore.rest"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        app.get(url).document.select("a.ep-simple-button").amap {
            loadExtractor(it.attr("href"), "", subtitleCallback, callback)
        }
    }
}

class LinkstoreDrive : ExtractorApi() {
    override val name: String = "Linkstore Drive"
    override val mainUrl: String = "https://drive.linkstore.rest"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val redirectUrl = url.replace(mainUrl, "https://new7.luxedrive.space")
        loadExtractor(redirectUrl, "", subtitleCallback, callback)
    }
}

open class Luxdrive : ExtractorApi() {
    override val name: String = "Luxdrive"
    override val mainUrl: String = "https://new7.luxedrive.space"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).document
        document.select("div > div > a").map {
            val href = it.attr("href")
            if(href.contains(".mkv")) {
                callback.invoke(
                    newExtractorLink(
                        "Instant(Download)",
                        "Instant(Download)",
                        href,
                    )
                )
            } else {
                loadExtractor(href, "", subtitleCallback, callback)
            }
        }
    }
}

class Driveseed : Driveleech() {
    override val name: String = "Driveseed"
    override val mainUrl: String = "https://driveseed.org"
}

open class Driveleech : ExtractorApi() {
    override val name: String = "Driveleech"
    override val mainUrl: String = "https://driveleech.net"
    override val requiresReferer = false

    private suspend fun CFType1(url: String): List<String> {
        val document = app.get(url+"?type=1").document
        val links = document.select("a.btn-success").mapNotNull { it.attr("href") }
        return links
    }

    private suspend fun resumeCloudLink(url: String): String {
        val resumeCloudUrl = mainUrl + url
        val document = app.get(resumeCloudUrl).document
        val link = document.selectFirst("a.btn-success")?.attr("href").toString()
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

    private suspend fun instantLink(finallink: String): String {
        val baseUrl = finallink.substringBefore("/?url=").substringAfter("https://")
        val token = finallink.substringAfter("url=")
        val downloadlink = app.post(
            url = "https://$baseUrl/api",
            data = mapOf(
                "keys" to token
            ),
            referer = finallink,
            headers = mapOf(
                "x-token" to baseUrl
            )
        )
        val link =
            downloadlink.toString().substringAfter("url\":\"")
                .substringBefore("\",\"name")
                .replace("\\/", "/")
        return link
    }


    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = if(url.contains("r?key=")) {
            val temp = app.get(url).document.selectFirst("script")?.data()?.substringAfter("replace(\"")?.substringBefore("\")") ?: ""
            app.get(mainUrl + temp).document
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
                                this.quality = quality                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }
                text.contains("Instant Download") -> {
                    try{
                        val instant = instantLink(href)
                        callback.invoke(
                            newExtractorLink(
                                "$name Instant(Download)",
                                "$name[Instant(Download)] $fileName[$fileSize]",
                                instant,
                            ) {
                                this.quality = quality
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
                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }

                }
                text.contains("Direct Links") -> {
                    try {
                        val link = mainUrl + href
                        CFType1(link).forEach {
                            callback.invoke(
                                newExtractorLink(
                                    "$name CF Type1",
                                    "$name[CF Type1] $fileName[$fileSize]",
                                    it,
                                ) {
                                    this.quality = quality
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }
                text.contains("Resume Cloud") -> {
                    try {
                        val resumeCloud = resumeCloudLink(href)
                        callback.invoke(
                            newExtractorLink(
                                "$name ResumeCloud",
                                "$name[ResumeCloud] $fileName[$fileSize]",
                                resumeCloud,
                            ) {
                                this.quality = quality
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

class VCloud : ExtractorApi() {
    override val name: String = "V-Cloud"
    override val mainUrl: String = "https://vcloud.lol"
    override val requiresReferer = false

    fun getBaseUrl(url: String): String {
        return URI(url).let {
            "${it.scheme}://${it.host}"
        }
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        var href=url
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

            div?.select("h2 a.btn")?.amap {
                val link = it.attr("href")
                val text = it.text()
                if (text.contains("Download [FSL Server]"))
                {
                    callback.invoke(
                        newExtractorLink(
                            "$name[FSL Server]",
                            "$name[FSL Server] $header[$size]",
                            link,
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                else if (text.contains("Download [Server : 1]")) {
                    callback.invoke(
                        newExtractorLink(
                            "$name",
                            "$name $header[$size]",
                            link,
                        ) {
                            this.quality = getIndexQuality(header)
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
                                baseUrl+dlink,
                            ) {
                                this.quality = getIndexQuality(header)
                            }
                        )
                    }
                }

                else if (link.contains("pixeldra")) {
                    callback.invoke(
                        newExtractorLink(
                            "Pixeldrain",
                            "Pixeldrain $header[$size]",
                            link,
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                else if (text.contains("Download [Server : 10Gbps]")) {
                    val dlink = app.get(link, allowRedirects = false).headers["location"] ?: ""
                    callback.invoke(
                        newExtractorLink(
                            "$name[Download]",
                            "$name[Download] $header[$size]",
                            dlink.substringAfter("link="),
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                else
                {
                    loadExtractor(link,"",subtitleCallback, callback)
                }
            }
        }
    }
}

open class Hubdrive : ExtractorApi() {
    override val name = "Hubdrive"
    override val mainUrl = "https://hubdrive.wales"
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

class HubCloudInk : HubCloud() {
    override val mainUrl: String = "https://hubcloud.ink"
}

class HubCloudArt : HubCloud() {
    override val mainUrl: String = "https://hubcloud.art"
}

class HubCloudDad : HubCloud() {
    override val mainUrl: String = "https://hubcloud.dad"
}

class HubCloudBz : HubCloud() {
    override val mainUrl: String = "https://hubcloud.bz"
}

open class HubCloud : ExtractorApi() {
    override val name: String = "Hub-Cloud"
    override val mainUrl: String = "https://hubcloud.one"
    override val requiresReferer = false

    fun getBaseUrl(url: String): String {
        return URI(url).let {
            "${it.scheme}://${it.host}"
        }
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val newBaseUrl = "https://hubcloud.one"
        val newUrl = url.replace(mainUrl, newBaseUrl)
        val doc = app.get(newUrl).document
        var link = if(newUrl.contains("drive")) {
            val scriptTag = doc.selectFirst("script:containsData(url)")?.toString() ?: ""
            Regex("var url = '([^']*)'").find(scriptTag) ?. groupValues ?. get(1) ?: ""
        }
        else {
            doc.selectFirst("div.vd > center > a") ?. attr("href") ?: ""
        }

        if(!link.startsWith("https://")) {
            link = newBaseUrl + link
        }

        val document = app.get(link).document
        val div = document.selectFirst("div.card-body")
        val header = document.select("div.card-header").text() ?: ""
        val size = document.select("i#size").text() ?: ""

        div?.select("h2 a.btn")?.amap {
            val link = it.attr("href")
            val text = it.text()

            if (text.contains("Download [FSL Server]"))
            {
                callback.invoke(
                    newExtractorLink(
                        "$name[FSL Server]",
                        "$name[FSL Server] $header[$size]",
                        link,
                    ) {
                        this.quality = getIndexQuality(header)
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
                        this.quality = getIndexQuality(header)
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
                            baseUrl+dlink,
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
            }

            else if (link.contains("pixeldra")) {
                callback.invoke(
                    newExtractorLink(
                        "Pixeldrain",
                        "Pixeldrain $header[$size]",
                        link,
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            }
            else if (text.contains("Download [Server : 10Gbps]")) {
                val dlink = app.get(link, allowRedirects = false).headers["location"] ?: ""
                callback.invoke(
                    newExtractorLink(
                        "$name[Download]",
                        "$name[Download] $header[$size]",
                        dlink.substringAfter("link="),
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            }
            else
            {
                loadExtractor(link,"",subtitleCallback, callback)
            }
        }
    }
}

class fastdlserver2 : fastdlserver() {
    override var mainUrl = "https://fastdlserver.life"
}

open class fastdlserver : ExtractorApi() {
    override val name: String = "fastdlserver"
    override var mainUrl = "https://fastdlserver.lol"
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
    override var mainUrl = "https://gdlink.dev"
}

class GDFlix3 : GDFlix() {
    override var mainUrl = "https://new4.gdflix.dad"
}

class GDFlix2 : GDFlix() {
    override var mainUrl = "https://new.gdflix.dad"
}

class GDFlix7 : GDFlix() {
    override var mainUrl = "https://gdflix.dad"
}

class GDFlixXYZ : GDFlix() {
    override var mainUrl = "https://gdflix.xyz"
}

class GDFlixDev : GDFlix() {
    override var mainUrl = "https://gdflix.dev"
}

open class GDFlix : ExtractorApi() {
    override val name = "GDFlix"
    override val mainUrl = "https://new.gdflix.net"
    override val requiresReferer = false

    private suspend fun getLatestUrl(): String {
        val url = JSONObject(
            app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/urls.json").text
        ).optString("gdflix")
        if(url.isNullOrEmpty()) {
            return mainUrl
        }
        return url
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val latestUrl = getLatestUrl()
        val newUrl = url.replace(mainUrl, latestUrl)
        val document = app.get(newUrl).document
        val fileName = document.select("ul > li.list-group-item:contains(Name)").text()
            .substringAfter("Name : ").orEmpty()
        val fileSize = document.select("ul > li.list-group-item:contains(Size)").text()
            .substringAfter("Size : ").orEmpty()

        document.select("div.text-center a").amap { anchor ->
            val text = anchor.select("a").text()

            when {
                text.contains("DIRECT DL") -> {
                    val link = anchor.attr("href")
                    callback.invoke(
                        newExtractorLink("GDFlix[Direct]", "GDFlix[Direct] $fileName[$fileSize]", link) {
                            this.quality = getIndexQuality(fileName)
                        }
                    )
                }

                text.contains("CLOUD DOWNLOAD [R2]") -> {
                    val link = anchor.attr("href")
                    callback.invoke(
                        newExtractorLink("GDFlix[Cloud]", "GDFlix[Cloud] $fileName[$fileSize]", link) {
                            this.quality = getIndexQuality(fileName)
                        }
                    )
                }

                text.contains("PixelDrain DL") -> {
                    val link = anchor.attr("href")
                    callback.invoke(
                        newExtractorLink(
                            "Pixeldrain",
                            "Pixeldrain $fileName[$fileSize]",
                            link
                        ) {
                            this.quality = getIndexQuality(fileName)
                        }
                    )
                }

                text.contains("Index Links") -> {
                    try {
                        val link = anchor.attr("href")
                        app.get("$latestUrl$link").document
                            .select("a.btn.btn-outline-info").amap { btn ->
                                val serverUrl = latestUrl + btn.attr("href")
                                app.get(serverUrl).document
                                    .select("div.mb-4 > a").amap { sourceAnchor ->
                                        val source = sourceAnchor.attr("href")
                                        callback.invoke(
                                            newExtractorLink("GDFlix[Index]", "GDFlix[Index] $fileName[$fileSize]", source) {
                                                this.quality = getIndexQuality(fileName)
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
                        val driveLink = anchor.attr("href")
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
                                        this.quality = getIndexQuality(fileName)
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
                        val instantLink = anchor.attr("href")
                        val link = app.get(instantLink, allowRedirects = false)
                            .headers["location"]?.substringAfter("url=").orEmpty()

                        callback.invoke(
                            newExtractorLink("GDFlix[Instant Download]", "GDFlix[Instant Download] $fileName[$fileSize]", link) {
                                this.quality = getIndexQuality(fileName)
                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Instant DL", e.toString())
                    }
                }
                text.contains("GoFile") -> {
                    try {
                        app.get(anchor.attr("href")).document
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
            callback.invoke(
                newExtractorLink(
                    "Gofile",
                    "Gofile $fileName[$formattedSize]",
                    link,
                ) {
                    this.quality = getQuality(fileName)
                    this.headers = mapOf(
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

class FastLinks : ExtractorApi() {
    override val name: String = "FastLinks"
    override val mainUrl: String = "https://fastilinks.online"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url)
        val ssid = res.cookies["PHPSESSID"].toString()
        val cookies = mapOf("PHPSESSID" to "$ssid")
        val formBody = FormBody.Builder()
            .add("_csrf_token_645a83a41868941e4692aa31e7235f2", "3000f5248d9d207e4941e0aa053e1bcfd04dcbab")
            .build()

        val doc = app.post(
            url,
            requestBody = formBody,
            cookies = cookies
        ).document
        doc.select("div.well > a"). amap { link ->
            loadExtractor(link.attr("href"), subtitleCallback, callback)
        }
    }
}

class Photolinx : ExtractorApi() {
    override val name: String = "Photolinx"
    override val mainUrl: String = "https://photolinx.space"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val client = OkHttpClient()
        val res = app.get(url)
        val document = res.document
        val fileName = document.selectFirst("h1")?.text() ?: ""
        val cookies = res.cookies["PHPSESSID"].toString()
        val accessToken = document.select("#generate_url").attr("data-token")
        val uid = document.select("#generate_url").attr("data-uid")
        val body = """
            {
                "type": "DOWNLOAD_GENERATE",
                "payload": {
                    "access_token": "$accessToken",
                    "uid": "$uid"
                }
            }
        """.trimIndent()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val postRequest = Request.Builder()
            .url("$mainUrl/action")
            .addHeader("sec-fetch-site", "same-origin")
            .addHeader("x-requested-with", "xmlhttprequest")
            .addHeader("cookie", "PHPSESSID=$cookies")
            .addHeader("Referer", url)
            .addHeader("Referrer-Policy", "strict-origin-when-cross-origin")
            .post(body.toRequestBody(mediaType))
            .build()
        val photolinxRes2 = client.newCall(postRequest).execute()
        val photolinxData2 = photolinxRes2.body.string()
        val jsonResponse = JSONObject(photolinxData2)
        val dwUrl = jsonResponse.optString("download_url")
        callback.invoke(
            newExtractorLink(
                "${this.name}[Download]",
                "${this.name}[Download] - $fileName",
                dwUrl,
            )
        )
    }
}

class WLinkFast : ExtractorApi() {
    override val name: String = "WLinkFast"
    override val mainUrl: String = "https://wlinkfast.store"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).document
        val link = document.selectFirst("h1 > a")?.attr("href").toString()
        val doc = app.get(link).document
        var downloadLink = doc.selectFirst("a#downloadButton")?.attr("href").toString()

        if(downloadLink.isEmpty()) {
           downloadLink = Regex("""window\.location\.href\s*=\s*['"]([^'"]+)['"];""").find(doc.html())?.groupValues?.get(1).toString()
        }

        callback.invoke (
            newExtractorLink (
                this.name,
                this.name,
                downloadLink,
            )
        )
    }
}
