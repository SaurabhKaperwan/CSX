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

class NetflixMirrorProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"

    override var mainUrl = "https://iosmirror.cc"
    override var name = "NetflixMirror"

    override val hasMainPage = true
    private var time = ""
    private val headers = mapOf(
        "X-Requested-With" to "XMLHttpRequest"
    )

    private suspend fun getCookieFromGithub(): String {
        val data = app.get("https://raw.githubusercontent.com/SaurabhKaperwan/Utils/main/NF_Cookie.json").parsed<Cookie>()
        return data.cookie
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
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
        return HomePageList(name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val id = selectFirst("a")?.attr("data-post") ?: attr("data-post") ?: return null
        val posterUrl =
            fixUrlNull(selectFirst(".card-img-container img, .top10-img img")?.attr("data-src"))

        return newAnimeSearchResponse("", Id(id).toJson()) {
            this.posterUrl = posterUrl
            posterHeaders = mapOf("Referer" to "$mainUrl/")
        }
    }

    private fun convertRuntimeToMinutes(runtime: String): Int {
        var totalMinutes = 0

        val parts = runtime.split(" ")

        for (part in parts) {
            when {
                part.endsWith("h") -> {
                    val hours = part.removeSuffix("h").trim().toIntOrNull() ?: 0
                    totalMinutes += hours * 60
                }
                part.endsWith("m") -> {
                    val minutes = part.removeSuffix("m").trim().toIntOrNull() ?: 0
                    totalMinutes += minutes
                }
            }
        }

        return totalMinutes
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
            "hd" to "on"
        )
        val url = "$mainUrl/search.php?s=$query&t=$time"
        val data = app.get(url, referer = "$mainUrl/", cookies = cookies).parsed<SearchData>()

        return data.searchResult.map {
            newAnimeSearchResponse(it.t, Id(it.id).toJson()) {
                posterUrl = "https://img.nfmirrorcdn.top/poster/v/${it.id}.jpg"
                posterHeaders = mapOf("Referer" to "$mainUrl/")
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val id = parseJson<Id>(url).id
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
            "hd" to "on"
        )
        val data = app.get(
            "$mainUrl/post.php?id=$id&t=$time", headers, referer = "$mainUrl/", cookies = cookies
        ).parsed<PostData>()

        val episodes = arrayListOf<Episode>()

        val title = data.title
        val castList = data.cast?.split(",")?.map { it.trim() } ?: emptyList()
        val cast = castList.map {
            ActorData(
                Actor(it),
            )
        }
        val genre = data.genre?.split(",")?.map { it.trim() } ?: emptyList()
        val rating = data.match?.replace("IMDb ", "")?.toRatingInt()
        val runTime = convertRuntimeToMinutes(data.runtime.toString())

        if (data.episodes.first() == null) {
            episodes.add(newEpisode(LoadData(title, id)) {
                name = data.title
            })
        } else {
            data.episodes.filterNotNull().mapTo(episodes) {
                newEpisode(LoadData(title, it.id)) {
                    this.name = it.t
                    this.episode = it.ep.replace("E", "").toIntOrNull()
                    this.season = it.s.replace("S", "").toIntOrNull()
                    this.posterUrl = "https://img.nfmirrorcdn.top/epimg/150/${it.id}.jpg"
                    this.runTime = it.time.replace("m", "").toIntOrNull()
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
            posterUrl = "https://img.nfmirrorcdn.top/poster/h/$id.jpg"
            posterHeaders = mapOf("Referer" to "$mainUrl/")
            plot = data.desc
            year = data.year.toIntOrNull()
            tags = genre
            actors = cast
            this.rating = rating
            this.duration = runTime
        }
    }

    private suspend fun getEpisodes(
        title: String, eid: String, sid: String, page: Int
    ): List<Episode> {
        val episodes = arrayListOf<Episode>()
        val cookie_value = getCookieFromGithub()
        val cookies = mapOf(
            "t_hash_t" to cookie_value,
            "hd" to "on"
        )
        var pg = page
        while (true) {
            val data = app.get(
                "$mainUrl/episodes.php?s=$sid&series=$eid&t=$time&page=$pg",
                headers,
                referer = "$mainUrl/",
                cookies = cookies
            ).parsed<EpisodesData>()
            data.episodes?.mapTo(episodes) {
                newEpisode(LoadData(title, it.id)) {
                    name = it.t
                    episode = it.ep.replace("E", "").toIntOrNull()
                    season = it.s.replace("S", "").toIntOrNull()
                    this.posterUrl = "https://img.nfmirrorcdn.top/epimg/150/${it.id}.jpg"
                    this.runTime = it.time.replace("m", "").toIntOrNull()
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
            "hd" to "on"
        )
        val playlist = app.get(
            "$mainUrl/playlist.php?id=$id&t=$title&tm=$time",
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

    data class Cookie(
        val cookie: String
    )
}
