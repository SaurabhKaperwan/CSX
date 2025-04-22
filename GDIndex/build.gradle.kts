// use an integer for version numbers
version = 4

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

//    description = "Lorem Ipsum"
    authors = listOf("Horis")

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
    )

    iconUrl = "https://github.com/SaurabhKaperwan/CSX/raw/refs/heads/master/GDIndex/icon.ico"

}
