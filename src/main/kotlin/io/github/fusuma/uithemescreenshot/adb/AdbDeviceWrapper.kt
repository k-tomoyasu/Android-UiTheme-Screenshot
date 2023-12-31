package io.github.fusuma.uithemescreenshot.adb

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver
import com.android.ddmlib.NullOutputReceiver
import com.android.ddmlib.TimeoutException
import io.github.fusuma.uithemescreenshot.image.resizeImage
import io.github.fusuma.uithemescreenshot.model.ScreenshotResult
import io.github.fusuma.uithemescreenshot.model.UiTheme
import io.github.fusuma.uithemescreenshot.receiver.UIThemeDetectReceiver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

interface AdbDeviceWrapper {
    val errorFlow: Flow<AdbError>
    fun screenshotFlow(
        scale: Float,
        isTakeBothTheme: Boolean,
        currentUiTheme: UiTheme
    ) : Flow<ScreenshotResult>
    val hasDevice: Boolean
    suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long)
    suspend fun getCurrentUiTheme() : UiTheme
}

enum class AdbError {
    TIMEOUT, NOT_FOUND
}

class AdbDeviceWrapperImpl(
    private val device: IDevice?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AdbDeviceWrapper {
    private val _errorFlow = MutableSharedFlow<AdbError>()
    override val errorFlow = _errorFlow.asSharedFlow()

    override val hasDevice = device != null

    override fun screenshotFlow(scale: Float, isTakeBothTheme: Boolean, currentUiTheme: UiTheme): Flow<ScreenshotResult> {
        return if (isTakeBothTheme) {
            takeScreenshots(scale, currentUiTheme)
        } else {
            takeScreenshot(scale, currentUiTheme, false)
        }
    }

    override suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long) {
        withContext(dispatcher) {
            runCmd(
                Cmd(
                    "cmd uimode night ${uiTheme.nightYesNo}",
                    NullOutputReceiver()
                )
            )
            delay(sleepTime)
        }
    }

    @OptIn(FlowPreview::class)
    private fun takeScreenshots(
        scale: Float,
        currentUiTheme: UiTheme,
    ): Flow<ScreenshotResult> {
        val targetOrder = currentUiTheme to currentUiTheme.toggle()
        return flowOf(
            takeScreenshot(scale, targetOrder.first, true),
            takeScreenshot(scale, targetOrder.second, false),
        ).flattenConcat()
    }

    private fun takeScreenshot(scale: Float, theme: UiTheme, hasNextTarget: Boolean) : Flow<ScreenshotResult> = flow {
        if (device == null) {
            _errorFlow.emit(AdbError.NOT_FOUND)
            return@flow
        }
        try {
            val screenshot = device.getScreenshot(
                10L,
                TimeUnit.SECONDS
            ).asBufferedImage()
            emit(
                ScreenshotResult(
                    theme,
                    resizeImage(screenshot, scale).toComposeImageBitmap(),
                    hasNextTarget
                )
            )
        } catch (e: TimeoutException) {
            _errorFlow.tryEmit(AdbError.TIMEOUT)
        } catch (e: AdbCommandRejectedException) {
            _errorFlow.emit(AdbError.NOT_FOUND)
        }
    }.flowOn(dispatcher)

   override suspend fun getCurrentUiTheme() : UiTheme {
        return withContext(dispatcher) {
            val receiver = UIThemeDetectReceiver()
            runCmd(
                Cmd(
                    "cmd uimode night",
                    receiver,
                )
            )
            requireNotNull(receiver.currentUiTheme())
        }
    }

    private suspend fun runCmd(cmd: Cmd) {
        if (device == null) {
            _errorFlow.emit(AdbError.NOT_FOUND)
            return
        }
        try {
            device.executeShellCommand(
                cmd.command,
                cmd.receiver,
                10L,
                TimeUnit.SECONDS
            )
        } catch (e: TimeoutException) {
            _errorFlow.emit(AdbError.TIMEOUT)
        } catch (e: AdbCommandRejectedException) {
            _errorFlow.emit(AdbError.NOT_FOUND)
        }
    }

    data class Cmd(
        val command: String,
        val receiver: IShellOutputReceiver
    )
}