package com.megix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable

//year => for movie : release year for series : season 1 release year
//airedYear => for movie : release year for series : episode release year
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

//AIO
data class ExtractedMediaData(
    val cast: List<ActorData>?,
    val poster: String?,
    val background: String?,
    val logo: String?
)

//Enc-dec
data class EncDecResponse(
    @param:JsonProperty("result") val result: EncDecResult?
)

data class EncDecResult(
    @param:JsonProperty("servers") val servers: String?,
    @param:JsonProperty("stream") val stream: String?,
    @param:JsonProperty("token") val token: String?,
)

data class EncDecStreamResponse(
    @param:JsonProperty("result") val result: EncDecStream?
)

data class EncDecStream(
    @param:JsonProperty("url") val url: String?,
    @param:JsonProperty("language") val language: String?,
)

// Vidfast
data class VidfastServers(
    @param:JsonProperty("name") val name: String?,
    @param:JsonProperty("description") val description: String?,
    @param:JsonProperty("data") val data: String?
)

data class VidfastStreamResponse(
    val result: List<VidfastServers>
)

data class VidfastServersStreamRoot(
    val result: VidfastServer,
)

data class VidfastServer(
    @param:JsonProperty("url") val url: String?,
    @param:JsonProperty("tracks") val tracks: List<VidfastTrack>?,
    @param:JsonProperty("4kAvailable") val is4kAvailable: Boolean?
)

data class VidfastTrack(
    @param:JsonProperty("file") val file: String?,
    @param:JsonProperty("label") val label: String?
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
    val romajiTitle: String?,
    val banner: String?,
    val description: String?
)
// --- Data Classes for AniList ---

data class AniListResponse(
    @param:JsonProperty("data") val data: AniListData?
)

data class AniListData(
    @param:JsonProperty("Media") val media: AniListMedia?
)

data class AniListMedia(
    @param:JsonProperty("title") val title: AniListTitle?,
    @param:JsonProperty("bannerImage") val bannerImage: String?,
    @param:JsonProperty("description") val description: String?
)

data class AniListTitle(
    @param:JsonProperty("english") val english: String?,
    @param:JsonProperty("romaji") val romaji: String?
)

//Anizip
data class AnizipEpisode(
    @param:JsonProperty("anidbEid") val anidbEid: Int?,
    @param:JsonProperty("episode") val episode: String?,
)

data class Anizip(val episodes: Map<String, AnizipEpisode>?)

//Animetosho

data class AnimetoshoResponse(
    val data: AnimetoshoData?
)

data class AnimetoshoData(
    val releases: List<AnimetoshoRelease>?
)

data class AnimetoshoRelease(
    val title: String?,
    val magnet: String?,
    val seeders: Int?,
    val leechers: Int?,
    @param:JsonProperty("size_bytes") val sizeBytes: Long?
)

//Vidlink
data class VidLinkStreamResponse(
    @param:JsonProperty("stream") val stream: VidLinkStreamData? = null
)

data class VidLinkStreamData(
    @param:JsonProperty("qualities") val qualities: Map<String, VidLinkQuality>? = null,
    @param:JsonProperty("captions") val captions: List<VidLinkCaption>? = null
)

data class VidLinkQuality(
    @param:JsonProperty("type") val type: String? = null,
    @param:JsonProperty("url") val url: String? = null,
    @param:JsonProperty("headers") val headers: Map<String, String>? = null,
    @param:JsonProperty("requiresProxy") val requiresProxy: Boolean? = null
)

data class VidLinkCaption(
    @param:JsonProperty("url") val url: String? = null,
    @param:JsonProperty("language") val language: String? = null,
    @param:JsonProperty("type") val type: String? = null
)

data class TmdbDate(
    val today: String,
    val nextWeek: String,
    val lastWeekStart: String,
    val monthStart: String
)

//Tmdb
data class TmdbResponse(
    @param:JsonProperty("meta") val meta: TmdbMeta?
)

data class TmdbMeta(
    @param:JsonProperty("app_extras") val appExtras: TmdbAppExtras?
)

data class TmdbAppExtras(
    @param:JsonProperty("cast") val cast: List<TmdbCastMember>?
)

