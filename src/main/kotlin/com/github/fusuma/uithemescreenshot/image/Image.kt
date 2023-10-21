package com.github.fusuma.uithemescreenshot.image

import com.android.SdkConstants.EXT_PNG
import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

suspend fun resizeImage(image: BufferedImage, scale: Float): BufferedImage {
    if (scale == 0f) return image

    return withContext(Dispatchers.IO) {
        val width = (image.width * scale).toInt()
        val height = (image.height * scale).toInt()

        val resizedAwtImage = ImageUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = resizedAwtImage.createGraphics()
        graphics.drawImage(image, 0, 0, width, height, null)
        graphics.dispose()

        resizedAwtImage
    }
}

fun saveImage(image: BufferedImage, uiTheme: UiTheme, screenshotTime: LocalDateTime, project: Project) {
    val descriptor = FileSaverDescriptor("Save Screenshot", "", EXT_PNG)
    val saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
    val fileName = "screenshot_${getTimeString(screenshotTime)}_${uiTheme.name.lowercase()}.png"
    val fileWrapper = saveFileDialog.save(
        Path.of(System.getProperty("user.home") + File.separator + "Desktop"),
        fileName
    )
    try {
        fileWrapper?.file?.let {
            ImageIO.write(image, "png", it)
        }
    } catch (e: Exception) {
        Messages.showErrorDialog(project, "Save failed.", "Save Screenshot")
        e.printStackTrace()
    }
}

private fun getTimeString(datetime: LocalDateTime): String {
    val pattern = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    return pattern.format(datetime)
}
