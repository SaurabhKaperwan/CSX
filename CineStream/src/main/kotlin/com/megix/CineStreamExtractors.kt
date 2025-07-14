package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.api.Log
import android.util.Base64
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import java.net.URLEncoder
import okhttp3.FormBody
import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.json.JSONObject
import org.json.JSONArray
import com.lagradost.cloudstream3.runAllAsync
import com.lagradost.cloudstream3.extractors.helper.GogoHelper
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.nicehttp.RequestBodyTypes
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import java.net.URI
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.USER_AGENT
import com.google.gson.Gson

object CineStreamExtractors : CineStreamProvider() {

    suspend fun invokeDramadrip(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val link = app.get("$dramadripAPI/?s=$imdbId").document.selectFirst("article > a")?.attr("href") ?: return
        val document = app.get(link).document
        if(season != null && episode != null) {
            val seasonLink = document.select("div.file-spoiler h2").filter { element ->
                val text = element.text().trim().lowercase()
                    "season $season ".lowercase() in text && "zip" !in text
            }.flatMap { h2 ->
                val sibling = h2.nextElementSibling()
                sibling?.select("a")?.mapNotNull { it.attr("href") } ?: emptyList()
            }

            seasonLink.amap {
                val doc = app.get(it).document
                var sourceUrl= doc.select("div.series_btn > a")
                    .getOrNull(episode-1)?.attr("href")
                    ?: return@amap
                sourceUrl = if ("unblockedgames" in sourceUrl) {
                    bypassHrefli(sourceUrl) ?: return@amap
                } else {
                    sourceUrl
                }

                loadSourceNameExtractor("Dramadrip", sourceUrl, "", subtitleCallback, callback)
            }
        } else {
            document.select("div.file-spoiler a").amap {
                val doc = app.get(it.attr("href")).document
                doc.select("a.wp-element-button").amap { source ->
                    var sourceUrl = source.attr("href")
                    sourceUrl = if ("unblockedgames" in sourceUrl) {
                        bypassHrefli(sourceUrl) ?: return@amap
                    } else {
                        sourceUrl
                    }
                    loadSourceNameExtractor(
                        "Dramadrip",
                        sourceUrl,
                        "",
                        subtitleCallback,
                        callback
                    )

                }
            }
        }
    }

    suspend fun invokeToonstream(
        title: String? = null,
        season: Int? = null,
        episode: Int?  = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$toonStreamAPI/movies/${title.createSlug()}/"
        } else {
            "$toonStreamAPI/episode/${title.createSlug()}-${season}x${episode}/"
        }

