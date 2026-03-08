package org.hhp227.concafe.presentation.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@Composable
fun ScrollableConCafeTabBar(
    labels: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFFFFBFD),
    onTabSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = Color(0xFFEF6797),
        edgePadding = 12.dp,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = Color(0xFFEF6797)
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
                        color = if (index == selectedIndex) Color(0xFFEF6797) else Color(0xFF777777),
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}
