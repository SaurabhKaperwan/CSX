package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URI

//Extractors
open class KMHD : ExtractorApi() {
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
        if(HubId != null) {
            val link = "https://hubcloud.day/drive/$HubId"
            loadExtractor(link, subtitleCallback, callback)
        }

        if(GDId != null) {
            val link = "https://new2.gdflix.cfd/file/$GDId"
            loadExtractor(link, subtitleCallback, callback)
        }

        if(KatId != null) {
            val link = "https://katdrive.in/file/$KatId"
            loadExtractor(link, subtitleCallback, callback)
        }

    }
}

open class KMHTFile : ExtractorApi() {
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

open class KatDrive : ExtractorApi() {
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
        var fileIdRegex = Regex("video/([^/]+)")
        var fileIdMatch = fileIdRegex.find(link)
        var fileId = fileIdMatch ?. groupValues ?. get(1) ?: ""
        var hubLink = "https://hubcloud.day/video/${fileId}"
        loadExtractor(hubLink, subtitleCallback, callback)
    }
}

open class KMHTNet : ExtractorApi() {
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

open class HubCloudLAT: ExtractorApi() {
    override val name: String = "HubCloudLAT"
    override val mainUrl: String = "https://hubcloud.lat"
    override val requiresReferer = false

    override suspend fun getUrl (
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    )
    {
        var link = url
        link = link.replace(".lat", ".ws")
        loadExtractor(link, subtitleCallback, callback)
    }
}

open class HubCloudWS: ExtractorApi() {
    override val name: String = "HubCloudWS"
    override val mainUrl: String = "https://hubcloud.ws"
    override val requiresReferer = false

    override suspend fun getUrl (
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
        val hubDocument = app.get(url, cookies = cookies).document
        val link = hubDocument.selectFirst("a.btn.btn-primary.btn-user.btn-success1.m-1") ?. attr("href") ?: "Empty"
        val newLink = link.replace(".lol", ".day")
        val hubDocument2 = app.get(newLink).document
        val lastLink = hubDocument2.selectFirst("div.vd > a") ?. attr("href") ?: "Empty"
        loadExtractor(lastLink, subtitleCallback, callback)
    }
}

open class HubCloud : ExtractorApi() {
    override val name: String = "HubCloud"
    override val mainUrl: String = "https://hubcloud.day"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url).document
        //for katdrive
        val vd = doc.selectFirst("div.vd > center > a")
        var urlValue = ""
        if(vd != null) {
            urlValue = vd.attr("href") ?: "" 
        }
        else {
            val scriptTag = doc.selectFirst("script:containsData(url)").toString()
            urlValue = Regex("var url = '([^']*)'").find(scriptTag) ?. groupValues ?. get(1) ?: ""
        }
        val document = app.get(urlValue).document
        val header = document.selectFirst("div.card-header")?.text()
        val size = document.selectFirst("i#size")?.text()
        val div = document.selectFirst("div.card-body")
        div.select("a").apmap {
            val link = it.attr("href")
            if (link.contains("pixeldra")){
                callback.invoke(
                    ExtractorLink(
                        "PixelDrain",
                        "PixelDrain $size",
                        link,
                        referer = link,
                        quality = getIndexQuality(header),
                    )
                )
            }
            else if (link.contains("dl.php")) {
                callback.invoke(
                    ExtractorLink(
                        "HubCloud[Download]",
                        "HubCloud[Download] $size",
                        link,
                        referer = "",
                        quality = getIndexQuality(header),
                    )
                )
            }
            else if(link.contains(".dev")){
                callback.invoke(
                    ExtractorLink(
                        "HubCloud",
                        "HubCloud $size",
                        link,
                        referer = "",
                        quality = getIndexQuality(header),
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

open class GDFlix : ExtractorApi() {
    override val name: String = "GDFlix"
    override val mainUrl: String = "https://new2.gdflix.cfd"
    override val requiresReferer = true

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
        app.get(url).document.select("div.text-center a").apmap {
            if (it.select("a").text().contains("FAST CLOUD DOWNLOAD"))
            {
                val link=it.attr("href")
                val trueurl=app.get("https://new2.gdflix.cfd$link").document.selectFirst("a.btn-success")?.attr("href") ?:""
                callback.invoke(
                    ExtractorLink(
                        "GDFlix[Fast Cloud]", "GDFLix[Fast Cloud] $tagquality", trueurl
                            ?: "", "", getQualityFromName(tags)
                    )
                )
            }
            else
            if (it.select("a").text().contains("Instant Download"))
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
                    )
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