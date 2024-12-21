package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

class CinemaluxeProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://cinemaluxe.fans"
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

    private suspend fun bypass(url: String): String {
        val document = app.get(url).document.toString()
        val encodeUrl = Regex("""link":"([^"]+)""").find(document) ?. groupValues ?. get(1) ?: ""
        return base64Decode(encodeUrl)
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

        for (i in 1..6) {
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
        val title = document.selectFirst("div.sheader > div.data > h1")?.text().toString()
        var posterUrl = document.selectFirst("div.sheader noscript img")?.attr("src")
        if (posterUrl.isNullOrEmpty()) {
            posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content")
        }
        var description = document.selectFirst("div[itemprop=description]")?.ownText()
        if(description.isNullOrEmpty()) {
            description = document.selectFirst("div.wp-content")?.text()
        }

        val tvType = if (url.contains("tvshow")) {
            "series"
        } else {
            "movie"
        }

        if(tvType == "series") {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var hTags = document.select("h3:matches((?i)(4K|[0-9]*0p))")
            if(hTags.isEmpty()) {
                hTags = document.select("a.maxbutton-5")
            }
            val episodesMap: MutableMap<Pair<Int, Int>, List<String>> = mutableMapOf()

            hTags.mapNotNull{ hTag ->
                val seasonText = hTag.text()
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val matchResult = realSeasonRegex.find(seasonText.toString())
                val realSeason = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
                var seasonLink =  if(hTag.tagName() == "a") {
                    hTag.attr("href")
                }
                else {
                    val spanTag = hTag.nextElementSibling()
                    spanTag ?.selectFirst("a")?.attr("href").toString()
                }
                if(seasonLink.contains("luxedailyupdates.xyz")) {
                    seasonLink = bypass(seasonLink)
                }
                val doc = app.get(seasonLink).document
                var aTags = doc.select("a.maxbutton:matches((?i)(Episode))")
                
                aTags.mapNotNull { aTag ->
                    val epText = aTag.text()
                    val e = Regex("""(?i)(?:episode\s*[-]?\s*)(\d{1,2})""").find(epText)?.groups?.get(1)?.value ?.toIntOrNull() ?: 0
                    val epUrl = aTag.attr("href")
                    val key = Pair(realSeason, e)
                    if (episodesMap.containsKey(key)) {
                        val currentList = episodesMap[key] ?: emptyList()
                        val newList = currentList.toMutableList()
                        newList.add(epUrl)
                        episodesMap[key] = newList
                    } else {
                        episodesMap[key] = mutableListOf(epUrl)
                    }
                }
            }

            for ((key, value) in episodesMap) {
                val data = value.map { source->
                    EpisodeLink(
                        source
                    )
                }
                tvSeriesEpisodes.add(
                    newEpisode(data) {
                        this.season = key.first
                        this.episode = key.second
                    }
                )
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }
        else {
            val buttons = document.select("a.maxbutton")
            val data = buttons.flatMap { button ->
                var link = button.attr("href")
                if(link.contains("luxedailyupdates.xyz")) {
                    link = bypass(link)
                }
                val doc = app.get(link).document
                doc.select("a.maxbutton").mapNotNull {
                    val source = it.attr("href")
                    EpisodeLink(
                        source
                    )
                }
            }
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val sources = parseJson<ArrayList<EpisodeLink>>(data)
        sources.amap {
            val source = it.source
            loadExtractor(source, subtitleCallback, callback)
        }
        return true   
    }

    data class EpisodeLink(
        val source: String
    )
}
