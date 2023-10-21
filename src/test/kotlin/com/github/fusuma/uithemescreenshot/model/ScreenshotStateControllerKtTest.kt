package com.github.fusuma.uithemescreenshot.model

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import com.github.fusuma.uithemescreenshot.adb.AdbError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ScreenshotStateControllerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `get deviceNameList on launch`() {
        val expected = listOf("emulator1", "emulator2")

        lateinit var stateController: ScreenshotStateController

        rule.setContent {
            stateController = prepareState(
                getConnectedDeviceNames = { expected },
                getDevice = { _ -> object : MockDeviceWrapper {} },
                getLocalDateTime = { LocalDateTime.now() },
                saveImage = { _: BufferedImage, _: UiTheme, _: LocalDateTime -> }
            )
        }
        rule.waitForIdle()
        val actual = stateController.state.deviceNameList
        assertEquals(expected, actual)
    }

    @Test
    fun `toggle theme`() {
        lateinit var stateController: ScreenshotStateController

        val history = mutableListOf<Unit?>()
        val mockDeviceWrapper = object : MockDeviceWrapper {
            var invokedCount = 0
            override suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long) {
                invokedCount++
                rule.waitForIdle()
            }
        }

        rule.setContent {
            stateController = prepareState(
                getDevice = { _ -> mockDeviceWrapper },
            )
            remember(stateController.state.onToggleTheme) {
                history.add(stateController.state.onToggleTheme)
            }
        }
        rule.waitForIdle()
        stateController.onToggleTheme()
        rule.waitForIdle()
        assertEquals(
            listOf(null, Unit, null),
            history
        )
        assertEquals(
            1,
            mockDeviceWrapper.invokedCount
        )
    }
}

@Composable
private fun prepareState(
    initialState: ScreenState = ScreenState(),
    getConnectedDeviceNames: () -> List<String> = { emptyList() },
    getDevice: (Int) -> AdbDeviceWrapper = { _ -> object : MockDeviceWrapper {} },
    getLocalDateTime: () -> LocalDateTime = { LocalDateTime.now() },
    saveImage: (BufferedImage, UiTheme, LocalDateTime) -> Unit = { _: BufferedImage, _: UiTheme, _: LocalDateTime -> }
) = useScreenshotScreenState(
    initialState = initialState,
    getConnectedDeviceNames = getConnectedDeviceNames,
    getDevice = getDevice,
    getLocalDateTime = getLocalDateTime,
    saveImage = saveImage,
)

interface MockDeviceWrapper : AdbDeviceWrapper {
    override val errorFlow: Flow<AdbError> get() = emptyFlow()
    override val hasDevice get() =  true

    override fun screenshotFlow(
        scale: Float,
        isTakeBothTheme: Boolean,
        currentUiTheme: UiTheme
    ): Flow<ScreenshotResult> = emptyFlow()

    override suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long) {
    }

    override suspend fun getCurrentUiTheme(): UiTheme {
        return UiTheme.LIGHT
    }
}