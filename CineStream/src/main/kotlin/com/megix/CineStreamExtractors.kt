package com.megix

import android.os.Build
import androidx.annotation.RequiresApi
import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import org.jsoup.Jsoup
import com.lagradost.cloudstream3.runAllAsync
import com.lagradost.cloudstream3.mvvm.safeApiCall

import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Headers

import org.json.JSONObject
import org.json.JSONArray

import com.lagradost.cloudstream3.utils.AppUtils.toJson
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.lagradost.cloudstream3.utils.JsUnpacker
import com.lagradost.cloudstream3.USER_AGENT

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

import com.lagradost.cloudstream3.network.CloudflareKiller
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.toString
import java.security.SecureRandom
import java.io.IOException

object CineStreamExtractors : CineStreamProvider() {

    //Call all providers here
    suspend fun invokeAllSources(
        res: AllLoadLinksData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        runAllAsync(
            { if (!res.isBollywood) invokeVegamovies("VegaMovies", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isBollywood) invokeVegamovies("RogMovies", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeNetflix(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeVideo(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokeDisney(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokeBollywood(res.title, res.year ,res.season, res.episode, callback) },
            { if (res.isAsian) invokeDramafull(res.title, res.year ,res.season, res.episode, subtitleCallback, callback) },
            { invokeHexa(res.tmdbId, res.season, res.episode, callback) },
            { invokeCinemacity(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeYflix(res.tmdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeMoviebox(res.title, res.season, res.episode, subtitleCallback, callback) },
            { invokeProjectfreetv(res.title, res.airedYear, res.season, res.episode, subtitleCallback, callback) },
            { invokeAkwam(res.imdbId ,res.title, res.airedYear, res.season, res.episode, subtitleCallback, callback) },
            { invokeRtally(res.title, res.season, res.episode, subtitleCallback, callback) },
            { invokeVidlink(res.tmdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeMultimovies(res.title, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isBollywood) invokeTopMovies(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { if (!res.isBollywood) invokeMoviesmod(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isAsian) invokeKisskh(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokeMoviesdrive(res.title, res.imdbId ,res.season, res.episode, subtitleCallback, callback) },
            { if(res.isAnime || res.isCartoon) invokeToonstream(res.title, res.season, res.episode, subtitleCallback, callback) },
            { if(!res.isAnime) invokeAsiaflix(res.title, res.season, res.episode, res.airedYear, subtitleCallback, callback) },
            { invokeXDmovies(res.title ,res.tmdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeMapple(res.tmdbId, res.season, res.episode, callback) },
            { invokeMadplayCDN(res.tmdbId, res.season, res.episode, callback) },
            { invokeXpass(res.tmdbId, res.season, res.episode, callback) },
            { invokeProtonmovies(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeVidstack(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeDahmerMovies(res.title, res.year, res.season, res.episode, callback) },
            { invokeVadapav(res.title, res.year, res.season, res.episode, callback) },
            { if (!res.isAnime) invokeSkymovies(res.title, res.airedYear, res.episode, subtitleCallback, callback) },
            { if (!res.isAnime) invokeHdmovie2(res.title, res.airedYear, res.episode, subtitleCallback, callback) },
            { invokeBollyflix(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeVideasy(res.title ,res.tmdbId, res.imdbId, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokeMovies4u(res.imdbId, res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokeTorrentio(res.imdbId, res.season, res.episode, callback) },
            { invokeTorrentsDB(res.imdbId, res.season, res.episode, callback) },
            { if (!res.isBollywood) invokeHindmoviez(res.imdbId, res.season, res.episode, callback) },
            { if (!res.isBollywood && !res.isAnime) invokeKatMovieHd("KatMovieHd", res.imdbId, res.season, res.episode, subtitleCallback ,callback) },
            { if (res.isBollywood) invokeKatMovieHd("Moviesbaba", res.imdbId, res.season, res.episode, subtitleCallback ,callback) },
            { invokeWYZIESubs(res.imdbId, res.season, res.episode, subtitleCallback) },
            { invokeStremioSubtitles(res.imdbId, res.season, res.episode, subtitleCallback) },
            // { invokePrimebox(res.title, res.year, res.season, res.episode, subtitleCallback, callback) },
            { invokePrimeSrc(res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (!res.isAnime) invoke2embed(res.imdbId, res.season, res.episode, callback) },
            // { invokePrimenet(res.tmdbId, res.season, res.episode, callback) },
            // { invokeMp4Moviez(res.title, res.season, res.episode, res.year, callback, subtitleCallback) },
            { invokeFilm1k(res.title, res.season, res.year, subtitleCallback, callback) },
            { invokeCinemaOS(res.imdbId, res.tmdbId, res.title, res.season, res.episode, res.year, callback, subtitleCallback) },
            // { invokeTripleOneMovies(res.tmdbId, res.season, res.episode, callback, subtitleCallback) },
            // { invokeVidFastPro(res.tmdbId, res.season, res.episode, callback, subtitleCallback) },
            // { invokeVidPlus(res.tmdbId,res.imdbId,res.title,res.season,res.episode, res.year,callback,subtitleCallback) },
            { invokeMultiEmbeded(res.tmdbId, res.season,res.episode, callback,subtitleCallback) },
            { invokeVicSrcWtf(res.tmdbId, res.season,res.episode, callback,subtitleCallback) },
            { invokeVidzee(res.tmdbId, res.season,res.episode, callback,subtitleCallback) },
            // { invokeStremioStreams("Nuvio", nuvioStreamsAPI, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeStremioStreams("WebStreamr", webStreamrAPI, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if(res.isAsian) invokeStremioStreams("Dramayo", daramayoAPI, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeStremioStreams("Nodebrid", nodebridAPI, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeStremioStreams("NoTorrent", notorrentAPI, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeStremioStreams("Leviathan", leviathanAPI, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            // { invokeStremioStreams("Sooti", sootiAPI, res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeStremioStreams("Castle", base64Decode(castleAPI), res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeStremioStreams("Cine", base64Decode(cineAPI), res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { invokeAllmovieland(res.imdbId, res.season, res.episode, callback) },
            { if(res.season == null) invokeMostraguarda(res.imdbId, subtitleCallback, callback) },
            { if (!res.isBollywood && !res.isAnime) invokeMoviesflix("Moviesflix", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (res.isBollywood) invokeMoviesflix("Hdmoviesflix", res.imdbId, res.season, res.episode, subtitleCallback, callback) },
            { if (!res.isBollywood) invokeUhdmovies(res.title, res.year, res.season, res.episode, callback, subtitleCallback) },
            { if (!res.isBollywood) invoke4khdhub(res.title, res.year, res.season, res.episode, subtitleCallback, callback) }
        )
    }

    suspend fun invokeAllAnimeSources(
        res: AllLoadLinksData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        runAllAsync(
            { invokeSudatchi(res.anilistId, res.episode, subtitleCallback, callback) },
            { invokeGojo(res.anilistId, res.episode, subtitleCallback ,callback) },
            { invokeTokyoInsider(res.title, res.episode, subtitleCallback, callback) },
            { invokeAllanime(res.title, res.year, res.episode, subtitleCallback, callback) },
            { invokeAnizone(res.title, res.episode, subtitleCallback, callback) },
            { invokeTorrentio("kitsu:${res.kitsuId}", res.season, res.episode, callback) },
            { invokeAnimetosho(res.kitsuId, res.malId, res.episode, callback) },
            { invokeTorrentsDB(res.imdbId, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeWYZIESubs(res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback) },
            { invokeStremioSubtitles(res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback) },
            { invokeAnimes(res.malId, res.anilistId, res.episode, res.year, "kitsu", subtitleCallback, callback) },
            { invokeBollywood(res.imdbTitle, res.year, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeNetflix(res.imdbTitle, res.year, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokePrimeVideo(res.imdbTitle, res.year, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMoviebox(res.imdbTitle, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeProtonmovies(res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            // { invokeStremioStreams("Sooti", sootiAPI, res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeStremioStreams("Castle", base64Decode(castleAPI), res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeCinemacity(res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMoviesmod(res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeHindmoviez(res.imdbId, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeXDmovies(res.imdbTitle ,res.tmdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMovies4u(res.imdbId, res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeBollyflix(res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeAllmovieland(res.imdbId, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeHexa(res.tmdbId, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeMapple(res.tmdbId, res.imdbSeason, res.imdbSeason, callback) },
            { invokeVidlink(res.tmdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            // { invokeStremioStreams("Nuvio", nuvioStreamsAPI, res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeStremioStreams("Nodebrid", nodebridAPI, res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeStremioStreams("Anime World[Multi]", animeWorldAPI, res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeVegamovies("VegaMovies", res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invoke4khdhub(res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMoviesdrive(res.imdbTitle, res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeToonstream(res.imdbTitle, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeMultimovies(res.imdbTitle, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokePrimeSrc(res.imdbId, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback) },
            { invokeDahmerMovies(res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, callback) },
            // { invokePrimebox(res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, subtitleCallback, callback)},
            // { invokePrimenet(res.tmdbId, res.imdbSeason, res.imdbEpisode, callback) },
            { invokeUhdmovies(res.imdbTitle, res.imdbYear, res.imdbSeason, res.imdbEpisode, callback, subtitleCallback) },
        )
    }

    suspend fun invokeAkwam(
        imdbId: String? = null,
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        suspend fun getLink(url: String) : String? {
            val link = app.get(url, referer = "$akwamAPI/")
            .document
            .selectFirst("a.link-download")
            ?.attr("href")
            ?: return null

            val link2 = app.get(link, referer = "$akwamAPI/")
                .document
                .selectFirst("a.download-link")
                ?.attr("href")
                ?: return null

            val source = app.get(link2, referer = "$akwamAPI/")
                .document
                .selectFirst("a.link")
                ?.attr("href")
                ?: return null

            return source
        }

        if(imdbId == null || title == null || year == null) return

        val type = if(season == null) "movie" else "series"
        val searchUrl = "$akwamAPI/search?q=${URLEncoder.encode(title, "UTF-8")}&section=$type&year=$year&rating=0&formats=0&quality=0"
        val url = app.get(searchUrl, referer = "$akwamAPI/")
            .document
            .selectFirst("a.box")
            ?.attr("href")
            ?: return
        val document = app.get(url, referer = "$akwamAPI/").document
        val imdb = document.selectFirst("a[href*='imdb.com']")
            ?.attr("href")
            ?.substringAfter("title/")
            ?.substringBefore("/")
            ?: return

        if(imdbId != imdb) return

        val source = if(season == null) {
            getLink(url)
        } else {
            val episodeLinks = document.select("h2 > a.text-white")

            val match = episodeLinks.find { element ->
                val text = element.text()
                val regex = "(?:حلقة|Episode)\\s+$episode(?!\\d)".toRegex(RegexOption.IGNORE_CASE)
                regex.containsMatchIn(text)
            }

            if(match == null) return
            getLink(match.attr("href"))
        }

        if(source == null) return

        callback.invoke(
            newExtractorLink(
                "Akwam 🇸🇦",
                "Akwam 🇸🇦",
                source,
                ExtractorLinkType.VIDEO
            ) {
                this.quality = Qualities.P720.value
                this.referer = "$akwamAPI/"
                this.headers = mapOf(
                    "Connection" to "keep-alive",
                    "Referer" to "$akwamAPI/",
                    "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36",
                )
            }
        )
    }

    suspend fun invokeProjectfreetv(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val query = if(season == null) {
            "$title".replace(" ", "+")
        } else {
            "${title?.replace(" ", "+")}+-+season+$season"
        }

        val seacrhUrl = "$projectfreetvAPI/data/browse/?lang=3&keyword=$query&year=$year&networks=&rating=&votes=&genre=&country=&cast=&directors=&type=&order_by=&page=1&limit=1"
        val searchJson = app.get(seacrhUrl, referer = projectfreetvAPI, timeout = 600L).text
        val searchObject = JSONObject(searchJson)
        val moviesArray = searchObject.getJSONArray("movies")
        if (moviesArray.length() == 0) return
        val id = moviesArray.getJSONObject(0).getString("_id")
        if(id.isEmpty()) return
        val jsonString = app.get("$projectfreetvAPI/data/watch/?_id=$id", referer = projectfreetvAPI, timeout = 600L).text
        val rootObject = JSONObject(jsonString)

        if (rootObject.has("streams")) {
            val streamsArray = rootObject.getJSONArray("streams")

            for (i in 0 until streamsArray.length()) {
                val item = streamsArray.getJSONObject(i)
                val currentEpisode = item.optString("e").toIntOrNull() ?: -1
                if (episode == null || currentEpisode == episode) {
                    val source = item.optString("stream")
                    loadSourceNameExtractor("ProjectFreeTV", source, "", subtitleCallback, callback)
                }
            }
        }
    }

    suspend fun invokeDramafull(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val jsonString = app.get("$dramafullAPI/api/live-search/$title").text
        val searchTypeId = if(season != null) 1 else 2
        val searchTitle = if(season == null) {
            "$title ($year)"
        } else if(season == 1) {
            "$title ($year)"
        } else {
            "$title Season $season"
        }

        val rootObject = JSONObject(jsonString)
        val dataArray = rootObject.getJSONArray("data")
        var matchedObject: JSONObject? = null

        for (i in 0 until dataArray.length()) {
            val item = dataArray.getJSONObject(i)
            val name = item.getString("name")
            val typeId = item.getInt("type_id")

            if (name.equals(searchTitle, ignoreCase = true) && typeId == searchTypeId) {
                matchedObject = item
                break
            }
        }

        if(matchedObject == null) return

        val document = app.get("$dramafullAPI/film/${matchedObject.getString("slug")}").document

        val href = if(season != null) {
            document
            .selectFirst("div.episode-item a[title*='Episode $episode']")
            ?.attr("href")
        } else {
            document
            .selectFirst("div.last-episode a")
            ?.attr("href")
        }

        val doc = app.get(href ?: return).document
        val script = doc.select("script:containsData(signedUrl)").firstOrNull()?.toString() ?: return
        val signedUrl = Regex("""window\.signedUrl\s*=\s*"(.+?)\"""").find(script)?.groupValues?.get(1)?.replace("\\/","/") ?: return
        val res = app.get(signedUrl).text
        val resJson = JSONObject(res)
        val videoSource = resJson.optJSONObject("video_source") ?: return
        val qualities = videoSource.keys().asSequence().toList()
            .sortedByDescending { it.toIntOrNull() ?: 0 }
        val bestQualityKey = qualities.firstOrNull() ?: return
        val bestQualityUrl = videoSource.optString(bestQualityKey)

        callback(
            newExtractorLink(
                "Dramafull",
                "Dramafull",
                bestQualityUrl
            )
        )

        val subJson = resJson.optJSONObject("sub")
        subJson?.optJSONArray(bestQualityKey)?.let { array ->
            for (i in 0 until array.length()) {
                subtitleCallback(newSubtitleFile("English", dramafullAPI + array.getString(i)))
            }
        }
    }

    suspend fun invokeCinemacity(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$cinemacityAPI/index.php?do=search&subaction=search&search_start=1&full_search=0&story=$imdbId"

        val movieUrl = app.get(url).document
            .selectFirst("div.dar-short_item > a")
            ?.attr("href")
            ?: return

        val headers = mapOf(
            "Cookie" to CC_COOKIE
        )

        val scriptData = app.get(movieUrl, headers).document
            .select("script:containsData(atob)")
            .getOrNull(1)
            ?.data()
            ?: return

        val playerJson = JSONObject(
            base64Decode(
                scriptData.substringAfter("atob(\"").substringBefore("\")")
            ).substringAfter("new Playerjs(").substringBeforeLast(");")
        )

        val fileArray = JSONArray(playerJson.getString("file"))

        fun extractQuality(url: String): Int {
            return when {
                url.contains("2160p") -> Qualities.P2160.value
                url.contains("1440p") -> Qualities.P1440.value
                url.contains("1080p") -> Qualities.P1080.value
                url.contains("720p") -> Qualities.P720.value
                url.contains("480p") -> Qualities.P480.value
                url.contains("360p") -> Qualities.P360.value
                else -> Qualities.Unknown.value
            }
        }

        suspend fun emitExtractorLinks(files: String) {
            callback.invoke(
                newExtractorLink(
                    "CineCity",
                    "CineCity Multi Audio 🌐",
                    files,
                    INFER_TYPE
                ) {
                    referer = movieUrl
                    quality = extractQuality(files)
                }
            )
        }

        val first = fileArray.getJSONObject(0)

        // MOVIE
        if (!first.has("folder")) {
            emitExtractorLinks(
                files = first.getString("file")
            )
            return
        }

        // SERIES
        for (i in 0 until fileArray.length()) {
            val seasonJson = fileArray.getJSONObject(i)

            val seasonNumber = Regex("Season\\s*(\\d+)", RegexOption.IGNORE_CASE)
                .find(seasonJson.optString("title"))
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
                ?: continue

            if (season != null && seasonNumber != season) continue

            val episodes = seasonJson.getJSONArray("folder")
            for (j in 0 until episodes.length()) {
                val epJson = episodes.getJSONObject(j)

                val episodeNumber = Regex("Episode\\s*(\\d+)", RegexOption.IGNORE_CASE)
                    .find(epJson.optString("title"))
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                    ?: continue

                if (episode != null && episodeNumber != episode) continue

                emitExtractorLinks(
                    files = epJson.getString("file")
                )
            }
        }
    }

    suspend fun invokeVidstack(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val json = app.get("$multiDecryptAPI/enc-vidstack").text
        val result = JSONObject(json).getJSONObject("result")
        val token = result.getString("token")
        val user_id = result.getString("user_id")

        if(token.isEmpty()) return

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Referer" to vidstackBaseAPI
        )

        // val servers = listOf(
        //     "videosmashyi", "smashystream",
        //     "short2embed", "videoophim", "videofsh"
        // )

        val servers = listOf(
            "videosmashyi"
        )

        servers.forEach { server ->

            val type = if(server == "videosmashyi" || server == "smashystream") {
                "1"
            } else {
                "2"
            }

            val url = if(season == null) {
                "$vidstackAPI/$server/$imdbId?token=$token&user_id=$user_id"
            } else {
                "$vidstackAPI/$server/$imdbId/$season/$episode?token=$token&user_id=$user_id"
            }

            val data_json = app.get(url, headers = headers).text
            val dataString = JSONObject(data_json).getString("data")

            if(dataString.isEmpty()) return@forEach

            val parts = dataString.split("/#")
            if (parts.size < 2) return@forEach
            val host = parts[0]
            val id = parts[1]
            val encrypted = app.get("$host/api/v1/video?id=$id", headers = headers).text

            val jsonBody = JsonObject().apply {
                addProperty("text", encrypted)
                addProperty("type", type)
            }

            val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaTypeJson)
            val decrypted = app.post("$multiDecryptAPI/dec-vidstack", requestBody = requestBody).text
            val resultObject = JSONObject(decrypted).getJSONObject("result")
            val m3u8 = resultObject.getString("source")

            callback.invoke(
                newExtractorLink(
                    "Vidstack",
                    "Vidstack",
                    m3u8,
                    ExtractorLinkType.M3U8
                ) {
                    this.headers = headers
                    this.quality = Qualities.P1080.value
                }
            )

            if (resultObject.has("subtitle")) {
                val subtitleObject = resultObject.getJSONObject("subtitle")
                val keysIterator = subtitleObject.keys()

                while (keysIterator.hasNext()) {
                    val languageName = keysIterator.next()
                    val relativePath = subtitleObject.getString(languageName)
                    val fullUrl = "$vidstackBaseAPI$relativePath"

                    subtitleCallback.invoke(
                        newSubtitleFile(
                            getLanguage(languageName) ?: languageName,
                            fullUrl
                        )
                    )
                }
            }
        }
    }

    suspend fun invokeMadplayCDN(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val m3u8 = if(season == null) {
            "https://cdn.madplay.site/api/hls/unknown/${tmdbId}/master.m3u8"
        } else {
            "https://cdn.madplay.site/api/hls/unknown/${tmdbId}/season_${season}/episode_${episode}/master.m3u8"
        }

        M3u8Helper.generateM3u8(
            "Madplay[CDN]",
            m3u8,
            "",
        ).forEach(callback)
    }

    suspend fun invokeXpass(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$xpassAPI/feb/${tmdbId}/0/0/0/playlist.json"
        } else {
            "$xpassAPI/meg/tv/${tmdbId}/${season}/${episode}/playlist.json"
        }

        val json = app.get(url).text
        val regex = """\"file":"(.*?)\"""".toRegex()
        val match = regex.find(json)
        val rawUrl = match?.groupValues?.get(1)
        val m3u8 = rawUrl?.replace("\\u0026", "&")

        if (m3u8 != null) {
            M3u8Helper.generateM3u8(
                "Xpass",
                m3u8,
                "$xpassAPI/",
            ).forEach(callback)
        }
    }

    suspend fun invokeRtally(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        fun getStreamUrl(
            id: String,
            service: String
        ): String? {
            if(service == "vidhide") return "https://vidhideplus.com/v/$id"
            else if(service == "lulustream") return "https://lulustream.com/e/$id"
            else if(service == "filemoon") return "https://filemoon.sx/e/$id"
            else if(service == "streamwish") return "https://playerwish.com/e/$id"
            else if(service == "strmup") return "https://strmup.cc/$id"
            else return null
        }

        if(season != null) return

        val slugTitle = title.createSlug()
        val url = "$rtallyAPI/post/$slugTitle"
        val doc = app.get(url).document

        val linkPattern = Regex("""\\"(small|medium|large|extraLarge)\\":\\"(https?://[^\\"]+)""")

        linkPattern.findAll(doc.toString()).forEach { match ->
            val quality = match.groupValues[1]
            val durl = match.groupValues[2]

            loadSourceNameExtractor("Rtally", durl, "", subtitleCallback, callback)
        }

        val streamPattern = Regex("""\\"(lulustream|strmup|filemoon|turbo|vidhide|doodStream|streamwish)Url\\":\\"?([^\\"]+)""")

        streamPattern.findAll(doc.toString()).forEach { match ->
            val service = match.groupValues[1]
            val id = match.groupValues[2]

            if (id != "null") {
                val eurl = getStreamUrl(id, service) ?: return@forEach
                loadSourceNameExtractor("Rtally", eurl, "", subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeYflix(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val client = OkHttpClient()
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        data class ServerItem(val name: String, val lid: String)

        suspend fun encrypt(id: String): String {
            val body = app.get("$multiDecryptAPI/enc-movies-flix?text=$id").text
            val json = JSONObject(body)
            return json.getString("result")
        }

        suspend fun decrypt(text: String): String {
            val jsonBody = """{"text":"$text"}"""
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val text = app.post(
                "$multiDecryptAPI/dec-movies-flix",
                requestBody = requestBody
            ).text

            val json = JSONObject(text)
            return json.getJSONObject("result").getString("url")
        }

        fun extractAllServers(root: JSONObject): List<ServerItem> {
            val list = mutableListOf<ServerItem>()

            try {
                val result = root.optJSONObject("result")
                val defaultObj = result?.optJSONObject("default")

                if (defaultObj != null) {
                    val keys = defaultObj.keys()

                    while (keys.hasNext()) {
                        val key = keys.next()
                        val serverNode = defaultObj.optJSONObject(key)

                        val lid = serverNode?.optString("lid")
                        val name = serverNode?.optString("name")

                        if (!lid.isNullOrEmpty() && !name.isNullOrEmpty()) {
                            list.add(ServerItem(name, lid))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }

        suspend fun parseHtml(html: String): JSONObject {
            val jsonBody = JSONObject().put("text", html)
            val request = Request.Builder()
                .url("https://enc-dec.app/api/parse-html")
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val resp = client.newCall(request).execute().body.string()

            return try {
                JSONObject(resp)
            } catch (e: Exception) {
                val clean = if (resp.startsWith("\"")) resp.substring(1, resp.length - 1).replace("\\\"", "\"") else resp
                JSONObject(clean)
            }
        }

        val findUrl = "https://enc-dec.app/db/flix/find?tmdb_id=$tmdbId"
        val findResp = app.get(findUrl).text
        val findJson = JSONArray(findResp)
        if (findJson.length() == 0) return

        val contentId = findJson.getJSONObject(0).optJSONObject("info")?.optString("flix_id")
            ?: return
        val encId = encrypt(contentId)
        val seasonText = if(season == null) "1" else "$season"
        val episodeText = if(episode == null) "1" else "$episode"
        val episodesUrl = "$YflixAPI/ajax/episodes/list?id=$contentId&_=$encId"
        val episodesResp = app.get(episodesUrl).text
        val episodesHtml = JSONObject(episodesResp).getString("result")
        val episodesObj = parseHtml(episodesHtml)
        val result = episodesObj.optJSONObject("result")
        val seasonObj = result?.optJSONObject(seasonText)
        val episodeObj = seasonObj?.optJSONObject(episodeText)
        val eid = episodeObj?.optString("eid")

        if (eid.isNullOrEmpty()) return

        val encTargetId = encrypt(eid)
        val serversUrl = "$YflixAPI/ajax/links/list?eid=$eid&_=$encTargetId"
        val serversResp = app.get(serversUrl).text
        val serversHtml = JSONObject(serversResp).getString("result")
        val serversObj = parseHtml(serversHtml)
        val servers = extractAllServers(serversObj)

        servers.forEach { server ->
            val lid = server.lid
            val encLid = encrypt(lid)
            val serverName = server.name
            val embedUrlReq = "$YflixAPI/ajax/links/view?id=$lid&_=$encLid"
            val embedRespStr = app.get(embedUrlReq).text
            val encryptedEmbed = JSONObject(embedRespStr).getString("result")
            if (encryptedEmbed.isEmpty()) return@forEach
            val embed_url = decrypt(encryptedEmbed)
            loadExtractor(embed_url, "Yflix", subtitleCallback, callback)
        }
    }

    suspend fun invokeStremioStreams(
        sourceName: String,
        api: String,
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$api/stream/movie/$imdbId.json"
        } else {
            "$api/stream/series/$imdbId:$season:$episode.json"
        }

        val json = app.get(url).text
        val gson = Gson()
        val data = gson.fromJson(json, StreamifyResponse::class.java)

        data.streams.forEach { stream ->
            val title = stream.title ?: ""
            val name = stream.name?.replace(" (SLOW) -", "") ?: stream.title ?: ""
            val type = if(
                title.contains("hls") ||
                title.contains("m3u8") ||
                name.contains("Vixsrc") ||
                sourceName.contains("Castle")
            ) {
                ExtractorLinkType.M3U8
            } else {
                INFER_TYPE
            }

            val headers = mapOf(
                "User-Agent" to (stream.behaviorHints?.proxyHeaders?.request?.userAgent ?: USER_AGENT),
                "Referer"    to (stream.behaviorHints?.proxyHeaders?.request?.Referer ?: ""),
                "Origin"     to (stream.behaviorHints?.proxyHeaders?.request?.Origin ?: "")
            )

            val blockedUrls = listOf("https://github.com", "video-downloads.googleusercontent")
            val blockedNames = listOf("4KHDHub", "Instant Download", "IOSMIRROR", "XDM")
            val blockedTitles = listOf("redirecting")

            if (blockedUrls.any { stream.url.contains(it) } ||
                blockedNames.any { key -> stream.name?.contains(key) == true } ||
                blockedTitles.any { key -> stream.title?.contains(key) == true }
            ) {
                return@forEach
            }

            val streamUrl = if(sourceName == "Nodebrid") {
                stream.url.substringAfter("url=").substringBefore("&")
                .let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
            } else {
                stream.url
            }

            callback.invoke(
                newExtractorLink(
                    sourceName,
                    "[$sourceName]" + " $title",
                    streamUrl,
                    type,
                ) {
                    this.referer = stream.behaviorHints?.proxyHeaders?.request?.Referer ?: ""
                    this.quality = getIndexQuality(title + name)
                    this.headers = headers
                }
            )

            stream.subtitles?.forEach { subtitle ->
                subtitleCallback.invoke(
                    newSubtitleFile(
                        getLanguage(subtitle.lang) ?: subtitle.lang,
                        subtitle.url
                    )
                )
            }
        }
    }

    suspend fun invokeVadapav(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val url = if(season == null) {
            "$vadapavAPI/movies/$title ($year)/"
        } else {
            "$vadapavAPI/shows/$title ($year)/Season $seasonSlug/"
        }

        val selector = if(episode != null) "a.wrap.directory-entry:contains(E$episodeSlug)" else "a.wrap.directory-entry"

        val aTag = app.get(url).document.selectFirst(selector) ?: return
        val dlink = aTag.attr("href")
        val text = aTag.text()

        if(dlink.isNullOrEmpty()) return

        callback.invoke(
            newExtractorLink(
                "Vadapav",
                "Vadapav | $text",
                vadapavAPI + dlink,
                ExtractorLinkType.VIDEO
            ) {
                this.quality = getIndexQuality(text)
            }
        )
    }

    suspend fun invokeDahmerMovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if (season == null) {
            "$dahmerMoviesAPI/movies/${title?.replace(":", "")} ($year)/"
        } else {
            "$dahmerMoviesAPI/tvs/${title?.replace(":", " -")}/Season $season/"
        }
        val request = app.get(url, timeout = 60L)
        if (!request.isSuccessful) return
        val paths = request.document.select("a").map {
            it.text() to it.attr("href")
        }.filter {
            if (season == null) {
                it.first.contains(Regex("(?i)(720p|1080p|2160p)"))
            } else {
                val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
                it.first.contains(Regex("(?i)S${seasonSlug}E${episodeSlug}"))
            }
        }.ifEmpty { return }

        paths.map {
            val quality = getIndexQuality(it.first)
            val tags = getIndexQualityTags(it.first)
            val href = if (it.second.contains(dahmerMoviesAPI)) it.second else (dahmerMoviesAPI + it.second)
            val videoLink = resolveFinalUrl(href)
            callback.invoke(
                newExtractorLink(
                    "DahmerMovies",
                    "DahmerMovies $tags",
                    videoLink,
                    ExtractorLinkType.VIDEO
                ) {
                    this.quality = quality
                    this.referer = dahmerMoviesAPI
                }
            )
        }
    }

    suspend fun invokeVideasy(
        title: String? = null,
        tmdbId: Int? = null,
        imdbId: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        fun quote(text: String): String {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
                .replace("+", "%20")
        }

        var headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
            "Connection" to "keep-alive",
            "Origin" to "https://player.videasy.net",
        )

        val servers = listOf(
            "myflixerzupcloud",
            "1movies",
            "moviebox",
            "primewire",
            "m4uhd",
            "hdmovie",
            "cdn",
            "primesrcme"
        )

        if(title == null) return

        val firstPass = quote(title)
        val encTitle = quote(firstPass)

        servers.amap { server ->
            val url = if (season == null) {
                "$videasyAPI/$server/sources-with-title?title=$encTitle&mediaType=movie&year=$year&tmdbId=$tmdbId&imdbId=$imdbId"
            } else {
                "$videasyAPI/$server/sources-with-title?title=$encTitle&mediaType=tv&year=$year&tmdbId=$tmdbId&episodeId=$episode&seasonId=$season&imdbId=$imdbId"
            }

            val enc_data = app.get(url, headers = headers).text

            val jsonBody = """{"text":"$enc_data","id":"$tmdbId"}"""
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
            val response = app.post(
                "$multiDecryptAPI/dec-videasy",
                requestBody = requestBody
            )

            if(response.isSuccessful) {
                val json = response.text
                val result = JSONObject(json).getJSONObject("result")

                val sourcesArray = result.getJSONArray("sources")
                for (i in 0 until sourcesArray.length()) {
                    val obj = sourcesArray.getJSONObject(i)
                    val quality = obj.getString("quality")
                    val source = obj.getString("url")
                    var type = INFER_TYPE

                    if(source.contains(".m3u8")) {
                        headers = headers + mapOf(
                            "Accept" to "application/vnd.apple.mpegurl,application/x-mpegURL,*/*",
                            "Referer" to "$videasyAPI/"
                        )
                        type = ExtractorLinkType.M3U8
                    } else if(source.contains(".mp4")) {
                        headers = headers + mapOf(
                            "Accept" to "video/mp4,*/*",
                            "Range" to "bytes=0-",
                        )
                        type = ExtractorLinkType.VIDEO
                    } else if(source.contains(".mkv")) {
                        headers = headers + mapOf(
                            "Accept" to "video/x-matroska,*/*",
                            "Range" to "bytes=0-",
                        )
                        type = ExtractorLinkType.VIDEO
                    }

                    callback.invoke(
                        newExtractorLink(
                            "Videasy[${server.uppercase()}]",
                            "Videasy[${server.uppercase()}]",
                            source,
                            type
                        ) {
                            this.quality = getIndexQuality(quality)
                            this.headers = headers
                        }
                    )
                }

                val subtitlesArray = result.getJSONArray("subtitles")
                for (i in 0 until subtitlesArray.length()) {
                    val obj = subtitlesArray.getJSONObject(i)
                    val source = obj.getString("url")
                    val language = obj.getString("language")

                    subtitleCallback.invoke(
                        newSubtitleFile(
                            getLanguage(language) ?: language,
                            source
                        )
                    )
                }
            }
        }
    }

    suspend fun invokeMapple(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        if(tmdbId == null) return
        var mediaType = ""
        var tv_slug = ""
        var url = ""

        if(season == null) {
          mediaType =  "movie"
          url = "$mappleAPI/watch/movie/$tmdbId"
        } else {
            mediaType = "tv"
            tv_slug = "$season-$episode"
            url = "$mappleAPI/watch/tv/$tmdbId/$season-$episode"
        }

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Referer" to "$mappleAPI/",
        )

        val text = app.get(url, headers = headers).text
        val regex = Regex("""window\.__REQUEST_TOKEN__\s*=\s*"([^"]+)\"""")
        val match = regex.find(text)
        val token = match?.groupValues?.get(1) ?: return

        val sources = listOf(
            "mapple", "sakura", "oak", "willow",
            "cherry", "pines", "magnolia", "sequoia"
        )

        sources.map { source ->
            try {
                val jsonBody = """
                    {
                        "data": {
                            "mediaId": $tmdbId,
                            "mediaType": "$mediaType",
                            "tv_slug": "$tv_slug",
                            "source": "$source"
                        },
                        "endpoint": "stream-encrypted"
                    }
                """.trimIndent()

                val encryptResText = app.post(
                    "$mappleAPI/api/encrypt",
                    json = jsonBody,
                    headers = headers
                ).text

                val encryptRes = JSONObject(encryptResText)
                val streamPath = encryptRes.getString("url")
                val finalUrl = "$mappleAPI$streamPath&requestToken=$token"

                val streamsDataText = app.get(
                    finalUrl,
                    headers = headers
                ).text

                val streamsData = JSONObject(streamsDataText)

                if (streamsData.optBoolean("success")) {
                    val data = streamsData.getJSONObject("data")
                    val streamUrl = data.optString("stream_url")

                    if (streamUrl.isNotEmpty()) {
                        M3u8Helper.generateM3u8(
                            "Mapple [${source.uppercase()}]",
                            streamUrl,
                            "$mappleAPI/",
                            headers = headers
                        ).forEach(callback)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun invokeHexa(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) {
            "$hexaAPI/api/tmdb/movie/$tmdbId/images"
        } else {
            "$hexaAPI/api/tmdb/tv/$tmdbId/season/$season/episode/$episode/images"
        }

        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        val key = keyBytes.joinToString("") { "%02x".format(it) }

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Accept" to "plain/text",
            "X-Api-Key" to key
        )

        val enc_data = app.get(url, headers = headers).text

        val jsonBody = """{"text":"$enc_data","key":"$key"}"""
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
        val response = app.post(
            "$multiDecryptAPI/dec-hexa",
            requestBody = requestBody,
            headers = mapOf("Content-Type" to "application/json")
        )

        if(response.isSuccessful) {
            val json = response.text
            val result = JSONObject(json).getJSONObject("result")
            val sourcesArray = result.getJSONArray("sources")

            for (i in 0 until sourcesArray.length()) {
                val src = sourcesArray.getJSONObject(i)
                val server = src.getString("server")
                val m3u8 = src.getString("url")

                M3u8Helper.generateM3u8(
                    "Hexa ${server.uppercase()}",
                    m3u8,
                    "https://hexa.su/",
                ).forEach(callback)
            }
        }
    }

    suspend fun invokeVidlink(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$multiDecryptAPI/enc-vidlink?text=$tmdbId"
        val json = app.get(url).text
        val enc_data = JSONObject(json).getString("result")

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Connection" to "keep-alive",
            "Referer" to "$vidlinkAPI/",
            "Origin" to "$vidlinkAPI/",
        )

        val epUrl = if(season == null) {
            "$vidlinkAPI/api/b/movie/$enc_data"
        } else {
            "$vidlinkAPI/api/b/tv/$enc_data/$season/$episode"
        }

        val epJson = app.get(epUrl, headers = headers).text
        val gson = Gson()
        val data = gson.fromJson(epJson, VidlinkResponse::class.java)
        val m3u8 = data.stream.playlist

        M3u8Helper.generateM3u8(
            "Vidlink",
            m3u8,
            "$vidlinkAPI/",
            headers = headers
        ).forEach(callback)
    }

    suspend fun invokeXDmovies(
        title: String? = null,
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Referer" to "$XDmoviesAPI/",
            "x-requested-with" to "XMLHttpRequest",
            "x-auth-token" to "7297skkihkajwnsgaklakshuwd"
        )

        val searchData = app.get(
            "$XDmoviesAPI/php/search_api.php?query=$title&fuzzy=true",
            headers = headers
        ).parsedSafe<XDMoviesSearchResponse>() ?: return

        val matched = searchData.firstOrNull { it.tmdb_id == tmdbId } ?: return
        val document = app.get(XDmoviesAPI + matched.path).document

        if(season == null) {
            document.select("div.download-item a").amap {
                loadSourceNameExtractor("XDmovies", it.attr("href"), "", subtitleCallback, callback)
            }
        } else {
            val epRegex = Regex(
                "S${season.toString().padStart(2, '0')}E${
                    episode.toString().padStart(2, '0')
                }", RegexOption.IGNORE_CASE
            )

            val episodeCards = document.select("div.episode-card").filter { card ->
                epRegex.containsMatchIn(card.selectFirst(".episode-title")?.text().orEmpty())
            }

            episodeCards.amap {
                val link = it.selectFirst("a")?.attr("href") ?: return@amap
                loadSourceNameExtractor("XDmovies", link, "", subtitleCallback, callback)
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

        app.get(url, referer = toonStreamAPI).document.select("div.video > iframe").amap {
            val source = it.attr("data-src")
            val doc = app.get(source).document
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
        if(title == null) return
        val document = app.get("$animezAPI/?act=search&f[keyword]=$title").document
        document.select("article > a").amap {
            val doc = app.get(animezAPI + it.attr("href")).document
            val titles = doc.select("ul.InfoList > li").text().replace(" -Dub", "")

            if(titles != title) return@amap

            val ep = episode ?: 1
            val links  = doc.select("li.wp-manga-chapter > a")
            val link = if (links.size >= ep) links[links.size - ep] else return@amap
            val type = if(link.text().contains("Dub", true)) "DUB" else "SUB"
            val epDoc = app.get(animezAPI + link.attr("href")).document
            val source = epDoc.select("iframe").attr("src")

            M3u8Helper.generateM3u8(
                "Animez [$type]",
                source.replace("/embed/", "/anime/"),
                "$animezAPI/",
            ).forEach(callback)
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
            "https://opensubtitles.stremio.homes/en|hi|de|ar|tr|es|ta|te|ru|ko/ai-translated=true|from=all|auto-adjustment=true",
            """https://subsense.nepiraw.com/gpqq9k22-{"languages":["en","hi","ta","es","ar"],"maxSubtitles":10}"""
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
                            newSubtitleFile(
                                getLanguage(lang) ?: lang,
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
                newSubtitleFile(
                    getLanguage(label) ?: label,
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

    // suspend fun invokePrimenet(
    //     tmdbId: Int? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     callback: (ExtractorLink) -> Unit
    // ) {
    //     val headers = mapOf(
    //         "Referer" to xprimeBaseAPI,
    //         "Origin" to xprimeBaseAPI,
    //         "User-Agent" to USER_AGENT
    //     )

    //     val tokenJson = app.get("$multiDecryptAPI/enc-xprime").text
    //     val jsonObject = JSONObject(tokenJson)
    //     val token = jsonObject.getString("result")

    //     val url = if(season == null) {
    //         "$xprimeAPI/primenet?id=$tmdbId&turnstile=$token"
    //     } else {
    //         "$xprimeAPI/primenet?id=$tmdbId&season=$season&episode=$episode&turnstile=$token"
    //     }

    //     val text = app.get(url, headers = headers).text
    //     val json = multiDecrypt(text, "dec-xprime") ?: return

    //     val sourceUrl = JSONObject(json).getString("url")
    //     if(sourceUrl == "null") {
    //         return
    //     }

    //     callback.invoke(
    //         newExtractorLink(
    //             "Primenet",
    //             "Primenet",
    //             sourceUrl,
    //             type = ExtractorLinkType.M3U8
    //         ) {
    //             this.referer = xprimeBaseAPI
    //             this.quality = 1080
    //             this.headers = headers
    //         }
    //     )
    // }

    // suspend fun invokePrimebox(
    //     title: String? = null,
    //     year: Int? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    //     callback: (ExtractorLink) -> Unit
    // ) {
    //     val headers = mapOf(
    //         "Referer" to xprimeBaseAPI,
    //         "Origin" to xprimeBaseAPI,
    //         "User-Agent" to USER_AGENT
    //     )

    //     val tokenJson = app.get("$multiDecryptAPI/enc-xprime").text
    //     val jsonObject = JSONObject(tokenJson)
    //     val token = jsonObject.getString("result")

    //     val url = if(season == null) {
    //         "$xprimeAPI/primebox?name=$title&fallback_year=$year&turnstile=$token"
    //     } else {
    //         "$xprimeAPI/primebox?name=$title&fallback_year=$year&season=$season&episode=$episode&turnstile=$token"
    //     }

    //     val text = app.get(url, headers = headers).text
    //     val json = multiDecrypt(text, "dec-xprime") ?: return
    //     val data = tryParseJson<Primebox>(json) ?: return

    //     data.streams?.let { streams ->
    //         listOf(
    //             360 to streams.quality360P,
    //             720 to streams.quality720P,
    //             1080 to streams.quality1080P
    //         ).forEach { (quality, link) ->
    //             if (!link.isNullOrBlank()) {
    //                 callback.invoke(
    //                     newExtractorLink(
    //                         "PrimeBox",
    //                         "PrimeBox",
    //                         link,
    //                         type = ExtractorLinkType.VIDEO,
    //                     ) {
    //                         this.quality = quality
    //                         this.headers = headers
    //                     }
    //                 )
    //             }
    //         }
    //     }

    //     if (data.hasSubtitles && data.subtitles.isNotEmpty()) {
    //         data.subtitles.forEach { sub ->
    //             val file = sub.file
    //             val label = sub.label
    //             if (!file.isNullOrBlank() && !label.isNullOrBlank()) {
    //                 subtitleCallback.invoke(
    //                     newSubtitleFile(
    //                         getLanguage(label),
    //                         file
    //                     )
    //                 )
    //             }
    //         }
    //     }
    // }

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
            val scriptText = app.get(episodeUrl).document.selectFirst("script#ng-state")?.data() ?: return
            val fullRegex = Regex("""\"number\"\s*:\s*${episode ?: 1}\b[\s\S]*?\"streamUrls\"\s*:\s*(\[[\s\S]*?])""")
            val epJson = fullRegex.find(scriptText)?.groupValues?.get(1) ?: return
            val urlRegex = Regex("""\"url\"\s*:\s*\"(.*?)\"""")
            urlRegex.findAll(epJson).forEach { match ->
                val source =  httpsify(match.groupValues[1])
                loadSourceNameExtractor("Asiaflix", source, episodeUrl, subtitleCallback, callback)
            }
        }
    }

    suspend fun invokeAllmovieland(
        id : String? = null,
        season : Int? = null,
        episode : Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val playerScript = app.get("https://allmovieland.link/player.js?v=60%20128").toString()
        val domainRegex = Regex("const AwsIndStreamDomain.*'(.*)';")
        val host = domainRegex.find(playerScript)?.groupValues?.getOrNull(1) ?: return
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

            callback.invoke(
                newExtractorLink(
                    "Allmovieland [$lang]",
                    "Allmovieland [$lang]",
                    path,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = referer
                    this.quality = Qualities.P1080.value
                }
            )
        }
    }

    suspend fun invokeHindmoviez(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        app.get("$hindMoviezAPI/?s=$id", timeout = 50L).document.select("h2.entry-title > a").amap {
            val doc = app.get(it.attr("href"), timeout = 50L).document
            if(episode == null) {
                doc.select("a.maxbutton").amap {
                    val res = app.get(it.attr("href"), timeout = 50L).document
                    val link = res.select("a.get-link-btn").attr("href")
                    getHindMoviezLinks("HindMoviez", link, callback)
                }
            }
            else {
                doc.select("a.maxbutton").amap {
                    val text = it.parent()?.parent()?.previousElementSibling()?.text() ?: ""
                    if(text.contains("Season $season")) {
                        val res = app.get(it.attr("href"), timeout = 50L).document
                        res.select("h3 > a").getOrNull(episode-1)?.let { link ->
                            getHindMoviezLinks("HindMoviez", link.attr("href"), callback)
                        }
                    }
                }
            }
        }
    }

    suspend fun invokeAnimekai(
        title: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36",
            "Connection" to "keep-alive",
        )

        suspend fun encrypt(id: String): String {
            val body = app.get("$multiDecryptAPI/enc-kai?text=$id").text
            val json = JSONObject(body)
            return json.getString("result")
        }

        data class ServerInfo(
            val serverType: String,
            val lid: String?,
        )

        fun getEpisodeToken(html: String, episode: Int): String? {
            val doc = Jsoup.parse(html)
            val selector = "div.eplist ul.range li a[num=\"$episode\"]"
            val episodeElement = doc.selectFirst(selector)
            return episodeElement?.attr("token")
        }

        fun parseServersFromHtml(html: String): List<ServerInfo> {
            val doc = Jsoup.parse(html)
            val servers = mutableListOf<ServerInfo>()

            val groups = doc.select("div.server-items.lang-group")
            for (grp in groups) {
                val serverType = grp.attr("data-id").uppercase()
                for (span in grp.select("span.server")) {
                    val lid = span.attr("data-lid").ifBlank { null }
                    servers.add(ServerInfo(serverType, lid))
                }
            }
            return servers
        }


        suspend fun decrypt(text: String): String {
            val jsonBody = """{"text":"$text"}"""
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val text = app.post(
                "$multiDecryptAPI/dec-kai",
                requestBody = requestBody
            ).text

            val json = JSONObject(text)
            return json.getJSONObject("result").getString("url")
        }

        val searchJson = app.get("$animekaiAPI/ajax/anime/search?keyword=$title", headers = headers).text
        val root = JSONObject(searchJson)
        var html = root.getJSONObject("result").getString("html")
        val doc = Jsoup.parse(html)
        val url = doc.selectFirst("a.aitem")?.attr("href") ?: return
        val id = app.get(animekaiAPI + url)
            .document
            .selectFirst("div.rate-box")?.attr("data-id") ?: return
        val enc_id = encrypt(id)
        val json = app.get("$animekaiAPI/ajax/episodes/list?ani_id=$id&_=$enc_id", headers = headers).text
        html = JSONObject(json).getString("result")
        val token = getEpisodeToken(html, episode ?: 1) ?: return
        val enc_token = encrypt(token)
        val servers_resp = app.get("$animekaiAPI/ajax/links/list?token=$token&_=$enc_token", headers = headers).text
        val servers = JSONObject(servers_resp).getString("result")
        val all = parseServersFromHtml(servers)

        all.amap {
            val lid = it.lid ?: return@amap
            val enc_lid = encrypt(lid)
            val type = it.serverType
            val embed_resp = app.get("$animekaiAPI/ajax/links/view?id=$lid&_=$enc_lid", headers = headers).text
            val encrypted = JSONObject(embed_resp).getString("result")
            val embed_url = decrypt(encrypted)
            loadExtractor(embed_url, "Animekai[$type]" ,subtitleCallback, callback)
        }
    }

    suspend fun invokeKisskh(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val slug = title.createSlug() ?: return
        val type = if (season == null) "2" else "1"
        val searchResponse = app.get(
            "$kissKhAPI/api/DramaList/Search?q=$title&type=$type",
            referer = "$kissKhAPI/"
        )
        if (searchResponse.code != 200) return
        val res = tryParseJson<ArrayList<KisskhResults>>(searchResponse.text) ?: return
        val (id, contentTitle) = if (res.size == 1) {
            res.first().id to res.first().title
        } else {
            val data = res.find {
                val slugTitle = it.title.createSlug() ?: return@find false
                val tSlug = it.title?.createSlug() ?: return@find false
                val tActual = it.title
                when (season) {
                    null -> tSlug == slug
                    1 -> tSlug == slug || (tSlug.contains(slug) && (tActual.contains("$year") || tActual.contains("Season 1", true)))
                    else -> tSlug.contains(slug) && tActual.contains("Season $season", true)
                }
            } ?: res.find { it.title.equals(title, true) }
            data?.id to data?.title
        }
        val detailResponse = app.get(
            "$kissKhAPI/api/DramaList/Drama/$id?isq=false",
            referer = "$kissKhAPI/Drama/${getKisskhTitle(contentTitle)}?id=$id"
        )
        if (detailResponse.code != 200) return
        val resDetail = detailResponse.parsedSafe<KisskhDetail>() ?: return
        val epsId =
            if (season == null) resDetail.episodes?.first()?.id else resDetail.episodes?.find { it.number == episode }?.id
                ?: return
        val epJson = app.get("$multiDecryptAPI/enc-kisskh?text=$epsId&type=vid", referer = kissKhAPI).text
        val vid_key = JSONObject(epJson).getString("result")
        val sourcesResponse = app.get(
            "$kissKhAPI/api/DramaList/Episode/$epsId.png?err=false&ts=&time=&kkey=$vid_key",
            referer = kissKhAPI
        )

        if (sourcesResponse.code != 200) return
        sourcesResponse.parsedSafe<KisskhSources>()?.let { source ->
            listOf(source.video, source.thirdParty).amap { link ->
                val safeLink = link ?: return@amap null
                when {
                    safeLink.contains(".m3u8") || safeLink.contains(".mp4") -> {
                        callback.invoke(
                            newExtractorLink(
                                "Kisskh",
                                "Kisskh",
                                fixUrl(safeLink, kissKhAPI),
                                INFER_TYPE
                            ) {
                                referer = kissKhAPI
                                quality = Qualities.P720.value
                                headers = mapOf("Origin" to kissKhAPI)
                            }
                        )
                    }

                    else -> {
                        val cleanedLink = safeLink.substringBefore("?").takeIf { it.isNotBlank() }
                            ?: return@amap null
                        loadSourceNameExtractor(
                            "Kisskh",
                            fixUrl(cleanedLink, kissKhAPI),
                            "$kissKhAPI/",
                            subtitleCallback,
                            callback,
                            Qualities.P720.value
                        )
                    }
                }
            }
        }

        val subJson = app.get("$multiDecryptAPI/enc-kisskh?text=$epsId&type=sub", referer = kissKhAPI).text
        val sub_key = JSONObject(subJson).getString("result")
        val subResponse = app.get("$kissKhAPI/api/Sub/$epsId&kkey=$sub_key")
        if (subResponse.code != 200) return
        tryParseJson<List<KisskhSubtitle>>(subResponse.text)?.forEach { sub ->
            subtitleCallback.invoke(newSubtitleFile(getLanguage(sub.label) ?: return@forEach, sub.src ?: return@forEach))
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
            "ott" to "hs",
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
            //provides wrong year
            if (season == null) {
                media?.title to netflixId
            } else {
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
        }

        app.get(
            "$netflix2API/mobile/hs/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            referer = "$netflix2API/",
            cookies = cookies,
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                newExtractorLink(
                    "Hotstar",
                    "Hotstar Multi Audio 🌐",
                    "$netflix2API/${it.file}",
                ) {
                    this.referer = "$netflix2API/"
                    this.quality = getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in"))
                    this.headers = M3U8_HEADERS + mapOf(
                        "Cookie" to "hd=on; ott=hs; t_hash_t=$NfCookie"
                    )
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
        val url = "$netflixAPI/pv/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals("${title?.trim()}", ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/pv/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
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
                        "$netflixAPI/pv/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}&page=$page",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/",
                    ).parsedSafe<NetflixResponse>()
                    episodeId = data?.episodes?.find { it.ep == "E$episode" }?.id
                    if(data?.nextPageShow != 1) { break }
                    page++
                }
                media?.title to episodeId
            } else {
                null to null
            }
        }

        app.get(
            "$netflix2API/pv/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflix2API/",
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                newExtractorLink(
                    "PrimeVideo",
                    "PrimeVideo Multi Audio 🌐",
                    "${netflix2API}${it.file}",
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$netflix2API/"
                    this.quality = getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in"))
                    this.headers = M3U8_HEADERS + mapOf(
                        "Cookie" to "hd=on; ott=pv; t_hash_t=$NfCookie"
                    )
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
            "hd" to "on",
            "ott" to "nf"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals("${title?.trim()}", ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
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
                        "$netflixAPI/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}&page=$page",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/",
                    ).parsedSafe<NetflixResponse>()
                    episodeId = data?.episodes?.find { it.ep == "$episode" }?.id
                    if(data?.nextPageShow != 1) { break }
                    page++
                }
                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        if(id == null || nfTitle == null) return

        val token = getNfVideoToken(netflixAPI, netflix2API, id, cookies)

        app.get(
            "$netflix2API/playlist.php?id=${id}&t=${nfTitle}&tm=${APIHolder.unixTime}&h=$token",
            headers = headers,
            cookies = cookies,
            referer = "$netflix2API/",
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                newExtractorLink(
                    "Netflix",
                    "Netflix Multi Audio 🌐",
                    "${netflix2API}${it.file}",
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$netflix2API/"
                    this.quality = getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in"))
                    this.headers = M3U8_HEADERS + mapOf(
                        "Cookie" to "hd=on; ott=nf; t_hash_t=$NfCookie"
                    )
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
        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
        )

        val document = app.get("$hdmovie2API/movies/${title.createSlug()}-$year", headers = headers, allowRedirects = true).document
        val ajaxUrl = "$hdmovie2API/wp-admin/admin-ajax.php"
        val commonHeaders = mapOf(
            "Accept" to "*/*",
            "X-Requested-With" to "XMLHttpRequest"
        )

        suspend fun String.getIframe(): String = Jsoup.parse(this).select("iframe").attr("src")

        suspend fun fetchSource(post: String, nume: String, type: String): String {
            val response = app.post(
                url = ajaxUrl,
                data = mapOf(
                "action" to "doo_player_ajax",
                "post" to post,
                "nume" to nume,
                "type" to type
            ),
            referer = hdmovie2API,
            headers = commonHeaders
            ).parsed<ResponseHash>()
            return response.embed_url.getIframe()
        }

        var link = ""

        if (episode != null) {
            document.select("ul#playeroptionsul > li").getOrNull(1)?.let { ep ->
                val post = ep.attr("data-post")
                val nume = (episode + 1).toString()
                link = fetchSource(post, nume, "movie")
        }
        } else {
            document.select("ul#playeroptionsul > li")
                .firstOrNull { it.text().contains("v2", ignoreCase = true) }
                ?.let { mv ->
                    val post = mv.attr("data-post")
                    val nume = mv.attr("data-nume")
                    link = fetchSource(post, nume, "movie")
                }
        }

        if (link.isEmpty()) {
            val type = if (episode != null) "(Combined)" else ""
            document.select("a[href*=dwo]").forEach { anchor ->
                val innerDoc = app.get(anchor.attr("href")).document
                innerDoc.select("div > p > a").forEach {
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

        if (link.isNotEmpty()) {
            loadSourceNameExtractor(
                "Hdmovie2",
                link,
                hdmovie2API,
                subtitleCallback,
                callback,
            )
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
            if(!it.text().trim().startsWith("$title ($year)")) return@amap
            val regex = Regex("""S\d{2}E\d{2}""", RegexOption.IGNORE_CASE)
            var singleEpEntry = false

            if (episode != null && regex.containsMatchIn(it.text())) {
                val currentEpRegex = Regex(
                    """E0*${episode}""",
                    RegexOption.IGNORE_CASE
                )

                if (!currentEpRegex.containsMatchIn(it.text())) {
                    return@amap
                } else {
                    singleEpEntry = true
                }
            }

            app.get(skymoviesAPI + it.attr("href")).document.select("div.Bolly > a").amap {
                val text = it.text()
                if(episode == null || singleEpEntry) {
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
        } else if(id?.contains("kitsu") == true) {
            "$torrentioAPI/$torrentioCONFIG/stream/series/$id:$episode.json"
        } else {
            "$torrentioAPI/$torrentioCONFIG/stream/series/$id:$season:$episode.json"
        }
        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
        )
        val res = app.get(url, headers = headers, timeout = 200L).parsedSafe<TorrentioResponse>()
        res?.streams?.forEach { stream ->
            val title = stream.title ?: stream.name ?: ""
            val regex = """👤\s*(\d+).*?💾\s*([0-9.]+\s*[A-Za-z]+)""".toRegex()
            val match = regex.find(title)
            val seeders = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val fileSize = match?.groupValues?.get(2) ?: ""

            if (seeders < 20) return@forEach
            val magnet = buildMagnetString(stream)
            callback.invoke(
                newExtractorLink(
                    "Torrentio🧲",
                    "Torrentio 🧲  |  👤 $seeders ⬆️  |" + getSimplifiedTitle(title + fileSize),
                    magnet,
                    ExtractorLinkType.MAGNET,
                ) {
                    this.quality = getIndexQuality(stream.name)
                }
            )
        }
    }

    suspend fun invokeAnimetosho(
        kitsuId: String? = null,
        malId: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val id = malId ?: kitsuId?.toIntOrNull() ?: return
        val type = if(malId == null) "kitsu_id" else "mal_id"
        val json = app.get("$anizipAPI/mappings?$type=$id").text
        val epId = getEpAnizipId(json, episode ?: 1) ?: return
        val json2 = app.get("$animetoshoAPI/json?eid=$epId").text
        val gson = Gson()
        val listType = object : TypeToken<List<Animetosho>>() {}.type
        val items: List<Animetosho> = gson.fromJson(json2, listType)
        val filtered = items.filter { (it.seeders ?: 0) > 20 }
        val sorted = filtered.sortedByDescending { it.seeders ?: -1 }

         for (it in sorted) {
            val title = it.title ?: ""
            val s = it.seeders ?: 0
            if (s < 20) continue
            val l = it.leechers ?: 0
            val magnet = it.magnetUri ?: ""
            val size = it.totalSize?.toLongOrNull() ?: 0L
            val sizeStr = formatSize(size)
            val type = if(
                title.contains("Dual", ignoreCase = true)
                || title.contains("DUB", ignoreCase = true)
            ) {
                "DUB"
            }
            else {
                "SUB"
            }

            val simplifiedTitle = getSimplifiedTitle(title + sizeStr)

            val displayTitle = "Animetosho [$type] 🧲 \n⬆️ $s | ⬇️ $l $simplifiedTitle"

            callback.invoke(
                newExtractorLink(
                    "Animetosho[$type]🧲",
                    displayTitle,
                    magnet,
                    ExtractorLinkType.MAGNET,
                ) {
                    this.quality = getIndexQuality(title)
                }
            )
        }
    }

    suspend fun invokeTorrentsDB(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) {
            "$torrentsDBAPI/stream/movie/$id.json"
        } else {
            "$torrentsDBAPI/stream/series/$id:$season:$episode.json"
        }
        val headers = mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
        )
        val res = app.get(url, headers = headers, timeout = 200L).parsedSafe<TorrentioResponse>()

        res?.streams?.forEach { stream ->
            val title = stream.title ?: stream.name ?: ""
            val regex = """👤\s*(\d+).*?💾\s*([0-9.]+\s*[A-Za-z]+)""".toRegex()
            val match = regex.find(title)
            val seeders = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val fileSize = match?.groupValues?.get(2) ?: ""

            if (seeders < 20) return@forEach
            val magnet = buildMagnetString(stream)
            callback.invoke(
                newExtractorLink(
                    "TorrentsDB🧲",
                    "TorrentsDB 🧲  | 👤 $seeders ⬆️  |" + getSimplifiedTitle(title + fileSize),
                    magnet,
                    ExtractorLinkType.MAGNET,
                ) {
                    this.quality = getIndexQuality(stream.name)
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun invokeBollyflix(
        id: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        var res1 = app.get("""$bollyflixAPI/search/${id ?: return} ${season ?: ""}""", interceptor = wpRedisInterceptor).document
        val url = res1.selectFirst("div > article > a")?.attr("href") ?: return
        val res = app.get(url, interceptor = wpRedisInterceptor).document
        val hTag = if (season == null) "h5" else "h4"
        val sTag = if (season == null) "" else "Season $season"
        val entries =
            res.select("div.thecontent.clearfix > $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                .filter { element -> !element.text().contains("Download", true) }.takeLast(4)
        entries.map {
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

    suspend fun invokeAnimes(
        malId: Int? = null,
        aniId: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        origin: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mal_response = app.get("$malsyncAPI/mal/anime/${malId ?: return}")
            .parsedSafe<MALSyncResponses>()

        // val zoroIds = malsync?.zoro?.keys?.map { it }
        val title = mal_response?.title
        val malsync = mal_response?.sites
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
                invokeAnimez(zorotitle, episode, callback)
            },
            {
                invokeAnimekai(title, episode, subtitleCallback, callback)
            },
            {
                if(origin == "imdb" && title != null) invokeTokyoInsider(
                    zorotitle,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb" && title != null) invokeAllanime(
                    zorotitle,
                    year,
                    episode,
                    subtitleCallback,
                    callback
                )
            },
            {
                if(origin == "imdb" && title!= null) invokeAnizone(
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
            }
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
        val id = app.get(url ?: return, headers).document.selectFirst("meta[property=og:url]")
            ?.attr("content").toString().substringAfterLast("/")
        val animeData =
            app.get("$animepaheAPI/api?m=release&id=$id&sort=episode_asc&page=1", headers)
                .parsedSafe<animepahe>()?.data
        val session = if(episode == null) {
            animeData?.firstOrNull()?.session ?: return
        } else {
            animeData?.getOrNull(episode-1)?.session ?: return
        }
        val doc = app.get("$animepaheAPI/play/$id/$session", headers).document

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
                    if (href.contains("kwik.cx")) {
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
            val lang = it.display ?: it.language
            subtitleCallback.invoke(
                newSubtitleFile(
                    getLanguage(lang) ?: return@forEach,
                    it.url
                )
            )
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
            "cookie" to "xla=s4t",
            "Accept-Language" to "en-US,en;q=0.9",
            "sec-ch-ua" to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Microsoft Edge\";v=\"120\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Linux\"",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "none",
            "Sec-Fetch-User" to "?1",
            "Upgrade-Insecure-Requests" to "1",
            "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36"
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
                    val doc = app.get(link, referer = api, headers = headers).document
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
                        val doc = app.get(it.attr("href"), referer = api, headers = headers).document
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
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = "$MovieDrive_API/searchapi.php?q=$imdbId"
        val jsonString = app.get(url, interceptor = wpRedisInterceptor).text
        val root = JSONObject(jsonString)
        if (!root.has("hits")) return
        val hits = root.getJSONArray("hits")

        for (i in 0 until hits.length()) {
            val hit = hits.getJSONObject(i)
            val doc = hit.getJSONObject("document")
            val currentImdbId = doc.optString("imdb_id")
            if(imdbId == currentImdbId) {
                val document = app.get(MovieDrive_API + doc.optString("permalink"), interceptor = wpRedisInterceptor).document
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
                val driveLink = if(it.contains("driveleech") || it.contains("driveseed")) {
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
            if (!href.isNullOrEmpty()) {
                app.get(
                    href,
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

    @RequiresApi(Build.VERSION_CODES.O)
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

        val entries = res.select("div.thecontent $hTag:matches((?i)$sTag.*(480p|720p|1080p|2160p))")
        .filter { element ->
            val text = element.text()
            !text.contains("MoviesMod", true)
        }

        entries.amap { it ->
            var link =
                it.nextElementSibling()?.select("a:contains($aTag)")?.attr("href")
                    ?.substringAfter("=") ?: ""
            //link = base64Decode(href)

            val selector =
                if (season == null) "p a.maxbutton" else "h3 a:matches(Episode $episode)"

            if (link.isNotEmpty()) {
                val source = app.get(link).document.selectFirst(selector)?.attr("href") ?: return@amap
                val bypassedLink = bypassHrefli(source).toString()
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
                newSubtitleFile(
                    getLanguage(it.attr("label")) ?: it.attr("label"),
                    it.attr("src")
                )
            )
        }

        val source = document.select("media-player").attr("src")
        callback.invoke(
            newExtractorLink(
                "Anizone",
                "Anizone Multi Audio 🌐",
                source,
                type = ExtractorLinkType.M3U8,
            ) {
                this.quality = Qualities.P1080.value
            }
        )
    }

    private suspend fun invokeHianime(
        url: String? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val hiId = url?.substringAfterLast("/") ?: return
        val id = hiId.substringAfterLast("-")

        val epId = app.get(
            "$hianimeAPI/ajax/v2/episode/list/$id",
            headers = headers
        ).parsedSafe<HianimeResponses>()?.html?.let {
            Jsoup.parse(it)
        }?.select("div.ss-list a")
            ?.find { it.attr("data-number") == "${episode ?: 1}" }
            ?.attr("data-id") ?: return

        val videoHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.5",
            "Accept-Encoding" to "gzip, deflate, br, zstd",
            "Origin" to "https://megacloud.blog",
            "Referer" to "https://megacloud.blog/",
            "Connection" to "keep-alive",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache"
        )

        val types = listOf("sub", "dub")
        val servers = listOf("HD-1", "HD-2")

        types.amap { t ->
            servers.amap { server ->
                val epData = app.get("$aniversehdAPI/api/v2/zoro/watch/$hiId?ep=$epId&type=$t&server=$server", referer = aniversehdAPI).parsedSafe<HianimeStreamResponse>() ?: return@amap
                val streamUrl = epData.sources.firstOrNull()?.url
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
                            newSubtitleFile(
                                getLanguage(track.label) ?: return@forEach,
                                track.file
                            )
                        )
                    }
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
                                sourceUrl,
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
                                            val lang = SubtitleHelper.fromTagToEnglishLanguageName(sub.lang ?: "") ?: sub.lang.orEmpty()
                                            val src = sub.src ?: return@forEach
                                            subtitleCallback(newSubtitleFile(getLanguage(lang) ?: "", httpsify(src)))
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

    suspend fun invokePrimeSrc(
        imdbId: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        val headers = mapOf(
            "accept" to "*/*",
            "referer" to if(season == null) "$PrimeSrcApi/embed/movie?imdb=$imdbId" else "$PrimeSrcApi/embed/tv?imdb=$imdbId&season=$season&episode=$episode",
            "sec-ch-ua" to "\"Chromium\";v=\"140\", \"Not=A?Brand\";v=\"24\", \"Google Chrome\";v=\"140\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Windows\"",
            "sec-fetch-dest" to "empty",
            "sec-fetch-mode" to "cors",
            "sec-fetch-site" to "same-origin",
            "user-agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
        )
        val url = if (season == null) {
            "$PrimeSrcApi/api/v1/s?imdb=$imdbId&type=movie"
        } else {
            "$PrimeSrcApi/api/v1/s?imdb=$imdbId&season=$season&episode=$episode&type=tv"
        }

        val serverList = app.get(url, timeout = 30, headers = headers).parsedSafe<PrimeSrcServerList>()
        serverList?.servers?.forEach {
            val rawServerJson = app.get("$PrimeSrcApi/api/v1/l?key=${it.key}", timeout = 30, headers = headers).text
            val jsonObject = JSONObject(rawServerJson)
            loadSourceNameExtractor("PrimeWire", jsonObject.optString("link",""), PrimeSrcApi, subtitleCallback, callback)
        }

    }

    // suspend fun invokeMp4Moviez(
    //     title: String?,
    //     season: Int?,
    //     episode: Int?,
    //     year: Int? = null,
    //     callback: (ExtractorLink) -> Unit,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    // ) {
    //     if (season == null) {
    //         val doc = app.get("$mp4MoviezAPI/search/$title.html", allowRedirects = true, timeout = 30).document
    //         val searchResponse = doc.select(".fl")
    //         searchResponse.forEach {
    //             val url = mp4MoviezAPI + it.select("a").attr("href")
    //             val name = it.select("img").attr("alt")
    //             val doc = app.get(url, allowRedirects = true, timeout = 30).document
    //             val link = mp4MoviezAPI + doc.select("div[style=\"text-align:left;\"] a").attr("href")
    //             if(name.contains(title.toString()) == true && name.contains(year.toString()))
    //             {
    //                 val cleanName =  name.replace("download","",true).replace("full","",true).replace("movie","",true).trim()
    //                 val doc = app.get(link).document
    //                 val links = doc.select("div[style=\"text-align:left;\"]")
    //                 links.forEach { item ->
    //                     val link = item.select("a").attr("href")
    //                     if (!link.contains("links4mad.online")) {
    //                         callback.invoke(
    //                             newExtractorLink(
    //                                 "Mp4Moviez [${cleanName}]",
    //                                 "Mp4Moviez [${cleanName}]",
    //                                 url = link,
    //                             ) {
    //                                 quality = getVideoQuality(link)
    //                             }
    //                         )
    //                     } else if (link.contains("links4mad.online")) {
    //                         val shortLinkUrl = item.select("a").attr("href")
    //                         val sDoc = app.post(shortLinkUrl).document
    //                         val links1 = sDoc.select(".col-sm-8.col-sm-offset-2.well.view-well a")
    //                         links1.forEach {
    //                             loadExtractor(it.attr("href"), subtitleCallback, callback)
    //                         }
    //                     }
    //                 }
    //             }
    //         }

    //     }
    // }

    // For rare movies
    suspend fun invokeFilm1k(
        title: String? = null,
        season: Int? = null,
        year: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        //val proxyUrl = "https://corsproxy.io/?url="
        val mainUrl = "$Film1kApi"
        if (season == null) {
            try {
                val fixTitle = title?.replace(":", "")?.replace(" ", "+")
                val doc = app.get("$mainUrl/?s=$fixTitle", cacheTime = 60, timeout = 30).document
                val posts = doc.select("header.entry-header").filter { element ->
                    element.selectFirst(".entry-title")?.text().toString().contains(
                        "${
                            title?.replace(
                                ":",
                                ""
                            )
                        }"
                    ) && element.selectFirst(".entry-title")?.text().toString()
                        .contains(year.toString())
                }.toList()
                val url = posts.firstOrNull()?.select("a:nth-child(1)")?.attr("href")
                val postDoc = url?.let { app.get("$it", cacheTime = 60, timeout = 30).document }
                val id = postDoc?.select("a.Button.B.on")?.attr("data-ide")
                repeat(5) { i ->
                    val mediaType = "application/x-www-form-urlencoded".toMediaType()
                    val body =
                        "action=action_change_player_eroz&ide=$id&key=$i".toRequestBody(mediaType)
                    val ajaxUrl = "$mainUrl/wp-admin/admin-ajax.php"
                    val doc =
                        app.post(ajaxUrl, requestBody = body, cacheTime = 60, timeout = 30).document
                    var url = doc.select("iframe").attr("src").replace("\\", "").replace(
                        "\"",
                        ""
                    ) // It is necessary because it returns link with double qoutes like this ("https://voe.sx/e/edpgpjsilexe")
                    val film1kRegex = Regex("https://film1k\\.xyz/e/([^/]+)/.*")
                    if (url.contains("https://film1k.xyz")) {
                        val matchResult = film1kRegex.find(url)
                        if (matchResult != null) {
                            val code = matchResult.groupValues[1]
                            url = "https://filemoon.sx/e/$code"
                        }
                    }
                    url = url.replace("https://films5k.com", "https://mwish.pro")
                    loadSourceNameExtractor(
                        "Film1k",
                        url,
                        "",
                        subtitleCallback,
                        callback
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun invokeCinemaOS(
        imdbId: String? = null,
        tmdbId: Int? = null,
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val sourceHeaders = mapOf(
            "Accept" to "*/*",
            "Accept-Language" to "en-US,en;q=0.9",
            "Connection" to "keep-alive",
            "Referer" to cinemaOSApi,
            "Host" to "cinemaos.tech",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "same-origin",
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36",
            "sec-ch-ua" to "\"Not;A=Brand\";v=\"99\", \"Google Chrome\";v=\"139\", \"Chromium\";v=\"139\"",
            "sec-ch-ua-mobile" to "?0",
            "sec-ch-ua-platform" to "\"Windows\"",
            "Content-Type" to "application/json"
        )

        val fixTitle = title?.replace(" ", "+")
        val cinemaOsSecretKeyRequest = CinemaOsSecretKeyRequest(tmdbId = tmdbId.toString(),imdbId= imdbId?.toString() ?: "", seasonId = season?.toString() ?: "", episodeId = episode?.toString() ?: "")
        val secretHash = cinemaOSGenerateHash(cinemaOsSecretKeyRequest,season != null)
        val type = if(season == null) {"movie"}  else {"tv"}
        val sourceUrl = if(season == null) {"$cinemaOSApi/api/provider?type=$type&tmdbId=$tmdbId&imdbId=$imdbId&t=$fixTitle&ry=$year&secret=$secretHash"} else {"$cinemaOSApi/api/provider?type=$type&tmdbId=$tmdbId&imdbId=$imdbId&seasonId=$season&episodeId=$episode&t=$fixTitle&ry=$year&secret=$secretHash"}
        val sourceResponse = app.get(sourceUrl, headers = sourceHeaders,timeout = 60).parsedSafe<CinemaOSReponse>()
        val decryptedJson = cinemaOSDecryptResponse(sourceResponse?.data,)
        val json = parseCinemaOSSources(decryptedJson.toString())

        json.forEach {
            val extractorLinkType = if(it["type"]?.contains("hls",true) ?: false) { ExtractorLinkType.M3U8} else if(it["type"]?.contains("dash",true) ?: false){ ExtractorLinkType.DASH} else if(it["type"]?.contains("mp4",true) ?: false){ ExtractorLinkType.VIDEO} else { INFER_TYPE}
            val bitrateQuality = if(it["bitrate"]?.contains("fhd",true) ?: false) { Qualities.P1080.value } else if(it["bitrate"]?.contains("hd",true) ?: false){ Qualities.P720.value} else if(it["bitrate"]?.contains("4K",true) ?: false){ Qualities.P2160.value} else { Qualities.P1080.value}
            val quality =  if(it["quality"]?.isNotEmpty() == true && it["quality"]?.toIntOrNull() !=null) getQualityFromName(it["quality"]) else if (it["quality"]?.isNotEmpty() == true)  if(it["quality"]?.contains("fhd",true) ?: false) { Qualities.P1080.value } else if(it["quality"]?.contains("hd",true) ?: false){ Qualities.P720.value} else { Qualities.P1080.value} else bitrateQuality
            callback.invoke(
                newExtractorLink(
                    "CinemaOS [${it["server"]}] ${it["bitrate"]}  ${it["speed"]}".replace("\\s{2,}".toRegex(), " ").trim(),
                    "CinemaOS [${it["server"]}] ${it["bitrate"]} ${it["speed"]}".replace("\\s{2,}".toRegex(), " ").trim(),
                    url = it["url"].toString(),
                    type = extractorLinkType
                )
                {
                    this.headers = mapOf("Referer" to cinemaOSApi)
                    this.quality = quality
                }
            )
        }
    }

    // suspend fun invokeTripleOneMovies(
    //     tmdbId: Int? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     callback: (ExtractorLink) -> Unit,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    // ) {
    //     val STATIC_PATH = "fcd552c4321aeac1e62c5304913b3420be75a19d390807281a425aabbb5dc4c0"
    //     val url = if(season == null) "$tripleOneMoviesApi/movie/$tmdbId" else "$tripleOneMoviesApi/tv/$tmdbId/$season/$episode"
    //     val headers = mapOf(
    //         "Referer" to tripleOneMoviesApi,
    //         "Content-Type" to "application/octet-stream",
    //         "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36",
    //         "X-Requested-With" to "XMLHttpRequest")
    //     val response = app.get(url,headers = headers, timeout = 20).text
    //     val rawData = extractData("\\{\"data\":\"(.*?)\"", response)
    //     // AES encryption
    //     val aesEncrypted = aesEncrypt(rawData)

    //     // XOR operation
    //     val xorResult = xorOperation(aesEncrypted)

    //     // Custom encoding
    //     val encodedFinal = customEncode(xorResult)

    //     // Get servers
    //     val apiServers = "$tripleOneMoviesApi/${STATIC_PATH}/$encodedFinal/sr"
    //     val serversResponse = app.get(apiServers, timeout = 20, headers = headers).text
    //     val servers = parseServers(serversResponse)
    //     val urlList = mutableMapOf<String,String>()
    //     servers.forEach {
    //         try {
    //             val apiStream = "$tripleOneMoviesApi/${STATIC_PATH}/${it.data}"
    //             val streamResponse = app.get(apiStream, timeout = 20, headers = headers).text
    //             if(streamResponse.isNotEmpty())
    //             {
    //                 val jsonObject = JSONObject(streamResponse)
    //                 val url = jsonObject.getString("url")

    //                 urlList.put(it.name,url)
    //             }
    //         } catch (e: Exception) {
    //             TODO("Not yet implemented")
    //         }
    //     }

    //     urlList.forEach {
    //         callback.invoke(
    //             newExtractorLink(
    //                 "111Movies [${it.key}]",
    //                 "111Movies [${it.key}]",
    //                 url = it.value,
    //                 type = ExtractorLinkType.M3U8
    //             )
    //             {
    //                 this.quality = Qualities.P1080.value
    //                 this.headers = M3U8_HEADERS
    //             }
    //         )
    //     }
    // }

    // suspend fun invokeVidPlus(
    //     tmdbId: Int? = null,
    //     imdbId: String? = null,
    //     title: String? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     year: Int? = null,
    //     callback: (ExtractorLink) -> Unit,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    // ) {
    //     val headers = mapOf(
    //         "Accept" to "*/*",
    //         "Referer" to vidPlusApi,
    //         "Origin" to vidPlusApi,
    //         "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36",
    //         "X-Requested-With" to "XMLHttpRequest"
    //     )

    //     // Build request parameters and fetch encrypted response
    //     val requestArgs = listOf(title, year, imdbId).joinToString("*")
    //     val urlListMap = mutableMapOf<String,String>()
    //     val serList = """
    //                 [
    //                   {
    //                     "flag": "US",
    //                     "name": "Nexon",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "US",
    //                     "name": "Crown",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "US",
    //                     "name": "Cine",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "US",
    //                     "name": "Wink",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "VN",
    //                     "name": "Viet",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "AU",
    //                     "name": "Orion",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "US",
    //                     "name": "Beta",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "GB",
    //                     "name": "Gork",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "US",
    //                     "name": "Vox",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "US",
    //                     "name": "Minecloud",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "US",
    //                     "name": "Joker",
    //                     "audioLanguage": "English audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "GB",
    //                     "name": "Leo",
    //                     "audioLanguage": "Original audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "4K",
    //                     "name": "4K",
    //                     "audioLanguage": "Original audio",
    //                     "language": "English"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Hindi",
    //                     "audioLanguage": "Hindi audio",
    //                     "language": "Hindi"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Indus",
    //                     "audioLanguage": "Hindi audio",
    //                     "language": "Hindi"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Delta",
    //                     "audioLanguage": "Bengali audio",
    //                     "language": "Bengali"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Ben",
    //                     "audioLanguage": "Bengali audio",
    //                     "language": "Bengali"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Pearl",
    //                     "audioLanguage": "Tamil audio",
    //                     "language": "Tamil"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Tamil",
    //                     "audioLanguage": "Tamil audio",
    //                     "language": "Tamil"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Ruby",
    //                     "audioLanguage": "Telugu audio",
    //                     "language": "Telugu"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Tel",
    //                     "audioLanguage": "Telugu audio",
    //                     "language": "Telugu"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Mal",
    //                     "audioLanguage": "Malayalam audio",
    //                     "language": "Malayalam"
    //                   },
    //                   {
    //                     "flag": "IN",
    //                     "name": "Kan",
    //                     "audioLanguage": "Kannada audio",
    //                     "language": "Kannada"
    //                   },
    //                   {
    //                     "flag": "FR",
    //                     "name": "Lava",
    //                     "audioLanguage": "French audio",
    //                     "language": "French"
    //                   }
    //                 ]
    //                 """.trimIndent()

    //     val serverJson = JSONArray(serList)
    //     for (index in 0 until serverJson.length()) {
    //         val obj = serverJson.getJSONObject(index)
    //         try {
    //             val serverName = obj.getString("name")
    //             val serverLanguage = obj.getString("language")
    //             val serverId = index+1;
    //             val serverUrl = if(season == null) "$vidPlusApi/api/server?id=$tmdbId&sr=$serverId&args=$requestArgs" else  "$vidPlusApi/api/server?id=$tmdbId&sr=$serverId&ep=$episode&ss=$season&args=$requestArgs"

    //             val apiResponse = app.get(serverUrl,headers=headers, timeout = 20,).text
    //             if (apiResponse.contains("\"data\"",ignoreCase = true)) {
    //                 val decodedPayload = String(Base64.getDecoder().decode(JSONObject(apiResponse).getString("data")))
    //                 val payloadJson = JSONObject(decodedPayload)

    //                 val ciphertext = Base64.getDecoder().decode(payloadJson.getString("encryptedData"))
    //                 val password = payloadJson.getString("key")
    //                 val salt = hexStringToByteArray2(payloadJson.getString("salt"))
    //                 val iv = hexStringToByteArray2(payloadJson.getString("iv"))
    //                 val derivedKey = derivePbkdf2Key(password, salt, 1000, 32)
    //                 val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    //                 cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derivedKey, "AES"), IvParameterSpec(iv))
    //                 val decryptedText = unpadData(cipher.doFinal(ciphertext))
    //                 val decryptedString = String(decryptedText)

    //                 val regex = Regex("\"url\":\"(.*?)\",")
    //                 val match = regex.find(decryptedString)
    //                 val streamURl = match?.groupValues?.get(1)
    //                 if (!streamURl.isNullOrEmpty()) {
    //                     var finalStreamUrl = streamURl
    //                     if (!hasHost(streamURl.toString())) {
    //                         finalStreamUrl = app.head("$vidPlusApi$streamURl",headers= headers, allowRedirects = false).headers.get("Location")
    //                    }


    //                     urlListMap["$serverName | $serverLanguage"] = finalStreamUrl.toString()
    //                 }
    //             }
    //         } catch (e: Exception) {
    //             TODO("Not yet implemented")
    //         }
    //     }

    //     urlListMap.forEach {
    //         callback.invoke(
    //             newExtractorLink(
    //                 "VidPlus [${it.key}]",
    //                 "VidPlus [${it.key}]",
    //                 url = it.value,
    //             )
    //             {
    //                 this.quality = Qualities.P1080.value
    //                 this.headers = mapOf("Origin" to vidPlusApi)
    //             }
    //         )
    //     }
    // }

    suspend fun invokeMultiEmbeded(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season == null) "$multiEmbededApi/?video_id=$tmdbId&tmdb=1" else " $multiEmbededApi/?video_id=$tmdbId&tmdb=1&s=$season&e=$episode"
        val streamingUrl = app.get(url, allowRedirects = false).headers.get("Location")
        val mediaType = "application/x-www-form-urlencoded".toMediaType()
        val body = "button-click=ZEhKMVpTLVF0LVBTLVF0TnprekxTLVF5LVBEVXRMLTAtVjNOLTBjMU8tMEF5TmpneC1QRFUtNQ==&button-referer=".toRequestBody(mediaType)
        val sourcesDoc = app.post(streamingUrl.toString(), requestBody = body, timeout = 40).text
        val pattern = "load_sources\\(\"(.*?)\"\\)".toRegex()
        val sourcesHash = pattern.find(sourcesDoc)?.groupValues?.getOrNull(1) ?: return
        val hostUrl = "${URI(streamingUrl).scheme}://${URI(streamingUrl).host}"
        val rbody = FormBody.Builder().add("token", sourcesHash).build()
        val sourceslistDoc = app.post("$hostUrl/response.php", requestBody = rbody, headers = mapOf("x-requested-with" to "XMLHttpRequest")).document
        val serverList = sourceslistDoc.select("li")
        serverList.forEach {
            val serverDataId = it.attr("data-id")
            val serverData = it.attr("data-server")
            val playVideoUrl = "$hostUrl/playvideo.php?video_id=$serverDataId&server_id=${serverData}r&token=$sourcesHash&init=0"
            val src = app.get(playVideoUrl, ).document
            val iframe = src.select("iframe").attr("src")
            loadSourceNameExtractor("SuperEmbeded",iframe,hostUrl,subtitleCallback,callback)
        }
    }


       suspend fun invokeVicSrcWtf(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val referer = "https://www.vidsrc.wtf"
        val headers = mapOf("Origin" to referer,"Referer" to referer,"User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")

        try {
            val mainServer = if(season == null) "$vidSrcApi/main/movie/$tmdbId" else "$vidSrcApi/main/tv/$tmdbId/$season/$episode"
            val mainResponse = app.get(mainServer, timeout = 30, headers = headers).text
            val jsonObject = JSONObject(mainResponse)
            val streamUrl = jsonObject.getJSONObject("stream").getString("url")
            M3u8Helper.generateM3u8(
                "VidSrc [Main]",
                streamUrl,
                referer,
            ).forEach(callback)
        } catch (e: Exception) {}

        try {
            val hindiServer =
                if (season == null) "$vidSrcHindiApi/movie/$tmdbId" else "$vidSrcHindiApi/tv/$tmdbId/$season/$episode"
            val hindiResponse = app.get(hindiServer, timeout = 30, headers = headers).text
            val hindiJsonObject = JSONObject(hindiResponse)
            val streamsArray = hindiJsonObject.getJSONArray("streams")
            for (i in 0 until streamsArray.length()) {
                val streamObj = streamsArray.getJSONObject(i)
                val language = streamObj.getString("language")
                val url = streamObj.getString("url")
                // Convert headers JSONObject to Map<String, String>
                val headersJson = streamObj.getJSONObject("headers")
                val headersMap = mutableMapOf<String, String>()
                headersJson.keys().forEach { key ->
                    headersMap[key] = headersJson.getString(key)
                }
                M3u8Helper.generateM3u8(
                    "VidSrc [$language]",
                    url,
                    headersJson.getString("Referer"),
                    headers = headersMap
                ).forEach(callback)

            }
        } catch (e: Exception) { }

        try {
            val embededServer = if(season == null) "$vidSrcApi/premium_embeds/movie/$tmdbId" else "$vidSrcApi/premium_embeds/tv/$tmdbId/$season/$episode"
            val embededResponse = app.get(embededServer, timeout = 30, headers = headers).text
            val embededJsonObject = JSONObject(embededResponse)
            val linksArray = embededJsonObject.getJSONArray("links")
            for (i in 0 until linksArray.length()) {
                val streamObj = linksArray.getJSONObject(i)
                val url = streamObj.getString("url")
                loadSourceNameExtractor("VidSrc",url,"",subtitleCallback,callback)
            }
        } catch (e: Exception) { }
    }

    suspend fun invokeVidzee(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val KEY_HEX = "6966796f75736372617065796f75617265676179000000000000000000000000"
        val headers = mapOf(
            "Referer" to "",
            "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        )
        for (i in 1..10) {
            try {
                val mainUrl = if(season == null) "$vidzeeApi/api/server?id=$tmdbId&sr=$i" else "$vidzeeApi/api/server?id=$tmdbId&sr=$i&ss=$season&ep=$episode"
                val response = app.get(mainUrl,headers, timeout = 30).text;
                val json = JSONObject(response);

                val urlArray = json.optJSONArray("url")
                if (urlArray != null) {
                    val encryptedUrl = urlArray.getJSONObject(0).getString("link")
                    val serverName = urlArray.getJSONObject(0).getString("name")

                    // Decode decryption parameters
                    val decoded = Base64.getDecoder().decode(encryptedUrl).toString(Charsets.UTF_8)
                    val parts = decoded.split(":")
                    val ivB64 = parts[0]
                    val ciphertextB64 = parts[1]

                    // Prepare decryption parameters
                    val iv = Base64.getDecoder().decode(ivB64)
                    val ciphertext = Base64.getDecoder().decode(ciphertextB64)
                    val key = hexStringToByteArray(KEY_HEX)


                    // Decrypt using AES-CBC
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    val secretKeySpec = SecretKeySpec(key, "AES")
                    val ivParameterSpec = IvParameterSpec(iv)

                    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
                    val decryptedData = cipher.doFinal(ciphertext)

                    // Remove PKCS7 padding (handled by PKCS5Padding in Java/Kotlin)
                    val videoUrl = String(decryptedData, Charsets.UTF_8).trim()

                    callback.invoke(
                        newExtractorLink(
                            "Vidzee [${serverName}]",
                            "Vidzee [${serverName}]",
                            url = videoUrl,
                            type =  ExtractorLinkType.M3U8,
                        ) {
                            this.quality = 1080
                            this.referer = "https://player.vidzee.wtf/"
                            this.headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                TODO("Not yet implemented")
            }
        }
    }

    suspend fun invokeBollywood(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit
    ) {
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val titleSlug = title?.replace(" ", ".")
        val url = if (season == null) {
            """$bollywoodAPI/files/search?q=${titleSlug}&page=1"""
        } else {
            """$bollywoodAPI/files/search?q=${titleSlug}.S${seasonSlug}E${episodeSlug}&page=1"""
        }

        val response = app.get(
            url,
            referer = bollywoodBaseAPI
        ).text
        val jsonObject = JsonParser.parseString(response).asJsonObject

        if (jsonObject.has("files")) {
            val filesArray = jsonObject.getAsJsonArray("files")

            filesArray.forEach { element ->
                val item = element.asJsonObject
                val fileName = item.get("file_name").asString
                if(fileName.contains(".$titleSlug")) return@forEach
                val fileId = item.get("id").asString
                val size = formatSize(item.get("file_size").asString.toLong())
                val res = app.get(
                    "$bollywoodAPI/genLink?type=files&id=$fileId",
                    referer = bollywoodBaseAPI
                ).text

                val linkJson = JsonParser.parseString(res).asJsonObject
                if (linkJson.has("url")) {
                    val streamUrl = linkJson.get("url").asString
                    val simplifiedTitle = getSimplifiedTitle("$fileName $size")

                    callback.invoke(
                        newExtractorLink(
                            "Bollywood",
                            "Bollywood $simplifiedTitle",
                            streamUrl,
                            ExtractorLinkType.VIDEO
                        ) {
                            this.quality = getIndexQuality(fileName)
                            this.referer = bollywoodBaseAPI
                        }
                    )
                }
            }
        }
    }

    suspend fun invokeMoviebox(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {

        fun unwrapData(json: JSONObject): JSONObject {
            val data = json.optJSONObject("data")
            if (data != null) {
                val nestedData = data.optJSONObject("data")
                return nestedData ?: data
            }
            return json
        }

        val client = OkHttpClient()
        val HOST = "h5.aoneroom.com"
        val BASE_URL = "https://$HOST"

        val BASE_HEADERS = Headers.Builder()
        .add("X-Client-Info", "{\"timezone\":\"Africa/Nairobi\"}")
        .add("Accept-Language", "en-US,en;q=0.5")
        .add("Accept", "application/json")
        .add("User-Agent", "okhttp/4.12.0")
        .add("Referer", BASE_URL)
        .add("Host", HOST)
        .add("Connection", "keep-alive")
        .build()

        val initRequest = Request.Builder()
            .url("$BASE_URL/wefeed-h5-bff/app/get-latest-app-pkgs?app_name=moviebox")
            .headers(BASE_HEADERS)
            .build()

        client.newCall(initRequest).execute().close()

        val subjectType = if (season != null) 2 else 1
        val searchJson = JSONObject().apply {
            put("keyword", title)
            put("page", 1)
            put("perPage", 24)
            put("subjectType", subjectType)
        }

        val searchRequest = Request.Builder()
            .url("$BASE_URL/wefeed-h5-bff/web/subject/search")
            .headers(BASE_HEADERS)
            .post(searchJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val searchResponseString = client.newCall(searchRequest).execute().use {
            if (!it.isSuccessful) throw IOException("Search failed: ${it.code}")
            it.body.string()
        }

        val searchObj = JSONObject(searchResponseString)
        val results = unwrapData(searchObj)
        val items = results.optJSONArray("items")

        if (items == null || items.length() == 0) return

        // Regex to clean Season info (e.g., " S2")
        val seasonSuffixRegex = Regex("\\sS\\d+$")

        // Regex to Validate Title & Capture Language
        // 1. Matches strictly the title (ignoring case)
        // 2. Optionally captures content inside brackets: \[([^\]]+)\]
        val titleMatchRegex = """^${Regex.escape(title ?: "")}(?: \[([^\]]+)\])?$""".toRegex(RegexOption.IGNORE_CASE)

        val uniqueIdsWithLang = mutableMapOf<String, String>()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val rawTitle = item.optString("title", "")
            val id = item.optString("subjectId")

            if (id.isEmpty()) continue

            // Clean Season Suffix: "Title [Hindi] S2" -> "Title [Hindi]"
            val cleanTitle = rawTitle.replace(seasonSuffixRegex, "")

            // Check match and extract language
            val matchResult = titleMatchRegex.find(cleanTitle)

            if (matchResult != null) {
                val langTag = matchResult.groups[1]?.value
                val language = langTag ?: "Original"

                if (!uniqueIdsWithLang.containsKey(id)) {
                    uniqueIdsWithLang[id] = language
                }
            }
        }

        if (uniqueIdsWithLang.isEmpty()) return

        uniqueIdsWithLang.forEach { (subjectId, language) ->
            val detailUrl = "$BASE_URL/wefeed-h5-bff/web/subject/detail?subjectId=${subjectId}"
            val detailRequest = Request.Builder()
                .url(detailUrl)
                .headers(BASE_HEADERS)
                .build()

            val detailResponseString = client.newCall(detailRequest).execute().use {
                it.body.string()
            }

            val detailObj = JSONObject(detailResponseString)
            val detailInfo = unwrapData(detailObj)
            val detailSubject = detailInfo.optJSONObject("subject")
            val detailPath = detailSubject?.optString("detailPath") ?: ""
            val params = StringBuilder("subjectId=$subjectId")

            if (season != null) {
                params.append("&se=$season")
                params.append("&ep=$episode")
            }

            val downloadHeaders = BASE_HEADERS.newBuilder()
                .set("Referer", "https://fmoviesunblocked.net/spa/videoPlayPage/movies/$detailPath?id=$subjectId&type=/movie/detail")
                .set("Origin", "https://fmoviesunblocked.net")
                .build()

            val downloadRequest = Request.Builder()
                .url("$BASE_URL/wefeed-h5-bff/web/subject/download?$params")
                .headers(downloadHeaders)
                .build()

            val downloadResponseString = client.newCall(downloadRequest).execute().use {
                it.body.string()
            }

            val sourceObj = JSONObject(downloadResponseString)
            val sourceData = unwrapData(sourceObj)
            val downloads = sourceData.optJSONArray("downloads")

            if (downloads == null || downloads.length() == 0) return@forEach

            for (i in 0 until downloads.length()) {
                val d = downloads.getJSONObject(i)
                val dlink = d.optString("url")
                if (dlink.isNotEmpty()) {
                    val resolution = d.optInt("resolution")
                    callback.invoke(
                        newExtractorLink(
                            "MovieBox [$language]",
                            "MovieBox [$language]",
                            dlink,
                        ) {
                            this.headers = mapOf(
                                "Referer" to "https://fmoviesunblocked.net/",
                                "Origin" to "https://fmoviesunblocked.net"
                            )
                            this.quality = resolution
                        }
                    )
                }
            }

            val subtitles = sourceData.optJSONArray("captions") ?: return@forEach

            for (i in 0 until subtitles.length()) {
                val s = subtitles.getJSONObject(i)
                val slink = s.optString("url")
                if (slink.isNotEmpty()) {
                    val lan = s.optString("lan")
                    subtitleCallback.invoke(
                        newSubtitleFile(
                            getLanguage(lan) ?: lan,
                            slink
                        )
                    )
                }
            }

        }
    }

    // suspend fun invokeVidFastPro(
    //     tmdbId: Int? = null,
    //     season: Int? = null,
    //     episode: Int? = null,
    //     callback: (ExtractorLink) -> Unit,
    //     subtitleCallback: (SubtitleFile) -> Unit,
    // ) {
    //     val STATIC_PATH =
    //         "hezushon/088b73be/1000068959767573/b28099eb-4dea-5589-9baa-a6b6560cad62/oh/y"
    //     val url =
    //         if (season == null) "$vidfastProApi/movie/$tmdbId" else "$vidfastProApi/tv/$tmdbId/$season/$episode"
    //     val headers = mapOf(
    //         "Accept" to "*/*",
    //         "Referer" to vidfastProApi,
    //         "X-Csrf-Token" to "pASKDBkXwNun4w2Y8RRo8lQ3thmugGxj",
    //         "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36",
    //         "X-Requested-With" to "XMLHttpRequest"
    //     )
    //     val response = app.get(url, headers = headers, timeout = 20).text
    //     val regex = Regex("""\\"en\\":\\"(.*?)\\"""")
    //     val match = regex.find(response)
    //     val rawData = match?.groupValues?.get(1)
    //     if (rawData.isNullOrEmpty()) {
    //         return;
    //     }
    //     // AES encryption setup
    //     val keyHex = "0cd6aa69d843f9565187caea6b260b59716a63f79dfef3ec3a2c2834b6724e55"
    //     val ivHex = "67b2ddac30102321a83e3dbf83417696"
    //     val aesKey = hexStringToByteArray2(keyHex)
    //     val aesIv = hexStringToByteArray2(ivHex)

    //     // Encrypt raw data
    //     val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    //     cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(aesIv))
    //     val paddedData = padData(rawData.toByteArray(Charsets.UTF_8), 16)
    //     val aesEncrypted = cipher.doFinal(paddedData)

    //     // XOR operation
    //     val xorKey = hexStringToByteArray2("2329aba4015a")
    //     val xorResult = aesEncrypted.mapIndexed { i, byte ->
    //         (byte.toInt() xor xorKey[i % xorKey.size].toInt()).toByte()
    //     }.toByteArray()

    //     // Encode XORed data
    //     val encodedFinal = customEncode(xorResult)

    //     // Get servers
    //     val apiServers = "$vidfastProApi/$STATIC_PATH/yu-5EXWKpA/$encodedFinal"
    //     val serversResponse = app.get(
    //         apiServers,
    //         timeout = 20,
    //         interceptor = CloudflareKiller(),
    //         headers = headers
    //     ).text
    //     if (serversResponse.isEmpty()) return
    //     val servers = parseServers(serversResponse)
    //     val urlList = mutableMapOf<String, String>()
    //     servers.forEach {
    //         try {
    //             val apiStream = "$vidfastProApi/${STATIC_PATH}/c14/${it.data}"
    //             val streamResponse = app.get(apiStream, timeout = 20, headers = headers).text
    //             if (streamResponse.isNotEmpty()) {
    //                 val jsonObject = JSONObject(streamResponse)
    //                 val url = jsonObject.getString("url")

    //                 urlList.put(it.name, url)
    //             }
    //         } catch (e: Exception) {
    //             TODO("Not yet implemented")
    //         }
    //     }

    //     urlList.forEach {
    //         callback.invoke(
    //             newExtractorLink(
    //                 "VidFastPro [${it.key}]",
    //                 "VidFastPro [${it.key}]",
    //                 url = it.value,
    //             )
    //             {
    //                 this.quality = Qualities.P1080.value
    //             }
    //         )
    //     }
    // }
}
