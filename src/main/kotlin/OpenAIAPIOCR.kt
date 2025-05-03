package io.github.octestx.krecall.plugins.captureScreen.kdespectacle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.exception.InvalidRequestException
import com.aallam.openai.api.exception.OpenAIErrorDetails
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIHost
import io.github.octestx.basic.multiplatform.common.exceptions.ConfigurationNotSavedException
import io.github.octestx.basic.multiplatform.common.utils.AIErrorType
import io.github.octestx.basic.multiplatform.common.utils.AIResult
import io.github.octestx.basic.multiplatform.common.utils.OS
import io.github.octestx.basic.multiplatform.common.utils.ojson
import io.github.octestx.krecall.plugins.basic.*
import io.klogging.noCoLogger
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.io.File


class OpenAIAPIOCR(metadata: PluginMetadata): AbsOCRPlugin(metadata), PluginAbilityInterfaces.SettingTabUI {
    companion object {
        val metadata = PluginMetadata(
            pluginId = "OCRByZhiPuPlugin",
            supportPlatform = setOf(OS.OperatingSystem.WIN, OS.OperatingSystem.LINUX, OS.OperatingSystem.MACOS, OS.OperatingSystem.OTHER),
            supportUI = true,
            mainClass = "io.github.octestx.krecall.plugins.impl.ocr.OCRByZhiPuPlugin"
        )
    }
    private val ologger = noCoLogger<OpenAIAPIOCR>()
    private val configFile = File(pluginDir, "config.json")
    @Volatile
    private lateinit var config: ScreenLanguageConverterByZhiPuPluginConfig

    @Serializable
    data class ScreenLanguageConverterByZhiPuPluginConfig(
        val baseUrl: String,
        val apiKey: String,
        val model: String,
        val systemMsg: String,
        val temperature: Double,
        val topP: Double,
        val frequencyPenalty: Double,
    )

    /**
     * @see
     */
    private val defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4/"
    private val defaultSystemMsg = """
        你是OCR后端，请将你看到的内容输出成文本,禁止重复说一个词
    """.trimIndent()
    private val defaultModel = "GLM-4V-Flash"

