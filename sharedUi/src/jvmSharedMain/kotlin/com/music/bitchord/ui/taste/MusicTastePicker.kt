package com.music.bitchord.ui.taste

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.music.bitchord.data.taste.ArtistRole
import com.music.bitchord.data.taste.MusicLanguageOption
import com.music.bitchord.data.taste.MusicTasteCatalog
import com.music.bitchord.data.taste.MusicTasteRepository
import com.music.bitchord.data.taste.MusicTasteStore
import com.music.bitchord.data.taste.TasteArtistPick
import com.music.bitchord.ui.components.SearchField
import com.music.bitchord.ui.haptics.Haptic
import com.music.bitchord.ui.haptics.rememberHaptics
import com.music.bitchord.ui.theme.NothingThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Guaranteed high-contrast dark palette for taste flows in both Standard Aura and Nothing OS
 * modes — preventing white-on-white buttons/pills when MaterialTheme primary is White, or
 * dark-text-on-dark-surface leakage when the host app is in Light Mode.
 */
private data class TasteUiPalette(
    val primaryBg: Color,
    val primaryContent: Color,
    val accentGlow: Color,
    val tileSurface: Color,
    val tileSelectedSurface: Color,
    val tileBorder: Color,
    val dockSurface: Color,
)

@Composable
private fun rememberTastePalette(nothingOsMode: Boolean): TasteUiPalette =
    remember(nothingOsMode) {
        if (nothingOsMode) {
            TasteUiPalette(
                primaryBg = NothingThemeTokens.Red,
                primaryContent = Color.White,
                accentGlow = NothingThemeTokens.Red,
                tileSurface = NothingThemeTokens.Surface,
                tileSelectedSurface = NothingThemeTokens.SurfaceElevated,
                tileBorder = NothingThemeTokens.Border,
                dockSurface = NothingThemeTokens.Black,
            )
        } else {
            TasteUiPalette(
                primaryBg = Color.White,
                primaryContent = Color(0xFF0A0B0F),
                accentGlow = Color.White,
                tileSurface = Color(0xFF151720).copy(alpha = 0.88f),
                tileSelectedSurface = Color(0xFF1E212E).copy(alpha = 0.96f),
                tileBorder = Color.White.copy(alpha = 0.12f),
                dockSurface = Color(0xFF0D0E14).copy(alpha = 0.96f),
            )
        }
    }

@Composable
private fun WithTasteDarkTheme(
    nothingOsMode: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = rememberTastePalette(nothingOsMode)
    val baseTypography = MaterialTheme.typography
    val scheme = darkColorScheme(
        primary = palette.primaryBg,
        onPrimary = palette.primaryContent,
        background = Color(0xFF090A0F),
        onBackground = Color.White,
        surface = palette.tileSurface,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1B1D28),
        onSurfaceVariant = Color(0xFFA0A3B1),
        outline = Color.White.copy(alpha = 0.14f),
    )
    MaterialTheme(colorScheme = scheme, typography = baseTypography) {
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}

/**
 * Aura-native Music Languages selector card grid.
 */
