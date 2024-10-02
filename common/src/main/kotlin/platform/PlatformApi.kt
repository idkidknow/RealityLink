package com.idkidknow.mcrealcomm.platform

import com.idkidknow.mcrealcomm.event.UnitEventManager
import java.nio.file.Path

interface PlatformApi {
    /** Should be managers without any handler in existence */
    fun createServerLifecycleEventManagers(): ServerLifecycleEventManagers
    /** Should be a manager without any handler in existence */
    fun createRegisterCommandsEventManager(): UnitEventManager<RegisterCommandsEvent>
    fun getGameRootDir(): Path
    fun getGameConfigDir(): Path
}
