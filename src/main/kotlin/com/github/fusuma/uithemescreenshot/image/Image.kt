package com.github.fusuma.uithemescreenshot.image

import com.github.fusuma.uithemescreenshot.model.UiTheme
import com.intellij.util.ui.ImageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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

fun saveImage(image: BufferedImage, UiTheme: UiTheme, screenshotTime: String) {
    val fileChooser = JFileChooser().apply {
        currentDirectory = File(System.getProperty("user.home") + File.separator + "Desktop")
        addChoosableFileFilter(FileNameExtensionFilter("PNG", "png"))

        val fileName = "screenshot_${screenshotTime}_${UiTheme.name.lowercase()}.png"
        setSelectedFile(File(fileName))
    }
    val result = fileChooser.showSaveDialog(null)

    if (result == JFileChooser.APPROVE_OPTION) {
        val selectedFile = fileChooser.selectedFile

        try {
            ImageIO.write(image, "png", selectedFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
