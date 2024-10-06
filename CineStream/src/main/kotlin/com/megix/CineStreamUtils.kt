package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

fun getEpisodeSlug(
    season: Int? = null,
    episode: Int? = null,
): Pair<String, String> {
    return if (season == null && episode == null) {
        "" to ""
    } else {
        (if (season!! < 10) "0$season" else "$season") to (if (episode!! < 10) "0$episode" else "$episode")
    }
}

fun getIndexQuality(str: String?): Int {
    return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
        ?: Qualities.Unknown.value
}

suspend fun loadCustomTagExtractor(
        tag: String? = null,
        url: String,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        quality: Int? = null,
) {
    loadExtractor(url, referer, subtitleCallback) { link ->
        callback.invoke(
            ExtractorLink(
                link.source,
                "${link.name} $tag",
                link.url,
                link.referer,
                when (link.type) {
                    ExtractorLinkType.M3U8 -> link.quality
                    else -> quality ?: link.quality
                },
                link.type,
                link.headers,
                link.extractorData
            )
        )
    }
}
