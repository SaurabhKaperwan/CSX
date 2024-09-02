package com.Deadstream

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Deadstream : MainAPI() {
    override var mainUrl = "https://deadstream.xyz"
    override var name = "Deadstream"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Movie, TvType.Anime)

    override val mainPage = mainPageOf(
        "$mainUrl/recently-updated" to "Latest",
        "$mainUrl/most-popular" to "Most Popular",
        "$mainUrl/tv" to "Series",
        "$mainUrl/movie" to "Movies",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + "?page=$page", timeout = 30L).document
        val home = document.select("div.flw-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = false
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrl(this.selectFirst("img")?.attr("data-src") ?: return null)
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val document = app.get("${mainUrl}/search?keyword=${query}", timeout = 30L).document
        val results = document.select("div.flw-item").mapNotNull { it.toSearchResult() }
        searchResponse.addAll(results)
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, timeout = 30L).document
        val title = document.selectFirst("h2.film-name")?.text() ?: return null
        val div = document.selectFirst("div[style*=background-image]") ?: return null
        val posterUrl = div.attr("style").substringAfter("url(").substringBefore(")")
        val plot = document.selectFirst("div.item-title.w-hide")?.text() ?: ""
        val type = if (document.selectFirst("div.film-stats")?.text()?.contains("MOVIE") == true) TvType.Movie else TvType.TvSeries

        if (type == TvType.TvSeries) {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var seasonNum = 1
            val seasonList = mutableListOf<Pair<String, Int>>()

            document.select("a.btn-play").mapNotNull {
                val seasonText = it.text()
                seasonList.add(Pair(seasonText, seasonNum))
                val href = fixUrl(it.attr("href"))
                val doc = app.get(href, timeout = 30L).document

                doc.selectFirst("div.ss-list")?.select("a")?.mapNotNull { episode ->
                    val epName = episode.attr("title")
                    val epNum = episode.attr("data-number").toIntOrNull() ?: 0
                    val epUrl = fixUrl(episode.attr("href"))
                    tvSeriesEpisodes.add(
                        newEpisode(epUrl) {
                            name = epName
                            season = seasonNum
                            this.episode = epNum
                        }
                    )
                }
                seasonNum++
            }

            return newTvSeriesLoadResponse(title, url, TvType.Anime, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.seasonNames = seasonList.map { (name, int) -> SeasonData(int, name) }
            }

        } else {
            val movieLink = fixUrl(document.selectFirst("a.btn-play")?.attr("href") ?: return null)
            return newMovieLoadResponse(title, url, TvType.Movie, movieLink) {
                this.posterUrl = posterUrl
                this.plot = plot
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data, timeout = 30L).document
        val quality = document.selectFirst("div#servers-content")

        quality?.select("div.item")?.amap {
            val id = it.attr("data-embed")
            val url = "https://deaddrive.xyz/embed/$id"
            val doc = app.get(url, timeout = 30L).document
            doc.selectFirst("ul.list-server-items")?.select("li")?.amap { source ->
                if (!source.attr("data-video").contains("short.ink")) {
                    loadExtractor(source.attr("data-video"), subtitleCallback, callback)
                }
            }
        }
        return true
    }
}
