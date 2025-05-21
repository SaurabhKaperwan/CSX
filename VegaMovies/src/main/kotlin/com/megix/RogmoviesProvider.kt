package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.megix.LuxMoviesProvider.Companion
import org.jsoup.nodes.Element
import kotlinx.coroutines.runBlocking

class RogmoviesProvider : VegaMoviesProvider() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://rogmovies.lol"
    override var name = "Rogmovies"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    companion object {
        val basemainUrl: String? by lazy {
            runBlocking {
                try {
                     app.get("https://vglist.nl/?re=rogmovies",allowRedirects = false)
                        .document
                        .selectFirst("meta[http-equiv=refresh]")
                        ?.attr("content")
                        ?.substringAfter("url=")
                        ?.takeIf { it.startsWith("http") }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override val mainPage = mainPageOf(
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/page/%d/" to "Latest",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/hindi-dubbed-movies/page/%d/" to "South Movies",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/bollywood/page/%d/" to "Bollywood",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/web-series/netflix/page/%d/" to "Netflix",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/web-series/disney-plus-hotstar/page/%d/" to "Disney Plus Hotstar",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/web-series/amazon-prime-video/page/%d/" to "Amazon Prime",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/web-series/jio-studios/page/%d/" to "Jio Cinema",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/web-series/sonyliv/page/%d/" to "Sony Liv",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/web-series/zee5-originals/page/%d/" to "Zee5",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/web-series/mx-original/page/%d/" to "MX Original",
        "${LuxMoviesProvider.basemainUrl ?: mainUrl}/category/web-series/alt-balaji-web-series/page/%d/" to "ALT Balaji",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(
            request.data.format(page),
            referer = mainUrl,
            headers = headers
        ).document
        val home = document.select("a.blog-img").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.attr("title").replace("Download ", "")
        val href = this.attr("href")
        val posterUrl = this.select("img").attr("data-src")
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }
    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..7) {
            val document = app.get(
                "${basemainUrl ?: mainUrl}/page/$i/?s=$query",
                referer = mainUrl,
                headers = headers
            ).document
            val results = document.select("a.blog-img").mapNotNull { it.toSearchResult() }
            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }
        return searchResponse
    }

}
