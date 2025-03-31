version = 39

cloudstream {
    //language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "Must Install(For other extensions to work properly)"
     authors = listOf("megix")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
        "Movie",
        "AsianDrama",
        "Anime"
    )

    iconUrl = "https://cdn-icons-png.flaticon.com/512/4961/4961639.png"
}
