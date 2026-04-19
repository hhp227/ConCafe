package com.hhp227.concafe.presentation.castedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.keyboardBottomInsets
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.castedit_accessibility_back
import concafe.composeapp.generated.resources.castedit_alert_image_required_desc
import concafe.composeapp.generated.resources.castedit_alert_image_required_title
import concafe.composeapp.generated.resources.castedit_birthday_label
import concafe.composeapp.generated.resources.castedit_birthday_pick
import concafe.composeapp.generated.resources.castedit_gallery_add
import concafe.composeapp.generated.resources.castedit_gallery_guide
import concafe.composeapp.generated.resources.castedit_gallery_item_label
import concafe.composeapp.generated.resources.castedit_gallery_remove
import concafe.composeapp.generated.resources.castedit_gallery_title
import concafe.composeapp.generated.resources.castedit_label_concept_role
import concafe.composeapp.generated.resources.castedit_label_intro
import concafe.composeapp.generated.resources.castedit_label_name
import concafe.composeapp.generated.resources.castedit_placeholder_concept_role
import concafe.composeapp.generated.resources.castedit_placeholder_intro
import concafe.composeapp.generated.resources.castedit_placeholder_name
import concafe.composeapp.generated.resources.castedit_profile_photo_hint
import concafe.composeapp.generated.resources.castedit_profile_photo_title
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.common_close
import concafe.composeapp.generated.resources.common_confirm
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import org.jetbrains.compose.resources.stringResource

