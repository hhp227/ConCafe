package com.hhp227.concafe.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = ConCafeColors.primary,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = ConCafeColors.primary
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
                        color = if (index == selectedIndex) ConCafeColors.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}
