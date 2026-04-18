package com.hhp227.concafe.presentation.review

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.keyboardBottomInsets
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.common_close
import concafe.composeapp.generated.resources.reviewedit_accessibility_back
import concafe.composeapp.generated.resources.reviewedit_atmosphere_negative
import concafe.composeapp.generated.resources.reviewedit_atmosphere_positive
import concafe.composeapp.generated.resources.reviewedit_atmosphere_question
import concafe.composeapp.generated.resources.reviewedit_cast_tag_title
import concafe.composeapp.generated.resources.reviewedit_photo_add
import concafe.composeapp.generated.resources.reviewedit_photo_helper
import concafe.composeapp.generated.resources.reviewedit_photo_remove
import concafe.composeapp.generated.resources.reviewedit_photo_section_title
import concafe.composeapp.generated.resources.reviewedit_rating_accessibility
import concafe.composeapp.generated.resources.reviewedit_rating_question
import concafe.composeapp.generated.resources.reviewedit_review_detail_hint
import concafe.composeapp.generated.resources.reviewedit_review_detail_title
import concafe.composeapp.generated.resources.reviewedit_review_length
import concafe.composeapp.generated.resources.reviewedit_verified_visit
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
fun ReviewEditScreen(
    cafeId: String? = null,
    reviewId: String? = null,
    onNavigationAction: (NavigationAction) -> Unit = {},
    viewModel: ReviewEditViewModel = viewModel(
        key = "review-edit-${cafeId ?: "unknown"}-${reviewId ?: "new"}",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<ReviewEditViewModel> { parametersOf(cafeId, reviewId) } }
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
        containerColor = MaterialTheme.colorScheme.background,
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.reviewedit_accessibility_back)
                        )
                    }
                },
                actions = {
                    if (uiState.isLoggedIn) {
                        TextButton(onClick = { onAction(ReviewEditAction.ClickSubmit) }) {
                            Text(
                                text = uiState.topActionLabel,
                                color = colorFromHex("EF6797"),
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
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .keyboardBottomInsets()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = { onAction(ReviewEditAction.ClickSubmit) },
                            enabled = uiState.isSubmitEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorFromHex("FFD1DC"),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                disabledContainerColor = colorFromHex("F0D9E0"),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                    CircularProgressIndicator(color = colorFromHex("EF6797"))
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
                        colors = listOf(colorFromHex("FFE5EE"), colorFromHex("F4C6D5"))
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
                color = colorFromHex("8A5C71"),
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
                        tint = colorFromHex("EF6797"),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(Res.string.reviewedit_verified_visit),
                        color = colorFromHex("EF6797"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = uiState.cafeName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = uiState.cafeAddress,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(Res.string.reviewedit_rating_question),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
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
                    contentDescription = stringResource(Res.string.reviewedit_rating_accessibility, index),
                    tint = if (isSelected) colorFromHex("FFC94D") else Color(0x33EF6797),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onAction(ReviewEditAction.SelectRating(index)) }
                )
            }
        }
        Text(
            text = uiState.ratingMessage,
            color = colorFromHex("EF6797"),
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(Res.string.reviewedit_photo_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
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
                            colors = listOf(colorFromHex("FFD8E6"), colorFromHex("FFEFF5"))
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
                            tint = colorFromHex("8B5164"),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = stringResource(Res.string.reviewedit_photo_add),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = stringResource(Res.string.reviewedit_photo_remove),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = colorFromHex("8B5164"),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(Res.string.reviewedit_photo_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ConCafeFormField(
            label = stringResource(Res.string.reviewedit_review_detail_title),
            value = uiState.content,
            onValueChange = { onAction(ReviewEditAction.ChangeReviewText(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            placeholder = stringResource(Res.string.reviewedit_review_detail_hint),
            minLines = 8,
            singleLine = false
        )
        Text(
            text = stringResource(
                Res.string.reviewedit_review_length,
                uiState.reviewLength,
                ReviewEditUiState.minimumReviewLength
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            color = if (uiState.reviewLength >= ReviewEditUiState.minimumReviewLength) colorFromHex("2E9E5B") else colorFromHex("9A8D95"),
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
            text = stringResource(Res.string.reviewedit_cast_tag_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        .background(if (selected) colorFromHex("FFD1DC") else Color(0x1AFFD1DC))
                        .border(
                            width = 1.dp,
                            color = if (selected) colorFromHex("FFD1DC") else Color(0x33FFD1DC),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { onToggle(cast.id) }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = cast.name,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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
                    tint = colorFromHex("EF6797")
                )
            }
            Text(
                text = stringResource(Res.string.reviewedit_atmosphere_question),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnswerChip(
                label = stringResource(Res.string.reviewedit_atmosphere_positive),
                selected = isSelected == true,
                onClick = { onSelect(true) }
            )
            AnswerChip(
                label = stringResource(Res.string.reviewedit_atmosphere_negative),
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
            .background(if (selected) colorFromHex("FFD1DC") else MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = if (selected) colorFromHex("FFD1DC") else colorFromHex("D9CFD5"),
                shape = RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(colorFromHex("FFF6D7"))
            .border(
                width = 1.dp,
                color = colorFromHex("F1D88D"),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = colorFromHex("6B5320"),
            style = MaterialTheme.typography.bodyMedium
        )
        TextButton(
            onClick = onDismiss,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
        ) {
            Text(
                text = stringResource(Res.string.common_close),
                color = colorFromHex("6B5320"),
                fontWeight = FontWeight.Bold
            )
        }
    }
}
