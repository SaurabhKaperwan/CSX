package com.hexated

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.fixUrl
import com.lagradost.cloudstream3.utils.getQualityFromName
import org.json.JSONObject

open class PorntrexExtractor : ExtractorApi() {
    override var name = "Porntrex"
    override var mainUrl = "porntrex.com"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val document = app.get(url).document

        val jsonObject = JSONObject(
            document.selectXpath("//script[contains(text(),'var flashvars')]").first()?.data()
                ?.substringAfter("var flashvars = ")
                ?.substringBefore("var player_obj")
                ?.replace(";", "") ?: ""
        )

        val sources = mutableListOf<ExtractorLink>()
        for (i in 0..6) {
            var videoUrl: String
            var quality: String
            if (i == 0) {
                videoUrl = jsonObject.optString("video_url") ?: ""
                quality = jsonObject.optString("video_url_text") ?: ""
            } else {
                if (i == 1) {
                    videoUrl = jsonObject.optString("video_alt_url") ?: ""
                    quality = jsonObject.optString("video_alt_url_text") ?: ""
                } else {
                    videoUrl = jsonObject.optString("video_alt_url${i}") ?: ""
                    quality = jsonObject.optString("video_alt_url${i}_text") ?: ""
                }
            }
            if (videoUrl == "") {
                continue
            }
            sources.add(
                ExtractorLink(
                    name,
                    name,
                    fixUrl(videoUrl),
                    referer = "https://www.porntrex.com/",
                    quality =
                    Regex("(\\d+.)").find(quality)?.groupValues?.get(1)
                        .let { getQualityFromName(it) }
                )
            )
        }
        return sources
    }
}