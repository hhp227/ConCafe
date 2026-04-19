package com.hhp227.concafe.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun ConCafeTabBar(
    labels: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onTabSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = colorFromHex("FFFBFD"),
        contentColor = colorFromHex("EF6797"),
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = colorFromHex("EF6797")
            )
        },
        divider = {}
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = label,
                        color = if (index == selectedIndex) colorFromHex("EF6797") else colorFromHex("777777"),
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}
