package com.driver.portal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.driver.portal.R

val AlyamamaFamily = FontFamily(
    Font(R.font.alyamama_light, FontWeight.Light),
    Font(R.font.alyamama_regular, FontWeight.Normal),
    Font(R.font.alyamama_medium, FontWeight.Medium),
    Font(R.font.alyamama_medium, FontWeight.SemiBold),
    Font(R.font.alyamama_bold, FontWeight.Bold),
    Font(R.font.alyamama_bold, FontWeight.ExtraBold),
)

private val BaseTypography = Typography()

private fun TextStyle.withAlyamama(): TextStyle = copy(fontFamily = AlyamamaFamily)

val Typography = Typography(
    displayLarge = BaseTypography.displayLarge.withAlyamama(),
    displayMedium = BaseTypography.displayMedium.withAlyamama(),
    displaySmall = BaseTypography.displaySmall.withAlyamama(),
    headlineLarge = BaseTypography.headlineLarge.withAlyamama(),
    headlineMedium = BaseTypography.headlineMedium.withAlyamama(),
    headlineSmall = TextStyle(
        fontFamily = AlyamamaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AlyamamaFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AlyamamaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = BaseTypography.titleSmall.withAlyamama(),
    bodyLarge = TextStyle(
        fontFamily = AlyamamaFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AlyamamaFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodySmall = BaseTypography.bodySmall.withAlyamama(),
    labelLarge = BaseTypography.labelLarge.withAlyamama(),
    labelMedium = BaseTypography.labelMedium.withAlyamama(),
    labelSmall = TextStyle(
        fontFamily = AlyamamaFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
)
