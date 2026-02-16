package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.httpsify
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.URI

open class VegaMoviesProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://vegamovies.cologne"
    override var name = "VegaMovies"
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
                    jsonObject.optString("vegamovies")
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
        "$mainUrl/category/anime-series/page/%d/" to "Anime Series",
        "$mainUrl/category/korean-series/page/%d/" to "Korean Series"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page)).document
        val home = document.select("div.movies-grid > a").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select("img").attr("alt").replace("Download ", "")
        val href = this.attr("href")
        val posterUrl = httpsify(this.select("img").attr("src"))

        return newMovieSearchResponse(title, URI(href).path, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val json = app.get("$mainUrl/search.php?q=$query&page=$page").text
        val response = tryParseJson<VegaSearchResponse>(json) ?: return null
        val results = response.hits.map { hit ->
            val doc = hit.document
            newMovieSearchResponse(doc.post_title.replace("Download ", ""), doc.permalink, TvType.Movie) {
                this.posterUrl = doc.post_thumbnail
            }
        }
        return newSearchResponseList(results)
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(fixUrl(url)).document
        var title = document.select("header.post-header > h1").text().replace("Download ", "")
        var posterUrl = document.select("p > img").attr("src")
        val imdbUrl =  document.select("a[href*=\"imdb\"]").attr("href")
        val imdbId = imdbUrl.substringAfter("title/").substringBefore("/")

        val tvtype = if(
            document
            .selectFirst("h3:has(span:contains(Series-SYNOPSIS/PLOT))") != null
        ) {
            "series"
        } else {
            "movie"
        }

        var description = document
            .selectFirst("h3:has(span:contains(SYNOPSIS/PLOT))")
            ?.nextElementSibling()
            ?.text()

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
            val hTags = document.select("main > h3:matches((?i)(4K|[0-9]*0p)),main > h5:matches((?i)(4K|[0-9]*0p))")
                .filter { element -> !element.text().contains("Zip", true) } ?: emptyList()

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
            val buttons = document.select("a:has(button.dwd-button)")
            val data = buttons.mapNotNull { button ->
                val link = fixUrl(button.attr("href"))
                val doc = app.get(link).document
                val source = doc.select("a:contains(V-Cloud)").attr("href")
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
        val background: String?,
        val moviedb_id: Int?,
        val name: String?,
        val description: String?,
        val genre: List<String>?,
        val genres: List<String>?,
        val releaseInfo: String?,
        val status: String?,
        val runtime: String?,
        val cast: List<String>?,
        val language: String?,
        val country: String?,
        val imdbRating: String?,
        val year: String?,
        val videos: List<EpisodeDetails>?,
    )

    data class EpisodeDetails(
        val id: String?,
        val name: String?,
        val title: String?,
        val season: Int,
        val episode: Int,
        val released: String?,
        val firstAired: String?,
        val overview: String?,
        val thumbnail: String?,
        val moviedb_id: Int?,
        val imdb_id: String?,
        val imdbSeason: Int?,
        val imdbEpisode: Int?,
    )

    data class ResponseData(
        val meta: Meta,
    )

    data class EpisodeLink(
        val source: String
    )

    data class VegaSearchResponse(
        val hits: List<VegaHit>
    )

    data class VegaHit(
        val document: VegaDocument
    )

    data class VegaDocument(
        val id: String,
        val imdb_id: String?,
        val post_title: String,
        val permalink: String,
        val post_thumbnail: String
    )
}
