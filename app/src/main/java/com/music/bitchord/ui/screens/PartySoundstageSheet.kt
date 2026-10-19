package com.music.bitchord.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.listentogether.ListenTogether

data class SoundstagePreset(
    val name: String,
    val description: String,
    val iconTint: Color,
)

data class VisualizerPalette(
    val id: String,
    val name: String,
    val primaryColor: Color,
    val accentColor: Color,
)

val SOUNDSTAGE_PRESETS = listOf(
    SoundstagePreset("Aura Spatial 3D", "Immersive virtual surround soundstage", Color(0xFF7C4DFF)),
    SoundstagePreset("Bass Booster Mega", "Deep low-end rumble for dance & hip-hop", Color(0xFFFF5252)),
    SoundstagePreset("Vocal Clarity HD", "Crisp midrange tuned for vocals and lyrics", Color(0xFF40C4FF)),
    SoundstagePreset("Club Stadium", "Wide acoustic hall reverb & energy", Color(0xFFFFAB40)),
    SoundstagePreset("Lo-Fi Midnight", "Warm vintage highs and mellow vibes", Color(0xFF69F0AE)),
)

val VISUALIZER_PALETTES = listOf(
    VisualizerPalette("electric_violet", "Electric Violet", Color(0xFF9D4EDD), Color(0xFFE0AAFF)),
    VisualizerPalette("cyber_cyan", "Cyber Neon", Color(0xFF00F0FF), Color(0xFF0077B6)),
    VisualizerPalette("sunset_amber", "Sunset Amber", Color(0xFFFF6B35), Color(0xFFF7C59F)),
    VisualizerPalette("emerald_aurora", "Emerald Glow", Color(0xFF00F5D4), Color(0xFF00BBF9)),
    VisualizerPalette("crimson_pulse", "Crimson Pulse", Color(0xFFFF0054), Color(0xFFFF5400)),
)

@Composable
fun PartySoundstageSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by ListenTogether.state.collectAsStateWithLifecycle()
    val syncedTheme by ListenTogether.syncedTheme.collectAsStateWithLifecycle()

    val currentPreset = syncedTheme?.presetName?.ifBlank { "Aura Spatial 3D" } ?: "Aura Spatial 3D"
    val currentPalette = syncedTheme?.palette?.ifBlank { "electric_violet" } ?: "electric_violet"
    val isHost = state.you?.isHost == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Room Soundstage & Theme",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (isHost) "You are host: synced across all listener devices" else "Synced live from room host",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Soundstage Presets
        Text(
            text = "EQUALIZER SOUNDSTAGE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
        )

        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SOUNDSTAGE_PRESETS.forEach { preset ->
                val selected = currentPreset == preset.name
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(enabled = isHost) {
                            ListenTogether.syncTheme(preset.name, currentPalette)
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(preset.iconTint.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SurroundSound,
                                contentDescription = null,
                                tint = preset.iconTint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = preset.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Visualizer Color Palette
        Text(
            text = "VISUALIZER PALETTE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
        )

        Spacer(Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(VISUALIZER_PALETTES) { palette ->
                val selected = currentPalette == palette.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .border(
                            1.5.dp,
                            if (selected) palette.primaryColor else Color.Transparent,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(enabled = isHost) {
                            ListenTogether.syncTheme(currentPreset, palette.id)
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentColor)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = palette.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) palette.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
