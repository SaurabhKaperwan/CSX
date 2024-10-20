import org.jetbrains.kotlin.konan.properties.Properties

version = 18

android {
    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "CONSUMET_API", "\"${properties.getProperty("CONSUMET_API")}\"")
    }
}

cloudstream {
    //language = "en"
    description = "Multi API Extension"
    authors = listOf("megix")
    status = 1
    tvTypes = listOf(
        "TvSeries",
        "Movie",
        "AsianDrama",
        "Anime"
    )

    iconUrl = "https://us.123rf.com/450wm/mrshamsjaman/mrshamsjaman2008/mrshamsjaman200800943/154338064-initial-letter-cs-logo-or-sc-logo-vector-design-template.jpg"
}
