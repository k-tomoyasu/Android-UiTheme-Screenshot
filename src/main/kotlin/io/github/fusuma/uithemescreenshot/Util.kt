package io.github.fusuma.uithemescreenshot

import androidx.compose.ui.Modifier

inline fun Modifier.ifTrue(predicate: Boolean, builder: () -> Modifier) =
    then(
        if (predicate) builder() else Modifier
    )