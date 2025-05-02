# Plugin Development Guide

## Change your project info
In the `settings.gradle.kts` file

```kotlin
rootProject.name = "SpectacleGetScreenKRecallPlugin"
```

In the `build.gradle.kts` file
```kotlin
val plugins = listOf(
    PluginMetadata(
        pluginId = "CaptureScreenByKDESpectaclePlugin",
        supportPlatform = setOf(OS.LINUX),
        supportUI = true,
        pluginClass = "io.github.octestx.krecall.plugins.captureScreen.kdespectacle.CaptureScreenByKDESpectaclePlugin"
    ),
)
val groupName = "io.github.octestx.krecall.plugins.ext.getscreen"
val versionName = "1.0-SNAPSHOT"
val pluginPackName = "CaptureScreenByKDESpectaclePlugin"
```

## Create Plugin Class
as CaptureScreenByKDESpectaclePlugin class

must extend AbsCaptureScreenPlugin, other AbsPlugin or PluginBasic

must has a constructor with need metadata parameter

remember to add pluginClass to the pluginMetadata

remember changed your package name
```kotlin
package io.github.octestx.krecall.plugins.captureScreen.kdespectacle

class CaptureScreenByKDESpectaclePlugin(metadata: PluginMetadata): AbsCaptureScreenPlugin(metadata)
```

# Build the plugin package
Gradle tasks -> Tasks -> shadow -> shadowJar