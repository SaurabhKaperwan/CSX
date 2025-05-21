package com.megix

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink

class PixelDra : ExtractorApi() {
    override val name            = "PixelDra"
    override val mainUrl         = "https://pixeldra.in"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val mId = Regex("/u/(.*)").find(url)?.groupValues?.get(1)
        if (mId.isNullOrEmpty())
        {
            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    url = url
                ) {
                    this.referer = url
                    this.quality = Qualities.Unknown.value
                }
            )
        }
        else {
            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    url = "$mainUrl/api/file/${mId}?download"
                ) {
                    this.referer = url
                    this.quality = Qualities.Unknown.value
                }
            )
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
        val newUrl = url.replace("ink", "dad").replace("art", "dad")
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
        val header = document.select("div.card-header").text()
        div?.select("h2 a.btn")?.amap {
            val link = it.attr("href")
            val text = it.text()

            if (text.contains("Download [FSL Server]")) {
                callback.invoke(
                    newExtractorLink(
                        "$name[FSL Server]",
                        "$name[FSL Server] - $header",
                        url = link
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            } else if (text.contains("Download File")) {
                callback.invoke(
                    newExtractorLink(
                        name,
                        "$name - $header",
                        url = link
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            } else if (text.contains("BuzzServer")) {
                val dlink =
                    app.get("$link/download", allowRedirects = false).headers["location"] ?: ""
                callback.invoke(
                    newExtractorLink(
                        "$name[BuzzServer]",
                        "$name[BuzzServer] - $header",
                        url = link.substringBeforeLast("/") + dlink
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            } else if (link.contains("pixeldra")) {
                callback.invoke(
                    newExtractorLink(
                        "Pixeldra",
                        "Pixeldra - $header",
                        url = link
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            } else if (text.contains("Download [Server : 10Gbps]")) {
                val dlink = app.get(link, allowRedirects = false).headers["location"] ?: ""
                callback.invoke(
                    newExtractorLink(
                        "$name[Download]",
                        "$name[Download] - $header",
                        url = dlink.substringAfter("link=")
                    ) {
                        this.quality = getIndexQuality(header)
                    }
                )
            } else {
                loadExtractor(link, "", subtitleCallback, callback)
            }
        }
    }

    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
            ?: Qualities.Unknown.value
    }
}