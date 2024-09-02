package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.net.URI

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

class WLinkFast : ExtractorApi() {
    override val name: String = "WLinkFast"
    override val mainUrl: String = "https://wlinkfast.store"
    override val requiresReferer = false
    private val client = OkHttpClient()

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url)
        val document = res.document
        val ssid = res.cookies["PHPSESSID"].toString()
        val cookies = mapOf("PHPSESSID" to "$ssid")
        val regex = Regex("""formData\.append\('key', '(\d+)'\);""")
        val key = regex.find(document.html()) ?. groupValues ?. get(1) ?: ""

        val quality = document.selectFirst("title") ?. text()
        
        val formBody = FormBody.Builder()
            .add("key", "$key")
            .build()

        val response = app.post(
            url,
            requestBody = formBody,
            cookies = cookies
        )
        
        val jsonResponse = response.text
        val jsonObject = JSONObject(jsonResponse)
        val link = jsonObject.getString("download")
        val requireRepairRequest = Request.Builder()
            .url(link)
            .head()
            .build()

        val requireRepairResponse: Response = client.newCall(requireRepairRequest).execute()
        val contentType = requireRepairResponse.header("Content-Type").toString()
        
        if(contentType.contains("video")) {
            callback.invoke (
                ExtractorLink (
                    this.name,
                    this.name,
                    link,
                    referer = "",
                    quality = getIndexQuality(quality),
                )
            )
        }
        else {
            val reResponse = app.get(link).document
            val reLink = "https://www.mediafire.com" + reResponse.selectFirst("a#continue-btn")?.attr("href").toString()
            val doc = app.get(reLink).document
            val downloadLink = doc.selectFirst("a.input.popsok")?.attr("href").toString()
            callback.invoke (
                ExtractorLink (
                    this.name,
                    this.name,
                    downloadLink,
                    referer = "",
                    quality = getIndexQuality(quality),
                )
            )
        }
    }
    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
            ?: Qualities.Unknown.value
    }
}

class GDFlix1 : GDFlix() {
    override val mainUrl: String = "https://new3.gdflix.cfd"
}

class GDFlix2 : GDFlix() {
    override val mainUrl: String = "https://new2.gdflix.cfd"
}


open class GDFlix : ExtractorApi() {
    override val name: String = "GDFlix"
    override val mainUrl: String = "https://new4.gdflix.cfd"
    override val requiresReferer = false

    private suspend fun extractbollytag(url:String): String {
        val tagdoc= app.get(url).text
        val tags ="""\b\d{3,4}p\b""".toRegex().find(tagdoc) ?. value ?. trim() ?:""
        return tags
    }

    private suspend fun extractbollytag2(url:String): String {
        val tagdoc= app.get(url).text
        val tags ="""\b\d{3,4}p\b\s(.*?)\[""".toRegex().find(tagdoc) ?. groupValues ?. get(1) ?. trim() ?:""
        return tags
    }

    private fun getBaseUrl(url: String): String {
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
        var originalUrl = url
        val tags = extractbollytag(originalUrl)
        val tagquality = extractbollytag2(originalUrl)

        if (originalUrl.startsWith("https://new2.gdflix.cfd/goto/token/")) {
            val partialurl = app.get(originalUrl).text.substringAfter("replace(\"").substringBefore("\")")
            originalUrl = mainUrl + partialurl
        }
        app.get(originalUrl).document.select("div.text-center a").amap {
            if (it.select("a").text().contains("FAST CLOUD DL"))
            {
                val link=it.attr("href")
                val trueurl=app.get("https://new2.gdflix.cfd$link", timeout = 30L).document.selectFirst("a.btn-success")?.attr("href") ?:""
                callback.invoke(
                    ExtractorLink(
                        "GDFlix[Fast Cloud]",
                        "GDFLix[Fast Cloud] $tagquality",
                        trueurl,
                        "",
                        getQualityFromName(tags)
                    )
                )
            }
            else if (it.select("a").text().contains("DRIVEBOT LINK"))
            {
                val driveLink = it.attr("href")
                val id = driveLink.substringAfter("id=").substringBefore("&")
                val doId = driveLink.substringAfter("do=").substringBefore("==")
                val indexbotlink = "https://indexbot.lol/download?id=${id}&do=${doId}"
                val indexbotresponse = app.get(indexbotlink, timeout = 30L)
                if(indexbotresponse.isSuccessful) {
                    val cookiesSSID = indexbotresponse.cookies["PHPSESSID"]
                    val indexbotDoc = indexbotresponse.document
                    val token = Regex("""formData\.append\('token', '([a-f0-9]+)'\)""").find(indexbotDoc.toString()) ?. groupValues ?. get(1) ?: "token"
                    val postId = Regex("""fetch\('\/download\?id=([a-zA-Z0-9\/+]+)'""").find(indexbotDoc.toString()) ?. groupValues ?. get(1) ?: "postId"

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
                        "https://indexbot.lol/download?id=${postId}",
                        requestBody = requestBody,
                        headers = headers,
                        cookies = cookies,
                        timeout = 30L
                    ).toString()

                    var downloadlink = Regex("url\":\"(.*?)\"").find(response) ?. groupValues ?. get(1) ?: ""

                    downloadlink = downloadlink.replace("\\", "")

                    callback.invoke(
                        ExtractorLink(
                            "GDFlix[IndexBot]",
                            "GDFlix[IndexBot] $tagquality",
                            downloadlink,
                            "https://indexbot.lol/",
                            getQualityFromName(tags)
                        )
                    )
                }
            }
            else if (it.select("a").text().contains("Instant DL"))
            {
                val Instant_link=it.attr("href")
                val token = Instant_link.substringAfter("url=")
                val domain= getBaseUrl(Instant_link)
                val downloadlink = app.post(
                    url = "$domain/api",
                    data = mapOf(
                        "keys" to token
                    ),
                    referer = Instant_link,
                    headers = mapOf(
                        "x-token" to "direct.zencloud.lol",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"
                    ),
                    timeout = 30L,
                )
                val finaldownloadlink =
                    downloadlink.toString().substringAfter("url\":\"")
                        .substringBefore("\",\"name")
                        .replace("\\/", "/")
                val link = finaldownloadlink
                callback.invoke(
                    ExtractorLink(
                        "GDFlix[Instant Download]",
                        "GDFlix[Instant Download] $tagquality",
                        url = link,
                        "",
                        getQualityFromName(tags)
                    )
                )
            }
            else
            {

            }
        }
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
                ExtractorLink(
                    this.name,
                    this.name,
                    locationHeader,
                    referer = "https://send.cm/",
                    quality = Qualities.Unknown.value,
                )
            )
        }
    }
}
