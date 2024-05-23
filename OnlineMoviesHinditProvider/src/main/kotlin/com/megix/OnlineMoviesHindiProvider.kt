package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class OnlineMoviesHindiProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://111.90.159.132"
    override var name = "Online Movies Hindi"
    override val hasMainPage = true
    override var lang = "hi"
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/year/2024/page/" to "Latest Movies",
        "$mainUrl/best-rating/page/" to "Popular Movies",
        "$mainUrl/hollywood-movies/page/" to "Hollywood Movies",
        "$mainUrl/bollywood-movies/page/" to "Bollywood Movies",
        "$mainUrl/tv-show/page/" to "TV Shows"
    ) 

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("p.entry-title")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("article img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query&post_type%5B%5D=post&post_type%5B%5D=tv").document

        return document.select("article").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h2.entry-title")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.gmr-movie-data img")?.attr("src"))
        val tvType = if (document.selectFirst("div.gmr-listseries a")?.text()
                ?.contains(Regex("(?i)(Eps\\s?[0-9]+)|(episode\\s?[0-9]+)")) == true
        ) TvType.TvSeries else TvType.Movie
        val description = document.selectFirst("div.entry-content p")?.text()?.trim()
        val recommendations = document.select("article").mapNotNull {
            it.toSearchResult()
        }

        if (tvType == TvType.TvSeries) {
            val episodes = document.select("a.button-shadow").mapNotNull {
                val href = fixUrl(it.attr("href"))
                val name = it.text()
                val season = name.substringAfter("S").substringBefore(' ').toIntOrNull() ?: 1
                val episode = name.substringAfterLast("Eps").toIntOrNull() ?: 1
                Episode(
                    href,
                    name ?: "",
                    season,
                    episode
                )
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.recommendations = recommendations
            }
        } else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.recommendations = recommendations
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val document = app.get(data).document
        val elements = document.select("a.button-shadow")
        val lastElement = elements.takeIf { it.isNotEmpty() }?.lastOrNull() //select last matched
        val href = lastElement?.attr("href") ?: ""

        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                href,
                referer = href,
                quality = Qualities.Unknown.value,
                //headers = mapOf("Range" to "bytes=0-"),
            )
        )

        return true
    }
}
