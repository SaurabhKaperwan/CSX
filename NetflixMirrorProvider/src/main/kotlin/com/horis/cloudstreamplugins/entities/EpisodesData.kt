package com.horis.cloudstreamplugins.entities

data class EpisodesData(
    val episodes: List<Episode>?,
    val nextPage: Int,
    val nextPageSeason: String,
    val nextPageShow: Int,
)
