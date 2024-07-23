package com.horis.cloudstreamplugins.entities

data class SearchData(
    val head: String,
    val searchResult: List<SearchResult>,
    val type: Int
)
