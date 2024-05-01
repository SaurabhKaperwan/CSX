package com.megix

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
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

    val title = document.selectFirst("div.video-block div.single-video-left div.single-video-title h2")?.text()?.trim().toString()

    val iframeUrl = fixUrlNull(document.selectFirst("div.video-block div.single-video-left div.single-video iframe")?.attr("src")) ?: ""

    val iframeDocument = app.get(iframeUrl).document

    val videoID = Regex("""var id = \"(.+?)\"""").find(iframeDocument.html())?.groupValues?.get(1)
    val pornTrexDocument = app.get("https://www.porntrex.com/embed/${videoID}").document
    val matchResult = Regex("""preview_url:\s*'([^']+)'""").find(pornTrexDocument.html())
    val poster = matchResult?.groupValues?.get(1)
    val posterUrl = fixUrlNull("https:$poster")



    val tags = document.select("div.video-block div.single-video-left div.single-video-title p.tag-link span a").map { it.text() }
    val description = document.selectFirst("div.video-block div.single-video-left div.single-video-title h2")?.text()?.trim().toString()
    val actors = document.select("div.video-block div.single-video-left div.single-video-info-content p a").map { it.text() }
    val recommendations = document.select("div.video-block div.video-recommendation div.video-card").mapNotNull { it.toSearchResult() }

    return newMovieLoadResponse(title, url, TvType.NSFW, url) {
        this.posterUrl = posterUrl
        this.plot = description
        this.tags = tags
        this.recommendations = recommendations
        addActors(actors)
    }
}

    override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val document = app.get(data).document
    val iframeUrl = fixUrlNull(document.selectFirst("div.video-block div.single-video-left div.single-video iframe")?.attr("src")) ?: ""
    val extlinkList = mutableListOf<ExtractorLink>()
    val iframeDocument = app.get(iframeUrl).document
    val videoID = Regex("""var id = \"(.+?)\"""").find(iframeDocument.html())?.groupValues?.getOrNull(1)

    if (videoID != null) {
        val pornTrexDocument = app.get("https://www.porntrex.com/embed/$videoID").document
        val videoUrlsRegex = Regex("""(?:video_url|video_alt_url2|video_alt_url3): \'(.+?)\',""")
        val matchResults = videoUrlsRegex.findAll(pornTrexDocument.html())

        val videoUrls = matchResults.map { it.groupValues[1] }.toList()

        videoUrls.forEach { videoUrl ->
            extlinkList.add(
                ExtractorLink(
                    name,
                    name,
                    videoUrl,
                    referer = "",
                    quality = Regex("""_(1080|720|480|360)p\.mp4""").find(videoUrl)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Qualities.Unknown.value,
                )
            )
        }
    }
    extlinkList.forEach(callback)

    return true
    }

}
