import org.jetbrains.kotlin.konan.properties.Properties

version = 129

android {
    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        android.buildFeatures.buildConfig=true
        buildConfigField("String", "BYPASS_API", "\"${properties.getProperty("BYPASS_API")}\"")
        buildConfigField("String", "CONSUMET_API", "\"${properties.getProperty("CONSUMET_API")}\"")
    }
}

cloudstream {
    // language = "en"
    description = "One stop solution for Movies, Series, Anime, Livetv, AsianDrama and Torrents"
    authors = listOf("megix")
    status = 1
    tvTypes = listOf(
        "TvSeries",
        "Movie",
        "AsianDrama",
        "Anime",
        "Torrent",
        "Live",
    )

    iconUrl = "https://github.com/SaurabhKaperwan/CSX/raw/refs/heads/master/CineStream/icon.jpg"
}
