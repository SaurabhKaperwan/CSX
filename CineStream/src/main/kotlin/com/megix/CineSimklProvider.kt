package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addSimklId
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.CommonActivity.activity
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.runAllAsync
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import com.megix.CineStreamExtractors.invokeVegamovies
import com.megix.CineStreamExtractors.invokeMoviesmod
import com.megix.CineStreamExtractors.invokeTopMovies
import com.megix.CineStreamExtractors.invokeMoviesdrive
import com.megix.CineStreamExtractors.invokeW4U
import com.megix.CineStreamExtractors.invokeWYZIESubs
import com.megix.CineStreamExtractors.invokeAnizone
// import com.megix.CineStreamExtractors.invokeVidbinge
import com.megix.CineStreamExtractors.invokeUhdmovies
// import com.megix.CineStreamExtractors.invokeRar
import com.megix.CineStreamExtractors.invokeAnimes
import com.megix.CineStreamExtractors.invokeMultimovies
import com.megix.CineStreamExtractors.invokeStreamify
import com.megix.CineStreamExtractors.invokeCinemaluxe
import com.megix.CineStreamExtractors.invokeBollyflix
import com.megix.CineStreamExtractors.invokeTorrentio
import com.megix.CineStreamExtractors.invokeTokyoInsider
import com.megix.CineStreamExtractors.invokeAllanime
import com.megix.CineStreamExtractors.invokeStreamAsia
import com.megix.CineStreamExtractors.invokeNetflix
import com.megix.CineStreamExtractors.invokePrimeVideo
import com.megix.CineStreamExtractors.invokeDisney
// import com.megix.CineStreamExtractors.invokeFlixhq
import com.megix.CineStreamExtractors.invokeSkymovies
import com.megix.CineStreamExtractors.invokeMoviesflix
import com.megix.CineStreamExtractors.invokeHdmovie2
import com.megix.CineStreamExtractors.invokeHindmoviez
import com.megix.CineStreamExtractors.invokeMostraguarda
import com.megix.CineStreamExtractors.invokePlayer4U
import com.megix.CineStreamExtractors.invokePrimeWire
import com.megix.CineStreamExtractors.invokeProtonmovies
import com.megix.CineStreamExtractors.invokeThepiratebay
import com.megix.CineStreamExtractors.invokeTom
import com.megix.CineStreamExtractors.invokeAllmovieland
import com.megix.CineStreamExtractors.invoke4khdhub
// import com.megix.CineStreamExtractors.invokeVidJoy
import com.megix.CineStreamExtractors.invokeMovies4u
import com.megix.CineStreamExtractors.invokeSoaper
import com.megix.CineStreamExtractors.invokeAsiaflix
import com.megix.CineStreamExtractors.invoke2embed
import com.megix.CineStreamExtractors.invokePrimebox
import com.megix.CineStreamExtractors.invokePrimenet
import com.megix.CineStreamExtractors.invokeAnimeparadise
import com.megix.CineStreamExtractors.invokeGojo
import com.megix.CineStreamExtractors.invokeSudatchi
import com.megix.CineStreamExtractors.invokePhoenix
import com.megix.CineStreamExtractors.invokeKatMovieHd
import com.megix.CineStreamExtractors.invokeMadplay
import com.megix.CineStreamExtractors.invokeStremioSubtitles
import com.megix.CineStreamExtractors.invokeToonstream
import com.megix.CineStreamExtractors.invokeDramadrip

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
    private val apiUrl = "https://api.simkl.com"
    private final val mediaLimit = 30
    private val auth = BuildConfig.SIMKL_API
    private val headers = mapOf("Content-Type" to "application/json")
    private val api = AccountManager.simklApi
    private val kitsuAPI = "https://anime-kitsu.strem.fun"
    private val cinemetaAPI = "https://v3-cinemeta.strem.io"
    private val haglund_url = "https://arm.haglund.dev/api/v2"

    override val mainPage = mainPageOf(
        "/movies/genres/all/all-types/all-countries/this-year/popular-this-week?limit=$mediaLimit&page=" to "Trending Movies This Week",
        "/tv/genres/all/all-types/all-countries/this-year/popular-today?limit=$mediaLimit&page=" to "Trending Shows Today",
        "/anime/airing?date?sort=popularity" to "Airing Anime Today",
        "/anime/genres/all/this-year/popular-today?limit=$mediaLimit&page=" to "Trending Anime",
        "/tv/genres/all/all-types/kr/all-networks/this-year/popular-this-week?limit=$mediaLimit&page=" to "Trending Korean Shows This Week",
        "/tv/genres/all/all-types/all-countries/netflix/all-years/popular-today?limit=$mediaLimit&page=" to "Trending Netflix Shows",
        "/tv/genres/all/all-types/all-countries/disney/all-years/popular-today?limit=$mediaLimit&page=" to "Trending Disney Shows",
        "/tv/genres/all/all-types/all-countries/hbo/all-years/popular-today?limit=$mediaLimit&page=" to "Trending HBO Shows",
        "/anime/premieres/soon?type=all&limit=$mediaLimit&page=" to "Upcoming Anime",
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

   private suspend fun extractImdbInfo(
        kitsuId: Int? = null,
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

    private suspend fun extractNameAndTMDBId(imdbId: String? = null): Pair<String?, Int?> {
        return try {
            if (imdbId.isNullOrBlank()) return Pair(null, null)

            val response = app.get("$cinemetaAPI/meta/series/$imdbId.json")
            if (!response.isSuccessful) {
                return Pair(null, null)
            }

            val jsonString = response.text
            val rootObject = JSONObject(jsonString)
            val metaObject = rootObject.optJSONObject("meta")

            val name = metaObject?.optString("name")?.takeIf { it.isNotBlank() }
            val moviedbId = metaObject?.optInt("moviedb_id", -1)?.takeIf { it != -1 }

            Pair(name, moviedbId)
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    private fun getPosterUrl(
        url: String? = null,
        type: String,
     ): String? {
        val baseUrl = "https://simkl.in"
        if(url == null) {
            return null
        } else if(type == "episode") {
            return "$baseUrl/episodes/${url}_w.webp"
        } else if(type == "poster") {
            return "$baseUrl/posters/${url}_m.webp"
        } else if(type == "youtube") {
            return "https://img.youtube.com/vi/${url}/maxresdefault.jpg"
        } else {
            return "$baseUrl/fanart/${url}_medium.webp"
        }
    }

    private fun normalizeSeasonString(input: String?): String? {
        if(input == null) return null
        val seasonRegex = """(?i)(season\s+\d+)""".toRegex()
        val matches = seasonRegex.findAll(input).toList()
        if (matches.isEmpty()) return input

        val firstSeason = matches.first().value
        val seasonNumber = """\d+""".toRegex().find(firstSeason)?.value ?: return input
        val normalizedSeason = "Season $seasonNumber"

        return input.replace(seasonRegex, normalizedSeason)
            .replace("$normalizedSeason(?:\\s+$normalizedSeason)+".toRegex(), normalizedSeason)
    }

    override suspend fun search(query: String): List<SearchResponse> = coroutineScope {

        suspend fun fetchResults(type: String): List<SearchResponse> {
            val result = runCatching {
                val json = app.get("$apiUrl/search/$type?q=$query&page=1&limit=$mediaLimit&extended=full&client_id=$auth", headers = headers).text
                parseJson<Array<SimklResponse>>(json).map {
                    newMovieSearchResponse("${it.title_en ?: it.title}", "$mainUrl${it.url}") {
                        posterUrl = getPosterUrl(it.poster, "poster")
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

        buildList {
            for (i in 0 until maxSize) {
                for (list in resultsLists) {
                    if (i < list.size) add(list[i])
                }
            }
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (request.name.contains("Personal")) {
            // Reading and manipulating personal library
            api.loginInfo()
                    ?: return newHomePageResponse(
                            "Login required for personal content.",
                            emptyList<SearchResponse>(),
                            false
                    )
            var homePageList =
                    api.getPersonalLibrary()?.allLibraryLists?.mapNotNull {
                        if (it.items.isEmpty()) return@mapNotNull null
                        val libraryName =
                                it.name.asString(activity ?: return@mapNotNull null)
                        HomePageList("${request.name}: $libraryName", it.items)
                    }
                            ?: return null
            return newHomePageResponse(homePageList, false)
        } else {
            val jsonString = app.get(apiUrl + request.data + page, headers = headers).text
            val json = parseJson<Array<SimklResponse>>(jsonString)
            val data = json.map {
                newMovieSearchResponse("${it.title}", "$mainUrl${it.url}") {
                    this.posterUrl = getPosterUrl(it.poster, "poster")
                }
            }

            return newHomePageResponse(
                list = HomePageList(
                    name = request.name,
                    list = data,
                ),
                hasNext = if(request.data.contains("page=")) true else false
            )
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val simklId = getSimklId(url)
        val jsonString = app.get("$apiUrl/tv/$simklId?extended=full", headers = headers).text
        val json = parseJson<SimklResponse>(jsonString)
        val genres = json.genres?.map { it.toString() }
        val tvType = json.type ?: ""
        val country = json.country ?: ""
        val isAnime = if(tvType == "anime") true else false
        val isBollywood = if(country == "IN") true else false
        val isCartoon = genres?.contains("Animation") ?: false
        val isAsian = if(!isAnime && (country == "JP" || country == "KR" || country == "CN")) true else false
        val en_title = if (isAnime) {
            normalizeSeasonString(json.en_title ?: json.title)
        } else {
            json.en_title ?: json.title
        }
        val allratings = json.ratings
        val rating = allratings?.mal?.rating ?: allratings?.imdb?.rating
        val kitsuId = json.ids?.kitsu?.toIntOrNull()
        val firstTrailerId = json.trailers?.firstOrNull()?.youtube
        val backgroundPosterUrl = getPosterUrl(json.fanart, "fanart") ?: getPosterUrl(firstTrailerId, "youtube")

        val users_recommendations = json.users_recommendations?.map {
            newMovieSearchResponse("${it.en_title ?: it.title}", "$mainUrl/${it.type}/${it.ids?.simkl}/${it.ids?.slug}") {
                this.posterUrl = getPosterUrl(it.poster, "poster")
            }
        } ?: emptyList()

        val relations = json.relations?.map {
            newMovieSearchResponse("(${it.relation_type?.replaceFirstChar { it.uppercase() }})${it.en_title ?: it.title}", "$mainUrl/${it.anime_type}/${it.ids?.simkl}/${it.ids?.slug}") {
                this.posterUrl = getPosterUrl(it.poster, "poster")
            }
        } ?: emptyList()

        val recommendations = relations + users_recommendations

        if (tvType == "movie" || (tvType == "anime" && json.anime_type?.equals("movie") == true)) {
            val data = LoadLinksData(
                json.title,
                en_title,
                tvType,
                simklId?.toIntOrNull(),
                json.ids?.imdb,
                json.ids?.tmdb?.toIntOrNull(),
                json.year,
                json.ids?.anilist?.toIntOrNull(),
                json.ids?.mal?.toIntOrNull(),
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
            return newMovieLoadResponse("${en_title}", url, if(isAnime) TvType.AnimeMovie  else TvType.Movie, data) {
                this.posterUrl = getPosterUrl(json.poster, "poster")
                this.backgroundPosterUrl = backgroundPosterUrl
                this.plot = json.overview
                this.tags = genres
                this.duration = json.runtime?.toIntOrNull()
                this.rating = rating.toString().toRatingInt()
                this.year = json.year
                this.recommendations = recommendations
                this.contentRating = json.certification
                this.addSimklId(simklId.toInt())
                this.addAniListId(json.ids?.anilist?.toIntOrNull())
            }
        } else {
            val epsJson = app.get("$apiUrl/tv/episodes/$simklId?extended=full", headers = headers).text
            val eps = parseJson<Array<Episodes>>(epsJson)
            val episodes = eps.filter { it.type != "special" }.map {
                newEpisode(
                    LoadLinksData(
                        json.title,
                        en_title,
                        tvType,
                        simklId?.toIntOrNull(),
                        json.ids?.imdb,
                        json.ids?.tmdb?.toIntOrNull(),
                        json.year,
                        json.ids?.anilist?.toIntOrNull(),
                        json.ids?.mal?.toIntOrNull(),
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
                    this.name = it.title
                    this.season = it.season
                    this.episode = it.episode
                    this.description = it.description
                    this.posterUrl = getPosterUrl(it.img, "episode") ?: "https://simkl.in/update_m_alert.jpg"
                    addDate(it.date)
                }
            }

            return newAnimeLoadResponse("${en_title}", url, if(tvType == "anime") TvType.Anime else TvType.TvSeries) {
                addEpisodes(DubStatus.Subbed, episodes)
                this.posterUrl = getPosterUrl(json.poster, "poster")
                this.backgroundPosterUrl = backgroundPosterUrl
                this.plot = json.overview
                this.tags = genres
                this.duration = json.runtime?.toIntOrNull()
                this.rating = rating.toString().toRatingInt()
                this.year = json.year
                this.recommendations = recommendations
                this.contentRating = json.certification
                this.addSimklId(simklId.toInt())
                this.addAniListId(json.ids?.anilist?.toIntOrNull())
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

        if(res.isAnime) runAnimeInvokers(res, subtitleCallback, callback)
        else runGeneralInvokers(res, subtitleCallback, callback)

        return true
    }

    private suspend fun runGeneralInvokers(
        res: LoadLinksData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        runAllAsync(
            { invokeTorrentio(res.imdbId, res.season, res.episode, callback) },
            { if(!res.isBollywood) invokeVegamovies("VegaMovies", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if(res.isBollywood) invokeVegamovies("RogMovies", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeNetflix(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeVideo(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokeDisney(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { if(res.season == null) invokeStreamify(res.imdbId, callback) },
            { invokeMultimovies(res.title, res.season, res.episode, subtitleCallback, callback) },
            { if(res.isCartoon) invokeToonstream(res.title, res.season, res.episode, subtitleCallback, callback) },
            { if(res.isBollywood) invokeTopMovies(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { if(!res.isBollywood) invokeMoviesmod(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if(res.isAsian) invokeDramadrip(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if(res.isAsian && res.season != null) invokeStreamAsia(res.title, "kdhd", res.season, res.episode, subtitleCallback, callback) },
            { invokeMoviesdrive(res.title, res.imdbId ,res.season, res.episode, subtitleCallback, callback) },
            { if(!res.isAnime) invokeAsiaflix(res.title, res.season, res.episode, res.airedYear, subtitleCallback, callback) },
            { invokeCinemaluxe(res.title, res.year, res.season, res.episode, callback, subtitleCallback) },
            { invokeSkymovies(res.title, res.airedYear, res.episode, subtitleCallback, callback) },
            { invokeHdmovie2(res.title, res.airedYear, res.episode, subtitleCallback, callback) },
            // { invokeFlixhq(res.title, res.season, res.episode, subtitleCallback, callback) },
            { invokeBollyflix(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeMovies4u(res.imdbId, res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { if(!res.isBollywood) invokeHindmoviez("HindMoviez", res.imdbId, res.title,res.season, res.episode, callback) },
            // { if (res.isBollywood) invokeHindmoviez("JaduMovies", res.imdbId, res.season, res.episode, callback) },
            { if (!res.isBollywood) invokeKatMovieHd("KatMovieHd", res.imdbId, res.season, res.episode, subtitleCallback ,callback) },
            { if (res.isBollywood) invokeKatMovieHd("Moviesbaba", res.imdbId, res.season, res.episode, subtitleCallback ,callback) },
            { invokeW4U(res.title, res.year, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeWYZIESubs(res.imdbId, res.season, res.episode, subtitleCallback) },
            { invokeStremioSubtitles(res.imdbId, res.season, res.episode, subtitleCallback) },
            { invokePrimebox(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeWire(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invoke2embed(res.imdbId, res.season, res.episode, callback) },
            { invokeMadplay(res.tmdbId, res.season, res.episode, callback) },
            { invokeSoaper(res.imdbId, res.tmdbId, res.title, res.season, res.episode, subtitleCallback, callback) },
            { invokePhoenix(res.title, res.imdbId, res.tmdbId, res.year, res.season, res.episode, callback) },
            { invokeTom(res.tmdbId, res.season, res.episode, callback, subtitleCallback) },
            { invokePrimenet(res.tmdbId, res.season, res.episode, callback) },
            { invokePlayer4U(res.title, res.season, res.episode, res.year, callback) },
            { invokeThepiratebay(res.imdbId, res.season, res.episode, callback) },
            // { if (!res.isAnime) invokeVidJoy(res.tmdbId, res.season, res.episode, callback) },
            { invokeProtonmovies(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeAllmovieland(res.imdbId, res.season, res.episode, callback) },
            { if(res.season == null) invokeMostraguarda(res.imdbId, subtitleCallback, callback) },
            { if(!res.isBollywood ) invokeMoviesflix("Moviesflix", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if(res.isBollywood) invokeMoviesflix("Hdmoviesflix", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if(!res.isBollywood) invokeUhdmovies(res.title, res.year, res.season, res.episode, callback, subtitleCallback) },
            { if(!res.isBollywood) invoke4khdhub(res.title, res.year, res.season, res.episode, subtitleCallback, callback) }
        )
    }

    private suspend fun runAnimeInvokers(
        res: LoadLinksData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        var (imdbId, imdbSeason, imdbEpisode) = try {
            if (res.imdbId != null) {
                Triple(res.imdbId, res.season, res.episode)
            } else {
                extractImdbInfo(res.kitsuId, res.season, res.episode) ?: Triple(null, null, null)
            }
        } catch (e: Exception) {
            Triple(null, null, null)
        }

        if(imdbId == null)  {
            imdbId = getExternalIds(res.malId)
            imdbSeason = res.imdbSeason
            imdbEpisode = res.episode
        }

        val (imdbTitle, tmdbId) = try {
            extractNameAndTMDBId(imdbId) ?: Pair(res.en_title, res.tmdbId)
        } catch (e: Exception) {
            Pair(res.en_title, res.tmdbId)
        }

        runAllAsync(
            { invokeAnimes(res.malId, res.anilistId, res.episode, res.year, "kitsu", subtitleCallback, callback) },
            { invokeSudatchi(res.anilistId, res.episode, subtitleCallback, callback) },
            { invokeGojo(res.anilistId, res.episode, subtitleCallback ,callback) },
            { invokeAnimeparadise(res.title, res.malId, res.episode, subtitleCallback, callback) },
            { invokeAllanime(res.title, res.year, res.episode, subtitleCallback, callback) },
            { invokeAnizone(res.title, res.episode, subtitleCallback, callback) },
            { invokeTokyoInsider(res.title, res.episode, subtitleCallback, callback) },
            { invokeTorrentio(imdbId, imdbSeason, imdbEpisode, callback) },
            { invokeVegamovies("VegaMovies", imdbId, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokeNetflix(imdbTitle, res.year, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokePrimeVideo(imdbTitle, imdbSeason, imdbEpisode, res.episode, subtitleCallback, callback) },
            { invokeMultimovies(imdbTitle, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokeToonstream(imdbTitle, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokeMoviesmod(imdbId, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokeBollyflix(imdbId, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokeMovies4u(imdbId, imdbTitle, res.year, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokeWYZIESubs(imdbId, imdbSeason, imdbEpisode, subtitleCallback) },
            { invokeStremioSubtitles(imdbId, imdbSeason, imdbEpisode, subtitleCallback) },
            { invokePrimebox(imdbTitle, res.year, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokePrimeWire(imdbId, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokeSoaper(imdbId, tmdbId, imdbTitle, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokePhoenix(imdbTitle, imdbId, tmdbId, res.year, imdbSeason, imdbEpisode, callback) },
            { invokeTom(tmdbId, imdbSeason, imdbEpisode, callback, subtitleCallback) },
            { invokePrimenet(tmdbId, imdbSeason, imdbEpisode, callback) },
            { invokePlayer4U(imdbTitle, imdbSeason, imdbEpisode, res.year, callback) },
            { invokeThepiratebay(imdbId, imdbSeason, imdbEpisode, callback) },
            { invokeProtonmovies(imdbId, imdbSeason, imdbEpisode, subtitleCallback, callback) },
            { invokeAllmovieland(imdbId, imdbSeason, imdbEpisode, callback) },
            { invokeUhdmovies(imdbTitle, res.year, imdbSeason, imdbEpisode, callback, subtitleCallback) },
            { invoke4khdhub(imdbTitle, res.year, imdbSeason, imdbEpisode, subtitleCallback, callback) }
        )
    }

    data class SimklResponse (
        var title                 : String?                          = null,
        var en_title              : String?                          = null,
        var title_en              : String?                          = null,
        var year                  : Int?                             = null,
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
        var trailers              : ArrayList<Trailers>?             = null
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
        var aired       : Boolean? = null,
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
        val kitsuId     : Int?    = null,
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
