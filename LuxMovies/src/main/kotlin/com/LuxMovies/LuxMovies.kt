package com.coxju

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class LuxMovies : MainAPI() {
    override var mainUrl              = "https://luxmovies.art/"
    override var name                 = "LuxMovies"
    override val hasMainPage          = true
    override var lang                 = "hi"
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie, TvType.TvSeries,)

    override val mainPage = mainPageOf(
        "" to "Latest",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/page/$page").document
        val home = document.select("article.post-item").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
                list    = HomePageList(
                    name               = request.name,
                    list               = home,
                    isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {

        val title =     this.select("h3 > a").attr("title")?.text() ?: return null
        val href  =     fixUrl(this.select("h3 > a").attr("href"))
        val posterUrl = fixUrlNull(this.select("img.block-picture").attr("data-src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        val queryString = query.replace(" ", "-")

        for (i in 1..5) {
            val document = app.get("$mainUrl/search/$queryString/").document

            val results = document.select("div.item.col").mapNotNull { it.toSearchResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title       = document.selectFirst("h1.mhead__h")?.text() ?: ""
        val coverRegex = Regex("""posterImage: "(.+?)"""")
        val coverMatchResult = coverRegex.find(document.html())
        val poster      = coverMatchResult?.groupValues?.get(1) ?: ""
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()


        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot      = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
        ): Boolean {

        val document = app.get(data).document.toString()
        val sourceRegex = Regex("""<source title="Best Quality" src="([^"]+)" type="video/mp4">""")
        val matchResult = sourceRegex.find(document)

        val src = matchResult?.groupValues?.get(1) ?: ""

        callback.invoke(
            ExtractorLink(
                this.name,
                this.name,
                fixUrl(src),
                referer = mainUrl,
                quality = Qualities.Unknown.value,
            )
        )
        return true
    }
}
