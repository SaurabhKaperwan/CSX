import org.jetbrains.kotlin.konan.properties.Properties

version = 101

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
    //language = "en"
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

    iconUrl = "https://us.123rf.com/450wm/mrshamsjaman/mrshamsjaman2008/mrshamsjaman200800943/154338064-initial-letter-cs-logo-or-sc-logo-vector-design-template.jpg"
}
