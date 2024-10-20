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
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.util.jar.JarEntry
import java.util.jar.JarInputStream
import java.util.jar.JarOutputStream

abstract class GenerateMinimalForgeMetadata : TransformAction<GenerateMinimalForgeMetadata.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val modId: Property<String>
        @get:Input
        val packFormat: Property<Int>
    }

    @get:InputArtifact
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = input.get().asFile
        val output = outputs.file("${input.nameWithoutExtension}-forge.jar")
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
                val packMcmeta = """
                    {
                      "pack": {
                        "description": "generated",
                        "pack_format": ${parameters.packFormat.get()}
                      }
                    }
                """.trimIndent()
                output.putNextEntry(JarEntry("META-INF/mods.toml"))
                output.write(modsToml.toByteArray())
                output.closeEntry()
                output.putNextEntry(JarEntry("pack.mcmeta"))
                output.write(packMcmeta.toByteArray())
                output.closeEntry()
                output.putNextEntry(JarEntry("_GENERATED/Mod.class"))
                output.write(generateModClass())
                output.closeEntry()
            }}
        }}
    }

    fun generateModClass(): ByteArray {
        val classWriter = ClassWriter(0)
        classWriter.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC.or(Opcodes.ACC_FINAL), "_GENERATED/Mod", null, "java/lang/Object", null)
        classWriter.visitAnnotation("Lnet/minecraftforge/fml/common/Mod;", false).let {
            it.visit("value", parameters.modId.get())
            it.visitEnd()
        }
        classWriter.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).let {
            it.visitVarInsn(Opcodes.ALOAD, 0)
            it.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            it.visitInsn(Opcodes.RETURN)
            it.visitMaxs(1, 1)
            it.visitEnd()
        }
        classWriter.visitEnd()
        return classWriter.toByteArray()
    }
}

fun Project.registerGenerateMinimalForgeMetadataAttribute(
    name: String,
    configure: GenerateMinimalForgeMetadata.Parameters.() -> Unit,
): Attribute<Boolean> {
    val attr = Attribute.of(name, Boolean::class.javaObjectType)

    dependencies.registerTransform(GenerateMinimalForgeMetadata::class.java) {
        from.attribute(attr, false)
        to.attribute(attr, true)
        parameters(configure)
    }

    dependencies.artifactTypes.all {
        attributes.attribute(attr, false)
    }

    return attr
}
