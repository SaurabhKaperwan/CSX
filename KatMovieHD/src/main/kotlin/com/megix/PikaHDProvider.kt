package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.network.CloudflareKiller


class PikaHDProvider : KatMovieHDProvider() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://pikahd.com"
    override var name = "PikaHD"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    private val cfInterceptor = CloudflareKiller()
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/%d/" to "Home",
        "$mainUrl/category/anime-hindi-dubbed/page/%d/" to "Hindi Dubbed",
        "$mainUrl/category/a-must-watch/page/%d/" to "A Must Watch",
        "$mainUrl/category/anime-dubbed/page/%d/" to "English Dubbed",
        "$mainUrl/category/complete-season/page/%d/" to "Complete Season",
    )

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]") ?. attr("content") ?: "" 
        val posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]") ?. attr("content"))

        val tvType = if (
            title.contains("Episode", ignoreCase = true) || 
            title.contains("season", ignoreCase = true) || 
            title.contains("series", ignoreCase = true)
        ) {
            TvType.TvSeries
        } else {
            TvType.Movie
        }

        if(tvType == TvType.TvSeries) {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            val episodesList = mutableListOf<Episode>()
            val hTags = document.select("h3:matches((?i)(E[0-9]+))")
            hTags.forEach { hTag ->
                val hTagString = hTag.toString()
                val details = hTag.text()
                val episodes = Episode(
                    name = details,
                    data = hTagString,
                )
                episodesList.add(episodes)
            }
            tvSeriesEpisodes.addAll(episodesList)
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
            }
        }
        else {
            val hTags = document.select("h2:matches((?i)((1080p|720p|480p|2160p|4K|[0-9]*0p)))")
            var hTagString = ""
            hTags.forEach { hTag ->
                hTagString = hTag.toString()
            }
            return newMovieLoadResponse(title, url, TvType.Movie, hTagString) {
                this.posterUrl = posterUrl
            }
        }   
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val regex = Regex("""<a href="([^"]+)">""")
        val links = regex.findAll(data).map { it.groupValues[1] }.toList()
        links.mapNotNull {
            loadExtractor(it, subtitleCallback, callback)
        }
        return true       
    }
}
