package com.github.fusuma.uithemescreenshot.receiver

import com.android.ddmlib.IShellOutputReceiver
import com.android.ddmlib.MultiLineReceiver
import com.github.fusuma.uithemescreenshot.model.UiTheme

interface UiThemeDetector : IShellOutputReceiver {
    fun currentUiTheme() : UiTheme?
}

class UIThemeDetectReceiver: MultiLineReceiver(), UiThemeDetector {
        private var uiTheme : UiTheme? = null
        override fun isCancelled(): Boolean {
            return false
        }
        override fun processNewLines(lines: Array<out String>) {
            uiTheme = when {
                lines.any {
                    it.contains("Night mode") && it.contains("yes")
                } -> UiTheme.DARK
                lines.any {
                    it.contains("Night mode") && it.contains("no")
                } -> UiTheme.LIGHT
                else -> return
            }
        }

        override fun currentUiTheme() = uiTheme
}