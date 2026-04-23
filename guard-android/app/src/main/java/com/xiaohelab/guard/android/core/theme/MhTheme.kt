package com.xiaohelab.guard.android.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * §5.3 Compose 主题骨架；§6 大字易读模式通过 [LocalAccessibility] 注入。
 * 主题切换**不重启 Activity**（HC-Theme）。
 */

@Immutable
data class MhSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
) {
    companion object {
        val Normal = MhSpacing()
        val Large = MhSpacing(xs = 6.dp, sm = 12.dp, md = 16.dp, lg = 20.dp, xl = 28.dp, xxl = 40.dp)
    }
}

@Immutable
data class MhDimens(
    /** Min button height (§6 大字模式 56dp, 否则 Material 默认 48dp). */
    val buttonHeight: Dp,
    /** Min touch target size. */
    val touchTarget: Dp,
    /** Min icon size. */
    val iconSize: Dp,
) {
    companion object {
        val Normal = MhDimens(buttonHeight = 48.dp, touchTarget = 48.dp, iconSize = 24.dp)
        val Large = MhDimens(buttonHeight = 56.dp, touchTarget = 48.dp, iconSize = 32.dp)
    }
}

@Immutable
data class AccessibilityState(
    val largeMode: Boolean,
)

val LocalAccessibility = staticCompositionLocalOf { AccessibilityState(largeMode = false) }
val LocalMhSpacing = staticCompositionLocalOf { MhSpacing.Normal }
val LocalMhDimens = staticCompositionLocalOf { MhDimens.Normal }

val MhLightColors = lightColorScheme(
    primary = MhOrange, onPrimary = Color.White,
    primaryContainer = MhOrangeContainer, onPrimaryContainer = Color(0xFF7C2D12),
    secondary = MhSky, onSecondary = Color.White,
    tertiary = MhSky,
    error = MhErrorLight, onError = Color.White,
    background = MhBackgroundLight, onBackground = MhOnBackgroundLight,
    surface = MhSurfaceLight, onSurface = MhOnBackgroundLight,
    surfaceVariant = MhSurfaceVariantLight, onSurfaceVariant = MhOnSurfaceVariantLight,
    outline = MhOutlineLight, outlineVariant = MhOutlineVariantLight,
)

val MhDarkColors = darkColorScheme(
    primary = MhOrange, onPrimary = Color(0xFF1A0F04),
    primaryContainer = MhOrangeContainerDark, onPrimaryContainer = MhOrangeContainer,
    secondary = MhSkyLight, onSecondary = Color(0xFF082F49),
    tertiary = MhSkyLight,
    error = MhErrorDark, onError = Color(0xFF450A0A),
    background = MhBackgroundDark, onBackground = MhOnBackgroundDark,
    surface = MhSurfaceDark, onSurface = MhOnBackgroundDark,
    surfaceVariant = MhSurfaceVariantDark, onSurfaceVariant = MhOnSurfaceVariantDark,
    outline = MhOutlineDark, outlineVariant = MhOutlineVariantDark,
)

@Composable
fun MhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accessibilityLarge: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MhDarkColors else MhLightColors
    val typography = if (accessibilityLarge) MhLargeTypography else MhTypography
    val spacing = if (accessibilityLarge) MhSpacing.Large else MhSpacing.Normal
    val dimens = if (accessibilityLarge) MhDimens.Large else MhDimens.Normal
    CompositionLocalProvider(
        LocalAccessibility provides AccessibilityState(largeMode = accessibilityLarge),
        LocalMhSpacing provides spacing,
        LocalMhDimens provides dimens,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MhShapes,
            content = content,
        )
    }
}
