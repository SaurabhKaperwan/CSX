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

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document
        val urlPatterns = listOf(
            Regex("""<source title='Best Quality' src="(.+?\.mp4)"""")
        )
        var episodeUrl = ""
        for (pattern in urlPatterns) {
            val matchResult = pattern.find(document)
            if (matchResult != null) {
                episodeUrl = matchResult.groupValues[1]
                break
            }
        }
        callback.invoke(
                   ExtractorLink(
                       source = this.name,
                       name = this.name,
                       url = episodeUrl,
                       referer = "",
                       quality = Qualities.Unknown.value,
                       isM3u8 = false
                   )
               )
       }
        return true
    }
}
