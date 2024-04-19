package com.hexated

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName

open class Xtremestream : ExtractorApi() {
    override var name = "Xtremestream"
    override var mainUrl = "xtremestream.co"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?, callback: (ExtractorLink) -> Unit) {
        val response = app.get(
            url, referer = "https://${url.substringAfter("//").substringBefore("/")}/",
        )
        val playerScript =
            response.document.selectXpath("//script[contains(text(),'var video_id')]")
                .html()

        if (playerScript.isNotBlank()) {
            val videoId = playerScript.substringAfter("var video_id = `").substringBefore("`;")
            val m3u8LoaderUrl =
                playerScript.substringAfter("var m3u8_loader_url = `").substringBefore("`;")

            if (videoId.isNotBlank() && m3u8LoaderUrl.isNotBlank()) {
                callback.invoke(
                    ExtractorLink(
                        name,
                        "$m3u8LoaderUrl/$videoId",
                        "$m3u8LoaderUrl/$videoId",
                        Qualities.Unknown.value,
                        isM3u8 = true,
                    )
                )
            }
        }
    }
}

/*

 M3u8Helper.generateM3u8(
                    name,
                    "$m3u8LoaderUrl/$videoId",
                    "$m3u8LoaderUrl/$videoId"
                ).forEach { link ->
                    sources.add(link)
                }

*/
