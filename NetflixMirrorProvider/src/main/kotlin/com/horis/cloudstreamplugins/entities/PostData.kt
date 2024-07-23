package com.horis.cloudstreamplugins.entities

data class PostData(
    val desc: String?,
    val director: String?,
    val episodes: List<Episode?>,
    val genre: String?,
    val nextPage: Int?,
    val nextPageSeason: String?,
    val nextPageShow: Int?,
    val season: List<Season>?,
    val title: String,
    val year: String
)
