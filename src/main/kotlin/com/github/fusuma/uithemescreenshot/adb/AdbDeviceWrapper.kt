package com.github.fusuma.uithemescreenshot.adb

import androidx.compose.ui.graphics.toComposeImageBitmap
import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver
import com.android.ddmlib.NullOutputReceiver
import com.android.ddmlib.TimeoutException
import com.github.fusuma.uithemescreenshot.image.resizeImage
import com.github.fusuma.uithemescreenshot.model.ScreenshotResult
import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.github.fusuma.uithemescreenshot.receiver.UIThemeDetectReceiver
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
    suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long)
    suspend fun getCurrentUiTheme() : UiTheme
}

enum class AdbError {
    TIMEOUT, NOT_FOUND
}

class AdbWrapperImpl(
    private val device: IDevice?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AdbDeviceWrapper {
    private val _errorFlow = MutableSharedFlow<AdbError>()
    override val errorFlow = _errorFlow.asSharedFlow()

    override fun screenshotFlow(scale: Float, isTakeBothTheme: Boolean, currentUiTheme: UiTheme): Flow<ScreenshotResult> {
        return if (isTakeBothTheme) {
            takeScreenshots(scale, currentUiTheme)
        } else {
            takeScreenshot(scale, currentUiTheme, null)
        }
    }

    override suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long) {
        withContext(dispatcher) {
            executeCmd(
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
            takeScreenshot(scale, targetOrder.first, targetOrder.second),
            takeScreenshot(scale, targetOrder.second, null),
        ).flattenConcat()
    }

    private fun takeScreenshot(scale: Float, theme: UiTheme, nextTargetTheme: UiTheme?) : Flow<ScreenshotResult> = flow {
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
                    nextTargetTheme
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
            executeCmd(
                Cmd(
                    "cmd uimode night",
                    receiver,
                )
            )
            requireNotNull(receiver.currentUiTheme())
        }
    }

    private suspend fun executeCmd(cmd: Cmd) {
        if (device == null) {
            _errorFlow.tryEmit(AdbError.NOT_FOUND)
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