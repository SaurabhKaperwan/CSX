// use an integer for version numbers
version = 32

cloudstream {
    //language = "en"
    // All of these properties are optional, you can safely remove them

    description = "Netflix, PrimeVideo, JioHotstar(Hotstar, Disney, Paramount, HBO, Peacock) Content in Multiple Languages"
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
        "AsianDrama",
        "Anime"
    )

    iconUrl = "https://github.com/SaurabhKaperwan/CSX/raw/refs/heads/master/NetflixMirrorProvider/icon.png"
}
