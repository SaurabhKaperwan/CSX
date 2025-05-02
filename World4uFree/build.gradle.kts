version = 14

cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "High Quality Movie and TV Series"
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
    )

    iconUrl = "https://github.com/SaurabhKaperwan/CSX/raw/refs/heads/master/World4uFree/icon.png"
}
