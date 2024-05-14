package com.megix

// import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.DoodLaExtractor
import com.lagradost.cloudstream3.extractors.StreamTape
import com.lagradost.cloudstream3.extractors.Voe

class MyDood : DoodLaExtractor() {
    override var mainUrl = "https://d000d.com"
}

class MyStreamTape : StreamTape() {
    override var mainUrl = "https://streamtape.to"
}

class MyVoe : Voe() {
    override var mainUrl = "https://michaelapplysome.com"
}
