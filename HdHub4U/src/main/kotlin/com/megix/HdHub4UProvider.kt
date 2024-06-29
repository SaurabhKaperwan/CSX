package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.network.CloudflareKiller

class HdHub4UProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://hdhub4u.durban"
    override var name = "HdHub4U"
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
        val home = document.select("section.home-wrapper > ul.recent-movies > li").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("figure > img")?.attr("alt") ?: ""
        val href = fixUrl(this.selectFirst("figure > a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("figure > img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..4) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("section.home-wrapper > ul.recent-movies > li").mapNotNull { it.toSearchResult() }

            searchResponse.addAll(results)

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val plot = document.selectFirst("meta[property=og:discription]")?.attr("content") ?: ""
        val title = document.selectFirst("meta[property=og:title]")?.attr("content") ?: ""
        val posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = posterUrl
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val regex = Regex("""https:\/\/hubdrive\.ws\/file\/\d+""")
        val links = regex.findAll(document.html()).mapNotNull { it.value }.toList()
        links.mapNotNull{
            val cookiesSSID = app.get(it).cookies["PHPSESSID"]
            val cookies = mapOf(
                "PHPSESSID" to "$cookiesSSID"
            )
            val hubDocument = app.get(it, cookies = cookies).document
            val link = hubDocument.selectFirst("a.btn.btn-primary.btn-user.btn-success1.m-1")?.attr("href") ?: "Empty"
            val newLink = link.replace(".lol", ".day")
            val hubDocument2 = app.get(newLink).document
            val lastLink = hubDocument2.selectFirst("div.vd > a")?.attr("href") ?: "Empty"
            loadExtractor(lastLink, subtitleCallback, callback)
        }
        return true
    }
}
