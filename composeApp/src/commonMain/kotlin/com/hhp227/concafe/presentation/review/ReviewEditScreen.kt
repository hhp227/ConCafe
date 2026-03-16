package com.hhp227.concafe.presentation.review

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.di.resolveCreateReviewUseCase
import com.hhp227.concafe.di.resolveGetCafeDetailUseCase
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun ReviewEditScreen(
    cafeId: String? = null,
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: ReviewEditViewModel = viewModel(
        key = "review-edit-${cafeId ?: "unknown"}",
        factory = viewModelFactory {
            initializer {
                ReviewEditViewModel(
                    cafeId = cafeId,
                    getCafeDetailUseCase = resolveGetCafeDetailUseCase(),
                    createReviewUseCase = resolveCreateReviewUseCase()
                )
            }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                ReviewEditEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }
    ReviewEditContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewEditContentScreen(
    uiState: ReviewEditUiState,
    onAction: (ReviewEditAction) -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF8F5F6),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.screenTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(ReviewEditAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    if (uiState.isLoggedIn) {
                        TextButton(onClick = { onAction(ReviewEditAction.ClickSubmit) }) {
                            Text(
                                text = uiState.topActionLabel,
                                color = Color(0xFFEF6797),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.isLoggedIn) {
                Surface(
                    modifier = Modifier.navigationBarsPadding(),
                    color = Color.White.copy(alpha = 0.96f),
                    shadowElevation = 10.dp
                ) {
                    Button(
                        onClick = { onAction(ReviewEditAction.ClickSubmit) },
                        enabled = uiState.isSubmitEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD1DC),
                            contentColor = Color(0xFF2B2330),
                            disabledContainerColor = Color(0xFFF0D9E0),
                            disabledContentColor = Color(0xFF7F7078)
                        )
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF2B2330)
                            )
                        } else {
                            Text(
                                text = uiState.submitButtonLabel,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F5F6), Color(0xFFFFFBFD))
                    )
                )
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFEF6797))
                }
            } else {
                CafeInfoSection(uiState = uiState)
            }
            RatingSection(uiState = uiState, onAction = onAction)
            ReviewFormSection(uiState = uiState, onAction = onAction)
            PhotoSection(uiState = uiState, onAction = onAction)
            uiState.infoMessage?.let { message ->
                InfoBanner(
                    message = message,
                    onDismiss = { onAction(ReviewEditAction.DismissInfoMessage) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CafeInfoSection(uiState: ReviewEditUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x1AFFD1DC))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFFFE5EE), Color(0xFFF4C6D5))
                    )
                )
                .border(
                    width = 2.dp,
                    color = Color(0x4DFFD1DC),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Cafe",
                color = Color(0xFF8A5C71),
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (uiState.isVisitVerified) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFEF6797),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "방문 인증됨",
                        color = Color(0xFFEF6797),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = uiState.cafeName,
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF24161E),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = uiState.cafeAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7A707A)
            )
        }
    }
}

