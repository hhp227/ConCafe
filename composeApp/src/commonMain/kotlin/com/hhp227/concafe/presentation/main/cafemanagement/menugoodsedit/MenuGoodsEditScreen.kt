package com.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.CompatImagePicker
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.component.colorFromHex
import com.hhp227.concafe.presentation.component.fixedBottomBarInsets
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.common_close
import concafe.composeapp.generated.resources.menugoods_edit_back_content_description
import concafe.composeapp.generated.resources.menugoods_edit_category_dessert
import concafe.composeapp.generated.resources.menugoods_edit_category_drink
import concafe.composeapp.generated.resources.menugoods_edit_category_food
import concafe.composeapp.generated.resources.menugoods_edit_category_goods
import concafe.composeapp.generated.resources.menugoods_edit_desc_placeholder
import concafe.composeapp.generated.resources.menugoods_edit_info_enter_name
import concafe.composeapp.generated.resources.menugoods_edit_info_enter_price
import concafe.composeapp.generated.resources.menugoods_edit_info_image_upload_failed
import concafe.composeapp.generated.resources.menugoods_edit_info_image_upload_next_step
import concafe.composeapp.generated.resources.menugoods_edit_info_item_not_found
import concafe.composeapp.generated.resources.menugoods_edit_info_load_failed
import concafe.composeapp.generated.resources.menugoods_edit_info_price_number_only
import concafe.composeapp.generated.resources.menugoods_edit_info_save_failed
import concafe.composeapp.generated.resources.menugoods_edit_label_category
import concafe.composeapp.generated.resources.menugoods_edit_label_desc
import concafe.composeapp.generated.resources.menugoods_edit_label_name
import concafe.composeapp.generated.resources.menugoods_edit_label_price
import concafe.composeapp.generated.resources.menugoods_edit_loading
import concafe.composeapp.generated.resources.menugoods_edit_placeholder_name_example
import concafe.composeapp.generated.resources.menugoods_edit_save_create
import concafe.composeapp.generated.resources.menugoods_edit_save_update
import concafe.composeapp.generated.resources.menugoods_edit_stock_available
import concafe.composeapp.generated.resources.menugoods_edit_stock_sold_out
import concafe.composeapp.generated.resources.menugoods_edit_stock_title
import concafe.composeapp.generated.resources.menugoods_edit_title_add
import concafe.composeapp.generated.resources.menugoods_edit_title_edit
import concafe.composeapp.generated.resources.menugoods_edit_upload_desc
import concafe.composeapp.generated.resources.menugoods_edit_upload_title
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
fun MenuGoodsEditScreen(
    cafeId: String,
    itemId: String?,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: MenuGoodsEditViewModel = viewModel(
        key = "menu-goods-edit-$cafeId-${itemId ?: "new"}",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<MenuGoodsEditViewModel> { parametersOf(cafeId, itemId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                MenuGoodsEditEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
            }
        }
    }
    MenuGoodsEditContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuGoodsEditContentScreen(
    uiState: MenuGoodsEditUiState,
    onAction: (MenuGoodsEditAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(if (uiState.isEditMode) {
                            Res.string.menugoods_edit_title_edit
                        } else {
                            Res.string.menugoods_edit_title_add
                        }),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(MenuGoodsEditAction.ClickBack) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.menugoods_edit_back_content_description)
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = if (isSystemInDarkTheme()) colorFromHex("FFFBFD") else Color.White.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fixedBottomBarInsets()
                        .border(BorderStroke(1.dp, Color(0x33FFD1DC)))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = { onAction(MenuGoodsEditAction.ClickSave) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorFromHex("FFD1DC"),
                            contentColor = colorFromHex("2B2330")
                        )
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Text(
                            text = stringResource(if (uiState.isEditMode) {
                                Res.string.menugoods_edit_save_update
                            } else {
                                Res.string.menugoods_edit_save_create
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
                    if (isSystemInDarkTheme()) {
                        Modifier.background(colorFromHex("FFFBFD"))
                    } else {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(colorFromHex("F8F5F6"), colorFromHex("FFFBFD"))
                            )
                        )
                    }
                )
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    CompatImagePicker(onImageSelected = {
                        onAction(MenuGoodsEditAction.SelectPhoto(it))
                    }) { launchPicker ->
                        PhotoUploadCard(
                            imageUrl = uiState.imageUrl,
                            onClick = launchPicker
                        )
                    }
                }
                uiState.infoMessageKey?.let { messageKey ->
                    item {
                        InfoBanner(
                            message = when (messageKey) {
                                "menugoods_edit_info_item_not_found" -> stringResource(Res.string.menugoods_edit_info_item_not_found)
                                "menugoods_edit_info_load_failed" -> stringResource(Res.string.menugoods_edit_info_load_failed)
                                "menugoods_edit_info_enter_name" -> stringResource(Res.string.menugoods_edit_info_enter_name)
                                "menugoods_edit_info_enter_price" -> stringResource(Res.string.menugoods_edit_info_enter_price)
                                "menugoods_edit_info_price_number_only" -> stringResource(Res.string.menugoods_edit_info_price_number_only)
                                "menugoods_edit_info_save_failed" -> stringResource(Res.string.menugoods_edit_info_save_failed)
                                "menugoods_edit_info_image_upload_failed" -> stringResource(Res.string.menugoods_edit_info_image_upload_failed)
                                "menugoods_edit_info_image_upload_next_step" -> stringResource(Res.string.menugoods_edit_info_image_upload_next_step)
                                else -> messageKey
                            },
                            onDismiss = { onAction(MenuGoodsEditAction.DismissInfoMessage) }
                        )
                    }
                }
                if (uiState.isLoading) {
                    item {
                        LoadingCard()
                    }
                } else {
                    item {
                        RoundedTextField(
                            label = stringResource(Res.string.menugoods_edit_label_name),
                            value = uiState.itemName,
                            onValueChange = { onAction(MenuGoodsEditAction.ChangeName(it)) },
                            placeholder = stringResource(Res.string.menugoods_edit_placeholder_name_example)
                        )
                    }
                    item {
                        PriceField(
                            label = stringResource(Res.string.menugoods_edit_label_price),
                            value = uiState.price,
                            onValueChange = { onAction(MenuGoodsEditAction.ChangePrice(it)) }
                        )
                    }
                    item {
                        FormField(
                            label = stringResource(Res.string.menugoods_edit_label_category)
                        ) {
                            CategoryGrid(
                                selectedCategoryId = uiState.selectedCategoryId,
                                onSelect = { onAction(MenuGoodsEditAction.SelectCategory(it)) }
                            )
                        }
                    }
                    item {
                        DescriptionField(
                            label = stringResource(Res.string.menugoods_edit_label_desc),
                            value = uiState.description,
                            onValueChange = { onAction(MenuGoodsEditAction.ChangeDescription(it)) }
                        )
                    }
                    item {
                        StockCard(
                            isInStock = uiState.isInStock,
                            onStockChange = { onAction(MenuGoodsEditAction.ToggleStock(it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoUploadCard(
    imageUrl: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = if (imageUrl.isNullOrBlank()) {
                        listOf(Color(0x33FFD1DC), Color(0x22FFF1F5))
                    } else {
                        listOf(Color(0x55FFD1DC), Color(0x44F9E3EA))
                    }
                )
            )
            .border(BorderStroke(2.dp, Color(0x66FFD1DC)), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White.copy(alpha = 0.8f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = colorFromHex("6F5968"),
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Text(
                    text = stringResource(Res.string.menugoods_edit_upload_title),
                    fontWeight = FontWeight.SemiBold,
                    color = colorFromHex("2B2330")
                )
                Text(
                    text = stringResource(Res.string.menugoods_edit_upload_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorFromHex("7A6671")
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                CompatImageDisplay(imageUrl = imageUrl, modifier = Modifier.fillMaxSize())
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = CircleShape,
                    color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White.copy(alpha = 0.92f),
                    shadowElevation = 2.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = colorFromHex("2B2330"),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = colorFromHex("4D404A"),
            fontWeight = FontWeight.SemiBold
        )
        content()
    }
}

@Composable
private fun RoundedTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    ConCafeFormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder
    )
}

@Composable
private fun PriceField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    ConCafeFormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = "0",
        leadingContent = {
            Text(
                text = "₩",
                color = colorFromHex("6B5A65"),
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}

@Composable
private fun CategoryGrid(
    selectedCategoryId: String,
    onSelect: (String) -> Unit
) {
    val categoryIds = listOf("drink", "food", "dessert", "goods")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val categories = categoryIds.chunked(2)

        categories.forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCategories.forEach { categoryId ->
                    val isSelected = categoryId == selectedCategoryId

                    CategoryButton(
                        modifier = Modifier.weight(1f),
                        categoryId = categoryId,
                        isSelected = isSelected,
                        onClick = { onSelect(categoryId) }
                    )
                }
                if (rowCategories.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryButton(
    modifier: Modifier = Modifier,
    categoryId: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0x33FFD1DC) else colorFromHex("F8F5F6"),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) colorFromHex("FFD1DC") else Color(0x55FFD1DC)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = categoryIcon(categoryId),
                contentDescription = null,
                tint = if (isSelected) colorFromHex("2B2330") else colorFromHex("6E6169")
            )
            Text(
                text = categoryLabel(categoryId),
                modifier = Modifier.padding(start = 8.dp),
                color = if (isSelected) colorFromHex("2B2330") else colorFromHex("6E6169"),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun categoryLabel(categoryId: String): String {
    return when (categoryId) {
        "food" -> stringResource(Res.string.menugoods_edit_category_food)
        "dessert" -> stringResource(Res.string.menugoods_edit_category_dessert)
        "goods" -> stringResource(Res.string.menugoods_edit_category_goods)
        else -> stringResource(Res.string.menugoods_edit_category_drink)
    }
}

private fun categoryIcon(categoryId: String): ImageVector {
    return when (categoryId) {
        "food" -> Icons.Default.Restaurant
        "dessert" -> Icons.Default.Icecream
        "goods" -> Icons.AutoMirrored.Filled.FeaturedPlayList
        else -> Icons.Default.LocalCafe
    }
}

@Composable
private fun DescriptionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    ConCafeFormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = stringResource(Res.string.menugoods_edit_desc_placeholder),
        minLines = 5,
        singleLine = false
    )
}

@Composable
private fun StockCard(
    isInStock: Boolean,
    onStockChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colorFromHex("F8F5F6")),
        border = BorderStroke(1.dp, Color(0x33FFD1DC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = colorFromHex("FF8AA8")
                )
                Column {
                    Text(
                        text = stringResource(Res.string.menugoods_edit_stock_title),
                        fontWeight = FontWeight.SemiBold,
                        color = colorFromHex("2B2330")
                    )
                    Text(
                        text = stringResource(if (isInStock) {
                            Res.string.menugoods_edit_stock_available
                        } else {
                            Res.string.menugoods_edit_stock_sold_out
                        }),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorFromHex("7A6671")
                    )
                }
            }
            Switch(
                checked = isInStock,
                onCheckedChange = onStockChange
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
            Text(
                text = stringResource(Res.string.common_close),
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable(onClick = onDismiss),
                style = MaterialTheme.typography.labelMedium,
                color = colorFromHex("6B5320"),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White)
            .border(BorderStroke(1.dp, Color(0x55FFD1DC)), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = colorFromHex("FF8AA8"))
            Text(
                text = stringResource(Res.string.menugoods_edit_loading),
                color = colorFromHex("7A6671"),
                textAlign = TextAlign.Center
            )
        }
    }
}

