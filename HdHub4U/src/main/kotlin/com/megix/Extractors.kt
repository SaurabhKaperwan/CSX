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
        val doc = app.get("$mainUrl/games/",headers = mapOf("Cookie" to Cookie)).document
        val links = doc.select("div.card-body > h2 > a").attr("href")
        val header = doc.selectFirst("div.card-header")?.text()
        if (links.contains("pixeldrain"))
        {
            callback.invoke(
                ExtractorLink(
                    "Gamerxyt",
                    "PixelDrain",
                    links,
                    referer = links,
                    quality = getIndexQuality(header),
                )
            )
        }else if (links.contains("gofile")) {
            loadExtractor(links, subtitleCallback, callback)
        }
        else {
            callback.invoke(
                ExtractorLink(
                    "Gamerxyt",
                    "HubCloud",
                    links,
                    referer = "",
                    quality = getIndexQuality(header),
                )
            )
        }
    }
    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }
}