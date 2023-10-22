package io.github.fusuma.uithemescreenshot.model

enum class UiTheme(val nightYesNo: String) {
    LIGHT("no"), DARK("yes");

    fun toggle() = when (this) {
        LIGHT -> DARK
        DARK -> LIGHT
    }
}