        app.get(url, referer = toonStreamAPI).document.select("#aa-options > div > iframe").amap {
            val doc = app.get(it.attr("data-src")).document
            doc.select("div.Video > iframe").amap { iframe ->
                loadSourceNameExtractor(
                    "ToonStream",
                    iframe.attr("src"),
                    "",
                    subtitleCallback,
                    callback
                )
            }
        }
    }

    suspend fun invokeAnimez(
        title: String? = null,
        episode: Int?  = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get("$animezAPI/?act=search&f[keyword]=$title").document
        document.select("article > a").amap {
            val doc = app.get(animezAPI + it.attr("href")).document
            val titles = doc.select("ul.InfoList > li").text()

            if(!titles.contains("$title")) return@amap

            val ep = episode ?: 1
            val links  = doc.select("li.wp-manga-chapter > a")
            val link = if (links.size >= ep) links[links.size - ep] else return@amap
            val type = if(link.text().contains("Dub")) "DUB" else "SUB"
            val epDoc = app.get(animezAPI + link.attr("href")).document
            val source = epDoc.select("iframe").attr("src")
            callback.invoke(
                newExtractorLink(
                    "Animez [$type]",
                    "Animez [$type]",
                    source.replace("/embed/", "/anime/"),
                    ExtractorLinkType.M3U8
                ) {
                    this.referer = "$animezAPI/"
                    this.quality = 1080
                }
            )
        }
    }

    suspend fun invokeStremioSubtitles(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val gson = Gson()
        val subsUrls = listOf(
            "https://3b4bbf5252c4-aio-streaming.baby-beamup.club/stremio/languages=english,hindi,spanish,arabic,mandarin,bengali,portuguese,russian,japanese,lahnda,thai,turkish,french,german,korean,telugu,marathi,tamil,urdu,italian",
            "https://subsource.strem.bar/ZW5nbGlzaCxoaW5kaSxzcGFuaXNoLGFyYWJpYyxtYW5kYXJpbixiZW5nYWxpLHBvcnR1Z3Vlc2UscnVzc2lhbixqYXBhbmVzZSxsYWhuZGEsdGhhaSx0dXJraXNoLGZyZW5jaCxnZXJtYW4sa29yZWFuLHRlbHVndSxtYXJhdGhpLHRhbWlsLHVyZHUsaXRhbGlhbi9oaUluY2x1ZGUv",
            "https://opensubtitles.stremio.homes/en|hi|de|ar|tr|es|ta|te|ru|ko/ai-translated=true|from=all|auto-adjustment=true"
        )

        subsUrls.amap { subUrl ->
            try {
                val url = if(season != null) {
                    subUrl + "/subtitles/series/$imdbId:$season:$episode.json"
                } else {
                    subUrl + "/subtitles/movie/$imdbId.json"
                }

                val json = app.get(url).text
                val subtitleResponse = gson.fromJson(json, StremioSubtitleResponse::class.java)

                subtitleResponse.subtitles.forEach {
                    val lang = it.lang ?: it.lang_code
                    val fileUrl = it.url
                    if(lang != null && fileUrl != null) {
                        subtitleCallback.invoke(
                            SubtitleFile(
                                lang,
                                fileUrl,
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                println("Error fetching/parsing subtitle from: $subUrl - ${e.message}")
            }
        }
    }

    suspend fun invokeKatMovieHd(
        sourceName: String,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val api = if(sourceName == "KatMovieHd") katmoviehdAPI else moviesBabaAPI
        val url = "$api/?s=$imdbId"
        val headers = mapOf(
            "Referer" to api,
            "Origin" to api,
            "User-Agent" to USER_AGENT
        )
        val doc = app.get(url, headers).document
        val link = if (season == null) {
            doc.selectFirst("div.post-thumb > a")?.attr("href")
        } else {
            doc.select("div.post-thumb > a")
                .firstOrNull { it.attr("title").contains("Season $season", ignoreCase = true) }
                ?.attr("href")
        } ?: return

        val div = app.get(link, headers).document.selectFirst("div.entry-content") ?: return
        val pattern = """<(?:a|iframe)\s[^>]*(?:href|src)="(https:\/\/links\.kmhd\.net\/play\?id=[^"]+)"[^>]*>""".toRegex()
        val match = pattern.find(div.toString())
        val watchUrl = match?.groupValues?.get(1) ?: return
        val watchDoc = app.get(watchUrl, headers).toString()
        val linksmap = extractKatStreamingLinks(watchDoc, episode)
        linksmap.amap { (key, value) ->
            loadSourceNameExtractor(
                sourceName,
                value,
                watchUrl,
                subtitleCallback,
                callback
            )
        }
    }

    suspend fun invokeMadplay(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to "https://uembed.site",
            "Origin" to "https://uembed.site",
            "User-Agent" to USER_AGENT
        )
        val jsonString = app.get("https://raw.githubusercontent.com/kunwarxshashank/rogplay_addons/refs/heads/main/cinema/hindi.json").text
        val serverInfoList = parseMadplayServerInfo(jsonString)

        serverInfoList.amap { info ->
            val url = if(season != null) {
                info.tvurl
                .replace("\${tmdb}", tmdbId.toString())
                .replace("\${season}", season.toString())
                .replace("\${episode}", episode.toString())
            } else {
                info.movieurl.replace("\${tmdb}", tmdbId.toString())
            }

            val fileUrl = try {
                val text = app.get(url, headers = headers).text
                JSONArray(text).getJSONObject(0).getString("file")
            } catch (e: Exception) { null }

            if(fileUrl != null) {
                callback.invoke(
                    newExtractorLink(
                        "Madplay [${info.server}]",
                        "Madplay [${info.server}]",
                        fileUrl,
                        type = if(fileUrl.contains("mp4")) ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                    ) {
                        this.headers = headers
                    }
                )
            }
        }
    }

    suspend fun invokeSudatchi(
        aniId: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to sudatchiAPI,
            "Origin" to sudatchiAPI,
        )

        val json = app.get("$sudatchiAPI/api/episode/$aniId/$episode", headers = headers).text
        val jsonObject = JSONObject(json)
        val episode = jsonObject.getJSONObject("episode")
        val epId = episode.getInt("id")
        val url = "$sudatchiAPI/api/streams?episodeId=$epId"
        callback.invoke(
            newExtractorLink(
                "Sudatchi",
                "Sudatchi",
                url,
                type = ExtractorLinkType.M3U8
            ) {
                this.quality = 1080
                this.headers = headers
            }
        )

        val subsJson = JSONArray(jsonObject.getString("subtitlesJson"))

        for (i in 0 until subsJson.length()) {
            val sub = subsJson.getJSONObject(i)
            val subUrl = sub.getString("url").replace("/ipfs", "").replace("\\", "")
            val file = "https://sudatchi.com/api/proxy$subUrl"
            val label = sub.getJSONObject("SubtitlesName").getString("name")
            subtitleCallback.invoke(
                SubtitleFile(
                    label,
                    file
                )
            )
        }
    }

    suspend fun invokeGojo(
        aniId: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if (aniId == null) return

        val episodeNumber = episode ?: 1
        val gojoAPI = gojoBaseAPI.replace("https://", "https://backend.")
        val headers = mapOf(
            "Referer" to gojoBaseAPI,
            "Origin" to gojoBaseAPI
        )

        val epDetailsJson = try {
            app.get(
                "$gojoAPI/api/anime/servers?id=$aniId&num=$episodeNumber",
                headers = headers
            ).text
        } catch (e: Exception) {
            return
        }

        val servers = try {
            getGojoServers(epDetailsJson)
        } catch (e: Exception) {
            return
        }

        if (servers.isEmpty()) {
            return
        }

        for ((provider, hasDub) in servers) {
            runAllAsync(
                {
                    try {
                        val json = app.get(
                            "$gojoAPI/api/anime/tiddies?server=$provider&id=$aniId&num=$episodeNumber&subType=sub",
                            headers = headers
                        ).text
                        getGojoStreams(json, "sub", provider, gojoBaseAPI, subtitleCallback ,callback)
                    } catch (e: Exception) {
                        println("Error fetching sub stream for $provider: ${e.message}")
                    }
                },
                {
                    if (hasDub) {
                        try {
                            val json = app.get(
                                "$gojoAPI/api/anime/tiddies?server=$provider&id=$aniId&num=$episodeNumber&subType=dub",
                                headers = headers
                            ).text
                            getGojoStreams(json, "dub", provider, gojoBaseAPI, subtitleCallback ,callback)
                        } catch (e: Exception) {
                            println("Error fetching dub stream for $provider: ${e.message}")
                        }
                    }
                }
            )
        }
    }

    suspend fun invokeAnimeparadise(
        title: String? = null,
        malId: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        if(malId == null || title == null || episode == null) return
        val searchJson = app.get("$animeparadiseAPI/search?q=$title").text
        val root = JSONObject(searchJson)
        val dataArray = root.getJSONArray("data")

        for (i in 0 until dataArray.length()) {
            val anime = dataArray.getJSONObject(i)
            val mappings = anime.getJSONObject("mappings")

            if (mappings.optInt("myanimelist") == malId) {
                val animeId = anime.getString("_id")
                val episodes = anime.getJSONArray("ep")
                if (episode in 1..episodes.length()) {
                    val episodeId = episodes.getString(episode - 1)
                    val epUrl = "$animeparadiseBaseAPI/watch/$episodeId?origin=$animeId"
                    val epDoc = app.get(epUrl).document
                    val epJsonString = epDoc.selectFirst("script#__NEXT_DATA__")?.data() ?: return
                    val epJson = JSONObject(epJsonString)
                    .getJSONObject("props")
                    .getJSONObject("pageProps")
                    .getJSONObject("episode")

                    val streamUrl = epJson.optString("streamLink")
                    //val backupUrl = epJson.optString("streamLinkBackup")
                    val headers = mapOf(
                        "Referer" to animeparadiseBaseAPI,
                        "Origin" to animeparadiseBaseAPI,
                        "User-Agent" to USER_AGENT
                    )

                    if(!streamUrl.isEmpty()) {
                        callback.invoke(
                            newExtractorLink(
                                "Animeparadise [SUB]",
                                "Animeparadise [SUB]",
                                "https://stream.animeparadise.moe/m3u8?url=" + streamUrl,
                                type = ExtractorLinkType.M3U8,
                            ) {
                                this.referer = animeparadiseBaseAPI
                                this.quality = 1080
                                this.headers = headers
                            }
                        )
                    }


                    val subData = epJson.optJSONArray("subData") ?: return
                    for (i in 0 until subData.length()) {
                        val sub = subData.getJSONObject(i)
                        val label = sub.optString("label")
                        var subUrl = sub.optString("src")
                        if(!subUrl.contains("https")) {
                            subUrl = "$animeparadiseAPI/stream/file/$subUrl"
                        }
                        subtitleCallback.invoke(
                            SubtitleFile(
                                label,
                                subUrl
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun invokePhoenix(
        title: String? = null,
        imdbId: String? = null,
        tmdbId: Int? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to xprimeBaseAPI,
            "Origin" to xprimeBaseAPI,
            "User-Agent" to USER_AGENT
        )

        val url = if(season == null) {
            "$xprimeAPI/phoenix?name=$title&year=$year&id=$tmdbId&imdb=$imdbId"
        } else {
            "$xprimeAPI/phoenix?name=$title&year=$year&id=$tmdbId&imdb=$imdbId&season=$season&episode=$episode"
        }

        val json = app.get(url, headers = headers).text

        val sourceUrl = JSONObject(json).getString("url")

        callback.invoke(
            newExtractorLink(
                "Phoenix",
                "Phoenix",
                sourceUrl,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = xprimeBaseAPI
                this.headers = headers
            }
        )
    }

    suspend fun invokePrimenet(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to xprimeBaseAPI,
            "Origin" to xprimeBaseAPI,
            "User-Agent" to USER_AGENT
        )

        val url = if(season == null) {
            "$xprimeAPI/primenet?id=$tmdbId"
        } else {
            "$xprimeAPI/primenet?id=$tmdbId&season=$season&episode=$episode"
        }

        val json = app.get(url, headers = headers).text
        val sourceUrl = JSONObject(json).getString("url")

        callback.invoke(
            newExtractorLink(
                "Primenet",
                "Primenet",
                sourceUrl,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = xprimeBaseAPI
                this.headers = headers
            }
        )
    }

    suspend fun invokePrimebox(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to xprimeBaseAPI,
            "Origin" to xprimeBaseAPI,
            "User-Agent" to USER_AGENT
        )

        val url = if(season == null) {
            "$xprimeAPI/primebox?name=$title&fallback_year=$year"
        } else {
            "$xprimeAPI/primebox?name=$title&fallback_year=$year&season=$season&episode=$episode"
        }
        val json = app.get(url, headers = headers).text
        val data = tryParseJson<Primebox>(json) ?: return

        data.streams?.let { streams ->
            listOf(
                360 to streams.quality360P,
                720 to streams.quality720P,
                1080 to streams.quality1080P
            ).forEach { (quality, link) ->
                if (!link.isNullOrBlank()) {
                    callback.invoke(
                        newExtractorLink(
                            "PrimeBox",
                            "PrimeBox",
                            link,
                            type = ExtractorLinkType.VIDEO,
                        ) {
                            this.quality = quality
                            this.headers = headers
                        }
                    )
                }
            }
        }

        if (data.hasSubtitles && data.subtitles.isNotEmpty()) {
            data.subtitles.forEach { sub ->
                val file = sub.file
                val label = sub.label
                if (!file.isNullOrBlank() && !label.isNullOrBlank()) {
                    subtitleCallback.invoke(
                        SubtitleFile(
                            label,
                            file
                        )
                    )
                }
            }
        }
    }

    suspend fun invoke2embed(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to twoembedAPI,
            "sec-fetch-dest" to "iframe"
        )

        val slug = if (season != null) {
            "embedtv/$id&s=$season&e=$episode"
            } else {
            "embed/$id"
        }

        val api = "$twoembedAPI/$slug"

        val text = app.get(api, headers = headers).text
        var sKey = "swish?id="
        var start = text.indexOf(sKey)

        if(start < 0) return

        start += sKey.length

        val eKey = "'"
        var end = text.indexOf(eKey, start)
        val strmId = text.substring(start, end)

        val uplUrl = "https://uqloads.xyz/e/$strmId"

        val res = app.get(uplUrl, headers = headers).text

        sKey = "eval(function"
        start = res.indexOf(sKey)

        if(start < 0) return

        val eKey2 = "split('|')))"
        end = res.indexOf(eKey2, start) + eKey2.length
        val data = res.substring(start, end)

        val strmData = JsUnpacker(data).unpack() ?: return

        sKey = "\"hls2\":\""
        start = strmData.indexOf(sKey)

        if(start < 0) return

        start += sKey.length
        end = strmData.indexOf("\"}", start)
        val streamUrl = strmData.substring(start, end)

        callback.invoke(
            newExtractorLink(
                "2Embed",
                "2Embed",
                streamUrl,
                type = ExtractorLinkType.M3U8
            ) {
                this.headers = headers
            }
        )
    }

    suspend fun invokeAsiaflix(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val searchUrl = """https://api.asiaflix.net/v1/drama/search?q=$title&page=1&projections=["releaseYear","status","casts"]"""
        val headers = mapOf(
            "Referer" to asiaflixAPI,
            "X-Access-Control" to "web"
        )
        val jsonString = app.get(searchUrl, headers = headers).text
        val jsonObject = JSONObject(jsonString)
        val bodyArray = jsonObject.getJSONArray("body")

        var matchedId: String? = null
        var matchedName: String? = null

        for (i in 0 until bodyArray.length()) {
            val item = bodyArray.getJSONObject(i)
            val name = item.getString("name")
            val releaseYear = item.getString("releaseYear")

            if ("$title" in name && releaseYear == "$year") {
                matchedId = item.getString("_id")
                matchedName = name
                break
            }
        }

        if(matchedId != null && matchedName != null) {
            val titleSlug = matchedName.replace(" ", "-")
            val episodeUrl = "$asiaflixAPI/play/$titleSlug-1/$matchedId/1"
            val scriptText = app.get(episodeUrl).document.selectFirst("script#ng-state")?.data().toString()

            val regex = Regex("""\"streamUrls\"\s*:\s*\[\s*(.*?)\s*](?=\s*[,}])""", RegexOption.DOT_MATCHES_ALL)
            val urlRegex = Regex("""\"url\"\s*:\s*\"(.*?)\"""")

            val matches = regex.findAll(scriptText).toList()

            val newEp = episode ?: 1

            if (matches.size >= newEp) {
                val streamSection = matches[newEp-1].groupValues[1]
                urlRegex.findAll(streamSection).forEach { urlMatch ->
                    val source = httpsify(urlMatch.groupValues[1])
                    loadSourceNameExtractor("Asiaflix", source, episodeUrl, subtitleCallback, callback)
                }
            }
        }
    }

    suspend fun invokeAllmovieland(
        id : String? = null,
        season : Int? = null,
        episode : Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val host = "https://zativertz295huk.com"
        val referer = "$allmovielandAPI/"
        val res =
                app.get("$host/play/$id", referer = referer)
                        .document
                        .selectFirst("script:containsData(playlist)")
                        ?.data()
                        ?.substringAfter("{")
                        ?.substringBefore(";")
                        ?.substringBefore(")")
        val json = tryParseJson<AllMovielandPlaylist>("{${res ?: return}")
        val headers = mapOf("X-CSRF-TOKEN" to "${json?.key}")

        val serverRes =
                app.get(fixUrl(json?.file ?: return, host), headers = headers, referer = referer)
                        .text
                        .replace(Regex(""",\s*\[]"""), "")
        val servers =
                tryParseJson<ArrayList<AllMovielandServer>>(serverRes).let { server ->
                    if (season == null) {
                        server?.map { it.file to it.title }
                    } else {
                        server
                                ?.find { it.id.equals("$season") }
                                ?.folder
                                ?.find { it.episode.equals("$episode") }
                                ?.folder
                                ?.map { it.file to it.title }
                    }
                }

        servers?.amap { (server, lang) ->
            val path =
                    app.post(
                        "${host}/playlist/${server ?: return@amap}.txt",
                        headers = headers,
                        referer = referer
                    ).text
            M3u8Helper.generateM3u8("Allmovieland [$lang]", path, referer).forEach(callback)
        }
    }

    suspend fun invokeHindmoviez(
        source: String,
        id: String? = null,
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val api = if(source == "HindMoviez") hindMoviezAPI else jaduMoviesAPI
        val doc = app.get("$api/${title?.lowercase()?.replace(" ", "-") ?: return}", timeout = 50L).document
        val imdbLink = doc.select("li > span > a").attr("href")
        if(!imdbLink.contains("$id")) return

        if(episode == null) {
            doc.select("a.maxbutton").amap {
                val res = app.get(it.attr("href"), timeout = 50L).document
                val link = res.select("a.get-link-btn").attr("href")
                getHindMoviezLinks(source, link, callback)
            }
        }
        else {
            doc.select("a.maxbutton").amap {
                val text = it.parent()?.parent()?.previousElementSibling()?.text() ?: ""
                if(text.contains("Season $season")) {
                    val res = app.get(it.attr("href"), timeout = 50L).document
                    res.select("h3 > a").getOrNull(episode-1)?.let { link ->
                        getHindMoviezLinks(source, link.attr("href"), callback)
                    }
                }
            }
        }
        // app.get("$api/?s=$id", timeout = 50L).document.select("h2.entry-title > a").amap {
        //     val doc = app.get(it.attr("href"), timeout = 50L).document
        //     if(episode == null) {
        //         doc.select("a.maxbutton").amap {
        //             val res = app.get(it.attr("href"), timeout = 50L).document
        //             val link = res.select("a.get-link-btn").attr("href")
        //             getHindMoviezLinks(source, link, callback)
        //         }
        //     }
        //     else {
        //         doc.select("a.maxbutton").amap {
        //             val text = it.parent()?.parent()?.previousElementSibling()?.text() ?: ""
        //             if(text.contains("Season $season")) {
        //                 val res = app.get(it.attr("href"), timeout = 50L).document
        //                 res.select("h3 > a").getOrNull(episode-1)?.let { link ->
        //                     getHindMoviezLinks(source, link.attr("href"), callback)
        //                 }
        //             }
        //         }
        //     }
        // }
    }

    suspend fun invokeStreamAsia(
        title: String? = null,
        provider: String,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val type = "series"
        var json = app.get("$StreamAsiaAPI/catalog/kisskh/kkh-search-results/search=$title.json").text
        val searhData = tryParseJson<StreamAsiaSearch>(json) ?: return
        val id = searhData.metas.firstOrNull { meta ->
            meta.type == type && (
                if(season == null) {
                    meta.name.equals(title, ignoreCase = true)
                }
                else if (season == 1) {
                    meta.name.equals(title, ignoreCase = true) ||
                    meta.name.equals("$title Season 1", ignoreCase = true)
                }
                else {
                    meta.name.equals("$title Season $season", ignoreCase = true)
                }
            )
        }?.id ?: return

        val epJson = app.get("$StreamAsiaAPI/meta/$type/$id.json").text
        val epData = tryParseJson<StreamAsiaInfo>(epJson) ?: return
        val epId = epData.meta.videos.firstOrNull { video ->
            video.episode == episode ?: 1
        }?.id ?: return

        val streamJson = app.get("$StreamAsiaAPI/stream/$type/$epId.json").text
        val streamData = tryParseJson<StreamAsiaStreams>(streamJson)

        if(streamData != null) {
            streamData.streams.forEach {
                callback.invoke(
                    newExtractorLink(
                        "Kisskh",
                        "Kisskh",
                        it.url ?: return@forEach,
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.headers = mapOf(
                            "Referer" to "https://kisskh.co/",
                            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"
                        )
                    }
                )
            }
        }

        val subtitleJson = app.get("$StreamAsiaAPI/subtitles/$type/$epId.json").text
        val subtitleData = tryParseJson<StreamAsiaSubtitles>(subtitleJson)

        if(subtitleData != null) {
            subtitleData.subtitles.forEach {
                val lang = it.lang ?: "und"
                subtitleCallback.invoke(
                    SubtitleFile(
                        lang.replace("(OpenSubs) ", ""),
                        it.url ?: return@forEach,
                    )
                )
            }
        }
    }

    suspend fun invokeTokyoInsider(
        title: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val tvtype = if(episode == null) "_(Movie)" else "_(TV)"
        val firstChar = getFirstCharacterOrZero("$title").uppercase()
        val newTitle = title?.replace(" ","_")
        val doc = app.get("$tokyoInsiderAPI/anime/$firstChar/$newTitle$tvtype", timeout = 500L).document
        val selector = if(episode != null) "a.download-link:matches((?i)(episode $episode\\b))" else "a.download-link"
        val aTag = doc.selectFirst(selector)
        val epUrl = aTag?.attr("href") ?: return
        val res = app.get(tokyoInsiderAPI + epUrl, timeout = 500L).document
        res.select("div.c_h2 > div > a").map {
            val name = it.text()
            val url = it.attr("href")
            callback.invoke(
                newExtractorLink(
                    "TokyoInsider",
                    "[TokyoInsider] - $name",
                    url,
                ) {
                    this.quality = getIndexQuality(name)
                }
            )
        }
    }

    suspend fun invokeDisney(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        if(netflixAPI.isEmpty()) return
        val NfCookie = NFBypass(netflixAPI)
        val cookies = mapOf(
            "t_hash_t" to NfCookie,
            "ott" to "dp",
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/mobile/hs/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals("${title?.trim()}", ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/mobile/hs/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/",
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                var episodeId : String? = null
                var page = 1

                while(episodeId == null && page < 10) {
                    val data = app.get(
                        "$netflixAPI/mobile/hs/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}&page=$page",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/",
                    ).parsedSafe<NetflixResponse>()
                    episodeId = data?.episodes?.find { it.ep == "E$episode" }?.id
                    if(data?.nextPageShow != 1) { break }
                    page++
                }

                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/mobile/hs/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/",
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                newExtractorLink(
                    "Disney",
                    "Disney",
                    "$netflixAPI/${it.file}",
                ) {
                    this.referer = "$netflixAPI/"
                    this.quality = getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in"))
                    this.headers = mapOf("Cookie" to "hd=on")
                }
            )
        }
    }

    suspend fun invokePrimeVideo(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        if(netflixAPI.isEmpty()) return
        val NfCookie = NFBypass(netflixAPI)
        val cookies = mapOf(
            "t_hash_t" to NfCookie,
            "ott" to "pv",
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/mobile/pv/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals("${title?.trim()}", ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/mobile/pv/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/",
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                var episodeId : String? = null
                var page = 1

                while(episodeId == null && page < 10) {
                    val data = app.get(
                        "$netflixAPI/mobile/pv/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}&page=$page",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/",
                    ).parsedSafe<NetflixResponse>()
                    episodeId = data?.episodes?.find { it.ep == "E$episode" }?.id
                    if(data?.nextPageShow != 1) { break }
                    page++
                }

                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/mobile/pv/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/",
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            val source = if(it.file?.contains(netflixAPI) == true) {
                "${it.file}"
            } else {
                "$netflixAPI/${it.file}"
            }
            callback.invoke(
                newExtractorLink(
                    "PrimeVideo",
                    "PrimeVideo",
                    source,
                ) {
                    this.referer = "$netflixAPI/"
                    this.quality = getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in"))
                    this.headers = mapOf("Cookie" to "hd=on")
                }
            )
        }
    }

    suspend fun invokeNetflix(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        if(netflixAPI.isEmpty()) return

        val NfCookie = NFBypass(netflixAPI)

        val cookies = mapOf(
            "t_hash_t" to NfCookie,
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/mobile/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals("${title?.trim()}", ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/mobile/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/",
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                var episodeId : String? = null
                var page = 1
                while(episodeId == null && page < 10) {
                    val data = app.get(
                        "$netflixAPI/mobile/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}&page=$page",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/",
                    ).parsedSafe<NetflixResponse>()
                    episodeId = data?.episodes?.find { it.ep == "E$episode" }?.id
                    if(data?.nextPageShow != 1) { break }
                    page++
                }
                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/mobile/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/",
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                newExtractorLink(
                    "Netflix",
                    "Netflix",
                    "$netflixAPI/${it.file}",
                ) {
                    this.referer = "$netflixAPI/"
                    this.quality = getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in"))
                    this.headers = mapOf("Cookie" to "hd=on")
                }
            )
        }
    }

    suspend fun invokeHdmovie2(
        title: String? = null,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val headers = mapOf("User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
        val document = app.get("$hdmovie2API/movies/${title.createSlug()}-$year", headers = headers, allowRedirects = true).document

        document.select("div.wp-content p a").amap {

            if(episode != null && it.text().contains("EP")) {
                if(
                    it.text().contains("EP$episode")||
                    it.text().contains("EP0$episode")
                ) {
                    app.get(it.attr("href")).document.select("div > p > a").amap {
                        loadSourceNameExtractor(
                            "Hdmovie2",
                            it.attr("href"),
                            "",
                            subtitleCallback,
                            callback,
                        )
                    }
                }
            }
            else {
                val type = if(episode != null) "(Combined)" else ""
                app.get(it.attr("href")).document.select("div > p > a").amap {
                    loadSourceNameExtractor(
                        "Hdmovie2$type",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
            }
        }
    }

    suspend fun invokeSkymovies(
        title: String? = null,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = "$skymoviesAPI/search.php?search=$title ($year)&cat=All"
        app.get(url).document.select("div.L a").amap {
            app.get(skymoviesAPI + it.attr("href")).document.select("div.Bolly > a").amap {
                val text = it.text()
                if(episode == null) {
                  loadSourceNameExtractor(
                        "Skymovies",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
                else if(text.contains("Episode")) {
                    if(text.contains("Episode $episode") || text.contains("Episode 0$episode")) {
                        loadSourceNameExtractor(
                            "Skymovies",
                            it.attr("href"),
                            "",
                            subtitleCallback,
                            callback,
                        )
                    }
                }
                else {
                    loadSourceNameExtractor(
                        "Skymovies(Combined)",
                        it.attr("href"),
                        "",
                        subtitleCallback,
                        callback,
                    )
                }
            }
        }
    }

    suspend fun invokeTom(
        id: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val url = if(season == null) "$TomAPI/api/getVideoSource?type=movie&id=$id" else "$TomAPI/api/getVideoSource?type=tv&id=$id/$season/$episode"
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:101.0) Gecko/20100101 Firefox/101.0",
            "Referer" to "https://autoembed.cc"
        )
        val json = app.get(url, headers = headers).text
        val data = tryParseJson<TomResponse>(json) ?: return

        callback.invoke(
            newExtractorLink(
                "Tom",
                "Tom",
                data.videoSource,
                ExtractorLinkType.M3U8
            )
        )

        data.subtitles.map {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.label,
                    it.file,
                )
            )
        }
    }

    suspend fun invokeProtonmovies(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Referer" to "$protonmoviesAPI/"
        )
        val url = "$protonmoviesAPI/search/$id/"
        val text = app.get(url, headers = headers).text
        val regex = Regex("""\[(?=.*?\"<div class\")(.*?)\]""")
        val htmlArray = regex.findAll(text).map { it.value }.toList()
        if (htmlArray.isNotEmpty()) {
            val lastJsonArray = JSONArray(htmlArray.last())
            val html = decodeHtml(Array(lastJsonArray.length()) { i -> lastJsonArray.getString(i) })
            val doc = Jsoup.parse(html)
            val link = doc.select(".col.mb-4 h5 a").attr("href")
            val document = app.get("${protonmoviesAPI}${link}", headers = headers).document
            val decodedDoc = decodeMeta(document)
            if (decodedDoc != null) {
                if(episode == null) {
                    getProtonStream(decodedDoc, protonmoviesAPI,subtitleCallback, callback)
                } else{
                    val episodeDiv = decodedDoc.select("div.episode-block:has(div.episode-number:matchesOwn(S${season}E${episode}))").firstOrNull()
                    episodeDiv?.selectFirst("a")?.attr("href")?.let {
                        val source = protonmoviesAPI + it
                        val doc2 = app.get(source, headers = headers).document
                        runAllAsync(
                            {
                                val scriptText = doc2.selectFirst("script:containsData(strm.json)")?.data().toString()
                                getProtonEmbed(scriptText, protonmoviesAPI, subtitleCallback, callback)
                            },
                            {
                                val decodedDoc = decodeMeta(doc2)
                                if(decodedDoc != null) {
                                    getProtonStream(decodedDoc, protonmoviesAPI, subtitleCallback, callback)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    suspend fun invokeMoviesflix(
        sourceName: String,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val api = if(sourceName == "Moviesflix") moviesflixAPI else hdmoviesflixAPI

        app.get("$api/?s=$id").document.select("a.post-image").amap {
            val doc = app.get(it.attr("href")).document
            if(episode == null) {
                doc.select("a.maxbutton").amap { button ->
                    val document = app.get(button.attr("href")).document
                    document.select("a.maxbutton").amap { source ->
                        val linkText = source.text()
                        if (!linkText.contains("Google Drive", ignoreCase = true) &&
                            !linkText.contains("Other Download Links", ignoreCase = true) &&
                            !linkText.contains("G-Direct", ignoreCase = true)
                        ) {
                            loadSourceNameExtractor(
                                sourceName,
                                source.attr("href"),
                                "",
                                subtitleCallback,
                                callback,
                            )
                        }
                    }
                }
            }
            else {
                doc.select("p:has(a.maxbutton)").amap { p ->
                    val possibleMatches = listOf(
                        "Season $season",
                        "Season 0$season",
                        "S$season",
                        "S0$season"
                    )
                    if(possibleMatches.any {
                        p.previousElementSibling()?.previousElementSibling()?.text()?.contains(it) == true
                    }) {
                        p.select("a.maxbutton").amap { button ->
                            val buttonText = button.text()
                            if(!buttonText.contains("G-Direct", ignoreCase = true) &&
                                !buttonText.contains("Drop Galaxy", ignoreCase = true) &&
                                !buttonText.contains("G-Drive", ignoreCase = true) &&
                                !buttonText.contains("Mega.nz", ignoreCase = true)
                            ) {
                                app.get(button.attr("href")).document.select("h3 > strong > a").getOrNull(episode-1)?.let { source ->
                                    loadSourceNameExtractor(
                                        sourceName,
                                        source.attr("href"),
                                        "",
                                        subtitleCallback,
                                        callback,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun invokeMovies4u(
        id: String? = null,
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val searchQuery = "$title $year".trim()
        val searchUrl = "$movies4uAPI/?s=$searchQuery"

        val searchDoc = app.get(searchUrl).document
        val links = searchDoc.select("article h3 a")

        links.amap { element ->
            val postUrl = element.attr("href")
            val postDoc = app.get(postUrl).document
            val imdbId = postDoc.select("p a:contains(IMDb Rating)").attr("href")
                            .substringAfter("title/").substringBefore("/")

            if(imdbId != id.toString()) { return@amap }

            if (season == null) {
                val innerUrl = postDoc.select("div.download-links-div a.btn").attr("href")
                val innerDoc = app.get(innerUrl).document
                val sourceButtons = innerDoc.select("div.downloads-btns-div a.btn")
                sourceButtons.amap { sourceButton ->
                    val sourceLink = sourceButton.attr("href")
                    loadSourceNameExtractor(
                        "Movies4u",
                        sourceLink,
                        "",
                        subtitleCallback,
                        callback
                    )
                }
            } else {
                val seasonBlocks = postDoc.select("div.downloads-btns-div")
                seasonBlocks.amap { block ->
                    val headerText = block.previousElementSibling()?.text().orEmpty()
                    if (headerText.contains("Season $season", ignoreCase = true)) {
                        val seasonLink = block.selectFirst("a.btn")?.attr("href") ?: return@amap

                        val episodeDoc = app.get(seasonLink).document
                        val episodeBlocks = episodeDoc.select("div.downloads-btns-div")

                        if (episode != null && episode in 1..episodeBlocks.size) {
                            val episodeBlock = episodeBlocks[episode - 1]
                            val episodeLinks = episodeBlock.select("a.btn")

                            episodeLinks.amap { epLink ->
                                val sourceLink = epLink.attr("href")
                                loadSourceNameExtractor(
                                    "Movies4u",
                                    sourceLink,
                                    "",
                                    subtitleCallback,
                                    callback
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun invokeTorrentio(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) {
            "$torrentioAPI/$torrentioCONFIG/stream/movie/$id.json"
        }
        else {
            "$torrentioAPI/$torrentioCONFIG/stream/series/$id:$season:$episode.json"
        }
        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
        )
        val res = app.get(url, headers = headers, timeout = 200L).parsedSafe<TorrentioResponse>()
        val resp = app.get(TRACKER_LIST_URL).text
        val sourceTrackers = resp
            .split("\n")
            .filterIndexed { i, _ -> i % 2 == 0 }
            .filter { s -> s.isNotEmpty() }.joinToString("") { "&tr=$it" }

        res?.streams?.forEach { stream ->
            val title = stream.title ?: stream.name ?: ""
            val regex = Regex("""\uD83D\uDC64\s*(\d+)""")
            val match = regex.find(title)
            val seeders = match?.groupValues?.get(1)?.toInt() ?: 0
            if (seeders < 10) return@forEach
            val magnet = "magnet:?xt=urn:btih:${stream.infoHash}&dn=${stream.infoHash}$sourceTrackers&index=${stream.fileIdx}"
            callback.invoke(
                newExtractorLink(
                    "Torrentio",
                    title,
                    magnet,
                    ExtractorLinkType.MAGNET,
                ) {
                    this.quality = getIndexQuality(stream.name)
                }
            )
        }
    }

    suspend fun invokeCinemaluxe(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val query = "$title $year"
        val url = app.get("$cinemaluxeAPI/?s=$query").document
            .select("div.title > a:contains($query)").attr("href")
        val document = app.get(url).document

        if(season == null) {
            document.select("div.wp-content div.ep-button-container > a").amap {
                val link = cinemaluxeBypass(it.attr("href"))
                loadSourceNameExtractor(
                    "Cinemaluxe",
                    link,
                    "",
                    subtitleCallback,
                    callback,
                )
            }
        }
        else {
            document.select("div.wp-content div.ep-button-container").amap { div ->
                val text = div.toString()
                if(text.contains("Season $season", ignoreCase = true) ||
                    text.contains("Season 0$season", ignoreCase = true)
                ) {
                    val link = cinemaluxeBypass(div.select("a").attr("href"))

                    app.get(link).document.select("""div.ep-button-container > a:matches((?i)(?:episode\s*[-]?\s*)(0?$episode\b))""").amap {
                        loadSourceNameExtractor(
                            "Cinemaluxe",
                            it.attr("href"),
                            "",
                            subtitleCallback,
                            callback,
                        )
                    }
                }
            }
        }
    }

    suspend fun invokeBollyflix(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        var res1 = app.get("""$bollyflixAPI/search/${id ?: return} ${season ?: ""}""", interceptor = wpRedisInterceptor).document
        val url = res1.select("div > article > a").attr("href") ?: return
        val res = app.get(url).document
        val hTag = if (season == null) "h5" else "h4"
        val sTag = if (season == null) "" else "Season $season"
        val entries =
            res.select("div.thecontent.clearfix > $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                ?.filter { element -> !element.text().contains("Download", true) }?.takeLast(4)
        entries?.map {
            val href = it.nextElementSibling()?.select("a")?.attr("href")
            val token = href?.substringAfter("id=")
            val encodedurl =
                app.get("https://web.sidexfee.com/?id=$token").text.substringAfter("link\":\"")
                    .substringBefore("\"};")
            val decodedurl = base64Decode(encodedurl.replace("\\/", "/"))

            if (season == null) {
                loadSourceNameExtractor("Bollyflix", decodedurl , "", subtitleCallback, callback)
            } else {
                val episodeText = "Episode " + episode.toString().padStart(2, '0')
                val link =
                    app.get(decodedurl).document.selectFirst("article h3 a:contains($episodeText)")!!
                        .attr("href")
                loadSourceNameExtractor("Bollyflix", link , "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeStreamify(
        id: String? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$stremifyAPI/movie/$id.json"
        val json = app.get(url).text
        val data = tryParseJson<StreamifyResponse>(json) ?: return
        data.streams.forEach {
            callback.invoke(
                newExtractorLink(
                    it.name,
                    "[${it.name}] ${it.title}",
                    it.url,
                )
            )
        }
    }

    suspend fun invokeMultimovies(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = title.createSlug()
        val url = if (season == null) {
            "$multimoviesAPI/movies/$fixTitle"
        } else {
            "$multimoviesAPI/episodes/$fixTitle-${season}x${episode}"
        }
        val req = app.get(url).document
        req.select("ul#playeroptionsul li").map {
            Triple(
                it.attr("data-post"),
                it.attr("data-nume"),
                it.attr("data-type")
            )
        }.amap { (id, nume, type) ->
            if (!nume.contains("trailer")) {
                val source = app.post(
                    url = "$multimoviesAPI/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "doo_player_ajax",
                        "post" to id,
                        "nume" to nume,
                        "type" to type
                    ),
                    referer = url,
                    headers = mapOf("X-Requested-With" to "XMLHttpRequest")
                ).parsed<ResponseHash>().embed_url
                val link = source.substringAfter("\"").substringBefore("\"")
                when {
                    !link.contains("youtube") -> {
                        loadSourceNameExtractor("Multimovies", link, referer = multimoviesAPI, subtitleCallback, callback)
                    }
                    else -> ""
                }
            }
        }
    }

    suspend fun invokeAniXL(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url ?: return).document
        val baseUrl = getBaseUrl(url)
        val epLink = document.select("div.flex-wrap > a.btn")
            .firstOrNull { it?.text()?.trim() == "${episode ?: 1}" }
            ?.attr("href")
            ?.takeIf { it.isNotBlank() }
            ?: return
        val epText = app.get(baseUrl + epLink).text
        val types = listOf("dub", "sub", "raw")

        types.forEach {
            val Regex = """\"${it}\",\"([^\"]+)\"""".toRegex()
            val epUrl = Regex.find(epText)?.groupValues?.get(1) ?: return@forEach
            val isDub = if(it == "dub") "[DUB]" else "[SUB]"

            callback.invoke(
                newExtractorLink(
                    "AniXL $isDub",
                    "AniXL $isDub",
                    epUrl,
                    ExtractorLinkType.M3U8
                ) {
                    this.quality = 1080
                }
            )
        }

        val subtitleRegex = """\"([^\"]+)\",\"[^\"]*\",\"(https?:\/\/[^\"]+\.vtt)\"""".toRegex()
        val subtitles = subtitleRegex.findAll(epText)
        .map { match ->
            val (language, subUrl) = match.destructured
            subtitleCallback.invoke(
                SubtitleFile(
                    language,
                    subUrl
                )
            )
        }
    }

    suspend fun invokeAnimes(
        malId: Int? = null,
        aniId: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        origin: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val malsync = app.get("$malsyncAPI/mal/anime/${malId ?: return}")
            .parsedSafe<MALSyncResponses>()?.sites

        // val zoroIds = malsync?.zoro?.keys?.map { it }
        val zorotitle = malsync?.zoro?.firstNotNullOf { it.value["title"] }?.replace(":"," ")
        val hianimeurl = malsync?.zoro?.firstNotNullOf { it.value["url"] }
        val animepaheUrl = malsync?.animepahe?.values?.firstNotNullOfOrNull { it["url"] }
        val aniXL = malsync?.AniXL?.values?.firstNotNullOfOrNull { it["url"] }

        runAllAsync(
            {
                invokeHianime(hianimeurl, episode, subtitleCallback, callback)
            },
            {
                if (animepaheUrl != null)
                    invokeAnimepahe(animepaheUrl, episode, subtitleCallback, callback)
            },
            {
                invokeAniXL(aniXL, episode, subtitleCallback, callback)
            },
            {
                invokeAnimez(zorotitle, episode, callback)
            },
            {
                if(origin == "imdb" && zorotitle != null) invokeTokyoInsider(
                    zorotitle,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb" && zorotitle != null) invokeAllanime(
                    zorotitle,
                    year,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb" && zorotitle != null) invokeAnizone(
                    zorotitle,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb") invokeGojo(
                    aniId,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb") invokeSudatchi(
                    aniId,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb" && zorotitle != null) invokeAnimeparadise(
                    zorotitle,
                    malId,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
        )
    }

    private suspend fun invokeAnimepahe(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to USER_AGENT,
            "Cookie" to "__ddg2_=1234567890"
        )
        val id = app.get("$proxyAPI/$url" ?: return, headers).document.selectFirst("meta[property=og:url]")
            ?.attr("content").toString().substringAfterLast("/")
        val animeData =
            app.get("$proxyAPI/$animepaheAPI/api?m=release&id=$id&sort=episode_asc&page=1", headers)
                .parsedSafe<animepahe>()?.data
        val session = if(episode == null) {
            animeData?.firstOrNull()?.session ?: return
        } else {
            animeData?.getOrNull(episode-1)?.session ?: return
        }
        val doc = app.get("$proxyAPI/$animepaheAPI/play/$id/$session", headers).document

        runAllAsync(
            {
                doc.select("div#pickDownload > a").amap {
                    val href = it.attr("href")
                    var type = "SUB"
                    if(it.select("span").text().contains("eng")) type = "DUB"

                    loadCustomExtractor(
                        "Animepahe [$type]",
                        href,
                        "",
                        subtitleCallback,
                        callback,
                        getIndexQuality(it.text())
                    )
                }
            },
            {
                doc.select("div#resolutionMenu > button").amap {
                    var type = "SUB"
                    if(it.select("span").text().contains("eng")) type = "DUB"
                    val quality = it.attr("data-resolution")
                    val href = it.attr("data-src")
                    if (href.contains("kwik.si")) {
                        loadCustomExtractor(
                            "Animepahe(VLC) [$type]",
                            href,
                            mainUrl,
                            subtitleCallback,
                            callback,
                            getQualityFromName(quality)
                        )
                    }
                }
            },
        )
    }

    // suspend fun invokeRar(
    //     title: String,
    //     year: Int? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     callback: (ExtractorLink) -> Unit,
    // ) {
    //     val json = app.get("$RarAPI/ajax/posts?q=$title ($year)").text
    //     val responseData = parseJson<RarResponseData>(json)
    //     val id = responseData.data?.firstOrNull {
    //         it.second_name == title
    //     }?.id ?: return
    //     val slug = "$title $year $id".createSlug()
    //     val url = if(season != null) "$RarAPI/show/$slug/season/$season/episode/$episode" else "$RarAPI/movie/$slug"
    //     val embedId = app.get(url).document.selectFirst("a.btn-service")?.attr("data-embed") ?: return
    //     val body = FormBody.Builder().add("id", embedId).build()
    //     val document = app.post("$RarAPI/ajax/embed", requestBody = body).document
    //     val regex = Regex("""(https?:\/\/[^\"']+\.m3u8)""")
    //     val link = regex.find(document.toString())?.groupValues?.get(1) ?: return
    //     callback.invoke(
    //         ExtractorLink(
    //             "Rar",
    //             "Rar",
    //             link,
    //             referer = "",
    //             Qualities.P1080.value,
    //             true
    //         )
    //     )
    // }

    suspend fun invoke4khdhub(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val document = app.get("$fourkhdhubAPI/?s=$title").document
        val link = document.select("div.card-grid > a").firstOrNull { element ->
            val content = element.selectFirst("div.movie-card-content")?.text()?.lowercase() ?: return@firstOrNull false
            val matchTitle = title?.lowercase()?.let { it in content } ?: true
            val matchYear = year?.toString()?.lowercase()?.let { it in content } ?: true
            matchTitle && matchYear
        }?.attr("href") ?: return

        val doc = app.get("$fourkhdhubAPI$link").document
        if(season == null) {
            doc.select("div.download-item a").amap {
               val source = getRedirectLinks(it.attr("href"))
               loadSourceNameExtractor(
                    "4Khdhub",
                    source,
                    "",
                    subtitleCallback,
                    callback
                )
            }
        } else {
            val seasonText = "S" + season.toString().padStart(2, '0')
            val episodeText = "E" + episode.toString().padStart(2, '0')
            doc.select("div.episode-download-item:has(div.episode-file-title:contains(${seasonText}${episodeText}))").amap {
                it.select("div.episode-links > a").amap {
                    val source = getRedirectLinks(it.attr("href"))
                    loadSourceNameExtractor(
                        "4Khdhub",
                        source,
                        "",
                        subtitleCallback,
                        callback
                    )
                }
            }
        }
    }

    suspend fun invokeMostraguarda(
        id: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = "$MostraguardaAPI/movie/$id"
        val doc = app.get(
            url,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
            )
        ).document

        doc.select("ul > li").amap {
            if(it.text().contains("supervideo")) {
                val source = "https:" + it.attr("data-link")
                SuperVideo().getUrl(source, "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeWYZIESubs(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null) "$WYZIESubsAPI/search?id=$id&season=$season&episode=$episode" else "$WYZIESubsAPI/search?id=$id"
        val json = app.get(url).text
        val data = parseJson<ArrayList<WYZIESubtitle>>(json)

        data.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.display ?: it.language ?: "Unknown",
                    it.url
                )
            )
        }
    }

    suspend fun invokeW4U(
        title: String? = null,
        year: Int? = null,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) "$W4UAPI/?s=$title $year" else "$W4UAPI/?s=$title $season"
        val document = app.get(url).document
        val link = document.selectFirst("div.post-thumb > a")?.attr("href")
        val doc = app.get(link ?: return).document
        val imdbId = doc.selectFirst("div.imdb_left > a")?.attr("href")?.substringAfter("title/")?.substringBefore("/") ?: ""
        if("$id" == imdbId) {
            if(season != null && episode != null) {
                doc.select("a.my-button").mapNotNull {
                    val title = it.parent()?.parent()?.previousElementSibling()?.text()?: ""
                    val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                    val quality = qualityRegex.find(title) ?. groupValues ?. get(1) ?: ""
                    val realSeason = Regex("""(?:Season |S)(\d+)""").find(title) ?. groupValues ?. get(1) ?. toIntOrNull() ?: 300
                    if(season == realSeason) {
                        val doc2 = app.get(it.attr("href")).document
                        val h3 = doc2.select("h3:matches((?i)(episode))").get(episode-1)
                        var source = h3?.nextElementSibling()?.selectFirst("a")?.attr("href") ?: ""
                        loadSourceNameExtractor("W4U", source, "", subtitleCallback, callback, getIndexQuality(quality))
                    }
                    else {
                    }
                }
            }
            else {
                doc.select("a.my-button").mapNotNull {
                    val title = it.parent()?.parent()?.previousElementSibling()?.text()?: ""
                    val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                    val quality = qualityRegex.find(title) ?. groupValues ?. get(1) ?: ""
                    app.get(it.attr("href")).document.select("h4 > a").mapNotNull {
                        loadSourceNameExtractor("W4U", it.attr("href"), "", subtitleCallback, callback, getIndexQuality(quality))
                    }
                }
            }
        }
    }

    suspend fun invokeVegamovies(
        sourceName: String,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
            "Cache-Control" to "no-store",
            "Accept-Language" to "en-US,en;q=0.9",
            "DNT" to "1",
            "sec-ch-ua" to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Microsoft Edge\";v=\"120\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Windows\"",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1",
            "Cookie" to "_lscache_vary=62abf8b96599676eb8ec211cffaeb8ff; ext_name=ojplmecpdpgccookcobabopnaifgidhf; cf_clearance=n4Y1XTKZ5TfIMBNQuAXzerwKpx0U35KoOm3imfT0GpU-1732097818-1.2.1.1-ZeAnEu.8D9TSZHYDoj7vwo1A1rpdKl304ZpaBn_QbAQOr211JFAb7.JRQU3EL2eIy1Dfl8HhYvH7_259.22lUz8gbchHcQ8hvfuQXMtFMCbqDBLzjNUZa9stuk.39l28IcPhH9Z2szsf3SGtNI1sAfo66Djt7sOReLK3lHw9UkJp7BdGqt6a2X9qAc8EsAI3lE480Tmt0fkHv14Oc30LSbPB_WwFmiqAki2W.Gv9hV7TN_QBFESleTDlXd.6KGflfd4.KwWF7rpSRo_cgoc9ALLLIafpxHVbe7_g5r7zvpml_Pj8fEL75fw.1GBuy16bciHBuB8s_kahuJYUnhtQFFgfTQl8_Gn6KeovBWx.PJ7nFv5sklHUfAyBVq3t30xKe8ZDydsQ_G.yipfj_In5GmmWcXGb6E4.bioDOwW_sKLtxwdTQt7Nu.RkILX_mKvXNpyLqflIVj8G7X5E8I.unw",
            "Upgrade-Insecure-Requests" to "1",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0"
        )
        val api = if(sourceName == "VegaMovies") vegamoviesAPI else rogmoviesAPI

        val url = "$api/?s=${id ?: return}"
        app.get(
            url,
            referer = api,
            headers = headers
        ).document.select("article h2 a,article h3 a").amap {
            val res = app.get(
                it.attr("href"),
                referer = api,
                headers = headers
            ).document
            if(season == null) {
                res.select("button.dwd-button").amap {
                    val link = it.parent()?.attr("href") ?: return@amap
                    val doc = app.get(link).document
                    val source = doc.selectFirst("button.btn:matches((?i)(V-Cloud))")
                        ?.parent()
                        ?.attr("href")
                        ?: return@amap
                    loadSourceNameExtractor(sourceName, source, referer = "", subtitleCallback, callback)
                }
            }
            else {
                res.select("h4:matches((?i)(Season $season)), h3:matches((?i)(Season $season))").amap { h4 ->
                    h4.nextElementSibling()?.select("a:matches((?i)(V-Cloud|Single|Episode|G-Direct))")?.amap {
                        val doc = app.get(it.attr("href")).document
                        val epLink = doc.selectFirst("h4:contains(Episodes):contains($episode)")
                            ?.nextElementSibling()
                            ?.selectFirst("a:matches((?i)(V-Cloud))")
                            ?.attr("href")
                            ?: return@amap
                        loadSourceNameExtractor(sourceName, epLink, referer = "", subtitleCallback, callback)
                    }
                }
            }
        }
    }

    suspend fun invokeMoviesdrive(
        title: String? = null,
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$MovieDrive_API/search/$title"
        val res = app.get(url, interceptor = wpRedisInterceptor).document
        res.select("li.thumb > figcaption > a").amap {
            val document = app.get(it.attr("href"), interceptor = wpRedisInterceptor).document
            val imdbId =  document.select("a[href*=\"imdb\"]").attr("href").substringAfter("title/").substringBefore("/")
            if(imdbId == id.orEmpty()) {
                if (season == null) {
                    document.select("h5 > a").amap {
                        val href = it.attr("href")
                        val server = extractMdrive(href)
                        server.amap {
                            loadSourceNameExtractor("MoviesDrive",it, "", subtitleCallback, callback)
                        }
                    }
                } else {
                    val stag = "Season $season|S0$season"
                    val sep = "Ep0$episode|Ep$episode"
                    val entries = document.select("h5:matches((?i)$stag)")
                    entries.amap { entry ->
                        val href = entry.nextElementSibling()?.selectFirst("a")?.attr("href") ?: ""

                        if (href.isNotBlank()) {
                            val doc = app.get(href).document
                            val fEp = doc.selectFirst("h5:matches((?i)$sep)")
                            val linklist = mutableListOf<String>()
                            val source1 = fEp?.nextElementSibling()?.selectFirst("a")?.attr("href")
                            val source2 = fEp?.nextElementSibling()?.nextElementSibling()?.selectFirst("a")?.attr("href")
                            if (source1 != null) linklist.add(source1)
                            if (source2 != null) linklist.add(source2)

                            linklist.amap { url ->
                                loadSourceNameExtractor(
                                    "MoviesDrive",
                                    url,
                                    referer = "",
                                    subtitleCallback,
                                    callback
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun invokeUhdmovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val url = app.get("$uhdmoviesAPI/search/$title $year").document
            .select("article div.entry-image a").attr("href")
        val doc = app.get(url).document

        val selector = if (season == null) {
            "div.entry-content p:matches($year)"
        } else {
            "div.entry-content p:matches((?i)(S0?$season|Season 0?$season))"
        }
        val epSelector = if (season == null) {
            "a:matches((?i)(Download))"
        } else {
            "a:matches((?i)(Episode $episode))"
        }

        val links = doc.select(selector).mapNotNull {
            val nextElementSibling = it.nextElementSibling()
            nextElementSibling?.select(epSelector)?.attr("href")
        }

        links.amap {
            if(!it.isNullOrEmpty()) {
                val driveLink = if(it.contains("driveleech")) {
                    val baseUrl = getBaseUrl(it)
                    val text = app.get(it).text
                    val regex = Regex("""window\.location\.replace\(["'](.*?)["']\)""")
                    val fileId = regex.find(text)?.groupValues?.get(1) ?: return@amap
                    baseUrl + fileId
                } else {
                    bypassHrefli(it) ?: return@amap
                }
                loadSourceNameExtractor(
                    "UHDMovies",
                    driveLink,
                    "",
                    subtitleCallback,
                    callback,
                )
            }
        }
    }

    suspend fun invokeTopMovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = title?.replace("-", " ")?.substringBefore(":")
        var url = ""
        val searchtitle = title?.replace("-", " ")?.substringBefore(":").createSlug()
        if (season == null) {
            url = "$topmoviesAPI/search/$fixTitle $year"
        } else {
            url = "$topmoviesAPI/search/$fixTitle Season $season $year"
        }
        var res1 =
            app.get(url).document.select("#content_box article")
                .toString()
        val hrefpattern =
            Regex("""(?i)<article[^>]*>\s*<a\s+href="([^"]*$searchtitle[^"]*)\"""").find(res1)?.groupValues?.get(
                1
            )
        val hTag = if (season == null) "h3" else "div.single_post h3"
        val aTag = if (season == null) "Download" else "G-Drive"
        val sTag = if (season == null) "" else "(Season $season)"
        //val media =res.selectFirst("div.post-cards article:has(h2.title.front-view-title:matches((?i)$title.*$match)) a")?.attr("href")
        val res = app.get(
            hrefpattern ?: return,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"),
            interceptor = wpRedisInterceptor
        ).document
        val entries = if (season == null) {
            res.select("$hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p|4K))")
                .filter { element -> !element.text().contains("Batch/Zip", true) && !element.text().contains("Info:", true) }.reversed()
        } else {
            res.select("$hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p|4K))")
                .filter { element -> !element.text().contains("Batch/Zip", true) || !element.text().contains("720p & 480p", true) || !element.text().contains("Series Info", true)}
        }
        entries.amap {
            val href =
                it.nextElementSibling()?.select("a.maxbutton:contains($aTag)")?.attr("href")
            val selector =
                if (season == null) "a.maxbutton-5:contains(Server)" else "h3 a:matches(Episode $episode)"
            if (href!!.isNotEmpty()) {
                app.get(
                    href ?: "",
                    headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"),
                    interceptor = wpRedisInterceptor, timeout = 10L
                ).document.selectFirst(selector)
                    ?.attr("href")?.let {
                        val link = bypassHrefli(it).toString()
                        loadSourceNameExtractor("Topmovies", link, referer = "", subtitleCallback, callback)
                    }
            }
        }
    }

    suspend fun invokeMoviesmod(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeModflix(
            id,
            season,
            episode,
            subtitleCallback,
            callback,
            MoviesmodAPI
        )
    }

    suspend fun invokeModflix(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        var url = ""
        if (season == null) {
            url = "$api/search/$id"
        } else {
            url = "$api/search/$id $season"
        }
        var href = app.get(url).document.selectFirst("#content_box article > a")?.attr("href")
        val hTag = if (season == null) "h4" else "h3"
        val aTag = if (season == null) "Download" else "Episode"
        val sTag = if (season == null) "" else "(S0$season|Season $season)"
        val res = app.get(
            href ?: return,
        ).document
        val entries =
            res.select("div.thecontent $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                .filter { element ->
                    !element.text().contains("MoviesMod", true) && !element.text()
                        .contains("1080p", true) || !element.text().contains("720p", true)
                }
        entries.amap { it ->
            var href =
                it.nextElementSibling()?.select("a:contains($aTag)")?.attr("href")
                    ?.substringAfter("=") ?: ""
            href = base64Decode(href)
            val selector =
                if (season == null) "p a.maxbutton" else "h3 a:matches(Episode $episode)"
            if (href.isNotEmpty())
            app.get(
                href,
            ).document.selectFirst(selector)?.let {
                val link = it.attr("href")
                val bypassedLink = bypassHrefli(link).toString()
                loadSourceNameExtractor("Moviesmod", bypassedLink, "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeAnizone(
        title: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$anizoneAPI/anime?search=$title"
        val link = app.get(url).document.select("div.truncate > a").firstOrNull {
            it.text().contains(title.toString(), ignoreCase = true)
        }?.attr("href") ?: return

        val document = app.get("$link/$episode").document

        val subtitles = document.select("track").map {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.attr("label"),
                    it.attr("src")
                )
            )
        }

        val source = document.select("media-player").attr("src")
        callback.invoke(
            newExtractorLink(
                "Anizone[Multi Lang]",
                "Anizone[Multi Lang]",
                source,
                type = ExtractorLinkType.M3U8,
            ) {
                this.quality = Qualities.P1080.value
            }
        )
    }

    // suspend fun invokeFlixhq(
    //     title: String? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    //     callback: (ExtractorLink) -> Unit
    // ) {
    //     val type = if (season == null) "Movie" else "TV Series"
    //     val searchJson = app.get("$CONSUMET_API/movies/flixhq/$title").text
    //     val searchData = tryParseJson<ConsumetSearch>(searchJson) ?: return
    //     val id = searchData.results.firstOrNull {
    //         it.title == "$title" && it.type == type
    //     }?.id ?: return
    //     val infoJson = app.get("$CONSUMET_API/movies/flixhq/info?id=$id").text
    //     val infoData = tryParseJson<ConsumetInfo>(infoJson) ?: return
    //     val epId = if(season == null) { infoData.episodes.firstOrNull()?.id ?: return }
    //     else {
    //         infoData.episodes.firstOrNull { it.number  == episode && it.season == season }?.id ?: return
    //     }

    //     val servers = listOf("upcloud", "vidcloud")
    //     servers.amap { server ->
    //         val epJson = app.get("$CONSUMET_API/movies/flixhq/watch?episodeId=$epId&mediaId=$id&server=$server").text
    //         val epData = tryParseJson<ConsumetWatch>(epJson) ?: return@amap
    //         val referer = epData.headers.Referer ?: ""

    //         epData.sources.map {
    //             callback.invoke(
    //                 newExtractorLink(
    //                     "Flixhq ${server.uppercase()}",
    //                     "Flixhq ${server.uppercase()}",
    //                     it.url,
    //                     type = if(it.isM3U8) ExtractorLinkType.M3U8 else INFER_TYPE,
    //                 ) {
    //                     this.referer = referer
    //                     this.quality = it.quality.toIntOrNull() ?: Qualities.Unknown.value
    //                 }
    //             )
    //         }

    //         epData.subtitles.map {
    //             subtitleCallback.invoke(
    //                 SubtitleFile(
    //                     it.lang.split(" - ").firstOrNull()?.trim() ?: it.lang,
    //                     it.url
    //                 )
    //             )
    //         }
    //     }
    // }

    private suspend fun invokeHianime(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val id = url?.substringAfterLast("/")?.substringAfterLast("-") ?: return

        val epId = app.get(
            "$hianimeAPI/ajax/v2/episode/list/$id",
            headers = headers
        ).parsedSafe<HianimeResponses>()?.html?.let {
            Jsoup.parse(it)
        }?.select("div.ss-list a")
            ?.find { it.attr("data-number") == "${episode ?: 1}" }
            ?.attr("data-id") ?: return

        val videoHeaders = mapOf(
            "Referer" to "https://megacloud.blog/",
            "Origin" to "https://megacloud.blog/"
        )

        val types = listOf("sub", "dub")

        types.forEach { t ->
            val epData = app.get("$miruroAPI/api/sources?episodeId=$epId&provider=zoro&fetchType=embed&category=$t").parsedSafe<HianimeStreamResponse>() ?: return@forEach
            val streamUrl = epData.streams.firstOrNull()?.url
            if(streamUrl != null) {
                M3u8Helper.generateM3u8(
                    "HiAnime [${t.uppercase()}]",
                    streamUrl,
                    "https://megacloud.blog/",
                    headers = videoHeaders
                ).forEach(callback)
            }

            epData.tracks.forEach { track ->
                if(track.kind == "captions") {
                    subtitleCallback.invoke(
                        SubtitleFile(
                            track.label ?: "und",
                            track.file
                        )
                    )

                }
            }
        }
    }

    suspend fun invokeAllanime(
        name: String? = null,
        year: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val privatereferer = "https://allmanga.to"
        val ephash = "5f1a64b73793cc2234a389cf3a8f93ad82de7043017dd551f38f65b89daa65e0"
        val queryhash = "06327bc10dd682e1ee7e07b6db9c16e9ad2fd56c1b769e47513128cd5c9fc77a"
        var type = ""
        if (episode == null) {
            type = "Movie"
        } else {
            type = "TV"
        }

        val query =
            """$AllanimeAPI?variables={"search":{"types":["$type"],"year":$year,"query":"$name"},"limit":26,"page":1,"translationType":"sub","countryOrigin":"ALL"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"$queryhash"}}"""
        val response =
            app.get(query, referer = privatereferer).parsedSafe<Anichi>()?.data?.shows?.edges
        if (response != null) {
            val id = response.firstOrNull()?.id ?: return
            val langType = listOf("sub", "dub")
            for (i in langType) {
                val epData =
                    """$AllanimeAPI?variables={"showId":"$id","translationType":"$i","episodeString":"${episode ?: 1}"}&extensions={"persistedQuery":{"version":1,"sha256Hash":"$ephash"}}"""
                val eplinks = app.get(epData, referer = privatereferer)
                    .parsedSafe<AnichiEP>()?.data?.episode?.sourceUrls
                eplinks?.amap { source ->
                    safeApiCall {
                        val headers =
                            mapOf(
                                "app-version" to "android_c-247",
                                "platformstr" to "android_c",
                                "Referer" to "https://allmanga.to"
                            )
                        val sourceUrl = source.sourceUrl
                        if (sourceUrl.startsWith("http")) {
                            val sourcename = sourceUrl.getHost()
                            loadCustomExtractor(
                                "Allanime [${i.uppercase()}] [$sourcename]",
                                sourceUrl ?: "",
                                "",
                                subtitleCallback,
                                callback,
                            )
                        }
                        else {
                            val decodedlink = if (sourceUrl.startsWith("--"))
                            {
                                decrypthex(sourceUrl)
                            }
                            else sourceUrl
                            val fixedLink = decodedlink.fixUrlPath()
                            val links = try {
                                app.get(fixedLink, headers = headers).parsedSafe<AnichiVideoApiResponse>()?.links ?: emptyList()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                return@safeApiCall
                            }
                            links.forEach { server ->
                                val host = server.link.getHost()
                                when {
                                    source.sourceName.contains("Default") && (server.resolutionStr == "SUB" || server.resolutionStr == "Alt vo_SUB") -> {
                                        getM3u8Qualities(
                                            server.link,
                                            "https://static.crunchyroll.com/",
                                            "Allanime [SUB] $host"
                                        ).forEach(callback)
                                    }

                                    server.hls == null -> {
                                        callback.invoke(
                                            newExtractorLink(
                                                "Allanime [${i.uppercase()}] ${host.replaceFirstChar { it.uppercase() }}",
                                                "Allanime [${i.uppercase()}] ${host.replaceFirstChar { it.uppercase() }}",
                                                server.link,
                                                INFER_TYPE
                                            )
                                            {
                                                this.quality = Qualities.P1080.value
                                            }
                                        )
                                    }

                                    server.hls == true -> {
                                        val endpoint = "https://allanime.day/player?uri=" +
                                                (if (URI(server.link).host.isNotEmpty())
                                                    server.link
                                                else "https://allanime.day" + URI(server.link).path)

                                        getM3u8Qualities(server.link, server.headers?.referer ?: endpoint, "Allanime [SUB] $host").forEach(callback)
                                    }

                                    else -> {
                                        server.subtitles?.forEach { sub ->
                                            val lang = SubtitleHelper.fromTwoLettersToLanguage(sub.lang ?: "") ?: sub.lang.orEmpty()
                                            val src = sub.src ?: return@forEach
                                            subtitleCallback(SubtitleFile(lang, httpsify(src)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun invokePlayer4U(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        if (title.isNullOrBlank()) return
        if(season == null && year == null) return

        val fixTitle = title.createPlayerSlug().orEmpty()
        val fixQuery = (season?.let { "$fixTitle S${"%02d".format(it)}E${"%02d".format(episode)}" } ?: "$fixTitle $year").replace(" ","+") // It is necessary for query with year otherwise it will give wrong movie
        val allLinks = HashSet<Player4uLinkData>()

        var page = 0
        var nextPageExists: Boolean = true

        do {
            val url = if(page == 0) {"$Player4uApi/embed?key=$fixQuery"} else {"$Player4uApi/embed?key=$fixQuery&page=$page"}
            try {
                var document = app.get(url, timeout = 20).document
                allLinks.addAll(
                    document.select(".playbtnx").map {
                        Player4uLinkData(name = it.text(), url = it.attr("onclick"))
                    }
                )

                // if(page == 0 && season == null && allLinks.size == 0)
                // {
                //     document = app.get("$Player4uApi/embed?key=${fixTitle.replace(" ","+")}", timeout = 20).document
                //     allLinks.addAll(
                //         document.select(".playbtnx").map {
                //             Player4uLinkData(name = it.text(), url = it.attr("onclick"))
                //         }
                //     )
                //     break
                // }

                nextPageExists = document.select("div a").any { it.text().contains("Next", true) }
            } catch (e: Exception) {}
            page++
        } while (nextPageExists && page <= 4)

        allLinks.distinctBy { it.name }.forEach { link ->
            try {

                val splitName = link.name.split("|").reversed()
                val firstPart = splitName.getOrNull(0)?.trim().orEmpty()
                val nameFormatted = "Player4U ${if(firstPart.isNullOrEmpty()) { "" } else { "{$firstPart}" }}"

                val qualityFromName = Regex("""(\d{3,4}p|4K|CAM|HQ|HD|SD|WEBRip|DVDRip|BluRay|HDRip|TVRip|HDTC|PREDVD)""", RegexOption.IGNORE_CASE)
                    .find(nameFormatted)?.value?.uppercase() ?: "UNKNOWN"


                val selectedQuality = getPlayer4UQuality(qualityFromName)

                val subLink = "go\\('(.*)'\\)".toRegex().find(link.url)?.groups?.get(1)?.value ?: return@forEach
                val iframeSource = app.get("$Player4uApi$subLink", timeout = 10, referer = Player4uApi)
                    .document.select("iframe").attr("src")
                getPlayer4uUrl(
                    nameFormatted,
                    selectedQuality,
                    "https://uqloads.xyz/e/$iframeSource",
                    Player4uApi,
                    callback
                )

            } catch (_: Exception) { }
        }
    }

    suspend fun invokePrimeWire(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if (season == null) {
            "$Primewire/embed/movie?imdb=$imdbId"
        } else {
            "$Primewire/embed/tv?imdb=$imdbId&season=$season&episode=$episode"
        }
        val doc = app.get(url, timeout = 10).document
        val userData = doc.select("#user-data")
        var decryptedLinks = decryptLinks(userData.attr("v"))
        for (link in decryptedLinks) {
            val url = "$Primewire/links/go/$link"
            val oUrl = app.get(url,timeout = 10)
            loadSourceNameExtractor(
                "Primewire",
                oUrl.url,
                "",
                subtitleCallback,
                callback
            )
        }
    }

    suspend fun invokeSoaper(
        imdbId: String? = null,
        tmdbId: Int? = null,
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Referer" to soaperAPI,
            "Origin" to soaperAPI
        )
        val document = app.get("$soaperAPI/search.html?keyword=$title", headers = headers).document
        val href = document.selectFirst("div.img-group a:has(img[src*='$tmdbId']), div.img-group a:has(img[src*='$imdbId'])")?.attr("href") ?: return

        if(season == null) {
            getSoaperLinks(soaperAPI, "$soaperAPI$href", "M", subtitleCallback, callback)
        } else {
            val doc = app.get("$soaperAPI$href", headers = headers).document
            val seasonDiv = doc.select("div.alert-info-ex").firstOrNull { div ->
                div.selectFirst("h4")?.text()?.contains("Season$season", ignoreCase = true) == true
            }

            val episodeLink = seasonDiv?.select("a")?.firstOrNull { a ->
                a.text().trim().startsWith("$episode.")
            }?.attr("href") ?: return

            getSoaperLinks(soaperAPI ,"$soaperAPI$episodeLink", "E", subtitleCallback, callback)
        }
    }

    suspend fun invokeThepiratebay(
        imdbId: String? =null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val url = if(season == null) {
                "$ThePirateBayApi/stream/movie/$imdbId.json"
            }
            else {
                "$ThePirateBayApi/stream/series/$imdbId:$season:$episode.json"
            }
            val res = app.get(url, timeout = 10).parsedSafe<TBPResponse>()

            for(stream in res?.streams!!)
            {
                val regex = Regex("""\uD83D\uDC64\s*(\d+)""")
                val match = regex.find(stream.title)
                val seeders = match?.groupValues?.get(1)?.toInt() ?: 0
                if (seeders < 10) continue
                val magnetLink = generateMagnetLink(TRACKER_LIST_URL,stream.infoHash)
                callback.invoke(
                    newExtractorLink(
                        "ThePirateBay",
                        "ThePirateBay [${stream.title}]",
                        magnetLink,
                        ExtractorLinkType.MAGNET
                    ) {
                        this.quality = getIndexQuality(stream.title)
                    }
                )
            }
        } catch (_: Exception) { }
    }


    // suspend fun invokeVidJoy(
    //     imdbId: Int? =null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     callback: (ExtractorLink) -> Unit
    // ) {
    //     try {
    //         val listSrc = listOf("Camelot","Atlantis","Babylon","NYC")
    //         val link = if(season != null)
    //         {
    //             "$VidJoyApi/embed/api/fetch2/$imdbId/$season/$episode"
    //         }
    //         else
    //         {
    //             "$VidJoyApi/embed/api/fetch2/$imdbId"
    //         }
    //         listSrc.forEach { src ->
    //             if(src == "Camelot")
    //             {
    //                 (0..4).forEach { i ->
    //                     try {
    //                         val finalLink = "$link/?srName=Camelot&sr=$i"
    //                         val encrptedText = app.get(finalLink).text;
    //                         val decryptedJson = decryptOpenSSLAES(encrptedText,"b91eeba6b7c848828ba8b84d44fa38a88affef23ec270ea4cd904810280b34fa")
    //                         if (decryptedJson != "> - <") {
    //                             val vidjoyResponse = tryParseJson<VidjoyResponse>(decryptedJson)
    //                             if (vidjoyResponse != null) {
    //                                 for(url in vidjoyResponse.url)
    //                                 {
    //                                     callback.invoke(
    //                                         newExtractorLink(
    //                                             "Vidjoy ${url.lang}",
    //                                             "Vidjoy ${url.lang}",
    //                                             url = url.link,
    //                                             type =  if(url.link.contains(".mp4")) ExtractorLinkType.VIDEO else INFER_TYPE
    //                                         ) {
    //                                             this.quality = getQualityFromName(url.resulation)
    //                                         }
    //                                     )
    //                                 }
    //                             }
    //                         }
    //                     } catch (e: Exception) { }
    //                 }
    //             }
    //             else
    //             {
    //                 try {
    //                     val finalLink = "$link/?srName=$src"
    //                     val encrptedText = app.get(finalLink).text;
    //                     val decryptedJson = decryptOpenSSLAES(encrptedText,"b91eeba6b7c848828ba8b84d44fa38a88affef23ec270ea4cd904810280b34fa")
    //                     if (decryptedJson != "> - <") {
    //                         val vidjoyResponse = tryParseJson<VidjoyResponse>(decryptedJson)
    //                         if (vidjoyResponse != null) {
    //                             for(url in vidjoyResponse.url)
    //                             {

    //                                 callback.invoke(
    //                                     newExtractorLink(
    //                                         "Vidjoy ${url.lang}",
    //                                         "Vidjoy ${url.lang}",
    //                                         url = url.link,
    //                                         type =  if(url.link.contains(".m3u8")) ExtractorLinkType.M3U8 else INFER_TYPE
    //                                     ) {
    //                                         this.quality = getQualityFromName(url.resulation)
    //                                     }
    //                                 )
    //                             }
    //                         }
    //                     }
    //                 } catch (e: Exception) { }
    //             }
    //         }

    //     } catch (_: Exception) { }
    // }
}
