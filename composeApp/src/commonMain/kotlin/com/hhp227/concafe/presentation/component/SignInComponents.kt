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
import concafe.composeapp.generated.resources.maid_logo
import org.jetbrains.compose.resources.painterResource

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
                    Brush.linearGradient(listOf(Color(0xFFEF6797), Color(0xFFF7A8C8))),
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
        ConCafeLogo(color = Color(0xFFDA4E84))
        Spacer(Modifier.height(6.dp))
        Text(
            text = "컨셉카페의 모든 것",
            color = Color(0xFF7C7180)
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
            text = "또는",
            modifier = Modifier.padding(horizontal = 14.dp),
            color = Color(0xFF8E8794)
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
        border = if (outlined) BorderStroke(1.dp, Color(0xFFE4DDE5)) else null,
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