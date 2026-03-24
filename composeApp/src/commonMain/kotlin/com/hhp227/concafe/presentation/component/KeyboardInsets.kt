package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier

fun Modifier.keyboardBottomInsets(): Modifier {
    return this
        .navigationBarsPadding()
        .imePadding()
}
