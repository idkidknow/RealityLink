plugins {
    id("realcomm.minecraft")
}

val platforms = property("enabled_platforms").toString().split(',')

architectury {
    common(platforms)
}

dependencies {
    // We depend on Fabric Loader here to use the Fabric @Environment annotations,
    // which get remapped to the correct annotations on each platform.
    // Do NOT use other classes from Fabric Loader.
    modImplementation(libs.fabric.loader)
    // Architectury API
    modImplementation(libs.architectury.api)
}
