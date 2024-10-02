package com.horis.cloudstreamplugins.entities

data class PlayListItem(
    val image: String,
    val sources: List<Source>,
    val tracks: List<Tracks>?,
    val title: String
)
