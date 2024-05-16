package com.Xtapes

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName


class Stream : Filesim() {
    override var mainUrl = "https://55k.io"
}


open class VID : ExtractorApi() {
    override var name = "VID Xtapes"
    override var mainUrl = "https://vid.xtapes.to"
    override val requiresReferer = false

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val response = app.get(url).document.toString()
        val link =response.substringAfter("src: '").substringBefore("',")
        return listOf(
            ExtractorLink(
                this.name,
                this.name,
                link,
                referer ?: "",
                Qualities.Unknown.value,
                type = INFER_TYPE
            )
        )
    }
}