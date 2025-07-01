package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.base64Decode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType


class CinemaluxeProvider : MainAPI() {
    override var mainUrl = "https://cinemalux.net"
    override var name = "Cinemaluxe"
    override val hasMainPage = true
    override var lang = "hi"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
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
                    jsonObject.optString("cinemaluxe")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }


    override val mainPage = mainPageOf(
        "page/" to "Home",
        "genre/hollywood/page/" to "Hollywood Movies",
        "genre/south-indian-movies/page/" to "South Indian Movies",
        "genre/hollywood-tv-show/page/" to "Hollywood TV Shows",
        "genre/bollywood-tv-show/page/" to "Bollywood TV Shows",
        "genre/anime-tv-show/page/" to "Anime TV Shows",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}${page}").document
        val home = document.select("article.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    data class Item(
        val token: String,
        val id: Long,
        val time: Long,
        val post: String,
        val redirect: String,
        val cacha: String,
        val new: Boolean,
        val link: String
    )

    private suspend fun makePostRequest(jsonString: String, url: String, action: String): String {
        val gson = Gson()
        val item = gson.fromJson(jsonString, Item::class.java)

        val requestBody = "token=${
          URLEncoder.encode(item.token, "UTF-8")
        }&id=${
          item.id
        }&time=${
          item.time
        }&post=${
          URLEncoder.encode(item.post, "UTF-8")
        }&redirect=${
          URLEncoder.encode(item.redirect, "UTF-8")
        }&cacha=${
          URLEncoder.encode(item.cacha, "UTF-8")
        }&new=${
          item.new
        }&link=${
          URLEncoder.encode(item.link, "UTF-8")
        }&action=$action".toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val response = app.post(url, requestBody = requestBody, allowRedirects = false).headers["Location"] ?: ""
        return response
    }

    private suspend fun bypass(url: String): String {
        val text = app.get(url).text
        val encodeUrl = Regex("""link":"([^"]+)""").find(text)?.groupValues?.get(1) ?: ""
        if(encodeUrl.isNotEmpty()) {
            return base64Decode(encodeUrl.replace("\\/", "/"))
        }
        val postUrl =
          """\"soralink_ajaxurl":"([^"]+)\"""".toRegex().find(text)?.groupValues?.get(1)
        val jsonData =
          """var\s+item\s*=\s*(\{.*?\});""".toRegex(RegexOption.DOT_MATCHES_ALL)
            .find(text)?.groupValues?.get(1)
        val soraLink =
          """\"soralink_z"\s*:\s*"([^"]+)\"""".toRegex().find(text)?.groupValues?.get(1)

        if(postUrl != null && jsonData != null && soraLink != null) {
            return makePostRequest(jsonData, postUrl, soraLink)
        }
        return url
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
        val description = document.select("div.wp-content > p").text()

        val tvType = if (url.contains("series")) {
            "series"
        } else {
            "movie"
        }

        if (tvType == "series") {
            val tvSeriesEpisodes = mutableListOf<Episode>()
            val aTags = document.select("div.wp-content div.ep-button-container > a")
            val episodesMap: MutableMap<Pair<Int, Int>, MutableList<String>> = mutableMapOf()

            coroutineScope {
                aTags.map { aTag ->
                    async {
                        val seasonText = aTag.text()
                        val realSeasonRegex = Regex("""(?:Season |S)(\d+)""")
                        val matchResult = realSeasonRegex.find(seasonText)
                        val realSeason = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: 0

                        val seasonLink = bypass(aTag.attr("href"))
                        val doc = app.get(seasonLink).document
                        val innerATags = doc.select("div.ep-button-container > a:matches((?i)(Episode))")

                        for (innerATag in innerATags) {
                            val epText = innerATag.text()
                            val epNumber = Regex("""(?i)(?:episode\s*[-]?\s*)(\d{1,2})""")
                                .find(epText)?.groups?.get(1)?.value?.toIntOrNull() ?: 0
                            val epUrl = innerATag.attr("href")
                            val key = Pair(realSeason, epNumber)

                            synchronized(episodesMap) {
                                episodesMap.getOrPut(key) { mutableListOf() }.add(epUrl)
                            }
                        }
                    }
                }.awaitAll()
            }

            for ((key, value) in episodesMap) {
                val data = value.map { source ->
                    EpisodeLink(source)
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
            val buttons = document.select("div.wp-content div.ep-button-container > a")

            val data = coroutineScope {
                buttons.map { button ->
                    async {
                        val link = bypass(button.attr("href"))
                        EpisodeLink(link)
                    }
                }.awaitAll()
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
            loadExtractor(it.source, subtitleCallback, callback)
        }
        return true   
    }

    data class EpisodeLink(
        val source: String
    )
}
