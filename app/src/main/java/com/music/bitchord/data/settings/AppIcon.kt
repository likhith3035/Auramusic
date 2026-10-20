package com.music.bitchord.data.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.music.bitchord.R

enum class AppIcon(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val aliasName: String,
    @DrawableRes val previewRes: Int,
) {
    DEFAULT(
        id = "default",
        titleRes = R.string.app_icon_default,
        subtitleRes = R.string.app_icon_default_desc,
        aliasName = "com.music.bitchord.MainActivityDefault",
        previewRes = R.drawable.ic_preview_default,
    ),
    GLASS_AZURE(
        id = "glass_azure",
        titleRes = R.string.app_icon_glass_azure,
        subtitleRes = R.string.app_icon_glass_azure_desc,
        aliasName = "com.music.bitchord.MainActivityGlassAzure",
        previewRes = R.drawable.ic_preview_glass_azure,
    ),
    GLASS_OBSIDIAN(
        id = "glass_obsidian",
        titleRes = R.string.app_icon_glass_obsidian,
        subtitleRes = R.string.app_icon_glass_obsidian_desc,
        aliasName = "com.music.bitchord.MainActivityGlassObsidian",
        previewRes = R.drawable.ic_preview_glass_obsidian,
    ),
    GLASS_CRIMSON(
        id = "glass_crimson",
        titleRes = R.string.app_icon_glass_crimson,
        subtitleRes = R.string.app_icon_glass_crimson_desc,
        aliasName = "com.music.bitchord.MainActivityGlassCrimson",
        previewRes = R.drawable.ic_preview_glass_crimson,
    ),
    GLASS_AMETHYST(
        id = "glass_amethyst",
        titleRes = R.string.app_icon_glass_amethyst,
        subtitleRes = R.string.app_icon_glass_amethyst_desc,
        aliasName = "com.music.bitchord.MainActivityGlassAmethyst",
        previewRes = R.drawable.ic_preview_glass_amethyst,
    ),
    GLASS_PRISM(
        id = "glass_prism",
        titleRes = R.string.app_icon_glass_prism,
        subtitleRes = R.string.app_icon_glass_prism_desc,
        aliasName = "com.music.bitchord.MainActivityGlassPrism",
        previewRes = R.drawable.ic_preview_glass_prism,
    );

    companion object {
        fun fromId(id: String?): AppIcon {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
