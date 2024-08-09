package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl

class MoviesDriveProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://moviesdrive.website"
    override var name = "MoviesDrive"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/category/amzn-prime-video/page/" to "Prime Video",
        "$mainUrl/category/netflix/page/" to "Netflix",
        "$mainUrl/category/hotstar/page/" to "Hotstar",
        "$mainUrl/category/anime/page/" to "Anime",
        "$mainUrl/category/k-drama/page/" to "K Drama",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("ul.recent-movies > li").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("figure > img") ?. attr("title")
        val trimTitle = title ?. let {
            if (it.contains("Download ")) {
                it.replace("Download ", "")
            } else {
                it
            }
        } ?: ""

        val href = fixUrl(this.selectFirst("figure > a") ?. attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("figure > img") ?. attr("src").toString())
    
        return newMovieSearchResponse(trimTitle, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("ul.recent-movies > li").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("meta[property=og:title]") ?. attr("content")
        val trimTitle = title ?.let {
            if (it.contains("Download ")) {
                it.replace("Download ", "")
            } else {
                it
            }
        } ?: ""

        val plotElement = document.select(
            "h2:contains(Storyline), h3:contains(Storyline), h5:contains(Storyline), h4:contains(Storyline), h4:contains(STORYLINE)"
        ).firstOrNull() ?. nextElementSibling()

        val plot = plotElement ?. text() ?: document.select(".ipc-html-content-inner-div").firstOrNull() ?. text() ?: ""

        val posterUrl = document.selectFirst("img[decoding=\"async\"]") ?. attr("src") ?: ""
        val seasonRegex = """(?i)season\s*\d+""".toRegex()
        val imdbId = document.selectFirst("a:contains(IMDb)") ?. attr("href")

        val tvType = if (
            title ?. contains("Episode", ignoreCase = true) ?: false || 
            seasonRegex.containsMatchIn(title ?: "") || 
            title ?. contains("series", ignoreCase = true) ?: false
        ) { 
            TvType.TvSeries
        } else {
            TvType.Movie
        }
        if(tvType == TvType.TvSeries) {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            var buttons = document.select("h5 > a")
                .filter { element -> !element.text().contains("Zip", true) }

            if(buttons.isNotEmpty()) {
                val seasonList = mutableListOf<Pair<String, Int>>()
                var seasonNum = 1
                buttons.forEach { button ->
                    val titleElement = button.parent() ?. previousElementSibling()
                    val mainTitle = titleElement ?. text() ?: ""
                    val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                    val realSeason = realSeasonRegex.find(mainTitle.toString()) ?. groupValues ?. get(1) ?: " Unknown"
                    val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                    val quality = qualityRegex.find(mainTitle.toString()) ?. groupValues ?. get(1) ?: " Unknown"
                    val sizeRegex = Regex("""\b\d+(?:\.\d+)?(?:MB|GB)\b""")
                    val size = sizeRegex.find(mainTitle.toString())?.value ?: ""
                    seasonList.add("S$realSeason $quality $size" to seasonNum)
                    val episodeLink = button.attr("href") ?: ""

                    val doc = app.get(episodeLink).document
                    var elements = doc.select("span:matches((?i)(Ep))")
                    if(elements.isEmpty()) {
                        elements = doc.select("a:matches((?i)(HubCloud))")
                    }
                    val episodes = mutableListOf<Episode>()
                    
                    elements.forEach { element ->
                        var episodeString = ""
                        var title = mainTitle
                        if(element.tagName() == "span") {
                            val titleTag = element.parent()
                            title = titleTag ?. text() ?: ""
                            var linkTag = titleTag ?. nextElementSibling()

                            while(linkTag != null && (linkTag.text() ?. contains("HubCloud", ignoreCase = true) ?: false)) {
                                episodeString += linkTag.toString()
                                linkTag = linkTag.nextElementSibling()
                            }
                        }
                        else {
                            episodeString = element.toString()
                        }

                        if (episodeString.isNotEmpty()) {
                            episodes.add(
                                newEpisode(episodeString,initializer = {
                                    name = "$title"
                                    season = seasonNum
                                    episode = elements.indexOf(element) + 1
                                }, fix = false)
                            )
                        }
                    }
                    tvSeriesEpisodes.addAll(episodes)
                    seasonNum++
                }
                return newTvSeriesLoadResponse(trimTitle, url, TvType.TvSeries, tvSeriesEpisodes) {
                    this.posterUrl = posterUrl
                    this.plot = plot
                    this.seasonNames = seasonList.map {(name, int) -> SeasonData(int, name)}
                    addImdbUrl(imdbId)
                }
            }
            else {
                val episodesList = mutableListOf<Episode>()
                val pTags = document.select("p.p1")
                pTags.forEach { pTag ->
                    val text = pTag.text() ?: ""
                    val nextTag = pTag.nextElementSibling()
                    val nextTagString = nextTag ?. toString() ?: ""
                    val episodes = newEpisode(nextTagString, initializer = {
                        name = text
                    }, fix = false)
                    episodesList.add(episodes)
                }
                return newTvSeriesLoadResponse(trimTitle, url, TvType.TvSeries, episodesList) {
                    this.posterUrl = posterUrl
                    this.plot = plot
                    addImdbUrl(imdbId)
                }
            }

        }
        else {
            return newMovieLoadResponse(trimTitle, url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = plot
                addImdbUrl(imdbId)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        if(data.contains("graph.")) {
            val regex = Regex("""(?i)https?:\/\/[^\s"<]+""")
            val links = regex.findAll(data).mapNotNull { it.value }.toList()
            links.amap {
                val doc = app.get(it).document
                doc.select("h3 > a").mapNotNull {
                    val src = it.attr("href")
                    loadExtractor(src, subtitleCallback, callback)
                }
            }
        }
        else if(data.contains("moviesdrive")) {
            val document = app.get(data).document
            val buttons = document.select("h5 > a")
            buttons.amap { button ->
                val link = button.attr("href")
                val doc = app.get(link).document
                val innerButtons = doc.select("h5 > a")
                innerButtons.amap { innerButton ->
                    val source = innerButton.attr("href")
                    loadExtractor(source, subtitleCallback, callback)
                }
            }
        }
        else {
            val hubCloudRegex = Regex("""(?i)https?:\/\/[^\s"<]+""")
            var hubCloudLinks = hubCloudRegex.findAll(data).mapNotNull { it.value }.toList()
            hubCloudLinks.amap { link ->
                loadExtractor(link, subtitleCallback, callback)
            }
        }
        return true   
    }
}
