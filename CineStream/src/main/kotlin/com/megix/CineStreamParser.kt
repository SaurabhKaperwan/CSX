package com.megix

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

//year => for movie : release year for movie : season 1 release year
//airedYear => for movie : release year for movie : episode release year
//imdbTitle, imdbSeason, imdbEpisode, imdbYear => for kitsu providers

data class AllLoadLinksData(
    val title: String? = null,
    val imdbId: String? = null,
    val tmdbId: Int? = null,
    val anilistId: Int? = null,
    val malId: Int? = null,
    val kitsuId: String? = null,
    val year: Int? = null,
    val airedYear: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val isAnime: Boolean = false,
    val isBollywood: Boolean = false,
    val isAsian: Boolean = false,
    val isCartoon: Boolean = false,
    val originalTitle: String? = null,
    val imdbTitle: String? = null,
    val imdbSeason : Int? = null,
    val imdbEpisode : Int? = null,
    val imdbYear : Int? = null,
)

//Kisskh
data class KisskhResults(
    @param:JsonProperty("id") val id: Int?,
    @param:JsonProperty("title") val title: String?,
)

data class KisskhDetail(
    @param:JsonProperty("episodes") val episodes: ArrayList<KisskhEpisodes>? = arrayListOf(),
)

data class KisskhEpisodes(
    @param:JsonProperty("id") val id: Int?,
    @param:JsonProperty("number") val number: Int?,
)

data class KisskhSources(
    @param:JsonProperty("Video") val video: String?,
    @param:JsonProperty("ThirdParty") val thirdParty: String?,
)

data class KisskhSubtitle(
    @param:JsonProperty("src") val src: String?,
    @param:JsonProperty("label") val label: String?,
)

//Anilist
data class AnimeInfo(
    val title: String?,
    val posterUrl: String?
)

//XDmovies
class XDMoviesSearchResponse: ArrayList<XDMoviesSearchResponse.SearchDataItem>() {
    data class SearchDataItem(
        val id: Int,
        val path: String,
        val title: String,
        val tmdb_id: Int,
        val type: String
    )
}

//Anizip
data class AnizipEpisode(
    @SerializedName("anidbEid") val anidbEid: Int?,
    @SerializedName("episode") val episode: String?,
)

data class Anizip(val episodes: Map<String, AnizipEpisode>?)

//Animetosho
data class Animetosho(
    val title: String?,
    @SerializedName("magnet_uri") val magnetUri: String?,
    val seeders: Int?,
    val leechers: Int?,
    @SerializedName("total_size") val totalSize: String?
)

//Vidlink
data class VidlinkResponse(
    @SerializedName("stream") val stream: VidlinkStream
)

data class VidlinkStream(
    @SerializedName("playlist") val playlist: String
)

data class TmdbDate(
    val today: String,
    val nextWeek: String,
    val lastWeekStart: String,
    val monthStart: String
)

//Tmdb
data class TmdbResponse(
    @SerializedName("meta") val meta: TmdbMeta?
)

data class TmdbMeta(
    @SerializedName("app_extras") val appExtras: TmdbAppExtras?
)

data class TmdbAppExtras(
    @SerializedName("cast") val cast: List<TmdbCastMember>?
)

data class TmdbCastMember(
    @SerializedName("name") val name: String?,
    @SerializedName("character") val character: String?,
    @SerializedName("photo") val photo: String?
)

//Primewire
data class PrimewireClass(
    val link: String,
    @param:JsonProperty("host_id")
    val hostId: Long,
    val host: String,
)

//Hianime
data class HianimeResponses(
    @param:JsonProperty("html") val html: String? = null,
    @param:JsonProperty("link") val link: String? = null,
)

data class HianimeStreamResponse(
    val sources: List<HianimeSources>,
    val tracks: List<HianimeTracks>,
)

data class HianimeSources(
    val url: String,
    val type: String,
)

data class HianimeTracks(
    val file: String,
    val label: String?,
    val kind: String,
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
    @param:JsonProperty("available_qualities") val availableQualities: List<String> = emptyList(),
    @param:JsonProperty("has_subtitles") val hasSubtitles: Boolean = false,
    @param:JsonProperty("status") val status: String? = null,
    @param:JsonProperty("streams") val streams: PrimeboxStreams? = null,
    @param:JsonProperty("subtitles") val subtitles: List<PrimeboxSubtitles> = emptyList(),
    @param:JsonProperty("title") val title: String? = null
)

