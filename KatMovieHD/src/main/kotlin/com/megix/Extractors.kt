package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URI
import okhttp3.FormBody

//Extractors
class KMHD : ExtractorApi() {
    override val name: String = "KMHD"
    override val mainUrl: String = "https://links.kmhd.net/file"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) 
    {
        val document = app.get(url).document
        val HubId = Regex("""hubdrive_res:"([^"]+)""").find(document.html()) ?. groupValues ?. get(1)
        val GDId = Regex("""gdflix_res:"([^"]+)""").find(document.html()) ?. groupValues ?. get(1)
        val KatId = Regex("""katdrive_res:"([^"]+)""").find(document.html()) ?. groupValues ?. get(1)
        val SendcmId = Regex("""sendcm_res:"([^"]+)""").find(document.html()) ?. groupValues ?. get(1)

        if(HubId != "None") {
            val link = "https://hubcloud.club/drive/$HubId"
            loadExtractor(link, subtitleCallback, callback)
        }

        if(GDId != "None") {
            val link = "https://new2.gdflix.cfd/file/$GDId"
            loadExtractor(link, subtitleCallback, callback)
        }

        if(KatId != "None") {
            val link = "https://katdrive.in/file/$KatId"
            loadExtractor(link, subtitleCallback, callback)
        }

        if(SendcmId != "None") {
            val link = "https://send.cm/$SendcmId"
            loadExtractor(link, subtitleCallback, callback)
        }
    }
}

class KMHTFile : ExtractorApi() {
    override val name: String = "KMHTFile"
    override val mainUrl: String = "https://gd.kmhd.net/file/"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    )
    {
        var link = url
        link = link.replace("https://gd.kmhd.net/file/", "https://new2.gdflix.cfd/file/")
        loadExtractor(link, subtitleCallback, callback)
    }
}

class KatDrive : ExtractorApi() {
    override val name: String = "KatDrive"
    override val mainUrl: String = "https://katdrive.in"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) 
    {
        val cookiesSSID = app.get(url).cookies["PHPSESSID"]
        val cookies = mapOf(
            "PHPSESSID" to "$cookiesSSID"
        )
        val document = app.get(url, cookies = cookies).document
        val link = document.selectFirst("h5 > a") ?. attr("href").toString()
        var fileIdRegex = Regex("""video\/([^"]+)"[^>]*>""")
        var fileIdMatch = fileIdRegex.find(link)
        var fileId = fileIdMatch ?. groupValues ?. get(1) ?: ""
        var hubLink = "https://hubcloud.club/video/${fileId}"
        loadExtractor(hubLink, subtitleCallback, callback)
    }
}

class KMHTNet : ExtractorApi() {
    override val name: String = "KMHTNet"
    override val mainUrl: String = "https://kmhd.net/archives/"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) 
    {
        val document = app.get(url).document
        document.select("h3 > a").apmap {
            val link = it.attr("href")
            loadExtractor(link, subtitleCallback, callback)
        }
    }
}

class HubCloud : ExtractorApi() {
    override val name: String = "Hub-Cloud"
    override val mainUrl: String = "https://hubcloud.club"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url).document
        var gamerLink = ""
        if(url.contains("drive")) {
            val scriptTag = doc.selectFirst("script:containsData(url)").toString()
            gamerLink = Regex("var url = '([^']*)'").find(scriptTag) ?. groupValues ?. get(1) ?: ""
        }
        else {
            gamerLink = doc.selectFirst("div.vd > center > a") ?. attr("href") ?: ""
        }
        
        val document = app.get(gamerLink).document

        val size = document.selectFirst("i#size") ?. text()
        val div = document.selectFirst("div.card-body")
        val header = document.selectFirst("div.card-header") ?. text()
        div.select("a").amap {
            val link = it.attr("href")
            if (link.contains("pixeldra")) {
                callback.invoke(
                    ExtractorLink(
                        "Pixeldrain",
                        "Pixeldrain $size",
                        link,
                        "",
                        getIndexQuality(header),
                    )
                )
            }
            else if(link.contains("dl.php")) {
                val response = app.get(link, allowRedirects = false)
                val downloadLink = response.headers["location"].toString().split("link=").getOrNull(1) ?: link
                callback.invoke(
                    ExtractorLink(
                        "Hub-Cloud[Download]",
                        "Hub-Cloud[Download] $size",
                        downloadLink,
                        "",
                        getIndexQuality(header),
                    )
                )
            }
            else if(link.contains(".dev")) {
                callback.invoke(
                    ExtractorLink(
                        "Hub-Cloud",
                        "Hub-Cloud $size",
                        link,
                        "",
                        getIndexQuality(header),
                    )
                )
            }
            else {
                loadExtractor(link, subtitleCallback, callback)
            }
        }
    }


    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
            ?: Qualities.Unknown.value
    }

}

class GDFlix : ExtractorApi() {
    override val name: String = "GDFlix"
    override val mainUrl: String = "https://new2.gdflix.cfd"
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
        var url = url
        val tags = extractbollytag(url)
        val tagquality = extractbollytag2(url)
        if (url.startsWith("https://new2.gdflix.cfd/goto/token/"))
        {
            val partialurl = app.get(url).text.substringAfter("replace(\"").substringBefore("\")")
            url = mainUrl + partialurl
        }
        else
        {
            url = url
        }
        app.get(url).document.select("div.text-center a").amap {
            if (it.select("a").text().contains("FAST CLOUD DOWNLOAD"))
            {
                val link=it.attr("href")
                val trueurl=app.get("https://new2.gdflix.cfd$link", timeout = 40L).document.selectFirst("a.btn-success")?.attr("href") ?:""
                callback.invoke(
                    ExtractorLink(
                        "GDFlix[Fast Cloud]", "GDFLix[Fast Cloud] $tagquality", trueurl
                            ?: "", "", getQualityFromName(tags)
                    )
                )
            }
            else if (it.select("a").text().contains("DRIVEBOT DOWNLOAD"))
            {
                val driveLink = it.attr("href")
                val id = driveLink.substringAfter("id=").substringBefore("&")
                val doId = driveLink.substringAfter("do=").substringBefore("==")
                val indexbotlink = "https://indexbot.lol/download?id=${id}&do=${doId}"
                val indexbotresponse = app.get(indexbotlink, timeout = 60L)
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
                        timeout = 60L
                    ).toString()
                
                    var downloadlink = Regex("url\":\"(.*?)\"").find(response) ?. groupValues ?. get(1) ?: ""    

                    downloadlink = downloadlink.replace("\\", "")                
                
                    callback.invoke(
                        ExtractorLink(
                            "IndexBot", 
                            "IndexBot $tagquality", 
                            downloadlink, 
                            "https://indexbot.lol/",
                            getQualityFromName(tags)
                        )
                    )
                }
            }
            else if (it.select("a").text().contains("Instant Download"))
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
                    timeout = 60L,
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
                val link=it.attr("href")
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
        val op = doc.select("input[name=op]") ?. attr("value").toString()
        val id = doc.select("input[name=id]") ?. attr("value").toString()
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
