package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ThemeStyle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink

fun Modifier.neonGlow(
    themeStyle: ThemeStyle,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp
): Modifier {
    return when (themeStyle) {
        ThemeStyle.NEON_BLUE -> this
            .border(borderWidth, NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(cornerRadius))
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        val frameworkPaint = asFrameworkPaint()
                        frameworkPaint.color = NeonCyan.copy(alpha = 0.35f).toArgb()
                        frameworkPaint.setShadowLayer(
                            18.dp.toPx(),
                            0f,
                            0f,
                            NeonCyan.copy(alpha = 0.6f).toArgb()
                        )
                    }
                    canvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadius.toPx(),
                        cornerRadius.toPx(),
                        paint
                    )
                }
            }

        ThemeStyle.NEON_PURPLE -> this
            .border(borderWidth, NeonPink.copy(alpha = 0.8f), RoundedCornerShape(cornerRadius))
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        val frameworkPaint = asFrameworkPaint()
                        frameworkPaint.color = NeonPink.copy(alpha = 0.35f).toArgb()
                        frameworkPaint.setShadowLayer(
                            18.dp.toPx(),
                            0f,
                            0f,
                            NeonPink.copy(alpha = 0.6f).toArgb()
                        )
                    }
                    canvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadius.toPx(),
                        cornerRadius.toPx(),
                        paint
                    )
                }
            }

        ThemeStyle.NEON_EMERALD -> this
            .border(borderWidth, NeonEmerald.copy(alpha = 0.8f), RoundedCornerShape(cornerRadius))
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        val frameworkPaint = asFrameworkPaint()
                        frameworkPaint.color = NeonEmerald.copy(alpha = 0.35f).toArgb()
                        frameworkPaint.setShadowLayer(
                            18.dp.toPx(),
                            0f,
                            0f,
                            NeonEmerald.copy(alpha = 0.6f).toArgb()
                        )
                    }
                    canvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        cornerRadius.toPx(),
                        cornerRadius.toPx(),
                        paint
                    )
                }
            }

        ThemeStyle.CLASSIC -> this
    }
}
