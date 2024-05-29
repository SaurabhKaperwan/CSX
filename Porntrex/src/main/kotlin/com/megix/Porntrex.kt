package com.megix

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.VPNStatus
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.getQualityFromName
import org.json.JSONObject
import org.jsoup.internal.StringUtil
import org.jsoup.nodes.Element

class Porntrex : MainAPI() {
    override var mainUrl = "https://www.porntrex.com"
    override var name = "Porntrex"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
            "latest-updates" to "Latest Videos",
            "most-popular/daily/?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&sort_by=video_viewed_today&from4=" to "Most popular daily",
            "top-rated/daily/?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&sort_by=rating_today&from4=" to "Top rated daily",
            "most-popular/weekly/?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&sort_by=video_viewed_week&from4=" to "Most popular weekly",
            "top-rated/weekly/?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&sort_by=rating_week&from4=" to "Top rated weekly",
            "most-popular/monthly/?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&sort_by=video_viewed_month&from4=" to "Most popular monthly",
            "top-rated/monthly/?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&sort_by=rating_month&from4=" to "Top rated monthly",
            "most-popular/?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&sort_by=video_viewed&from4=" to "Most popular all time",
            "top-rated/?mode=async&function=get_block&block_id=list_videos_common_videos_list_norm&sort_by=rating&from4=" to "Top rated all time",
    )

    override suspend fun getMainPage(
            page: Int,
            request: MainPageRequest
    ): HomePageResponse {
        var url: String
        url = if (page == 1) {
            "$mainUrl/${request.data}/"
        } else {
            "$mainUrl/${request.data}/${page}/"
        }
        if (request.data.contains("mode=async")) {
            url = "$mainUrl/${request.data}${page}"
        }
        val document = app.get(url).document
        val home =
                document.select("div.video-list div.video-item")
                        .mapNotNull {
                            it.toSearchResult()
                        }
        return newHomePageResponse(
                list = HomePageList(
                        name = request.name,
                        list = home,
                        isHorizontalImages = true
                ),
                hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("p.inf a")?.text() ?: return null
        val href = fixUrl(this.selectFirst("p.inf a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.select("a.thumb img.cover").attr("data-src"))
        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
            this.posterHeaders = mapOf(Pair("referer", "${mainUrl}/"))
        }

    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..15) {
            val url: String = if (i == 1) {
                "$mainUrl/search/${query.replace(" ", "-")}/"
            } else {
                "$mainUrl/search/${query.replace(" ", "-")}/$i/"
            }
            val document =
                    app.get(url).document
            val results =
                    document.select("div.video-list div.video-item")
                            .mapNotNull {
                                it.toSearchResult()
                            }
            searchResponse.addAll(results)
            if (results.isEmpty()) break
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val jsonObject = JSONObject(document.selectXpath("//script[contains(text(),'var flashvars')]").first()?.data()
                ?.substringAfter("var flashvars = ")
                ?.substringBefore("var player_obj")
                ?.replace(";", "") ?: "")

        val title = jsonObject.getString("video_title")
        val poster =
                fixUrlNull(jsonObject.getString("preview_url"))

        val tags = jsonObject.getString("video_tags").split(", ").map { it.replace("-", "") }.filter { it.isNotBlank() && !StringUtil.isNumeric(it) }
        val description = jsonObject.getString("video_title")

        val recommendations =
                document.select("div#list_videos_related_videos div.video-list div.video-item").mapNotNull {
                    it.toSearchResult()
                }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.posterHeaders = mapOf(Pair("referer", "${mainUrl}/"))
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        val jsonObject = JSONObject(document.selectXpath("//script[contains(text(),'var flashvars')]").first()?.data()
                ?.substringAfter("var flashvars = ")
                ?.substringBefore("var player_obj")
                ?.replace(";", "") ?: "")
        val extlinkList = mutableListOf<ExtractorLink>()
        for (i in 0 until 7) {
            var url: String
            var quality: String
            if (i == 0) {
                url = jsonObject.optString("video_url") ?: ""
                quality = jsonObject.optString("video_url_text") ?: ""
            } else {
                if (i == 1) {
                    url = jsonObject.optString("video_alt_url") ?: ""
                    quality = jsonObject.optString("video_alt_url_text") ?: ""
                } else {
                    url = jsonObject.optString("video_alt_url${i}") ?: ""
                    quality = jsonObject.optString("video_alt_url${i}_text") ?: ""
                }
            }
            if (url == "") {
                continue
            }
            extlinkList.add(
                    ExtractorLink(
                            name,
                            name,
                            fixUrl(url),
                            referer = "",
                            quality =
                            Regex("(\\d+.)").find(quality)?.groupValues?.get(1)
                                    .let { getQualityFromName(it) }
                    )
            )
        }
        extlinkList.forEach(callback)
        return true
    }
}
