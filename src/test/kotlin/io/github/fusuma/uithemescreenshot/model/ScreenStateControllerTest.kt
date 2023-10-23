package io.github.fusuma.uithemescreenshot.model

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.fusuma.uithemescreenshot.adb.AdbDeviceWrapper
import io.github.fusuma.uithemescreenshot.adb.AdbError
import kotlinx.coroutines.flow.*
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ScreenStateControllerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun `get deviceNameList on launch`() {
        // setup
        lateinit var stateController: ScreenStateController

        val expected = listOf("emulator1", "emulator2")

        // act
        rule.setContent {
            stateController = prepareState(
                getConnectedDeviceNames = { expected },
            )
        }
        rule.waitForIdle()

        // assert
        val actual = stateController.state.deviceNameList
        assertEquals(expected, actual)
    }

    @Test
    fun `toggle theme`() {
        // setup
        lateinit var stateController: ScreenStateController

        val history = mutableListOf<Unit?>()
        val mockDeviceWrapper = object : MockDeviceWrapper {
            var invokedCount = 0
            override suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long) {
                invokedCount++
                rule.waitForIdle()
            }
        }

        // act
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

        // assert
        assertEquals(
            listOf(null, Unit, null),
            history
        )
        assertEquals(
            1,
            mockDeviceWrapper.invokedCount
        )
    }

    @Test
    fun `screenshot one theme`() {
        lateinit var stateController: ScreenStateController

        // setup
        val initialTheme = UiTheme.LIGHT
        val initialState = ScreenState(
            isTakeBothTheme = false,
            lightScreenshot = ImageBitmap(0, 0),
            darkScreenshot = ImageBitmap(0, 0),
        )
        val dummyLightBitmap = ImageBitmap(0, 0)
        val history = mutableListOf<ScreenState>()
        val mockDeviceWrapper = object : MockDeviceWrapper {
            var invokedCurrentUiTheme = 0
            var invokedChangeUiThemeCount = 0
            var invokedScreenshotCount = 0
            override suspend fun getCurrentUiTheme(): UiTheme {
                invokedCurrentUiTheme++
                return initialTheme
            }
            override suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long) {
                invokedChangeUiThemeCount++
            }
            override fun screenshotFlow(
                scale: Float,
                isTakeBothTheme: Boolean,
                currentUiTheme: UiTheme
            ): Flow<ScreenshotResult> {
                invokedScreenshotCount++
                return flow {
                    emit(
                        ScreenshotResult(
                            theme = currentUiTheme,
                            image = dummyLightBitmap,
                            hasNextTarget = false
                        )
                    )
                }.onEach {
                    rule.waitForIdle()
                }.onCompletion {
                    rule.waitForIdle()
                }
            }
        }

        // act
        rule.setContent {
            stateController = prepareState(
                initialState = initialState,
                getDevice = { _ -> mockDeviceWrapper },
            )
            remember(stateController.state) {
                history.add(stateController.state)
            }
        }

        rule.waitForIdle()
        stateController.onTakeScreenshot()
        rule.waitForIdle()

        // assert
        val startScreenshotScate = initialState.copy(
            lightScreenshot = null,
            onScreenshot = ScreenshotTarget.LIGHT
        )
        val lightScreenshotDoneState = startScreenshotScate.copy(
            lightScreenshot = dummyLightBitmap
        )
        val screenshotDoneState = lightScreenshotDoneState.copy(
            onScreenshot = ScreenshotTarget.NOT_PROCESSING,
        )
        assertEquals(startScreenshotScate, history[1])
        assertEquals(lightScreenshotDoneState, history[2])
        assertEquals(screenshotDoneState, history[3])

        assertEquals(
            1,
            mockDeviceWrapper.invokedCurrentUiTheme
        )
        assertEquals(
            1,
            mockDeviceWrapper.invokedScreenshotCount
        )
        assertEquals(
            0,
            mockDeviceWrapper.invokedChangeUiThemeCount
        )
    }

    @Test
    fun `screenshot both theme`() {
        lateinit var stateController: ScreenStateController

        // setup
        val initialTheme = UiTheme.LIGHT
        val initialState = ScreenState(
            isTakeBothTheme = true,
            lightScreenshot = ImageBitmap(0, 0),
            darkScreenshot = ImageBitmap(0, 0),
        )
        val dummyLightBitmap = ImageBitmap(0, 0)
        val dummyDarkBitmap = ImageBitmap(0, 0)
        val history = mutableListOf<ScreenState>()
        val mockDeviceWrapper = object : MockDeviceWrapper {
            var invokedCurrentUiTheme = 0
            var invokedChangeUiThemeCount = 0
            var invokedScreenshotCount = 0
            override suspend fun getCurrentUiTheme(): UiTheme {
                invokedCurrentUiTheme++
                return initialTheme
            }
            override suspend fun changeUiTheme(uiTheme: UiTheme, sleepTime: Long) {
                invokedChangeUiThemeCount++
            }
            override fun screenshotFlow(
                scale: Float,
                isTakeBothTheme: Boolean,
                currentUiTheme: UiTheme
            ): Flow<ScreenshotResult> {
                invokedScreenshotCount++
                return flow {
                    emit(
                        ScreenshotResult(
                            theme = currentUiTheme,
                            image = dummyLightBitmap,
                            hasNextTarget = true
                        )
                    )
                    emit(
                        ScreenshotResult(
                            theme = currentUiTheme.toggle(),
                            image = dummyDarkBitmap,
                            hasNextTarget = false
                        )
                    )
                }.onEach {
                    rule.waitForIdle()
                }.onCompletion {
                    rule.waitForIdle()
                }
            }
        }

        // act
        rule.setContent {
            stateController = prepareState(
                initialState = initialState,
                getDevice = { _ -> mockDeviceWrapper },
            )
            remember(stateController.state) {
                history.add(stateController.state)
            }
        }

        rule.waitForIdle()
        stateController.onTakeScreenshot()
        rule.waitForIdle()

        // assert
        val startScreenshotState = initialState.copy(
            lightScreenshot = null,
            darkScreenshot = null,
            onScreenshot = ScreenshotTarget.BOTH
        )
        val lightScreenshotDoneState = startScreenshotState.copy(
            lightScreenshot = dummyLightBitmap
        )
        val darkScreenshotDoneState = lightScreenshotDoneState.copy(
            darkScreenshot = dummyDarkBitmap
        )
        val screenshotDoneState = darkScreenshotDoneState.copy(
            onScreenshot = ScreenshotTarget.NOT_PROCESSING,
        )

        assertEquals(startScreenshotState, history[1])
        assertEquals(lightScreenshotDoneState, history[2])
        assertEquals(darkScreenshotDoneState, history[3])
        assertEquals(screenshotDoneState, history[4])

        assertEquals(
            1,
            mockDeviceWrapper.invokedCurrentUiTheme
        )
        assertEquals(
            1,
            mockDeviceWrapper.invokedScreenshotCount
        )
        assertEquals(
            2,
            mockDeviceWrapper.invokedChangeUiThemeCount
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
) = useScreenState(
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