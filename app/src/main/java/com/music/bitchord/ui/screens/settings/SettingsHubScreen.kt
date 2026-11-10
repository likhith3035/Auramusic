package com.music.bitchord.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.BuildConfig
import com.music.bitchord.R
import com.music.bitchord.data.model.Account
import com.music.bitchord.ui.screens.RowDivider
import com.music.bitchord.ui.screens.SettingsGroup
import com.music.bitchord.ui.screens.SettingsPageTitle
import com.music.bitchord.ui.screens.SettingsRow

@Composable
fun SettingsHubScreen(
    windowWidth: Dp,
    signedIn: Boolean,
    account: Account?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onAccountScrobbling: () -> Unit,
    onNavigate: (SettingsRoute) -> Unit,
    onSources: () -> Unit,
    onEqualizer: () -> Unit,
    onOpenReplay: () -> Unit,
    onAppLanguage: () -> Unit,
    onShowUpdateDialog: (() -> Unit)?,
    contentPadding: PaddingValues,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchResultsContent: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding),
    ) {
        SettingsPageTitle(stringResource(R.string.settings))

        // Search Field matching AuraMusic surface style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.settings_search_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                ),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (searchResultsContent != null) {
            searchResultsContent()
        } else {
            // Profile / Account Banner Card
        SettingsGroup {
            if (signedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAccountScrobbling)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val avatarUrl = account?.thumbnailUrl
                    if (avatarUrl != null) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = account?.name ?: stringResource(R.string.youtube_music_account),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = account?.email ?: stringResource(R.string.signed_in),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "Manage",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                SettingsRow(
                    icon = Icons.Rounded.Cloud,
                    title = stringResource(R.string.sign_in),
                    subtitle = stringResource(R.string.tap_to_sign_in_google),
                    onClick = onSignIn,
                )
            }
        }

        // Primary 7 Category Domain Cards
        SettingsGroup(header = stringResource(R.string.settings_hub_audio_title)) {
            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = stringResource(R.string.settings_hub_audio_title),
                subtitle = stringResource(R.string.settings_hub_audio_subtitle),
                onClick = { onNavigate(SettingsRoute.AUDIO) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.settings_hub_appearance_title),
                subtitle = stringResource(R.string.settings_hub_appearance_subtitle),
                onClick = { onNavigate(SettingsRoute.APPEARANCE) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.settings_hub_content_title),
                subtitle = stringResource(R.string.settings_hub_content_subtitle),
                onClick = { onNavigate(SettingsRoute.CONTENT) },
            )
        }

        SettingsGroup(header = stringResource(R.string.storage)) {
            SettingsRow(
                icon = Icons.Rounded.Download,
                title = stringResource(R.string.settings_hub_storage_title),
                subtitle = stringResource(R.string.settings_hub_storage_subtitle),
                onClick = { onNavigate(SettingsRoute.STORAGE) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Wifi,
                title = stringResource(R.string.settings_hub_network_title),
                subtitle = stringResource(R.string.settings_hub_network_subtitle),
                onClick = { onNavigate(SettingsRoute.NETWORK) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Extension,
                title = stringResource(R.string.settings_hub_sources_title),
                subtitle = stringResource(R.string.settings_hub_sources_subtitle),
                onClick = onSources,
            )
        }

        SettingsGroup(header = stringResource(R.string.advanced_options)) {
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.settings_hub_advanced_title),
                subtitle = stringResource(R.string.settings_hub_advanced_subtitle),
                onClick = { onNavigate(SettingsRoute.ADVANCED) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Equalizer,
                title = stringResource(R.string.equalizer),
                subtitle = stringResource(R.string.equalizer_subtitle),
                onClick = onEqualizer,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.History,
                title = stringResource(R.string.replay),
                subtitle = stringResource(R.string.replay_subtitle),
                onClick = onOpenReplay,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.app_language),
                onClick = onAppLanguage,
            )
            if (onShowUpdateDialog != null) {
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "Check for Software Updates",
                    subtitle = "Current build: v${BuildConfig.VERSION_NAME}",
                    onClick = onShowUpdateDialog,
                )
            }
        }

        // Developer Credit & Architecture Card
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                ) {
                    // Header Tag Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                            )
                            Text(
                                text = "DEVELOPER CREDIT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.2.sp,
                            )
                        }
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "kami likhith",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Lead Architecture & Engineering · Bit-Perfect Sound Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Crafted with precision for bit-perfect audio, ambient intelligence, and an uncompromising listening experience.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )

                    Spacer(Modifier.height(14.dp))

                    // Action buttons: Developer Profile + Repository
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/likhith3035"),
                                    )
                                    context.startActivity(intent)
                                },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Developer",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/likhith3035/Auramusic"),
                                    )
                                    context.startActivity(intent)
                                },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Code,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Repository",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        }
    }
}
