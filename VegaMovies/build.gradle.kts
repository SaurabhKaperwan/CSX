version = 9

cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "Contains Hindi and English Movies and TV Series upto 4k"
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

    iconUrl = "https://vegamovies.ph/wp-content/uploads/2022/03/Logo.png"
}
