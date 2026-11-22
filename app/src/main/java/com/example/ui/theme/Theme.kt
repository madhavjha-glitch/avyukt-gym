package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val GymShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AmitGymTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GymLightColorScheme,
        typography = GymTypography,
        shapes = GymShapes,
        content = content
    )
}
