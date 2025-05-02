version = 19

cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "Movies and Series upto 4K"
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

    iconUrl = "https://raw.githubusercontent.com/SaurabhKaperwan/CSX/refs/heads/master/Bollyflix/icon.png"
}
