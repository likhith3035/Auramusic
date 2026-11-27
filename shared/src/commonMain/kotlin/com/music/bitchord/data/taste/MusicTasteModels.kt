package com.music.bitchord.data.taste

import kotlinx.serialization.Serializable

/**
 * Distinguishes composers/producers from vocalists so listeners can filter
 * during onboarding and settings taste personalization.
 */
@Serializable
enum class ArtistRole(val label: String, val singularLabel: String) {
    ALL("All", "Artist"),
    MUSIC_DIRECTOR("Music Directors", "Music Director"),
    SINGER("Singers", "Singer"),
}

/**
 * A regional or global music tradition offered during taste personalization.
 * Stored independently from the UI localization language.
 */
@Serializable
data class MusicLanguageOption(
    val id: String,
    val displayName: String,
    val nativeScript: String,
    val accentHex: Long,
    val subtitle: String,
)

/**
 * Curated seed artist or music director paired with a verified official YouTube Music
 * channel id so avatars and "Fans might also like" cascades match accurately.
 */
@Serializable
data class CuratedArtistSeed(
    val query: String,
    val displayName: String,
    val role: ArtistRole,
    val languageId: String,
    val knownBrowseId: String? = null,
)

/**
 * A concrete artist or music director picked by the listener or suggested dynamically.
 */
@Serializable
data class TasteArtistPick(
    val browseId: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val role: ArtistRole = ArtistRole.SINGER,
    val languageId: String = "english",
    val isRelatedSuggestion: Boolean = false,
    val relatedToArtistName: String? = null,
)

/**
 * Persisted user music taste preferences across Android and Desktop.
 */
@Serializable
data class MusicTastePreferences(
    val selectedLanguageIds: List<String> = emptyList(),
    val selectedArtists: List<TasteArtistPick> = emptyList(),
    val tasteConfigured: Boolean = false,
    val autoSubscribeOnSignIn: Boolean = true,
    val updatedAtMs: Long = 0L,
)

/**
 * Curated catalog of music languages, flagship composers/music directors, and vocalists.
 */
object MusicTasteCatalog {

    val LANGUAGES: List<MusicLanguageOption> = listOf(
        MusicLanguageOption("telugu", "Telugu", "తెలుగు", 0xFF22C55E, "Tollywood & melodies"),
        MusicLanguageOption("hindi", "Hindi", "हिन्दी", 0xFFEF4444, "Bollywood & indie soul"),
        MusicLanguageOption("tamil", "Tamil", "தமிழ்", 0xFFF59E0B, "Kollywood scores"),
        MusicLanguageOption("english", "English", "International", 0xFF38BDF8, "Pop, hip-hop & rock"),
        MusicLanguageOption("malayalam", "Malayalam", "മലയാളം", 0xFF10B981, "Mollywood & acoustic"),
        MusicLanguageOption("kannada", "Kannada", "ಕನ್ನಡ", 0xFFF97316, "Sandalwood & fusion"),
        MusicLanguageOption("punjabi", "Punjabi", "ਪੰਜਾਬੀ", 0xFFA855F7, "Urban Desi & beats"),
        MusicLanguageOption("marathi", "Marathi", "मराठी", 0xFFEC4899, "Cinema & folk"),
        MusicLanguageOption("bengali", "Bengali", "বাংলা", 0xFF3B82F6, "Indie & classics"),
        MusicLanguageOption("gujarati", "Gujarati", "ગુજરાતી", 0xFFE11D48, "Garba & folk"),
        MusicLanguageOption("bhojpuri", "Bhojpuri", "भोजपुरी", 0xFF14B8A6, "Folk & dance hits"),
        MusicLanguageOption("korean", "Korean", "한국어", 0xFF8B5CF6, "K-Pop & OSTs"),
    )

