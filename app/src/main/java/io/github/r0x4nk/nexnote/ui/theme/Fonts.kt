package io.github.r0x4nk.nexnote.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.r0x4nk.nexnote.R
import io.github.r0x4nk.nexnote.domain.model.AppFont

/**
 * Maps the persisted [AppFont] preference to a Compose [FontFamily].
 *
 * [AppFont.SYSTEM] keeps the platform typeface; the other entries resolve to
 * bundled OFL variable fonts. Each variable file is registered once per weight
 * the typography uses, keeping the assets small while preserving text weight.
 */
@OptIn(ExperimentalTextApi::class)
fun AppFont.fontFamily(): FontFamily = when (this) {
    AppFont.SYSTEM -> FontFamily.Default
    AppFont.INTER -> variableFontFamily(R.font.inter_variable)
    AppFont.LORA -> variableFontFamily(R.font.lora_variable)
    AppFont.FIRA_CODE -> variableFontFamily(R.font.fira_code_variable)
    AppFont.JETBRAINS_MONO -> variableFontFamily(R.font.jetbrains_mono_variable)
}

@OptIn(ExperimentalTextApi::class)
private fun variableFontFamily(resId: Int): FontFamily = FontFamily(
    variableWeight(resId, FontWeight.Normal, 400),
    variableWeight(resId, FontWeight.Medium, 500),
    variableWeight(resId, FontWeight.SemiBold, 600),
    variableWeight(resId, FontWeight.Bold, 700)
)

@OptIn(ExperimentalTextApi::class)
private fun variableWeight(resId: Int, weight: FontWeight, axisWeight: Int): Font = Font(
    resId = resId,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(axisWeight))
)
