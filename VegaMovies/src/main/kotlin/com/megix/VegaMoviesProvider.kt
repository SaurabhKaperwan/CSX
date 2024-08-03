package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.network.CloudflareKiller

open class VegaMoviesProvider : MainAPI() { // all providers must be an instance of MainAPI

    override var mainUrl = "https://vegamovies.tw"
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
        "$mainUrl/category/web-series/mx-original/page/%d/" to "MX Original",
        "$mainUrl/category/anime-series/page/%d/" to "Anime Series",
        "$mainUrl/category/korean-series/page/%d/" to "Korean Series"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page), interceptor = cfInterceptor).document
        val home = document.select("article.post-item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title")
        val trimTitle = title?.let {
            if (it.contains("Download ")) {
                it.replace("Download ", "")
            } else {
                it
            }
        } ?: ""

        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        var posterUrl = fixUrlNull(this.selectFirst("img.blog-picture")?.attr("data-src").toString())
        if (posterUrl == null) {
            posterUrl = fixUrlNull(this.selectFirst("img.blog-picture")?.attr("src").toString())
        }

        return newMovieSearchResponse(trimTitle, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..3) {
            val document = app.get("$mainUrl/page/$i/?s=$query", interceptor = cfInterceptor).document
            val results = document.select("article.post-item").mapNotNull { it.toSearchResult() }
            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
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
        val documentText = document.text()
        val div = document.select("div.entry-content")
        val hTagsDisc = div.select("h3:matches((?i)(SYNOPSIS|PLOT)), h4:matches((?i)(SYNOPSIS|PLOT))")
        val pTagDisc = hTagsDisc.first()?.nextElementSibling()
        val plot = pTagDisc?.text()

        val aTagRatings = div.select("a:matches((?i)(Rating))")
        val aTagRating = aTagRatings.firstOrNull()
        val ratingText = aTagRating?.selectFirst("span")?.text()
        val rating = ratingText?.substringAfter("-")
            ?.substringBefore("/")
            ?.trim()
            ?.toRatingInt()

        val tvType = if (url.contains("season") ||
            (title?.contains("(Season") ?: false) ||
            Regex("Series synopsis").containsMatchIn(documentText) || Regex("Series Name").containsMatchIn(documentText)) {
            TvType.TvSeries
        } else {
            TvType.Movie
        }

        if (tvType == TvType.TvSeries) {
            val hTags = div.select("h3:matches((?i)(4K|[0-9]*0p)),h5:matches((?i)(4K|[0-9]*0p))")
                .filter { element -> !element.text().contains("Zip", true) }

            val tvSeriesEpisodes = mutableListOf<Episode>()
            var seasonNum = 1
            val seasonList = mutableListOf<Pair<String, Int>>()

            for (tag in hTags) {
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(tag.toString())?.groupValues?.get(1) ?: "Unknown"
                val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                val quality = qualityRegex.find(tag.toString())?.groupValues?.get(1) ?: "Unknown"
                val sizeRegex = Regex("""\b\d+(?:\.\d+)?(?:MB|GB)\b""")
                val size = sizeRegex.find(tag.toString())?.value ?: ""

                seasonList.add("S$realSeason $quality $size" to seasonNum)

                val pTag = tag.nextElementSibling()
                val aTags: List<Element>? = if (pTag != null && pTag.tagName() == "p") {
                    pTag.select("a")
                } else {
                    tag.select("a")
                }

                var unilink = aTags ?. find {
                    it.text().contains("V-Cloud", ignoreCase = true) ||
                    it.text().contains("Episode", ignoreCase = true) ||
                    it.text().contains("Download", ignoreCase = true)
                }

                if (unilink == null) {
                    unilink = aTags ?. find {
                        it.text().contains("G-Direct", ignoreCase = true)
                    }
                }

                val Eurl = unilink?.attr("href")
                Eurl?.let { eurl ->
                    val document2 = app.get(eurl).document
                    val vcloudRegex = Regex("""https:\/\/vcloud\.lol\/[^\s"]+""")
                    var vcloudLinks = vcloudRegex.findAll(document2.html()).mapNotNull { it.value }.toList()

                    if (vcloudLinks.isEmpty()) {
                        val fastDlRegex = Regex("""https:\/\/fastdl.icu\/embed\?download=[a-zA-Z0-9]+""")
                        vcloudLinks = fastDlRegex.findAll(document2.html()).mapNotNull { it.value }.toList()
                    }

                    val episodes = vcloudLinks.mapNotNull { vcloudlink ->
                        newEpisode(vcloudlink) {
                            name = "S${realSeason} E${vcloudLinks.indexOf(vcloudlink) + 1} ${quality}"
                            season = seasonNum
                            episode = vcloudLinks.indexOf(vcloudlink) + 1
                        }
                    }

                    tvSeriesEpisodes.addAll(episodes)
                    seasonNum++
                }
            }

            return newTvSeriesLoadResponse(trimTitle, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.rating = rating
                this.seasonNames = seasonList.map { (name, int) -> SeasonData(int, name) }
            }
        } else {
            val pTags = document.select("p:has(a:has(button))")
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var seasonNum = 1
            val seasonList = mutableListOf<Pair<String, Int>>()

            pTags.forEach { pTag ->
                val link = pTag.selectFirst("a")?.attr("href") ?: ""
                val details = pTag.previousElementSibling().text() ?: "Unknown"
                seasonList.add("$details" to seasonNum)
                val doc = app.get(link).document
                val source = doc.selectFirst("a:contains(V-Cloud)").attr("href") ?: ""
                
                val episode = newEpisode(source){
                    name = "Play"
                    season = seasonNum
                    episode = 1
                }

                tvSeriesEpisodes.add(episode)
                seasonNum++
            }
            return newTvSeriesLoadResponse(trimTitle, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.rating = rating
                this.seasonNames = seasonList.map { (name, int) -> SeasonData(int, name) }
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var url = data
        if (url.contains("vcloud.lol/api")) {
            val document = app.get(url).document
            url = document.selectFirst("h4 > a").attr("href")
        }
        loadExtractor(url, subtitleCallback, callback)
        return true
    }
}