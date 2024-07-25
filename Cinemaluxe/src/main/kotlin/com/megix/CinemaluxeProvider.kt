package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class CinemaluxeProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://cinemaluxe.world"
    override var name = "Cinemaluxe"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/genre/hollywood/page/" to "Hollywood Movies",
        "$mainUrl/genre/south-indian-movies/page/" to "South Indian Movies",
        "$mainUrl/genre/hollywood-tv-show/page/" to "Hollywood TV Shows",
        "$mainUrl/genre/bollywood-tv-show/page/" to "Bollywood TV Shows",
        "$mainUrl/genre/anime-tv-show/page/" to "Anime TV Shows",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("article.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("img") ?. attr("alt") ?: ""
        val href = this.selectFirst("a") ?. attr("href") ?: ""
        val posterUrl = this.selectFirst("img") ?. attr("data-src") ?: ""
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("div.result-item").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.sheader > div.data > h1") ?. text() ?: ""
        val posterUrl = document.selectFirst("meta[property=og:image]") ?. attr("content") ?: document.selectFirst("div.sheader noscript img") ?. attr("src")
        val description = document.selectFirst("div[itemprop=description]")?.text() ?: document.selectFirst("div.wp-content")?.text() ?: ""

        val tvType = if (url.contains("tvshows")) { 
            TvType.TvSeries
        } else {
            TvType.Movie
        }

        if(tvType == TvType.TvSeries) {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var seasonNum = 1
            val seasonList = mutableListOf<Pair<String, Int>>()
            var hTags = document.select("h3:matches((?i)(4K|[0-9]*0p))")

            hTags.mapNotNull{ hTag ->
                val seasonText = hTag.text() ?: "Unknown"
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(seasonText.toString()) ?. groupValues ?. get(1) ?: " Unknown"
                val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                val quality = qualityRegex.find(seasonText.toString()) ?. groupValues ?. get(1) ?: " Unknown"
                val sizeRegex = Regex("""\b\d+(?:\.\d+)? (?:MB|GB)\b""")
                val size = sizeRegex.find(seasonText.toString())?.value ?: ""
                seasonList.add("S$realSeason $quality $size" to seasonNum)
                val spanTag = hTag.nextElementSibling()
                val seasonLink = spanTag ?. selectFirst("a") ?. attr("href") ?: ""
                val doc = app.get(seasonLink).document
                var aTags = doc.select("a:matches((?i)(Episode))")
                val episodes = mutableListOf<Episode>()
                
                aTags.mapNotNull { aTag ->
                    val epText = aTag.text() ?: "Unknown"
                    val epLink = aTag.attr("href")
                    episodes.add(
                        Episode(
                            name = epText,
                            data = epLink,
                            season = seasonNum,
                            episode = aTags.indexOf(aTag) + 1
                        )
                    )
                }
                tvSeriesEpisodes.addAll(episodes)
                seasonNum++
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name)}
                this.plot = description
            }
        }
    
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if(data.contains("cinemaluxe") && !data.contains("sharepoint")) {
            val document = app.get(data).document
            val buttons = document.select("a.maxbutton")
            buttons.mapNotNull { button ->
                val link = button.attr("href")
                val doc = app.get(link).document
                doc.select("a.maxbutton").mapNotNull {
                    loadExtractor(it.attr("href"), subtitleCallback, callback)
                }

            }
        }
        else {
            loadExtractor(data, subtitleCallback, callback)
        }
        return true   
    }
}
