package com.music.bitchord.ui.ai

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.ai.AuraStemEngine
import com.music.bitchord.data.model.Song

/**
 * On-device AI Stem Separator and Live Karaoke Studio sheet.
 * Dual faders for vocal and instrumental balance with live pitch shifting.
 */
@Composable
fun AuraKaraokeSheet(
    song: Song?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stemState by AuraStemEngine.stemState.collectAsState()

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = Color(0xFF101322),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8A2387).copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE94057)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Stem Separator & Karaoke",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = song?.let { "${it.title} • ${it.artist}" } ?: "Live Audio Stems",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                        )
                    }
                }

                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Quick Mode Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ModePresetButton(
                    label = "🎤 Karaoke",
                    isSelected = stemState.karaokeModeEnabled,
                    onClick = { AuraStemEngine.toggleKaraokeMode() },
                    modifier = Modifier.weight(1f),
                )
                ModePresetButton(
                    label = "🗣️ Acapella",
                    isSelected = stemState.instrumentalVolume < 0.2f,
                    onClick = { AuraStemEngine.toggleAcapellaMode() },
                    modifier = Modifier.weight(1f),
                )
                ModePresetButton(
                    label = "🔄 Reset",
                    isSelected = !stemState.karaokeModeEnabled && stemState.instrumentalVolume >= 0.8f,
                    onClick = { AuraStemEngine.resetStems() },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(22.dp))

            // Vocals Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Vocal Lead Level",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    Text(
                        text = "${(stemState.vocalVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = Color(0xFFE94057),
                    )
                }
                Slider(
                    value = stemState.vocalVolume,
                    onValueChange = { AuraStemEngine.setVocalVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFE94057),
                        activeTrackColor = Color(0xFFE94057),
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                    ),
                )
            }

            Spacer(Modifier.height(14.dp))

            // Instrumental Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Backing Music & Instrumental",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    Text(
                        text = "${(stemState.instrumentalVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = Color(0xFF00F5D4),
                    )
                }
                Slider(
                    value = stemState.instrumentalVolume,
                    onValueChange = { AuraStemEngine.setInstrumentalVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00F5D4),
                        activeTrackColor = Color(0xFF00F5D4),
                        inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Pitch Transpose (Semitones)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Pitch Key Shift",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                    Text(
                        text = "Adjust key to match your vocal range",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { AuraStemEngine.setPitchShift(stemState.pitchShiftSemitones - 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2438)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("-1", color = Color.White)
                    }

                    Text(
                        text = if (stemState.pitchShiftSemitones > 0) "+${stemState.pitchShiftSemitones}" else "${stemState.pitchShiftSemitones}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = Color(0xFFFFA07A),
                    )

                    Button(
                        onClick = { AuraStemEngine.setPitchShift(stemState.pitchShiftSemitones + 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2438)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("+1", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModePresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF8A2387) else Color(0xFF1B2033),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFE94057) else Color.White.copy(alpha = 0.08f),
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                ),
                color = Color.White,
            )
        }
    }
}
