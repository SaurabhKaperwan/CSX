package com.megix

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

//Allmovieland
 data class AllMovielandPlaylist(
    @JsonProperty("file") val file: String? = null,
    @JsonProperty("key") val key: String? = null,
    @JsonProperty("href") val href: String? = null,
)

data class AllMovielandServer(
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("file") val file: String? = null,
    @JsonProperty("folder")
    val folder: ArrayList<AllMovielandSeasonFolder>? = arrayListOf(),
) {
    data class AllMovielandSeasonFolder(
        @JsonProperty("episode") val episode: String? = null,
        @JsonProperty("id") val id: String? = null,
        @JsonProperty("folder")
        val folder: ArrayList<AllMovielandEpisodeFolder>? = arrayListOf(),
    ) {
        data class AllMovielandEpisodeFolder(
            @JsonProperty("title") val title: String? = null,
            @JsonProperty("id") val id: String? = null,
            @JsonProperty("file") val file: String? = null,
        )
    }
}

//Tom
data class TomResponse (
  var videoSource    : String,
  var subtitles      : ArrayList<TomSubtitles> = arrayListOf(),
)

data class TomSubtitles (
  var file    : String,
  var label   : String
)

//TMDB to mal
data class AniMedia(
    @JsonProperty("id") var id: Int? = null,
    @JsonProperty("idMal") var idMal: Int? = null
)

data class AniPage(@JsonProperty("media") var media: java.util.ArrayList<AniMedia> = arrayListOf())

data class AniData(@JsonProperty("Page") var Page: AniPage? = AniPage())

data class AniSearch(@JsonProperty("data") var data: AniData? = AniData())

data class AniIds(var id: Int? = null, var idMal: Int? = null)

//Multiautoembed
data class MultiAutoembedResponse (
  var audioTracks : ArrayList<MultiembedAudioTracks> = arrayListOf()
)

data class MultiembedAudioTracks (
  var label : String,
  var file  : String
)

//Cinemaluxe
data class CinemaluxeRedirectUrl(
    val redirectUrl: String
)

//NF
data class NFVerifyUrl(
    val url: String
)

data class NfSearchData(
    val head: String,
    val searchResult: List<NfSearchResult>,
    val type: Int
)

data class NfSearchResult(
    val id: String,
    val t: String
)

data class NetflixSources(
    @JsonProperty("file") val file: String? = null,
    @JsonProperty("label") val label: String? = null,
)

data class NetflixEpisodes(
    @JsonProperty("id") val id: String? = null,
    @JsonProperty("t") val t: String? = null,
    @JsonProperty("s") val s: String? = null,
    @JsonProperty("ep") val ep: String? = null,
)

data class NetflixSeason(
    @JsonProperty("s") val s: String? = null,
    @JsonProperty("id") val id: String? = null,
)

data class NetflixResponse(
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("year") val year : String? = null,
    @JsonProperty("season") val season: ArrayList<NetflixSeason>? = arrayListOf(),
    @JsonProperty("episodes") val episodes: ArrayList<NetflixEpisodes>? = arrayListOf(),
    @JsonProperty("sources") val sources: ArrayList<NetflixSources>? = arrayListOf(),
    @JsonProperty("nextPageShow") val nextPageShow: Int? = null,
)

//Dramacool
data class Dramacool (
  var streams : ArrayList<DramacoolStreams> = arrayListOf()
)

data class DramacoolSubtitles (
  var lang : String,
  var url  : String
)

data class DramacoolStreams (
  var subtitles : ArrayList<DramacoolSubtitles> = arrayListOf(),
  var title     : String,
  var url       : String
)

//Anichi

data class Anichi(
    val data: AnichiData,
)

data class AnichiData(
    val shows: AnichiShows,
)

data class AnichiShows(
    val pageInfo: PageInfo,
    val edges: List<Edge>,
)

data class PageInfo(
    val total: Long,
)

data class Edge(
    @JsonProperty("_id")
    val id: String,
    val name: String,
    val englishName: String,
    val nativeName: String,
)

//Anichi Ep Parser

data class AnichiEP(
    val data: AnichiEPData,
)

data class AnichiEPData(
    val episode: AnichiEpisode,
)

data class AnichiEpisode(
    val sourceUrls: List<SourceUrl>,
)

data class SourceUrl(
    val sourceUrl: String,
    val sourceName: String,
    val downloads: AnichiDownloads?,
)

data class AnichiDownloads(
    val sourceName: String,
    val downloadUrl: String,
)

//Anichi Download URL Parser

data class AnichiDownload(
    val links: List<AnichiDownloadLink>,
)

data class AnichiDownloadLink(
    val link: String,
    val hls: Boolean,
    val mp4: Boolean?,
    val resolutionStr: String,
    val priority: Long,
    val src: String?,
)



data class TvStreamsResponse(
    val streams: List<TvStream>
)

data class TvStream(
    val title: String?,
    val name: String?,
    val description: String?,
    val url: String,
    val behaviorHints: TvBehaviorHints
)

data class TvBehaviorHints(
    val proxyHeaders: TvProxyHeaders
)

data class TvProxyHeaders(
    val request: Map<String, String>? = null,
)

data class AnimiaResponse(
    val server1embedLink: String? = null,
    val server2embedLink: String? = null,
    val server3embedLink: String? = null,
)

data class HiAnime(
    val subOrDub: String,
    val episodes: List<HiAnimeEpisode>
)

data class HiAnimeEpisode(
    val id: String,
    val number: Int,
    val isDubbed: Boolean,
)
data class HiAnimeMedia(
    val sources: List<HiAnimeSource>,
    val subtitles: List<HiAnimeSubtitle>
)
data class HiAnimeSource(
    val url: String,
    val isM3U8: Boolean,
    val type: String,
)

