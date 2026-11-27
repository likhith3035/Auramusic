package com.music.bitchord.ui.screens.settings

enum class SettingsRoute(val id: String) {
    HUB("hub"),
    AUDIO("audio"),
    APPEARANCE("appearance"),
    CONTENT("content"),
    STORAGE("storage"),
    NETWORK("network"),
    ADVANCED("advanced"),
    ABOUT("about");

    companion object {
        fun fromId(id: String?): SettingsRoute? = entries.firstOrNull { it.id == id }
    }
}

