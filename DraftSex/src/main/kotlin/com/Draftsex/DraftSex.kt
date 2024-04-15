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
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/page$page.html").document
        val home = document.select("div.item.col").mapNotNull { it.toSearchResult() }

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

        val title =     this.select("a").attr("title")
        val href  =     fixUrl(this.select("a").attr("href"))
        val posterUrl = fixUrlNull(this.select("img").attr("src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val document = app.get("${mainUrl}/page/$i/?s=$query&id=5036").document

            val results = document.select("article").mapNotNull { it.toSearchResult() }

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

        val title       = document.selectFirst("meta[property=og:title]:last-of-type")?.attr("content")?.trim().toString()
        val poster      = fixUrlNull(document.selectFirst("meta[property^=og:image]:last-of-type")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()


        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot      = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        //val sources = mutableListOf<String>()
        document.select("article > div > div > button").forEach { button ->
            val onclickAttr = button.attr("onclick")
            val regex = Regex("""playEmbed\('([^']+)'\)""")
            val matchResult = regex.find(onclickAttr)
            val playlistUrl = matchResult?.groups?.get(1)?.value
            playlistUrl?.let {
                //println(it) // Print the extracted URL
                //sources.add(it) // Add the URL to the sources list
                loadExtractor(it, subtitleCallback, callback)
            }
        }
        return true
    }
}
