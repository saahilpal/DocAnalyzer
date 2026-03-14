package com.nitrous.docanalyzer.auth.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nitrous.docanalyzer.ui.motion.AppMotion
import com.nitrous.docanalyzer.ui.theme.*

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { 
                Text(
                    text = label.uppercase(), 
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CornerRadiusMedium),
            isError = error != null,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = image, 
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.IconSizeMedium - 4.dp),
                            tint = SecondaryText
                        )
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = StrokeColor,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = HintText,
                cursorColor = Color.White,
                errorBorderColor = DestructiveRed,
                errorLabelColor = DestructiveRed,
                focusedContainerColor = ElevatedSurface,
                unfocusedContainerColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = PrimaryText, fontSize = 15.sp)
        )
        if (error != null) {
            Text(
                text = error,
                color = DestructiveRed,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = Dimens.PaddingSmall, top = Dimens.PaddingTiny)
            )
        }
    }
}

@Composable
fun AuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AppMotion.PressScaleTarget else 1.0f,
        animationSpec = if (isPressed) tween(AppMotion.PressDuration) else AppMotion.ReleaseSpring,
        label = "ButtonScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDestructive) DestructiveRed else Color.White,
            contentColor = if (isDestructive) Color.White else BackgroundBlack,
            disabledContainerColor = Color(0xFF222222),
            disabledContentColor = HintText
        ),
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimens.IconSizeMedium - 4.dp),
                color = if (isDestructive) Color.White else BackgroundBlack,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text.uppercase(), 
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) AppMotion.PressScaleTarget else 1.0f,
        animationSpec = if (isPressed) tween(AppMotion.PressDuration) else AppMotion.ReleaseSpring,
        label = "ButtonScale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        border = BorderStroke(1.dp, StrokeColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PrimaryText
        ),
        enabled = enabled,
        interactionSource = interactionSource
    ) {
        Text(
            text = text.uppercase(), 
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        )
    }
}
