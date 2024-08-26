version = 7

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Contains Movies, TV Series and Anime upto 4K"
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
        "Movie",
        "TvSeries",
        "Anime",
        "AsianDrama",
    )

    iconUrl = "https://vadapav.mov/assets/favicon-32x32.png"
}
