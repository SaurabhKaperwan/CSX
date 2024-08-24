package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.network.CloudflareKiller


open class KatMovieHDProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://katmoviehd.fyi"
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
        val href = this.selectFirst("a") ?. attr("href").toString()
        val posterUrl = this.selectFirst("img")?.attr("src")

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

    //eg: the boys 4
    private suspend fun type1(url: String): MutableList<Episode> {
        val document = app.get(url).document
        val episodesList = mutableListOf<Episode>()
        val pTags = document.select("p:matches((?i)(Episode [0-9]+)),h3:matches((?i)(E[0-9]+)),h2:matches((?i)(Episode [0-9]+))")
        
        pTags.apmap { pTag ->
            var hTagString = ""
            var hTag = pTag
            
            if(hTag.tagName() == "p") {
                hTag = pTag.nextElementSibling()
            }

            if(hTag != null && hTag.tagName () == "div") {
                hTag = hTag.nextElementSibling()
            }
            
            while(hTag != null && hTag.tagName().matches(Regex("h\\d+"))) {
                hTagString += hTag.toString()
                hTag = hTag.nextElementSibling()
            }
            val details = pTag.text()
            val episodes = newEpisode(hTagString){
                name = details
            }
            episodesList.add(episodes)
        }
        return episodesList
    }

    private suspend fun type2(url: String, seasonList: MutableList<Pair<String, Int>>): MutableList<Episode> {
        val document = app.get(url).document
        val episodesList = mutableListOf<Episode>()

        val aTags = document.select("h2 > a:matches((?i)(4K|[0-9]*0p))")
                        .filter { element -> !element.text().contains("Pack", true) }
        var seasonNum = 1
        aTags.forEach { aTag ->
            val details = aTag ?. text() ?: ""
            val quality = Regex("(\\d{3,4})[pP]").find(details) ?. groupValues ?. getOrNull(1) ?: "Unknown"
            seasonList.add(Pair(quality, seasonNum))
            val link = aTag.attr("href")
            val episodeDocument = app.get(link).document
            if(link.contains("kmhd.net/archives")) {
                val episodes = episodeDocument.select("p > strong > a").mapIndexed { index, element ->
                    newEpisode(element.attr("href")) {
                        name = "E${index + 1} $quality"
                        season = seasonNum
                        episode = index + 1
                    }
                }
                episodesList.addAll(episodes)
                seasonNum++
            }
            else {
                val kmhdPackRegex = Regex("""My_[a-zA-Z0-9]+""")
                var kmhdLinks = kmhdPackRegex.findAll(episodeDocument.html()).mapNotNull { it.value }.toList()
                if(kmhdLinks.isEmpty()) {
                    kmhdLinks = Regex("""([A-Za-z0-9]+_[a-z0-9]+):\s*\{name:"[^"]+""").findAll(episodeDocument.html()).mapNotNull { it.groups[1]?.value }.toList()
                }
                val episodes = kmhdLinks.mapIndexed { index, kmhdLink ->
                    newEpisode("https://links.kmhd.net/file/$kmhdLink") {
                        name = "E${index + 1} $quality"
                        season = seasonNum
                        episode = index + 1
                    }
                }
                episodesList.addAll(episodes)
                seasonNum++
            }
        }
        return episodesList
    }
 
    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, interceptor = cfInterceptor).document
        val title = document.selectFirst("title").text()
        val posterUrl = document.selectFirst("meta[property=og:image]").attr("content")

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
                val episodesList = type1(url)
                tvSeriesEpisodes.addAll(episodesList)
                return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                    this.posterUrl = posterUrl
                }
            }
            else {
                val seasonList = mutableListOf<Pair<String, Int>>()
                val episodesList = type2(url, seasonList)
                tvSeriesEpisodes.addAll(episodesList)
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
            links.apmap {
                loadExtractor(it, subtitleCallback, callback)
            }
        }
        else if(data.contains("kmhd.net/file") || data.contains("gdflix")) {
            loadExtractor(data, subtitleCallback, callback)
        }
        else {
            val document = app.get(data).document
            val aTags = document.select("h2 > a")
            aTags.apmap {
                val link = it.attr("href")
                loadExtractor(link, subtitleCallback, callback)
            }
        }
        return true       
    }
}
