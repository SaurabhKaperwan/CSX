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

        for (i in 1..3) {
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

        val tvType = if (url.contains("season")) TvType.TvSeries else TvType.Movie

        if (tvType == TvType.TvSeries) {
            val div = document.selectFirst("div.entry-content")
            val tvSeriesEpisodes = mutableListOf<Episode>()
            val seasons = mutableListOf<String>()
            val qualities = mutableListOf<String>()
            val regex = Regex("""<h3.*>(?=.*(?:S))(?=.*(?:1080p|720p|480p)).*<\/h3>""")
            val matches = regex.findAll(div.html()).mapNotNull { it.value }.toList()
            if(matches.isNotEmpty()) {
                for(match in matches) {
                    val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                    val realSeason = realSeasonRegex.find(match)?.groupValues?.get(1)

                    val qualityRegex = Regex("""1080p|720p|480p""")
                    val quality = qualityRegex.find(match)?.value

                    if(realSeason != null && quality != null) {
                        seasons.add(realSeason)
                        qualities.add(quality)
                    }
                }
            }

            val regex1 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*V-Cloud)(?!.*G-Direct)""")
            val regex2 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*Episode Link)""")
            val regex3 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*Download)""")
            val regex4 = Regex("""https:\/\/unilinks\.lol\/[a-zA-Z0-9]+\/(?=.*G-Direct)""")

            var urls = regex1.findAll(div.html()).mapNotNull { it.value }.toList()
            if(urls.isEmpty()) {
                urls = regex2.findAll(div.html()).mapNotNull { it.value }.toList()
                if(urls.isEmpty()) {
                    urls = regex3.findAll(div.html()).mapNotNull { it.value }.toList()
                }
                if(urls.isEmpty()) {
                    urls = regex4.findAll(div.html()).mapNotNull { it.value }.toList()
                }
            }
            var seasonNum = 1
            var i = 0

            for (url in urls) {
                val document2 = app.get(url).document
                val vcloudRegex = Regex("""https:\/\/vcloud\.lol\/[^\s"]+""")
                val fastDlRegex = Regex("""https:\/\/fastdl\.icu\/embed\?download=[a-zA-Z0-9]+""")

                var vcloudLinks = vcloudRegex.findAll(document2.html()).mapNotNull { it.value }.toList()
                if(vcloudLinks.isEmpty()) {
                    vcloudLinks = fastDlRegex.findAll(document2.html()).mapNotNull { it.value }.toList()
                }
                if(seasons.size <= i || qualities.size <= i) break
                val episodes = vcloudLinks.mapNotNull { vcloudlink ->
                    Episode(
                        name = "${seasons[i]} Episode ${vcloudLinks.indexOf(vcloudlink) + 1} ${qualities[i]}",
                        data = vcloudlink,
                        season = seasonNum,
                        episode = vcloudLinks.indexOf(vcloudlink) + 1,
                    )
                }
                tvSeriesEpisodes.addAll(episodes)
                seasonNum++
                i++
            }
            return newTvSeriesLoadResponse(trimTitle, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
            }
        }
        else {
            return newMovieLoadResponse(trimTitle, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
            }
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if (data.contains("vcloud.lol")) {
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


//<h3.*>(?=.*(?:S))(?=.*(?:1080p|720p|480p)).*<\/h3>
//Season (\d+)
//(\d+p)
