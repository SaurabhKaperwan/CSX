package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.base64Decode
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.google.gson.Gson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

open class MoviesmodProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://moviesmod.gift"
    override var name = "Moviesmod"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    val cinemeta_url = "https://v3-cinemeta.strem.io/meta"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama,
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/web-series/on-going/page/" to "Latest Web Series",
        "$mainUrl/movies/page/" to "Latest Movies",
        "$mainUrl/animated-web-series/page/" to "Anime",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div.post-cards > article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a")?.attr("title")?.replace("Download ", "").toString()
        val href = this.selectFirst("a")?.attr("href").toString()
        val posterUrl = this.selectFirst("a > div > img")?.attr("src").toString()

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..7) {
            val document = app.get("$mainUrl/search/$query/page/$i").document

            val results = document.select("div.post-cards > article").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }


    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        var title = document.selectFirst("meta[property=og:title]")?.attr("content")?.replace("Download ", "").toString()
        val ogTitle = title
        var posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content").toString()
        var description = document.selectFirst("div.imdbwp__teaser")?.text()
        val div = document.selectFirst("div.thecontent")?.text().toString()
        val tvtype = if (div.contains("season", ignoreCase = true) == true) "series" else "movie"
        val imdbUrl = document.selectFirst("a.imdbwp__link")?.attr("href")

        val responseData = if (!imdbUrl.isNullOrEmpty()) {
            val imdbId = imdbUrl.substringAfter("title/").substringBefore("/")
            val jsonResponse = app.get("$cinemeta_url/$tvtype/$imdbId.json").text
            if(jsonResponse.isNotEmpty() && jsonResponse.startsWith("{")) {
                val gson = Gson()
                gson.fromJson(jsonResponse, ResponseData::class.java)
            }
            else {
                null
            }
        } else {
            null
        }

        var cast: List<String> = emptyList()
        var genre: List<String> = emptyList()
        var imdbRating: String = ""
        var year: String = ""
        var background: String = posterUrl

        if(responseData != null) {
            description = responseData.meta?.description ?: description
            cast = responseData.meta?.cast ?: emptyList()
            title = responseData.meta?.name ?: title
            genre = responseData.meta?.genre ?: emptyList()
            imdbRating = responseData.meta?.imdbRating ?: ""
            year = responseData.meta?.year ?: ""
            posterUrl = responseData.meta?.poster ?: posterUrl
            background = responseData.meta?.background ?: background
        }

        if(tvtype == "series") {

            if(title != ogTitle) {
                val checkSeason = Regex("""Season\s*\d*1|S\s*\d*1""").find(ogTitle)
                if (checkSeason == null) {
                    val seasonText = Regex("""Season\s*\d+|S\s*\d+""").find(ogTitle)?.value
                    if(seasonText != null) {
                        title = title + " " + seasonText.toString()
                    }
                }
            }

            val tvSeriesEpisodes = mutableListOf<Episode>()
            val episodesMap: MutableMap<Pair<Int, Int>, List<String>> = mutableMapOf()
            val buttons = document.select("a.maxbutton-episode-links,.maxbutton-g-drive,.maxbutton-af-download")

            buttons.mapNotNull {
                var link = it.attr("href")
                val seasonText = it.parent()?.previousElementSibling()?.text().toString()
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(seasonText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                if(link.contains("url=")) {
                    val base64Value = link.substringAfter("url=")
                    link = base64Decode(base64Value)
                }
                val doc = app.get(fixUrl(link)).document
                val hTags = doc.select("h3,h4")
                var e = 1
                hTags.mapNotNull {
                    val epUrl = it.selectFirst("a")?.attr("href")
                    val key = Pair(realSeason, e)
                    if(epUrl != null) {
                        if (episodesMap.containsKey(key)) {
                            val currentList = episodesMap[key] ?: emptyList()
                            val newList = currentList.toMutableList()
                            newList.add(epUrl)
                            episodesMap[key] = newList
                        } else {
                            episodesMap[key] = mutableListOf(epUrl)
                        }
                    }
                    e++
                }
                e = 1
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
                this.rating = imdbRating.toRatingInt()
                this.year = year.toIntOrNull()
                this.backgroundPosterUrl = background
                addActors(cast)
                addImdbUrl(imdbUrl)
            }
        }
        else {
            val data = document.select("a.maxbutton-download-links").mapNotNull {
                var link = it.attr("href")
                if(link.contains("url=")) {
                    val base64Value = link.substringAfter("url=")
                    link = base64Decode(base64Value)
                }

                val doc = app.get(fixUrl(link)).document
                val source = doc.selectFirst("a.maxbutton-1, a.maxbutton-5")?.attr("href").toString()
                EpisodeLink(
                    source
                )
            }
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.rating = imdbRating.toRatingInt()
                this.year = year.toIntOrNull()
                this.backgroundPosterUrl = background
                addActors(cast)
                addImdbUrl(imdbUrl)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val sources = parseJson<ArrayList<EpisodeLink>>(data)
        sources.amap {
            var source = it.source
            if(source.contains("unblocked")) {
                source = bypass(source).toString()
            }
            loadExtractor(source, subtitleCallback, callback)
        }
        return true
    }

    data class Meta(
        val id: String?,
        val imdb_id: String?,
        val type: String?,
        val poster: String?,
        val logo: String?,
        val background: String?,
        val moviedb_id: Int?,
        val name: String?,
        val description: String?,
        val genre: List<String>?,
        val releaseInfo: String?,
        val status: String?,
        val runtime: String?,
        val cast: List<String>?,
        val language: String?,
        val country: String?,
        val imdbRating: String?,
        val slug: String?,
        val year: String?,
        val videos: List<EpisodeDetails>?
    )

    data class EpisodeDetails(
        val id: String?,
        val name: String?,
        val title: String?,
        val season: Int?,
        val episode: Int?,
        val released: String?,
        val overview: String?,
        val thumbnail: String?,
        val moviedb_id: Int?
    )

    data class ResponseData(
        val meta: Meta?
    )

    data class EpisodeLink(
        val source: String
    )
}

