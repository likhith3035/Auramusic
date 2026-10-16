package com.music.bitchord.data.stats

/**
 * Distinguishes lead singers / vocalists from music directors, composers, and producers.
 *
 * Designed to accurately classify multi-artist credits (especially prevalent in Indian,
 * film, and pop collaborations) where one artist is the composer / music director and
 * another is the featured or lead singer.
 */
object MusicDirectorClassifier {

    private val KNOWN_DIRECTORS = setOf(
        // Indian / Film Music Directors & Composers
        "pritam", "a.r. rahman", "ar rahman", "anirudh ravichander", "sachin-jigar",
        "sachin jigar", "devi sri prasad", "dsp", "vishal-shekhar", "vishal shekhar",
        "shankar-ehsaan-loy", "amit trivedi", "ajay-atul", "hiphop tamizha",
        "harris jayaraj", "yuvan shankar raja", "santhosh narayanan", "thaman s",
        "s.s. thaman", "m.m. keeravaani", "keeravani", "ilaiyaraaja", "ilayaraja",
        "vidyasagar", "d. imman", "g.v. prakash kumar", "mithoon", "tanishk bagchi",
        "rochak kohli", "amaal mallik", "b praak", "jaani", "manan bhardwaj",
        "ram sampath", "sneha khanwalkar", "shantanu moitra", "jatin-lalit",
        "nadeem-shravan", "laxmikant-pyarelal", "r.d. burman", "s.d. burman",
        "anu malik", "himesh reshammiya", "yo yo honey singh", "badshah",

        // Global / Pop / Hip-Hop Composers & Producers
        "metro boomin", "finneas", "dr. dre", "max martin", "jack antonoff",
        "timbaland", "pharrell williams", "pharrell", "kanye west", "mike dean",
        "ludwig goransson", "hans zimmer", "john williams", "murda beatz",
        "mustard", "benny blanco", "skrillex", "diplo", "calvin harris",
        "david guetta", "alesso", "marshmello", "chainsmokers", "alan walker",
        "martin garrix", "kygo", "avicii", "dj snake", "illenium", "zedd"
    )

    private val SEPARATORS = Regex("""\s*,\s*|\s+&\s+|\s+x\s+|\s+feat\.?\s+|\s+ft\.?\s+|\s+featuring\s+|\s+with\s+|/|\|""", RegexOption.IGNORE_CASE)

    fun classify(
        artistString: String,
        creditedArtists: List<String> = emptyList(),
    ): ArtistRoles {
        val splitNames = artistString.split(SEPARATORS)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val allNames = (creditedArtists.map { it.trim() } + splitNames)
            .filter { it.isNotBlank() }
            .distinct()

        if (allNames.isEmpty()) {
            return ArtistRoles(singer = "Unknown Artist", musicDirector = null)
        }

        if (allNames.size == 1) {
            val solo = allNames.first()
            val isDirector = solo.lowercase() in KNOWN_DIRECTORS
            return ArtistRoles(
                singer = solo,
                musicDirector = if (isDirector) solo else solo,
            )
        }

        // Multiple artists: search for a recognized music director
        val matchedDirector = allNames.firstOrNull { it.lowercase() in KNOWN_DIRECTORS }

        if (matchedDirector != null) {
            val singer = allNames.firstOrNull { it != matchedDirector } ?: allNames.first()
            return ArtistRoles(singer = singer, musicDirector = matchedDirector)
        }

        // If no specifically recognized director, treat first as singer and second as collaborator/producer
        val singer = allNames.first()
        val second = allNames.getOrNull(1)
        return ArtistRoles(singer = singer, musicDirector = second ?: singer)
    }
}
