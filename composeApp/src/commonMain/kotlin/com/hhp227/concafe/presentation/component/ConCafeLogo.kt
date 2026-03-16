package com.hhp227.concafe.presentation.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun ConCafeLogo(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val font = goyangFont()

    Text(
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontSize = 25.sp,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            ) { append("콘") }
            withStyle(
                SpanStyle(
                    fontSize = 7.sp,
                    color = Color.Gray,
                    baselineShift = BaselineShift(0.0f)
                )
            ) { append("셉") }
            withStyle(
                SpanStyle(
                    fontSize = 25.sp,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            ) { append("카") }
            withStyle(
                SpanStyle(
                    fontSize = 7.sp,
                    color = Color.Gray,
                    baselineShift = BaselineShift(0.0f)
                )
            ) { append("페") }
        },
        modifier = modifier,
        fontFamily = font
    )
}