data class HiAnimeSubtitle(
    val url: String,
    val lang: String
)

data class TorrentioResponse(val streams: List<TorrentioStream>)

data class TorrentioStream(
    val name: String?,
    val title: String?,
    val infoHash: String?,
    val fileIdx: Int?,
)

data class StreamifyResponse(
    val streams: List<Streamify>
)

data class Streamify(
    val name: String,
    val type: String,
    val url: String,
    val title: String
)

data class JikanExternal(
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("url") val url: String? = null,
)

data class JikanData(
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("external") val external: ArrayList<JikanExternal>? = arrayListOf(),
    val season: String,
)

data class JikanResponse(
    @JsonProperty("data") val data: JikanData? = null,
)


data class ResponseHash(
    @JsonProperty("embed_url") val embed_url: String,
    @JsonProperty("key") val key: String? = null,
    @JsonProperty("type") val type: String? = null,
)

data class animepahe(
    val total: Long,
    @JsonProperty("per_page")
    val perPage: Long,
    @JsonProperty("current_page")
    val currentPage: Long,
    @JsonProperty("last_page")
    val lastPage: Long,
    @JsonProperty("next_page_url")
    val nextPageUrl: Any?,
    @JsonProperty("prev_page_url")
    val prevPageUrl: Any?,
    val from: Long,
    val to: Long,
    val data: List<Daum>,
)

data class Daum(
    val id: Long,
    @JsonProperty("anime_id")
    val animeId: Long,
    val episode: Int,
    val episode2: Long,
    val edition: String,
    val title: String,
    val snapshot: String,
    val disc: String,
    val audio: String,
    val duration: String,
    val session: String,
    val filler: Long,
    @JsonProperty("created_at")
    val createdAt: String,
)

data class MALSyncSites(
    @JsonProperty("Gogoanime") val Gogoanime: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
    @JsonProperty("Zoro") val zoro: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
    @JsonProperty("9anime") val nineAnime: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
    @JsonProperty("animepahe") val animepahe: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
)

data class HianimeResponses(
    @JsonProperty("html") val html: String? = null,
    @JsonProperty("link") val link: String? = null,
)


data class MALSyncResponses(
    @JsonProperty("Sites") val sites: MALSyncSites? = null,
)

// data class RarResponseData(
//     val data: List<RarItem>?
// )

// data class RarItem(
//     val id: Int,
//     val name: String,
//     val second_name: String,
//     val image: String,
//     val url: String,
//     val type: String
// )

data class WHVXToken(
    val token : String,
)

data class OrionStreamData(
    val stream: List<OrionStream>
)

data class OrionStream(
    val id: String,
    val type: String,
    val playlist: String,
    val flags: List<String>,
    val captions: List<OrionCaption>
)

data class OrionCaption(
    val id: String,
    val url: String,
    val type: String,
    val hasCorsRestrictions: Boolean,
    val language: String
)

data class AstraQuery(
    val stream: List<AstraStream>
)

data class AstraStream(
    val id: String,
    val type: String,
    val playlist: String,
)

data class NovaStream(
    val id: String,
    val qualities: Map<String, NovaQuality>,
    val captions: List<NovaCaption>
)

data class NovaQuality(
    val type: String,
    val url: String
)

data class NovaCaption(
    val id: String,
    val url: String,
    val type: String,
    val hasCorsRestrictions: Boolean,
    val language: String
)

data class NovaVideoData(
    val stream: List<NovaStream>
)

data class WHVX(
    val embedId: String,
    val url: String,
)


//Subtitles
data class WHVXSubtitle(
    val url: String,
    val languageName: String?,
    val display: String?,
)

//Consumet

data class ConsumetSearch (
    var results     : ArrayList<ConsumetResults> = arrayListOf()
)

data class ConsumetResults (
    var id          : String,
    var title       : String,
    var type        : String
)

data class ConsumetInfo (
    var id          : String,
    var episodes    : ArrayList<ConsumetEpisodes> = arrayListOf()
)


data class ConsumetEpisodes (
    var id     : String,
    var number : Int? = null,
    var season : Int? = null,
)

data class ConsumetWatch (
    var headers   : ConsumetHeaders      = ConsumetHeaders(),
    var sources   : ArrayList<ConsumetSources>   = arrayListOf(),
    var subtitles : ArrayList<ConsumetSubtitles> = arrayListOf()
)

data class ConsumetHeaders (
  var Referer : String? = null,
)

data class ConsumetSources (
    var url     : String,
    var quality : String,
    var isM3U8  : Boolean
)

data class ConsumetSubtitles (
    var url  : String,
    var lang : String
)

data class Player4uLinkData(
    val name: String,
    val url: String,
)

data class TBPResponse(
    val streams: List<TBPStream>,
    val cacheMaxAge: Long,
    val staleRevalidate: Long,
    val staleError: Long,
)

data class TBPStream(
    val name: String,
    val title: String,
    val infoHash: String,
    val tag: String,
)

data class VidjoyResponse(
    val headers: Headers,
    val proxy: Boolean,
    val servers: List<Any?>,
    val tracks: List<Track>,
    val provider: String,
    val needConfig: Boolean,
    val url: List<Url>,
)

data class Headers(
    @JsonProperty("Referer")
    val referer: String,
)

data class Track(
    val lang: String,
    val code: String,
    val url: String,
    val type: String,
)

data class Url(
    val lang: String,
    val type: String,
    val link: String,
    val resulation: String,
)
