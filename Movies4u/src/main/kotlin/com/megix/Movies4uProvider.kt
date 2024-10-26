package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.google.gson.Gson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import okhttp3.FormBody

class Movies4uProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://www.movies4u.com.vc"
    override var name = "Movies4u"
    override val hasMainPage = true
    override var lang = "hi"
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
        "$mainUrl/category/bollywood/page/" to "Bollywood",
        "$mainUrl/category/web-series/page/" to "Web Series",
        "$mainUrl/category/anime/page/" to "Anime"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("article.post").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3 > a")?.text().toString()
        val href = fixUrl(this.selectFirst("figure > a")?.attr("href").toString())
        val posterUrl = this.selectFirst("figure > a > img")?.attr("src").toString()
        val quality = getQualityFromString(this.selectFirst("figure > a > span.video-label")?.text().toString())
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.quality = quality
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val document = app.get("$mainUrl/search.php?q=$query&page=$i/").document

            val results = document.select("article.post").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        var title = document.selectFirst("title")?.text().toString()
        var posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content").toString()

        val checkType = document.selectFirst("h3 > strong")?.text().toString()
        val imdbUrl = document.selectFirst("strong > a:matches((?i)(IMDb))")?.attr("href")
        var description = ""

        var tvtype = if (checkType.contains("Movie")) {
            "movie"
        } else {
            "series"
        }

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
            val tvSeriesEpisodes = mutableListOf<Episode>()
            val episodesMap: MutableMap<Pair<Int, Int>, List<String>> = mutableMapOf()
            var buttons = document.select("a:has(button.btn.btn-sm.btn-outline:matches((?i)(V-Cloud)))")
            if(buttons.isEmpty()) {
                buttons = document.select("a:has(button.dwd-button)")
            }
            buttons.forEach { button ->
                val titleElement = button.parent()?.previousElementSibling()?.text()
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(titleElement.toString()) ?. groupValues ?. get(1) ?.toInt() ?: 0
                val doc = app.get(button.attr("href")).document
                val episodes = doc.select("button.btn.btn-sm.btn-outline:matches((?i)(VCloud))")
                var e = 1

                episodes.forEach { element ->
                    val epUrl = element.parent()?.attr("href")
                    if(epUrl != null) {
                        val key = Pair(realSeason, e)
                        if (episodesMap.containsKey(key)) {
                            val currentList = episodesMap[key] ?: emptyList()
                            val newList = currentList.toMutableList()
                            newList.add(epUrl)
                            episodesMap[key] = newList
                        } else {
                            episodesMap[key] = mutableListOf(epUrl)
                        }
                        e++
                    }
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
            val data = document.select("a:has(button.dwd-button)").mapNotNull {
                val doc = app.get(it.attr("href")).document  
                val link = doc.selectFirst("p > a:matches((?i)(VCloud))")?.attr("href") ?: ""
                EpisodeLink(
                    link
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

    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
            ?: Qualities.Unknown.value
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val sources = parseJson<ArrayList<EpisodeLink>>(data)
        sources.mapNotNull {
            val source = it.source
            val doc = app.get(source).document
            val quality = doc.selectFirst("tbody > tr > td:matches((?i)(Name:))")?.nextElementSibling()?.text() ?: ""
            val size = doc.selectFirst("tbody > tr > td:matches((?i)(Size))")?.nextElementSibling()?.text() ?: ""
            val value = doc.selectFirst("form > input")?.attr("value") ?: ""
            callback.invoke(
                ExtractorLink(
                    "Movies4u",
                    "Movies4u $size",
                    value,
                    "",
                    getIndexQuality(quality),
                )
            )
            val body = FormBody.Builder().add("hash", value).build()
            val doc2 = app.post(source, requestBody = body).document
            doc2.select("a:matches((?i)(Download Server))").mapNotNull { aTag->
                val link = aTag.attr("href")
                callback.invoke(
                    ExtractorLink(
                        "Movies4u",
                        "Movies4u $size",
                        link,
                        "",
                        getIndexQuality(quality),
                    )
                )
            }
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

