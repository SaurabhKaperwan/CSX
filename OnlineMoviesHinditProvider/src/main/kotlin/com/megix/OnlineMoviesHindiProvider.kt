
package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class OnlineMoviesHindiProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://111.90.159.132"
    override var name = "Online Movies Hindi"
    override val hasMainPage = true
    override var lang = "hi"
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
        val year = document.select("div.gmr-moviedata time").text().trim().split(" ").last()
            .toIntOrNull()
        val tvType = if (document.selectFirst("div.gmr-listseries a")?.text()
                ?.contains(Regex("(?i)(Eps\\s?[0-9]+)|(episode\\s?[0-9]+)")) == true
        ) TvType.TvSeries else TvType.Movie
        val description = document.selectFirst("div.entry-content p")?.text()?.trim()
        val trailer = fixUrlNull(document.select("iframe").attr("src"))
        val rating = document.select("div.gmr-meta-rating > span:nth-child(3)").text().toRatingInt()
        val actors = document.select("div.clearfix.content-moviedata > div:nth-child(7) a").map { it.text() }
        val recommendations = document.select("article").mapNotNull {
            it.toSearchResult()
        }

        return if (tvType == TvType.TvSeries) {
            val episodes = document.select("a.button-shadow").mapNotNull {
                val href = fixUrl(it.attr("href")?: return null)
                val name = it.text()?.trim()?: return null
                val season = name.substringAfter("S").substringBefore(' ').toInt() ?: return null
                val episode = name.substringAfterLast("Eps").toInt()?: return null
                Episode(
                    href,
                    name,
                    season,
                    episode
                )
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.rating = rating
                addActors(actors)
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                //this.tags = tags
                this.rating = rating
                addActors(actors)
                this.recommendations = recommendations
                addTrailer(trailer)
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
