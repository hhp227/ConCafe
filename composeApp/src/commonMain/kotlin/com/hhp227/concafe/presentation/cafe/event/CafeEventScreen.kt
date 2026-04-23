package com.hhp227.concafe.presentation.cafe.event

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CafeEventManagementItem
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.ImageDisplaySize
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cafe_error_retry_prompt
import concafe.composeapp.generated.resources.noticeevent_info_event_load_failed
import concafe.composeapp.generated.resources.noticeevent_tab_event
import concafe.composeapp.generated.resources.noticeevent_validation_event_required
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeEventScreen(
    cafeId: String,
    eventId: String,
    viewModel: CafeEventViewModel = viewModel(
        key = "$cafeId:$eventId",
        factory = viewModelFactory {
            initializer {
                GlobalContext.get().get<CafeEventViewModel> {
                    parametersOf(cafeId, eventId)
                }
            }
        }
    ),
    onNavigationAction: (NavigationAction) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CafeEventEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.noticeevent_tab_event)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(CafeEventAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colorFromHex("EF6797")
                )
                uiState.event != null -> CafeEventDetailContent(
                    event = requireNotNull(uiState.event),
                    modifier = Modifier.fillMaxSize()
                )
                else -> CafeEventErrorContent(
                    message = when (uiState.errorMessage) {
                        "Event not found." -> stringResource(Res.string.noticeevent_validation_event_required)
                        else -> stringResource(Res.string.noticeevent_info_event_load_failed)
                    },
                    onRetry = { viewModel.onAction(CafeEventAction.Retry) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun CafeEventDetailContent(
    event: CafeEventManagementItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        CafeEventHeroImage(event)
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(100),
                color = colorFromHex("FDE7EF")
            ) {
                Text(
                    text = event.statusLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = colorFromHex("9E2E5C"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = event.periodText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = event.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun CafeEventHeroImage(event: CafeEventManagementItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (event.imageUrl.isNotBlank()) {
            BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                CompatImageDisplay(
                    imageUrl = event.imageUrl,
                    modifier = Modifier.size(maxWidth, maxHeight),
                    displaySize = ImageDisplaySize.FULL,
                    applyRoundedClip = false
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(listOf(colorFromHex("FDE7EF"), colorFromHex("FCCFDF"))))
            )
        }
    }
}

@Composable
private fun CafeEventErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.cafe_error_retry_prompt))
        }
    }
}
