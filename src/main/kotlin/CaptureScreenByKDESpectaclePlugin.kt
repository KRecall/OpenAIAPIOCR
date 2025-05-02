package io.github.octestx.krecall.plugins.captureScreen.kdespectacle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import io.github.octestx.basic.multiplatform.common.utils.OS
import io.github.octestx.krecall.plugins.basic.AbsCaptureScreenPlugin
import io.github.octestx.krecall.plugins.basic.PluginContext
import io.github.octestx.krecall.plugins.basic.PluginMetadata
import io.github.octestx.krecall.plugins.basic.WindowInfo
import io.klogging.noCoLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream

/**
 * 记得把包名改成你的
 * 这是插件类，每个插件包包括多个这样的插件类
 * 插件类必须继承PluginBasicExt
 * 不过根据依赖关系，可以根据需要选择继承PluginBasic或者其他子类
 * 插件的其他能力例如注册Nav导航，SettingPage中的页面，侧滑栏的标签页等待是通过实现PluginAbilityInterfaces中各种各样的接口
 * @see io.github.octestx.krecall.plugins.basic.PluginBasicExt
 * @see io.github.octestx.krecall.plugins.basic.PluginBasic
 * @see io.github.octestx.krecall.plugins.basic.PluginAbilityInterfaces
 */
class CaptureScreenByKDESpectaclePlugin(metadata: PluginMetadata): AbsCaptureScreenPlugin(metadata) {
    companion object {
        val metadata = PluginMetadata(
            pluginId = "CaptureScreenByKDESpectaclePlugin",
            supportPlatform = setOf(OS.OperatingSystem.LINUX),
            supportUI = true,
            mainClass = "CaptureScreenByKDESpectaclePlugin"
        )
    }

    private val ologger = noCoLogger<CaptureScreenByKDESpectaclePlugin>()
    override suspend fun supportOutputToStream(): Boolean = false

    override suspend fun getScreen(outputStream: OutputStream): WindowInfo {
        throw UnsupportedOperationException()
    }

    override suspend fun getScreen(outputFileBitItNotExits: File): WindowInfo {
        withContext(Dispatchers.IO) {
            ologger.info { "getScreen: $outputFileBitItNotExits" }
            val processBuilder = ProcessBuilder("/usr/bin/spectacle", "-f", "-b", "-n", "-o", outputFileBitItNotExits.absolutePath)
            processBuilder.redirectErrorStream(false)
            val process = processBuilder.start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val err = RuntimeException("Command[${processBuilder.command()}] failed with exit code $exitCode")
                ologger.error(err) { "Exception: "+err.message }
                throw err
            }
        }
        return WindowInfo(
            0,
            "DEFAULT__Spectacle",
            "DEFAULT__Spectacle",
        )
    }

    override fun loadInner(context: PluginContext) {
        ologger.info { "Loaded" }
    }

    override fun selected() {}
    override fun unselected() {}

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        var painter: Painter? by remember { mutableStateOf(null) }
        Column {
            Button(onClick = {
                scope.launch {
                    val f = test()
                    val img = f.inputStream().readAllBytes().decodeToImageBitmap()
                    painter = BitmapPainter(img)
                    ologger.info("Test: ${f.absolutePath}")
                }
            }) {
                Text("Test")
            }
            painter?.let { Image(it, contentDescription = null) }
        }
    }

    private suspend fun test(): File {
        val f = File(pluginDir, "test.png")
        getScreen(f)
        if (!f.exists()) {
            throw FileNotFoundException("testFile not found")
        }
        ologger.info("Test: ${f.absolutePath}")
        return f
    }


    override suspend fun tryInitInner(): InitResult {
        ologger.info { "TryInit" }
        val e = runBlocking {
            try {
                test()
                null
            } catch (e: Exception) {
                e
            }
        }
        if (e == null) {
            _initialized.value = true
            return InitResult.Success
        } else {
            return InitResult.Failed(e)
        }
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized
}