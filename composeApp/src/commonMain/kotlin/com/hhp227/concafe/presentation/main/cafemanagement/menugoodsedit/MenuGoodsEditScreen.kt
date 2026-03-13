package com.hhp227.concafe.presentation.main.cafemanagement.menugoodsedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hhp227.concafe.di.resolveGetCafeDetailUseCase
import com.hhp227.concafe.di.resolveUpsertCafeMenuGoodsUseCase
import com.hhp227.concafe.presentation.component.ConCafeFormField
import com.hhp227.concafe.presentation.navigation.NavigationAction

@Composable
fun MenuGoodsEditScreen(
    cafeId: String,
    itemId: String?,
    onNavigationAction: (NavigationAction) -> Unit,
    viewModel: MenuGoodsEditViewModel = viewModel(
        key = "menu-goods-edit-$cafeId-${itemId ?: "new"}",
        factory = viewModelFactory {
            initializer {
                MenuGoodsEditViewModel(
                    cafeId = cafeId,
                    itemId = itemId,
                    getCafeDetailUseCase = resolveGetCafeDetailUseCase(),
                    upsertCafeMenuGoodsUseCase = resolveUpsertCafeMenuGoodsUseCase()
                )
            }
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
                        text = uiState.screenTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(MenuGoodsEditAction.ClickBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
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
                            containerColor = Color(0xFFFFD1DC),
                            contentColor = Color(0xFF2B2330)
                        )
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    PhotoUploadCard(
                        imageUrl = uiState.imageUrl,
                        onClick = { onAction(MenuGoodsEditAction.ClickPhotoUpload) }
                    )
                }
                uiState.infoMessage?.let { message ->
                    item {
                        InfoBanner(
                            message = message,
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
                            label = "항목 이름",
                            value = uiState.itemName,
                            onValueChange = { onAction(MenuGoodsEditAction.ChangeName(it)) },
                            placeholder = "예: 딸기 메이드 파르페"
                        )
                    }
                    item {
                        PriceField(
                            label = "가격",
                            value = uiState.price,
                            onValueChange = { onAction(MenuGoodsEditAction.ChangePrice(it)) }
                        )
                    }
                    item {
                        FormField(
                            label = "카테고리"
                        ) {
                            CategoryGrid(
                                selectedCategory = uiState.selectedCategory,
                                onSelect = { onAction(MenuGoodsEditAction.SelectCategory(it)) }
                            )
                        }
                    }
                    item {
                        DescriptionField(
                            label = "설명",
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.8f)
            ) {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = null,
                    tint = Color(0xFF6F5968),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Text(
                text = "항목 사진 업로드",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2B2330)
            )
            Text(
                text = "JPG, PNG 최대 5MB",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7A6671)
            )
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
            color = Color(0xFF4D404A),
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
                text = "¥",
                color = Color(0xFF6B5A65),
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}

@Composable
private fun CategoryGrid(
    selectedCategory: MenuGoodsEditUiState.ItemCategory,
    onSelect: (MenuGoodsEditUiState.ItemCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val categories = MenuGoodsEditUiState.ItemCategory.entries.chunked(2)
        categories.forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCategories.forEach { category ->
                    val isSelected = category == selectedCategory
                    CategoryButton(
                        modifier = Modifier.weight(1f),
                        category = category,
                        isSelected = isSelected,
                        onClick = { onSelect(category) }
                    )
                }
                if (rowCategories.size == 1) {
                    SpacerCell(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpacerCell(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryButton(
    modifier: Modifier = Modifier,
    category: MenuGoodsEditUiState.ItemCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0x33FFD1DC) else Color(0xFFF8F5F6),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFFFFD1DC) else Color(0x55FFD1DC)
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
                imageVector = category.icon(),
                contentDescription = null,
                tint = if (isSelected) Color(0xFF2B2330) else Color(0xFF6E6169)
            )
            Text(
                text = category.label,
                modifier = Modifier.padding(start = 8.dp),
                color = if (isSelected) Color(0xFF2B2330) else Color(0xFF6E6169),
                fontWeight = FontWeight.Medium
            )
        }
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
        placeholder = "재료 또는 특징을 설명해주세요...",
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F5F6)),
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
                    tint = Color(0xFFFF8AA8)
                )
                Column {
                    Text(
                        text = "재고 상태",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2B2330)
                    )
                    Text(
                        text = if (isInStock) "현재 판매 가능 상태입니다." else "현재 품절 상태입니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF7A6671)
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
            Text(
                text = "닫기",
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable(onClick = onDismiss),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6B5320),
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
            .background(Color.White)
            .border(BorderStroke(1.dp, Color(0x55FFD1DC)), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFFFF8AA8))
            Text(
                text = "항목 정보를 준비하고 있습니다.",
                color = Color(0xFF7A6671),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun MenuGoodsEditUiState.ItemCategory.icon(): ImageVector {
    return when (this) {
        MenuGoodsEditUiState.ItemCategory.DRINK -> Icons.Default.LocalCafe
        MenuGoodsEditUiState.ItemCategory.FOOD -> Icons.Default.Restaurant
        MenuGoodsEditUiState.ItemCategory.DESSERT -> Icons.Default.Icecream
        MenuGoodsEditUiState.ItemCategory.GOODS -> Icons.Default.FeaturedPlayList
    }
}
