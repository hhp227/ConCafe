package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.keyboardBottomInsets(): Modifier {
    return composed {
        windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    }
}
