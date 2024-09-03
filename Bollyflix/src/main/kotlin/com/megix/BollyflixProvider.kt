package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.base64Decode

class BollyflixProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://bollyflix.wales"
    override var name = "BollyFlix"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/movies/bollywood/page/" to "Bollywood Movies",
        "$mainUrl/movies/hollywood/page/" to "Hollywood Movies",
        "$mainUrl/web-series/ongoing-series/page/" to "Ongoing Series",
        "$mainUrl/anime/page/" to "Anime"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.post-cards > article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private suspend fun bypass(id: String): String {
        val url = "https://web.sidexfee.com/?id=$id"
        val document = app.get(url).text
        val encodeUrl = Regex("""link":"([^"]+)""").find(document) ?. groupValues ?. get(1) ?: ""
        return base64Decode(encodeUrl)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a") ?. attr("title") ?. replace("Download ", "").toString()
        val href = this.selectFirst("a") ?. attr("href").toString()
        val posterUrl = this.selectFirst("img") ?. attr("src").toString()
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/search/$query/page/$i/").document

            val results = document.select("div.post-cards > article").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("title")?.text()?.replace("Download ", "").toString()
        var posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content").toString()
        val plot = document.selectFirst("span#summary")?.text().toString()
        val tvType = if(title.contains("Series") || url.contains("web-series")) {
            TvType.TvSeries
        }
        else {
            TvType.Movie
        }
        if(tvType == TvType.TvSeries) {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var seasonNum = 1
            val seasonList = mutableListOf<Pair<String, Int>>()
            val buttons = document.select("a.maxbutton-download-links, a.dl")
            buttons.mapNotNull { button ->
                val id = button.attr("href").substringAfterLast("id=").toString()
                val seasonText = button.parent()?.previousElementSibling()?.text().toString()
                seasonList.add(Pair(seasonText, seasonNum))
                val decodeUrl = bypass(id)
                val seasonDoc = app.get(decodeUrl).document
                val epLinks = seasonDoc.select("h3 > a")
                    .filter { element -> !element.text().contains("Zip", true) }
                var epNum = 1
                epLinks.mapNotNull {
                    val epLink = app.get(it.attr("href"), allowRedirects = false).headers["location"].toString()
                    val epText = it.text()
                    tvSeriesEpisodes.add(
                        newEpisode(epLink) {
                            this.name = epText
                            this.season = seasonNum
                            this.episode = epNum
                        }
                    )
                    epNum++
                }
                epNum = 1
                seasonNum++
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name) }
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
        if(data.contains("gdflix")) {
            loadExtractor(data, subtitleCallback, callback)
        }
        else {
            val document = app.get(data).document
            document.select("a.dl").amap {
                val id = it.attr("href").substringAfterLast("id=").toString()
                val decodeUrl = bypass(id)
                val gdflixUrl = app.get(decodeUrl, allowRedirects = false).headers["location"].toString()
                loadExtractor(gdflixUrl, subtitleCallback, callback)
            }
        }
        return true
    }
}
