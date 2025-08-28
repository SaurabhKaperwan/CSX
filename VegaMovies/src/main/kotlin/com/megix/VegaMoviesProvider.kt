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

open class VegaMoviesProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://vegamovies.frl"
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

    open val headers = mapOf(
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
        "cookie" to "xla=s4t",
        "Accept-Language" to "en-US,en;q=0.9",
        "sec-ch-ua" to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Microsoft Edge\";v=\"120\"",
        "sec-ch-ua-mobile" to "?0",
        "sec-ch-ua-platform" to "\"Linux\"",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "none",
        "Sec-Fetch-User" to "?1",
        "Upgrade-Insecure-Requests" to "1",
        "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/%d/" to "Home",
        "$mainUrl/web-series/netflix/page/%d/" to "Netflix",
        "$mainUrl/web-series/disney-plus-hotstar/page/%d/" to "Disney Plus Hotstar",
        "$mainUrl/web-series/amazon-prime-video/page/%d/" to "Amazon Prime",
        "$mainUrl/web-series/mx-original/page/%d/" to "MX Original",
        "$mainUrl/anime-series/page/%d/" to "Anime Series",
        "$mainUrl/korean-series/page/%d/" to "Korean Series"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(
            request.data.format(page),
            referer = mainUrl,
            headers = headers
        ).document
        val home = document.select(".post-inner.post-hover").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select("h2 > a").text().replace("Download ", "")
        val href = this.select("a").attr("href")
        var posterUrl = httpsify(this.select("img").attr("src"))

        if(posterUrl.contains("data:image")) {
            posterUrl = httpsify(this.select("img").attr("data-lazy-src"))
        }

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResults = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            try {
                val document = app.get(
                    "$mainUrl/page/$i/?s=$query",
                    referer = mainUrl,
                    headers = headers
                ).document ?: continue

                val results = document.select(".post-inner.post-hover")
                    .mapNotNull { it.toSearchResult() }

                if (results.isEmpty()) break

                searchResults.addAll(results)
            } catch (e: Exception) {
                break
            }
        }

        return searchResults
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(
            url,
            referer = mainUrl,
            headers = headers
        ).document
        var title = document.select("meta[property=og:title]").attr("content").replace("Download ", "")
        val ogTitle = title
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
            if(title != ogTitle) {
                val checkSeason = Regex("""Season\s*\d*1|S\s*\d*1""").find(ogTitle)
                if (checkSeason == null) {
                    val seasonText = Regex("""Season\s*\d+|S\s*\d+""").find(ogTitle)?.value
                    if(seasonText != null) {
                        title = title + " " + seasonText.toString()
                    }
                }
            }
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
                    val document2 = app.get(eurl, headers = headers).document
                    val vcloudRegex = Regex("""https:\/\/vcloud\.lol\/[^\s"]+""")
                    var vcloudLinks = vcloudRegex.findAll(document2.html()).mapNotNull { it.value }.toList()

                    if (vcloudLinks.isEmpty()) {
                        val fastDlRegex = Regex("""https:\/\/fastdl.icu\/embed\?download=[a-zA-Z0-9]+""")
                        vcloudLinks = fastDlRegex.findAll(document2.html()).mapNotNull { it.value }.toList()
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
                this.rating = imdbRating.toRatingInt()
                this.year = year.toIntOrNull() ?: year.substringBefore("–").toIntOrNull()
                this.backgroundPosterUrl = background
                addActors(cast)
                addImdbUrl(imdbUrl)
            }
        } else {
            val buttons = document.select("p > a:has(button)")
            val data = buttons.mapNotNull { button ->
                val link = fixUrl(button.attr("href"))
                val doc = app.get(link, headers = headers).document
                val source = doc.selectFirst("a:contains(V-Cloud)")?.attr("href").toString()
                EpisodeLink(source)
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
}
