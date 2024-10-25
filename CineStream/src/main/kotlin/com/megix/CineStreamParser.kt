package com.megix

import com.fasterxml.jackson.annotation.JsonProperty

data class RarResponseData(
    val data: List<RarItem>?
)

data class RarItem(
    val id: Int,
    val name: String,
    val second_name: String,
    val image: String,
    val url: String,
    val type: String
)

data class TwoEmbedQuery(
    val stream: List<TwoEmbedStream>
)
data class TwoEmbedStream(
    val id: String,
    val type: String,
    val playlist: String,
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

data class WYZIESubtitle(
    val url: String,
    val display: String,
)

data class WHVXSubtitle(
    val url: String,
    val languageName: String,
)

data class ConsumetSources(
    val sources: List<ConsumetSource>?,
    val subtitles: List<ConsumetSubtitle>?,
    val download: String?
)
    
data class ConsumetSource(
    val url: String,
    val isM3u8: Boolean
)

data class ConsumetSubtitle(
    val url: String,
    val lang: String
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
)