    override suspend fun recognize(screen: ByteArray): OCRResult {
        val imgBase64 = screen.encodeBase64()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(config.model),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = config.systemMsg
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = listOf(
                        ImagePart(imgBase64)
                    )
                ),
            ),
            temperature = config.temperature,
            topP = config.topP,
            frequencyPenalty = config.frequencyPenalty,
        )
        ologger.info { "ConvertingData..." }
        try {
            val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
            val msg = completion.choices[0].message.content!!
            ologger.info { "ConvertedData: $msg" }
            return OCRResult(msg, 1.0, listOf(), 0)
        } catch (e: Exception) {
            if (e is InvalidRequestException) {
                val detail = e.error.detail
                val fail = AIResult.Failed<String>(e, getErrorTypeByZhiPuAI(detail))
                ologger.error(e) { "InvalidRequestException: [detail=$detail, fail=$fail]" }
                throw e
            } else {
                //TODO 错误类型分类
                val fail = AIResult.Failed<String>(e, AIErrorType.UNKNOWN)
                ologger.error(e) { "ConvertDataError: ${e.message}, fail: $fail" }
                throw e
            }
        }
    }

    private val openAI by lazy { OpenAI(config.apiKey, host = OpenAIHost(config.baseUrl)) }
    override fun loadInner(context: PluginContext) {
        try {
            config = ojson.decodeFromString(configFile.readText())
        } catch (e: Throwable) {
            ologger.warn { "加载配置文件时遇到错误，已复原: ${configFile.absolutePath}" }
            configFile.renameTo(File(configFile.parentFile, "config.json.old"))
            //TODO remove private api key
            config = ScreenLanguageConverterByZhiPuPluginConfig(defaultBaseUrl, "2137bdde5a5344618ac99458a160430d.SQsjadVmdhLb5CgN", defaultModel, defaultSystemMsg, 0.1, 1.0, 2.0)
            configFile.writeText(ojson.encodeToString(config))
        }
        ologger.info { "Loaded" }
    }

    override fun selected() {}
    override fun unselected() {}
    private var savedConfig = MutableStateFlow(true)

    @Composable
    override fun UI() {
        val scope = rememberCoroutineScope()
        Column {
            var apiBaseUrl by remember { mutableStateOf(config.baseUrl) }
            OutlinedTextField(apiBaseUrl, {
                apiBaseUrl = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-BaseUrl")
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp))
            var apiKey by remember { mutableStateOf(config.apiKey) }
            OutlinedTextField(apiKey, {
                apiKey = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-Key")
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp))
            var model by remember { mutableStateOf(config.model) }
            OutlinedTextField(model, {
                model = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-Model")
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp))
            var sysMsg by remember { mutableStateOf(config.systemMsg) }
            OutlinedTextField(sysMsg, {
                sysMsg = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-SystemMessage")
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp))
            var temperatureStr by remember { mutableStateOf(config.temperature.toString()) }
            OutlinedTextField(temperatureStr, {
                temperatureStr = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-temperatureStr")
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp))
            var topPStr by remember { mutableStateOf(config.topP.toString()) }
            OutlinedTextField(topPStr, {
                topPStr = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-topP")
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp))
            var frequencyPenaltyStr by remember { mutableStateOf(config.frequencyPenalty.toString()) }
            OutlinedTextField(frequencyPenaltyStr, {
                frequencyPenaltyStr = it
                savedConfig.value = false
                _initialized.value = false
            }, label = {
                Text("API-frequencyPenalty")
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp))

            var saveText = "Save"
            Button(onClick = {
                try {
                    if (
                        apiKey.isNotEmpty() &&
                        model.isNotEmpty() &&
                        sysMsg.isNotEmpty() &&
                        temperatureStr.toDoubleOrNull() != null &&
                        topPStr.toDoubleOrNull() != null &&
                        frequencyPenaltyStr.toDoubleOrNull() != null
                    ) {
                        val newConfig = ScreenLanguageConverterByZhiPuPluginConfig(
                            apiBaseUrl,
                            apiKey,
                            model,
                            sysMsg,
                            temperatureStr.toDouble(),
                            topPStr.toDouble(),
                            frequencyPenaltyStr.toDouble(),
                        )
                        configFile.writeText(ojson.encodeToString(newConfig))
                        config = newConfig
                        scope.launch {
                            saveText = "Saved"
                            delay(3000)
                            saveText = "Save"
                        }
                        savedConfig.value = true
                        ologger.info { "Saved" }
                    }
                } catch (e: Throwable) {
                    ologger.error(e)
                }
            }, enabled = initialized.value.not()) {
                Text(saveText)
            }
        }
    }

    override suspend fun tryInitInner(): InitResult {
//        runBlocking { convert(File("/home/octest/Myself/tmp/Screenshot_20250301_234058.png").readBytes()) }
        val confined = config.apiKey.isNotEmpty() && config.model.isNotEmpty()
        if (savedConfig.value.not()) {
            return InitResult.Failed(ConfigurationNotSavedException())
        }
        if (confined.not()) return InitResult.Failed(IllegalArgumentException("需要补全参数"))
        ologger.info { "Initialized" }
        _initialized.value = true
        return InitResult.Success
    }

    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized


    /**
     * https://www.bigmodel.cn/dev/api/error-code/service-error
     */
    private fun getErrorTypeByZhiPuAI(details: OpenAIErrorDetails?): AIErrorType {
        if (details == null) return AIErrorType.UNKNOWN
        return when (details.code) {
            // 1000系列（API密钥相关）
            "1000" -> AIErrorType.INVALID_API_KEY    // 原第5位
            "1001" -> AIErrorType.INVALID_API_KEY    // 原第4位
            "1002" -> AIErrorType.INVALID_API_KEY    // 原第3位
            "1003" -> AIErrorType.INVALID_API_KEY    // 原第2位
            "1004" -> AIErrorType.INVALID_API_KEY    // 原第1位

            // 1110-1120系列（API密钥/配额）
            "1110" -> AIErrorType.INVALID_API_KEY    // 原第9位
            "1111" -> AIErrorType.INVALID_API_KEY    // 原第8位
            "1112" -> AIErrorType.INVALID_API_KEY    // 原第7位
            "1113" -> AIErrorType.API_QUOTA_EXHAUSTED// 原第10位
            "1120" -> AIErrorType.INVALID_API_KEY    // 原第6位

            // 1200系列（模型相关）
            "1211" -> AIErrorType.INVALID_MODEL      // 原第11位
            "1220" -> AIErrorType.INVALID_MODEL      // 原第12位
            "1221" -> AIErrorType.INVALID_MODEL      // 原第13位
            "1222" -> AIErrorType.INVALID_MODEL      // 原第14位
            "1261" -> AIErrorType.API_QUOTA_EXHAUSTED// 原第15位

            // 1300系列（请求相关）
            "1300" -> AIErrorType.REQUEST_INTERRUPTED// 原第16位
            "1301" -> AIErrorType.SENSITIVE_INFO     // 原第17位
            "1302" -> AIErrorType.API_RATE_LIMIT_CONCURRENCY// 原第18位
            "1304" -> AIErrorType.API_RATE_LIMIT_TOKEN// 原第19位
            "1305" -> AIErrorType.API_RATE_LIMIT_CONCURRENCY// 原第20位
            else -> AIErrorType.UNKNOWN
        }
    }

    override val settingTabName: String = "OCRByOpenAIAPI"

    @Composable
    override fun SettingTabUIShader() {
        UI()
    }
}