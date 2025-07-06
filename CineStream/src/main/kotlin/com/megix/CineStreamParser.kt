package com.megix

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

//Hianime
data class HianimeResponses(
    @JsonProperty("html") val html: String? = null,
    @JsonProperty("link") val link: String? = null,
)

data class HianimeStreamResponse(
    val streams: List<HianimeStream>,
    val tracks: List<HianimeTrack>,
)

data class HianimeStream(
    val url: String,
    val type: String,
)

data class HianimeTrack(
    val file: String,
    val label: String?,
    val kind: String,
)

//Cinemaluxe
data class CinemaluxeItem(
    val token: String,
    val id: Long,
    val time: Long,
    val post: String,
    val redirect: String,
    val cacha: String,
    val new: Boolean,
    val link: String
)

//Stremio Subtitles

data class StremioSubtitleResponse(
    val subtitles: List<StremioSubtitle> = emptyList()
)

data class StremioSubtitle(
    val lang_code: String? = null,
    val lang: String? = null,
    val title: String? = null,
    val url: String? = null,
)

//Madplay
data class MadplayServerInfo(
    val tvurl: String,
    val movieurl: String,
    val server: String
)

//Primebox
data class Primebox(
    @JsonProperty("available_qualities") val availableQualities: List<String> = emptyList(),
    @JsonProperty("has_subtitles") val hasSubtitles: Boolean = false,
    @JsonProperty("status") val status: String? = null,
    @JsonProperty("streams") val streams: PrimeboxStreams? = null,
    @JsonProperty("subtitles") val subtitles: List<PrimeboxSubtitles> = emptyList(),
    @JsonProperty("title") val title: String? = null
)

data class PrimeboxStreams(
    @JsonProperty("360P") val quality360P: String? = null,
    @JsonProperty("720P") val quality720P: String? = null,
    @JsonProperty("1080P") val quality1080P: String? = null
)

data class PrimeboxSubtitles(
    @JsonProperty("file") val file: String? = null,
    @JsonProperty("label") val label: String? = null
)

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


//NF
data class NFVerifyUrl(
    val nfverifyurl: String
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

//StreamAsia

data class StreamAsiaSearch(
    var metas : ArrayList<StreamAsiaMetas> = arrayListOf()
)

data class StreamAsiaMetas (
  var id          : String? = null,
  var name        : String? = null,
  var type        : String? = null,
)

data class  StreamAsiaInfo(
  var meta : StreamAsiaMeta = StreamAsiaMeta()
)

data class StreamAsiaMeta (
    var videos      : ArrayList<StreamAsiaVideos> = arrayListOf()
)

data class StreamAsiaVideos (
  var episode   : Int?    = null,
  var id        : String? = null,
)

data class StreamAsiaStreams (
  var streams : ArrayList<StreamAsiaStream> = arrayListOf()
)

data class StreamAsiaStream (
  var title     : String? = null,
  var url       : String? = null
)

data class StreamAsiaSubtitles (
  var subtitles : ArrayList<StreamAsiaSubtitle> = arrayListOf()
)

data class StreamAsiaSubtitle (
  var lang : String? = null,
  var url  : String? = null
)

//Anichi

data class AkIframe(
    @JsonProperty("idUrl") val idUrl: String? = null,
)

data class AnichiVideoApiResponse(@JsonProperty("links") val links: List<AnichiLinks>)

data class AnichiStream(
    @JsonProperty("format") val format: String? = null,
    @JsonProperty("audio_lang") val audio_lang: String? = null,
    @JsonProperty("hardsub_lang") val hardsub_lang: String? = null,
    @JsonProperty("url") val url: String? = null,
)

data class PortData(
    @JsonProperty("streams") val streams: ArrayList<AnichiStream>? = arrayListOf(),
)

data class AnichiSubtitles(
    @JsonProperty("lang") val lang: String?,
    @JsonProperty("label") val label: String?,
    @JsonProperty("src") val src: String?,
)

data class AnichiLinks(
    @JsonProperty("link") val link: String,
    @JsonProperty("hls") val hls: Boolean? = null,
    @JsonProperty("resolutionStr") val resolutionStr: String,
    @JsonProperty("src") val src: String? = null,
    @JsonProperty("headers") val headers: Headers? = null,
    @JsonProperty("portData") val portData: PortData? = null,
    @JsonProperty("subtitles") val subtitles: ArrayList<AnichiSubtitles>? = arrayListOf(),
)

data class Headers(
    @JsonProperty("Referer") val referer: String? = null,
    @JsonProperty("Origin") val origin: String? = null,
    @JsonProperty("user-agent") val userAgent: String? = null,
)


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

data class AnimiaResponse(
    val server1embedLink: String? = null,
    val server2embedLink: String? = null,
    val server3embedLink: String? = null,
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
    @JsonProperty("AniXL") val AniXL: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
    @JsonProperty("Zoro") val zoro: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
    @JsonProperty("animepahe") val animepahe: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
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

//Subtitles
data class WYZIESubtitle(
    val url: String,
    val language: String?,
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
    val headers: VidjoyHeaders,
    val proxy: Boolean,
    val servers: List<Any?>,
    val tracks: List<Track>,
    val provider: String,
    val needConfig: Boolean,
    val url: List<Url>,
)

data class VidjoyHeaders(
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
