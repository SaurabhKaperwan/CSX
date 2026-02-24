package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.megix.CineStreamExtractors.invokeAllSources
import com.megix.CineStreamExtractors.invokeAnimes

class CineTmdbProvider: MainAPI() {
    override var name = "CineTmdb"
    override var mainUrl = "https://www.themoviedb.org"
    override var supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
        TvType.Torrent
    )
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = true
    // override val providerType = ProviderType.MetaProvider
    private val apiUrl = "https://api.themoviedb.org/3"
    private val image_proxy = "https://wsrv.nl/?url="

    companion object {
        private const val apiKey = BuildConfig.TMDB_KEY
    }

    override val mainPage = mainPageOf(
        "trending/all/day?api_key=$apiKey&region=US" to "Trending",
        "trending/movie/week?api_key=$apiKey&region=US" to "Popular Movies",
        "trending/tv/week?api_key=$apiKey&region=US" to "Popular TV Shows",
        "discover/tv?api_key=$apiKey&with_keywords=210024|222243&sort_by=popularity.desc&air_date.lte=${getDate().today}&air_date.gte=${getDate().today}" to "Airing Today Anime",
        "discover/tv?api_key=$apiKey&with_keywords=210024|222243&sort_by=popularity.desc&air_date.lte=${getDate().nextWeek}&air_date.gte=${getDate().today}" to "On The Air Anime",
        "discover/tv?api_key=$apiKey&with_original_language=ko" to "Korean Shows",
        "discover/tv?api_key=$apiKey&with_networks=213" to "Netflix",
        "discover/tv?api_key=$apiKey&with_networks=1024" to "Amazon",
        "discover/tv?api_key=$apiKey&with_networks=2739" to "Disney+",
        "discover/tv?api_key=$apiKey&with_watch_providers=2336&watch_region=IN" to "JioHotstar",
        "discover/tv?api_key=$apiKey&with_networks=453" to "Hulu",
        "discover/tv?api_key=$apiKey&with_networks=2552" to "Apple TV+",
        "discover/tv?api_key=$apiKey&with_networks=49" to "HBO",
        "discover/tv?api_key=$apiKey&with_networks=4330" to "Paramount+",
        "discover/tv?api_key=$apiKey&with_networks=3353" to "Peacock",
        "discover/movie?api_key=$apiKey&language=en-US&page=1&sort_by=popularity.desc&with_origin_country=IN&release_date.gte=${getDate().lastWeekStart}&release_date.lte=${getDate().today}" to "Trending Indian Movies",
        "discover/movie?api_key=$apiKey&with_keywords=210024|222243" to "Anime Movies",
        "movie/top_rated?api_key=$apiKey&region=US" to "Top Rated Movies",
        "tv/top_rated?api_key=$apiKey&region=US" to "Top Rated TV Shows",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val type = if (request.data.contains("/movie")) "movie" else "tv"
        val home = app.get("$apiUrl/${request.data}&page=$page", timeout = 10000)
            .parsedSafe<Results>()?.results?.mapNotNull { media ->
                media.toSearchResponse(type)
            } ?: throw ErrorLoadingException("Invalid Json reponse")
        return newHomePageResponse(request.name, home)
    }

     private fun Media.toSearchResponse(type: String? = null): SearchResponse? {
        return newMovieSearchResponse(
            title ?: name ?: originalTitle ?: return null,
            Data(id = id, type = mediaType ?: type).toJson(),
            TvType.Movie,
        ) {
            this.posterUrl = getImageUrl(posterPath)
            this.score= Score.from10(voteAverage)
        }
    }

    private fun getImageUrl(link: String?): String? {
        if (link == null) return null
        return if (link.startsWith("/")) "https://image.tmdb.org/t/p/w500/$link" else link
    }

    private fun getOriImageUrl(link: String?): String? {
        if (link == null) return null
        return if (link.startsWith("/")) "https://image.tmdb.org/t/p/original/$link" else link
    }

    fun getType(t: String?): TvType {
        return when (t) {
            "movie" -> TvType.Movie
            else -> TvType.TvSeries
        }
    }

    fun getStatus(t: String?): ShowStatus {
        return when (t) {
            "Returning Series" -> ShowStatus.Ongoing
            else -> ShowStatus.Completed
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query)

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        return app.get("$apiUrl/search/multi?api_key=$apiKey&language=en-US&query=$query&page=$page")
            .parsedSafe<Results>()?.results?.mapNotNull { media ->
                media.toSearchResponse()
            }?.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val data = parseJson<Data>(url)
        val type = getType(data.type)

        val append = "alternative_titles,credits,external_ids,videos,recommendations,content_ratings,release_dates"

        val resUrl = if (type == TvType.Movie) {
            "$apiUrl/movie/${data.id}?api_key=$apiKey&append_to_response=$append"
        } else {
            "$apiUrl/tv/${data.id}?api_key=$apiKey&append_to_response=$append"
        }

        val res = app.get(resUrl).parsedSafe<MediaDetail>()
            ?: throw ErrorLoadingException("Invalid Json Response")
        val title = res.title ?: res.name ?: return null
        val poster = getOriImageUrl(res.posterPath)
        val bgPoster = getOriImageUrl(res.backdropPath)
        val ageRating = res.usAgeRating
        val orgTitle = res.originalTitle ?: res.originalName
        val releaseDate = res.releaseDate ?: res.firstAirDate
        val year = releaseDate?.split("-")?.first()?.toIntOrNull()
        val genres = res.genres?.mapNotNull { it.name }
        val imdbId = res.external_ids?.imdb_id
        val isCartoon = genres?.contains("Animation") ?: false
        val isAnime = isCartoon && (res.original_language == "zh" || res.original_language == "ja" || res.original_language == "ko")
        val isAsian = !isAnime && (res.original_language == "zh" || res.original_language == "ko")
        val isBollywood = res.production_countries?.any { it.name == "India" } ?: false

        val keywords = res.keywords?.results?.mapNotNull { it.name }.orEmpty()
            .ifEmpty { res.keywords?.keywords?.mapNotNull { it.name } }

        val actors = res.credits?.cast?.mapNotNull { cast ->
            val name = cast.name ?: cast.originalName ?: return@mapNotNull null
            ActorData(
                Actor(name, getImageUrl(cast.profilePath)), roleString = cast.character
            )
        } ?: emptyList()

        val logo = fetchTmdbLogoUrl(
            apiUrl,
            apiKey,
            type,
            res.id,
            "en"
        )

        val recommendations =
            res.recommendations?.results?.mapNotNull { media -> media.toSearchResponse() }

        val trailer = res.videos?.results.orEmpty()
            .filter { it.type == "Trailer" }
            .map { "https://www.youtube.com/watch?v=${it.key}" }
            .reversed()
            .ifEmpty {
                res.videos?.results?.map { "https://www.youtube.com/watch?v=${it.key}" } ?: emptyList()
            }

         if (type == TvType.TvSeries) {
            val lastSeason = res.last_episode_to_air?.season_number
            val episodes = res.seasons?.filter { it.seasonNumber != 0 }?.mapNotNull { season ->
                app.get("$apiUrl/${data.type}/${data.id}/season/${season.seasonNumber}?api_key=$apiKey")
                    .parsedSafe<MediaDetailEpisodes>()?.episodes?.map { eps ->
                        newEpisode(
                            LinkData(
                                data.id,
                                res.external_ids?.imdb_id,
                                res.external_ids?.tvdb_id,
                                data.type,
                                eps.seasonNumber,
                                eps.episodeNumber,
                                eps.id,
                                title = title,
                                year = season.airDate?.split("-")?.first()?.toIntOrNull(),
                                orgTitle = orgTitle,
                                isAnime = isAnime,
                                airedYear = year,
                                lastSeason = lastSeason,
                                epsTitle = eps.name,
                                jpTitle = res.alternative_titles?.results?.find { it.iso_3166_1 == "JP" }?.title,
                                date = season.airDate,
                                airedDate = res.releaseDate ?: res.firstAirDate,
                                isAsian = isAsian,
                                isBollywood = isBollywood,
                                isCartoon = isCartoon,
                                alttitle = res.title,
                                nametitle = res.name
                            ).toJson()
                        ) {
                            this.name = eps.name
                            // eps.name + if (isUpcoming(eps.airDate)) " • [UPCOMING]" else ""
                            this.season = eps.seasonNumber
                            this.episode = eps.episodeNumber
                            this.posterUrl = getImageUrl(eps.stillPath) ?: "https://github.com/SaurabhKaperwan/Utils/raw/refs/heads/main/missing_thumbnail.png"
                            this.score = Score.from10(eps.voteAverage)
                            this.description = eps.overview
                            this.runTime = eps.runtime
                        }.apply {
                            this.addDate(eps.airDate)
                        }
                    }
            }?.flatten() ?: listOf()

            return newAnimeLoadResponse(title, url, if(isAnime) TvType.Anime else TvType.TvSeries) {
                addEpisodes(DubStatus.Subbed, episodes)
                this.posterUrl = poster
                this.backgroundPosterUrl = bgPoster
                this.year = year
                this.plot = res.overview
                this.contentRating = ageRating
                try { this.logoUrl = logo} catch(_:Throwable){}
                this.tags = keywords?.map { word -> word.replaceFirstChar { it.titlecase() } }
                    ?.takeIf { it.isNotEmpty() } ?: genres
                this.score = Score.from10(res.vote_average.toString())
                this.showStatus = getStatus(res.status)
                this.recommendations = recommendations
                this.actors = actors
                addTrailer(trailer)
                addImdbId(imdbId)
            }
        } else {
            return newMovieLoadResponse(
                title,
                url,
                if(isAnime) TvType.AnimeMovie  else TvType.Movie,
                LinkData(
                    data.id,
                    res.external_ids?.imdb_id,
                    res.external_ids?.tvdb_id,
                    data.type,
                    title = title,
                    year = year,
                    orgTitle = orgTitle,
                    isAnime = isAnime,
                    jpTitle = res.alternative_titles?.results?.find { it.iso_3166_1 == "JP" }?.title,
                    airedDate = res.releaseDate ?: res.firstAirDate,
                    isAsian = isAsian,
                    isBollywood = isBollywood,
                    alttitle = res.title,
                    nametitle = res.name
                ).toJson(),
            ) {
                this.posterUrl = poster
                this.backgroundPosterUrl = bgPoster
                // this.comingSoon = isUpcoming(releaseDate)
                this.year = year
                this.plot = res.overview
                this.duration = res.runtime
                this.contentRating = ageRating
                try { this.logoUrl = logo} catch(_:Throwable){}
                this.tags = keywords?.map { word -> word.replaceFirstChar { it.titlecase() } }
                    ?.takeIf { it.isNotEmpty() } ?: genres

                this.score = Score.from10(res.vote_average.toString())
                this.recommendations = recommendations
                this.actors = actors
                addTrailer(trailer)
                addImdbId(imdbId)
            }
        }
    }

     override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val res = parseJson<LinkData>(data)
        val year = res.airedYear ?: res.year
        val seasonYear = res.year ?: res.airedYear

        runAllAsync(
            {
                invokeAllSources(
                    AllLoadLinksData(
                        res.title,
                        res.imdbId,
                        res.id,
                        res.aniId?.toIntOrNull(),
                        null,
                        null,
                        year,
                        seasonYear,
                        res.season,
                        res.episode,
                        res.isAnime,
                        res.isBollywood,
                        res.isAsian,
                        res.isCartoon,
                        res.orgTitle,
                        null,
                        null,
                        null,
                        null,
                    ),
                    subtitleCallback,
                    callback
                )
            },
            {
                if (res.isAnime) {
                    val (aniId, malId) = convertTmdbToAnimeId(res.title, res.date, res.airedDate, if (res.season == null) TvType.AnimeMovie else TvType.Anime)
                    invokeAnimes(malId, aniId, res.episode, res.airedYear, "imdb", subtitleCallback, callback)
                }
            }
        )

        return true
    }

    data class LinkData(
        val id: Int? = null,
        val imdbId: String? = null,
        val tvdbId: Int? = null,
        val type: String? = null,
        val season: Int? = null,
        val episode: Int? = null,
        val epid: Int? = null,
        val aniId: String? = null,
        val animeId: String? = null,
        val title: String? = null,
        val year: Int? = null,
        val orgTitle: String? = null,
        val isAnime: Boolean = false,
        val airedYear: Int? = null,
        val lastSeason: Int? = null,
        val epsTitle: String? = null,
        val jpTitle: String? = null,
        val date: String? = null,
        val airedDate: String? = null,
        val isAsian: Boolean = false,
        val isBollywood: Boolean = false,
        val isCartoon: Boolean = false,
        val alttitle: String? = null,
        val nametitle: String? = null,
    )

    data class Data(
        val id: Int? = null,
        val type: String? = null,
        val aniId: String? = null,
        val malId: Int? = null,
    )

    data class Results(
        @param:JsonProperty("results") val results: ArrayList<Media>? = arrayListOf(),
    )

    data class Media(
        @param:JsonProperty("id") val id: Int? = null,
        @param:JsonProperty("name") val name: String? = null,
        @param:JsonProperty("title") val title: String? = null,
        @param:JsonProperty("original_title") val originalTitle: String? = null,
        @param:JsonProperty("media_type") val mediaType: String? = null,
        @param:JsonProperty("poster_path") val posterPath: String? = null,
        @param:JsonProperty("vote_average") val voteAverage: Double? = null,
    )

    data class Genres(
        @param:JsonProperty("id") val id: Int? = null,
        @param:JsonProperty("name") val name: String? = null,
    )

    data class Keywords(
        @param:JsonProperty("id") val id: Int? = null,
        @param:JsonProperty("name") val name: String? = null,
    )

    data class KeywordResults(
        @param:JsonProperty("results") val results: ArrayList<Keywords>? = arrayListOf(),
        @param:JsonProperty("keywords") val keywords: ArrayList<Keywords>? = arrayListOf(),
    )

    data class LastEpisodeToAir(
        @param:JsonProperty("episode_number") val episode_number: Int? = null,
        @param:JsonProperty("season_number") val season_number: Int? = null,
    )

    data class Seasons(
        @param:JsonProperty("id") val id: Int? = null,
        @param:JsonProperty("name") val name: String? = null,
        @param:JsonProperty("season_number") val seasonNumber: Int? = null,
        @param:JsonProperty("air_date") val airDate: String? = null,
    )

    data class Trailers(
        @param:JsonProperty("key") val key: String? = null,
        @param:JsonProperty("type") val type: String? = null,
    )

    data class ResultsTrailer(
        @param:JsonProperty("results") val results: ArrayList<Trailers>? = arrayListOf(),
    )

    data class ExternalIds(
        @param:JsonProperty("imdb_id") val imdb_id: String? = null,
        @param:JsonProperty("tvdb_id") val tvdb_id: Int? = null,
    )

    data class Credits(
        @param:JsonProperty("cast") val cast: ArrayList<Cast>? = arrayListOf(),
    )

    data class Cast(
        @param:JsonProperty("id") val id: Int? = null,
        @param:JsonProperty("name") val name: String? = null,
        @param:JsonProperty("original_name") val originalName: String? = null,
        @param:JsonProperty("character") val character: String? = null,
        @param:JsonProperty("known_for_department") val knownForDepartment: String? = null,
        @param:JsonProperty("profile_path") val profilePath: String? = null,
    )

    data class ResultsRecommendations(
        @param:JsonProperty("results") val results: ArrayList<Media>? = arrayListOf(),
    )

    data class AltTitles(
        @param:JsonProperty("iso_3166_1") val iso_3166_1: String? = null,
        @param:JsonProperty("title") val title: String? = null,
        @param:JsonProperty("type") val type: String? = null,
    )

    data class ResultsAltTitles(
        @param:JsonProperty("results") val results: ArrayList<AltTitles>? = arrayListOf(),
    )

    data class ProductionCountries(
        @param:JsonProperty("name") val name: String? = null,
    )

    data class MediaDetail(
        @param:JsonProperty("id") val id: Int? = null,
        @param:JsonProperty("adult") val adult: Boolean = false,
        @param:JsonProperty("imdb_id") val imdbId: String? = null,
        @param:JsonProperty("title") val title: String? = null,
        @param:JsonProperty("name") val name: String? = null,
        @param:JsonProperty("original_title") val originalTitle: String? = null,
        @param:JsonProperty("original_name") val originalName: String? = null,
        @param:JsonProperty("poster_path") val posterPath: String? = null,
        @param:JsonProperty("backdrop_path") val backdropPath: String? = null,
        @param:JsonProperty("release_date") val releaseDate: String? = null,
        @param:JsonProperty("first_air_date") val firstAirDate: String? = null,
        @param:JsonProperty("overview") val overview: String? = null,
        @param:JsonProperty("runtime") val runtime: Int? = null,
        @param:JsonProperty("vote_average") val vote_average: Any? = null,
        @param:JsonProperty("original_language") val original_language: String? = null,
        @param:JsonProperty("status") val status: String? = null,
        @param:JsonProperty("genres") val genres: ArrayList<Genres>? = arrayListOf(),
        @param:JsonProperty("keywords") val keywords: KeywordResults? = null,
        @param:JsonProperty("last_episode_to_air") val last_episode_to_air: LastEpisodeToAir? = null,
        @param:JsonProperty("seasons") val seasons: ArrayList<Seasons>? = arrayListOf(),
        @param:JsonProperty("videos") val videos: ResultsTrailer? = null,
        @param:JsonProperty("external_ids") val external_ids: ExternalIds? = null,
        @param:JsonProperty("credits") val credits: Credits? = null,
        @param:JsonProperty("recommendations") val recommendations: ResultsRecommendations? = null,
        @param:JsonProperty("alternative_titles") val alternative_titles: ResultsAltTitles? = null,
        @param:JsonProperty("production_countries") val production_countries: ArrayList<ProductionCountries>? = arrayListOf(),
        @param:JsonProperty("content_ratings") val contentRatings: ContentRatings? = null,
        @param:JsonProperty("release_dates") val releaseDates: ReleaseDates? = null
    ) {
        // HELPER PROPERTY: Instantly grabs the US Rating for both Movies and TV
        val usAgeRating: String?
            get() {
                contentRatings?.results?.firstOrNull { it.iso3166_1 == "US" }?.rating?.takeIf { it.isNotBlank() }?.let { return it }
                releaseDates?.results?.firstOrNull { it.iso3166_1 == "US" }?.releaseDates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification?.let { return it }
                return null
            }
    }

    data class Episodes(
        @param:JsonProperty("id") val id: Int? = null,
        @param:JsonProperty("name") val name: String? = null,
        @param:JsonProperty("overview") val overview: String? = null,
        @param:JsonProperty("air_date") val airDate: String? = null,
        @param:JsonProperty("still_path") val stillPath: String? = null,
        @param:JsonProperty("vote_average") val voteAverage: Double? = null,
        @param:JsonProperty("episode_number") val episodeNumber: Int? = null,
        @param:JsonProperty("season_number") val seasonNumber: Int? = null,
        @param:JsonProperty("runtime") val runtime: Int? = null,
    )

     data class MediaDetailEpisodes(
        @param:JsonProperty("episodes") val episodes: ArrayList<Episodes>? = arrayListOf(),
    )

    data class ContentRatings(
        @param:JsonProperty("results") val results: ArrayList<ContentRatingResult>? = arrayListOf()
    )

    data class ContentRatingResult(
        @param:JsonProperty("iso_3166_1") val iso3166_1: String? = null,
        @param:JsonProperty("rating") val rating: String? = null
    )

    data class ReleaseDates(
        @param:JsonProperty("results") val results: ArrayList<ReleaseDatesResult>? = arrayListOf()
    )

    data class ReleaseDatesResult(
        @param:JsonProperty("iso_3166_1") val iso3166_1: String? = null,
        @param:JsonProperty("release_dates") val releaseDates: ArrayList<ReleaseDateItem>? = arrayListOf()
    )

    data class ReleaseDateItem(
        @param:JsonProperty("certification") val certification: String? = null
    )
}
