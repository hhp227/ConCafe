package com.hhp227.concafe.presentation.main.cafemanagement.menugoods

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.navigation.NavigationAction
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.changepw_back_content_description
import concafe.composeapp.generated.resources.common_cancel
import concafe.composeapp.generated.resources.common_close
import concafe.composeapp.generated.resources.menugoods_add_new_item
import concafe.composeapp.generated.resources.menugoods_available
import concafe.composeapp.generated.resources.menugoods_context_default_title
import concafe.composeapp.generated.resources.menugoods_context_subtitle
import concafe.composeapp.generated.resources.menugoods_delete_confirm
import concafe.composeapp.generated.resources.menugoods_delete_content_description
import concafe.composeapp.generated.resources.menugoods_delete_message
import concafe.composeapp.generated.resources.menugoods_delete_title
import concafe.composeapp.generated.resources.menugoods_edit_content_description
import concafe.composeapp.generated.resources.menugoods_empty_default_desc
import concafe.composeapp.generated.resources.menugoods_empty_default_title
import concafe.composeapp.generated.resources.menugoods_empty_search_desc
import concafe.composeapp.generated.resources.menugoods_empty_search_title
import concafe.composeapp.generated.resources.menugoods_goods_desc
import concafe.composeapp.generated.resources.menugoods_info_availability_save_failed
import concafe.composeapp.generated.resources.menugoods_info_delete_failed
import concafe.composeapp.generated.resources.menugoods_info_delete_success
import concafe.composeapp.generated.resources.menugoods_info_load_failed
import concafe.composeapp.generated.resources.menugoods_search_action
import concafe.composeapp.generated.resources.menugoods_search_placeholder
import concafe.composeapp.generated.resources.menugoods_sold_out
import concafe.composeapp.generated.resources.menugoods_stock
import concafe.composeapp.generated.resources.menugoods_tab_goods
import concafe.composeapp.generated.resources.menugoods_tab_menu
import concafe.composeapp.generated.resources.menugoods_title
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

