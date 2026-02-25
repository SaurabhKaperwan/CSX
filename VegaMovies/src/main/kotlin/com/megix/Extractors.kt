package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URI
import com.lagradost.api.Log

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

fun getBaseUrl(url: String): String {
    try {
        return URI(url).let {
            "${it.scheme}://${it.host}"
        }
    } catch (e: Exception) {
        return url
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

suspend fun getLatestBaseUrl(baseUrl: String, source: String): String {
    return try {
        val dynamicUrls = app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/urls.json")
            .parsedSafe<Map<String, String>>()
        dynamicUrls?.get(source)?.takeIf { it.isNotBlank() } ?: baseUrl
    } catch (e: Exception) {
        baseUrl
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

        document.select("h2 a.btn").amap {
            val link = it.attr("href")
            val text = it.text()

            if (text.contains("FSL Server")) myCallback(link, "[FSL Server]")
            else if (text.contains("FSLv2")) myCallback(link, "[FSLv2 Server]")
            else if (text.contains("Mega Server")) myCallback(link, "[Mega Server]")
            else if (text.contains("Download File")) myCallback(link)
            else if (text.contains("BuzzServer")) {
                val dlink = app.get("$link/download", referer = link, allowRedirects = false).headers["hx-redirect"] ?: ""
                val baseUrl = getBaseUrl(link)
                if(dlink != "") myCallback( baseUrl + dlink, "[BuzzServer]")
            }
            else if (link.contains("pixeldra")) {
                val baseUrlLink = getBaseUrl(link)
                val finalURL = if (link.contains("download", true)) link
                else "$baseUrlLink/api/file/${link.substringAfterLast("/")}?download"
                myCallback(finalURL, "[Pixeldrain]")
            }
            else if (text.contains("Server : 10Gbps")) {
                var redirectUrl = resolveFinalUrl(link) ?: return@amap
                if(redirectUrl.contains("link=")) redirectUrl = redirectUrl.substringAfter("link=")
                myCallback(redirectUrl, "[Download]")
            }
            else { Log.d("Error", "No Server matched") }
        }
    }
}
