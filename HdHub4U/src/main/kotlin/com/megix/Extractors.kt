package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class Gamerxyt : ExtractorApi() {
    override val name: String = "Gamerxyt"
    override val mainUrl: String = "https://gamerxyt.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val host=url.substringAfter("?").substringBefore("&")
        val id=url.substringAfter("id=").substringBefore("&")
        val token=url.substringAfter("token=").substringBefore("&")
        val Cookie="$host; hostid=$id; hosttoken=$token"
        val document = app.get("$mainUrl/games/",headers = mapOf("Cookie" to Cookie)).document
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