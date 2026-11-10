package com.music.bitchord.ui.home

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.weather.AuraLocationResult
import com.music.bitchord.data.weather.AuraWeatherRepository
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
        containerColor = if (nothingOsMode) NothingThemeTokens.Surface else Color(0xFF121420),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.22f)),
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
                        color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                    )
                    Text(
                        text = if (weather.isCustomLocation) "Custom location selected" else "Auto-detected via coarse IP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                        ),
                        color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.6f),
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
                        tint = if (nothingOsMode) NothingThemeTokens.White else Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Current Weather Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else Color(0xFF181B29),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.08f),
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
                                    .background(
                                        if (nothingOsMode) NothingThemeTokens.Red.copy(alpha = 0.16f)
                                        else Color(0xFF00D2FF).copy(alpha = 0.16f)
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF),
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
                                    color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!weather.country.isNullOrBlank()) {
                                    Text(
                                        text = weather.country!!,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.55f),
                                    )
                                }
                            }
                        }

                        // °C / °F Switcher Pill
                        Surface(
                            shape = RoundedCornerShape(percent = 50),
                            color = if (nothingOsMode) NothingThemeTokens.Surface else Color.White.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.15f),
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
                                    color = if (!weather.isFahrenheit) {
                                        if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF)
                                    } else Color.Transparent,
                                ) {
                                    Text(
                                        text = "°C",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = if (!weather.isFahrenheit) Color.White else (if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.5f)),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(percent = 50),
                                    color = if (weather.isFahrenheit) {
                                        if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF)
                                    } else Color.Transparent,
                                ) {
                                    Text(
                                        text = "°F",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                        ),
                                        color = if (weather.isFahrenheit) Color.White else (if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.5f)),
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
                            color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                        )

                        Text(
                            text = weather.conditionLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            ),
                            color = if (nothingOsMode) NothingThemeTokens.White else Color(0xFF7DD3FC),
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
                                        tint = if (nothingOsMode) NothingThemeTokens.MutedText else Color(0xFF38BDF8),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Humidity ${weather.humidity}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                        ),
                                        color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.65f),
                                    )
                                }
                            }
                            if (weather.windSpeedKmH != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Air,
                                        contentDescription = null,
                                        tint = if (nothingOsMode) NothingThemeTokens.MutedText else Color(0xFF38BDF8),
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Wind ${weather.windSpeedKmH} km/h",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                        ),
                                        color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.65f),
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
                    color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.10f),
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
                            tint = if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF),
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Reset to Current Location (Auto)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                            ),
                            color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // City Search Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (nothingOsMode) NothingThemeTokens.SurfaceElevated else Color(0xFF181B29),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (nothingOsMode) NothingThemeTokens.Border else Color.White.copy(alpha = 0.12f),
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
                        tint = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search any city or town worldwide...",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.4f),
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
                            ),
                            cursorBrush = SolidColor(if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.5.dp,
                            color = if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF),
                        )
                    } else if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear search",
                            tint = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" },
                        )
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
                                color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.45f),
                            )
                        }
                    }
                }

                items(searchResults) { result ->
                    val isSelected = weather.city == result.name && weather.isCustomLocation
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            if (nothingOsMode) NothingThemeTokens.SurfaceElevated else Color.White.copy(alpha = 0.10f)
                        } else Color.Transparent,
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
                                    color = if (nothingOsMode) NothingThemeTokens.White else Color.White,
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
                                        color = if (nothingOsMode) NothingThemeTokens.MutedText else Color.White.copy(alpha = 0.55f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = if (nothingOsMode) NothingThemeTokens.Red else Color(0xFF00D2FF),
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
