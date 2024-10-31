version = 1

cloudstream {
    authors     = listOf("Dilip")
    language    = "hi"
    description = "Movies & Series"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "Movie",
        "TvSeries"
    )
    iconUrl = "https://cinedoze.com/wp-content/uploads/2024/05/CineDoze.Com-Logo.png"
}
