package com.horis.cloudstreamplugins

import com.horis.cloudstreamplugins.entities.EpisodesData
import com.horis.cloudstreamplugins.entities.PlayList
import com.horis.cloudstreamplugins.entities.PostData
import com.horis.cloudstreamplugins.entities.SearchData
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.nodes.Element
import org.json.JSONObject

class PrimeVideoMirrorProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"

    override var mainUrl = "https://iosmirror.cc"
    override var name = "PrimeVideoMirror"

    override val hasMainPage = true
    private var time = ""
    private val headers = mapOf(
        "X-Requested-With" to "XMLHttpRequest"
    )

    private suspend fun getCookieFromGithub(): String {
        val document = app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/main/NF_Cookie.json").document
        val json = document.body().text()
        val jsonObject = JSONObject(json)
        return jsonObject.getString("cookie")
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
            "ott" to "pv",
            "hd" to "on"
        )
        val document = app.get("$mainUrl/home", cookies = cookies).document
        time = document.select("body").attr("data-time")
        val items = document.select(".tray-container, #top10").map {
            it.toHomePageList()
        }
        return HomePageResponse(items, false)
    }

    private fun Element.toHomePageList(): HomePageList {
        val name = select("h2, span").text()
        val items = select("article, .top10-post").mapNotNull {
            it.toSearchResult()
        }
        return HomePageList(
            name,
            items,
            isHorizontalImages = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val id = selectFirst("a")?.attr("data-post") ?: attr("data-post") ?: return null
        val posterUrl = fixUrlNull(selectFirst(".card-img-container img, img.top10-img-1")?.attr("data-src"))

        return newAnimeSearchResponse("", Id(id).toJson()) {
            this.posterUrl = posterUrl
            posterHeaders = mapOf("Referer" to "$mainUrl/")
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
            "ott" to "pv",
            "hd" to "on"
        )
        val url = "$mainUrl/pv/search.php?s=$query&t=$time"
        val data = app.get(url, referer = "$mainUrl/", cookies = cookies).parsed<SearchData>()

        return data.searchResult.map {
            newAnimeSearchResponse(it.t, Id(it.id).toJson()) {
                posterUrl = "https://img.nfmirrorcdn.top/pv/700/${it.id}.jpg"
                posterHeaders = mapOf("Referer" to "$mainUrl/")
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = parseJson<Id>(url).id
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
            "ott" to "pv",
            "hd" to "on"
        )
        val data = app.get(
            "$mainUrl/pv/post.php?id=$id&t=$time", headers, referer = "$mainUrl/", cookies = cookies
        ).parsed<PostData>()

        val episodes = arrayListOf<Episode>()

        val title = data.title

        if (data.episodes.first() == null) {
            episodes.add(newEpisode(LoadData(title, id)) {
                name = data.title
            })
        } else {
            data.episodes.filterNotNull().mapTo(episodes) {
                newEpisode(LoadData(title, it.id)) {
                    name = it.t
                    episode = it.ep.replace("E", "").toIntOrNull()
                    season = it.s.replace("S", "").toIntOrNull()
                }
            }

            if (data.nextPageShow == 1) {
                episodes.addAll(getEpisodes(title, url, data.nextPageSeason!!, 2))
            }

            data.season?.dropLast(1)?.amap {
                episodes.addAll(getEpisodes(title, url, it.id, 1))
            }
        }

        val type = if (data.episodes.first() == null) TvType.Movie else TvType.TvSeries

        return newTvSeriesLoadResponse(title, url, type, episodes) {
            posterUrl = "https://img.nfmirrorcdn.top/pv/700/$id.jpg"
            posterHeaders = mapOf("Referer" to "$mainUrl/")
            plot = data.desc
            year = data.year.toIntOrNull()
        }
    }

    private suspend fun getEpisodes(
        title: String, eid: String, sid: String, page: Int
    ): List<Episode> {
        val episodes = arrayListOf<Episode>()
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
            "ott" to "pv",
            "hd" to "on"
        )
        var pg = page
        while (true) {
            val data = app.get(
                "$mainUrl/pv/episodes.php?s=$sid&series=$eid&t=$time&page=$pg",
                headers,
                referer = "$mainUrl/",
                cookies = cookies
            ).parsed<EpisodesData>()
            data.episodes?.mapTo(episodes) {
                newEpisode(LoadData(title, it.id)) {
                    name = it.t
                    episode = it.ep.replace("E", "").toIntOrNull()
                    season = it.s.replace("S", "").toIntOrNull()
                }
            }
            if (data.nextPageShow == 0) break
            pg++
        }
        return episodes
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val (title, id) = parseJson<LoadData>(data)
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
            "ott" to "pv",
            "hd" to "on"
        )
        val playlist = app.get(
            "$mainUrl/pv/playlist.php?id=$id&t=$title&tm=$time",
            headers,
            referer = "$mainUrl/",
            cookies = cookies
        ).parsed<PlayList>()

        playlist.forEach { item ->
            item.sources.forEach {
                callback(
                    ExtractorLink(
                        name,
                        it.label,
                        fixUrl(it.file),
                        "$mainUrl/",
                        getQualityFromName(it.file.substringAfter("q=", "")),
                        true
                    )
                )
            }
        }
        return true
    }

    @Suppress("ObjectLiteralToLambda")
    override fun getVideoInterceptor(extractorLink: ExtractorLink): Interceptor? {
        return object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()
                if (request.url.toString().contains(".m3u8")) {
                    val newRequest = request.newBuilder()
                        .header("Cookie", "hd=on")
                        .build()
                    return chain.proceed(newRequest)
                }
                return chain.proceed(request)
            }
        }
    }

    data class Id(
        val id: String
    )

    data class LoadData(
        val title: String, val id: String
    )

}
