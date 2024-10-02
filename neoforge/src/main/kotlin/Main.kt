package com.idkidknow.mcrealcomm.neoforge

import com.idkidknow.mcrealcomm.platformEntry
import com.idkidknow.mcrealcomm.neoforge.platform.NeoForgeApi

fun neoforgeModInit() {
    platformEntry(NeoForgeApi())
}
