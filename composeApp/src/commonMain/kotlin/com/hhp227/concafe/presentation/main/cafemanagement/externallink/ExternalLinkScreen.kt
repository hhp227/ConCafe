package com.hhp227.concafe.presentation.main.cafemanagement.externallink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.externallink_title
import concafe.composeapp.generated.resources.signin_back_content_description
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalLinkScreen(
    title: String,
    url: String,
    onNavigationAction: (NavigationAction) -> Unit
) {
    val viewModel = viewModel<ExternalLinkViewModel>(
        key = "external-link-$title-$url",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<ExternalLinkViewModel> { parametersOf(title, url) } }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                ExternalLinkEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        uiState.displayTitle.ifBlank {
                            stringResource(Res.string.externallink_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onAction(ExternalLinkAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.signin_back_content_description)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            ExternalLinkWebView(
                url = uiState.url,
                modifier = Modifier
                    .fillMaxSize()
            )
        }
    }
}
