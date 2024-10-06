package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller

object CineStreamExtractors : CineStreamProvider() {
     suspend fun invokeRogmovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeWpredis(
            title,
            year,
            season,
            episode,
            subtitleCallback,
            callback,
            rogMoviesAPI
        )
    }
    suspend fun invokeVegamovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeWpredis(
            title,
            year,
            season,
            episode,
            subtitleCallback,
            callback,
            vegaMoviesAPI
        )
    }

    private suspend fun invokeWpredis(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val cfInterceptor = CloudflareKiller()
        val fixtitle = title?.substringBefore("-")?.substringBefore(":")?.replace("&", " ")
        val url = if (season == null) {
            "$api/search/$fixtitle $year"
        } else {
            "$api/search/$fixtitle season $season $year"
        }
        val domain = api.substringAfter("//").substringBefore(".")
        app.get(url, interceptor = cfInterceptor).document.select("#main-content article")
        .filter { element ->
            element.text().contains(
                fixtitle.toString(), true
            )
        }
        .amap {
            val hrefpattern =
                Regex("""(?i)<a\s+href="([^"]+)"[^>]*?>[^<]*?\b($fixtitle)\b[^<]*?""").find(
                    it.toString()
                )?.groupValues?.get(1)
            if (hrefpattern!=null) {
                val res = hrefpattern.let { app.get(it).document }
                val hTag = if (season == null) "h5" else "h3,h5"
                val aTag =
                    if (season == null) "Download Now" else "V-Cloud,Download Now,G-Direct"
                val sTag = if (season == null) "" else "(Season $season|S$seasonSlug)"
                val entries =
                    res.select("div.entry-content > $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                        .filter { element ->
                            !element.text().contains("Series", true) &&
                            !element.text().contains("Zip", true) &&
                            !element.text().contains("[Complete]", true) &&
                            !element.text().contains("480p, 720p, 1080p", true) &&
                            !element.text().contains(domain, true) &&
                            element.text().matches("(?i).*($sTag).*".toRegex())
                        }
                entries.amap { it ->
                    val tags =
                        """(?:480p|720p|1080p|2160p)(.*)""".toRegex().find(it.text())?.groupValues?.get(1)
                            ?.trim()
                    val tagList = aTag.split(",")
                    val href = it.nextElementSibling()?.select("a")?.find { anchor ->
                        tagList.any { tag ->
                            anchor.text().contains(tag.trim(), true)
                        }
                    }?.attr("href") ?: ""
                    val selector =
                        if (season == null) "p a:matches(V-Cloud|G-Direct)" else "h4:matches(0?$episode) ~ p a:matches(V-Cloud|G-Direct)"
                    if (href.isNotEmpty()) {
                        app.get(
                            href, interceptor = wpRedisInterceptor
                        ).document.select("div.entry-content > $selector").first()?.let { sources ->
                            val server = sources.attr("href")
                            loadCustomTagExtractor(
                                tags,
                                server,
                                "$api/",
                                subtitleCallback,
                                callback,
                                getIndexQuality(it.text())
                            )
                        }
                    }
                }
            }
        }
    }
}
