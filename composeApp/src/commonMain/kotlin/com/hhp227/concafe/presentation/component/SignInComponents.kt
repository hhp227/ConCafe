package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import concafe.composeapp.generated.resources.Res
import concafe.composeapp.generated.resources.signin_divider_or
import concafe.composeapp.generated.resources.signin_logo_subtitle
import concafe.composeapp.generated.resources.maid_logo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignInLogoSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        /*Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    Brush.linearGradient(listOf(ConCafeColors.primary, ConCafeColors.secondaryContainer)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }*/
        Image(
            painter = painterResource(Res.drawable.maid_logo),
            contentDescription = "Cafe Logo",
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))
        ConCafeLogo(color = ConCafeColors.primary)
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.signin_logo_subtitle),
            color = ConCafeColors.textSecondary
        )
    }
}

@Composable
fun SignInDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.signin_divider_or),
            modifier = Modifier.padding(horizontal = 14.dp),
            color = ConCafeColors.textMuted
        )
        Divider(modifier = Modifier.weight(1f))
    }
}

@Composable
fun SignInSocialButton(
    label: String,
    icon: Painter,
    containerColor: Color,
    contentColor: Color,
    outlined: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = if (outlined) BorderStroke(1.dp, ConCafeColors.outline) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                textAlign = TextAlign.Center
            )
        }
    }
}
