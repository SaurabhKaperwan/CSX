version = 2

cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "Testing only movie right now"
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

    iconUrl = "https://hdhub4u.durban/wp-content/uploads/2021/05/hdhub4ulogo.png"
}
