package com.hhp227.concafe.presentation.main.checkin.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CheckInCafeSummary
import com.hhp227.concafe.presentation.component.CheckInCafeMap
import com.hhp227.concafe.presentation.component.CheckInMapCameraTarget
import com.hhp227.concafe.presentation.main.explore.ExploreUiState
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    initialRegionKey: String? = null,
    viewModel: MapViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<MapViewModel>() }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialRegionKey) {
        viewModel.initializeRegion(initialRegionKey)
    }
    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                MapEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is MapEvent.NavigateToCafe -> onNavigationAction(NavigationAction.NavigateToCafe(event.id))
            }
        }
    }
    val filteredCafes = rememberFilteredCafes(
        cafes = uiState.mapCafes,
        selectedRegion = uiState.selectedRegion,
        userCityKey = uiState.userCityKey,
        searchQuery = uiState.searchQuery
    )
    val isRegionMenuExpanded = remember { mutableStateOf(false) }
    val isSearchActive = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        CheckInCafeMap(
            cafes = filteredCafes,
            onCafeClick = { viewModel.onAction(MapAction.ClickCafe(it)) },
            showCheckInButton = false,
            cameraTarget = resolveCheckInMapCameraTarget(uiState.selectedRegion),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.36f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSearchActive.value) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White.copy(alpha = 0.94f),
                        tonalElevation = 0.dp
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onAction(MapAction.UpdateSearchQuery(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("카페명 또는 지역 검색") },
                            leadingIcon = {
                                IconButton(onClick = { isSearchActive.value = false }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null
                                    )
                                }
                            },
                            trailingIcon = if (uiState.searchQuery.isNotBlank()) {
                                {
                                    IconButton(onClick = { viewModel.onAction(MapAction.UpdateSearchQuery("")) }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = null
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "컨셉 카페 지도",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${filteredCafes.size}곳 표시 중",
                                    color = Color.White.copy(alpha = 0.84f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        },
                        navigationIcon = {
                            MapToolbarIconButton(onClick = { viewModel.onAction(MapAction.ClickBack) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            MapToolbarIconButton(onClick = { isSearchActive.value = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredCafes.size}곳 표시 중",
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Box {
                        Surface(
                            color = Color.White.copy(alpha = 0.94f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.widthIn(min = 112.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (uiState.selectedRegion == ExploreUiState.RegionFilter.ALL) "근처" else uiState.selectedRegion.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(
                                    onClick = { isRegionMenuExpanded.value = true },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = isRegionMenuExpanded.value,
                            onDismissRequest = { isRegionMenuExpanded.value = false }
                        ) {
                            ExploreUiState.RegionFilter.entries.forEach { region ->
                                DropdownMenuItem(
                                    text = {
                                        Text(if (region == ExploreUiState.RegionFilter.ALL) "근처 주요 카페" else region.label)
                                    },
                                    onClick = {
                                        viewModel.onAction(MapAction.UpdateRegion(region))
                                        isRegionMenuExpanded.value = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        uiState.errorMessage?.let { message ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.94f)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MapToolbarIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.36f)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(40.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun rememberFilteredCafes(
    cafes: List<CheckInCafeSummary>,
    selectedRegion: ExploreUiState.RegionFilter,
    userCityKey: String?,
    searchQuery: String
): List<CheckInCafeSummary> {
    val regionFiltered = when {
        selectedRegion != ExploreUiState.RegionFilter.ALL -> cafes.filter { cafe ->
            cafe.matchesRegion(selectedRegion)
        }
        else -> cafes.filter { cafe ->
            cafe.matchesNearbyCity(userCityKey)
        }
    }
    val normalizedQuery = searchQuery.trim().lowercase()
    if (normalizedQuery.isBlank()) return regionFiltered
    return regionFiltered.filter { cafe ->
        cafe.name.lowercase().contains(normalizedQuery) ||
            cafe.locationLabel.lowercase().contains(normalizedQuery)
    }
}

private fun CheckInCafeSummary.matchesRegion(region: ExploreUiState.RegionFilter): Boolean {
    val normalizedLocation = locationLabel.lowercase()
    return normalizedLocation.contains(region.key) || normalizedLocation.contains(region.label.lowercase())
}

private fun CheckInCafeSummary.matchesNearbyCity(cityKey: String?): Boolean {
    return matchesRegion(resolveNearbyRegion(cityKey))
}

private fun resolveNearbyRegion(cityKey: String?): ExploreUiState.RegionFilter {
    val normalizedCityKey = cityKey?.trim()?.lowercase()
    return ExploreUiState.RegionFilter.entries
        .firstOrNull { it.key == normalizedCityKey }
        ?: ExploreUiState.RegionFilter.SEOUL
}

private fun resolveCheckInMapCameraTarget(region: ExploreUiState.RegionFilter): CheckInMapCameraTarget? {
    return when (region) {
        ExploreUiState.RegionFilter.ALL -> null
        ExploreUiState.RegionFilter.SEOUL -> CheckInMapCameraTarget(37.5665, 126.9780, 12.5f)
        ExploreUiState.RegionFilter.BUSAN -> CheckInMapCameraTarget(35.1796, 129.0756, 12.0f)
        ExploreUiState.RegionFilter.DAEGU -> CheckInMapCameraTarget(35.8714, 128.6014, 12.0f)
        ExploreUiState.RegionFilter.TOKYO -> CheckInMapCameraTarget(35.6762, 139.6503, 12.0f)
        ExploreUiState.RegionFilter.OSAKA -> CheckInMapCameraTarget(34.6937, 135.5023, 12.0f)
        ExploreUiState.RegionFilter.YOKOHAMA -> CheckInMapCameraTarget(35.4437, 139.6380, 12.0f)
    }
}
