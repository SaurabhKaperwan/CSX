package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class World4uFreeProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://world4ufree.boston"
    override var name = "World4uFree"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/category/bollywood/page" to "Bollywood",
        "$mainUrl/category/hollywood/page" to "Hollywood",
        "$mainUrl/category/web-series/page" to "Web Series",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("ul.recent-posts > li").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div > a").attr("title").replace("Download ", "")
        val href = this.selectFirst("div > a") ?. attr("href").toString()
        var posterUrl = this.selectFirst("div > a > img").attr("data-src").toString()
        if(posterUrl.isEmpty()) {
            posterUrl = this.selectFirst("div > a > img").attr("src").toString()
        }
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("ul.recent-posts > li").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]").attr("content").replace("Download ", "")
        val div = document.selectFirst("div.entry-content")
        val plot = div ?. selectFirst("p:matches((?i)(plot|synopsis|story))") ?. text() ?: ""
        var posterUrl = document.selectFirst("meta[property=og:image]").attr("content").toString()

        if(posterUrl.isEmpty() || posterUrl.contains("$mainUrl/favicon-32x32.png")) {
            posterUrl = document.selectFirst("div.separator > a > img").attr("data-src").toString()
        }
        val tvType = if (document.select("div.entry-content").text().contains("movie name", ignoreCase = true)) {
            TvType.Movie
        }
        else {
            TvType.TvSeries
        }

        if(tvType == TvType.TvSeries) {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var seasonNum = 1
            val seasonList = mutableListOf<Pair<String, Int>>()
            val buttons = document.select("a.my-button")
            buttons.forEach { button ->
                val titleElement = button.parent().parent().previousElementSibling()
                val title = titleElement.text()
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(title.toString()) ?. groupValues ?. get(1) ?: "Unknown"
                val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                val quality = qualityRegex.find(title.toString()) ?. groupValues ?. get(1) ?: "Unknown"
                val sizeRegex = Regex("""\b\d+(?:\.\d+)?(?:Mb|Gb|mb|gb)\b""")
                val size = sizeRegex.find(title.toString())?.value ?: ""
                if(realSeason != "Unknown" && quality != "Unknown") {
                    seasonList.add("S$realSeason $quality $size" to seasonNum)
                }
                else {
                    seasonList.add("$title" to seasonNum)
                }

                val wlinkz = button.attr("href")
                val doc = app.get(wlinkz).document
                val elements = doc.select("h3:matches((?i)(episode))")
                val episodes = mutableListOf<Episode>()
                elements.forEach { element ->
                    val title = element.text().replace("—", "") ?: "Empty"
                    var linkElement = element.nextElementSibling()
                    while (linkElement != null && linkElement.tagName() != "h4") {
                        linkElement = linkElement.nextElementSibling()
                    }
                    var link = ""
                    if(linkElement != null) {
                        val aTag = linkElement.selectFirst("a")
                        link = aTag ?. attr("href") ?: ""
                    }

                    if (link.isNotEmpty() && !title.contains("zip", ignoreCase = true)) {
                        episodes.add(
                            newEpisode(link){
                                name = "$title"
                                season = seasonNum
                                episode = elements.indexOf(element) + 1
                            }
                        )
                    }
                }
                tvSeriesEpisodes.addAll(episodes)
                seasonNum++
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name)}
            }
        }
        else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = plot
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if(data.contains("world4ufree")) {
            val document = app.get(data).document
            val links = document.select("a.my-button")
            links.amap {
                val link = it.attr("href")
                val doc = app.get(link).document
                val links = doc.select("a:matches((?i)(instant|download|direct))")
                links.amap {
                    loadExtractor(it.attr("href"), subtitleCallback, callback)
                }
            }
        }
        else {
            loadExtractor(data, subtitleCallback, callback)
        }
        return true   
    }
}
