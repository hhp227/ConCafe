package com.hhp227.concafe.presentation.community.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext

@Composable
fun PostEditScreen(
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: PostEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<PostEditViewModel>() }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                PostEditEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }
    CompatImagePicker(
        onImageSelected = { imageUrl ->
            viewModel.onAction(PostEditAction.AddImage(imageUrl))
        }
    ) { launchPicker ->
        PostEditContentScreen(
            uiState = uiState,
            onAction = { action ->
                if (action == PostEditAction.ClickAddImage) launchPicker()
                viewModel.onAction(action)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostEditContentScreen(
    uiState: PostEditUiState,
    onAction: (PostEditAction) -> Unit
) {
    val pink = colorFromHex("EF6797")
    val softPink = colorFromHex("FFD1DC")
    val textColor = colorFromHex("2B2330")

    Scaffold(
        containerColor = colorFromHex("F8F5F6"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.post_edit_screen_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(PostEditAction.ClickBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = textColor
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onAction(PostEditAction.ClickSubmit) },
                        enabled = uiState.canSubmit && !uiState.isSubmitting
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = pink,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.post_edit_submit),
                                color = if (uiState.canSubmit) pink else colorFromHex("B1A3AC"),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            uiState.infoMessage?.let { message ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colorFromHex("FFF6D7"))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp,
                            color = colorFromHex("6B5320")
                        )
                        TextButton(onClick = { onAction(PostEditAction.DismissInfoMessage) }) {
                            Text(stringResource(Res.string.common_close), color = colorFromHex("6B5320"), fontSize = 12.sp)
                        }
                    }
                }
            }
            ConCafeFormField(
                label = stringResource(Res.string.post_edit_title_label),
                value = uiState.title,
                onValueChange = { onAction(PostEditAction.ChangeTitle(it)) },
                placeholder = stringResource(Res.string.post_edit_title_placeholder),
                singleLine = true
            )
            ConCafeFormField(
                label = stringResource(Res.string.post_edit_content_label),
                value = uiState.content,
                onValueChange = { onAction(PostEditAction.ChangeContent(it)) },
                placeholder = stringResource(Res.string.post_edit_content_placeholder),
                singleLine = false,
                minLines = 6
            )
            ImageSection(
                imageUrls = uiState.imageUrls,
                imageMaxCount = uiState.imageMaxCount,
                softPink = softPink,
                pink = pink,
                onAddImage = { onAction(PostEditAction.ClickAddImage) },
                onRemoveImage = { index -> onAction(PostEditAction.RemoveImage(index)) }
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ImageSection(
    imageUrls: List<String>,
    imageMaxCount: Int,
    softPink: Color,
    pink: Color,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.post_edit_image_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorFromHex("665A63")
            )
            Text(
                text = stringResource(Res.string.post_edit_image_limit, imageUrls.size, imageMaxCount),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = pink
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            imageUrls.forEachIndexed { index, imageUrl ->
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    CompatImageDisplay(
                        imageUrl = imageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { onRemoveImage(index) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(22.dp)
                            .background(Color.Black.copy(alpha = 0.52f), shape = RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            if (imageUrls.size < imageMaxCount) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(softPink.copy(alpha = 0.15f))
                        .clickable(onClick = onAddImage),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.post_edit_add_image),
                        tint = pink,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.post_edit_image_guide, imageMaxCount),
            fontSize = 12.sp,
            color = colorFromHex("8A8088")
        )
    }
}
