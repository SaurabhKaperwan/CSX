package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class TopmoviesProvider : MoviesmodProvider() { // all providers must be an instance of MainAPI
    override var mainUrl = "https://topmovies.pet"
    override var name = "TopMovies"
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
                    jsonObject.optString("topmovies")
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override val mainPage = mainPageOf(
        "/page/" to "Home",
        "/web-series/page/" to "Latest Web Series",
        "/movies/hindi-movies/page/" to "Latest Hindi Movies",
    )

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val document = app.get("$mainUrl/search/$query/page/$page").document
        val results = document.select("div.post-cards > article").mapNotNull { it.toSearchResult() }
        val hasNext = if(results.isEmpty()) false else true
        return SearchResponseList(results, hasNext)
    }
}
