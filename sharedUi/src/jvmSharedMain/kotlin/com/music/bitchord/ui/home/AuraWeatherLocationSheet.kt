package com.music.bitchord.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.weather.AuraLocationResult
import com.music.bitchord.data.weather.AuraWeatherRepository
import com.music.bitchord.data.weather.WeatherVisualType
import com.music.bitchord.ui.theme.NothingThemeTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modal Bottom Sheet for Weather & Location controls:
 * - Live temperature and atmospheric stats (humidity, wind speed).
 * - Instant Celsius (°C) and Fahrenheit (°F) toggle.
 * - Worldwide keyless city search powered by Open-Meteo Geocoding.
 * - 1-tap reset to Auto-Detect (Coarse IP Geolocation).
 * - Strictly zero Unicode emojis - clean vector glyphs and typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraWeatherLocationSheet(
    nothingOsMode: Boolean,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val weather by AuraWeatherRepository.currentWeather.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val sheetBg = when {
        nothingOsMode && isDark -> NothingThemeTokens.Surface
        nothingOsMode -> Color(0xFFF3F3F7)
        isDark -> Color(0xFF121420)
        else -> Color(0xFFF8FAFC)
    }
    val cardBg = when {
        nothingOsMode && isDark -> NothingThemeTokens.SurfaceElevated
        nothingOsMode -> Color(0xFFE7E7EE)
        isDark -> Color(0xFF181B29)
        else -> Color(0xFFF1F5F9)
    }
    val subtleSurfaceBg = when {
        nothingOsMode && isDark -> NothingThemeTokens.Surface
        nothingOsMode -> Color(0xFFE7E7EE)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color(0xFFE2E8F0)
    }
    val subtleActionBg = when {
        nothingOsMode && isDark -> NothingThemeTokens.SurfaceElevated
        nothingOsMode -> Color(0xFFE7E7EE)
        isDark -> Color.White.copy(alpha = 0.06f)
        else -> Color(0xFFE2E8F0)
    }
    val borderStrokeColor = when {
        nothingOsMode && isDark -> NothingThemeTokens.Border
        nothingOsMode -> Color(0xFFD4D4DC)
        isDark -> Color.White.copy(alpha = 0.12f)
        else -> Color(0xFFCBD5E1)
    }
    val primaryText = if (isDark) Color.White else Color(0xFF0F172A)
    val mutedText = when {
        nothingOsMode && isDark -> NothingThemeTokens.MutedText
        isDark -> Color.White.copy(alpha = 0.60f)
        else -> Color(0xFF475569)
    }
    val accentBlueOrRed = if (nothingOsMode) NothingThemeTokens.Red else if (isDark) Color(0xFF00D2FF) else Color(0xFF0284C7)
    val skyInfoAccent = if (nothingOsMode) mutedText else if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AuraLocationResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Debounced search querying Open-Meteo Geocoding API
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(320) // debounce
        searchResults = AuraWeatherRepository.searchCities(q)
        isSearching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (nothingOsMode && isDark) NothingThemeTokens.Border else if (isDark) Color.White.copy(alpha = 0.22f) else Color(0xFF94A3B8).copy(alpha = 0.45f)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .navigationBarsPadding(),
        ) {
            // Header Row: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (nothingOsMode) "W E A T H E R   &   L O C A T I O N" else "Weather & Location",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (nothingOsMode) FontFamily.Monospace else FontFamily.Default,
                            fontSize = if (nothingOsMode) 13.sp else 16.sp,
                            letterSpacing = if (nothingOsMode) 1.2.sp else (-0.2).sp,
                        ),
                        color = primaryText,
                    )
                    Text(
                        text = if (weather.isCustomLocation) "Custom location selected" else "Auto-detected via coarse IP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                        ),
                        color = mutedText,
                    )
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = primaryText.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Current Weather Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = borderStrokeColor,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Top: Location info + Unit toggle button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(accentBlueOrRed.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = accentBlueOrRed,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = weather.city ?: "Detecting location...",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!weather.country.isNullOrBlank()) {
                                    Text(
                                        text = weather.country!!,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = mutedText,
                                    )
                                }
                            }
                        }

                        // °C / °F Switcher Pill
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = subtleSurfaceBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                borderStrokeColor,
                            ),
                            modifier = Modifier.clickable {
                                AuraWeatherRepository.toggleTemperatureUnit()
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = if (!weather.isFahrenheit) accentBlueOrRed else Color.Transparent,
                                ) {
                                    Text(
                                        text = "°C",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = if (!weather.isFahrenheit) Color.White else mutedText,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = if (weather.isFahrenheit) accentBlueOrRed else Color.Transparent,
                                ) {
                                    Text(
                                        text = "°F",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = if (weather.isFahrenheit) Color.White else mutedText,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Middle: Big Temperature & Condition
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            text = weather.displayTemperature,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                letterSpacing = (-1).sp,
                            ),
                            color = primaryText,
                        )

                        Text(
                            text = weather.conditionLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            ),
                            color = if (nothingOsMode) primaryText else if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                        )
                    }

                    // Micro stats: Humidity & Wind speed
                    if (weather.humidity != null || weather.windSpeedKmH != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (weather.humidity != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.WaterDrop,
                                        contentDescription = null,
                                        tint = skyInfoAccent,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Humidity ${weather.humidity}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                        ),
                                        color = mutedText,
                                    )
                                }
                            }
                            if (weather.windSpeedKmH != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Air,
                                        contentDescription = null,
                                        tint = skyInfoAccent,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Wind ${weather.windSpeedKmH} km/h",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                        ),
                                        color = mutedText,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Reset to Auto-Detect button (if currently custom)
            if (weather.isCustomLocation) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = subtleActionBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        borderStrokeColor,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            coroutineScope.launch {
                                AuraWeatherRepository.clearCustomLocation()
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = null,
                            tint = accentBlueOrRed,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Reset to Current Location (Auto)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                            ),
                            color = primaryText,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Interactive Atmospheric Scene Controls (Sunrise, Sunny Day, Sunset, Clear Night, Rain, Storm, Snow)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (nothingOsMode) "A T M O S P H E R E   S C E N E" else "Atmospheric Scene",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = if (nothingOsMode) FontFamily.Monospace else FontFamily.Default,
                            fontSize = 11.5.sp,
                            letterSpacing = if (nothingOsMode) 1.sp else (-0.2).sp,
                        ),
                        color = primaryText,
                    )
                    val currentOverride = AuraWeatherRepository.currentManualVisualOverride()
                    if (currentOverride != null) {
                        Text(
                            text = "Manual Mode",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp,
                            ),
                            color = skyInfoAccent.let { if (nothingOsMode) NothingThemeTokens.Red else it },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                val scenes = listOf(
                    null to "Auto",
                    WeatherVisualType.SUNRISE to "Sunrise",
                    WeatherVisualType.CLEAR_DAY to "Sunny Day",
                    WeatherVisualType.SUNSET to "Sunset",
                    WeatherVisualType.CLEAR_NIGHT to "Clear Night",
                    WeatherVisualType.CLOUDY to "Cloudy",
                    WeatherVisualType.RAIN to "Rain",
                    WeatherVisualType.THUNDERSTORM to "Storm",
                    WeatherVisualType.SNOW to "Snow",
                )
                val currentOverride = AuraWeatherRepository.currentManualVisualOverride()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for ((sceneType, label) in scenes) {
                        val isSelected = (currentOverride == sceneType) || (sceneType == null && currentOverride == null)
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (isSelected) accentBlueOrRed else subtleActionBg,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else borderStrokeColor,
                            ),
                            modifier = Modifier.clickable {
                                AuraWeatherRepository.setManualVisualOverride(sceneType)
                            },
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                ),
                                color = if (isSelected) Color.White else primaryText.copy(alpha = 0.78f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // City Search Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = cardBg,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = borderStrokeColor,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = mutedText,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search any city or town worldwide...",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = mutedText,
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                color = primaryText,
                            ),
                            cursorBrush = SolidColor(accentBlueOrRed),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = accentBlueOrRed,
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { searchQuery = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = mutedText,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Search Results List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
            ) {
                if (searchQuery.length >= 2 && searchResults.isEmpty() && !isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No matching cities found",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = mutedText,
                            )
                        }
                    }
                }

                items(
                    items = searchResults,
                    key = { "${it.name}_${it.country}_${it.latitude}_${it.longitude}" },
                ) { result ->
                    val isSelected = weather.city == result.name && weather.isCustomLocation
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) subtleSurfaceBg else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    AuraWeatherRepository.setCustomLocation(result)
                                    focusManager.clearFocus()
                                    sheetState.hide()
                                    onDismiss()
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                    ),
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val subtitle = result.displaySubtitle
                                if (subtitle.isNotBlank()) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                        ),
                                        color = mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = accentBlueOrRed,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
