package com.megix

import android.annotation.TargetApi
import android.os.Build
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.amap
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.util.Base64

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

class DriveTotScanJs : ExtractorApi() {
    override val name: String = "DriveTotScanJs"
    override val mainUrl: String = "https://drivetot.top"
    override val requiresReferer = false

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val base64EncodedUrl = url.substringAfterLast("/")
        val decodedUrl = String(Base64.getDecoder().decode(base64EncodedUrl))
        loadExtractor(decodedUrl, referer, subtitleCallback, callback)
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
        val doc = app.get(url).document
        val link = if (url.contains("drivetot.top")) {
            val scriptTag = doc.selectFirst("script:containsData(url)")?.toString() ?: ""
            Regex("var url = '([^']*)'").find(scriptTag)?.groupValues?.get(1) ?: ""
        } else {
            doc.selectFirst("div.vd > center > a")?.attr("href") ?: ""
        }

        val document = app.get(link).document
        val div = document.selectFirst("div.card-body")
        val header = document.select("div.card-header").text()
        div?.select("h2 a.btn")?.amap {
            val extractedLink = it.attr("href")
            val text = it.text()

            when {
                text.contains("Download [FSL Server]") -> {
                    callback.invoke(
                        newExtractorLink(
                            "$name[FSL Server]",
                            "$name[FSL Server] - $header",
                            url = extractedLink
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                text.contains("Download File") -> {
                    callback.invoke(
                        newExtractorLink(
                            name,
                            "$name - $header",
                            url = extractedLink
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                text.contains("BuzzServer") -> {
                    val dlink = app.get("$extractedLink/download", allowRedirects = false).headers["location"] ?: ""
                    callback.invoke(
                        newExtractorLink(
                            "$name[BuzzServer]",
                            "$name[BuzzServer] - $header",
                            url = extractedLink.substringBeforeLast("/") + dlink
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                extractedLink.contains("pixeldra") -> {
                    callback.invoke(
                        newExtractorLink(
                            "Pixeldra",
                            "Pixeldra - $header",
                            url = extractedLink
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                text.contains("Download [Server : 10Gbps]") -> {
                    val dlink = app.get(extractedLink, allowRedirects = false).headers["location"] ?: ""
                    callback.invoke(
                        newExtractorLink(
                            "$name[Download]",
                            "$name[Download] - $header",
                            url = dlink.substringAfter("link=")
                        ) {
                            this.quality = getIndexQuality(header)
                        }
                    )
                }
                else -> {
                    loadExtractor(extractedLink, "", subtitleCallback, callback)
                }
            }
        }
    }

    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
            ?: Qualities.Unknown.value
    }
}
