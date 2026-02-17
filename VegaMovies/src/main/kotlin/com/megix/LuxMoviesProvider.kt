package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import java.net.URI

class LuxMoviesProvider : VegaMoviesProvider() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://luxmovies.cash"
    override var name = "LuxMovies"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    init {
        runBlocking {
            basemainUrl?.let {
                mainUrl = it
            }
        }
    }

    companion object {
        val basemainUrl: String? by lazy {
            runBlocking {
                try {
                    val response = app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/refs/heads/main/urls.json")
                    val json = response.text
                    val jsonObject = JSONObject(json)
                    jsonObject.optString("luxmovies")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override val mainPage = mainPageOf(
        "$mainUrl/page/%d/" to "Home",
        "$mainUrl/category/web-series/netflix/page/%d/" to "Netflix",
        "$mainUrl/category/web-series/disney-plus-hotstar/page/%d/" to "Disney Plus Hotstar",
        "$mainUrl/category/web-series/amazon-prime-video/page/%d/" to "Amazon Prime",
        "$mainUrl/category/web-series/mx-original/page/%d/" to "MX Original",
        "$mainUrl/category/web-series/jio-studios/page/%d/" to "Jio Cinema",
        "$mainUrl/category/web-series/sonyliv/page/%d/" to "Sony Liv",
        "$mainUrl/category/web-series/zee5-originals/page/%d/" to "Zee5",
        "$mainUrl/category/web-series/alt-balaji-web-series/page/%d/" to "ALT Balaji",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page)).document
        val home = document.select("a.blog-img").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.attr("title").replace("Download ", "")
        val href = this.attr("href")
        var posterUrl = this.select("img").attr("data-src")
        if(posterUrl.isEmpty()){
            posterUrl =  this.select("img").attr("src")
        }

        return newMovieSearchResponse(title, URI(href).path, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }
    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val document = app.get("$mainUrl/page/$page/?s=$query").document
        val results = document.select("a.blog-img").mapNotNull { it.toSearchResult() }
        val hasNext = if(results.isEmpty()) false else true
        return newSearchResponseList(results, hasNext)
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(fixUrl(url)).document
        var title = document.select("meta[property=og:title]").attr("content").replace("Download ", "")
        var posterUrl = document.select("meta[property=og:image]").attr("content")
        val div = document.selectFirst(".entry-content, .entry-inner")
        var description = div?.selectFirst("h3:matches((?i)(SYNOPSIS|PLOT)), h4:matches((?i)(SYNOPSIS|PLOT))")?.nextElementSibling()?.text()
        val imdbUrl = div?.selectFirst("a:matches((?i)(Rating))")?.attr("href")
        val heading = div?.selectFirst("h3 > strong > span")

        val tvtype = if (heading?.text()
            ?.let { it.contains("Series") || it.contains("SHOW") } == true) {
                "series"
        }   else {
            "movie"
        }

        val imdbId = imdbUrl?.substringAfter("title/")?.substringBefore("/")
        val jsonResponse = app.get("$cinemeta_url/$tvtype/$imdbId.json").text
        val responseData = tryParseJson<ResponseData>(jsonResponse)

        var cast: List<String> = emptyList()
        var genre: List<String> = emptyList()
        var imdbRating: String = ""
        var year: String = ""
        var background: String = posterUrl

        if(responseData != null) {
            description = responseData.meta.description ?: description
            cast = responseData.meta.cast ?: emptyList()
            title = responseData.meta.name ?: title
            genre = responseData.meta.genre ?: emptyList()
            imdbRating = responseData.meta.imdbRating ?: ""
            year = responseData.meta.year ?: ""
            posterUrl = responseData.meta.poster ?: posterUrl
            background = responseData.meta.background ?: background
        }

        if (tvtype == "series") {
            val hTags = div?.select("h3:matches((?i)(4K|[0-9]*0p)),h5:matches((?i)(4K|[0-9]*0p))")
                ?.filter { element -> !element.text().contains("Zip", true) } ?: emptyList()

            val tvSeriesEpisodes = mutableListOf<Episode>()
            val episodesMap: MutableMap<Pair<Int, Int>, List<String>> = mutableMapOf()

            for (tag in hTags) {
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(tag.toString())?.groupValues?.get(1)?.toIntOrNull() ?: 0

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
                    val vcloudLinks = document2.select("p > a").mapNotNull {
                        if(it.attr("href").contains("vcloud", true)) {
                            it.attr("href")
                        } else {
                            null
                        }
                    }

                    vcloudLinks.mapNotNull { vcloudlink ->
                        val key = Pair(realSeason, vcloudLinks.indexOf(vcloudlink) + 1)
                        if (episodesMap.containsKey(key)) {
                            val currentList = episodesMap[key] ?: emptyList()
                            val newList = currentList.toMutableList()
                            newList.add(vcloudlink)
                            episodesMap[key] = newList
                        } else {
                            episodesMap[key] = mutableListOf(vcloudlink)
                        }
                    }
                }
            }

            for ((key, value) in episodesMap) {
                val episodeInfo = responseData?.meta?.videos?.find { it.season == key.first && it.episode == key.second }
                val data = value.map { source->
                    EpisodeLink(
                        source
                    )
                }
                tvSeriesEpisodes.add(
                    newEpisode(data) {
                        this.name = episodeInfo?.name ?: episodeInfo?.title
                        this.season = key.first
                        this.episode = key.second
                        this.posterUrl = episodeInfo?.thumbnail
                        this.description = episodeInfo?.overview
                    }
                )
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.score = Score.from10(imdbRating)
                this.year = year.toIntOrNull() ?: year.substringBefore("–").toIntOrNull()
                this.backgroundPosterUrl = background
                addActors(cast)
                addImdbUrl(imdbUrl)
            }
        } else {
            val buttons = document.select("p > a:has(button)")
            val data = buttons.mapNotNull { button ->
                val link = fixUrl(button.attr("href"))
                val doc = app.get(link).document
                val source = doc.selectFirst("a:contains(V-Cloud)")?.attr("href").toString()
                EpisodeLink(source)
            }
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.score = Score.from10(imdbRating)
                this.year = year.toIntOrNull()
                this.backgroundPosterUrl = background
                addActors(cast)
                addImdbUrl(imdbUrl)
            }
        }
    }
}
