package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.goyangdeogyang_bold
import concafe.composeapp.generated.resources.goyangdeogyang_extrabold
import org.jetbrains.compose.resources.Font

@Composable
fun goyangFont(): FontFamily {
    return FontFamily(
        Font(Res.font.goyangdeogyang_bold),
        Font(Res.font.goyangdeogyang_extrabold, weight = FontWeight.Bold)
    )
}