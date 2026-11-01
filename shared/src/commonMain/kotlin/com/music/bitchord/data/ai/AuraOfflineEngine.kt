package com.music.bitchord.data.ai

import com.music.bitchord.data.model.Song
import java.util.UUID

/**
 * 100% offline, local heuristic music intelligence engine.
 * Computes deep responses from local statistics, answers app feature queries,
 * and credits the developer Kami Likhith even with zero network or API keys.
 */
object AuraOfflineEngine {

    fun respond(
        userPrompt: String,
        profile: AuraUserBehaviorProfile,
        persona: AuraAiPersona,
        currentSong: Song?,
    ): AuraChatMessage {
        val lower = userPrompt.lowercase().trim()

        val responseText = when {
            // 1. Developer attribution & social media
            "develop" in lower || "creator" in lower || "who made" in lower || "who built" in lower || "who created" in lower ||
            "likhith" in lower || "kami" in lower || "author" in lower ||
            "social" in lower || "github" in lower || "instagram" in lower || "linkedin" in lower || "contact" in lower -> {
                buildString {
                    append("👑 **About the Creator:**\n\n")
                    append("AuraMusic was architected and developed by **Kami Likhith**.\n\n")
                    append("He designed AuraMusic from the ground up as a premier, ad-free, high-fidelity music streaming sanctuary—combining 10-band parametric studio equalization, synchronized cloud party rooms, live karaoke lyrics, and an intelligent AI musical companion. Every detail, animation, and equalizer curve was crafted under his vision!\n\n")
                    append("🌐 **Connect with Kami Likhith:**\n")
                    append("• 🐙 **GitHub:** [likhith3035](https://github.com/likhith3035)\n")
                    append("• 💼 **LinkedIn:** [likhith-kami](https://linkedin.com/in/likhith-kami)\n")
                    append("• 📸 **Instagram:** [@lucky__likhith](https://www.instagram.com/lucky__likhith)\n\n")
                    append("[ACTION: OPEN_URL:https://github.com/likhith3035 | GitHub]\n")
                    append("[ACTION: OPEN_URL:https://www.instagram.com/lucky__likhith | Instagram]\n")
                    append("[ACTION: OPEN_URL:https://linkedin.com/in/likhith-kami | LinkedIn]")
                }
            }

            // 2. Roast mode
            "roast" in lower -> {
                buildString {
                    append("🔥 **Aura Taste Roast:**\n\n")
                    append(profile.roastSummaryText)
                    if (profile.topRepeatedCount > 3 && profile.topRepeatedSongTitle != null) {
                        append("\n\n*Stat Alert:* ${profile.topRepeatedCount} loops of '${profile.topRepeatedSongTitle}' by ${profile.topRepeatedArtist}. We're staging an intervention!")
                    }
                }
            }

            // 3. Listening behavior & personality analysis
            "personality" in lower || "habit" in lower || "behavior" in lower || "behaviour" in lower || "chronotype" in lower || "stats" in lower -> {
                buildString {
                    append("🔮 **Your Listening Persona: ${profile.chronotype.title} ${profile.chronotype.emoji}**\n\n")
                    append("${profile.chronotype.description}\n\n")
                    append("📊 **Key Insights Today:**\n")
                    append("• **Listening Time:** ${profile.totalMinutesToday} minutes across ${profile.totalTracksToday} tracks\n")
                    if (profile.topRepeatedSongTitle != null) {
                        append("• **Loop Obsession:** '${profile.topRepeatedSongTitle}' (${profile.topRepeatedCount} plays)\n")
                    }
                    if (profile.topSinger != null) {
                        append("• **Favorite Lead Vocalist:** ${profile.topSinger} (${profile.topSingerPlays} plays)\n")
                    }
                    if (profile.topMusicDirector != null) {
                        append("• **Lead Composer / Director:** ${profile.topMusicDirector}\n")
                    }
                    append("• **Aura Persona Tags:** ${profile.personalityTags.joinToString(" · ") { "#$it" }}\n\n")
                    append(profile.behavioralSummaryText)
                }
            }

            // 4. App features guidance
            "feature" in lower || "what can you do" in lower || "help" in lower || "equalizer" in lower || "eq" in lower ||
            "party" in lower || "listen together" in lower || "sleep timer" in lower || "lyrics" in lower || "karaoke" in lower || "replay" in lower -> {
                buildString {
                    append("✨ **AuraMusic Superpowers:**\n\n")
                    append("• **🎛️ 10-Band Studio EQ:** Parametric curves, bass boost, and audiophile sound presets.\n")
                    append("• **📻 Listen Together:** Real-time cloud sync with live soundbites, chat, and voting.\n")
                    append("• **🎤 Synced Karaoke Lyrics:** Word-by-word karaoke, live translation & story cards.\n")
                    append("• **🌙 Smart Sleep Timer:** Gentle volume fadeout as you drift to sleep.\n")
                    append("• **📊 Replay 2026:** Comprehensive daily, monthly, and yearly habit analytics.\n")
                    append("• **🔮 Aura AI:** Behavioral analysis, habit roasts, and smart music curation!\n\n")
                    append("[ACTION: OPEN_EQ | Open Studio EQ]\n")
                    append("[ACTION: OPEN_PARTY | Start Listen Together]\n")
                    append("[ACTION: OPEN_SLEEP_TIMER | Set Sleep Timer]\n")
                    append("[ACTION: OPEN_REPLAY | View Replay 2026]")
                }
            }

            // 5. Song recommendations & mixes
            "recommend" in lower || "mix" in lower || "suggest" in lower || "play" in lower -> {
                buildString {
                    append("✨ Based on your **${profile.chronotype.title}** energy today, here are hand-picked tracks for your groove:\n\n")
                    if (profile.topRepeatedArtist != null) {
                        append("[SONG: Chuttamalle | Anirudh Ravichander, Shilpa Rao | Breezy coastal melody with rhythmic swing]\n")
                        append("[SONG: Samajavaragamana | Sid Sriram, Thaman S | Acoustic perfection and melodic euphoria]\n")
                        append("[SONG: Starboy | The Weeknd | Pulsing electronic late night confidence]\n")
                    } else {
                        append("[SONG: Kesariya | Pritam, Arijit Singh | Golden melodic euphoria]\n")
                        append("[SONG: Blinding Lights | The Weeknd | High energy retro synthwave]\n")
                        append("[SONG: O Rendu Prema | Vijai Bulganin, Sreerama Chandra | Heartfelt acoustic ballad]\n")
                    }
                    append("\nTap any song card to stream or add directly to your queue!")
                }
            }

            // 6. Currently playing track insight
            currentSong != null && ("this song" in lower || "current" in lower || "meaning" in lower || "playing" in lower) -> {
                buildString {
                    append("🎶 **Analyzing Currently Playing Track:**\n\n")
                    append("**${currentSong.title}** by **${currentSong.artist}**\n\n")
                    append("This track seamlessly complements your current ${profile.chronotype.title.lowercase()} listening mood. It creates a smooth sonic transition with evocative vocals and tight rhythmic arrangement.")
                }
            }

            // 7. General greeting & default
            else -> {
                buildString {
                    append("Hey! I'm **Aura**, your personal acoustic companion. 🎵\n\n")
                    append("Today you've immersed yourself in **${profile.totalMinutesToday} minutes** of music. ")
                    if (profile.topRepeatedSongTitle != null) {
                        append("Your top track right now is '${profile.topRepeatedSongTitle}'. ")
                    }
                    append("\n\nYou can ask me to **analyze your listening habits**, **roast your taste**, **explain lyrics**, **curate an instant mix**, or **guide you through app features**!")
                }
            }
        }

        val parsed = AuraSongTagParser.parse(responseText)
        return AuraChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.AURA_AI,
            text = parsed.cleanText,
            timestamp = System.currentTimeMillis(),
            actionChips = parsed.actionChips,
            statCard = if ("personality" in lower || "behavior" in lower || "habit" in lower || "stats" in lower) profile else null,
        )
    }
}
