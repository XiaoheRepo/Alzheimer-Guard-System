package com.xiaohelab.guard.android.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * §6.2 Typography Normal vs Large.
 * Large mode multiplier ≈ 1.25x, body minimum 20sp (HC-A11y).
 */
private fun ts(sizeSp: Int, weight: FontWeight = FontWeight.Normal, lineHeightSp: Int = (sizeSp * 1.4f).toInt()) =
    TextStyle(fontSize = sizeSp.sp, fontWeight = weight, lineHeight = lineHeightSp.sp)

val MhTypography = Typography(
    displayLarge  = ts(40, FontWeight.Bold),
    headlineLarge = ts(28, FontWeight.SemiBold),
    headlineMedium = ts(24, FontWeight.SemiBold),
    titleLarge    = ts(20, FontWeight.SemiBold),
    titleMedium   = ts(16, FontWeight.Medium),
    bodyLarge     = ts(16),
    bodyMedium    = ts(14),
    labelLarge    = ts(14, FontWeight.Medium),
    labelMedium   = ts(12, FontWeight.Medium),
)

val MhLargeTypography = Typography(
    displayLarge  = ts(52, FontWeight.Bold),
    headlineLarge = ts(36, FontWeight.SemiBold),
    headlineMedium = ts(30, FontWeight.SemiBold),
    titleLarge    = ts(26, FontWeight.SemiBold),
    titleMedium   = ts(20, FontWeight.Medium),
    bodyLarge     = ts(20),
    bodyMedium    = ts(18),
    labelLarge    = ts(18, FontWeight.Medium),
    labelMedium   = ts(16, FontWeight.Medium),
)
