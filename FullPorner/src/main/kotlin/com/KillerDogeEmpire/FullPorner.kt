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
        val home     = document.select("div.video-block div.video-card").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
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

        val poster: String?
        val posterHeaders: Map<String, String>
        if (iframeUrl.contains("videoh")) {
            val iframeDocument = app.get(iframeUrl, interceptor = WebViewResolver(Regex("""mydaddy"""))).document

            val videoHtml = iframeDocument.selectXpath("//script[contains(text(),'poster')]").first()?.html()?.substringAfter("else{ \$(\"#jw\").html(\"")?.substringBefore("\");}if(hasAdblock)")?.replace("\\", "")
            val video     = Jsoup.parse(videoHtml.toString()).selectFirst("video")

            poster        = fixUrlNull(video?.attr("poster"))
            posterHeaders = mapOf(Pair("referer", "https://mydaddy.cc/"))
        } else {
            val iframeDocument = app.get(iframeUrl).document
            val videoDocument  = Jsoup.parse("<video" + iframeDocument.selectXpath("//script[contains(text(),'\$(\"#jw\").html(')]")[0]?.toString()?.replace("\\", "")?.substringAfter("<video")?.substringBefore("</video>") + "</video>")

            poster        = fixUrlNull(videoDocument.selectFirst("video")?.attr("poster").toString())
            posterHeaders = mapOf(Pair("referer", "https://xiaoshenke.net/"))
        }

        val tags            = document.select("div.video-blockdiv.single-video-left div.single-video-title p.tag-link span a").map { it.text() }
        val description     = document.selectFirst("div.video-block div.single-video-left div.single-video-title h2")?.text()?.trim().toString()
        val actors          = document.select("div.video-block div.single-video-left div.single-video-info-content p a").map { it.text() }
        val recommendations = document.select("div.video-block div.video-recommendation div.video-card").mapNotNull { it.toSearchResult() }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl       = poster
            this.posterHeaders   = posterHeaders
            this.plot            = description
            this.tags            = tags
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document    = app.get(data).document

        val iframeUrl   = fixUrlNull(document.selectFirst("div.video-block div.single-video-left div.single-video iframe")?.attr("src")) ?: ""

        val extlinkList = mutableListOf<ExtractorLink>()
        if (iframeUrl.contains("videoh")) {
            val iframeDocument = app.get(iframeUrl, interceptor = WebViewResolver(Regex("""mydaddy"""))).document
            val videoDocument  = Jsoup.parse("<video" + iframeDocument.selectXpath("//script[contains(text(),'\$(\"#jw\").html(')]").first()?.toString()?.replace("\\", "")?.substringAfter("<video")?.substringAfter("<video")?.substringBefore("</video>") + "</video>")

            videoDocument.select("source").map { res -> 
                extlinkList.add(ExtractorLink(
                    name,
                    name,
                    fixUrl(res.attr("src")),
                    referer = data,
                    quality = Regex("(\\d+.)").find(res.attr("title"))?.groupValues?.get(1).let { getQualityFromName(it) }
                )) 
            }
        } else if (iframeUrl.contains("xiaoshenke")) {
            val iframeDocument = app.get(iframeUrl).document
            val videoID        = Regex("""var id = \"(.+?)\"""").find(iframeDocument.html())?.groupValues?.get(1)

            val pornTrexDocument = app.get("https://www.porntrex.com/embed/${videoID}").document
            val video_url = fixUrlNull(Regex("""video_url: \'(.+?)\',""").find(pornTrexDocument.html())?.groupValues?.get(1))
            if (video_url != null) {
                extlinkList.add(ExtractorLink(
                    name,
                    name,
                    video_url,
                    referer = data,
                    quality = Qualities.Unknown.value
                ))
            }
        } else {
            val iframeDocument = app.get(iframeUrl).document
            val videoDocument  = Jsoup.parse("<video" + iframeDocument.selectXpath("//script[contains(text(),'\$(\"#jw\").html(')]").first()?.toString()?.replace("\\", "")?.substringAfter("<video")?.substringBefore("</video>") + "</video>")

            videoDocument.select("source").map { res -> 
                extlinkList.add(ExtractorLink(
                    this.name,
                    this.name,
                    fixUrl(res.attr("src")),
                    referer = mainUrl,
                    quality = Regex("(\\d+.)").find(res.attr("title"))?.groupValues?.get(1).let { getQualityFromName(it) }
                )) 
            }
        }

        extlinkList.forEach(callback)

        return true
    }
}
