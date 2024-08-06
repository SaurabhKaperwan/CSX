// use an integer for version numbers
version = 2

cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "Indian Multilingual Dubbed & Subbed Anime"
    language = "hi"
    authors = listOf("megix")

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
    tvTypes = listOf("Movie,Anime,Cartoon")
    iconUrl="https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEg6keB0Vjw3pV6mq9JhKZVKpIBEZXft11cFDS2ANbg8sL0SRyXJZEipVxWEepSs9xbCC95dyUhPvqGn_-H0sTKoU5rW5vvoC307-ffdOWYzDMyUzecwXDM_hkgtIiDOneTx9H-VQ_nATYcCsGzzM7Sp-wOJ6M3HIEpqJ9nfwj4Ssuf3GIJD0CeGBgg1liep/s800/anitime%20logo.png"
}
