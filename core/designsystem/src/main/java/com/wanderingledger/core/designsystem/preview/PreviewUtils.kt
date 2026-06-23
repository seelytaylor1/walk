package com.wanderingledger.core.designsystem.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multi-preview annotation for light and dark themes.
 */
@Preview(
    name = "Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
annotation class ThemePreviews

/**
 * Preview annotation for different screen sizes.
 */
@Preview(
    name = "Phone",
    device = "spec:width=411dp,height=891dp",
)
@Preview(
    name = "Tablet",
    device = "spec:width=1280dp,height=800dp,dpi=240",
)
annotation class DevicePreviews

/**
 * Combined preview for theme and device variations.
 */
@ThemePreviews
@DevicePreviews
annotation class CompletePreviews
