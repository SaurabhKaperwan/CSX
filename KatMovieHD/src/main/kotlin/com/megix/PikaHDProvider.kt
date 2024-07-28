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
        val title = document.selectFirst("meta[property=og:title]")?.attr("content") ?: "" 
        val posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))

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
            
            val hTags = document.select("h3:matches((?i)(E[0-9]+))")
            if(hTags.isNotEmpty()) {
                val episodesList = mutableListOf<Episode>()
                hTags.apmap { hTag ->
                    val hTagString = hTag.toString()
                    val details = hTag.text()
                    val episodes = newEpisode(hTagString){
                        name = details
                    }
                    episodesList.add(episodes)
                }
                tvSeriesEpisodes.addAll(episodesList)
                return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                    this.posterUrl = posterUrl
                }
            }
            else {
                val aTags = document.select("h2 > a:matches((?i)(4K|[0-9]*0p))")
                    .filter { element -> !element.text().contains("Pack", true) }
                var seasonNum = 1
                val seasonList = mutableListOf<Pair<String, Int>>()
                aTags.apmap {
                    val emText = it.selectFirst("em")?.text() ?: ""
                    val quality = Regex("(\\d{3,4})[pP]").find(emText ?: "") ?.groupValues ?.getOrNull(1) ?: "Unknown"
                    seasonList.add("$quality" to seasonNum)
                    val link = it.attr("href")
                    val episodeDocument = app.get(link).document
                    val kmhdPackRegex = Regex("""My_[a-zA-Z0-9]+""")
                    val kmhdLinks = kmhdPackRegex.findAll(episodeDocument.html()).mapNotNull { it.value }.toList()
                    val episodes = kmhdLinks.mapNotNull { kmhdLink ->
                        newEpisode("https://links.kmhd.net/file/$kmhdLink") {
                            name = "E${kmhdLinks.indexOf(kmhdLink) + 1} $quality"
                            season = seasonNum
                            episode = kmhdLinks.indexOf(kmhdLink) + 1
                        }
                    }
                    tvSeriesEpisodes.addAll(episodes)
                    seasonNum++
                }

                return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                    this.posterUrl = posterUrl
                    seasonNames = seasonList.map { (name, int) -> SeasonData(int, name) }
                }
            }
        }
        else {
            val hTags = document.select("h2:matches((?i)((1080p|720p|480p|2160p|4K|[0-9]*0p)))")
            var hTagString = ""
            hTags.apmap { hTag ->
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
        links.apmap {
            loadExtractor(it, subtitleCallback, callback)
        }
        return true       
    }
}