package com.hhp227.concafe.presentation.castedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.di.resolveGetCastDetailUseCase
import com.hhp227.concafe.di.resolveUpsertCastUseCase
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun CastEditScreen(
    cafeId: String? = null,
    castId: String? = null,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CastEditViewModel = viewModel(
        key = "cast-edit-${cafeId ?: "none"}-${castId ?: "new"}",
        factory = viewModelFactory {
            initializer {
                CastEditViewModel(
                    cafeId = cafeId,
                    castId = castId,
                    getCastDetailUseCase = resolveGetCastDetailUseCase(),
                    upsertCastUseCase = resolveUpsertCastUseCase()
                )
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CastEditEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }
    CastEditContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastEditContentScreen(
    uiState: CastEditUiState,
    onAction: (CastEditAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.screenTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CastEditAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { onAction(CastEditAction.ClickSave) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD1DC),
                            contentColor = Color(0xFF2B2330)
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2B2330)
                            )
                        } else {
                            Icon(Icons.Default.HowToReg, contentDescription = null)
                        }
                        Text(
                            text = uiState.saveButtonLabel,
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F5F6), Color(0xFFFFFBFD))
                    )
                )
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFEF6797))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        CompatImagePicker(
                            onImageSelected = { imageUrl ->
                                onAction(CastEditAction.SelectProfilePhoto(imageUrl))
                            }
                        ) { launchImagePicker ->
                            ProfilePhotoSection(
                                imageUrl = uiState.profileImageUrl,
                                onClick = {
                                    onAction(CastEditAction.ClickProfilePhoto)
                                    launchImagePicker()
                                }
                            )
                        }
                    }
                    uiState.infoMessage?.let { message ->
                        item {
                            InfoBanner(
                                message = message,
                                onDismiss = { onAction(CastEditAction.DismissInfoMessage) }
                            )
                        }
                    }
                    item {
                        ConCafeFormField(
                            label = "캐스트 이름",
                            value = uiState.castName,
                            onValueChange = { onAction(CastEditAction.ChangeCastName(it)) },
                            placeholder = "활동명을 입력해주세요"
                        )
                    }
                    item {
                        ConCafeFormField(
                            label = "컨셉 역할",
                            value = uiState.conceptRole,
                            onValueChange = { onAction(CastEditAction.ChangeConceptRole(it)) },
                            placeholder = "예: 리드 메이드, 티 마스터, 어프렌티스"
                        )
                    }
                    item {
                        ConCafeFormField(
                            label = "생일",
                            value = uiState.birthday,
                            onValueChange = { onAction(CastEditAction.ChangeBirthday(it)) },
                            placeholder = "MM / DD / YYYY",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color(0xFFB1A3AC)
                                )
                            }
                        )
                    }
                    item {
                        ConCafeFormField(
                            label = "소개 및 바이오",
                            value = uiState.introduction,
                            onValueChange = { onAction(CastEditAction.ChangeIntroduction(it)) },
                            placeholder = "성격, 특징, 특기를 소개해주세요...",
                            minLines = 4,
                            singleLine = false
                        )
                    }
                    item {
                        CompatImagePicker(
                            onImageSelected = { imageUrl ->
                                onAction(CastEditAction.AddGalleryImage(imageUrl))
                            }
                        ) { launchImagePicker ->
                            GallerySection(
                                galleryImages = uiState.galleryImages,
                                galleryLimitText = uiState.galleryLimitText,
                                galleryMaxCount = uiState.galleryMaxCount,
                                onAddClick = {
                                    onAction(CastEditAction.ClickAddGalleryPhoto)
                                    launchImagePicker()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePhotoSection(
    imageUrl: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.clickable(onClick = onClick),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFFFE3EC), Color(0xFFF8C5D7))
                            )
                        )
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        CompatImageDisplay(
                            imageUrl = imageUrl,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFFD1DC),
                    border = BorderStroke(2.dp, Color.White),
                    shadowElevation = 6.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = Color(0xFF2B2330)
                    )
                }
            }
            Text("캐스트 프로필 사진", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("탭해서 사진을 변경하세요", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8C7E87))
        }
    }
}

@Composable
private fun GallerySection(
    galleryImages: List<String>,
    galleryLimitText: String,
    galleryMaxCount: Int,
    onAddClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("갤러리 사진", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = Color(0xFF665A63))
            Text(galleryLimitText, style = MaterialTheme.typography.labelMedium, color = Color(0xFFEF6797), fontWeight = FontWeight.Bold)
        }
        CastGalleryGrid(
            galleryImages = galleryImages,
            galleryMaxCount = galleryMaxCount,
            onAddClick = onAddClick
        )
        Text(
            text = "캐스트 갤러리에는 최대 ${galleryMaxCount}장까지 등록할 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A8088)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CastGalleryGrid(
    galleryImages: List<String>,
    galleryMaxCount: Int,
    onAddClick: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        galleryImages.forEachIndexed { index, imageUrl ->
            CastGalleryImageTile(
                label = "이미지 ${index + 1}",
                imageUrl = imageUrl,
                index = index
            )
        }
        if (galleryImages.size < galleryMaxCount) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x1AFFD1DC))
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "사진 추가", tint = Color(0xFFEF6797))
            }
        }
    }
}

@Composable
private fun CastGalleryImageTile(
    label: String,
    imageUrl: String,
    index: Int
) {
    val gradients = listOf(
        listOf(Color(0xFFFFE6EE), Color(0xFFF7C9D8)),
        listOf(Color(0xFFFFD8E6), Color(0xFFFFEFF5)),
        listOf(Color(0xFFFFD9CF), Color(0xFFFFF0EA))
    )
    val colors = gradients[index % gradients.size]

    Box(
        modifier = Modifier
            .size(96.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.BottomStart
    ) {
        CompatImageDisplay(
            imageUrl = imageUrl,
            modifier = Modifier.fillMaxSize()
        )
        Surface(
            modifier = Modifier.padding(10.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.Black.copy(alpha = 0.32f)
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun InfoBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFF6D7),
        border = BorderStroke(1.dp, Color(0xFFF1D88D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5320)
            )
            TextButton(onClick = onDismiss) {
                Text("닫기", color = Color(0xFF6B5320), fontWeight = FontWeight.Bold)
            }
        }
    }
}
