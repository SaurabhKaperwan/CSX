package com.horis.cloudstreamplugins

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import okhttp3.Interceptor
import okhttp3.Response

class GDIndexProvider : MainAPI() {
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )
    override var lang = "en"
    override var mainUrl = "https://a.111477.xyz"
    override var name = "GDIndex"

    override val hasMainPage = true

    private val headers = mapOf(
        "Referer" to "$mainUrl/",
    )

    private var rootPageData = emptyList<GDFile>()

    private val cacheFiles = hashMapOf<GDFile, List<GDFile>>()

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        if (rootPageData.isEmpty()) {
            rootPageData = listDir("$mainUrl/").filter { it.isFolder }
        }

        val homePages = rootPageData.amap {
            val gdFiles = listDir(it)
            cacheFiles[it] = gdFiles
            HomePageList(it.name, gdFiles.toSearchResponseList())
        }

        return HomePageResponse(homePages)
    }

    override suspend fun search(query: String): List<SearchResponse>? {
        if (cacheFiles.isEmpty()) {
            rootPageData = listDir("$mainUrl/").filter { it.isFolder }
            rootPageData.amap {
                cacheFiles[it] = listDir(it)
            }
        }
        val files = arrayListOf<GDFile>()
        cacheFiles.forEach { (_, fs) ->
            fs.forEach {
                if (it.name.contains(query, true)) {
                    files.add(it)
                }
            }
        }
        return files.toSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val file = tryParseJson<GDFile>(url) ?: return null
        val title = file.name

        var seasons: List<SeasonData>? = null

        val episodes = if (file.isFolder) {
            val items = listDir(file)
            val folders = items.filter { it.isFolder }
            val files =
                items.filter { !it.isFolder && it.name.contains("(?i)\\.(mkv|mp4)$".toRegex()) }
            seasons = folders.mapIndexed { i, f ->
                SeasonData(i + 1, "S\\d+".toRegex().find(f.name)?.value ?: f.name)
            }
            folders.amapIndexed { index, gdFile ->
                listDir(gdFile).mapNotNull {
                    if (!it.name.contains("(?i)\\.(mkv|mp4)$".toRegex())) return@mapNotNull null
                    newEpisode(it) {
                        name = "E\\d+".toRegex().find(it.name)?.value ?: it.name
                        season = index + 1
                    }
                }
            }.flatten().toMutableList().also {
                files.mapTo(it) { gdFile ->
                    newEpisode(gdFile) {
                        name = gdFile.name
                        season = seasons.size
                    }
                }
            }
        } else {
            arrayListOf(newEpisode(file) {
                name = title
            })
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            seasonNames = seasons
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val file = parseJson<GDFile>(data)
        val path = file.path
        callback.invoke(
            newExtractorLink(
                name,
                name,
                path,
                INFER_TYPE
            ) {
                this.referer = "$mainUrl/"
            }
        )
        return true
    }

    private fun List<GDFile>.toSearchResponseList(): List<SearchResponse> {
        return filter { it.name.contains("(?i)\\.(mkv|mp4)$".toRegex()) || it.isFolder }.map {
            newAnimeSearchResponse(it.name, it.toJson())
        }
    }

    private suspend fun listDir(file: GDFile): List<GDFile> {
        return listDir(file.path)
    }

    private suspend fun listDir(file: String): List<GDFile> {
        val doc = app.get(file, headers = headers).document
        val files = doc.select("a").map {
            val filename = it.text()
            GDFile(
                name = filename.removeSuffix("/"),
                isFolder = filename.endsWith("/"),
                parentFolder = file
            )
        }
        return files
    }

    private fun error(msg: String = "加载数据失败"): Nothing {
        throw ErrorLoadingException(msg)
    }

    data class GDFile(
        val name: String,
        val isFolder: Boolean,
        var parentFolder: String?
    ) {
        val path
            get() = "$parentFolder${
                name.replace("#", "%23").replace("?", "%3F")
            }" + if (isFolder) "/" else ""
    }

}
