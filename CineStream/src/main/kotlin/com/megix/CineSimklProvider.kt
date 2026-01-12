package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addSimklId
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.CommonActivity.activity
import com.lagradost.cloudstream3.syncproviders.SyncRepo
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import com.google.gson.Gson
import com.megix.CineStreamExtractors.invokeAllSources
import com.megix.CineStreamExtractors.invokeAllAnimeSources

class CineSimklProvider: MainAPI() {
    override var name = "CineSimkl"
    override var mainUrl = "https://simkl.com"
    override var supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Torrent
    )
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = false
    override val supportedSyncNames = setOf(SyncIdName.Simkl)
    private val apiUrl = "https://api.simkl.com"
    private final val mediaLimit = 10
    private val auth = BuildConfig.SIMKL_API
    private val headers = mapOf("Content-Type" to "application/json")
    private val repo = SyncRepo(AccountManager.simklApi)
    private val kitsuAPI = "https://anime-kitsu.strem.fun"
    private val cinemetaAPI = "https://v3-cinemeta.strem.io"
    private val haglund_url = "https://arm.haglund.dev/api/v2"
    private val image_proxy = "https://wsrv.nl/?url="
    private val aio_meta = "https://aiometadata.elfhosted.com/stremio/9197a4a9-2f5b-4911-845e-8704c520bdf7"

    override val mainPage = mainPageOf(
        "/movies/trending/today?limit=$mediaLimit&extended=overview" to "Trending Movies Today",
        "/tv/trending/today?limit=$mediaLimit&extended=overview" to "Trending Shows Today",
        "/anime/trending/today?limit=$mediaLimit&extended=overview" to "Trending Anime",
        "/anime/airing?today?sort=rank" to "Airing Anime Today",
        "/tv/genres/all/all-types/kr/all-networks/this-year/popular-today?limit=$mediaLimit" to "Trending Korean Shows",
        "/movies/genres/all/all-types/all-countries/this-year/rank?limit=$mediaLimit" to "Top Rated Movies This Year",
        "/tv/genres/all/all-types/all-countries/all-networks/this-year/rank?limit=$mediaLimit" to "Top Rated Shows This Year",
        "/tv/genres/all/all-types/all-countries/netflix/all-years/popular-today?limit=$mediaLimit" to "Trending Netflix Shows",
        "/tv/genres/all/all-types/all-countries/disney/all-years/popular-today?limit=$mediaLimit" to "Trending Disney Shows",
        "/tv/genres/all/all-types/all-countries/hbo/all-years/popular-today?limit=$mediaLimit" to "Trending HBO Shows",
        "/tv/genres/all/all-types/all-countries/appletv/all-years/popular-today?limit=$mediaLimit" to "Trending Apple TV+ Shows",
        "/movies/genres/all/all-types/all-countries/this-year/revenue?limit=$mediaLimit" to "Box Office Hits This Year",
        "/movies/genres/all/all-types/all-countries/all-years/rank?limit=$mediaLimit" to "Top Rated Movies",
        "/tv/genres/all/all-types/all-countries/all-networks/all-years/rank?limit=$mediaLimit" to "Top Rated Shows",
        "/anime/genres/all/all-types/all-countries/all-networks/all-years/rank?limit=$mediaLimit" to "Top Rated Anime",
        "/tv/genres/all/all-types/kr/all-networks/all-years/rank?limit=$mediaLimit" to "Top Rated Korean Shows",
        "/movies/genres/all/all-countries/all-years/most-anticipated?limit=$mediaLimit" to "Most Anticipated Movies",
        "/anime/premieres/soon?type=all&limit=$mediaLimit" to "Upcoming Anime",
        "Personal" to "Personal",
    )

    private fun getSimklId(url: String): String {
        return url.split('/')
            .filter { part -> part.toIntOrNull() != null } // Keep only numeric parts
            .firstOrNull() ?: "" // Take the first numeric ID found
    }

    private suspend fun getExternalIds(id: Int? = null) : String?{
        val url = "$haglund_url/ids?source=myanimelist&id=$id"
        val json = app.get(url).text
        return tryParseJson<ExtenalIds>(json)?.imdb
    }

    private fun getStatus(status: String?): ShowStatus? {
        return when (status) {
            "airing" -> ShowStatus.Ongoing
            "ended" -> ShowStatus.Completed
            else -> null
        }
    }

    private suspend fun extractMetaAIO(malId: Int): JSONObject? {
        return try {
            val jsonString = app.get("$aio_meta/meta/series/mal%3A${malId}.json").text
            val root = JSONObject(jsonString)
            root.optJSONObject("meta")
        } catch (e: Exception) {
            null
        }
    }

   private suspend fun extractImdbInfo(
        kitsuId: String? = null,
        season: Int? = null,
        episode: Int? = null
    ): Triple<String?, Int?, Int?>? {
        return try {
            if (kitsuId == null) return null

            val response = app.get("$kitsuAPI/meta/series/kitsu:$kitsuId.json")
            if (!response.isSuccessful) {
                return null
            }

            val jsonString = response.text
            val rootObject = JSONObject(jsonString)
            val metaObject = rootObject.optJSONObject("meta") ?: return null

            val imdbId = metaObject.optString("imdb_id").takeIf { it.isNotBlank() }

            if (episode == null) {
                return Triple(imdbId, null, null)
            }

            val videosArray = metaObject.optJSONArray("videos") ?: return Triple(imdbId, null, null)

            for (i in 0 until videosArray.length()) {
                val videoObject = videosArray.optJSONObject(i) ?: continue
                if (videoObject.optInt("episode") == episode) {
                    val imdbSeason = videoObject.optInt("imdbSeason").takeIf { it > 0 }
                    val imdbEpisode = videoObject.optInt("imdbEpisode").takeIf { it > 0 }
                    return Triple(imdbId, imdbSeason, imdbEpisode)
                }
            }

            Triple(imdbId, season, episode)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun extractNameAndTMDBId(imdbId: String? = null): Triple<String?, Int?, Int?> {
        return try {
            if (imdbId.isNullOrBlank()) return Triple(null, null, null)

            val response = app.get("$cinemetaAPI/meta/series/$imdbId.json")
            if (!response.isSuccessful) {
                return Triple(null, null, null)
            }

            val jsonString = response.text
            val rootObject = JSONObject(jsonString)
            val metaObject = rootObject.optJSONObject("meta")

            val name = metaObject?.optString("name")?.takeIf { it.isNotBlank() }
            val moviedbId = metaObject?.optInt("moviedb_id", -1)?.takeIf { it != -1 }
            val year = metaObject?.optString("year")?.substringBefore("-")?.toIntOrNull()
                    ?: metaObject?.optString("year")?.substringBefore("–")?.toIntOrNull()
                    ?: metaObject?.optString("year")?.toIntOrNull()

            Triple(name, moviedbId, year)
        } catch (e: Exception) {
            Triple(null, null, null)
        }
    }

    private fun getPosterUrl(
        id: String? = null,
        type: String,
     ): String? {
        val baseUrl = "${image_proxy}https://simkl.in"
        if(id == null) {
            return null
        } else if(type == "imdb:lg") {
            return "${image_proxy}https://live.metahub.space/logo/large/$id/img"
        } else if(type == "episode") {
            return "$baseUrl/episodes/${id}_w.webp"
        } else if(type == "poster") {
            return "$baseUrl/posters/${id}_m.webp"
        } else if(type == "imdb:bg") {
            return "${image_proxy}https://images.metahub.space/background/large/$id/img"
        } else if(type == "youtube") {
            return "https://img.youtube.com/vi/${id}/maxresdefault.jpg"
        } else {
            return "$baseUrl/fanart/${id}_medium.webp"
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? = coroutineScope {

        suspend fun fetchResults(type: String): List<SearchResponse> {
            val result = runCatching {
                val json = app.get("$apiUrl/search/$type?q=$query&page=$page&limit=$mediaLimit&extended=full&client_id=$auth", headers = headers).text
                parseJson<Array<SimklResponse>>(json).map {
                    val allratings = it.ratings
                    val score = allratings?.mal?.rating ?: allratings?.imdb?.rating
                    newMovieSearchResponse("${it.title_en ?: it.title}", "$mainUrl/tv/${it.ids?.simkl_id}") {
                        posterUrl = getPosterUrl(it.poster, "poster")
                        this.score = Score.from10(score)
                    }
                }
            }.getOrDefault(emptyList())

            if (result.isNotEmpty()) return result
            return emptyList()
        }

        val types = listOf("movie", "tv", "anime")
        val resultsLists = types.map {
            async { fetchResults(it) }
        }.awaitAll()

        val maxSize = resultsLists.maxOfOrNull { it.size } ?: 0

        val combinedList: List<SearchResponse> = buildList {
            for (i in 0 until maxSize) {
                for (list in resultsLists) {
                    if (i < list.size) add(list[i])
                }
            }
        }

        newSearchResponseList(combinedList, true)
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (request.name.contains("Personal")) {
            // Reading and manipulating personal library
            repo.authUser()
                    ?: return newHomePageResponse(
                            "Login required for personal content.",
                            emptyList<SearchResponse>(),
                            false
                    )
            var homePageList =
                    repo.library()?.getOrThrow()?.allLibraryLists?.mapNotNull {
                        if (it.items.isEmpty()) return@mapNotNull null
                        val libraryName =
                                it.name.asString(activity ?: return@mapNotNull null)
                        HomePageList("${request.name}: $libraryName", it.items)
                    }
                            ?: return null
            return newHomePageResponse(homePageList, false)
        } else {

             val data = app.get(apiUrl + request.data + "&client_id=$auth&page=$page", headers = headers)
                .parsedSafe<Array<SimklResponse>>()?.mapNotNull {
                    val allratings = it.ratings
                    val score = allratings?.mal?.rating ?: allratings?.imdb?.rating
                    newMovieSearchResponse("${it.title}", "$mainUrl/tv/${it.ids?.simkl_id}") {
                        this.posterUrl = getPosterUrl(it.poster, "poster")
                        this.score = Score.from10(score)
                    }
                } ?: return null

            return newHomePageResponse(
                list = HomePageList(
                    name = request.name,
                    list = data,
                ),
                hasNext = if(request.data.contains("limit=")) true else false
            )
        }
    }

     override suspend fun load(url: String): LoadResponse {
        val simklId = getSimklId(url)
        val jsonString = app.get("$apiUrl/tv/$simklId?client_id=$auth&extended=full", headers = headers).text
        val json = parseJson<SimklResponse>(jsonString)
        val genres = json.genres?.map { it.toString() }
        val tvType = json.type.orEmpty()
        val country = json.country.orEmpty()
        val isAnime = tvType == "anime"
        val isBollywood = country == "IN"
        val isCartoon = genres?.contains("Animation") == true
        val isAsian = !isAnime && country in listOf("JP", "KR", "CN")
        val ids = json.ids
        val allRatings = json.ratings
        val rating = allRatings?.mal?.rating ?: allRatings?.imdb?.rating
        val kitsuId = ids?.kitsu
        val anilistId = ids?.anilist?.toIntOrNull()
        val malId = ids?.mal?.toIntOrNull()
        val tmdbId = ids?.tmdb?.toIntOrNull()
        val imdbId = ids?.imdb

        val aio_meta = if(malId != null) extractMetaAIO(malId) else null

        val enTitle =
            aio_meta?.optString("name", null)
            ?: json.en_title
            ?: json.title

        val plot = if(anilistId != null) {
            null
        } else {
            json.overview
        }

        val logo = imdbId?.let { getPosterUrl(it, "imdb:lg") }
        val firstTrailerId = json.trailers?.firstOrNull()?.youtube
        val trailerLink = firstTrailerId?.let { "https://www.youtube.com/watch?v=$it" }
        val backgroundPosterUrl =
            getPosterUrl(json.fanart, "fanart")
            ?: aio_meta?.optString("background", null)
            ?: getPosterUrl(imdbId, "imdb:bg")
            ?: getPosterUrl(firstTrailerId, "youtube")

        val users_recommendations = json.users_recommendations?.map {
            newMovieSearchResponse("${it.en_title ?: it.title}", "$mainUrl/tv/${it.ids?.simkl}") {
                this.posterUrl = getPosterUrl(it.poster, "poster")
            }
        } ?: emptyList()

        val relations = json.relations?.map {
            newMovieSearchResponse("(${it.relation_type?.replaceFirstChar { it.uppercase() }})${it.en_title ?: it.title}", "$mainUrl/tv/${it.ids?.simkl}") {
                this.posterUrl = getPosterUrl(it.poster, "poster")
            }
        } ?: emptyList()

        val recommendations = relations + users_recommendations

        val imdbType = if (tvType == "show") "series" else tvType
        val cast = parseCastData(imdbType, imdbId)

        if (tvType == "movie" || (tvType == "anime" && json.anime_type?.equals("movie") == true)) {
            val data = LoadLinksData(
                json.title,
                enTitle,
                tvType,
                simklId?.toIntOrNull(),
                imdbId,
                tmdbId,
                json.year,
                anilistId,
                malId,
                kitsuId,
                null,
                null,
                null,
                null,
                isAnime,
                isBollywood,
                isAsian,
                isCartoon
            ).toJson()
            return newMovieLoadResponse("${enTitle}", url, if(isAnime) TvType.AnimeMovie  else TvType.Movie, data) {
                this.posterUrl = getPosterUrl(json.poster, "poster")
                this.backgroundPosterUrl = backgroundPosterUrl
                this.plot = plot
                this.tags = genres
                this.comingSoon = isUpcoming(json.released)
                this.duration = json.runtime?.toIntOrNull()
                this.score = Score.from10(rating)
                this.year = json.year
                this.actors = cast
                try { this.logoUrl = logo} catch(_:Throwable){}
                this.recommendations = recommendations
                this.contentRating = json.certification
                this.addSimklId(simklId.toInt())
                this.addAniListId(anilistId)
                this.addMalId(malId)
                this.addTrailer(trailerLink)
            }
        } else {
            val epsJson = app.get("$apiUrl/tv/episodes/$simklId?client_id=$auth&extended=full", headers = headers).text
            val eps = parseJson<Array<Episodes>>(epsJson)
            val episodes = eps.filter { it.type != "special" }.map {
                newEpisode(
                    LoadLinksData(
                        json.title,
                        enTitle,
                        tvType,
                        simklId?.toIntOrNull(),
                        imdbId,
                        tmdbId,
                        json.year,
                        anilistId,
                        malId,
                        kitsuId,
                        json.season?.toIntOrNull(),
                        it.season,
                        it.episode,
                        it.date.toString().substringBefore("-").toIntOrNull(),
                        isAnime,
                        isBollywood,
                        isAsian,
                        isCartoon
                    ).toJson()
                ) {
                    this.name = it.title + if(it.aired == false) " • [UPCOMING]" else ""
                    this.season = it.season
                    this.episode = it.episode
                    this.description = it.description
                    this.posterUrl = getPosterUrl(it.img, "episode") ?: "https://github.com/SaurabhKaperwan/Utils/raw/refs/heads/main/missing_thumbnail.png"
                    addDate(it.date, "yyyy-MM-dd'T'HH:mm:ss")
                }
            }

            return newAnimeLoadResponse("${enTitle}", url, if(tvType == "anime") TvType.Anime else TvType.TvSeries) {
                addEpisodes(DubStatus.Subbed, episodes)
                this.posterUrl = getPosterUrl(json.poster, "poster")
                this.backgroundPosterUrl = backgroundPosterUrl
                this.plot = plot
                this.tags = genres
                this.duration = json.runtime?.toIntOrNull()
                this.score = Score.from10(rating)
                this.year = json.year
                try { this.logoUrl = logo} catch(_:Throwable){}
                this.actors = cast
                this.showStatus = getStatus(json.status)
                this.recommendations = recommendations
                this.contentRating = json.certification
                this.addSimklId(simklId.toInt())
                this.addAniListId(anilistId)
                this.addMalId(malId)
                this.addTrailer(trailerLink)
            }
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = parseJson<LoadLinksData>(data)
        if(res.isAnime) {
            runAnimeInvokers(res, subtitleCallback, callback)
        } else {
            invokeAllSources(
                AllLoadLinksData(
                    res.title,
                    res.imdbId,
                    res.tmdbId,
                    res.anilistId,
                    res.malId,
                    res.kitsuId,
                    res.year,
                    res.airedYear,
                    res.season,
                    res.episode,
                    res.isAnime,
                    res.isBollywood,
                    res.isAsian,
                    res.isCartoon,
                    null,
                    null,
                    null,
                    null,
                    null,
                ),
                subtitleCallback,
                callback
            )
        }
        return true
    }

    private suspend fun runAnimeInvokers(
        res: LoadLinksData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        var (imdbId, imdbSeason, imdbEpisode) = try {
            if (res.imdbId != null) {
                Triple(res.imdbId, res.imdbSeason ?: res.season, res.episode)
            } else {
                extractImdbInfo(res.kitsuId, res.imdbSeason ?: res.season, res.episode) ?: Triple(null, null, null)
            }
        } catch (e: Exception) {
            Triple(null, null, null)
        }

        if(imdbId == null)  {
            imdbId = getExternalIds(res.malId)
            imdbSeason = res.imdbSeason
            imdbEpisode = res.episode
        }

        val (imdbTitle, tmdbId, imdbYear) = try {
            extractNameAndTMDBId(imdbId) ?: Triple(res.en_title, res.tmdbId, res.year)
        } catch (e: Exception) {
            Triple(res.en_title, res.tmdbId, res.year)
        }

        invokeAllAnimeSources(
            AllLoadLinksData(
                res.title,
                imdbId,
                tmdbId,
                res.anilistId,
                res.malId,
                res.kitsuId,
                res.year,
                res.airedYear,
                res.season,
                res.episode,
                res.isAnime,
                res.isBollywood,
                res.isAsian,
                res.isCartoon,
                null,
                imdbTitle,
                imdbSeason,
                imdbEpisode,
                imdbYear,
            ),
            subtitleCallback,
            callback
        )
    }

    data class SimklResponse (
        var title                 : String?                          = null,
        var en_title              : String?                          = null,
        var title_en              : String?                          = null,
        var year                  : Int?                             = null,
        var released              : String?                          = null,
        var type                  : String?                          = null,
        var url                   : String?                          = null,
        var poster                : String?                          = null,
        var fanart                : String?                          = null,
        var ids                   : Ids?                             = Ids(),
        var release_date          : String?                          = null,
        var ratings               : Ratings?                         = Ratings(),
        var country               : String?                          = null,
        var certification         : String?                          = null,
        var runtime               : String?                          = null,
        var status                : String?                          = null,
        var total_episodes        : Int?                             = null,
        var network               : String?                          = null,
        var overview              : String?                          = null,
        var anime_type            : String?                          = null,
        var season                : String?                          = null,
        var endpoint_type         : String?                          = null,
        var genres                : ArrayList<String>?               = null,
        var users_recommendations : ArrayList<UsersRecommendations>? = null,
        var relations             : ArrayList<Relations>?            = null,
        var trailers              : ArrayList<Trailers>?             = null,
    )

    data class Trailers (
        var name    : String? = null,
        var youtube : String? = null,
    )
    data class Ids (
        var simkl_id : Int?    = null,
        var tmdb     : String? = null,
        var imdb     : String? = null,
        var slug     : String? = null,
        var mal      : String? = null,
        var anilist  : String? = null,
        var kitsu    : String? = null,
        var anidb    : String? = null,
        var simkl    : Int?    = null
    )

    data class Ratings (
        var simkl : Simkl? = Simkl(),
        var imdb  : Imdb?  = Imdb(),
        var mal   : Mal?   = Mal()
    )

    data class Simkl (
        var rating : Double? = null,
        var votes  : Int?    = null
    )

    data class Imdb (
        var rating : Double? = null,
        var votes  : Int?    = null
    )

    data class Mal (
        var rating : Double? = null,
        var votes  : Int?    = null
    )

    data class UsersRecommendations (
        var title        : String? = null,
        var en_title     : String?  = null,
        var year         : Int?    = null,
        var poster       : String? = null,
        var type         : String? = null,
        var ids          : Ids     = Ids()
    )

    data class Relations (
        var title         : String?  = null,
        var en_title      : String?  = null,
        var poster        : String?  = null,
        var anime_type    : String?  = null,
        var relation_type : String?  = null,
        var ids           : Ids     = Ids()
    )

    data class Episodes (
        var title       : String?  = null,
        var season      : Int?     = null,
        var episode     : Int?     = null,
        var type        : String?  = null,
        var description : String?  = null,
        var aired       : Boolean  = false,
        var img         : String?  = null,
        var date        : String?  = null,
    )
    data class LoadLinksData(
        val title       : String? = null,
        val en_title    : String? = null,
        val tvtype      : String? = null,
        val simklId     : Int?    = null,
        val imdbId      : String? = null,
        val tmdbId      : Int?    = null,
        val year        : Int?    = null,
        val anilistId   : Int?    = null,
        val malId       : Int?    = null,
        val kitsuId     : String? = null,
        val imdbSeason  : Int?    = null,
        val season      : Int?    = null,
        val episode     : Int?    = null,
        val airedYear   : Int?    = null,
        val isAnime     : Boolean = false,
        val isBollywood : Boolean = false,
        val isAsian     : Boolean = false,
        val isCartoon   : Boolean = false
    )

    data class ExtenalIds(
        val imdb     : String? = null,
    )
}
