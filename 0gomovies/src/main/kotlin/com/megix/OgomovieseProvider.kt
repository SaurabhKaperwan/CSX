package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

open class OgomoviesProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://0gomovies.movie"
    override var name = "0gomovies"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/ogomovies/page/" to "Home",
        "$mainUrl/genre/hindi-web-series/page/" to "Web Series",
        "$mainUrl/genre/watch-tamil-movies/page/" to "Tamil Movies",
        "$mainUrl/genre/gomovies-malayalam/page/" to "Malayalam Movies",
        "$mainUrl/genre/hollywood/page/" to "Hollywood Movies",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.ml-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a > img")?.attr("alt").toString()
        val href = this.selectFirst("a")?.attr("href").toString()
        val posterUrl = this.selectFirst("a > img")?.attr("src").toString()
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/search-query/$query/page/$i").document

            val results = document.select("div.ml-item").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("div.detail-mod > h3") ?. text() ?: ""
        val posterUrl = document.selectFirst("meta[property=og:image]") ?. attr("content") ?: document.selectFirst("div.sheader noscript img") ?. attr("src")
        val plot = document.selectFirst("div.desc") ?. text() ?: ""
        val seasonPattern = Regex("s[0-9]{2}")
        val tvType = if(url.contains("season", ignoreCase = true) || 
                        url.contains("series", ignoreCase = true) ||
                        seasonPattern.containsMatchIn(url) ){            
            TvType.TvSeries
        } else {
            TvType.Movie
        }

        if(tvType == TvType.TvSeries) {
            val headers = mapOf("Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            val doc = app.get("${url}/watching/", headers = headers).document
            val listLink = doc.selectFirst("li.episode-item")?.attr("data-drive").toString()
            val listDoc = app.get(listLink).document
            val episodesList = mutableListOf<Episode>()
            listDoc.select("div.content-pt > p > a").mapNotNull {
                val href = it.attr("href").substringAfterLast("link=")
                val epInfo = it.selectFirst("button") ?. text() ?: ""
                val episodes = Episode(href, "${epInfo}")
                episodesList.add(episodes)
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodesList) {
                this.posterUrl = posterUrl
                this.plot = plot
            }
        }
        else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = plot
            }   
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if(data.contains("0gomovies")) {
            val headers = mapOf("Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            val document = app.get("${data}/watching/", headers = headers).document
            val link = document.selectFirst("li.episode-item")?.attr("data-drive").toString()
            loadExtractor(link, subtitleCallback, callback)
        }
        else {
            loadExtractor(data, subtitleCallback, callback)
        }
        return true   
    }
}
