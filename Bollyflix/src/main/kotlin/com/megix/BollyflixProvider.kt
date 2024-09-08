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

class BollyflixProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://bollyflix.wales"
    override var name = "BollyFlix"
    override val hasMainPage = true
    override var lang = "hi"
    val cinemeta_url = "https://v3-cinemeta.strem.io/meta"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama,
        TvType.Anime
    )

    override val mainPage = mainPageOf(
        "$mainUrl/" to "Home",
        "$mainUrl/movies/hollywood/" to "Hollywood Movies",
        "$mainUrl/movies/south-hindi-dubbed/" to "SOUTH HINDI DUBBED",
        "$mainUrl/movies/bollywood/" to "Bollywood Movies",
        "$mainUrl/movies/hollywood/" to "Hollywood Movies",
        "$mainUrl/web-series/netflix/" to "NETFLIX",
        "$mainUrl/web-series/amazon-prime-vide/" to "AMAZON PRIME VIDEO",
        "$mainUrl/web-series/hotstar/" to "HOTSTAR",
        "$mainUrl/web-series/mx-player-originals/" to "MX PLAYER ORIGINALS",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = if(page == 1) {
            app.get(request.data).document
        }
        else {
            app.get(request.data + "page/" + page).document
        }
        val home = document.select("div.post-cards > article").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private suspend fun bypass(id: String): String {
        val url = "https://web.sidexfee.com/?id=$id"
        val document = app.get(url).text
        val encodeUrl = Regex("""link":"([^"]+)""").find(document) ?. groupValues ?. get(1) ?: ""
        return base64Decode(encodeUrl)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("a") ?. attr("title") ?. replace("Download ", "").toString()
        val href = this.selectFirst("a") ?. attr("href").toString()
        val posterUrl = this.selectFirst("img") ?. attr("src").toString()
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..3) {
            val document = app.get("$mainUrl/search/$query/page/$i/").document

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
        var title = document.selectFirst("title")?.text()?.replace("Download ", "").toString()
        var posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content").toString()
        var description = document.selectFirst("span#summary")?.text().toString()
        val tvtype = if(title.contains("Series") || url.contains("web-series")) {
            "series"
        }
        else {
            "movie"
        }
        val imdbUrl = document.selectFirst("div.imdb_left > a")?.attr("href")
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
            val buttons = document.select("a.maxbutton-download-links, a.dl")
            buttons.mapNotNull { button ->
                val id = button.attr("href").substringAfterLast("id=").toString()
                val seasonText = button.parent()?.previousElementSibling()?.text().toString()
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(seasonText)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val decodeUrl = bypass(id)
                val seasonDoc = app.get(decodeUrl).document
                val epLinks = seasonDoc.select("h3 > a")
                    .filter { element -> !element.text().contains("Zip", true) }
                var e = 1
                epLinks.mapNotNull {
                    val epUrl = app.get(it.attr("href"), allowRedirects = false).headers["location"].toString()
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
                        this.name = episodeInfo?.name
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
            val data = document.select("a.dl").amap {
                val id = it.attr("href").substringAfterLast("id=").toString()
                val decodeUrl = bypass(id)
                val source = app.get(decodeUrl, allowRedirects = false).headers["location"].toString()
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
            val source = it.source
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
