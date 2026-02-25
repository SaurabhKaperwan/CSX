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
    override var mainUrl = "https://new15.gdflix.*"
}

open class GDFlix : ExtractorApi() {
    override val name = "GDFlix"
    override val mainUrl = "https://gdflix.*"
    override val requiresReferer = false

    private suspend fun CFType(url: String): List<String> {
        val types = listOf("1", "2")
        val downloadLinks = mutableListOf<String>()

        types.amap { t ->
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

        document.select("div.text-center a").amap { anchor ->
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
                    if(dlink == "") return@amap
                    myCallback(dlink, "[FAST CLOUD]")
                }

                link.contains("pixeldra") -> {
                    val baseUrlLink = getBaseUrl(link)
                    val finalURL = if (link.contains("download", true)) link
                    else "$baseUrlLink/api/file/${link.substringAfterLast("/")}?download"
                    myCallback(finalURL, "[Pixeldrain]")
                }

                text.contains("Instant DL") -> {
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
                            .select(".row .row a").amap { gofileAnchor ->
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

            sources.amap { source ->
                val redirectUrl = resolveFinalUrl(source) ?: return@amap
                myCallback(redirectUrl, "[CF]")
            }
        } catch (e: Exception) {
            Log.d("CF", e.toString())
        }
    }
}
