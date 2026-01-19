package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
// import com.lagradost.cloudstream3.network.CloudflareKiller

class MoviesDriveProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://moviesdrive.forum"
    override var name = "MoviesDrive"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    val aiometa_url = "https://aiometadata.elfhosted.com/stremio/9197a4a9-2f5b-4911-845e-8704c520bdf7/meta"
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
                    jsonObject.optString("moviesdrive")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override val mainPage = mainPageOf(
        "/page/" to "Home",
        "/category/amzn-prime-video/page/" to "Prime Video",
        "/category/netflix/page/" to "Netflix",
        "/category/hotstar/page/" to "Hotstar",
        "/category/anime/page/" to "Anime",
        "/category/k-drama/page/" to "K Drama",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("${mainUrl}${request.data}${page}").document
        val home = document.select("#moviesGridMain > a").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select("p").text().replace("Download ", "")
        val href = this.attr("href")
        val posterUrl = this.select("img").attr("src")
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val text = app.get(
            "$mainUrl/searchapi.php?q=$query&page=$page"
        ).text
        val gson = Gson()
        val response = gson.fromJson(text, MSearchResponse::class.java)
        //val response = parseJson<MSearchResponse>(text)
        val hasNext = response.hits.isNotEmpty()
        val results = response.hits.map { hit ->
            val doc = hit.document
            newMovieSearchResponse(doc.postTitle, mainUrl + doc.permalink, TvType.Movie) {
                this.posterUrl = doc.postThumbnail
            }
        }
        return newSearchResponseList(results, hasNext)
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        var title = document.select("title").text().replace("Download ", "")
        var posterUrl = document.select("main > p > img").attr("src")
        val imdbUrl =  document.select("a[href*=\"imdb\"]").attr("href")
        val imdbId = imdbUrl.substringAfter("title/").substringBefore("/")
        val seasonRegex = """(?i)season\s*\d+""".toRegex()

        val tvtype = if (
            title.contains("Episode", true) == true ||
            seasonRegex.containsMatchIn(title) ||
            title.contains("series", true) == true
        ) {
            "series"
        } else {
            "movie"
        }

        val jsonResponse = app.get("$aiometa_url/$tvtype/$imdbId.json").text
        val responseData = tryParseJson<ResponseData>(jsonResponse)

        var cast: List<ActorData> = emptyList()
        var genre: List<String> = emptyList()
        var imdbRating: String = ""
        var year: String = ""
        var background: String = posterUrl
        var description = ""

        if(responseData != null) {
            val meta = responseData.meta
            description = meta.description ?: description
            cast = meta?.app_extras?.cast?.mapNotNull { castMember ->
                if (castMember.name != null) {
                    ActorData(
                        Actor(
                            name = castMember.name,
                            image = castMember.photo
                        ),
                        roleString = castMember.character
                    )
                } else {
                    null
                }
            } ?: emptyList()
            title = meta.name ?: title
            genre = meta.genre ?: emptyList()
            imdbRating = meta.imdbRating ?: ""
            year = meta.year ?: ""
            posterUrl = meta.poster ?: posterUrl
            background = meta.background ?: background
        }

        if(tvtype == "series") {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            val episodesMap: MutableMap<Pair<Int, Int>, List<String>> = mutableMapOf()
            var buttons = document.select("h5 > a")
                .filter { element -> !element.text().contains("Zip", true) }

            buttons.forEach { button ->
                val titleElement = button.parent() ?. previousElementSibling()
                val mainTitle = titleElement ?. text() ?: ""
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(mainTitle.toString()) ?. groupValues ?. get(1) ?.toInt() ?: 0
                val episodeLink = button.attr("href") ?: ""

                val doc = app.get(episodeLink).document
                var elements = doc.select("span:matches((?i)(Ep))")
                if(elements.isEmpty()) {
                    elements = doc.select("a:matches((?i)(HubCloud|GDFlix))")
                }
                var e = 1

                elements.forEach { element ->
                    if(element.tagName() == "span") {
                        val titleTag = element.parent()
                        var hTag = titleTag?.nextElementSibling()
                        e = Regex("""Ep(\d{2})""").find(element.toString())?.groups?.get(1)?.value ?.toIntOrNull() ?: e
                        while (
                            hTag != null &&
                            (
                                hTag.text().contains("HubCloud", ignoreCase = true) ||
                                hTag.text().contains("gdflix", ignoreCase = true) ||
                                hTag.text().contains("gdlink", ignoreCase = true)
                            )
                        ) {
                            val aTag = hTag.selectFirst("a")
                            val epUrl = aTag?.attr("href").toString()
                            val key = Pair(realSeason, e)
                            if (episodesMap.containsKey(key)) {
                                val currentList = episodesMap[key] ?: emptyList()
                                val newList = currentList.toMutableList()
                                newList.add(epUrl)
                                episodesMap[key] = newList
                            } else {
                                episodesMap[key] = mutableListOf(epUrl)
                            }
                            hTag = hTag.nextElementSibling()
                        }
                        e++
                    }
                    else {
                        val epUrl = element.attr("href")
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
                this.score = Score.from10(imdbRating)
                this.year = year.toIntOrNull()
                this.backgroundPosterUrl = background
                this.actors = cast
                addImdbUrl(imdbUrl)
            }
        }
        else {
            val buttons = document.select("h5 > a")
            val data = buttons.flatMap { button ->
                val link = button.attr("href")
                val doc = app.get(link).document
                val innerButtons = doc.select("a").filter { element ->
                    element.attr("href").contains(Regex("hubcloud|gdflix|gdlink", RegexOption.IGNORE_CASE))
                }
                innerButtons.mapNotNull { innerButton ->
                    val source = innerButton.attr("href")
                    EpisodeLink(
                        source
                    )
                }
            }
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
                this.tags = genre
                this.score = Score.from10(imdbRating)
                this.year = year.toIntOrNull()
                this.backgroundPosterUrl = background
                this.actors = cast
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
        val app_extras: AppExtras? = null,
        val language: String?,
        val country: String?,
        val imdbRating: String?,
        val slug: String?,
        val year: String?,
        val videos: List<EpisodeDetails>?
    )

    data class AppExtras (
        val cast: List<Cast> = emptyList()
    )

    data class Cast (
        val name      : String? = null,
        val character : String? = null,
        val photo     : String? = null
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
        val meta: Meta
    )

    data class EpisodeLink(
        val source: String
    )

    data class MSearchResponse(
        val hits: List<Hit>
    )

    data class Hit(
        val document: Document
    )

    data class Document(
        val permalink: String,

        @SerializedName("post_thumbnail")
        val postThumbnail: String,

        @SerializedName("post_title")
        val postTitle: String
    )
}