data class PrimeboxStreams(
    @param:JsonProperty("360P") val quality360P: String? = null,
    @param:JsonProperty("720P") val quality720P: String? = null,
    @param:JsonProperty("1080P") val quality1080P: String? = null
)

data class PrimeboxSubtitles(
    @param:JsonProperty("file") val file: String? = null,
    @param:JsonProperty("label") val label: String? = null
)

//Allmovieland
 data class AllMovielandPlaylist(
    @param:JsonProperty("file") val file: String? = null,
    @param:JsonProperty("key") val key: String? = null,
    @param:JsonProperty("href") val href: String? = null,
)

data class AllMovielandServer(
    @param:JsonProperty("title") val title: String? = null,
    @param:JsonProperty("id") val id: String? = null,
    @param:JsonProperty("file") val file: String? = null,
    @param:JsonProperty("folder")
    val folder: ArrayList<AllMovielandSeasonFolder>? = arrayListOf(),
) {
    data class AllMovielandSeasonFolder(
        @param:JsonProperty("episode") val episode: String? = null,
        @param:JsonProperty("id") val id: String? = null,
        @param:JsonProperty("folder")
        val folder: ArrayList<AllMovielandEpisodeFolder>? = arrayListOf(),
    ) {
        data class AllMovielandEpisodeFolder(
            @param:JsonProperty("title") val title: String? = null,
            @param:JsonProperty("id") val id: String? = null,
            @param:JsonProperty("file") val file: String? = null,
        )
    }
}

//TMDB to mal
data class AniMedia(
    @param:JsonProperty("id") var id: Int? = null,
    @param:JsonProperty("idMal") var idMal: Int? = null
)

data class AniPage(@param:JsonProperty("media") var media: java.util.ArrayList<AniMedia> = arrayListOf())

data class AniData(@param:JsonProperty("Page") var Page: AniPage? = AniPage())

data class AniSearch(@param:JsonProperty("data") var data: AniData? = AniData())

data class AniIds(var id: Int? = null, var idMal: Int? = null)


//NF
data class NFVerifyUrl(
    val nfverifyurl: String
)

data class NfSearchData(
    val searchResult: List<NfSearchResult>,
)

data class NfSearchResult(
    val id: String,
    val t: String
)

data class NetflixSources(
    @param:JsonProperty("file") val file: String? = null,
    @param:JsonProperty("label") val label: String? = null,
)

data class NetflixEpisodes(
    @param:JsonProperty("id") val id: String? = null,
    @param:JsonProperty("t") val t: String? = null,
    @param:JsonProperty("s") val s: String? = null,
    @param:JsonProperty("ep") val ep: String? = null,
)

data class NetflixSeason(
    @param:JsonProperty("s") val s: String? = null,
    @param:JsonProperty("id") val id: String? = null,
)

data class NetflixResponse(
    @param:JsonProperty("title") val title: String? = null,
    @param:JsonProperty("year") val year : String? = null,
    @param:JsonProperty("season") val season: ArrayList<NetflixSeason>? = arrayListOf(),
    @param:JsonProperty("episodes") val episodes: ArrayList<NetflixEpisodes>? = arrayListOf(),
    @param:JsonProperty("sources") val sources: ArrayList<NetflixSources>? = arrayListOf(),
    @param:JsonProperty("nextPageShow") val nextPageShow: Int? = null,
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
    @param:JsonProperty("idUrl") val idUrl: String? = null,
)

data class AnichiVideoApiResponse(@param:JsonProperty("links") val links: List<AnichiLinks>)

data class AnichiStream(
    @param:JsonProperty("format") val format: String? = null,
    @param:JsonProperty("audio_lang") val audio_lang: String? = null,
    @param:JsonProperty("hardsub_lang") val hardsub_lang: String? = null,
    @param:JsonProperty("url") val url: String? = null,
)

data class PortData(
    @param:JsonProperty("streams") val streams: ArrayList<AnichiStream>? = arrayListOf(),
)

data class AnichiSubtitles(
    @param:JsonProperty("lang") val lang: String?,
    @param:JsonProperty("label") val label: String?,
    @param:JsonProperty("src") val src: String?,
)

data class AnichiLinks(
    @param:JsonProperty("link") val link: String,
    @param:JsonProperty("hls") val hls: Boolean? = null,
    @param:JsonProperty("resolutionStr") val resolutionStr: String,
    @param:JsonProperty("src") val src: String? = null,
    @param:JsonProperty("headers") val headers: Headers? = null,
    @param:JsonProperty("portData") val portData: PortData? = null,
    @param:JsonProperty("subtitles") val subtitles: ArrayList<AnichiSubtitles>? = arrayListOf(),
)

