plugins {
    id("realcomm.common")
    id("realcomm.common-deps")
    alias(libs.plugins.kotlin.serialization)
}

val platforms = property("enabled_platforms").toString().split(',')

architectury {
    common(platforms)
}

dependencies {
    // Only for mixin deps
    modImplementation(libs.fabric.loader)
}
