package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.network.CloudflareKiller

open class VegaMoviesProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://vegamovies.yt"
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
        "$mainUrl/category/featured/page/%d/" to "Amazon Prime",
        "$mainUrl/category/web-series/netflix/page/%d/" to "Netflix",
        "$mainUrl/category/web-series/disney-plus-hotstar/page/%d/" to "Disney Plus Hotstar",
        "$mainUrl/category/web-series/amazon-prime-video/page/%d/" to "Amazon Prime",
        "$mainUrl/category/web-series/mx-original/page/%d/" to " MX Original",
        "$mainUrl/category/anime-series/page/%d/" to "Anime Series",
        "$mainUrl/category/korean-series/page/%d/" to "Korean Series",
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

    private suspend fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title")
        val trimTitle = title?.let {
            if (it.contains("Download ")) {
                it.replace("Download ", "")
            } else {
                it
            }
        } ?: ""

        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val document = app.get(href).document
        val posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))

        return newMovieSearchResponse(trimTitle, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..2) {
            val document = app.get("$mainUrl/page/$i/?s=$query", interceptor = cfInterceptor).document

            val results = document.select("article.post-item").mapNotNull { it.toSearchResult() }

            searchResponse.addAll(results)

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]")?.attr("content")
        val trimTitle = title?.let {
            if (it.contains("Download ")) {
                it.replace("Download ", "")
            } else {
                it
            }
        } ?: ""
        val posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content"))
        val div = document.selectFirst("div.entry-content")
        val element = div.selectFirst("h3:containsOwn(plot), h4:containsOwn(plot)")
        val description = element?.nextElementSibling()?.select("p")?.first()?.text()

        val tvType = if (url.contains("season") ||
                  (title?.contains("(Season") ?: false) ||
                  Regex("Series synopsis").containsMatchIn(url ?: "")) {
            TvType.TvSeries
        } else {
            TvType.Movie
        }

        if (tvType == TvType.TvSeries) {
            val HPRegex = Regex("""<(?:h3|h5).*?>.*?(?:1080p|720p|480p|2160p|4K).*?(?:MB|GB).*?\s*<p.*?class="(?:dwd-button|btn btn-sm btn-outline)".*?<\/p>""")
            val HPTags = HPRegex.findAll(div.html()).mapNotNull { it.value }.toList()
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var seasonNum = 1

            for(HPTag in HPTags) {
                val realSeasonRegex = Regex("""(?:Season|S|Season 0| S0)(\d+)""")
                val realSeason = realSeasonRegex.find(HPTag)?.groupValues?.get(1) ?: "Unknown"
                val qualityRegex = Regex("""1080p|720p|480p|2160p|4K""")
                val quality = qualityRegex.find(HPTag)?.value ?: "Unknown"

                var Eurl = ""

                val regex1 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*V-Cloud)(?!.*G-Direct)""")
                val regex2 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*Episode Link)(?!.*G-Direct)""")
                val regex3 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*Episodes Link)(?!.*G-Direct)""")
                val regex4 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*Single Episode)(?!.*G-Direct)""")
                val regex5 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*Download)(?!.*G-Direct)""")
                val regex6 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*G-Direct)""")

                val regexList = listOf(regex1, regex2, regex3, regex4, regex5, regex6)

                for (regex in regexList) {
                    val match = regex.find(HPTag)
                    if (match != null) {
                        Eurl = match.value
                        break
                    }
                }

                val document2 = app.get(Eurl).document
                val vcloudRegex1 = Regex("""https:\/\/vcloud\.lol\/[^\s"]+""")
                val vcloud2Regex2 = Regex("""https:\/\/vcloud\.lol\/\w+""")
                val fastDlRegex = Regex("""https:\/\/fastdl\.icu\/embed\?download=[a-zA-Z0-9]+""")

                var vcloudLinks = vcloudRegex1.findAll(document2.html()).mapNotNull { it.value }.toList()
                if(vcloudLinks.isEmpty()) {
                    vcloudLinks = vcloud2Regex2.findAll(document2.html()).mapNotNull { it.value }.toList()
                    if(vcloudLinks.isEmpty()){
                        vcloudLinks = fastDlRegex.findAll(document2.html()).mapNotNull { it.value }.toList()
                    }
                }
                val episodes = vcloudLinks.mapNotNull { vcloudlink ->
                    Episode(
                        name = "S${realSeason} E${vcloudLinks.indexOf(vcloudlink) + 1} ${quality}",
                        data = vcloudlink,
                        season = seasonNum,
                        episode = vcloudLinks.indexOf(vcloudlink) + 1,
                    )
                }
                tvSeriesEpisodes.addAll(episodes)
                seasonNum++
            }
            return newTvSeriesLoadResponse(trimTitle, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }
        else {
            return newMovieLoadResponse(trimTitle, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.contains("vcloud.lol") || data.contains("filebee")) {
            loadExtractor(data, subtitleCallback, callback)
            return true
        } else {
            val document1 = app.get(data).document
            val regex = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/""")
            val links = regex.findAll(document1.html()).mapNotNull { it.value }.toList()

            links.mapNotNull { link ->
                val document2 = app.get(link).document
                val vcloudRegex = Regex("""https:\/\/vcloud\.lol\/[^\s"]+""")
                var vcloudLinks = vcloudRegex.findAll(document2.html()).mapNotNull { it.value }.toList()
                if(vcloudLinks.isEmpty()) {
                    val newRegex = Regex("""https:\/\/fastdl\.icu\/embed\?download=[a-zA-Z0-9]+""")
                    vcloudLinks = newRegex.findAll(document2.html()).mapNotNull { it.value }.toList()
                }

                if (vcloudLinks.isNotEmpty()) {
                    loadExtractor(vcloudLinks.first(), subtitleCallback, callback)
                }
            }
            return true
        }
    }
}
