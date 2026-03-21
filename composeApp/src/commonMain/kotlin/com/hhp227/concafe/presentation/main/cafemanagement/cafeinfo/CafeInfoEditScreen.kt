package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
fun CafeInfoEditScreen(
    cafeId: String? = null,
    isRegistrationMode: Boolean = false,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: CafeInfoEditViewModel = viewModel(
        key = "cafe-info-edit-${cafeId ?: "registration"}-$isRegistrationMode",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<CafeInfoEditViewModel> { parametersOf(cafeId, isRegistrationMode) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                CafeInfoEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                CafeInfoEvent.ShowSaveSuccessMessage -> {
                    snackbarHostState.showSnackbar("카페 정보가 저장되었습니다.")
                }
            }
        }
    }
    CafeInfoEditContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        snackbarHostState = snackbarHostState
    )
    if (uiState.isImageRequiredAlertVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(CafeInfoEditAction.DismissImageRequiredAlert) },
            title = { Text("이미지 등록 필요") },
            text = { Text("카페 등록/수정에는 대표 이미지 또는 갤러리 이미지가 필요합니다.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(CafeInfoEditAction.DismissImageRequiredAlert) }) {
                    Text("확인")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CafeInfoEditContent(
    uiState: CafeInfoEditUiState,
    onAction: (CafeInfoEditAction) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.screenTitle, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CafeInfoEditAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0x33FFD1DC))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { onAction(CafeInfoEditAction.ClickSave) },
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD1DC),
                            contentColor = Color(0xFF2B2330)
                        )
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Text(
                            text = uiState.submitButtonText,
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
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFEF6797))
                        }
                    }
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = message,
                            onDismiss = { onAction(CafeInfoEditAction.DismissInfoMessage) }
                        )
                    }
                }
                item {
                    EditSectionCard(title = "기본 정보") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CafeInfoTextField(
                                label = "카페명",
                                value = uiState.cafeName,
                                onValueChange = { onAction(CafeInfoEditAction.ChangeCafeName(it)) }
                            )
                            CafeInfoTextField(
                                label = "카페 소개",
                                value = uiState.cafeDescription,
                                minLines = 5,
                                onValueChange = { onAction(CafeInfoEditAction.ChangeCafeDescription(it)) }
                            )
                        }
                    }
                }
                item {
                    EditSectionCard(title = "대표 이미지") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CompatImagePicker(
                                onImageSelected = { imageUrl ->
                                    onAction(CafeInfoEditAction.SelectRepresentativeImage(imageUrl))
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
                                            ),
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { launchImagePicker() }
                                ) {
                                    if (uiState.representativeImageUrl.isNullOrBlank()) {
                                        Column(
                                            modifier = Modifier.align(Alignment.Center),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PhotoCamera,
                                                contentDescription = null,
                                                tint = Color(0xFF8B5164),
                                                modifier = Modifier.size(34.dp)
                                            )
                                            Text(
                                                text = uiState.representativeImageTitle,
                                                color = Color(0xFF5A4954),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        CompatImageDisplay(
                                            imageUrl = uiState.representativeImageUrl,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "검색 결과에 노출되는 대표 이미지입니다",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8A8088),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (!uiState.isRegistrationMode) {
                    item {
                        EditSectionCard(
                            title = "카페 갤러리",
                            trailing = {
                                Text(uiState.galleryLimitText, color = Color(0xFFEF6797), fontWeight = FontWeight.SemiBold)
                            }
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                maxItemsInEachRow = 3
                            ) {
                                uiState.galleryImages.forEachIndexed { index, imageUrl ->
                                    GalleryImageTile(
                                        label = "이미지 ${index + 1}",
                                        imageUrl = imageUrl,
                                        index = index
                                    )
                                }
                                if (uiState.galleryImages.size < uiState.galleryMaxCount) {
                                    CompatImagePicker(
                                        onImageSelected = { imageUrl ->
                                            onAction(CafeInfoEditAction.AddGalleryImage(imageUrl))
                                        }
                                    ) { launchImagePicker ->
                                        AddGalleryTile(onClick = launchImagePicker)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    EditSectionCard(title = "위치 및 연락처") {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CafeInfoTextField(
                                label = "지역 / 주소",
                                value = uiState.address,
                                trailingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEF6797))
                                },
                                onValueChange = { onAction(CafeInfoEditAction.ChangeAddress(it)) }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(Color(0xFFF4EFF2), RoundedCornerShape(18.dp))
                            ) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFFB5A9B0), modifier = Modifier.size(36.dp))
                                    Text("지도 미리보기", color = Color(0xFF998D95))
                                }
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .clickable { onAction(CafeInfoEditAction.ClickPinLocation) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color.White.copy(alpha = 0.92f),
                                    border = BorderStroke(1.dp, Color(0x33FFD1DC))
                                ) {
                                    Text(
                                        text = "위치 지정",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            CafeInfoTextField(
                                label = "연락처",
                                value = uiState.contactNumber,
                                onValueChange = { onAction(CafeInfoEditAction.ChangeContactNumber(it)) }
                            )
                        }
                    }
                }
                item {
                    EditSectionCard(title = "영업시간") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HoursRow(
                                label = "평일",
                                open = uiState.weekdayOpen,
                                close = uiState.weekdayClose,
                                onOpenChange = { onAction(CafeInfoEditAction.ChangeWeekdayOpen(it)) },
                                onCloseChange = { onAction(CafeInfoEditAction.ChangeWeekdayClose(it)) }
                            )
                            HoursRow(
                                label = "주말",
                                open = uiState.weekendOpen,
                                close = uiState.weekendClose,
                                onOpenChange = { onAction(CafeInfoEditAction.ChangeWeekendOpen(it)) },
                                onCloseChange = { onAction(CafeInfoEditAction.ChangeWeekendClose(it)) }
                            )
                            TextButton(
                                onClick = { onAction(CafeInfoEditAction.ClickManageExceptionDates) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFFEF6797))
                                Text("예외 영업일 관리", color = Color(0xFFEF6797), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0x1AFFD1DC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    trailing?.invoke()
                }
                content()
            }
        )
    }
}

