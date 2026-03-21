package com.hhp227.concafe.presentation.main.checkin

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hhp227.concafe.domain.model.CheckInCafeSummary

@Composable
expect fun CheckInCafeMap(
    cafes: List<CheckInCafeSummary>,
    onCafeClick: (String) -> Unit,
    modifier: Modifier = Modifier
)
