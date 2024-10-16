plugins {
    id("realcomm.common")
    id("realcomm.vanilla")
}

dependencies {
    compileOnly(libs.mixin)
}

configurations.register("mixinCodes") { isCanBeResolved = false; isCanBeConsumed = true }
configurations.register("mixinResources") { isCanBeResolved = false; isCanBeConsumed = true }

artifacts {
    add("mixinCodes", sourceSets.main.get().java.sourceDirectories.singleFile)
    add("mixinResources", sourceSets.main.get().resources.sourceDirectories.singleFile)
}
