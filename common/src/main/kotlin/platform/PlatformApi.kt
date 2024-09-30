package com.idkidknow.mcrealcomm.platform

import java.nio.file.Path

interface PlatformApi {
    fun createServerLifecycleEventManagers(): ServerLifecycleEventManagers
    /** Call only once in a whole lifecycle. Can be expensive. */
    fun createCommonEventManagers(): CommonEventManagers
    fun getGameRootDir(): Path
    fun getGameConfigDir(): Path
}