@Composable
fun MusicLanguagesStepContent(
    selectedLanguageIds: List<String>,
    onToggleLanguage: (String) -> Unit,
    onContinue: () -> Unit,
    nothingOsMode: Boolean,
    modifier: Modifier = Modifier,
    headline: String = "What sounds move you?",
    subtitle: String = "Choose the music languages you enjoy. App menu language stays unchanged.",
    continueLabel: String = "Next: Singers & Directors",
    skipLabel: String? = null,
    onSkip: (() -> Unit)? = null,
) {
    WithTasteDarkTheme(nothingOsMode = nothingOsMode) {
        val haptics = rememberHaptics()
        val palette = rememberTastePalette(nothingOsMode)
        val portraits = remember { mutableStateMapOf<String, String?>() }

        LaunchedEffect(Unit) {
            val loaded = runCatching { MusicTasteRepository.loadLanguagePortraits() }.getOrDefault(emptyMap())
            portraits.putAll(loaded)
        }

        Column(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    SelectionCountPill(
                        text = if (selectedLanguageIds.isEmpty()) {
                            "Pick 1+"
                        } else {
                            "${selectedLanguageIds.size} selected"
                        },
                        highlighted = selectedLanguageIds.isNotEmpty(),
                        nothingOsMode = nothingOsMode,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.68f),
                    lineHeight = 20.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(MusicTasteCatalog.LANGUAGES, key = { it.id }) { language ->
                    MusicLanguageGlassTile(
                        language = language,
                        portraitUrl = portraits[language.id],
                        selected = language.id in selectedLanguageIds,
                        nothingOsMode = nothingOsMode,
                        palette = palette,
                        onClick = {
                            haptics.play(
                                if (language.id in selectedLanguageIds) Haptic.ToggleOff else Haptic.ToggleOn,
                            )
                            onToggleLanguage(language.id)
                        },
                    )
                }
            }

            Surface(
                color = palette.dockSurface,
                border = BorderStroke(1.dp, palette.tileBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (skipLabel != null && onSkip != null) {
                        TextButton(onClick = onSkip) {
                            Text(
                                text = skipLabel,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Button(
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onContinue()
                        },
                        enabled = selectedLanguageIds.isNotEmpty(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primaryBg,
                            contentColor = palette.primaryContent,
                            disabledContainerColor = Color.White.copy(alpha = 0.14f),
                            disabledContentColor = Color.White.copy(alpha = 0.45f),
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(
                            text = continueLabel,
                            color = if (selectedLanguageIds.isNotEmpty()) {
                                palette.primaryContent
                            } else {
                                Color.White.copy(alpha = 0.45f)
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicLanguageGlassTile(
    language: MusicLanguageOption,
    portraitUrl: String?,
    selected: Boolean,
    nothingOsMode: Boolean,
    palette: TasteUiPalette,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.985f else 1f,
        animationSpec = spring(stiffness = 480f),
        label = "lang_tile_scale",
    )
    val langColor = Color(language.accentHex)
    val stripeColor = if (nothingOsMode) NothingThemeTokens.Red else langColor
    val borderColor = when {
        selected && nothingOsMode -> NothingThemeTokens.Red
        selected -> Color.White.copy(alpha = 0.78f)
        else -> palette.tileBorder
    }

    var portraitLoaded by remember(portraitUrl) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) palette.tileSelectedSurface else palette.tileSurface)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        stripeColor.copy(alpha = if (selected) 0.22f else 0.10f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(stripeColor.copy(alpha = if (selected) 1f else 0.6f)),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = language.nativeScript,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) stripeColor else Color.White.copy(alpha = 0.68f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!portraitUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = portraitUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onState = { portraitLoaded = it is AsyncImagePainter.State.Success },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = if (selected) stripeColor else Color.White.copy(alpha = 0.18f),
                                shape = CircleShape,
                            ),
                    )
                }
                if (!portraitLoaded) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(stripeColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = stripeColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(palette.primaryBg)
                            .border(1.5.dp, Color.Black, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = palette.primaryContent,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Aura-native Singers & Music Directors picker featuring:
 * - Live YouTube Music artist search via shared SearchField
 * - Language & Role filter pills (All / Music Directors / Singers)
 * - Clean 3-column circular portrait grid + non-jumping "Similar to <Artist>" strip
 * - Subtle audio sample toggle + sticky overlapping mini-avatar dock
 */
@Composable
fun MusicArtistsStepContent(
    selectedLanguageIds: List<String>,
    selectedArtists: List<TasteArtistPick>,
    onToggleArtist: (TasteArtistPick) -> Unit,
    onContinue: () -> Unit,
    nothingOsMode: Boolean,
    modifier: Modifier = Modifier,
    headline: String = "Choose singers & music directors",
    subtitle: String = "Pick 3 or more creators you enjoy. Filter Music Directors vs Singers or search anyone.",
    continueLabel: String = "Continue",
    onBackStep: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    onPreviewArtist: ((TasteArtistPick) -> Unit)? = null,
    previewingArtistKey: String? = null,
    previewLoadingArtistKey: String? = null,
) {
    WithTasteDarkTheme(nothingOsMode = nothingOsMode) {
        val haptics = rememberHaptics()
        val palette = rememberTastePalette(nothingOsMode)
        val scope = rememberCoroutineScope()

        var searchQuery by remember { mutableStateOf("") }
        var activeLanguageFilter by remember(selectedLanguageIds) { mutableStateOf<String?>(null) }
        var activeRoleFilter by remember { mutableStateOf(ArtistRole.ALL) }

        var catalogArtists by remember { mutableStateOf<List<TasteArtistPick>>(emptyList()) }
        var searchResults by remember { mutableStateOf<List<TasteArtistPick>>(emptyList()) }
        var relatedArtists by remember { mutableStateOf<List<TasteArtistPick>>(emptyList()) }
        var relatedAnchorName by remember { mutableStateOf<String?>(null) }
        var isLoadingCatalog by remember { mutableStateOf(true) }
        var isSearching by remember { mutableStateOf(false) }

        val effectiveLanguages = remember(selectedLanguageIds) {
            selectedLanguageIds.ifEmpty { MusicTasteCatalog.defaultLanguageSelection() }
        }

        LaunchedEffect(effectiveLanguages, activeLanguageFilter, activeRoleFilter) {
            isLoadingCatalog = true
            val targetLangs = if (activeLanguageFilter != null) {
                listOf(activeLanguageFilter!!)
            } else {
                effectiveLanguages
            }
            val loaded = runCatching {
                MusicTasteRepository.loadArtistsForLanguages(
                    languageIds = targetLangs,
                    roleFilter = activeRoleFilter,
                )
            }.getOrElse {
                MusicTasteCatalog.seedsForLanguages(targetLangs, activeRoleFilter).map { seed ->
                    TasteArtistPick(
                        browseId = seed.knownBrowseId ?: MusicTasteCatalog.seedBrowseId(seed.languageId, seed.displayName),
                        name = seed.displayName,
                        role = seed.role,
                        languageId = seed.languageId,
                    )
                }
            }
            catalogArtists = loaded
            isLoadingCatalog = false
        }

        LaunchedEffect(searchQuery) {
            val trimmed = searchQuery.trim()
            if (trimmed.isEmpty()) {
                searchResults = emptyList()
                isSearching = false
                return@LaunchedEffect
            }
            isSearching = true
            delay(240)
            val langHint = activeLanguageFilter ?: effectiveLanguages.firstOrNull() ?: "english"
            searchResults = runCatching {
                MusicTasteRepository.searchArtists(trimmed, preferredLanguageId = langHint)
            }.getOrDefault(emptyList())
            isSearching = false
        }

        val displayedArtists = remember(searchQuery, searchResults, catalogArtists, activeRoleFilter) {
            if (searchQuery.isNotBlank()) {
                when (activeRoleFilter) {
                    ArtistRole.ALL -> searchResults
                    else -> searchResults.filter { it.role == activeRoleFilter }.ifEmpty { searchResults }
                }
            } else {
                catalogArtists
            }
        }

        fun handleArtistTap(artist: TasteArtistPick) {
            val wasSelected = selectedArtists.any { MusicTasteStore.sameArtist(it, artist) }
            haptics.play(if (wasSelected) Haptic.ToggleOff else Haptic.ToggleOn)
            onToggleArtist(artist)
            if (!wasSelected) {
                relatedAnchorName = artist.name
                val excludedIds = (selectedArtists.map { it.browseId } + artist.browseId).toSet()
                val excludedNames = (selectedArtists.map { it.name } + artist.name).toSet()
                scope.launch {
                    val related = runCatching {
                        MusicTasteRepository.loadRelatedArtists(
                            artist = artist,
                            excludeBrowseIds = excludedIds,
                            excludeNames = excludedNames,
                            limit = 8,
                        )
                    }.getOrDefault(emptyList())
                    if (relatedAnchorName == artist.name) {
                        relatedArtists = related
                    }
                }
            }
        }

        Column(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    SelectionCountPill(
                        text = if (selectedArtists.size >= 3) {
                            "${selectedArtists.size} picked"
                        } else {
                            "${selectedArtists.size} / 3"
                        },
                        highlighted = selectedArtists.size >= 3,
                        nothingOsMode = nothingOsMode,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f),
                )
                Spacer(Modifier.height(10.dp))

                SearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSubmit = {},
                    placeholder = "Search singers or music directors",
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "lang_for_you") {
                    TasteFilterChip(
                        label = "For You",
                        selected = activeLanguageFilter == null,
                        nothingOsMode = nothingOsMode,
                        palette = palette,
                        onClick = { activeLanguageFilter = null },
                    )
                }
                items(effectiveLanguages, key = { "lang_$it" }) { langId ->
                    val lang = MusicTasteCatalog.languageById(langId) ?: return@items
                    TasteFilterChip(
                        label = lang.displayName,
                        selected = activeLanguageFilter == langId,
                        nothingOsMode = nothingOsMode,
                        palette = palette,
                        onClick = { activeLanguageFilter = if (activeLanguageFilter == langId) null else langId },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ArtistRole.entries, key = { it.name }) { role ->
                    TasteFilterChip(
                        label = role.label,
                        selected = activeRoleFilter == role,
                        nothingOsMode = nothingOsMode,
                        palette = palette,
                        emphasizeAccent = true,
                        onClick = { activeRoleFilter = role },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if ((isLoadingCatalog && displayedArtists.isEmpty()) || (isSearching && searchResults.isEmpty())) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(
                            color = palette.primaryBg,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (isSearching) "Searching creators..." else "Loading singers & music directors...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                } else if (displayedArtists.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "No creators matched \"$searchQuery\"",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Try a different spelling or switch All / Music Directors / Singers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.62f),
                            textAlign = TextAlign.Center,
                        )
                        if (searchQuery.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { searchQuery = "" }) {
                                Text("Clear search", color = palette.primaryBg)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 104.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(displayedArtists, key = { "main:${it.browseId}:${it.name}" }) { artist ->
                            val picked = selectedArtists.any { MusicTasteStore.sameArtist(it, artist) }
                            val key = "${artist.browseId}:${artist.name}"
                            CircularArtistTile(
                                artist = artist,
                                selected = picked,
                                palette = palette,
                                isPreviewPlaying = previewingArtistKey == key,
                                isPreviewLoading = previewLoadingArtistKey == key,
                                onClick = { handleArtistTap(artist) },
                                onPreviewClick = onPreviewArtist?.let { cb -> { cb(artist) } },
                            )
                        }

                        val visibleRelated = relatedArtists.filter { cand ->
                            selectedArtists.none { MusicTasteStore.sameArtist(it, cand) }
                        }
                        if (searchQuery.isBlank() && visibleRelated.isNotEmpty() && relatedAnchorName != null) {
                            item(span = { GridItemSpan(maxLineSpan) }, key = "related_header") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = palette.primaryBg,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = "Similar to $relatedAnchorName",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                    Text(
                                        text = "Tap any suggestion to add and keep expanding",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.6f),
                                    )
                                }
                            }

                            item(span = { GridItemSpan(maxLineSpan) }, key = "related_carousel") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                ) {
                                    items(visibleRelated, key = { "rel:${it.browseId}:${it.name}" }) { relArtist ->
                                        val key = "${relArtist.browseId}:${relArtist.name}"
                                        Box(modifier = Modifier.width(102.dp)) {
                                            CircularArtistTile(
                                                artist = relArtist,
                                                selected = false,
                                                palette = palette,
                                                isPreviewPlaying = previewingArtistKey == key,
                                                isPreviewLoading = previewLoadingArtistKey == key,
                                                onClick = { handleArtistTap(relArtist) },
                                                onPreviewClick = onPreviewArtist?.let { cb -> { cb(relArtist) } },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                color = palette.dockSurface,
                border = BorderStroke(1.dp, palette.tileBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    AnimatedVisibility(
                        visible = selectedArtists.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                                selectedArtists.take(5).forEach { artist ->
                                    MiniPickedAvatar(
                                        artist = artist,
                                        borderColor = palette.primaryBg,
                                        onClick = {
                                            haptics.play(Haptic.ToggleOff)
                                            onToggleArtist(artist)
                                        },
                                    )
                                }
                            }
                            if (selectedArtists.size > 5) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "+${selectedArtists.size - 5}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            Text(
                                text = if (selectedArtists.size >= 3) {
                                    "${selectedArtists.size} selected · Tap avatar to remove"
                                } else {
                                    "Pick ${3 - selectedArtists.size} more for sharper mixes"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.74f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onBackStep != null) {
                            OutlinedButton(
                                onClick = onBackStep,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                                modifier = Modifier.height(46.dp),
                            ) {
                                Text("Languages", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        } else if (onSkip != null) {
                            TextButton(onClick = onSkip) {
                                Text("Skip", color = Color.White.copy(alpha = 0.78f))
                            }
                        }

                        Button(
                            onClick = {
                                haptics.play(Haptic.Tap)
                                onContinue()
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryBg,
                                contentColor = palette.primaryContent,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                        ) {
                            Text(
                                text = continueLabel,
                                color = palette.primaryContent,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularArtistTile(
    artist: TasteArtistPick,
    selected: Boolean,
    palette: TasteUiPalette,
    isPreviewPlaying: Boolean,
    isPreviewLoading: Boolean,
    onClick: () -> Unit,
    onPreviewClick: (() -> Unit)?,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.96f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "artist_scale",
    )
    var imageLoaded by remember(artist.thumbnailUrl) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(92.dp)) {
            val ringColor = when {
                selected -> palette.primaryBg
                artist.isRelatedSuggestion -> palette.primaryBg.copy(alpha = 0.45f)
                else -> palette.tileBorder
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF1A1C26))
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = ringColor,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!imageLoaded) {
                    Text(
                        text = artist.name
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercaseChar().toString() }
                            .ifBlank { "A" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
                if (!artist.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artist.thumbnailUrl,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        onState = { imageLoaded = it is AsyncImagePainter.State.Success },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(palette.primaryBg)
                        .border(2.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = palette.primaryContent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            if (onPreviewClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPreviewPlaying) palette.primaryBg else Color.Black.copy(alpha = 0.78f),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .clickable(onClick = onPreviewClick),
                    contentAlignment = Alignment.Center,
                ) {
                    val previewTint = if (isPreviewPlaying) palette.primaryContent else Color.White
                    when {
                        isPreviewLoading -> CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 1.8.dp,
                            modifier = Modifier.size(12.dp),
                        )
                        isPreviewPlaying -> Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = "Stop sample",
                            tint = previewTint,
                            modifier = Modifier.size(13.dp),
                        )
                        else -> Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Sample artist",
                            tint = previewTint,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.9f),
            maxLines = 2,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (artist.isRelatedSuggestion) "Similar pick" else artist.role.singularLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (selected) palette.primaryBg else Color.White.copy(alpha = 0.56f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MiniPickedAvatar(
    artist: TasteArtistPick,
    borderColor: Color,
    onClick: () -> Unit,
) {
    var loaded by remember(artist.thumbnailUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color(0xFF20222A))
            .border(1.5.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) {
            Text(
                text = artist.name.firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        if (!artist.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = artist.thumbnailUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                onState = { loaded = it is AsyncImagePainter.State.Success },
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    }
}

@Composable
private fun TasteFilterChip(
    label: String,
    selected: Boolean,
    nothingOsMode: Boolean,
    palette: TasteUiPalette,
    emphasizeAccent: Boolean = false,
    onClick: () -> Unit,
) {
    val activeBg = when {
        nothingOsMode -> NothingThemeTokens.Red
        emphasizeAccent -> palette.primaryBg
        else -> palette.primaryBg
    }
    val activeContent = when {
        nothingOsMode -> Color.White
        else -> palette.primaryContent
    }
    val inactiveBg = if (nothingOsMode) NothingThemeTokens.Surface else Color.White.copy(alpha = 0.08f)
    val border = when {
        selected -> activeBg
        nothingOsMode -> NothingThemeTokens.Border
        else -> Color.White.copy(alpha = 0.16f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) activeBg else inactiveBg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) activeContent else Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun SelectionCountPill(
    text: String,
    highlighted: Boolean,
    nothingOsMode: Boolean,
) {
    val tint = when {
        nothingOsMode && highlighted -> NothingThemeTokens.Red
        highlighted -> Color.White
        else -> Color.White.copy(alpha = 0.78f)
    }
    val bg = when {
        nothingOsMode && highlighted -> NothingThemeTokens.Red.copy(alpha = 0.2f)
        highlighted -> Color.White.copy(alpha = 0.16f)
        else -> Color.White.copy(alpha = 0.08f)
    }
    val border = when {
        nothingOsMode && highlighted -> NothingThemeTokens.Red.copy(alpha = 0.75f)
        highlighted -> Color.White.copy(alpha = 0.48f)
        else -> Color.White.copy(alpha = 0.16f)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint,
        )
    }
}

/**
 * Reusable Taste Editor dialog shared across Android Settings and Desktop Settings
 * so listeners can refine languages, singers, and music directors anytime without
 * restarting onboarding or touching listening history.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicTasteEditorDialog(
    initialLanguages: List<String>,
    initialArtists: List<TasteArtistPick>,
    nothingOsMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (languageIds: List<String>, artists: List<TasteArtistPick>) -> Unit,
    onPreviewArtist: ((TasteArtistPick) -> Unit)? = null,
    previewingArtistKey: String? = null,
    previewLoadingArtistKey: String? = null,
) {
    var draftLanguages by remember(initialLanguages) {
        mutableStateOf(initialLanguages.ifEmpty { MusicTasteCatalog.defaultLanguageSelection() })
    }
    var draftArtists by remember(initialArtists) { mutableStateOf(initialArtists) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Languages, 1 = Singers & Directors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        WithTasteDarkTheme(nothingOsMode = nothingOsMode) {
            val palette = rememberTastePalette(nothingOsMode)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 760.dp)
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(24.dp)),
                color = if (nothingOsMode) NothingThemeTokens.Black else Color(0xFF0D0E13),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.12f),
                ),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = palette.primaryBg,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Music Taste",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = Color.White.copy(alpha = 0.78f),
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TasteFilterChip(
                                label = "Languages (${draftLanguages.size})",
                                selected = activeTab == 0,
                                nothingOsMode = nothingOsMode,
                                palette = palette,
                                emphasizeAccent = true,
                                onClick = { activeTab = 0 },
                            )
                            TasteFilterChip(
                                label = "Creators (${draftArtists.size})",
                                selected = activeTab == 1,
                                nothingOsMode = nothingOsMode,
                                palette = palette,
                                emphasizeAccent = true,
                                onClick = { activeTab = 1 },
                            )
                        }
                    }

                    if (activeTab == 0) {
                        MusicLanguagesStepContent(
                            selectedLanguageIds = draftLanguages,
                            onToggleLanguage = { langId ->
                                draftLanguages = if (langId in draftLanguages) {
                                    draftLanguages - langId
                                } else {
                                    draftLanguages + langId
                                }
                            },
                            onContinue = { activeTab = 1 },
                            nothingOsMode = nothingOsMode,
                            continueLabel = "Pick Singers & Directors",
                            skipLabel = "Save",
                            onSkip = { onSave(draftLanguages, draftArtists) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        MusicArtistsStepContent(
                            selectedLanguageIds = draftLanguages,
                            selectedArtists = draftArtists,
                            onToggleArtist = { artist ->
                                val exists = draftArtists.any { MusicTasteStore.sameArtist(it, artist) }
                                draftArtists = if (exists) {
                                    draftArtists.filterNot { MusicTasteStore.sameArtist(it, artist) }
                                } else {
                                    draftArtists + artist
                                }
                            },
                            onContinue = { onSave(draftLanguages, draftArtists) },
                            nothingOsMode = nothingOsMode,
                            continueLabel = "Save Personalization",
                            onBackStep = { activeTab = 0 },
                            onPreviewArtist = onPreviewArtist,
                            previewingArtistKey = previewingArtistKey,
                            previewLoadingArtistKey = previewLoadingArtistKey,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
