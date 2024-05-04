package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.network.CloudflareKiller

open class VegaMoviesProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://vegamovies.ph"
    override var name = "VegaMovies"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    private val cfInterceptor = CloudflareKiller()
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/%d/" to "Home",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page), interceptor = cfInterceptor).document
        val home = document.select("article.post-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title") ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img.blog-picture")?.attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query", interceptor = cfInterceptor).document

        return document.select("article.post-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content") ?: return null
        val posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document1 = app.get(data).document
        val regex = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/""")
        val links = regex.findAll(document1.html()).mapNotNull { it.value }.toList()

        return links.mapNotNull { link ->
            val document2 = app.get(link).document
            val vcloudRegex = Regex("""https:\/\/vcloud\.lol\/[^\s"]+""")
            val vcloudLinks = vcloudRegex.findAll(document2.html()).mapNotNull { it.value }.toList()

            if (vcloudLinks.isNotEmpty()) {
                loadExtractor(vcloudLinks.first(), subtitleCallback, callback)
                true
            } else {
                null
            }
        }.firstOrNull() ?: false
    }
}