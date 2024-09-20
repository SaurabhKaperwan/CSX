package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

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
                    Qualities.Unknown.value,
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
                    Qualities.Unknown.value,
                )
            )
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
