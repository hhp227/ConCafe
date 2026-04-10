package com.hhp227.concafe.presentation.cafe.tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.domain.model.CafeMenu
import com.hhp227.concafe.domain.model.Goods
import com.hhp227.concafe.presentation.component.CompatImageDisplay
import com.hhp227.concafe.presentation.component.colorFromHex
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.cafe_goods_empty
import concafe.composeapp.generated.resources.cafe_goods_in_stock
import concafe.composeapp.generated.resources.cafe_menu_empty
import concafe.composeapp.generated.resources.cafe_menu_price
import concafe.composeapp.generated.resources.menugoods_sold_out
import concafe.composeapp.generated.resources.menugoods_tab_goods
import concafe.composeapp.generated.resources.menugoods_tab_menu
import org.jetbrains.compose.resources.stringResource

@Composable
fun CafeMenuScreen(
    menus: List<CafeMenu>,
    goods: List<Goods>,
    isLoading: Boolean
) {
    val hasMenu = menus.isNotEmpty()
    val hasGoods = goods.isNotEmpty()

    if (isLoading && !hasMenu && !hasGoods) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = colorFromHex("EF6797"),
                strokeWidth = 2.dp
            )
        }
        return
    }
    if (!hasMenu && !hasGoods) {
        EmptyContent(text = stringResource(Res.string.cafe_menu_empty))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        if (hasMenu) {
            MenuSection(menus)
        }
        if (hasGoods) {
            GoodsSection(goods)
        }
    }
}

@Composable
private fun MenuSection(menus: List<CafeMenu>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(Res.string.menugoods_tab_menu))
        menus.forEach { menu ->
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = if (!menu.image.isNullOrBlank()) {
                                        listOf(colorFromHex("FFD8E8"), colorFromHex("F5AFCC"))
                                    } else {
                                        listOf(colorFromHex("FFE2D2"), colorFromHex("FFC9A9"))
                                    }
                                )
                            )
                    ) {
                        if (!menu.image.isNullOrBlank()) {
                            CompatImageDisplay(
                                imageUrl = menu.image,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                colorFromHex("FFD8E8").copy(alpha = 0.28f),
                                                colorFromHex("F5AFCC").copy(alpha = 0.28f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = menu.name,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(Res.string.cafe_menu_price, menu.price),
                            color = colorFromHex("EF6797"),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = menu.desc,
                            color = Color(0xFF777777),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoodsSection(goods: List<Goods>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(Res.string.menugoods_tab_goods))
        goods.chunked(2).forEach { rowGoods ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowGoods.forEach { good ->
                    GoodsTile(good = good, modifier = Modifier.weight(1f))
                }
                if (rowGoods.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GoodsTile(good: Goods, modifier: Modifier = Modifier) {
    val isInStock = good.stock > 0
    val soldOutLabel = stringResource(Res.string.menugoods_sold_out)
    val inStockLabel = stringResource(Res.string.cafe_goods_in_stock)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        Brush.verticalGradient(
                            colors = if (!good.image.isNullOrBlank()) {
                                listOf(colorFromHex("FFD8E8"), colorFromHex("F5AFCC"))
                            } else {
                                listOf(colorFromHex("FFE2D2"), colorFromHex("FFC9A9"))
                            }
                        )
                    )
            ) {
                if (!good.image.isNullOrBlank()) {
                    CompatImageDisplay(
                        imageUrl = good.image,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (!isInStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = soldOutLabel,
                            color = Color(0xFF2B2330),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = good.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(Res.string.cafe_menu_price, good.price),
                        color = colorFromHex("EF6797"),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isInStock) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = inStockLabel,
                                color = Color(0xFF16A34A),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmptyContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(width = 1.dp, color = Color(0xFFF0E4EA), shape = RoundedCornerShape(24.dp))
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF777777)
        )
    }
}
