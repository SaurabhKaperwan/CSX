package com.megix

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URLEncoder

class DailymotionProvider : MainAPI() {

    data class VideoSearchResponse(
        val list: List<VideoItem>
    )

    data class VideoItem(
        val id: String,
        val title: String,
        val thumbnail_360_url: String
    )

    data class VideoDetailResponse(
        val id: String,
        val title: String,
        val description: String,
        val thumbnail_720_url: String
    )

    override var mainUrl = "https://api.dailymotion.com"
    override var name = "Dailymotion"
    override val supportedTypes = setOf(TvType.Others)

    override var lang = "en"

    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val response = app.get("$mainUrl/videos?fields=id,title,thumbnail_360_url&limit=26").text
        val popular = tryParseJson<VideoSearchResponse>(response)?.list ?: emptyList()

        return newHomePageResponse(
            listOf(
                HomePageList(
                    "Popular",
                    popular.map { it.toSearchResponse(this) },
                    true
                ),
            ),
            false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$mainUrl/videos?fields=id,title,thumbnail_360_url&limit=10&search=${query.encodeUri()}").text
        val searchResults = tryParseJson<VideoSearchResponse>(response)?.list ?: return emptyList()
        return searchResults.map { it.toSearchResponse(this) }
    }

    override suspend fun load(url: String): LoadResponse? {
        val videoId = Regex("dailymotion.com/video/([a-zA-Z0-9]+)").find(url)?.groups?.get(1)?.value
        val response = app.get("$mainUrl/video/$videoId?fields=id,title,description,thumbnail_720_url").text
        val videoDetail = tryParseJson<VideoDetailResponse>(response) ?: return null
        return videoDetail.toLoadResponse(this)
    }

    private fun VideoItem.toSearchResponse(provider: DailymotionProvider): SearchResponse {
        return provider.newMovieSearchResponse(
            this.title,
            "https://www.dailymotion.com/video/${this.id}",
            TvType.Movie
        ) {
            this.posterUrl = thumbnail_360_url
        }
    }

    private suspend fun VideoDetailResponse.toLoadResponse(provider: DailymotionProvider): LoadResponse {
        return provider.newMovieLoadResponse(
            this.title,
            "https://www.dailymotion.com/video/${this.id}",
            TvType.Movie,
            this.id
        ) {
            plot = description
            posterUrl = thumbnail_720_url
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        loadExtractor(
            "https://www.dailymotion.com/embed/video/$data",
            subtitleCallback,
            callback
        )
        return true
    }

    companion object {
        fun String.encodeUri(): String = URLEncoder.encode(this, "utf8")
    }
}
