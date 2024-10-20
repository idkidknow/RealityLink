package reallink

import org.gradle.api.Project
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream

abstract class GenerateMinimalNeoForgeMetadata : TransformAction<GenerateMinimalNeoForgeMetadata.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val modId: Property<String>
    }

    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val output = outputs.file("${input.nameWithoutExtension}-neoforge.jar")
        @Suppress("NAME_SHADOWING")
        input.inputStream().use { input -> JarInputStream(input).use { input ->
            output.outputStream().use { output -> JarOutputStream(output).use { output ->
                while (true) {
                    val entry = input.nextJarEntry ?: break
                    val bytes = input.readBytes()
                    output.putNextEntry(entry)
                    output.write(bytes)
                    output.closeEntry()
                }
                val modsToml = """
                modLoader = "javafml"
                loaderVersion = "[1,)"
                license = "All rights reserved"

                [[mods]]
                modId = "${parameters.modId.get()}"
                
            """.trimIndent()
                output.putNextEntry(JarEntry("META-INF/neoforge.mods.toml"))
                output.write(modsToml.toByteArray())
                output.closeEntry()
            }}
        }}
    }
}

fun Project.registerGenerateMinimalNeoForgeMetadataAttribute(
    name: String,
    configure: GenerateMinimalNeoForgeMetadata.Parameters.() -> Unit,
): Attribute<Boolean> {
    val attr = Attribute.of(name, Boolean::class.javaObjectType)

    dependencies.registerTransform(GenerateMinimalNeoForgeMetadata::class.java) {
        from.attribute(attr, false)
        to.attribute(attr, true)
        parameters(configure)
    }

    dependencies.artifactTypes.all {
        attributes.attribute(attr, false)
    }

    return attr
}
