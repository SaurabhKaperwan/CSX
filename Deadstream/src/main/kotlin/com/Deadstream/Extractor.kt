package com.Deadstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.Filesim

class MyFileMoon : Filesim() {
    override var mainUrl = "https://filemoon.nl"
}
