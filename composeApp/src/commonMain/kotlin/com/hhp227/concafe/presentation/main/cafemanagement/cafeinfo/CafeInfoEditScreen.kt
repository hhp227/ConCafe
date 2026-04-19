package com.hhp227.concafe.presentation.main.cafemanagement.cafeinfo

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
import com.hhp227.concafe.core.util.PhoneNumberTextField
import com.hhp227.concafe.core.util.TimeUtils
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.keyboardBottomInsets
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf
import kotlin.math.round

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
                    snackbarHostState.showSnackbar(getString(Res.string.cafeinfo_info_saved))
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
            title = { Text(stringResource(Res.string.cafeinfo_alert_image_title)) },
            text = { Text(stringResource(Res.string.cafeinfo_alert_image_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(CafeInfoEditAction.DismissImageRequiredAlert) }) {
                    Text(stringResource(Res.string.banner_action_ok))
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
    val coroutineScope = rememberCoroutineScope()
    val isDarkMode = isSystemInDarkTheme()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (uiState.isRegistrationMode) {
                            Res.string.cafeinfo_screen_title_registration
                        } else {
                            Res.string.cafeinfo_screen_title_edit
                        }),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CafeInfoEditAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.banner_content_back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = if (isDarkMode) colorFromHex("FFFBFD") else Color.White.copy(alpha = 0.92f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0x33FFD1DC))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .keyboardBottomInsets()
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
                            containerColor = colorFromHex("FFD1DC"),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Text(
                            text = stringResource(if (uiState.isRegistrationMode) {
                                Res.string.cafeinfo_submit_registration
                            } else {
                                Res.string.cafeinfo_submit_edit
                            }),
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
                    if (isDarkMode) {
                        Modifier.background(colorFromHex("FFFBFD"))
                    } else {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.background)
                            )
                        )
                    }
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
                            CircularProgressIndicator(color = colorFromHex("EF6797"))
                        }
                    }
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = when {
                                message == "cafeinfo_info_saved" -> stringResource(Res.string.cafeinfo_info_saved)
                                message == "cafeinfo_info_load_failed" -> stringResource(Res.string.cafeinfo_info_load_failed)
                                message == "cafeinfo_info_image_required_one_or_more" -> stringResource(Res.string.cafeinfo_info_image_required_one_or_more)
                                message == "cafeinfo_info_save_failed" -> stringResource(Res.string.cafeinfo_info_save_failed)
                                message == "cafeinfo_info_registration_rep_required" -> stringResource(Res.string.cafeinfo_info_registration_rep_required)
                                message == "cafeinfo_info_rep_upload_next_step" -> stringResource(Res.string.cafeinfo_info_rep_upload_next_step)
                                message == "cafeinfo_info_gallery_add_next_step" -> stringResource(Res.string.cafeinfo_info_gallery_add_next_step)
                                message == "cafeinfo_info_pin_location_hint" -> stringResource(Res.string.cafeinfo_info_pin_location_hint)
                                message == "cafeinfo_info_exception_next_step" -> stringResource(Res.string.cafeinfo_info_exception_next_step)
                                message == "cafeinfo_info_image_upload_failed" -> stringResource(Res.string.cafeinfo_info_image_upload_failed)
                                message.startsWith("cafeinfo_info_gallery_max_exceeded:") -> {
                                    val count = message.substringAfter(':').toIntOrNull() ?: 0
                                    stringResource(Res.string.cafeinfo_info_gallery_max_exceeded, count)
                                }
                                else -> message
                            },
                            onDismiss = { onAction(CafeInfoEditAction.DismissInfoMessage) }
                        )
                    }
                }
                item {
                    EditSectionCard(title = stringResource(Res.string.cafeinfo_section_basic)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CafeInfoTextField(
                                label = stringResource(Res.string.cafeinfo_label_name),
                                value = uiState.cafeName,
                                onValueChange = { onAction(CafeInfoEditAction.ChangeCafeName(it)) }
                            )
                            CafeInfoTextField(
                                label = stringResource(Res.string.cafeinfo_label_description),
                                value = uiState.cafeDescription,
                                minLines = 5,
                                onValueChange = { onAction(CafeInfoEditAction.ChangeCafeDescription(it)) }
                            )
                        }
                    }
                }
                item {
                    EditSectionCard(title = stringResource(Res.string.cafeinfo_section_representative)) {
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
                                                colors = listOf(colorFromHex("FFD8E6"), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
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
                                                tint = colorFromHex("8B5164"),
                                                modifier = Modifier.size(34.dp)
                                            )
                                            Text(
                                                text = stringResource(Res.string.cafeinfo_representative_title),
                                                color = colorFromHex("5A4954"),
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
                                text = stringResource(Res.string.cafeinfo_representative_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorFromHex("8A8088"),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                if (!uiState.isRegistrationMode) {
                    item {
                        EditSectionCard(
                            title = stringResource(Res.string.cafeinfo_section_gallery),
                            trailing = {
                                Text(
                                    stringResource(Res.string.cafeinfo_gallery_limit, uiState.galleryLimitCount, uiState.galleryMaxCount),
                                    color = colorFromHex("EF6797"),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                maxItemsInEachRow = 3
                            ) {
                                uiState.galleryImages.forEachIndexed { index, imageUrl ->
                                    GalleryImageTile(
                                        label = stringResource(Res.string.cafeinfo_image_label_prefix, index + 1),
                                        imageUrl = imageUrl,
                                        index = index,
                                        onRemoveClick = { onAction(CafeInfoEditAction.RemoveGalleryImage(index)) }
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
                    EditSectionCard(title = stringResource(Res.string.cafeinfo_section_location_contact)) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CafeInfoTextField(
                                label = stringResource(Res.string.cafeinfo_label_address),
                                value = uiState.address,
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val resolved = resolveCafeAddress(uiState.address)
                                                if (resolved == null) {
                                                    snackbarHostState.showSnackbar(getString(Res.string.cafeinfo_info_address_not_found))
                                                } else {
                                                    onAction(
                                                        CafeInfoEditAction.SetPinnedLocation(
                                                            resolved.latitude,
                                                            resolved.longitude
                                                        )
                                                    )
                                                    onAction(CafeInfoEditAction.ChangeAddress(resolved.fullAddress))
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = stringResource(Res.string.cafeinfo_content_find_by_address),
                                            tint = colorFromHex("EF6797")
                                        )
                                    }
                                },
                                onValueChange = { onAction(CafeInfoEditAction.ChangeAddress(it)) }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .background(colorFromHex("F4EFF2"), RoundedCornerShape(18.dp))
                            ) {
                                CafeInfoLocationPickerMap(
                                    latitude = uiState.mapLatitude,
                                    longitude = uiState.mapLongitude,
                                    onLocationSelected = { latitude, longitude, address ->
                                        onAction(CafeInfoEditAction.SetPinnedLocation(latitude, longitude))
                                        if (address.isNullOrBlank()) {
                                            coroutineScope.launch {
                                                val fallbackAddress = getString(
                                                    Res.string.cafeinfo_coordinate_fallback,
                                                    formatCoordinate(latitude),
                                                    formatCoordinate(longitude)
                                                )

                                                onAction(CafeInfoEditAction.ChangeAddress(fallbackAddress))
                                            }
                                        } else {
                                            onAction(CafeInfoEditAction.ChangeAddress(address))
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(18.dp))
                                )
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                        .clickable { onAction(CafeInfoEditAction.ClickPinLocation) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White.copy(alpha = 0.92f),
                                    border = BorderStroke(1.dp, Color(0x33FFD1DC))
                                ) {
                                    Text(
                                        text = stringResource(Res.string.cafeinfo_action_pin_location),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = stringResource(
                                    Res.string.cafeinfo_selected_coordinate,
                                    formatCoordinate(uiState.mapLatitude),
                                    formatCoordinate(uiState.mapLongitude)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorFromHex("7E737B")
                            )
                            PhoneNumberTextField(
                                label = stringResource(Res.string.cafeinfo_label_contact),
                                value = uiState.contactNumber,
                                onValueChange = { onAction(CafeInfoEditAction.ChangeContactNumber(it)) }
                            )
                        }
                    }
                }
                item {
                    EditSectionCard(title = stringResource(Res.string.cafeinfo_section_business_hours)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HoursRow(
                                label = stringResource(Res.string.cafeinfo_label_weekday),
                                open = uiState.weekdayOpen,
                                close = uiState.weekdayClose,
                                onOpenChange = { onAction(CafeInfoEditAction.ChangeWeekdayOpen(it)) },
                                onCloseChange = { onAction(CafeInfoEditAction.ChangeWeekdayClose(it)) }
                            )
                            HoursRow(
                                label = stringResource(Res.string.cafeinfo_label_weekend),
                                open = uiState.weekendOpen,
                                close = uiState.weekendClose,
                                onOpenChange = { onAction(CafeInfoEditAction.ChangeWeekendOpen(it)) },
                                onCloseChange = { onAction(CafeInfoEditAction.ChangeWeekendClose(it)) }
                            )
                            TextButton(
                                onClick = { onAction(CafeInfoEditAction.ClickManageExceptionDates) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = colorFromHex("EF6797"))
                                Text(stringResource(Res.string.cafeinfo_action_manage_exception), color = colorFromHex("EF6797"), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCoordinate(value: Double): String {
    val rounded = round(value * 100000.0) / 100000.0
    return rounded.toString()
}

@Composable
private fun EditSectionCard(
    title: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
    index: Int,
    onRemoveClick: () -> Unit
) {
    val gradients = listOf(
        0xFFFFD8E6L to 0xFFFFF1F6L,
        0xFFF9D4E4L to 0xFFFFE7F0L,
        0xFFFFD9CFL to 0xFFFFF0EAL
    )
    val colors = gradients[index % gradients.size]

    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                color = colorFromHex("5A4954"),
                fontWeight = FontWeight.SemiBold
            )
        }
        Surface(
            modifier = Modifier.offset(x = 6.dp, y = (-6).dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.52f),
            onClick = onRemoveClick
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .padding(4.dp)
                    .size(12.dp)
            )
        }
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
                contentDescription = stringResource(Res.string.cafeinfo_content_add_image),
                tint = colorFromHex("EF6797"),
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        SmallTimeField(value = open, onValueChange = onOpenChange)
        Text(stringResource(Res.string.cafeinfo_dash), color = colorFromHex("8A8088"))
        SmallTimeField(value = close, onValueChange = onCloseChange)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SmallTimeField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val isDarkMode = isSystemInDarkTheme()
    var isTimePickerVisible by remember { mutableStateOf(false) }
    val (initialHour, initialMinute) = remember(value) {
        TimeUtils.parseHourMinuteOrDefault(value)
    }

    Box(modifier = Modifier.width(108.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            readOnly = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
                unfocusedContainerColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White,
                focusedBorderColor = Color(0x33FFD1DC),
                unfocusedBorderColor = Color(0x33FFD1DC)
            ),
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = stringResource(Res.string.cafeinfo_content_select_time),
                    tint = colorFromHex("8A8088"),
                    modifier = Modifier.size(18.dp)
                )
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { isTimePickerVisible = true }
        )
    }
    if (isTimePickerVisible) {
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { isTimePickerVisible = false },
            title = { Text(stringResource(Res.string.cafeinfo_time_picker_title)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(TimeUtils.formatHourMinute(timePickerState.hour, timePickerState.minute))
                        isTimePickerVisible = false
                    }
                ) {
                    Text(stringResource(Res.string.banner_action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { isTimePickerVisible = false }) {
                    Text(stringResource(Res.string.banner_action_cancel))
                }
            }
        )
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
                Text(stringResource(Res.string.banneredit_action_close), color = colorFromHex("6B5320"))
            }
        }
    }
}
