package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject
import okhttp3.FormBody
import okhttp3.*
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

open class Luxdrive : ExtractorApi() {
    override val name: String = "Luxdrive"
    override val mainUrl: String = "https://new.luxedrive.space"
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
            loadExtractor(href, "", subtitleCallback, callback)
        }
    }
}


class Driveseed : Driveleech() {
    override val name: String = "Driveseed"
    override val mainUrl: String = "https://driveseed.org"
}

open class Driveleech : ExtractorApi() {
    override val name: String = "Driveleech"
    override val mainUrl: String = "https://driveleech.org"
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
        val url = if(finallink.contains("video-leech")) "video-leech.xyz" else "video-seed.xyz"
        val token = finallink.substringAfter("url=")
        val downloadlink = app.post(
            url = "https://$url/api",
            data = mapOf(
                "keys" to token
            ),
            referer = finallink,
            headers = mapOf(
                "x-token" to url
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
        val quality = document.selectFirst("li.list-group-item")?.text() ?: ""
        val fileName = quality.replace("Name : ", "")

        document.select("div.text-center > a").amap { element ->
            val text = element.text()
            val href = element.attr("href")
            when {
                text.contains("Instant Download") -> {
                    try{
                        val instant = instantLink(href)
                        callback.invoke(
                            newExtractorLink(
                                "$name Instant(Download)",
                                "$name[Instant(Download)] $fileName",
                                instant,
                            ) {
                                this.quality = getIndexQuality(quality)
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
                                "$name[ResumeBot] $fileName",
                                resumeLink,
                            ) {
                                this.quality = getIndexQuality(quality)
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
                                    "$name[CF Type1] $fileName",
                                    it,
                                ) {
                                    this.quality = getIndexQuality(quality)
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
                                "$name[ResumeCloud] $fileName",
                                resumeCloud,
                            ) {
                                this.quality = getIndexQuality(quality)
                            }
                        )
                    } catch (e: Exception) {
                        Log.d("Error:", e.toString())
                    }
                }
                else -> {
                    loadExtractor(href, "", subtitleCallback, callback)
                }
            }
        }
    }
}

class VCloud : ExtractorApi() {
    override val name: String = "V-Cloud"
    override val mainUrl: String = "https://vcloud.lol"
    override val requiresReferer = false

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

