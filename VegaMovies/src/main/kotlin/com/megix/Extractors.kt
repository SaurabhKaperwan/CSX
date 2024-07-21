package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class VCloud : ExtractorApi() {
    override val name: String = "V-Cloud"
    override val mainUrl: String = "https://vcloud.lol"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url).document
        val scriptTag = doc.selectFirst("script:containsData(url)").toString()
        val urlValue = Regex("var url = '([^']*)'").find(scriptTag) ?. groupValues ?. get(1) ?: ""
        val document = app.get(urlValue).document

        val size = document.selectFirst("i#size") ?. text()
        val div = document.selectFirst("div.card-body")
        val header = document.selectFirst("div.card-header") ?. text()
        div.select("a").apmap {
            val link = it.attr("href")
            if (link.contains("pixeldra")) {
                callback.invoke(
                    ExtractorLink(
                        "Pixeldrain",
                        "Pixeldrain $size",
                        link,
                        "",
                        getIndexQuality(header),
                    )
                )
            }
            else if(link.contains("dl.php")) {
                val response = app.get(link, allowRedirects = false)
                val downloadLink = response.headers["location"].toString().split("link=").getOrNull(1) ?: link
                callback.invoke(
                    ExtractorLink(
                        "V-Cloud[Download]",
                        "V-Cloud[Download] $size",
                        downloadLink,
                        "",
                        getIndexQuality(header),
                    )
                )
            }
            else if(link.contains(".dev")) {
                callback.invoke(
                    ExtractorLink(
                        "V-Cloud",
                        "V-Cloud $size",
                        link,
                        "",
                        getIndexQuality(header),
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


open class FastDL : ExtractorApi() {
    override val name: String = "FastDl"
    override val mainUrl: String = "https://fastdl.icu"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val cookiesSSID = app.get(url).cookies["PHPSESSID"]
        val cookies = mapOf(
            "PHPSESSID" to "$cookiesSSID"
        )
        val document = app.get(url, cookies = cookies).document
        val link = document.selectFirst("a#vd") ?. attr("href")
        if(link != null) {
            callback.invoke(ExtractorLink("FastDL", "FastDL[Download]", link, "", Qualities.Unknown.value))
        }
    }
}
