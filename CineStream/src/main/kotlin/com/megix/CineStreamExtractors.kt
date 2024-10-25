package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.network.CloudflareKiller
import android.util.Log
import android.util.Base64
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import java.net.URLEncoder
import okhttp3.FormBody
import java.nio.charset.StandardCharsets
import org.jsoup.Jsoup

object CineStreamExtractors : CineStreamProvider() {

    suspend fun invokeRar(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val json = app.get("$RarAPI/ajax/posts?q=$title ($year)").text
        val responseData = parseJson<RarResponseData>(json)
        val id = responseData.data?.firstOrNull { 
            it.second_name == title 
        }?.id ?: return
        val slug = "$title $year $id".createSlug()
        val url = if(season != null) "$RarAPI/show/$slug/season/$season/episode/$episode" else "$RarAPI/movie/$slug"
        val embedId = app.get(url).document.selectFirst("a.btn-service")?.attr("data-embed") ?: return
        val body = FormBody.Builder().add("id", embedId).build()
        val document = app.post("$RarAPI/ajax/embed", requestBody = body).document
        val regex = Regex("""(https?:\/\/[^\"']+\.m3u8)""")
        val link = regex.find(document.toString())?.groupValues?.get(1) ?: return
        callback.invoke(
            ExtractorLink(
                "Rar",
                "Rar",
                link,
                referer = "",
                Qualities.P1080.value,
                true
            )
        )
    }

