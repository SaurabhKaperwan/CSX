// use an integer for version numbers
version = 9

cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "The largest movie link store in Bangladesh"
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
        "NSFW",
        "AnimeMovie",
        "AsianDrama"
    )
    language = "bn"

    iconUrl = "https://mlsbd.shop/wp-content/uploads/2020/08/MLSBD-Logo.png"
}
