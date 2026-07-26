package com.hhp227.concafe.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ConCafeFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    readOnly: Boolean = false,
    enabled: Boolean = true,
    minLines: Int = 1,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        )
    }

    LaunchedEffect(value) {
        if (textFieldValue.text != value && textFieldValue.composition == null) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { nextValue ->
                textFieldValue = nextValue

                if (nextValue.text != value) {
                    onValueChange(nextValue.text)
                }
            },
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            minLines = minLines,
            singleLine = singleLine,
            visualTransformation = if (isPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = if (isPassword) {
                KeyboardOptions(keyboardType = KeyboardType.Password)
            } else {
                KeyboardOptions(keyboardType = keyboardType)
            },
            shape = RoundedCornerShape(16.dp),
            placeholder = {
                if (placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                }
            },
            leadingIcon = leadingContent,
            trailingIcon = trailingContent,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isSystemInDarkTheme()) colorScheme.surfaceVariant else Color.White,
                unfocusedContainerColor = if (isSystemInDarkTheme()) colorScheme.surfaceVariant else Color.White,
                focusedBorderColor = ConCafeColors.primaryContainer,
                unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.45f)
            )
        )
    }
}
