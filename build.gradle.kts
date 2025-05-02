import org.jetbrains.compose.desktop.application.dsl.TargetFormat

val groupName = "io.github.octestx.krecall.plugins.captureScreen.kdespectacle"
val versionName = "1.0-SNAPSHOT"
val pluginPackName = "CaptureScreenByKDESpectaclePlugin"
val plugins = listOf(
    PluginMetadata(
        pluginId = "CaptureScreenByKDESpectaclePlugin",
        supportPlatform = setOf(OS.LINUX),
        supportUI = true,
        // 插件类的全限定名
        pluginClass = "$groupName.CaptureScreenByKDESpectaclePlugin"
    ),
)





plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = groupName
version = versionName

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.components.resources)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.github.octestx.krecall.plugins.basiclib:library:1.4.9")
    implementation("io.github.octestx:basic-multiplatform-lib:0.1.1")
    implementation("io.github.octestx:basic-multiplatform-ui-lib:0.1.3")
}

//compose.desktop {
//    application {
//        mainClass = "MainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "SpectacleGetScreenKRecallPlugin"
//            packageVersion = "1.0.0"
//        }
//    }
//}

tasks {
    shadowJar {
        archiveBaseName.set(pluginPackName)
        mergeServiceFiles()
        archiveVersion.set(versionName)
        archiveClassifier.set("")

        // 包含所有插件元数据
        into("META-INF/plugins") {
            from(generatePluginMetadata())
        }

        configurations = listOf(project.configurations.runtimeClasspath.get())
    }

    register("generateMetadata") {
        dependsOn("compileKotlin")
        doLast {
            generatePluginMetadata()
        }
    }
}

fun generatePluginMetadata(): FileCollection {
    plugins.forEach { metadata ->
        file("build/metadata/${metadata.pluginId}.json").apply {
            parentFile.mkdirs()
            writeText(
                buildString {
                    append("{\n")
                    append("  \"plugin_id\": \"${metadata.pluginId}\",\n")
                    append("  \"support_platform\": [")
                    metadata.supportPlatform.forEachIndexed { index, os ->
                        append("\"${os.name}\"")
                        if (index < metadata.supportPlatform.size - 1) {
                            append(", ")
                        }
                    }
                    append("],\n")
                    append("  \"support_ui\": ${metadata.supportUI},\n")
                    append("  \"main_class\": \"${metadata.pluginClass}\"\n")
                    append("}")
                }
            )
        }
    }
    return files(fileTree("build/metadata").files)
}

enum class OS { WIN, MACOS, LINUX, OTHER }

val currentOS: OS by lazy {
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.contains("win") -> OS.WIN
        osName.contains("mac") -> OS.MACOS
        osName.contains("nix") || osName.contains("nux") -> OS.LINUX
        else -> OS.OTHER
    }
}

data class PluginMetadata(
    val pluginId: String,
    val supportPlatform: Set<OS>,
    val supportUI: Boolean,
    val pluginClass: String
)