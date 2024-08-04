version = 1

cloudstream {
    language = "multi"
    // All of these properties are optional, you can safely remove them

    description = "Watch content from Dailymotion"
     authors = listOf("megix")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf("Others")
    iconUrl = "https://static1.dmcdn.net/images/dailymotion-logo-ogtag.png.v2889585078f8ced02"
}
