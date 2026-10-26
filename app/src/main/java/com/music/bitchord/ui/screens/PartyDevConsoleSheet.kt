package com.music.bitchord.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.listentogether.ListenTogether
import com.music.bitchord.data.listentogether.ListenTogether.DevLogLevel
import com.music.bitchord.data.listentogether.ListenTogether.DevLogTag
import com.music.bitchord.data.listentogether.ListenTogether.PartyDevLog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val FounderGold = Color(0xFFFFB300)
private val FounderGoldDark = Color(0xFFD97706)
private val DevTerminalBg = Color(0xFF0D1117)
private val EmeraldStatus = Color(0xFF10B981)
private val AmberStatus = Color(0xFFF59E0B)
private val RoseStatus = Color(0xFFEF4444)
private val CyanTag = Color(0xFF38BDF8)
private val VioletTag = Color(0xFFA855F7)

@Composable
fun PartyDevConsoleSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val devLogs by ListenTogether.devLogs.collectAsStateWithLifecycle()
    val state by ListenTogether.state.collectAsStateWithLifecycle()
    val framesSent by ListenTogether.framesSent.collectAsStateWithLifecycle()
    val framesReceived by ListenTogether.framesReceived.collectAsStateWithLifecycle()
    val audioOffsetMs by ListenTogether.audioOffsetMs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // High-tech Developer Suite Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            FounderGold.copy(alpha = 0.18f),
                            Color(0xFF6366F1).copy(alpha = 0.08f),
                            Color.Transparent,
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(FounderGold.copy(alpha = 0.20f), CircleShape)
                        .border(1.dp, FounderGold.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = FounderGold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DEVELOPER SUITE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FounderGold.copy(alpha = 0.25f),
                            border = BorderStroke(0.8.dp, FounderGold.copy(alpha = 0.7f))
                        ) {
                            Text(
                                text = "FOUNDER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = FounderGold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Real-time party telemetry & god-mode controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Developer Console",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Navigation Bar
        val tabs = listOf("LIVE LOGS", "TELEMETRY HUD", "GOD CONTROLS")
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = FounderGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = FounderGold,
                    height = 3.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                val countText = if (index == 0 && devLogs.isNotEmpty()) " (${devLogs.size})" else ""
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title + countText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTab == index) FounderGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> DevLogsTab(
                    logs = devLogs,
                    onClear = { ListenTogether.clearDevLogs() },
                    onCopyAll = {
                        val text = devLogs.joinToString("\n") { log ->
                            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(log.timestampMs))
                            "[$time] [${log.tag.name}] [${log.level.name}] ${log.message}${if (!log.source.isNullOrBlank()) " (${log.source})" else ""}"
                        }
                        clipboard.setText(AnnotatedString(text))
                        Toast.makeText(context, "Copied ${devLogs.size} logs to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )

                1 -> DevTelemetryTab(
                    state = state,
                    roundTripMs = ListenTogether.roundTripMs,
                    clockOffsetMs = ListenTogether.clockOffsetMs,
                    isClockSynced = ListenTogether.isClockSynced,
                    framesSent = framesSent,
                    framesReceived = framesReceived,
                    audioOffsetMs = audioOffsetMs,
                    onAudioOffsetChange = { ListenTogether.setAudioOffsetMs(it) },
                    onResync = { ListenTogether.forceSyncRoom() }
                )

                2 -> DevGodControlsTab(
                    state = state,
                    onBroadcastGodAnnouncement = { text ->
                        ListenTogether.broadcastGodAnnouncement(text)
                        Toast.makeText(context, "God Announcement Broadcasted!", Toast.LENGTH_SHORT).show()
                    },
                    onForceResync = {
                        ListenTogether.forceSyncRoom()
                        Toast.makeText(context, "Room Clock Probe Fired", Toast.LENGTH_SHORT).show()
                    },
                    onMasterSkip = {
                        ListenTogether.masterSkip()
                        Toast.makeText(context, "Master Instant Skip Triggered", Toast.LENGTH_SHORT).show()
                    },
                    onAutopilotRefill = {
                        ListenTogether.triggerAutopilotRefill()
                        Toast.makeText(context, "Overdrive Radio Injected", Toast.LENGTH_SHORT).show()
                    },
                    onSetAux = { memberId ->
                        ListenTogether.setAuxMember(memberId)
                        Toast.makeText(context, "Aux DJ Updated", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun DevLogsTab(
    logs: List<PartyDevLog>,
    onClear: () -> Unit,
    onCopyAll: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(DevLogTag.ALL) }
    var autoScroll by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val filteredLogs = remember(logs, searchQuery, selectedTag) {
        logs.filter { log ->
            val matchesTag = selectedTag == DevLogTag.ALL || log.tag == selectedTag
            val matchesQuery = searchQuery.isBlank() ||
                log.message.contains(searchQuery, ignoreCase = true) ||
                log.source?.contains(searchQuery, ignoreCase = true) == true
            matchesTag && matchesQuery
        }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter logs...", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FounderGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                )
            )

            // Auto-scroll toggle
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (autoScroll) FounderGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (autoScroll) FounderGold else Color.Transparent),
                modifier = Modifier
                    .clickable { autoScroll = !autoScroll }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (autoScroll) Icons.Rounded.PlayArrow else Icons.Rounded.PauseCircle,
                        contentDescription = null,
                        tint = if (autoScroll) FounderGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (autoScroll) "Live" else "Frozen",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (autoScroll) FounderGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Copy button
            IconButton(
                onClick = onCopyAll,
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy All Logs",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Clear button
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(36.dp)
                    .background(RoseStatus.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "Clear Logs",
                    tint = RoseStatus,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Tag Filters Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DevLogTag.entries.forEach { tag ->
                val isSelected = selectedTag == tag
                val chipColor = when (tag) {
                    DevLogTag.ALL -> FounderGold
                    DevLogTag.WS -> CyanTag
                    DevLogTag.SYNC -> VioletTag
                    DevLogTag.PLAY -> EmeraldStatus
                    DevLogTag.CHAT -> AmberStatus
                    DevLogTag.SYS -> Color(0xFFEC4899)
                    DevLogTag.ERR -> RoseStatus
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) chipColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, if (isSelected) chipColor else Color.Transparent),
                    modifier = Modifier.clickable { selectedTag = tag }
                ) {
                    Text(
                        text = tag.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Monospace Terminal Feed
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = DevTerminalBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF6E7681),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No log frames recorded yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8B949E),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            } else {
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val tagColor = when (log.tag) {
                            DevLogTag.ALL -> FounderGold
                            DevLogTag.WS -> CyanTag
                            DevLogTag.SYNC -> VioletTag
                            DevLogTag.PLAY -> EmeraldStatus
                            DevLogTag.CHAT -> AmberStatus
                            DevLogTag.SYS -> Color(0xFFEC4899)
                            DevLogTag.ERR -> RoseStatus
                        }
                        val levelTextColor = when (log.level) {
                            DevLogLevel.INFO -> Color(0xFFC9D1D9)
                            DevLogLevel.WARN -> AmberStatus
                            DevLogLevel.ERROR -> RoseStatus
                            DevLogLevel.SUCCESS -> EmeraldStatus
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboard.setText(AnnotatedString(log.message))
                                    Toast.makeText(context, "Copied log entry", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Timestamp
                            Text(
                                text = timeFormat.format(Date(log.timestampMs)),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFF6E7681),
                                modifier = Modifier.width(76.dp)
                            )

                            // Tag
                            Text(
                                text = "[${log.tag.label}]",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = tagColor,
                                modifier = Modifier.width(48.dp)
                            )

                            // Message
                            Text(
                                text = log.message + if (!log.source.isNullOrBlank()) " (${log.source})" else "",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = levelTextColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DevTelemetryTab(
    state: ListenTogether.State,
    roundTripMs: Long,
    clockOffsetMs: Long?,
    isClockSynced: Boolean,
    framesSent: Long,
    framesReceived: Long,
    audioOffsetMs: Long,
    onAudioOffsetChange: (Long) -> Unit,
    onResync: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Latency & Connection HUD Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val pingColor = when {
                            roundTripMs <= 0 -> Color.Gray
                            roundTripMs < 60 -> EmeraldStatus
                            roundTripMs < 160 -> AmberStatus
                            else -> RoseStatus
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(pingColor, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "NETWORK LATENCY & CLOCK",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = onResync,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FounderGold),
                        border = BorderStroke(1.dp, FounderGold.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Probe", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (roundTripMs > 0) "${roundTripMs} ms" else "---",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (roundTripMs < 60) EmeraldStatus else if (roundTripMs < 160) AmberStatus else RoseStatus
                        )
                        Text(
                            text = "Round-Trip Time (RTT)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (clockOffsetMs != null) "${if (clockOffsetMs >= 0) "+" else ""}${clockOffsetMs} ms" else "Unsynced",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isClockSynced) CyanTag else RoseStatus
                        )
                        Text(
                            text = "NTP Drift Offset",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Network frames summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Frames TX: $framesSent",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Frames RX: $framesReceived",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Bluetooth Audio Calibration Slider
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = CyanTag,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "AUDIO DRIFT CALIBRATION",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = { onAudioOffsetChange(0L) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Reset (0ms)", style = MaterialTheme.typography.labelSmall, color = FounderGold)
                    }
                }

                Text(
                    text = "Compensate for Bluetooth headphone latency (-300ms to +300ms)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (audioOffsetMs >= 0) "+" else ""}${audioOffsetMs} ms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = FounderGold,
                        modifier = Modifier.width(72.dp)
                    )

                    Slider(
                        value = audioOffsetMs.toFloat(),
                        onValueChange = { onAudioOffsetChange(it.toLong()) },
                        valueRange = -300f..300f,
                        steps = 59,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = FounderGold,
                            activeTrackColor = FounderGold,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }
        }

        // Room Telemetry & Members Roster
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PARTY MEMBERS ROSTER (${state.members.size}/${state.maxMembers})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "ROOM: ${state.code ?: "NONE"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = FounderGold
                    )
                }

                Spacer(Modifier.height(12.dp))

                state.members.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        if (!member.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = member.avatarUrl,
                                contentDescription = member.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = member.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(Modifier.width(6.dp))

                                if (member.isFounder || member.displayName.equals("likki", ignoreCase = true)) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = FounderGold.copy(alpha = 0.2f),
                                        border = BorderStroke(0.6.dp, FounderGold)
                                    ) {
                                        Text(
                                            text = "FOUNDER",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = FounderGold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (member.isHost) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CyanTag.copy(alpha = 0.2f),
                                        border = BorderStroke(0.6.dp, CyanTag)
                                    ) {
                                        Text(
                                            text = "HOST",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = CyanTag,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                } else if (state.auxMemberId != null && state.auxMemberId == member.memberId) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = VioletTag.copy(alpha = 0.2f),
                                        border = BorderStroke(0.6.dp, VioletTag)
                                    ) {
                                        Text(
                                            text = "AUX DJ",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = VioletTag,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "ID: ${member.memberId.take(12)}...",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Connection Dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (member.connected) EmeraldStatus else Color.Gray,
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DevGodControlsTab(
    state: ListenTogether.State,
    onBroadcastGodAnnouncement: (String) -> Unit,
    onForceResync: () -> Unit,
    onMasterSkip: () -> Unit,
    onAutopilotRefill: () -> Unit,
    onSetAux: (String?) -> Unit,
) {
    var announcementText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // God Announcement Broadcaster
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.2.dp, FounderGold.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(FounderGold.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = null,
                            tint = FounderGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "GOD ANNOUNCEMENT BROADCASTER",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = FounderGold
                        )
                        Text(
                            text = "Broadcasts a golden priority alert banner to all party members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = announcementText,
                    onValueChange = { announcementText = it },
                    placeholder = { Text("Enter room-wide announcement message...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FounderGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (announcementText.isNotBlank()) {
                            onBroadcastGodAnnouncement(announcementText.trim())
                            announcementText = ""
                        }
                    },
                    enabled = announcementText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FounderGold,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Broadcast to Room", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Playback & Queue God Actions
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PLAYBACK & QUEUE GOD ACTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                // Master Instant Skip
                Button(
                    onClick = onMasterSkip,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldStatus,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Master Instant Skip (0 Votes Needed)", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                // Autopilot Overdrive
                Button(
                    onClick = onAutopilotRefill,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Radio, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Autopilot Overdrive: Inject 5 Radio Songs", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))

                // Master NTP Clock Force Resync
                OutlinedButton(
                    onClick = onForceResync,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanTag),
                    border = BorderStroke(1.dp, CyanTag.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Force Master Clock NTP Resync", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Aux DJ Delegation
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DELEGATE AUX DJ PRIVILEGES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.auxMemberId != null) {
                        TextButton(
                            onClick = { onSetAux(null) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Revoke Aux", style = MaterialTheme.typography.labelSmall, color = RoseStatus)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                val nonHostMembers = state.members.filter { !it.isHost }
                if (nonHostMembers.isEmpty()) {
                    Text(
                        text = "No other listeners in party to delegate to",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    nonHostMembers.forEach { member ->
                        val isAux = state.auxMemberId == member.memberId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = member.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            if (isAux) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = VioletTag.copy(alpha = 0.2f),
                                    border = BorderStroke(0.8.dp, VioletTag)
                                ) {
                                    Text(
                                        text = "CURRENT AUX DJ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VioletTag,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onSetAux(member.memberId) },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VioletTag),
                                    border = BorderStroke(1.dp, VioletTag.copy(alpha = 0.5f))
                                ) {
                                    Text("Make Aux DJ", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