            div?.select("h2 a.btn")?.amap {
                val link = it.attr("href")
                val text = it.text()
                if (text.contains("Download [FSL Server]"))
                {
                    callback.invoke(
                        newExtractorLink(
                            "$name[FSL Server]",
                            "$name[FSL Server] $header",
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
                            "$name $header",
                            link,
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                else if(text.contains("BuzzServer")) {
                    val dlink = app.get("$link/download", allowRedirects = false).headers["location"] ?: ""
                    callback.invoke(
                        newExtractorLink(
                            "$name[BuzzServer]",
                            "$name[BuzzServer] $header",
                            link.substringBeforeLast("/") + dlink,
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }

                else if (link.contains("pixeldra")) {
                    callback.invoke(
                        newExtractorLink(
                            "Pixeldrain",
                            "Pixeldrain $header",
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
                            "$name[Download] $header",
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

class HubCloudInk : HubCloud() {
    override val mainUrl: String = "https://hubcloud.ink"
}

class HubCloudArt : HubCloud() {
    override val mainUrl: String = "https://hubcloud.art"
}

class HubCloudDad : HubCloud() {
    override val mainUrl: String = "https://hubcloud.dad"
}

open class HubCloud : ExtractorApi() {
    override val name: String = "Hub-Cloud"
    override val mainUrl: String = "https://hubcloud.bz"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val newUrl = url.replace(mainUrl, "https://hubcloud.bz")
        val doc = app.get(newUrl).document
        val link = if(url.contains("drive")) {
            val scriptTag = doc.selectFirst("script:containsData(url)")?.toString() ?: ""
            Regex("var url = '([^']*)'").find(scriptTag) ?. groupValues ?. get(1) ?: ""
        }
        else {
            doc.selectFirst("div.vd > center > a") ?. attr("href") ?: ""
        }

        val document = app.get(link).document
        val div = document.selectFirst("div.card-body")
        val header = document.select("div.card-header").text() ?: ""

        div?.select("h2 a.btn")?.amap {
            val link = it.attr("href")
            val text = it.text()

            if (text.contains("Download [FSL Server]"))
            {
                callback.invoke(
                    newExtractorLink(
                        "$name[FSL Server]",
                        "$name[FSL Server] $header",
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
                        "$name $header",
                        link,
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            }
            else if(text.contains("BuzzServer")) {
                val dlink = app.get("$link/download", allowRedirects = false).headers["location"] ?: ""
                callback.invoke(
                    newExtractorLink(
                        "$name[BuzzServer]",
                        "$name[BuzzServer] $header",
                        link.substringBeforeLast("/") + dlink,
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            }

            else if (link.contains("pixeldra")) {
                callback.invoke(
                    newExtractorLink(
                        "Pixeldrain",
                        "Pixeldrain $header",
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
                        "$name[Download] $header",
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

class fastdlserver : ExtractorApi() {
    override val name: String = "fastdlserver"
    override var mainUrl = "https://fastdlserver.online"
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

class GDFlix4 : GDFlix() {
    override var mainUrl = "https://new2.gdflix.dad"
}

class GDFlix2 : GDFlix() {
    override var mainUrl = "https://new.gdflix.dad"
}

class GDFlix5 : GDFlix() {
    override var mainUrl = "https://new3.gdflix.dad"
}

open class GDFlix : ExtractorApi() {
    override val name: String = "GDFlix"
    override val mainUrl: String = "https://new5.gdflix.dad"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val newUrl = url.replace(mainUrl, "https://new5.gdflix.dad")
        val document = app.get(newUrl).document
        val fileName = document.selectFirst("ul > li.list-group-item")?.text()?.substringAfter("Name : ") ?: ""

        document.select("div.text-center a").amap {
            val text = it.select("a").text()
            // if (text.contains("FAST CLOUD"))
            // {
            //     val trueurl=app.get("https://new4.gdflix.dad$link").document.selectFirst("a.btn-success")?.attr("href") ?:""
            //     callback.invoke(
            //         ExtractorLink(
            //             "GDFlix[Fast Cloud]",
            //             "GDFLix[Fast Cloud] $fileName",
            //             trueurl,
            //             "",
            //             getIndexQuality(fileName)
            //         )
            //     )
            // }
            if(text.contains("DIRECT DL")) {
                val link = it.attr("href")
                callback.invoke(
                    newExtractorLink(
                        "GDFlix[Direct]",
                        "GDFLix[Direct] $fileName",
                        link,
                    ) {
                        this.quality = getIndexQuality(fileName)
                    }
                )
            }
            else if(text.contains("Index Links")) {
                try {
                    val link = it.attr("href")
                    app.get("https://new5.gdflix.dad$link")
                    .document.select("a.btn.btn-outline-info").amap {
                        val serverUrl = "https://new5.gdflix.dad" + it.attr("href")
                        app.get(serverUrl).document.select("div.mb-4 > a").amap {
                            val source = it.attr("href")
                            callback.invoke(
                                newExtractorLink(
                                    "GDFlix[Index]",
                                    "GDFLix[Index] $fileName",
                                    source,
                                ) {
                                    this.quality = getIndexQuality(fileName)
                                }
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.d("Index Links", e.toString())
                }
            }
            else if (text.contains("DRIVEBOT"))
            {
                try {
                    val driveLink = it.attr("href")
                    val id = driveLink.substringAfter("id=").substringBefore("&")
                    val doId = driveLink.substringAfter("do=").substringBefore("==")
                    val baseUrls = listOf("https://drivebot.sbs", "https://drivebot.cfd")
                    baseUrls.amap { baseUrl ->
                        val indexbotlink = "$baseUrl/download?id=$id&do=$doId"
                        val indexbotresponse = app.get(indexbotlink, timeout = 100L)
                        if(indexbotresponse.isSuccessful) {
                            val cookiesSSID = indexbotresponse.cookies["PHPSESSID"]
                            val indexbotDoc = indexbotresponse.document
                            val token = Regex("""formData\.append\('token', '([a-f0-9]+)'\)""").find(indexbotDoc.toString()) ?. groupValues ?. get(1) ?: ""
                            val postId = Regex("""fetch\('/download\?id=([a-zA-Z0-9/+]+)'""").find(indexbotDoc.toString()) ?. groupValues ?. get(1) ?: ""

                            val requestBody = FormBody.Builder()
                                .add("token", token)
                                .build()

                            val headers = mapOf(
                                "Referer" to indexbotlink
                            )

                            val cookies = mapOf(
                                "PHPSESSID" to "$cookiesSSID",
                            )

                            val response = app.post(
                                "$baseUrl/download?id=${postId}",
                                requestBody = requestBody,
                                headers = headers,
                                cookies = cookies,
                                timeout = 100L
                            ).text

                            var downloadlink = Regex("url\":\"(.*?)\"").find(response) ?. groupValues ?. get(1) ?: ""

                            downloadlink = downloadlink.replace("\\", "")

                            callback.invoke(
                                newExtractorLink(
                                    "GDFlix[DriveBot]",
                                    "GDFlix[DriveBot] $fileName",
                                    downloadlink,
                                ) {
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
            else if (text.contains("Instant DL"))
            {
                try {
                    val instantLink = it.attr("href")
                    val link = app.get(instantLink, timeout = 30L, allowRedirects = false).headers["location"]?.substringAfter("url=") ?: ""
                    callback.invoke(
                        newExtractorLink(
                            "GDFlix[Instant Download]",
                            "GDFlix[Instant Download] $fileName",
                            link,
                        ) {
                            this.quality = getIndexQuality(fileName)
                        }
                    )
                } catch (e: Exception) {
                    Log.d("Instant DL", e.toString())
                }
            }
            else if(text.contains("CLOUD DOWNLOAD")) {
                callback.invoke(
                    newExtractorLink(
                        "GDFlix[CLOUD DOWNLOAD]",
                        "GDFlix[CLOUD DOWNLOAD] $fileName",
                        it.attr("href"),
                    ) {
                        this.quality = getIndexQuality(fileName)
                    }
                )
            }
            else if(text.contains("GoFile")) {
                try {
                    app.get(it.attr("href")).document.select(".row .row a").amap {
                        val link = it.attr("href")
                        if(link.contains("gofile")) {
                            Gofile().getUrl(link, "", subtitleCallback, callback)
                        }
                    }
                } catch (e: Exception) {
                    Log.d("Gofile", e.toString())
                }
            }
            else {
                Log.d("Error", "No Server matched")
            }
        }
    }
}

open class Gofile : ExtractorApi() {
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

        val res = app.get(url)
        val id = Regex("/(?:\\?c=|d/)([\\da-zA-Z-]+)").find(url)?.groupValues?.get(1) ?: return
        val genAccountRes = app.post("$mainApi/accounts").text
        val jsonResp = JSONObject(genAccountRes)
        val token = jsonResp.getJSONObject("data").getString("token") ?: return

        val globalRes = app.get("$mainUrl/dist/js/global.js").text
        val wt = Regex("""appdata\.wt\s*=\s*[\"']([^\"']+)[\"']""").find(globalRes)?.groupValues?.get(1) ?: return

        val response = app.get("$mainApi/contents/$id?wt=$wt",
            headers = mapOf(
                "Authorization" to "Bearer $token",
            )
        ).text

        val jsonResponse = JSONObject(response)
        val data = jsonResponse.getJSONObject("data")
        val children = data.getJSONObject("children")
        val oId = children.keys().next()
        val link = children.getJSONObject(oId).getString("link")
        val fileName = children.getJSONObject(oId).getString("name")
        if(link != null && fileName != null) {
            callback.invoke(
                newExtractorLink(
                    "Gofile",
                    "Gofile $fileName",
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
    override val mainUrl: String = "https://fastilinks.fun"
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
            .add("_csrf_token_645a83a41868941e4692aa31e7235f2", "8afaabe2fa563a3cd17780e9b832ba4fdc778a9e")
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
    override val mainUrl: String = "https://photolinx.shop"
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
            .url("https://photolinx.shop/action")
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

class Sendcm : ExtractorApi() {
    override val name: String = "Sendcm"
    override val mainUrl: String = "https://send.cm"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    )
    {
        val doc = app.get(url).document
        val op = doc.select("input[name=op]").attr("value").toString()
        val id = doc.select("input[name=id]").attr("value").toString()
        val body = FormBody.Builder()
            .addEncoded("op", op)
            .addEncoded("id", id)
            .build()
        val response = app.post(
            mainUrl,
            requestBody = body,
            allowRedirects = false
        )

        val locationHeader = response.headers["location"].toString()

        if(locationHeader.contains("watch")) {
            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    locationHeader,
                ) {
                    this.referer = mainUrl
                    this.quality = Qualities.Unknown.value
                }
            )
        }
    }
}
