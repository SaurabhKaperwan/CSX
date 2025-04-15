version = 18

cloudstream {
    language = "hi"
    // All of these properties are optional, you can safely remove them

    description = "High Quality Movies and TV Shows"
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

    iconUrl = "https://moviesdrive.xyz/wp-content/uploads/2023/08/cropped-My-Brand-New-Logo-%E2%80%94-%E2%80%94-Create-a-logo-Google-Chrome-28-08-2023-23_14_19-180x180.png"
}
