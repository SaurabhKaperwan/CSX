version = 1

cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "Indian Multilingual Provider"
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

    iconUrl = "https://0gomovies.movie/wp-content/themes/assets/images/logo-dark.png"
}
