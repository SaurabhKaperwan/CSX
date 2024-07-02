package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.network.CloudflareKiller


open class KatMovieHDProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://katmoviehd.foo"
    override var name = "KatMovieHD"
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
        "$mainUrl/category/hollywood-eng/page/%d/" to "Hollywood",
        "$mainUrl/category/tv-shows/page/%d/" to "TV Shows",
        "$mainUrl/category/netflix/page/%d/" to "NetFlix",
        "$mainUrl/category/disney/page/%d/" to "Disney",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page), interceptor = cfInterceptor).document
        val home = document.select("div.post-thumb").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a") ?. attr("title") ?: ""
        val href = fixUrl(this.selectFirst("a") ?. attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..4) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("div.post-thumb").mapNotNull { it.toSearchResult() }

            searchResponse.addAll(results)

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]") ?. attr("content") ?: "" 
        val posterUrl = fixUrlNull(document.selectFirst("meta[property=og:image]") ?. attr("content"))

        val tvType = if (
            title.contains("Episode", ignoreCase = true) || 
            title.contains("season", ignoreCase = true) || 
            title.contains("series", ignoreCase = true)
        ) {
            TvType.TvSeries
        } else {
            TvType.Movie
        }

        if(tvType == TvType.TvSeries) {
            val tvSeriesEpisodes = mutableListOf<Episode>()

            val pTags = document.select("p:matches((?i)(Episode [0-9]+)),h3:matches((?i)(E[0-9]+)),h2:matches((?i)(Episode [0-9]+))")
            if (pTags.isNotEmpty()) {
                val episodesList = mutableListOf<Episode>()
    
                pTags.forEach { pTag ->
                    var hTagString = ""
                    var hTag = pTag
                    if(hTag.tagName() == "p") {
                        hTag = pTag.nextElementSibling()
                    }
                    while(hTag != null && hTag.tagName().matches(Regex("h\\d+"))) {
                        hTagString += hTag.toString()
                        hTag = hTag.nextElementSibling()
                    }
                    val details = pTag.text()
                    val episodes = Episode(
                        name = details,
                        data = hTagString,
                    )
                    episodesList.add(episodes)
                }
                tvSeriesEpisodes.addAll(episodesList)
                return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                    this.posterUrl = posterUrl
                }
            }
            else {
                val aTags = document.select("h2 > a:matches((?i)(4K|[0-9]*0p))")
                    .filter { element -> !element.text().contains("Pack", true) }
                var seasonNum = 1
                val seasonList = mutableListOf<Pair<String, Int>>()
                aTags.mapNotNull {
                    val emText = it.selectFirst("em") ?. text() ?: ""
                    val quality = Regex("(\\d{3,4})[pP]").find(emText ?: "") ?. groupValues ?. getOrNull(1) ?: "Unknown"
                    seasonList.add("$quality" to seasonNum)
                    val link = it . attr("href")
                    val episodeDocument = app.get(link).document
                    val kmhdPackRegex = Regex("""My_[a-zA-Z0-9]+""")
                    var kmhdLinks = kmhdPackRegex.findAll(episodeDocument.html()).mapNotNull { it.value }.toList()
                    val episodes = kmhdLinks.mapNotNull { kmhdLink ->
                        Episode(
                            name = "E${kmhdLinks.indexOf(kmhdLink) + 1} ${quality}",
                            data = "https://links.kmhd.net/file/${kmhdLink}",
                            season = seasonNum,
                            episode = kmhdLinks.indexOf(kmhdLink) + 1,
                        )
                    }
                    tvSeriesEpisodes.addAll(episodes)
                    seasonNum++
                }

                return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                    this.posterUrl = posterUrl
                    this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name)}
                }
            }
        }
        else {
            return newMovieLoadResponse(title, url, TvType.Movie, url) {
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
        if(data.contains("href")) {
            val regex = Regex("""<a href="([^"]+)">""")
            val links = regex.findAll(data).map { it.groupValues[1] }.toList()
            links.mapNotNull {
                var link = it
                if(link.contains("https://gd.kmhd.net/file/")) {
                    link.replace("https://gd.kmhd.net/file/", "https://new2.gdflix.cfd/file/")
                }
                loadExtractor(link, subtitleCallback, callback)
            }
        }
        else if(data.contains("kmhd.net/file")) {
            loadExtractor(data, subtitleCallback, callback)
        }
        else {
            val document = app.get(data).document
            val aTags = document.select("h2 > a")
            aTags.mapNotNull {
                val link = it.attr("href")
                loadExtractor(link, subtitleCallback, callback)
            }
        }
        return true       
    }
}
