package com.hhp227.concafe.core.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hhp227.concafe.presentation.component.ConCafeColors

fun formatKoreanPhoneNumber(input: String): String {
    val digits = input.filter { it.isDigit() }.take(11)
    return when {
        digits.length <= 3 -> digits
        digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
        else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
    }
}

fun normalizeKoreanPhoneToE164(input: String): String? {
    val digitsOnly = input.filter { char -> char.isDigit() }

    return if (digitsOnly.isBlank()) {
        null
    } else if (digitsOnly.startsWith("82")) {
        "+$digitsOnly"
    } else if (digitsOnly.startsWith("0")) {
        "+82${digitsOnly.drop(1)}"
    } else if (digitsOnly.startsWith("10")) {
        "+82$digitsOnly"
    } else {
        null
    }
}

/**
 * 전화번호 전용 입력 필드. 숫자만 허용하며 010-XXXX-XXXX 포맷으로 자동 변환,
 * 대시 삽입 후에도 커서를 항상 끝에 유지한다.
 * ConCafeFormField 스타일(라벨 위 배치, 핑크 테두리)과 동일.
 */
@Composable
fun PhoneNumberTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "010-1234-5678",
    trailingContent: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    var fieldValue by remember(value) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = ConCafeColors.textSecondary
        )
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { new ->
                val formatted = formatKoreanPhoneNumber(new.text)
                fieldValue = TextFieldValue(formatted, TextRange(formatted.length))
                onValueChange(formatted)
            },
            placeholder = { Text(text = placeholder, color = ConCafeColors.textMuted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            trailingIcon = trailingContent,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ConCafeColors.surfaceVariant,
                unfocusedContainerColor = ConCafeColors.surfaceVariant,
                focusedBorderColor = ConCafeColors.primaryContainer,
                unfocusedBorderColor = ConCafeColors.primaryContainer.copy(alpha = 0.3f)
            )
        )
    }
}
