package com.KillerDogeEmpire

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import org.jsoup.Jsoup

class FullPorner : MainAPI() {
    override var mainUrl              = "https://fullporner.com"
    override var name                 = "FullPorner"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/home/"                to "Featured",
        "${mainUrl}/category/amateur/"    to "Amateur",
        "${mainUrl}/category/teen/"       to "Teen",
        "${mainUrl}/category/cumshot/"    to "CumShot",
        "${mainUrl}/category/deepthroat/" to "DeepThroat",
        "${mainUrl}/category/orgasm/"     to "Orgasm",
        "${mainUrl}/category/threesome/"  to "ThreeSome",
        "${mainUrl}/category/group-sex/"  to "Group Sex",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}").document
        val home = document.select("div.video-block div.video-card").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list = HomePageList(
            name = request.name,
            list = home,
            isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.video-card div.video-card-body div.video-title a")?.text() ?: return null
        val href = fixUrl(this.selectFirst("div.video-card div.video-card-body div.video-title a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.select("div.video-card div.video-card-image a img").attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..15) {
            val document = app.get("${mainUrl}/search?q=${query.replace(" ", "+")}&p=$i").document

            val results = document.select("div.video-block div.video-card").mapNotNull { it.toSearchResult() }

            searchResponse.addAll(results)

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title     = document.selectFirst("div.video-block div.single-video-left div.single-video-title h2")?.text()?.trim().toString()
        val iframeUrl = fixUrlNull(document.selectFirst("div.video-block div.single-video-left div.single-video iframe")?.attr("src")) ?: ""
        val iframeDocument = app.get(iframeUrl).document
        val posterUrl = "https://www.porntrex.com/contents/videos_screenshots/2241000/2241112/preview.jpg"

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document    = app.get(data).document

        val iframeUrl   = fixUrlNull(document.selectFirst("div.video-block div.single-video-left div.single-video iframe")?.attr("src")) ?: ""

        val extlinkList = mutableListOf<ExtractorLink>()

        val iframeDocument = app.get(iframeUrl).document
        val videoID = Regex("""var id = \"(.+?)\"""").find(iframeDocument.html())?.groupValues?.get(1)

        val pornTrexDocument = app.get("https://www.porntrex.com/embed/${videoID}").document
        val video_url = fixUrlNull(Regex("""video_url: \'(.+?)\',""").find(pornTrexDocument.html())?.groupValues?.get(1))
        if (video_url != null) {
            extlinkList.add(ExtractorLink(
                name,
                name,
                video_url,
                referer = "",
                quality = Qualities.Unknown.value
            ))
        }

        extlinkList.forEach(callback)

        return true
    }
}
