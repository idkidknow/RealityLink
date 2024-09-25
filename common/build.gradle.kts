import com.google.protobuf.gradle.id
import org.gradle.jvm.tasks.Jar

plugins {
    id("realcomm.minecraft")
    alias(libs.plugins.protobuf)
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

    // grpc
    runtimeOnly(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    compileOnly(libs.tomcat.annotations.api) // necessary for Java 9+
}

protobuf {
    protoc {
        artifact = libs.protoc.asProvider().get().toString()
    }

    plugins {
        id("grpc") {
            artifact = libs.protoc.gen.grpc.java.get().toString()
        }
    }

    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") {}
            }
        }
    }
}
