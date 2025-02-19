version = 16

cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "Contains Indian Movies and TV Series upto 1080p"
     authors = listOf("megix")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
        "TvSeries",
        "Movie",
    )

    iconUrl = "https://www.full4movies.delivery/wp-content/uploads/2023/12/cropped-cropped-admin-ajax-1.png"
}