@Composable
private fun CafeInfoTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    minLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    ConCafeFormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        minLines = minLines,
        singleLine = minLines == 1,
        trailingContent = trailingIcon
    )
}

@Composable
private fun GalleryImageTile(
    label: String,
    imageUrl: String,
    index: Int
) {
    val gradients = listOf(
        0xFFFFD8E6L to 0xFFFFF1F6L,
        0xFFF9D4E4L to 0xFFFFE7F0L,
        0xFFFFD9CFL to 0xFFFFF0EAL
    )
    val colors = gradients[index % gradients.size]

    Box(
        modifier = Modifier
            .size(96.dp)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(colors.first), Color(colors.second))
                ),
                RoundedCornerShape(16.dp)
            )
    ) {
        if (imageUrl.isNotBlank()) {
            CompatImageDisplay(
                imageUrl = imageUrl,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
            )
        }
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF5A4954),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AddGalleryTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(Color(0x1AFFD1DC), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, Color(0x66FFD1DC))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "이미지 추가",
                tint = Color(0xFFEF6797),
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun HoursRow(
    label: String,
    open: String,
    close: String,
    onOpenChange: (String) -> Unit,
    onCloseChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F5F6), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        SmallTimeField(value = open, onValueChange = onOpenChange)
        Text("—", color = Color(0xFF8A8088))
        SmallTimeField(value = close, onValueChange = onCloseChange)
    }
}

@Composable
private fun SmallTimeField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(108.dp),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0x33FFD1DC),
            unfocusedBorderColor = Color(0x33FFD1DC)
        )
    )
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
                Text("닫기", color = Color(0xFF6B5320))
            }
        }
    }
}
