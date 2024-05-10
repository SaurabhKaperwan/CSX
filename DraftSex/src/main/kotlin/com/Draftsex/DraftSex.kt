package com.coxju

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class DraftSex : MainAPI() {
    override var mainUrl              = "https://draftsex.porn"
    override var name                 = "DraftSex"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "" to "Latest",
        "$mainUrl/most-viewed/" to "Most Viewed",
        "$mainUrl/top-rated/" to "Top Rated",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/page$page.html").document
        val home = document.select("div.item.col").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title =     this.select("a").attr("title")
        val href  =     fixUrl(this.select("a").attr("href"))
        val posterUrl = fixUrlNull(this.select("img").attr("src"))
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
        val coverMatchResult = Regex("""posterImage: "(.+?)"""").find(document.html())
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
