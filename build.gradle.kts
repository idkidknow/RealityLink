tasks.register<Copy>("buildAll") {
    val versions: List<String> = file("versions").listFiles().map { f -> f.name }
    for (v in versions) {
        dependsOn(gradle.includedBuild(v).task(":productJar"))
        from(layout.projectDirectory.dir("versions").dir(v).dir("build/libs")) {
            include("*-product.jar")
        }
    }
    into(layout.buildDirectory.dir("libs"))
    rename { name ->
        name.replace("-product.jar", ".jar")
    }
}
