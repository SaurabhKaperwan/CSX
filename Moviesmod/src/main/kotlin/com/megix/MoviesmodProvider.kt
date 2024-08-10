package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.network.CloudflareKiller

class MoviesmodProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://moviesmod.band"
    override var name = "Moviesmod"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    val cfInterceptor = CloudflareKiller()
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/web-series/on-going/page/" to "Latest Web Series",
        "$mainUrl/movies/latest-released/page/" to "Latest Movies",
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

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a").attr("title").replace("Download ", "")
        val href = this.selectFirst("a").attr("href")
        val posterUrl = this.selectFirst("a > div > img").attr("src")
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/search/$query/page/$i", interceptor = cfInterceptor).document

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
        val title = document.selectFirst("meta[property=og:title]").attr("content").replace("Download ", "")
        val posterUrl = document.selectFirst("meta[property=og:image]").attr("content")
        val description = document.selectFirst("div.imdbwp__teaser").text()
        val div = document.selectFirst("div.thecontent").text()
        val tvtype = if(div.contains("season", ignoreCase = true)) TvType.TvSeries else TvType.Movie
        val imdbUrl = document.selectFirst("a.imdbwp__link").attr("href")

        if(tvtype == TvType.TvSeries) {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var seasonNum = 1
            val seasonList = mutableListOf<Pair<String, Int>>()
            val buttons = document.select("a.maxbutton-episode-links,.maxbutton-g-drive,.maxbutton-af-download")

            buttons.mapNotNull {
                var link = it.attr("href")
                val titleElement = it.parent().previousElementSibling()
                val seasonText = titleElement.text()
                seasonList.add(Pair(seasonText, seasonNum))

                if(link.contains("url=")) {
                    val base64Value = link.substringAfter("url=")
                    link = base64Decode(base64Value)
                }
                val doc = app.get(link).document
                val hTags = doc.select("h3,h4")
                var e = 1
                hTags.mapNotNull {
                    val title = it.text()
                    val epUrl = it.selectFirst("a").attr("href")
                    tvSeriesEpisodes.add(
                        newEpisode(epUrl) {
                            name = title
                            season = seasonNum
                            episode = e++
                        }
                    )
                }
                e = 1
                seasonNum++
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name)}
                this.plot = description
                addImdbUrl(imdbUrl)
            }
        }
        else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = description
                addImdbUrl(imdbUrl)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if(data.contains("unblockedgames")) {
            val link = bypass(data).toString()
            loadExtractor(link, subtitleCallback, callback)
        }
        else {
            val document = app.get(data).document
            document.select("a.maxbutton-download-links").mapNotNull {
                var link = it.attr("href")
                if(link.contains("url=")) {
                    val base64Value = link.substringAfter("url=")
                    link = base64Decode(base64Value)
                }

                val doc = app.get(link).document
                val url = doc.selectFirst("a.maxbutton-1").attr("href")
                val driveLink = bypass(url).toString()
                loadExtractor(driveLink, subtitleCallback, callback)
            }
        }
        return true
    }
}
