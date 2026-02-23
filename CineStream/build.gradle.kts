import org.jetbrains.kotlin.konan.properties.Properties

version = 341

android {
    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        android.buildFeatures.buildConfig=true
        buildConfigField("String", "CONSUMET_API", "\"${properties.getProperty("CONSUMET_API")}\"")
        buildConfigField("String", "SIMKL_API", "\"${properties.getProperty("SIMKL_API")}\"")
        buildConfigField("String", "TMDB_KEY", "\"${properties.getProperty("TMDB_KEY")}\"")
        buildConfigField("String", "CC_COOKIE", "\"${properties.getProperty("CC_COOKIE")}\"")
        buildConfigField("String", "CINE_API", "\"${properties.getProperty("CINE_API")}\"")
        buildConfigField("String", "CASTLE_API", "\"${properties.getProperty("CASTLE_API")}\"")
        buildConfigField("String", "HDMOVIELOVER_API", "\"${properties.getProperty("HDMOVIELOVER_API")}\"")
    }
}

cloudstream {
    // language = "en"
    description = "One stop solution for Movies, Series, Anime, AsianDrama and Torrents"
    authors = listOf("megix")
    status = 1
    tvTypes = listOf(
        "TvSeries",
        "Movie",
        "AsianDrama",
        "Anime",
        "Torrent"
    )

    iconUrl = "https://github.com/SaurabhKaperwan/CSX/raw/refs/heads/master/CineStream/icon.png"
}