    suspend fun invoke2embed(
        id:  String,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null && episode != null) "${TwoEmbedAPI}/scrape?id=${id}&s=${season}&e=${episode}" else "${TwoEmbedAPI}/scrape?id=${id}"
        val json = app.get(url).text
        val data = parseJson<TwoEmbedQuery>(json)
        data.stream.forEach {
            callback.invoke(
                ExtractorLink(
                    "2embed[${it.type}]",
                    "2embed[${it.type}]",
                    it.playlist,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    INFER_TYPE,
                )
            )
        }
    }

    suspend fun invokeFilmyxy(
        id: String,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null && episode != null) "${FilmyxyAPI}/search?id=${id}&s=${season}&e=${episode}" else "${FilmyxyAPI}/search?id=${id}"
        val json = app.get(url, timeout = 20L).text
        val data = parseJson<NovaVideoData>(json) ?: return
        for (stream in data.stream) {
            for ((quality, details) in stream.qualities) {
                callback.invoke(
                    ExtractorLink(
                        "Filmyxy",
                        "Filmyxy",
                        details.url,
                        "",
                        getQualityFromName(quality),
                        INFER_TYPE,
                    )
                )
            }
        }
        for (stream in data.stream) {
            for (caption in stream.captions) {
                subtitleCallback.invoke(
                    SubtitleFile(
                        caption.language,
                        caption.url
                    )
                )
            }
        }
    }

    suspend fun invokeMovies(
        tmdbId: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if (season == null) {
            "$moviesAPI/movie/$tmdbId"
        } else {
            "$moviesAPI/tv/$tmdbId-$season-$episode"
        }

        val iframe =
            app.get(
                url,
                referer = "https://pressplay.top/"
            ).document.selectFirst("iframe")
                ?.attr("src")

        loadExtractor(iframe ?: return, "$moviesAPI/", subtitleCallback, callback)
    }

    suspend fun invokeVidSrcNL(
        id: Int,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
    ) {
        val sources = listOf("vidcloud", "upstream", "hindi", "english")
        sources.forEach { source ->
            val url = if(season != null) "https://${source}.vidsrc.nl/stream/tv/${id}/${season}/{episode}" else "https://${source}.vidsrc.nl/stream/movie/${id}"
            val doc = app.get(url).document
            val link = doc.selectFirst("div#player-container > media-player")?.attr("src")
            if (!link.isNullOrEmpty()) {
                callback.invoke(
                    ExtractorLink(
                        "VidSrcNL[${source}]",
                        "VidSrcNL[${source}]",
                        link,
                        referer = "",
                        quality = Qualities.Unknown.value,
                        isM3u8 = true,
                    )
                )
            }
        }
    }

    suspend fun invokeAstra(
        title: String,
        imdb_id: String,
        tmdb_id: Int,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val query = if (season != null && episode != null) {
            """{"title":"$title","releaseYear":$year,"tmdbId":"$tmdb_id","imdbId":"$imdb_id","type":"show","season":"$season","episode":"$episode"}"""
        } else {
            """{"title":"$title","releaseYear":$year,"tmdbId":"$tmdb_id","imdbId":"$imdb_id","type":"movie","season":"","episode":""}"""
        }
        val headers = mapOf("Origin" to "https://www.vidbinge.com")
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val json = app.get("${WHVXAPI}/search?query=${encodedQuery}&provider=astra", headers = headers).text
        val data = parseJson<WHVX>(json) ?: return
        val encodedUrl = URLEncoder.encode(data.url, StandardCharsets.UTF_8.toString())
        val json2 = app.get("${WHVXAPI}/source?resourceId=${encodedUrl}&provider=astra", headers = headers).text
        val data2 = parseJson<AstraQuery>(json2) ?: return
        data2.stream.forEach {
            callback.invoke(
                ExtractorLink(
                    "Astra",
                    "Astra",
                    it.playlist,
                    "",
                    Qualities.Unknown.value,
                    INFER_TYPE
                )
            )    
        }
    }

    suspend fun invokeNova(
        title: String,
        imdb_id: String,
        tmdb_id: Int,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val query = if (season != null && episode != null) {
            """{"title":"$title","releaseYear":$year,"tmdbId":"$tmdb_id","imdbId":"$imdb_id","type":"show","season":"$season","episode":"$episode"}"""
        } else {
            """{"title":"$title","releaseYear":$year,"tmdbId":"$tmdb_id","imdbId":"$imdb_id","type":"movie","season":"","episode":""}"""
        }
        val headers = mapOf(
            "Origin" to "https://www.vidbinge.com",
        )

        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val json = app.get("${WHVXAPI}/search?query=${encodedQuery}&provider=nova", headers = headers).text
        val data = parseJson<WHVX>(json) ?: return
        val encodedUrl = URLEncoder.encode(data.url, StandardCharsets.UTF_8.toString())
        val json2 = app.get("${WHVXAPI}/source?resourceId=${encodedUrl}&provider=nova", headers = headers).text
        val data2 = parseJson<NovaVideoData>(json2) ?: return
        for (stream in data2.stream) {
            for ((quality, details) in stream.qualities) {
                callback.invoke(
                    ExtractorLink(
                        "Nova",
                        "Nova",
                        details.url,
                        "",
                        getQualityFromName(quality),
                        INFER_TYPE,
                    )
                )
            }
        }

        for (stream in data2.stream) {
            for (caption in stream.captions) {
                subtitleCallback.invoke(
                    SubtitleFile(
                        caption.language,
                        caption.url
                    )
                )
            }
        }
    }

    suspend fun invokeAutoembed(
        id: Int,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null && episode != null) "${AutoembedAPI}/embed/player.php?id=${id}&s=${season}&e=${episode}" else "${AutoembedAPI}/embed/player.php?id=${id}"
        val document = app.get(url).document
        val regex = Regex("""(?:"title":\s*"([^"]*)",\s*"file":\s*"([^"]*)")""")
        val matches = regex.findAll(document.toString())

        matches.forEach { match ->
            val title = match.groups?.get(1)?.value ?: ""
            val file = match.groups?.get(2)?.value ?: ""
            callback.invoke(
                ExtractorLink(
                    "Autoembed[${title}]",
                    "Autoembed[${title}]",
                    file,
                    referer = "",
                    quality = Qualities.Unknown.value,
                    INFER_TYPE,
                )
            )
        }
    }

    suspend fun invokeWYZIESubs(
        id: String,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null && episode != null) "${WYZIESubsAPI}/search?id=${id}&season=${season}&episode=${episode}" else "${WYZIESubsAPI}/search?id=${id}" 
        val json = app.get(url).text
        val data = parseJson<ArrayList<WYZIESubtitle>>(json)
        data.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.display,
                    it.url
                )
            )
        }
    }

    suspend fun invokeWHVXSubs(
        id: String,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
    ) {
        val url = if(season != null && episode != null) "${WHVXSubsAPI}/search?id=${id}&season=${season}&episode=${episode}" else "${WHVXSubsAPI}/search?id=${id}" 
        val json = app.get(url).text
        val data = parseJson<ArrayList<WHVXSubtitle>>(json)
        data.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.languageName,
                    it.url
                )
            )
        }
    }

    suspend fun invokeW4U(
        title: String,
        year: Int? = null,
        id: String,
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
        if(id == imdbId) {
            if(season != null && episode != null) {
                doc.select("a.my-button").mapNotNull {
                    val title = it.parent()?.parent()?.previousElementSibling()?.text()?: ""
                    val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                    val quality = qualityRegex.find(title) ?. groupValues ?. get(1) ?: ""
                    val realSeason = Regex("""(?:Season |S)(\d+)""").find(title) ?. groupValues ?. get(1) ?. toIntOrNull() ?: 300
                    if(season == realSeason) {
                        val doc2 = app.get(it.attr("href")).document
                        val h3 = doc2.select("h3:matches((?i)(episode))").get(episode-1)
                        var source = h3.nextElementSibling().selectFirst("a")?.attr("href") ?: ""
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

    suspend fun invokeDramaCool(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val json = if(season == null && episode == null) { 
            var episodeSlug = "$title episode 1".createSlug()
            val url = "${myConsumetAPI}/movies/dramacool/watch?episodeId=${episodeSlug}" 
            val res = app.get(url).text
            if(res.contains("Media Not found")) {
                val newEpisodeSlug = "$title $year episode 1".createSlug()
                val newUrl = "$myConsumetAPI/movies/dramacool/watch?episodeId=${newEpisodeSlug}"
                app.get(newUrl).text
            }
            else {
                res
            }
        }
        else {
            val seasonText = if(season == 1) "" else "season $season"
            val episodeSlug = "$title $seasonText episode $episode".createSlug()
            val url =  "${myConsumetAPI}/movies/dramacool/watch?episodeId=${episodeSlug}"
            val res = app.get(url).text
            if(res.contains("Media Not found")) {
                val newEpisodeSlug = "$title $seasonText $year episode $episode".createSlug()
                val newUrl = "$myConsumetAPI/movies/dramacool/watch?episodeId=${newEpisodeSlug}"
                app.get(newUrl).text
            }
            else {
                res
            }
        }

        val data = parseJson<ConsumetSources>(json)
        data.sources?.forEach {
            callback.invoke(
                ExtractorLink(
                    "DramaCool",
                    "DramaCool",
                    it.url,
                    referer = "",
                    quality = Qualities.P1080.value,
                    isM3u8 = true
                )
            )
        }
        
        data.subtitles?.forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    it.lang,
                    it.url
                )
            )
        }
    }

    suspend fun invokePrimeVideo(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val cookie = NFBypass(netflixAPI)
        val cookies = mapOf(
            "t_hash_t" to cookie,
            "ott" to "pv",
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/pv/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals(title.trim(), ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/pv/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                val episodeId =
                    app.get(
                        "$netflixAPI/pv/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/"
                    ).parsedSafe<NetflixResponse>()?.episodes?.find { it.ep == "E$episode" }?.id
                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/pv/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                ExtractorLink(
                    "PrimeVideo",
                    "PrimeVideo",
                    "$netflixAPI/${it.file}",
                    "$netflixAPI/",
                    getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in")),
                    INFER_TYPE,
                    headers = mapOf("Cookie" to "hd=on")
                )
            )
        }
    }
    
    suspend fun invokeNetflix(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val cookie = NFBypass(netflixAPI)
        val cookies = mapOf(
            "t_hash_t" to cookie,
            "hd" to "on"
        )
        val headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        val url = "$netflixAPI/search.php?s=$title&t=${APIHolder.unixTime}"
        val data = app.get(url, headers = headers, cookies = cookies).parsedSafe<NfSearchData>()
        val netflixId = data ?.searchResult ?.firstOrNull { it.t.equals(title.trim(), ignoreCase = true) }?.id

        val (nfTitle, id) = app.get(
            "$netflixAPI/post.php?id=${netflixId ?: return}&t=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).parsedSafe<NetflixResponse>().let { media ->
            if (season == null && year.toString() == media?.year.toString()) {
                media?.title to netflixId
            } else if(year.toString() == media?.year.toString()) {
                val seasonId = media?.season?.find { it.s == "$season" }?.id
                val episodeId =
                    app.get(
                        "$netflixAPI/episodes.php?s=${seasonId}&series=$netflixId&t=${APIHolder.unixTime}",
                        headers = headers,
                        cookies = cookies,
                        referer = "$netflixAPI/"
                    ).parsedSafe<NetflixResponse>()?.episodes?.find { it.ep == "E$episode" }?.id
                media?.title to episodeId
            }
            else {
                null to null
            }
        }

        app.get(
            "$netflixAPI/playlist.php?id=${id ?: return}&t=${nfTitle ?: return}&tm=${APIHolder.unixTime}",
            headers = headers,
            cookies = cookies,
            referer = "$netflixAPI/"
        ).text.let {
            tryParseJson<ArrayList<NetflixResponse>>(it)
        }?.firstOrNull()?.sources?.map {
            callback.invoke(
                ExtractorLink(
                    "Netflix",
                    "Netflix",
                    "$netflixAPI/${it.file}",
                    "$netflixAPI/",
                    getQualityFromName(it.file?.substringAfter("q=")?.substringBefore("&in")),
                    INFER_TYPE,
                    headers = mapOf("Cookie" to "hd=on")
                )
            )
        }
    }

    suspend fun NFBypass(mainUrl : String): String {
        val document = app.get("$mainUrl/home").document
        val addhash = document.selectFirst("body").attr("data-addhash").toString()
        val verify = app.get("https://userverify.netmirror.app/verify?hash=${addhash}&t=${APIHolder.unixTime}") //just make request to verify
        val requestBody = FormBody.Builder().add("verify", addhash).build()
        return app.post("$mainUrl/verify2.php", requestBody = requestBody).cookies["t_hash_t"].toString()
    }

    suspend fun invokeVadaPav(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val mirrors = listOf(
            "https://vadapav.mov",
            "https://dl1.vadapav.mov",
            "https://dl2.vadapav.mov",
            "https://dl3.vadapav.mov",
        )

        val url = if(season != null && episode != null) "$VadapavAPI/s/$title" else "$VadapavAPI/s/$title ($year)"
        val document = app.get(url).document
        val result = document.selectFirst("div.directory > ul > li > div > a")
        val text = result?.text()?.trim().toString()
        val href = VadapavAPI + (result?.attr("href") ?: return)
        if(season != null && episode != null && title.equals(text, true)) {
            val doc = app.get(href).document
            val filteredLink = doc.select("div.directory > ul > li > div > a.directory-entry").firstOrNull { aTag ->
                val seasonFromText = Regex("""Season\s(\d{1,2})""").find(aTag.text())?.groupValues ?. get(1)
                seasonFromText ?.toInt() == season
            }
            val seasonLink = VadapavAPI + (filteredLink ?. attr("href") ?: return)
            val seasonDoc = app.get(seasonLink).document
            val filteredLinks = seasonDoc.select("div.directory > ul > li > div > a.file-entry")
                .filter { element ->
                    val episodeFromText = Regex("""E(\d{1,3})""").find(element.text())?.groupValues?.get(1)
                    episodeFromText?.toIntOrNull() ?: return@filter false
                    episodeFromText.toInt() == episode
                }
            
            filteredLinks.forEach {
                if(it.text().contains(".mkv", true) || it.text().contains(".mp4", true)) {
                    val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                    val quality = qualityRegex.find(it.text()) ?. groupValues ?. get(1) ?: ""
                    for((index, mirror) in mirrors.withIndex()) {
                        callback.invoke(
                            ExtractorLink(
                                "[VadaPav" + " ${index+1}]",
                                "[VadaPav" + " ${index+1}] ${it.text()}",
                                mirror + it.attr("href"),
                                referer = "",
                                quality = getIndexQuality(quality),
                            )
                        )
                    }
                }
            }
        }
        else if(season == null) {
            val doc = app.get(href).document
            doc.select("div.directory > ul > li > div > a.file-entry:matches((?i)(.mkv|.mp4))").forEach {
                val qualityRegex = """(1080p|720p|480p|2160p|4K|[0-9]*0p)""".toRegex(RegexOption.IGNORE_CASE)
                val quality = qualityRegex.find(it.text()) ?. groupValues ?. get(1) ?: ""
                for((index, mirror) in mirrors.withIndex()) {
                    callback.invoke(
                        ExtractorLink(
                            "[VadaPav" + " ${index+1}]",
                            "[VadaPav" + " ${index+1}] ${it.text()}",
                            mirror + it.attr("href"),
                            referer = "",
                            quality = getIndexQuality(quality),
                        )
                    )
                }
            }
        }
        else {
            //Nothing
        }
    }


    suspend fun invokeFull4Movies(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val url = if(season == null) "$Full4MoviesAPI/?s=$title $year" else "$Full4MoviesAPI/?s=$title season $season $year"
        val document = app.get(url).document
        val text = document.selectFirst("div.content > h2 > a")?.text().toString()
        val href = document.selectFirst("div.content > h2 > a")?.attr("href").toString()
        if (
            text.contains(title, true) &&
            year.let { text.contains("$it") } == true &&
            (season == null || season.let { text.contains("Season $it", true) } == true)
        ) {
            val doc2 = app.get(href).document
            val link = if(season == null) {
                Regex("""<a\s+class="myButton"\s+href="([^"]+)".*?>Watch Online 1<\/a>""").find(doc2.html())?.groupValues?.get(1) ?: ""

            } else {
                val urls = Regex("""<a[^>]*href="([^"]*)"[^>]*>(?:WCH|Watch)<\/a>""").findAll(doc2.html())
                urls.elementAtOrNull(episode?.minus(1) ?: return)?.groupValues?.get(1) ?: ""
            }
            if(link.contains("4links.")) {
                val doc = app.get(fixUrl(link)).document
                val source = doc.selectFirst("iframe").attr("src") ?: ""
                loadSourceNameExtractor("Full4Movies",source, referer = link, subtitleCallback, callback)
            }
            else {
                loadSourceNameExtractor("Full4Movies",link, referer = href, subtitleCallback, callback)
            }
        }

    }

    suspend fun invokeRogmovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeWpredis(
            title,
            year,
            season,
            episode,
            subtitleCallback,
            callback,
            rogMoviesAPI
        )
    }
    suspend fun invokeVegamovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeWpredis(
            title,
            year,
            season,
            episode,
            subtitleCallback,
            callback,
            vegaMoviesAPI
        )
    }

    private suspend fun invokeWpredis(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        val cfInterceptor = CloudflareKiller()
        val fixtitle = title?.substringBefore("-")?.substringBefore(":")?.replace("&", " ")
        val url = if (season == null) {
            "$api/search/$fixtitle $year"
        } else {
            "$api/search/$fixtitle season $season"
        }
        val domain= api.substringAfter("//").substringBefore(".")
        app.get(url, interceptor = cfInterceptor).document.select("#main-content article")
            .filter { element ->
                element.text().contains(
                    fixtitle.toString(), true
                )
            }
            .amap {
                val hrefpattern =
                    Regex("""(?i)<a\s+href="([^"]+)"[^>]*?>[^<]*?\b($fixtitle)\b[^<]*?""").find(
                        it.toString()
                    )?.groupValues?.get(1)
                if (hrefpattern!=null) {
                    val res = hrefpattern.let { app.get(it).document }
                    val hTag = if (season == null) "h5" else "h3,h5"
                    val aTag =
                        if (season == null) "Download Now" else "V-Cloud,Download Now,G-Direct,Episode Links"
                    val sTag = if (season == null) "" else "(Season $season|S$seasonSlug)"
                    val entries =
                        res.select("div.entry-content > $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                            .filter { element ->
                                !element.text().contains("Series Info", true) &&
                                        !element.text().contains("Zip", true) &&
                                        !element.text().contains("[Complete]", true) &&
                                        !element.text().contains("480p, 720p, 1080p", true) &&
                                        !element.text().contains(domain, true) &&
                                element.text().matches("(?i).*($sTag).*".toRegex())
                            }
                    entries.amap { it ->
                        val tags =
                            """(?:480p|720p|1080p|2160p)(.*)""".toRegex().find(it.text())?.groupValues?.get(1)
                                ?.trim()
                        val tagList = aTag.split(",")
                        val href = it.nextElementSibling()?.select("a")?.filter { anchor ->
                            tagList.any { tag ->
                                anchor.text().contains(tag.trim(), true)
                            }
                        }?.map { anchor ->
                            anchor.attr("href")
                        } ?: emptyList()
                        val selector =
                            if (season == null) "p a:matches(V-Cloud|G-Direct)" else "h4:matches(0?$episode)"
                        if (href.isNotEmpty()) {
                            href.amap { url ->
                            if (season==null)
                            {
                                app.get(
                                    url, interceptor = wpRedisInterceptor
                                ).document.select("div.entry-content > $selector").map { sources ->
                                    val server = sources.attr("href")
                                    loadSourceNameExtractor(
                                        "VegaMovies",
                                        server,
                                        "$api/",
                                        subtitleCallback,
                                        callback,
                                    )
                                }
                            }
                            else
                            {
                                app.get(url, interceptor = wpRedisInterceptor).document.select("div.entry-content > $selector")
                                    .forEach { h4Element ->
                                        var sibling = h4Element.nextElementSibling()
                                        while (sibling != null && sibling.tagName() != "p") {
                                            sibling = sibling.nextElementSibling()
                                        }
                                        while (sibling != null && sibling.tagName() == "p") {
                                            sibling.select("a:matches(V-Cloud|G-Direct)").forEach { sources ->
                                                val server = sources.attr("href")
                                                loadSourceNameExtractor(
                                                    "VegaMovies",
                                                    server,
                                                    "$api/",
                                                    subtitleCallback,
                                                    callback,
                                                )
                                            }
                                            sibling = sibling.nextElementSibling()
                                        }
                                    }
                             }
                            }
                        }
                    }
                }
            }
    }

    suspend fun invokeMoviesdrive(
        title: String? = null,
        season: Int? = null,
        episode: Int? = null,
        year: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val fixTitle = title?.substringBefore("-")?.replace(":", " ")?.replace("&", " ")
        val searchtitle = title?.substringBefore("-").createSlug()
        val url = if (season == null) {
            "$MovieDrive_API/search/$fixTitle $year"
        } else {
            "$MovieDrive_API/search/$fixTitle"
        }
        val res1 =
            app.get(url, interceptor = wpRedisInterceptor).document.select("figure")
                .toString()
        val hrefpattern =
            Regex("""(?i)<a\s+href="([^"]*\b$searchtitle\b[^"]*)"""").find(res1)?.groupValues?.get(1)
                ?: ""
        val document = app.get(hrefpattern).document
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
                    val fEp = doc.selectFirst("h5:matches((?i)$sep)")?.toString()
                    if (fEp.isNullOrEmpty()) {
                        val furl = doc.select("h5 a:contains(HubCloud)").attr("href")
                        loadSourceNameExtractor("MoviesDrive",furl, "", subtitleCallback, callback)
                    } else
                        doc.selectFirst("h5:matches((?i)$sep)")?.let { epElement ->
                            val linklist = mutableListOf<String>()
                            val firstHubCloudH5 = epElement.nextElementSibling()
                            val secondHubCloudH5 = firstHubCloudH5?.nextElementSibling()
                            val firstLink = secondHubCloudH5?.selectFirst("a")?.attr("href")
                            val secondLink = secondHubCloudH5?.selectFirst("a")?.attr("href")
                            if (firstLink != null) linklist.add(firstLink)
                            if (secondLink != null) linklist.add(secondLink)
                            linklist.forEach { url ->
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

    suspend fun invokeUhdmovies(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        callback: (ExtractorLink) -> Unit,
        subtitleCallback: (SubtitleFile) -> Unit
    ) {
        val fixTitle = title?.replace("-", " ")?.replace(":", " ")
        val searchtitle = fixTitle.createSlug()
        val (seasonSlug, episodeSlug) = getEpisodeSlug(season, episode)
        app.get("$uhdmoviesAPI/search/$fixTitle $year").document.select("#content article").map {
            val hrefpattern =
                Regex("""(?i)<a\s+href="([^"]*?\b$searchtitle\b[^"]*?\b$year\b[^"]*?)"[^>]*>""").find(
                    it.toString()
                )?.groupValues?.get(1)
            val detailDoc = hrefpattern?.let { app.get(it).document }
            val iSelector = if (season == null) {
                "div.entry-content p:has(:matches($year))"
            } else {
                "div.entry-content p:has(:matches((?i)(?:S\\s*$seasonSlug|Season\\s*$seasonSlug)))"
            }
            val iframeList = detailDoc!!.select(iSelector).mapNotNull {
                if (season == null) {
                    it.text() to it.nextElementSibling()?.select("a")?.attr("href")
                } else {
                    it.text() to it.nextElementSibling()?.select("a")?.find { child ->
                        child.select("span").text().equals("Episode $episode", true)
                    }?.attr("href")
                }
            }.filter { it.first.contains(Regex("(2160p)|(1080p)")) }
                .filter { element -> !element.toString().contains("Download", true) }
            iframeList.amap { (quality, link) ->
                val driveLink = bypassHrefli(link ?: "") ?: ""
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
            Regex("""(?i)<article[^>]*>\s*<a\s+href="([^"]*$searchtitle[^"]*)"""").find(res1)?.groupValues?.get(
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
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        invokeModflix(
            title,
            year,
            season,
            episode,
            subtitleCallback,
            callback,
            MoviesmodAPI
        )
    }

    suspend fun invokeModflix(
        title: String? = null,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
        api: String
    ) {
        val fixTitle = title?.replace("-", " ")?.replace(":", " ")?.replace("&", " ")
        var url = ""
        val searchtitle =
            title?.replace("-", " ")?.replace(":", " ")?.replace("&", " ").createSlug()
        if (season == null) {
            url = "$api/search/$fixTitle"
        } else {
            url = "$api/search/$fixTitle $season"
        }
        var res1 =
            app.get(url, interceptor = wpRedisInterceptor).document.select("#content_box article")
                .toString()
        val hrefpattern =
            Regex("""(?i)<article[^>]*>\s*<a\s+href="([^"]*$searchtitle[^"]*)"""").find(res1)?.groupValues?.get(1)
        val hTag = if (season == null) "h4" else "h3"
        val aTag = if (season == null) "Download" else "Episode"
        val sTag = if (season == null) "" else "(S0$season|Season $season)"
        val res = app.get(
            hrefpattern ?: return,
            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0"),
            interceptor = wpRedisInterceptor
        ).document
        val entries =
            res.select("div.thecontent $hTag:matches((?i)$sTag.*(720p|1080p|2160p))")
                .filter { element ->
                    !element.text().contains("MoviesMod", true) && !element.text()
                        .contains("1080p", true) || !element.text().contains("720p", true)
                }
        entries.amap { it ->
            val tags =
                """(?:480p|720p|1080p|2160p)(.*)""".toRegex().find(it.text())?.groupValues?.get(1)
                    ?.trim()
            val quality =
                """480p|720p|1080p|2160p""".toRegex().find(it.text())?.groupValues?.get(0)
                    ?.trim()
            var href =
                it.nextElementSibling()?.select("a:contains($aTag)")?.attr("href")
                    ?.substringAfter("=") ?: ""
            href = base64Decode(href)
            val selector =
                if (season == null) "p a.maxbutton" else "h3 a:matches(Episode $episode)"
            if (href.isNotEmpty())
            app.get(
                href,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:125.0) Gecko/20100101 Firefox/125.0")
            ).document.selectFirst(selector)?.let {
                val link = it.attr("href")
                if (link.contains("driveseed.org"))
                {
                    val file=app.get(link).toString().substringAfter("replace(\"").substringBefore("\")")
                    val domain= getBaseUrl(link)
                    val server="$domain$file"
                    loadSourceNameExtractor("Moviesmod", server, "", subtitleCallback, callback)
                }
                val server = bypassHrefli(link) ?: ""
                if (server.isNotEmpty()) {
                    loadSourceNameExtractor("Moviesmod", server, "", subtitleCallback, callback)
                }
            }
        }
    }
    suspend fun invokeAutoembedDrama(
        title: String,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ) {
        val url = if(season == null) {
            val episodeSlug = "$title $year episode 1".createSlug()
            "$AutoembedDramaAPI/embed/${episodeSlug}"
        }
        else {
            val seasonText = if(season == 1) "" else "season $season"
            val episodeSlug = "$title $seasonText $year episode $episode".createSlug()
            "$AutoembedDramaAPI/embed/${episodeSlug}"
        }

        val document = app.get(url).document
        val regex = Regex("""file:\s*"(https?:\/\/[^"]+)"""")
        val link = regex.find(document.toString())?.groupValues?.get(1) ?: ""
        callback.invoke(
            ExtractorLink(
                "AutoembedDrama",
                "AutoembedDrama",
                link,
                "",
                Qualities.P1080.value,
                isM3u8 = true,
            )
        )
    }
}