@Composable
fun MenuGoodsScreen(
    cafeId: String,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: MenuGoodsViewModel = viewModel(
        key = "menu-goods-$cafeId",
        factory = viewModelFactory {
            initializer { GlobalContext.get().get<MenuGoodsViewModel> { parametersOf(cafeId) } }
        }
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.event.collect { event ->
            when (event) {
                MenuGoodsEvent.NavigateBack -> onNavigationAction(NavigationAction.NavigateBack)
                is MenuGoodsEvent.NavigateToEdit -> {
                    onNavigationAction(
                        NavigationAction.NavigateToMenuGoodsEdit(
                            cafeId = event.cafeId,
                            itemId = event.itemId
                        )
                    )
                }
            }
        }
    }
    MenuGoodsContentScreen(
        uiState = uiState,
        onAction = viewModel::onAction
    )
    uiState.pendingDeleteItemId?.let { itemId ->
        AlertDialog(
            onDismissRequest = { viewModel.onAction(MenuGoodsAction.CancelDeleteItem) },
            title = { Text(stringResource(Res.string.menugoods_delete_title)) },
            text = { Text(stringResource(Res.string.menugoods_delete_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(MenuGoodsAction.ConfirmDeleteItem(itemId)) }) {
                    Text(stringResource(Res.string.menugoods_delete_confirm), color = Color(0xFFD96B7A))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(MenuGoodsAction.CancelDeleteItem) }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuGoodsContentScreen(
    uiState: MenuGoodsUiState,
    onAction: (MenuGoodsAction) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.menugoods_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(MenuGoodsAction.ClickBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.changepw_back_content_description))
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(MenuGoodsAction.ClickSearch) }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.menugoods_search_action))
                    }
                }
            )
        },
        floatingActionButton = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                ExtendedFloatingActionButton(
                    onClick = { onAction(MenuGoodsAction.ClickAddNewItem) },
                    containerColor = Color(0xFFFFD1DC),
                    contentColor = Color(0xFF2B2330),
                    text = {
                        Text(
                            text = stringResource(Res.string.menugoods_add_new_item),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFF8FB), Color(0xFFFFF2F6), Color(0xFFFFFCFD))
                    )
                )
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    CafeContextCard(cafeName = uiState.cafeName)
                }
                item {
                    CollectionTabRow(
                        selectedCollection = uiState.selectedCollection,
                        onSelect = { onAction(MenuGoodsAction.SelectCollection(it)) }
                    )
                }
                if (uiState.isSearchVisible) {
                    item {
                        SearchField(
                            value = uiState.searchQuery,
                            onValueChange = { onAction(MenuGoodsAction.ChangeSearchQuery(it)) }
                        )
                    }
                }
                item {
                    CategoryChipRow(
                        chips = uiState.visibleCategories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onSelect = { onAction(MenuGoodsAction.SelectCategory(it)) }
                    )
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = when (message) {
                                "menugoods_info_load_failed" -> stringResource(Res.string.menugoods_info_load_failed)
                                "menugoods_info_availability_save_failed" -> stringResource(Res.string.menugoods_info_availability_save_failed)
                                "menugoods_info_delete_success" -> stringResource(Res.string.menugoods_info_delete_success)
                                "menugoods_info_delete_failed" -> stringResource(Res.string.menugoods_info_delete_failed)
                                else -> message
                            },
                            onDismiss = { onAction(MenuGoodsAction.DismissInfoMessage) }
                        )
                    }
                }
                if (uiState.isLoading) {
                    item {
                        LoadingCard()
                    }
                } else if (
                    uiState.selectedCollection == MenuGoodsUiState.CollectionTab.MENU &&
                    uiState.filteredMenuItems.isEmpty()
                ) {
                    item {
                        EmptyStateCard(isSearchMode = uiState.searchQuery.isNotBlank())
                    }
                } else if (
                    uiState.selectedCollection == MenuGoodsUiState.CollectionTab.GOODS &&
                    uiState.filteredGoodsItems.isEmpty()
                ) {
                    item {
                        EmptyStateCard(isSearchMode = uiState.searchQuery.isNotBlank())
                    }
                } else {
                    if (uiState.selectedCollection == MenuGoodsUiState.CollectionTab.MENU) {
                        items(uiState.filteredMenuItems, key = { it.id }) { item ->
                            MenuItemCard(
                                item = item,
                                isAvailable = uiState.isMenuAvailable(item),
                                categoryLabel = uiState.menuCategoryLabel(item),
                                onEdit = { onAction(MenuGoodsAction.ClickEditItem(item.id)) },
                                onDelete = { onAction(MenuGoodsAction.ClickDeleteItem(item.id)) },
                                onToggleAvailability = { onAction(MenuGoodsAction.ToggleItemAvailability(item.id)) }
                            )
                        }
                    } else {
                        items(uiState.filteredGoodsItems, key = { it.id }) { item ->
                            GoodsItemCard(
                                item = item,
                                isAvailable = uiState.isGoodsAvailable(item),
                                categoryLabel = uiState.goodsCategoryLabel(item),
                                onEdit = { onAction(MenuGoodsAction.ClickEditItem(item.id)) },
                                onDelete = { onAction(MenuGoodsAction.ClickDeleteItem(item.id)) },
                                onToggleAvailability = { onAction(MenuGoodsAction.ToggleItemAvailability(item.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CafeContextCard(cafeName: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF351B42), Color(0xFF7B3F68), Color(0xFFF28EB5))
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (cafeName.isBlank()) stringResource(Res.string.menugoods_context_default_title) else cafeName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.menugoods_context_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun CollectionTabRow(
    selectedCollection: MenuGoodsUiState.CollectionTab,
    onSelect: (MenuGoodsUiState.CollectionTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0x1AFFD1DC)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CollectionTabButton(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.menugoods_tab_menu),
                selected = selectedCollection == MenuGoodsUiState.CollectionTab.MENU,
                onClick = { onSelect(MenuGoodsUiState.CollectionTab.MENU) }
            )
            CollectionTabButton(
                modifier = Modifier.weight(1f),
                label = stringResource(Res.string.menugoods_tab_goods),
                selected = selectedCollection == MenuGoodsUiState.CollectionTab.GOODS,
                onClick = { onSelect(MenuGoodsUiState.CollectionTab.GOODS) }
            )
        }
    }
}

@Composable
private fun CollectionTabButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFFFD1DC) else Color.Transparent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color(0xFF2B2330) else Color(0xFF7A6671),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1D9E4))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF9A7D8E)
            )
            Spacer(modifier = Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF2B2330)),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = stringResource(Res.string.menugoods_search_placeholder),
                            color = Color(0xFFB395A8),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChipRow(
    chips: List<MenuGoodsUiState.CategoryChip>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(chips, key = { it.id ?: "all" }) { chip ->
            val selected = chip.id == selectedCategoryId

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selected) Color(0xFFFFD1DC) else Color(0x33FFD1DC),
                onClick = { onSelect(chip.id) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = categoryChipIcon(chip.iconKey),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (selected) Color(0xFF2B2330) else Color(0xFF6F5E68)
                    )
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color(0xFF2B2330) else Color(0xFF6F5E68),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun categoryChipIcon(iconKey: String): ImageVector = when (iconKey) {
    "food" -> Icons.Default.Restaurant
    "drink" -> Icons.Default.LocalCafe
    "dessert" -> Icons.Default.Icecream
    "goods" -> Icons.Default.Inventory2
    else -> Icons.Default.Storefront
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B5320)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.common_close),
                    tint = Color(0xFF6B5320)
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0xFFF0E2E9)), RoundedCornerShape(24.dp))
            .padding(vertical = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFEF6797))
    }
}