data class Headers(
    @param:JsonProperty("Referer") val referer: String? = null,
    @param:JsonProperty("Origin") val origin: String? = null,
    @param:JsonProperty("user-agent") val userAgent: String? = null,
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
    @param:JsonProperty("_id")
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
    val description: String?,
    val sources: List<String>? = null,
)

data class StreamifyResponse(
    var streams: List<Streamify>
)

data class StreamifySubs(
    var url  : String,
    var lang : String
)

data class Streamify(
    var name: String? = null,
    var type: String? = null,
    var url: String,
    var title: String? = null,
    var subtitles: List<StreamifySubs>? = null,
    @SerializedName("behaviorHints" ) var behaviorHints: StreamifyBehaviorHints? = StreamifyBehaviorHints()
)

data class StreamifyBehaviorHints(
    @SerializedName("proxyHeaders" ) var proxyHeaders: StreamifyProxyHeaders? = StreamifyProxyHeaders()
)

data class StreamifyProxyHeaders(
    @SerializedName("request" ) var request: StreamifyRequest? = StreamifyRequest()
)

data class StreamifyRequest(
    @SerializedName("Referer" ) var Referer: String? = null,
    @SerializedName("Origin"  ) var Origin  : String? = null,
    @SerializedName("User-Agent") var userAgent: String? = null
)

data class JikanExternal(
    @param:JsonProperty("name") val name: String? = null,
    @param:JsonProperty("url") val url: String? = null,
)

data class JikanData(
    @param:JsonProperty("title") val title: String? = null,
    @param:JsonProperty("external") val external: ArrayList<JikanExternal>? = arrayListOf(),
    val season: String,
)

data class JikanResponse(
    @param:JsonProperty("data") val data: JikanData? = null,
)


data class ResponseHash(
    @param:JsonProperty("embed_url") val embed_url: String,
    @param:JsonProperty("key") val key: String? = null,
    @param:JsonProperty("type") val type: String? = null,
)

data class animepahe(
    val total: Long,
    @param:JsonProperty("per_page")
    val perPage: Long,
    @param:JsonProperty("current_page")
    val currentPage: Long,
    @param:JsonProperty("last_page")
    val lastPage: Long,
    @param:JsonProperty("next_page_url")
    val nextPageUrl: Any?,
    @param:JsonProperty("prev_page_url")
    val prevPageUrl: Any?,
    val from: Long,
    val to: Long,
    val data: List<Daum>,
)

data class Daum(
    val id: Long,
    @param:JsonProperty("anime_id")
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
    @param:JsonProperty("created_at")
    val createdAt: String,
)

data class MALSyncSites(
    @param:JsonProperty("AniXL") val AniXL: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
    @param:JsonProperty("Zoro") val zoro: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
    @param:JsonProperty("animepahe") val animepahe: HashMap<String?, HashMap<String, String?>>? = hashMapOf(),
)

data class MALSyncResponses(
    @param:JsonProperty("title") val title: String? = null,
    @param:JsonProperty("Sites") val sites: MALSyncSites? = null,
)

//Subtitles
data class WYZIESubtitle(
    val url: String,
    val language: String?,
    val display: String?,
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

data class CinemaOsSecretKeyRequest(
    val tmdbId: String,
    val imdbId: String,
    val seasonId: String,
    val episodeId: String)


data class CinemaOSReponse(
    val data: CinemaOSReponseData,
    val encrypted: Boolean,
)

data class CinemaOSReponseData(
    val encrypted: String,
    val cin: String,
    val mao: String,
    val salt: String,
)

data class CinemaOsAuthResponse(
    val token: String,
    val expiresIn: Long,
)

typealias TripleOneMoviesServerList = List<TripleOneMoviesServer>;

data class TripleOneMoviesServer(
    val name: String,
    val description: String,
    val image: String,
    val data: String,
)

data class TripleOneMoviesStream(
    val noReferrer: Boolean,
    val url: String,
)


data class PrimeSrcServerList(
    val servers: List<PrimeSrcServer>,
)

data class PrimeSrcServer(
    val name: String,
    val key: String,
    @param:JsonProperty("file_size")
    val fileSize: String?,
    @param:JsonProperty("file_name")
    val fileName: String?,
)