    val CURATED_ARTISTS: List<CuratedArtistSeed> = listOf(
        // Telugu — Music Directors & Singers
        CuratedArtistSeed("Devi Sri Prasad", "Devi Sri Prasad", ArtistRole.MUSIC_DIRECTOR, "telugu", "UCwHleA-qNWiMl05mwTDbuWA"),
        CuratedArtistSeed("Thaman S", "Thaman S", ArtistRole.MUSIC_DIRECTOR, "telugu", "UCLnRv17aPDpTM_Z_2nHFikA"),
        CuratedArtistSeed("Anirudh Ravichander", "Anirudh Ravichander", ArtistRole.MUSIC_DIRECTOR, "telugu", "UCbRSywya_rl8YS15Lo9ttsA"),
        CuratedArtistSeed("M. M. Keeravani", "M. M. Keeravani", ArtistRole.MUSIC_DIRECTOR, "telugu", "UCuO2zYTV1HzP9D6Dei_3-aA"),
        CuratedArtistSeed("Mani Sharma", "Mani Sharma", ArtistRole.MUSIC_DIRECTOR, "telugu", "UCnjwRkvII1DDqPj34ERsXmA"),
        CuratedArtistSeed("Mickey J Meyer", "Mickey J Meyer", ArtistRole.MUSIC_DIRECTOR, "telugu", "UCe7EX_oZTueMu-sLTO7Ub0g"),
        CuratedArtistSeed("Sid Sriram", "Sid Sriram", ArtistRole.SINGER, "telugu", "UC7_KgmSrwM247k2lnh5GhHw"),
        CuratedArtistSeed("S. P. Balasubrahmanyam", "S. P. Balasubrahmanyam", ArtistRole.SINGER, "telugu", "UCl4iPtukwe7m0kIxUMskkgA"),
        CuratedArtistSeed("Geetha Madhuri", "Geetha Madhuri", ArtistRole.SINGER, "telugu", "UC-rssJXkjsqa0XcP8jHzm6w"),
        CuratedArtistSeed("Karthik", "Karthik", ArtistRole.SINGER, "telugu", "UCO59Hgc5qAqhLr6EW9vy9pg"),
        CuratedArtistSeed("Sunitha", "Sunitha", ArtistRole.SINGER, "telugu", "UC72dVmCeyr-zJxgRsRhKXYw"),
        CuratedArtistSeed("Ram Miriyala", "Ram Miriyala", ArtistRole.SINGER, "telugu", "UCK5zappMYMGEvFBGxR7md6g"),

        // Hindi — Music Directors & Singers
        CuratedArtistSeed("Pritam", "Pritam", ArtistRole.MUSIC_DIRECTOR, "hindi", "UCCTN01plFzn4npREHKT2_9Q"),
        CuratedArtistSeed("A. R. Rahman", "A. R. Rahman", ArtistRole.MUSIC_DIRECTOR, "hindi", "UCtJe0RYzgPddQXKtWduxz_w"),
        CuratedArtistSeed("Amit Trivedi", "Amit Trivedi", ArtistRole.MUSIC_DIRECTOR, "hindi", "UC19pnd-F7tw6vDhkmEiAkgA"),
        CuratedArtistSeed("Sachin-Jigar", "Sachin-Jigar", ArtistRole.MUSIC_DIRECTOR, "hindi", "UCcuXjaACiyViK8QTuztjR6A"),
        CuratedArtistSeed("Vishal - Shekhar", "Vishal-Shekhar", ArtistRole.MUSIC_DIRECTOR, "hindi", "UCWXJyrk1diAufFuTHCTfQ2A"),
        CuratedArtistSeed("Mithoon", "Mithoon", ArtistRole.MUSIC_DIRECTOR, "hindi", "UCHxwQajTiv8div1Fe-yvWpA"),
        CuratedArtistSeed("Arijit Singh", "Arijit Singh", ArtistRole.SINGER, "hindi", "UCDxKh1gFWeYsqePvgVzmPoQ"),
        CuratedArtistSeed("Shreya Ghoshal", "Shreya Ghoshal", ArtistRole.SINGER, "hindi", "UCrC-7fsdTCYeaRBpwA6j-Eg"),
        CuratedArtistSeed("Sonu Nigam", "Sonu Nigam", ArtistRole.SINGER, "hindi", "UCsC4u-BJAd4OX1hJXtwXSOQ"),
        CuratedArtistSeed("Atif Aslam", "Atif Aslam", ArtistRole.SINGER, "hindi", "UCVGomUS__PL0c4jDXa0QwXA"),
        CuratedArtistSeed("Jubin Nautiyal", "Jubin Nautiyal", ArtistRole.SINGER, "hindi", "UCdWuR07og626xwU93eSCh9A"),
        CuratedArtistSeed("Sunidhi Chauhan", "Sunidhi Chauhan", ArtistRole.SINGER, "hindi", "UC41L0sFqy6_Q43RGgUnJ75Q"),

        // Tamil — Music Directors & Singers
        CuratedArtistSeed("Anirudh Ravichander", "Anirudh Ravichander", ArtistRole.MUSIC_DIRECTOR, "tamil", "UCbRSywya_rl8YS15Lo9ttsA"),
        CuratedArtistSeed("A. R. Rahman", "A. R. Rahman", ArtistRole.MUSIC_DIRECTOR, "tamil", "UCtJe0RYzgPddQXKtWduxz_w"),
        CuratedArtistSeed("Yuvan Shankar Raja", "Yuvan Shankar Raja", ArtistRole.MUSIC_DIRECTOR, "tamil", "UCQXg6kTstIOwbrjBwE3IlDw"),
        CuratedArtistSeed("Ilaiyaraaja", "Ilaiyaraaja", ArtistRole.MUSIC_DIRECTOR, "tamil", "UCwzzSiogpjsYBMoyVcwDXwQ"),
        CuratedArtistSeed("Harris Jayaraj", "Harris Jayaraj", ArtistRole.MUSIC_DIRECTOR, "tamil", "UCv6xCP3bn5Pdq0E4SJ4iEKg"),
        CuratedArtistSeed("Santhosh Narayanan", "Santhosh Narayanan", ArtistRole.MUSIC_DIRECTOR, "tamil", "UCAMJ6FZcDvdrSRoYcZ14x1Q"),
        CuratedArtistSeed("G. V. Prakash Kumar", "G. V. Prakash Kumar", ArtistRole.MUSIC_DIRECTOR, "tamil", "UCK-E95XlAlzJtXKIrF4-hjA"),
        CuratedArtistSeed("Sid Sriram", "Sid Sriram", ArtistRole.SINGER, "tamil", "UC7_KgmSrwM247k2lnh5GhHw"),
        CuratedArtistSeed("Pradeep Kumar", "Pradeep Kumar", ArtistRole.SINGER, "tamil", "UC0tPcuSe25Ly0wksSzN4iKg"),
        CuratedArtistSeed("Jonita Gandhi", "Jonita Gandhi", ArtistRole.SINGER, "tamil", "UC3eFGgfV2cvYB_0cQnh_omg"),
        CuratedArtistSeed("Shreya Ghoshal", "Shreya Ghoshal", ArtistRole.SINGER, "tamil", "UCrC-7fsdTCYeaRBpwA6j-Eg"),

        // English / International
        CuratedArtistSeed("Metro Boomin", "Metro Boomin", ArtistRole.MUSIC_DIRECTOR, "english", "UCFYaIRiUe_1hvOtXu5IiBWQ"),
        CuratedArtistSeed("Hans Zimmer", "Hans Zimmer", ArtistRole.MUSIC_DIRECTOR, "english", "UCfCNL5oajlQBAlyjWv1ChVw"),
        CuratedArtistSeed("Ludwig Goransson", "Ludwig Goransson", ArtistRole.MUSIC_DIRECTOR, "english", "UCOpLOH5uXtBzwc7LKOr0y-Q"),
        CuratedArtistSeed("Fred again..", "Fred again..", ArtistRole.MUSIC_DIRECTOR, "english", "UCvA7ExlBsBUb5XQEYzJC1ww"),
        CuratedArtistSeed("The Weeknd", "The Weeknd", ArtistRole.SINGER, "english", "UClYV6hHlupm_S_ObS1W-DYw"),
        CuratedArtistSeed("Taylor Swift", "Taylor Swift", ArtistRole.SINGER, "english", "UCPC0L1d253x-KuMNwa05TpA"),
        CuratedArtistSeed("Billie Eilish", "Billie Eilish", ArtistRole.SINGER, "english", "UCERrDZ8oN0U_n9MphMKERcg"),
        CuratedArtistSeed("Kendrick Lamar", "Kendrick Lamar", ArtistRole.SINGER, "english", "UCprAFmT0C6O4X0ToEXpeFTQ"),
        CuratedArtistSeed("Dua Lipa", "Dua Lipa", ArtistRole.SINGER, "english", "UCzVb0SIXp9q9PeKCcFjsBtA"),
        CuratedArtistSeed("Coldplay", "Coldplay", ArtistRole.SINGER, "english", "UCIaFw5VBEK8qaW6nRpx_qnw"),

        // Malayalam
        CuratedArtistSeed("Sushin Shyam", "Sushin Shyam", ArtistRole.MUSIC_DIRECTOR, "malayalam", "UCzpmF_8698JYBqh96PPCTfw"),
        CuratedArtistSeed("Hesham Abdul Wahab", "Hesham Abdul Wahab", ArtistRole.MUSIC_DIRECTOR, "malayalam", "UC0aV0qinwdUmB2NuEhfRS_w"),
        CuratedArtistSeed("Govind Vasantha", "Govind Vasantha", ArtistRole.MUSIC_DIRECTOR, "malayalam", "UC3UB9XRMBprHRUXLMjj8t9A"),
        CuratedArtistSeed("Jakes Bejoy", "Jakes Bejoy", ArtistRole.MUSIC_DIRECTOR, "malayalam", "UCF35kALVhAo3kWiREPpkfmQ"),
        CuratedArtistSeed("K. S. Chithra", "K. S. Chithra", ArtistRole.SINGER, "malayalam", "UC9VthIFaCM85yb8BBluF1Ag"),
        CuratedArtistSeed("Vineeth Sreenivasan", "Vineeth Sreenivasan", ArtistRole.SINGER, "malayalam", "UCjYCNtvEZzfcj_69by4umCA"),
        CuratedArtistSeed("Vijay Yesudas", "Vijay Yesudas", ArtistRole.SINGER, "malayalam", "UCPqQ6MQrWAB3gigb_ut-aMQ"),

        // Kannada
        CuratedArtistSeed("Ravi Basrur", "Ravi Basrur", ArtistRole.MUSIC_DIRECTOR, "kannada", "UCTEZlIfENA3wuJHrEBQNarA"),
        CuratedArtistSeed("Arjun Janya", "Arjun Janya", ArtistRole.MUSIC_DIRECTOR, "kannada", "UCUygKAW0WF4xJof8s8opK0A"),
        CuratedArtistSeed("B. Ajaneesh Loknath", "Ajaneesh Loknath", ArtistRole.MUSIC_DIRECTOR, "kannada", "UCYdqhysEsL3xzTjElSOPYdA"),
        CuratedArtistSeed("Charan Raj", "Charan Raj", ArtistRole.MUSIC_DIRECTOR, "kannada", "UCbpTuRpwAwO4D4ceP7rTOhA"),
        CuratedArtistSeed("Vijay Prakash", "Vijay Prakash", ArtistRole.SINGER, "kannada", "UCwQE3u490GTgL3KkGylwpkQ"),
        CuratedArtistSeed("Sanjith Hegde", "Sanjith Hegde", ArtistRole.SINGER, "kannada", "UCGCzMLhz0ucUUACQAHw-rmg"),

        // Punjabi
        CuratedArtistSeed("B Praak", "B Praak", ArtistRole.MUSIC_DIRECTOR, "punjabi", "UC3RXV3x1J7HdgHskfDyVeEw"),
        CuratedArtistSeed("Jaani", "Jaani", ArtistRole.MUSIC_DIRECTOR, "punjabi", "UCjjUhXXWXX7x5fAasF0FMDg"),
        CuratedArtistSeed("Diljit Dosanjh", "Diljit Dosanjh", ArtistRole.SINGER, "punjabi", "UCJ2m-WpROlZCiZZID9r7NSQ"),
        CuratedArtistSeed("AP Dhillon", "AP Dhillon", ArtistRole.SINGER, "punjabi", "UCQmNiXx378nooDuZPA2fTAg"),
        CuratedArtistSeed("Karan Aujla", "Karan Aujla", ArtistRole.SINGER, "punjabi", "UCSmK5WX5U4gdtebWjoL81og"),
        CuratedArtistSeed("Sidhu Moose Wala", "Sidhu Moose Wala", ArtistRole.SINGER, "punjabi", "UCIOXXUXQ8y5ivei97JkiBAw"),
        CuratedArtistSeed("Shubh", "Shubh", ArtistRole.SINGER, "punjabi", "UCDoxhZGShhNvN4Bc3nWZptg"),

        // Marathi
        CuratedArtistSeed("Ajay-Atul", "Ajay-Atul", ArtistRole.MUSIC_DIRECTOR, "marathi", "UCydVTh4ercNX9cKyYFhuxMw"),
        CuratedArtistSeed("Avadhoot Gupte", "Avadhoot Gupte", ArtistRole.MUSIC_DIRECTOR, "marathi", "UCG23kuLBdi09ovPkx5jjWsA"),
        CuratedArtistSeed("Shankar Mahadevan", "Shankar Mahadevan", ArtistRole.SINGER, "marathi", "UCX4g5pMVYlYN9K6D8n6HmGg"),
        CuratedArtistSeed("Bela Shende", "Bela Shende", ArtistRole.SINGER, "marathi", "UC14XfAyGPD17zR8Ak0BU3AQ"),
        CuratedArtistSeed("Arya Ambekar", "Arya Ambekar", ArtistRole.SINGER, "marathi", "UCoHEjUXauqqcf3rXjySzCpg"),

        // Bengali
        CuratedArtistSeed("Anupam Roy", "Anupam Roy", ArtistRole.MUSIC_DIRECTOR, "bengali", "UCahH3l5sz429SqebyK5ZCbA"),
        CuratedArtistSeed("Rupam Islam", "Rupam Islam", ArtistRole.SINGER, "bengali", "UCxWh0Wsxr6lx1IO_T11TYEA"),
        CuratedArtistSeed("Arijit Singh", "Arijit Singh", ArtistRole.SINGER, "bengali", "UCDxKh1gFWeYsqePvgVzmPoQ"),

        // Gujarati
        CuratedArtistSeed("Sachin-Jigar", "Sachin-Jigar", ArtistRole.MUSIC_DIRECTOR, "gujarati", "UCcuXjaACiyViK8QTuztjR6A"),
        CuratedArtistSeed("Aditya Gadhvi", "Aditya Gadhvi", ArtistRole.SINGER, "gujarati", "UC3y5yjSpNh7OChQ6J7CEZgQ"),
        CuratedArtistSeed("Jigardan Gadhavi", "Jigardan Gadhavi", ArtistRole.SINGER, "gujarati", "UClftjG4058YgPp7_f0v0HKg"),
        CuratedArtistSeed("Darshan Raval", "Darshan Raval", ArtistRole.SINGER, "gujarati", "UCNMewQFJlBeyXpWwo2dezSw"),

        // Bhojpuri
        CuratedArtistSeed("Pawan Singh", "Pawan Singh", ArtistRole.SINGER, "bhojpuri", "UCN9zAwrHTeVTYYbkS2wniAg"),
        CuratedArtistSeed("Khesari Lal Yadav", "Khesari Lal Yadav", ArtistRole.SINGER, "bhojpuri", "UCxpu57trwVlTicWq1MEZ5qg"),
        CuratedArtistSeed("Shilpi Raj", "Shilpi Raj", ArtistRole.SINGER, "bhojpuri", "UCYMthbEErlf2Jq10A9tcEbA"),
        CuratedArtistSeed("Neelkamal Singh", "Neelkamal Singh", ArtistRole.SINGER, "bhojpuri", "UCmRRdlhQ1vVYw2sFYt7yzJA"),

        // Korean
        CuratedArtistSeed("SUGA", "Agust D / SUGA", ArtistRole.MUSIC_DIRECTOR, "korean", "UCzoDKUSaHvCswDxO1yH3zYw"),
        CuratedArtistSeed("BTS", "BTS", ArtistRole.SINGER, "korean", "UC9vrvNSL3xcWGSkV86REBSg"),
        CuratedArtistSeed("BLACKPINK", "BLACKPINK", ArtistRole.SINGER, "korean", "UCkbbMCA40i18i7UdjayMPAg"),
        CuratedArtistSeed("IU", "IU", ArtistRole.SINGER, "korean", "UCTUR0sVEkD8T5MlSHqgaI_Q"),
        CuratedArtistSeed("NewJeans", "NewJeans", ArtistRole.SINGER, "korean", "UCxOqS3cYg4FaHbobICo7nFQ"),
    )

    fun languageById(id: String): MusicLanguageOption? =
        LANGUAGES.firstOrNull { it.id == id }

    fun defaultLanguageSelection(): List<String> = listOf("hindi", "telugu", "english")

    fun seedsForLanguages(
        languageIds: List<String>,
        role: ArtistRole = ArtistRole.ALL,
    ): List<CuratedArtistSeed> {
        val activeIds = if (languageIds.isEmpty()) defaultLanguageSelection() else languageIds
        val filtered = CURATED_ARTISTS.filter { seed ->
            seed.languageId in activeIds && (role == ArtistRole.ALL || seed.role == role)
        }
        val seen = HashSet<String>()
        return filtered.filter { seen.add(it.displayName.lowercase()) }
    }

    fun inferRoleByName(name: String): ArtistRole? {
        val norm = name.trim().lowercase()
        return CURATED_ARTISTS.firstOrNull { it.displayName.lowercase() == norm }?.role
    }

    fun seedBrowseId(languageId: String, name: String): String =
        "seed:$languageId:${name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_")}"
}
