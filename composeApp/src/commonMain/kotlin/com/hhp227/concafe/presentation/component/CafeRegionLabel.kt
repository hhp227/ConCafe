package com.hhp227.concafe.presentation.component

import androidx.compose.runtime.Composable
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.region_busan
import concafe.composeapp.generated.resources.region_daegu
import concafe.composeapp.generated.resources.region_osaka
import concafe.composeapp.generated.resources.region_seoul
import concafe.composeapp.generated.resources.region_tokyo
import concafe.composeapp.generated.resources.region_yokohama
import org.jetbrains.compose.resources.stringResource

@Composable
fun localizedRegionCity(rawCity: String): String {
    val normalized = rawCity.trim()
    if (normalized.isEmpty()) {
        return normalized
    }
    return when (normalized.lowercase()) {
        "seoul" -> stringResource(Res.string.region_seoul)
        "busan" -> stringResource(Res.string.region_busan)
        "daegu" -> stringResource(Res.string.region_daegu)
        "tokyo" -> stringResource(Res.string.region_tokyo)
        "osaka" -> stringResource(Res.string.region_osaka)
        "yokohama" -> stringResource(Res.string.region_yokohama)
        else -> normalized
    }
}