data class TmdbCastMember(
    @param:JsonProperty("name") val name: String?,
    @param:JsonProperty("character") val character: String?,
    @param:JsonProperty("photo") val photo: String?
)

//Primewire
data class PrimewireClass(
    val link: String,
    @param:JsonProperty("host_id")
    val hostId: Long,
    val host: String,
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

//FlixIndia
data class Flixindia(
    val results: List<FlixindiaResult>
)

data class FlixindiaResult(
    val url: String
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

//Vega
data class VegaSearchResponse(
    val hits: List<VegaHit>
)

data class VegaHit(
    val document: VegaDocument
)

data class VegaDocument(
    val id: String,
    val imdb_id: String?,
    val post_title: String,
    val permalink: String,
    val post_thumbnail: String
)

data class AnimiaResponse(
    val server1embedLink: String? = null,
    val server2embedLink: String? = null,
    val server3embedLink: String? = null,
)

data class TorrentioResponse(val streams: List<TorrentioStream>)

data class TorrentioStream(
    val name: String? = null,
    val title: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val description: String? = null,
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
    var url: String? = null,
    var title: String? = null,
    var description: String? = null,
    var subtitles: List<StreamifySubs>? = null,
    @param:JsonProperty("behaviorHints" ) var behaviorHints: StreamifyBehaviorHints? = StreamifyBehaviorHints()
)

data class StreamifyBehaviorHints(
    @param:JsonProperty("proxyHeaders" ) var proxyHeaders: StreamifyProxyHeaders? = StreamifyProxyHeaders(),
    @param:JsonProperty("headers") var headers: Map<String, String>? = null
)

data class StreamifyProxyHeaders(
    @param:JsonProperty("request" ) var request: StreamifyRequest? = StreamifyRequest()
)

data class StreamifyRequest(
    @param:JsonProperty("Referer" ) var Referer: String? = null,
    @param:JsonProperty("Origin"  ) var Origin  : String? = null,
    @param:JsonProperty("User-Agent") var userAgent: String? = null
)

//Multimovies
data class ResponseHash(
    @param:JsonProperty("embed_url") val embed_url: String,
    @param:JsonProperty("key") val key: String? = null,
    @param:JsonProperty("type") val type: String? = null,
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

data class PrimeSrcServerList(
    @param:JsonProperty("servers") val servers: List<PrimeSrcServer>?
)

data class PrimeSrcServer(
    @param:JsonProperty("name") val name: String?,
    @param:JsonProperty("key") val key: String?,
    @param:JsonProperty("quality") val quality: String?,
    @param:JsonProperty("file_size") val fileSize: String?,
    @param:JsonProperty("file_name") val fileName: String?
)

//Onetouchtv

data class OneMediaItem(
    val id: String,
    val title: String,
)

data class OnePlaybackData(
    val sources: List<OneVideoSource>,
    val track: List<OneSubtitleTrack>
)

data class OneVideoSource(
    val type: String,
    val id: String,
    val name: String,
    val quality: String,
    val url: String,
    val headers: Map<String, String>?
)

data class OneSubtitleTrack(
    val file: String,
    val name: String,
)

//Reanime

data class ReanimeResponse(
    val success: Boolean,
    val servers: List<ReanimeServer>
)

data class ReanimeServer(
    val serverName: String,
    val dataLink: String,
    val dataType: String,
)

data class ResolvedReAnime(
    val result: ResolvedReAnimeResult,
)

data class ResolvedReAnimeResult(
    val token: String,
    val context: ResolvedReAnimeContext,
)

data class ResolvedReAnimeContext(
    val token: String,
    @param:JsonProperty("frag1_b64")
    val frag1B64: String,

    @param:JsonProperty("frag2_b64")
    val frag2B64: String,

    @param:JsonProperty("iv_b64")
    val ivB64: String,

    @param:JsonProperty("obfuscation_seed")
    val obfuscationSeed: String,

    @param:JsonProperty("w_payload")
    val wPayload: String,
)

data class ReAnimeStream(
    val result: ReAnimeStreamResult,
)

data class ReAnimeStreamResult(
    val stream: String,
    val context: ReAnimeStreamContext,
)

data class ReAnimeStreamContext(
    @param:JsonProperty("w_payload")
    val wPayload: String,
)

//Animesalt

data class AnimeSaltData(
    val videoSource: String? = null,
    val securedLink: String? = null,
)

//Zinkmovies

data class ZinkTokenResponse(
    val status: String? = null,
    val token: String? = null
)

data class ZinkLink(
    val name: String,
    val url: String,
    val title: String,
)

//Showbox

data class ShareLinkData(val link: String? = null)

data class ShareLinkResponse(val data: ShareLinkData? = null)

data class FileItem(
    val fid: Long = 0L,
    val file_name: String? = null,
    val is_dir: Boolean = false
)
data class FileListData(val file_list: List<FileItem>? = null)

data class FileListResponse(val data: FileListData? = null)

data class VideoQualityResponse(val html: String? = null)

data class VideoQuality(val url: String, val quality: String)

//Anidb

data class AnidbResponse(
    val episodes: List<AnidbEpisode>? = null
)

data class AnidbEpisode(
    val id: Int? = null,
    val number: Int? = null,
)

data class AnidbLanguagesResponse(
    val languages: List<AnidbLanguage>? = null
)

data class AnidbLanguage(
    val code: String? = null,
    val name: String? = null,
    @param:JsonProperty("embed_url") val embedUrl: String? = null
)

//Vidcore

data class VidcoreResponse(
    val result: VidcoreResult? = null,
)

data class VidcoreResult(
    val servers: String,
    val stream: String,
    val token: String
)

data class VidcoreServers(
    val result: List<VidcoreServersResult>? = null,
)

data class VidcoreServersResult(
    val name: String,
    val description: String,
    val data: String
)

data class VidcoreTrack(
    val file: String,
    val label: String
)

data class VidcoreStreamResponse(
    val result: VidcoreStreamData?
)

data class VidcoreStreamData(
    val url: String,
    val noReferrer: Boolean?,
    val tracks: List<VidcoreTrack>?,
)

//Anikage

data class AnikageSearch(
    @param:JsonProperty("count") val count: Int? = null,
    @param:JsonProperty("data") val data: List<AnikageResult>? = null
)

data class AnikageResult(
    @param:JsonProperty("slug") val slug: String? = null,
    @param:JsonProperty("anilistId") val anilistId: Int? = null,
)

data class AnikageServersResponse(
    val servers: List<AnikageServer>? = null,
    val embeds: List<AnikageEmbeds>? = null
)

data class AnikageServer(
    val id: String? = null,
)

data class AnikageEmbeds(
    val id: String? = null,
)

data class AnikageSource(
    @param:JsonProperty("sources") val sources: List<AnikageStreamSource>? = null,
    @param:JsonProperty("subtitles") val subtitles: List<AnikageSub>? = null,
    @param:JsonProperty("embeds") val embeds: List<AnikageEmbed>? = null,
)
data class AnikageSub(
    @param:JsonProperty("file") val file: String? = null,
    @param:JsonProperty("label") val label: String? = null,
)
data class AnikageEmbed(
    @param:JsonProperty("url") val url: String,
    @param:JsonProperty("type") val type: String,
    @param:JsonProperty("server") val server: String,
)
data class AnikageStreamSource(
    @param:JsonProperty("url") val url: String? = null,
    @param:JsonProperty("quality") val quality: String? = null,
    @param:JsonProperty("isM3U8") val isM3U8: Boolean? = null,
)

//Fshare

data class FshareDownload(
    val src: String,
    val label: String
)

data class FshareFile(
    val sources: List<FshareSource>,
    val backups: List<FshareSource>,
    val alternatives: List<List<FshareSource>>,
    val downloads: List<FshareDownload>?,
    val vast: Int?
)

data class FshareData(
    val file: FshareFile
)

data class FshareResponse(
    val data: FshareData,
    val status: String
)

data class FshareSource(
    val src: String,
    val label: String,
    val type: String,
    val quality: String?,
    val storage: String,
    val id: String,
    val selected: Boolean? = null
)

//VaPlayer

data class VaPlayerResponse(
    val status_code: String? = null,
    val data: VaPlayerData? = null,
    val default_subs: List<VaPlayerSub>? = null
)

data class VaPlayerData(
    val title: String? = null,
    val imdb_id: String? = null,
    val season: String? = null,
    val episode: String? = null,
    val file_name: String? = null,
    val backdrop: String? = null,
    val stream_urls: List<String>? = null
)

data class VaPlayerSub(
    val lang: String? = null,
    val code: String? = null,
    val url: String? = null
)

//Anikoto

data class AnikotoResponse(
    val status: Int,
    val result: String
)

data class AnikotoServerResponse(
    val status: Int? = null,
    val result: AnikotoServerResult? = null
)

data class AnikotoServerResult(
    val url: String? = null,
)

//HdGharTv

data class HdGharSearchResponse(
    val movies: List<HdGharSearchItem>? = null,
    val series: List<HdGharSearchItem>? = null
)

data class HdGharSearchItem(
    @param:JsonProperty("_id") val id: String? = null,
    val tmdbId: Int? = null
)

data class HdGharDetailsResponse(
    val streamingLinks: List<HdGharLink>? = null, // For Movies
    val seasons: List<HdGharSeason>? = null              // For Series
)

data class HdGharSeason(
    val seasonNumber: Int? = null,
    val episodes: List<HdGharEpisode>? = null
)

data class HdGharEpisode(
    val episodeNumber: Int? = null,
    val streamingLinks: List<HdGharLink>? = null
)

data class HdGharLink(
    val quality: String? = null,
    val url: String? = null,
    val type: String? = null
)

//CtgMovies

data class CTGLink(
    val quality: String,
    val url: String,
    val hlsUrl: String?,
    val type: String,
    val source: String,
    val language: String,
    val sizeBytes: Long?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val audioTracks: List<Pair<String, String>>
)

//MovieBlast

data class MovieBlastSearchResponse(
    @param:JsonProperty("search") val search: List<MovieBlastSearchItem>?
)

data class MovieBlastSearchItem(
    @param:JsonProperty("id") val id: Int?,
    @param:JsonProperty("type") val type: String?,
    @param:JsonProperty("name") val name: String?,
    @param:JsonProperty("original_name") val originalName: String?
)

data class MovieBlastDetailsResponse(
    @param:JsonProperty("videos") val videos: List<MovieBlastVideo>?,
    @param:JsonProperty("seasons") val seasons: List<MovieBlastSeason>?,
    @param:JsonProperty("substitles") val subtitles: List<MovieBlastSubtitle>?
)

data class MovieBlastSeason(
    @param:JsonProperty("season_number") val seasonNumber: Int?,
    @param:JsonProperty("episodes") val episodes: List<MovieBlastEpisode>?
)

data class MovieBlastEpisode(
    @param:JsonProperty("episode_number") val episodeNumber: Int?,
    @param:JsonProperty("videos") val videos: List<MovieBlastVideo>?
)

data class MovieBlastVideo(
    @param:JsonProperty("link") val link: String?,
    @param:JsonProperty("server") val server: String?,
    @param:JsonProperty("lang") val lang: String?
)

data class MovieBlastSubtitle(
    @param:JsonProperty("link") val link: String?,
    @param:JsonProperty("lang") val lang: String?
)

//Vidup

data class VidupResponse(
    val status: Int? = null,
    val result: VidupResult? = null,
    val info: String? = null
)

data class VidupResult(
    val servers: String? = null,
    val stream: String? = null,
    val token: String? = null
)

data class VidupServersResponse(
    val status: Int? = null,
    val result: List<VidupServer>? = null
)

data class VidupServer(
    val name: String? = null,
    val description: String? = null,
    val image: String? = null,
    val data: String? = null
)

data class VidupStreamResponse(
    val status: Int? = null,
    val result: VidupStreamResult? = null
)

data class VidupStreamResult(
    val url: String? = null,
    val tracks: List<VidupTrack>? = emptyList()
)

data class VidupTrack(
    val file: String? = null,
    val label: String? = null
)

//Fibwatch

data class FibwatchEpisode(
    @param:JsonProperty("title") val title: String? = null,
    @param:JsonProperty("url") val url: String? = null
)

data class FibwatchEpisodesResponse(
    @param:JsonProperty("episodes") val episodes: List<FibwatchEpisode>? = null
)

data class FibwatchSource(
    @param:JsonProperty("url") val url: String? = null,
    @param:JsonProperty("res") val res: String? = null
)

data class FibwatchSwitcherResponse(
    @param:JsonProperty("current") val current: List<FibwatchSource>? = null,
    @param:JsonProperty("popup") val popup: List<FibwatchSource>? = null
)

//Anineko

data class AninekoSearchResponse(
    @param:JsonProperty("success") val success: Boolean? = null,
    @param:JsonProperty("results") val results: List<AninekoSearchResult>? = null
)

data class AninekoSearchResult(
    @param:JsonProperty("title") val title: String? = null,
    @param:JsonProperty("url") val url: String? = null,
    @param:JsonProperty("image") val image: String? = null,
    @param:JsonProperty("meta") val meta: String? = null
)

//Cinejoy

data class CinejoyServersResponse(
    var servers: List<CinejoyServers>? = null
)

data class CinejoyServers(
    var name: String? = null
)

data class CinejoyEncResponse(
    var status: Int? = null,
    var result: CinejoyEnc? = null
)

data class CinejoyEnc(
    var data: String? = null,
    var state: CinejoyState? = null
)

data class CinejoyState(
    var responseKey: String? = null,
    var aad: String? = null
)

data class CinejoyDecResponse(
    var status: Int? = null,
    var result: CinejoyDecResultOuter? = null
)

data class CinejoyDecResultOuter(
    var data: CinejoyDecData? = null,
    var status: Int? = null
)

data class CinejoyDecData(
    var stream: List<CinejoyStream>? = null,
    var error: String? = null
)

data class CinejoyStream(
    var type: String? = null,
    var id: String? = null,
    var playlist: String? = null,
    var qualities: Map<String, CinejoyQuality>? = null,
    var captions: List<CinejoyCaption>? = null
)

data class CinejoyQuality(
    var type: String? = null,
    var url: String? = null
)

data class CinejoyCaption(
    var type: String? = null,
    var id: String? = null,
    var url: String? = null,
    var language: String? = null
)

//Just4Anime

data class Just4Anime(
    val success: Boolean? = null,
    val data: Just4AnimeData? = null
)

data class Just4AnimeData(
    val animeId: String? = null,
    val malId: Long? = null,
    val servers: List<Just4AnimeServer> = emptyList()
)

data class Just4AnimeServer(
    val code: String? = null,
    val displayName: String? = null,
    val animeId: String? = null,
    val episodeId: String? = null,
    val hasEpisode: Boolean = false,
    val totalEpisodes: Int = 0,
    val types: List<String> = emptyList()
)

data class Just4AnimeMetaSources(
    val success: Boolean? = null,
    val data: Just4AnimeMetaSourceData? = null
)

data class Just4AnimeMetaSourceData(
    val episode: Just4AnimeMetaEpisode? = null,
    val isDub: Boolean? = null,
    val type: String? = null,
    val sources: List<Just4AnimeSourceItem> = emptyList(),
    val subtitles: List<Just4AnimeSubtitleItem> = emptyList(),
    val iframe: List<Just4AnimeIframeItem> = emptyList()
)

data class Just4AnimeMetaEpisode(
    val number: Int? = null,
    val id: String? = null,
    val title: String? = null
)

data class Just4AnimeSourceItem(
    val url: String? = null,
    val quality: String? = null,
    val isM3U8: Boolean = false,
    val isDub: Boolean? = null,
    val server: String? = null,
    val headers: Map<String, String>? = null,
    val proxied: Boolean? = null
)

data class Just4AnimeSubtitleItem(
    val url: String? = null,
    val lang: String? = null,
    val language: String? = null,
    val format: String? = null,
    val headers: Map<String, String>? = null
)

data class Just4AnimeIframeItem(
    val url: String? = null,
    val quality: String? = null,
    val server: String? = null,
    val category: String? = null
)
