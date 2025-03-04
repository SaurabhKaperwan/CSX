package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbUrl
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.google.gson.Gson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson

class World4uFreeProvider : MainAPI() {
    override var mainUrl = "https://world4ufree.fyi"
    override var name = "World4uFree"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    val cinemeta_url = "https://v3-cinemeta.strem.io/meta"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/category/bollywood/page" to "Bollywood",
        "$mainUrl/category/hollywood/page" to "Hollywood",
        "$mainUrl/category/web-series/page" to "Web Series",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("ul.recent-posts > li").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div > a")?.attr("title")?.replace("Download ", "").toString()
        val href = this.selectFirst("div > a") ?. attr("href").toString()
        var posterUrl = this.selectFirst("div > a > img")?.attr("data-src").toString()
        if(posterUrl.isEmpty()) {
            posterUrl = this.selectFirst("div > a > img")?.attr("src").toString()
        }
        val quality = if(title.contains("HDCAM", ignoreCase = true) || title.contains("CAMRip", ignoreCase = true)) {
            SearchQuality.CamRip
        }
        else {
            null
        }
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.quality = quality
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..25) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("ul.recent-posts > li").mapNotNull { it.toSearchResult() }

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

        val div = document.selectFirst("div.entry-content")
        val imdbUrl = document.selectFirst("div.imdb_left > a")?.attr("href")
        var description = div?.selectFirst("p:matches((?i)(plot|synopsis|story))")?.text() ?: ""
        var posterUrl = document.selectFirst("meta[property=og:image]")?.attr("content").toString()

        if(posterUrl.isEmpty() || posterUrl.contains("$mainUrl/favicon-32x32.png")) {
            posterUrl = document.selectFirst("div.separator > a > img")?.attr("data-src").toString()
        }
        val tvtype = if (document.select("div.entry-content").text().contains("movie name", ignoreCase = true)) {
            "movie"
        }
        else {
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
            val buttons = document.select("a.my-button")
            val episodesMap: MutableMap<Pair<Int, Int>, List<Pair<String, String>>> = mutableMapOf()

            buttons.forEach { button ->
                val titleElement = button.parent()?.parent()?.previousElementSibling()
                val titleText = titleElement ?. text() ?: ""
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val realSeason = realSeasonRegex.find(titleText) ?. groupValues ?. get(1) ?.toIntOrNull() ?: 0
                val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                val quality = qualityRegex.find(titleText) ?. groupValues ?. get(1) ?: ""
                var ep = 1
                val wlinkz = button.attr("href")
                if(wlinkz.isNotEmpty()) {
                    val doc = app.get(wlinkz).document
                    val elements = doc.select("h3:matches((?i)(episode))")
                    elements.forEach { element ->
                        //val epTitle = element.text()
                        var linkElement = element.nextElementSibling()
                        while (linkElement != null && linkElement.tagName() != "h4") {
                            linkElement = linkElement.nextElementSibling()
                        }
                        var link = ""
                        if(linkElement != null) {
                            val aTag = linkElement.selectFirst("a")
                            link = aTag ?. attr("href") ?: ""
                        }

                        if (link.isNotEmpty() && !title.contains("zip", ignoreCase = true)) {
                            val key = Pair(realSeason, ep)
                            val episodePair = Pair(link, quality)
                            if (episodesMap.containsKey(key)) {
                                val currentList = episodesMap[key] ?: emptyList()
                                val newList = currentList.toMutableList()
                                newList.add(episodePair)
                                episodesMap[key] = newList
                            } else {
                                episodesMap[key] = mutableListOf(episodePair)
                            }
                            ep++
                        }
                    }
                    ep = 1
                }
            }

            for ((key, value) in episodesMap) {
                val episodeInfo = responseData?.meta?.videos?.find { it.season == key.first && it.episode == key.second }
                val data = value.map { pair ->
                    EpisodeLink(
                        pair.first,
                        pair.second
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
            val links = document.select("a.my-button")
            val data = links.flatMap {
                val link = it.attr("href")
                val quality = it.text()
                val doc = app.get(link).document
                val urls = doc.select("a:matches((?i)(instant|download|direct))")
                urls.mapNotNull {
                    EpisodeLink(
                        it.attr("href"),
                        quality
                    )
                }
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
            val quality = it.quality
            loadCustomExtractor(source, subtitleCallback, callback, getIndexQuality(quality))
        }
        return true   
    }

    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "") ?. groupValues ?. getOrNull(1) ?. toIntOrNull()
            ?: Qualities.Unknown.value
    }

    private suspend fun loadCustomExtractor(
        url: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        quality: Int = Qualities.Unknown.value,
    ){
        loadExtractor(url,subtitleCallback) { link ->
            callback.invoke (
                ExtractorLink (
                    link.source,
                    link.name,
                    link.url,
                    link.referer,
                    if(link.quality == Qualities.Unknown.value) quality else link.quality,
                    link.type,
                    link.headers,
                    link.extractorData
                )
            )
        }
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
        val source: String,
        val quality: String
    )
}
