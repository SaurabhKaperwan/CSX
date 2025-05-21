version = 5
dependencies {
    implementation("androidx.annotation:annotation-jvm:1.9.1")
}

cloudstream {
    //description = "Movie website in Bangladesh"
    authors = listOf("megix")

    description = "Hdmovies4u Provider"
    language = "hi"
    authors = listOf("HindiProviders")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified

    // List of video source types. Users are able to filter for extensions in a given category.
    // You can find a list of avaliable types here:
    // https://recloudstream.github.io/cloudstream/html/app/com.lagradost.cloudstream3/-tv-type/index.html
    tvTypes = listOf(
        "Movie",
        "TvSeries",
    )
    iconUrl = "https://www.uptoplay.net/imagescropped/hdmovies4udownloadandwatchmoviesicon128.jpgplus.webp"
}
