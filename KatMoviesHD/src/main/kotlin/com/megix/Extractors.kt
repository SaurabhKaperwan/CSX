package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URI

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
        if(HubId != null) {
            val link = "https://hubcloud.day/drive/$HubId"
            loadExtractor(link, subtitleCallback, callback)
        }

        if(GDId != null) {
            val link = "https://new2.gdflix.cfd/file/$HubId"
            loadExtractor(link, subtitleCallback, callback)
        }

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
        val hubDocument = app.get(url).document
        val gamerLink = hubDocument.selectFirst("div.vd > a")?.attr("href") ?: ""
        val host = gamerLink.substringAfter("?").substringBefore("&")
        val id = gamerLink.substringAfter("id=").substringBefore("&")
        val token = gamerLink.substringAfter("token=").substringBefore("&")
        val Cookie = "$host; hostid=$id; hosttoken=$token"
        val document = app.get("https://gamerxyt.com/games/",headers = mapOf("Cookie" to Cookie)).document
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
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
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
        app.get(url).document.select("div.text-center a").forEach {
            if (it.select("a").text().contains("FAST CLOUD DOWNLOAD"))
            {
                val link=it.attr("href")
                val trueurl=app.get("https://new2.gdflix.cfd$link").document.selectFirst("a.btn-success")?.attr("href") ?:""
                callback.invoke(
                    ExtractorLink(
                        "GDFlix", "GDFLix $tagquality", trueurl
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
                        "GDFlix",
                        "GDFlix $tagquality",
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