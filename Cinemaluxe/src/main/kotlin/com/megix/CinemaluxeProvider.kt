package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class CinemaluxeProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://cinemaluxe.foo"
    override var name = "Cinemaluxe"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Home",
        "$mainUrl/genre/hollywood/page/" to "Hollywood Movies",
        "$mainUrl/genre/south-indian-movies/page/" to "South Indian Movies",
        "$mainUrl/genre/hollywood-tv-show/page/" to "Hollywood TV Shows",
        "$mainUrl/genre/bollywood-tv-show/page/" to "Bollywood TV Shows",
        "$mainUrl/genre/anime-tv-show/page/" to "Anime TV Shows",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("article.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    data class RedirectUrl(
        val redirectUrl: String
    )

    private suspend fun bypass(url: String): String {
        val jsonBody = """{"url":"$url"}"""
        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
        val json = app.post(
            "${BuildConfig.BYPASS_API}/cinemaluxe",
            headers = mapOf(
                "Content-Type" to "application/json",
            ),
            requestBody = requestBody
        ).text
        return parseJson<RedirectUrl>(json).redirectUrl
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.select("img").attr("alt")
        val href = this.select("a").attr("href")
        val posterUrl = this.select("img").attr("data-src")
    
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..6) {
            val document = app.get("$mainUrl/page/$i/?s=$query").document

            val results = document.select("div.result-item").mapNotNull { it.toSearchResult() }

            if (results.isEmpty()) {
                break
            }
            searchResponse.addAll(results)
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.select("div.data > h1").text()
        val posterUrl = document.select("div.poster > img").attr("data-src")
        val description = document.selectFirst("div.wp-content")?.ownText() ?: ""

        val tvType = if (url.contains("tvshow")) {
            "series"
        } else {
            "movie"
        }

        if(tvType == "series") {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            val aTags = document.select("div.ep-button-container > a")
            val episodesMap: MutableMap<Pair<Int, Int>, List<String>> = mutableMapOf()

            aTags.mapNotNull{ aTag ->
                val seasonText = aTag.text()
                val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                val matchResult = realSeasonRegex.find(seasonText)
                val realSeason = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val seasonLink = bypass(aTag.attr("href"))
                val doc = app.get(seasonLink).document
                var innerATags = doc.select("div.ep-button-container > a:matches((?i)(Episode))")
                
                innerATags.mapNotNull { innerATag ->
                    val epText = innerATag.text()
                    val e = Regex("""(?i)(?:episode\s*[-]?\s*)(\d{1,2})""").find(epText)?.groups?.get(1)?.value ?.toIntOrNull() ?: 0
                    val epUrl = innerATag.attr("href")
                    val key = Pair(realSeason, e)
                    if (episodesMap.containsKey(key)) {
                        val currentList = episodesMap[key] ?: emptyList()
                        val newList = currentList.toMutableList()
                        newList.add(epUrl)
                        episodesMap[key] = newList
                    } else {
                        episodesMap[key] = mutableListOf(epUrl)
                    }
                }
            }

            for ((key, value) in episodesMap) {
                val data = value.map { source->
                    EpisodeLink(
                        source
                    )
                }
                tvSeriesEpisodes.add(
                    newEpisode(data) {
                        this.season = key.first
                        this.episode = key.second
                    }
                )
            }
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, tvSeriesEpisodes) {
                this.posterUrl = posterUrl
                this.plot = description
            }
        }
        else {
            val buttons = document.select("div.ep-button-container > a")
            val data = buttons.flatMap { button ->
                var link = button.attr("href")
                link = bypass(link)
                val doc = app.get(link).document
                val selector = if(link.contains("linkstore")) "div.ep-button-container > a" else "div.mirror-buttons a"
                doc.select(selector).mapNotNull {
                    val source = it.attr("href")
                    EpisodeLink(
                        source
                    )
                }
            }
            return newMovieLoadResponse(title, url, TvType.Movie, data) {
                this.posterUrl = posterUrl
                this.plot = description
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

    data class EpisodeLink(
        val source: String
    )
}
