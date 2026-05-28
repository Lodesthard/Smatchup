package com.example.smatchup.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.smatchup.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val Cinzel = FontFamily(
    Font(googleFont = GoogleFont("Cinzel"), fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(googleFont = GoogleFont("Cinzel"), fontProvider = provider, weight = FontWeight.Bold,   style = FontStyle.Normal),
)

val SmatchupTypography: Typography = Typography(
    displayLarge   = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 36.sp),
    displayMedium  = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineLarge  = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleLarge     = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 18.sp),
    titleMedium    = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyLarge      = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp),
    labelLarge     = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium    = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Normal, fontSize = 12.sp),
)