@Composable
private fun EmptyStateCard(
    isSearchMode: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0E2E9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFCE7EF)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = Color(0xFFEF6797),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Text(
                text = stringResource(if (isSearchMode) Res.string.menugoods_empty_search_title else Res.string.menugoods_empty_default_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2B2330)
            )
            Text(
                text = stringResource(if (isSearchMode) {
                    Res.string.menugoods_empty_search_desc
                } else {
                    Res.string.menugoods_empty_default_desc
                }),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF7B6B75)
            )
        }
    }
}

@Composable
private fun MenuItemCard(
    item: CafeMenu,
    isAvailable: Boolean,
    categoryLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0E2E9))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ItemThumbnail(
                label = item.name,
                imageUrl = item.image,
                isMenu = true,
                isAvailable = isAvailable
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2330),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatPrice(item.price),
                            color = Color(0xFFEF6797),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.menugoods_edit_content_description), tint = Color(0xFF7A6671))
                        }
                        IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.menugoods_delete_content_description), tint = Color(0xFFD96B7A))
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFFCE7EF)
                ) {
                    Text(
                        text = categoryLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB64A79),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = item.desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7B6B75),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Divider(color = Color(0xFFF4E7EE))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(if (isAvailable) Res.string.menugoods_available else Res.string.menugoods_sold_out),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isAvailable) Color(0xFF3B7B5A) else Color(0xFF8A7A82),
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { onToggleAvailability() }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoodsItemCard(
    item: Goods,
    isAvailable: Boolean,
    categoryLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailability: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0E2E9))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ItemThumbnail(
                label = item.name,
                imageUrl = item.image,
                isMenu = false,
                isAvailable = isAvailable
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2330),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatPrice(item.price),
                            color = Color(0xFFEF6797),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row {
                        IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.menugoods_edit_content_description), tint = Color(0xFF7A6671))
                        }
                        IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.menugoods_delete_content_description), tint = Color(0xFFD96B7A))
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFFFCE7EF)
                ) {
                    Text(
                        text = categoryLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFB64A79),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = stringResource(Res.string.menugoods_goods_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF7B6B75),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(Res.string.menugoods_stock, item.stock),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8B7A84),
                    fontWeight = FontWeight.Medium
                )
                Divider(color = Color(0xFFF4E7EE))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAvailable) stringResource(Res.string.menugoods_available) else stringResource(Res.string.menugoods_sold_out),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isAvailable) Color(0xFF3B7B5A) else Color(0xFF8A7A82),
                        fontWeight = FontWeight.SemiBold
                    )
                    Switch(
                        checked = isAvailable,
                        onCheckedChange = { onToggleAvailability() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemThumbnail(
    label: String,
    imageUrl: String?,
    isMenu: Boolean,
    isAvailable: Boolean
) {
    val hasImage = !imageUrl.isNullOrBlank()
    val gradient = if (isMenu) {
        Brush.linearGradient(
            colors = if (isAvailable) {
                listOf(Color(0xFFFFE0EA), Color(0xFFFAB6D0))
            } else {
                listOf(Color(0xFFF1E2EA), Color(0xFFD7C1CE))
            }
        )
    } else {
        Brush.linearGradient(
            colors = if (isAvailable) {
                listOf(Color(0xFFFFEBCB), Color(0xFFFFD7A1))
            } else {
                listOf(Color(0xFFE7E1DA), Color(0xFFCBC0B2))
            }
        )
    }
    val overlayGradient = if (isMenu) {
        Brush.linearGradient(
            colors = if (isAvailable) {
                listOf(Color(0xFFFFE0EA).copy(alpha = 0.28f), Color(0xFFFAB6D0).copy(alpha = 0.28f))
            } else {
                listOf(Color(0xFFF1E2EA).copy(alpha = 0.28f), Color(0xFFD7C1CE).copy(alpha = 0.28f))
            }
        )
    } else {
        Brush.linearGradient(
            colors = if (isAvailable) {
                listOf(Color(0xFFFFEBCB).copy(alpha = 0.28f), Color(0xFFFFD7A1).copy(alpha = 0.28f))
            } else {
                listOf(Color(0xFFE7E1DA).copy(alpha = 0.28f), Color(0xFFCBC0B2).copy(alpha = 0.28f))
            }
        )
    }

    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 108.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            CompatImageDisplay(
                imageUrl = imageUrl,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayGradient)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isMenu) Icons.Default.Storefront else Icons.Default.Inventory2,
                contentDescription = null,
                tint = Color(0xFF704A5F)
            )
            Text(
                text = label.take(1),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF704A5F),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatPrice(price: Int): String {
    return buildString {
        append("KRW ")
        append(price.toString().reversed().chunked(3).joinToString(",").reversed())
    }
}