@Composable
private fun RatingSection(
    uiState: ReviewEditUiState,
    onAction: (ReviewEditAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "카페 경험은 어떠셨나요?",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2B2330),
            fontWeight = FontWeight.Bold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 1..ReviewEditUiState.maximumRating) {
                val isSelected = index <= uiState.rating
                Icon(
                    imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "평점 $index",
                    tint = if (isSelected) Color(0xFFFFC94D) else Color(0x33EF6797),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onAction(ReviewEditAction.SelectRating(index)) }
                )
            }
        }
        Text(
            text = uiState.ratingMessage,
            color = Color(0xFFEF6797),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PhotoSection(
    uiState: ReviewEditUiState,
    onAction: (ReviewEditAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "사진 등록 (선택)",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2B2330),
            fontWeight = FontWeight.Bold
        )
        CompatImagePicker(
            onImageSelected = { imageUrl ->
                onAction(ReviewEditAction.SelectPhoto(imageUrl))
            }
        ) { launchImagePicker ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFFFD8E6), Color(0xFFFFEFF5))
                        )
                    )
                    .clickable {
                        onAction(ReviewEditAction.ClickAddPhoto)
                        launchImagePicker()
                    }
            ) {
                if (uiState.photoImageUrl.isNullOrBlank()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color(0xFF8B5164),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "리뷰 사진 추가",
                            color = Color(0xFF5A4954),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    CompatImageDisplay(
                        imageUrl = uiState.photoImageUrl,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (!uiState.photoImageUrl.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clickable { onAction(ReviewEditAction.RemovePhoto) },
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White
                    ) {
                        Text(
                            text = "제거",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8B5164),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Text(
            text = "리뷰 사진은 선택사항이며 최대 1장만 등록할 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF8A8088)
        )
    }
}

@Composable
private fun ReviewFormSection(
    uiState: ReviewEditUiState,
    onAction: (ReviewEditAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ConCafeFormField(
            label = "상세 리뷰",
            value = uiState.content,
            onValueChange = { onAction(ReviewEditAction.ChangeReviewText(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            placeholder = "카페 분위기, 맛, 서비스 등에 대한 솔직한 경험을 남겨주세요 (최소 10자 이상)",
            minLines = 8,
            singleLine = false
        )
        Text(
            text = "${uiState.reviewLength}/${ReviewEditUiState.minimumReviewLength}자 이상",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            color = if (uiState.reviewLength >= ReviewEditUiState.minimumReviewLength) {
                Color(0xFF2E9E5B)
            } else {
                Color(0xFF9A8D95)
            },
            style = MaterialTheme.typography.labelMedium
        )
        if (uiState.availableCastTags.isNotEmpty()) {
            CastTagSection(
                availableCastTags = uiState.availableCastTags,
                taggedCastIds = uiState.taggedCastIds,
                onToggle = { onAction(ReviewEditAction.ToggleCastTag(it)) }
            )
        }
        AtmosphereQuestionCard(
            isSelected = uiState.atmosphereAnswer,
            onSelect = { onAction(ReviewEditAction.SelectAtmosphereAnswer(it)) }
        )
    }
}

@Composable
private fun CastTagSection(
    availableCastTags: List<ReviewEditUiState.CastTag>,
    taggedCastIds: List<String>,
    onToggle: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "함께 언급한 캐스트",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF665A63),
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableCastTags.forEach { cast ->
                val selected = taggedCastIds.contains(cast.id)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) Color(0xFFFFD1DC) else Color(0x1AFFD1DC))
                        .border(
                            width = 1.dp,
                            color = if (selected) Color(0xFFFFD1DC) else Color(0x33FFD1DC),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { onToggle(cast.id) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = cast.name,
                        color = if (selected) Color(0xFF2B2330) else Color(0xFF6E6169),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AtmosphereQuestionCard(
    isSelected: Boolean?,
    onSelect: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8F5F6))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFD1DC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mood,
                    contentDescription = null,
                    tint = Color(0xFFEF6797)
                )
            }
            Text(
                text = "분위기가 좋았나요?",
                color = Color(0xFF2B2330),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnswerChip(
                label = "네",
                selected = isSelected == true,
                onClick = { onSelect(true) }
            )
            AnswerChip(
                label = "아니요",
                selected = isSelected == false,
                onClick = { onSelect(false) }
            )
        }
    }
}

@Composable
private fun AnswerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color(0xFFFFD1DC) else Color.White)
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFFFFD1DC) else Color(0xFFD9CFD5),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF2B2330) else Color(0xFF8E7F88),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InfoBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF6D7))
            .border(
                width = 1.dp,
                color = Color(0xFFF1D88D),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = Color(0xFF6B5320),
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(
            onClick = onDismiss,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = "닫기",
                color = Color(0xFF6B5320),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