@Composable
fun CastEditScreen(
    cafeId: String? = null,
    castId: String? = null,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CastEditViewModel = viewModel(
        key = "cast-edit-${cafeId ?: "none"}-${castId ?: "new"}",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CastEditViewModel> { parametersOf(cafeId, castId) } }
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
    if (uiState.isImageRequiredAlertVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(CastEditAction.DismissImageRequiredAlert) },
            title = { Text(stringResource(Res.string.castedit_alert_image_required_title)) },
            text = { Text(stringResource(Res.string.castedit_alert_image_required_desc)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(CastEditAction.DismissImageRequiredAlert) }) {
                    Text(stringResource(Res.string.common_confirm))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CastEditContentScreen(
    uiState: CastEditUiState,
    onAction: (CastEditAction) -> Unit
) {
    var isBirthdayPickerVisible by remember { mutableStateOf(false) }
    val initialBirthdayMillis = remember(uiState.birthday) { TimeUtils.parseBirthdayToEpochMillisOrNull(uiState.birthday) }

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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.castedit_accessibility_back)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .keyboardBottomInsets(),
                color = if (isSystemInDarkTheme()) colorFromHex("FFFBFD") else Color.White.copy(alpha = 0.92f),
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
                            containerColor = colorFromHex("FFD1DC"),
                            contentColor = colorFromHex("2B2330")
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colorFromHex("2B2330")
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
                .then(
                    if (isSystemInDarkTheme()) {
                        Modifier.background(colorFromHex("FFFBFD"))
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                colors = listOf(colorFromHex("F8F5F6"), colorFromHex("FFFBFD"))
                            )
                        )
                    }
                )
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorFromHex("EF6797"))
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
                            label = stringResource(Res.string.castedit_label_name),
                            value = uiState.castName,
                            onValueChange = { onAction(CastEditAction.ChangeCastName(it)) },
                            placeholder = stringResource(Res.string.castedit_placeholder_name)
                        )
                    }
                    item {
                        ConCafeFormField(
                            label = stringResource(Res.string.castedit_label_concept_role),
                            value = uiState.conceptRole,
                            onValueChange = { onAction(CastEditAction.ChangeConceptRole(it)) },
                            placeholder = stringResource(Res.string.castedit_placeholder_concept_role)
                        )
                    }
                    item {
                        BirthdayInputField(
                            value = uiState.birthday,
                            onValueChange = { onAction(CastEditAction.ChangeBirthday(it)) },
                            onClickCalendar = { isBirthdayPickerVisible = true }
                        )
                    }
                    item {
                        ConCafeFormField(
                            label = stringResource(Res.string.castedit_label_intro),
                            value = uiState.introduction,
                            onValueChange = { onAction(CastEditAction.ChangeIntroduction(it)) },
                            placeholder = stringResource(Res.string.castedit_placeholder_intro),
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
                                galleryMaxCount = uiState.galleryMaxCount,
                                onAddClick = {
                                    onAction(CastEditAction.ClickAddGalleryPhoto)
                                    launchImagePicker()
                                },
                                onRemoveClick = { index ->
                                    onAction(CastEditAction.RemoveGalleryImage(index))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (isBirthdayPickerVisible) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialBirthdayMillis)

        DatePickerDialog(
            onDismissRequest = { isBirthdayPickerVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            onAction(CastEditAction.ChangeBirthday(TimeUtils.formatBirthdayFromEpochMillis(selected)))
                        }
                        isBirthdayPickerVisible = false
                    }
                ) {
                    Text(stringResource(Res.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isBirthdayPickerVisible = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun BirthdayInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onClickCalendar: () -> Unit
) {
    var birthdayTextFieldValue by remember {
        mutableStateOf(TextFieldValue())
    }

    LaunchedEffect(value) {
        if (birthdayTextFieldValue.text != value) {
            birthdayTextFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.castedit_birthday_label),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colorFromHex("665A63")
        )
        OutlinedTextField(
            value = birthdayTextFieldValue,
            onValueChange = { nextValue ->
                val normalized = TimeUtils.normalizeBirthdayInput(nextValue.text)
                birthdayTextFieldValue = TextFieldValue(
                    text = normalized,
                    selection = TextRange(normalized.length)
                )
                onValueChange(normalized)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            placeholder = { Text("MM/DD/YYYY", color = colorFromHex("AA98A4")) },
            shape = RoundedCornerShape(16.dp),
            trailingIcon = {
                IconButton(onClick = onClickCalendar) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(Res.string.castedit_birthday_pick),
                        tint = colorFromHex("B1A3AC")
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colorFromHex("F8F5F6"),
                unfocusedContainerColor = colorFromHex("F8F5F6"),
                focusedBorderColor = colorFromHex("FFD1DC"),
                unfocusedBorderColor = Color(0x4DFFD1DC)
            )
        )
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
        Column(
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
                                colors = listOf(colorFromHex("FFE3EC"), colorFromHex("F8C5D7"))
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
                    color = colorFromHex("FFD1DC"),
                    border = BorderStroke(2.dp, Color.White),
                    shadowElevation = 6.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = colorFromHex("2B2330")
                    )
                }
            }
            Text(
                stringResource(Res.string.castedit_profile_photo_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(Res.string.castedit_profile_photo_hint),
                style = MaterialTheme.typography.bodySmall,
                color = colorFromHex("8C7E87")
            )
        }
    }
}

@Composable
private fun GallerySection(
    galleryImages: List<String>,
    galleryMaxCount: Int,
    onAddClick: () -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    val galleryLimitText = "${galleryImages.size} / $galleryMaxCount"

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.castedit_gallery_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colorFromHex("665A63")
            )
            Text(galleryLimitText, style = MaterialTheme.typography.labelMedium, color = colorFromHex("EF6797"), fontWeight = FontWeight.Bold)
        }
        CastGalleryGrid(
            galleryImages = galleryImages,
            galleryMaxCount = galleryMaxCount,
            onAddClick = onAddClick,
            onRemoveClick = onRemoveClick
        )
        Text(
            text = stringResource(Res.string.castedit_gallery_guide, galleryMaxCount),
            style = MaterialTheme.typography.bodySmall,
            color = colorFromHex("8A8088")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CastGalleryGrid(
    galleryImages: List<String>,
    galleryMaxCount: Int,
    onAddClick: () -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        galleryImages.forEachIndexed { index, imageUrl ->
            CastGalleryImageTile(
                label = stringResource(Res.string.castedit_gallery_item_label, index + 1),
                imageUrl = imageUrl,
                index = index,
                onRemoveClick = { onRemoveClick(index) }
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
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(Res.string.castedit_gallery_add),
                    tint = colorFromHex("EF6797")
                )
            }
        }
    }
}

@Composable
private fun CastGalleryImageTile(
    label: String,
    imageUrl: String,
    index: Int,
    onRemoveClick: () -> Unit
) {
    val gradients = listOf(
        listOf(colorFromHex("FFE6EE"), colorFromHex("F7C9D8")),
        listOf(colorFromHex("FFD8E6"), colorFromHex("FFEFF5")),
        listOf(colorFromHex("FFD9CF"), colorFromHex("FFF0EA"))
    )
    val colors = gradients[index % gradients.size]

    Box(
        modifier = Modifier
            .size(96.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
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
        Surface(
            modifier = Modifier
                .offset(x = 6.dp, y = (-6).dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.52f),
            onClick = onRemoveClick
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.castedit_gallery_remove),
                tint = Color.White,
                modifier = Modifier
                    .padding(4.dp)
                    .size(12.dp)
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
        color = colorFromHex("FFF6D7"),
        border = BorderStroke(1.dp, colorFromHex("F1D88D"))
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
                color = colorFromHex("6B5320")
            )
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(Res.string.common_close),
                    color = colorFromHex("6B5320"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
