package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

open class VCloud : ExtractorApi() {
    override val name: String = "V-Cloud"
    override val mainUrl: String = "https://vcloud.lol"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url)
        val doc = res.document
        val changedLink = doc.selectFirst("script:containsData(url =)")?.data()?.let {
            val regex = """url\s*=\s*['"](.*)['"];""".toRegex()
            val doc2 = app.get(regex.find(it)?.groupValues?.get(1) ?: return).text
            regex.find(doc2)?.groupValues?.get(1)?.substringAfter("r=")
        }
        val header = doc.selectFirst("div.card-header")?.text()
        app.get(
            base64Decode(changedLink ?: return), cookies = res.cookies, headers = mapOf(
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
            )
        ).document.select("p.text-success ~ a").apmap {
            val link = it.attr("href")
            if ( link.contains("workers.dev") || link.contains("cloudflare") || link.contains("pixeldrain")
                || link.contains("dl.php") || it.text().contains("Download")
             ) {
                callback.invoke(
                    ExtractorLink(
                        this.name,
                        this.name,
                        link,
                        "",
                        getIndexQuality(header),
                    )
                )
            }
        }

    }

    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }

}
