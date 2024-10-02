package com.idkidknow.mcrealcomm.fabric

import com.idkidknow.mcrealcomm.fabric.platform.FabricApi
import com.idkidknow.mcrealcomm.platformEntry

fun fabricModInit() {
    platformEntry(FabricApi())
}
