package com.music.bitchord

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.auth.DiscordLoginScreen
import com.music.bitchord.auth.GoogleAccountSession
import com.music.bitchord.auth.YouTubeProfile
import com.music.bitchord.data.AppUpdateChecker
import com.music.bitchord.download.DownloadSession
import com.music.bitchord.data.model.Account
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.scrobbling.LastFM
import com.music.bitchord.data.settings.AppSettings
import com.music.bitchord.data.settings.SongSort
import com.music.bitchord.data.sources.SourceConfig
import com.music.bitchord.data.sources.SourceKind
import com.music.bitchord.data.sources.SourceRegistry
import com.music.bitchord.data.stats.DailyHighlightsSummary
import com.music.bitchord.data.webdav.WebDavUploads
import com.music.bitchord.playback.QueueSource
import com.music.bitchord.data.ai.AuraBehaviorEngine
import com.music.bitchord.ui.MainViewModel
import com.music.bitchord.ui.ai.AuraChatScreen
import com.music.bitchord.ui.ai.AuraKaraokeSheet
import com.music.bitchord.ui.ai.AuraNeuralDjSheet
import com.music.bitchord.ui.ai.AuraRadarOverlay
import com.music.bitchord.ui.components.AccountProfileSelector
import com.music.bitchord.ui.components.AppLanguageDialog
import com.music.bitchord.ui.components.ConfirmationAlert
import com.music.bitchord.ui.components.DownloadManagerSheet
import com.music.bitchord.ui.components.FieldConfig
import com.music.bitchord.ui.components.LastfmLoginAlert
import com.music.bitchord.ui.components.ListenBrainzTokenAlert
import com.music.bitchord.ui.components.LyricsSourcesDialog
import com.music.bitchord.ui.screens.PartyServerEditor
import com.music.bitchord.ui.components.PlaylistPickerSheet
import com.music.bitchord.ui.components.ReorderPlaylistSheet
import com.music.bitchord.ui.components.ServerEditorHost
import com.music.bitchord.ui.player.SongShareSheet
import com.music.bitchord.ui.screens.SourceEditorAlert
import com.music.bitchord.ui.screens.SpotifyCanvasAuthScreen
import com.music.bitchord.ui.components.SpotifyImportAlert
import com.music.bitchord.ui.components.TranslationLanguageDialog
import com.music.bitchord.ui.components.UpdateAvailableDialog
import com.music.bitchord.ui.components.WebDavConflictAlert
import com.music.bitchord.ui.components.optimizedHazeEffect
import com.music.bitchord.ui.player.SmartSleepTimerSheet
import com.music.bitchord.data.stats.ReplayPeriod
import com.music.bitchord.ui.replay.ReplayScreen
import com.music.bitchord.ui.replay.ReplayState
import com.music.bitchord.ui.replay.ReplayStoryPage
import com.music.bitchord.ui.screens.AccountAndScrobblingScreen
import com.music.bitchord.ui.screens.DiscordDialog
import com.music.bitchord.ui.screens.DiscordDialogHost
import com.music.bitchord.ui.screens.EqualizerScreen
import com.music.bitchord.ui.screens.ListenTogetherScreen
import com.music.bitchord.ui.screens.SourcesScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BitChordAuraOverlays(
    hazeState: HazeState,
    playerSong: Song?,
    isPlaying: Boolean,
    playerQueue: List<Song>,
    dailyHighlights: DailyHighlightsSummary?,
    showSleepTimer: Boolean,
    onDismissSleepTimer: () -> Unit,
    showAuraChat: Boolean,
    onDismissAuraChat: () -> Unit,
    showAuraRadar: Boolean,
    onDismissAuraRadar: () -> Unit,
    showAuraKaraoke: Boolean,
    onDismissAuraKaraoke: () -> Unit,
    showAuraDj: Boolean,
    onDismissAuraDj: () -> Unit,
    togglePlayPause: () -> Unit,
    playFrom: (List<Song>, Int, QueueSource) -> Unit,
    addToQueue: (Song) -> Unit,
    onOpenEq: () -> Unit,
    onOpenParty: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenReplay: () -> Unit,
    onOpenRadar: () -> Unit,
) {
    if (showSleepTimer) {
        SmartSleepTimerSheet(
            hazeState = hazeState,
            onDismiss = onDismissSleepTimer,
        )
    }

    if (showAuraChat) {
        val auraBehaviorProfile = remember(dailyHighlights) {
            AuraBehaviorEngine.analyze(dailyHighlights)
        }
        AuraChatScreen(
            userProfile = auraBehaviorProfile,
            currentSong = playerSong,
            isPlaying = isPlaying,
            initialPersona = AppSettings.auraAiPersona.value,
            customGrokKey = AppSettings.grokApiKey.value,
            customGeminiKey = AppSettings.geminiApiKey.value,
            onClose = onDismissAuraChat,
            onTogglePlayPause = togglePlayPause,
            onPlaySong = { song: Song ->
                playFrom(listOf(song), 0, QueueSource("Aura AI", PlaybackSourceType.SEARCH))
            },
            onQueueSong = { song: Song -> addToQueue(song) },
            onPlayMix = { songs: List<Song> ->
                playFrom(songs, 0, QueueSource("Aura AI Mix", PlaybackSourceType.SEARCH))
            },
            onQueueMix = { songs: List<Song> -> songs.forEach { addToQueue(it) } },
            onOpenEq = {
                onDismissAuraChat()
                onOpenEq()
            },
            onOpenParty = {
                onDismissAuraChat()
                onOpenParty()
            },
            onOpenSleepTimer = {
                onDismissAuraChat()
                onOpenSleepTimer()
            },
            onOpenReplay = {
                onDismissAuraChat()
                onOpenReplay()
            },
            onOpenRadar = {
                onOpenRadar()
            },
        )
    }

    if (showAuraRadar) {
        AuraRadarOverlay(
            onClose = onDismissAuraRadar,
            onPlaySong = { song: Song ->
                playFrom(listOf(song), 0, QueueSource("Aura Radar", PlaybackSourceType.SEARCH))
            },
            onQueueSong = { song: Song -> addToQueue(song) },
        )
    }

    if (showAuraKaraoke) {
        AuraKaraokeSheet(
            song = playerSong,
            onClose = onDismissAuraKaraoke,
        )
    }

    if (showAuraDj) {
        AuraNeuralDjSheet(
            currentSong = playerSong,
            queueCandidates = playerQueue,
            onClose = onDismissAuraDj,
            onPlaySong = { song: Song ->
                playFrom(listOf(song), 0, QueueSource("Aura Neural DJ", PlaybackSourceType.SEARCH))
            },
            onQueueSong = { song: Song -> addToQueue(song) },
        )
    }
}

@Composable
fun BitChordDialogs(
    hazeState: HazeState,
    updateNotice: AppUpdateChecker.UpdateInfo?,
    showUpdateDialog: Boolean,
    onDismissUpdateDialog: () -> Unit,
    showLyricsSources: Boolean,
    onDismissLyricsSources: () -> Unit,
    songSortMenuOpen: Boolean,
    songSort: SongSort,
    detailBrowseId: String?,
    onDismissSongSortMenu: () -> Unit,
    showAccountSelector: Boolean,
    onDismissAccountSelector: () -> Unit,
    googleAccounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    onSelectProfile: (GoogleAccountSession, YouTubeProfile) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (GoogleAccountSession) -> Unit,
    onOpenSettingsFromAccounts: () -> Unit,
    showAppLanguage: Boolean,
    onDismissAppLanguage: () -> Unit,
    showTranslationLanguage: Boolean,
    onDismissTranslationLanguage: () -> Unit,
    showListenBrainzLogin: Boolean,
    onDismissListenBrainzLogin: () -> Unit,
    listenBrainzToken: String,
    showLastfmLogin: Boolean,
    onDismissLastfmLogin: () -> Unit,
    showWebDavEditor: Boolean,
    onDismissWebDavEditor: () -> Unit,
    showSmbEditor: Boolean,
    onDismissSmbEditor: () -> Unit,
    showDiscordLogin: Boolean,
    onDismissDiscordLogin: () -> Unit,
    showSpotifyCanvasAuth: Boolean,
    onDismissSpotifyCanvasAuth: () -> Unit,
    discordDialog: DiscordDialog?,
    onDismissDiscordDialog: () -> Unit,
    editingPartyServer: Boolean,
    onDismissEditingPartyServer: () -> Unit,
    editingSource: SourceConfig?,
    onDismissEditingSource: () -> Unit,
    confirmJioSaavn: Boolean,
    onDismissConfirmJioSaavn: () -> Unit,
    scope: CoroutineScope,
    context: Context,
) {
    if (showUpdateDialog) {
        updateNotice?.let { update ->
            UpdateAvailableDialog(
                version = update.version,
                notes = update.notes,
                isMandatory = update.isMandatory,
                hazeState = hazeState,
                onDismiss = onDismissUpdateDialog,
                onDownload = {
                    if (update.apkUrl != null) {
                        scope.launch { AppUpdateChecker.downloadApk(context) }
                    } else {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                        onDismissUpdateDialog()
                    }
                },
                onCancelDownload = {
                    AppUpdateChecker.cancelDownload(context)
                },
                onInstall = {
                    val ready = AppUpdateChecker.download.value as? AppUpdateChecker.DownloadState.Ready
                    ready?.let { AppUpdateChecker.installApk(context, it.file) }
                    onDismissUpdateDialog()
                },
                onOpenReleasePage = {
                    AppUpdateChecker.resetDownload(context)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl)))
                    onDismissUpdateDialog()
                },
                onSkipVersion = {
                    AppSettings.skipUpdateVersion(update.version)
                    onDismissUpdateDialog()
                },
            )
        }
    }

    if (showLyricsSources) {
        BackHandler(onBack = onDismissLyricsSources)
        LyricsSourcesDialog(
            hazeState = hazeState,
            onDismiss = onDismissLyricsSources,
        )
    }

    if (songSortMenuOpen) {
        BackHandler(onBack = onDismissSongSortMenu)
        FrostedSortMenu(
            hazeState = hazeState,
            selected = songSort,
            onSelect = { option ->
                detailBrowseId?.let { AppSettings.setDetailSongSort(it, option) }
                onDismissSongSortMenu()
            },
            onFlipDateDirection = {
                detailBrowseId?.let { browseId ->
                    val next = when (songSort) {
                        SongSort.DATE_ADDED_DESC -> SongSort.DATE_ADDED_ASC
                        SongSort.DATE_ADDED_ASC -> SongSort.DATE_ADDED_DESC
                        else -> SongSort.DATE_ADDED_DESC
                    }
                    AppSettings.setDetailSongSort(browseId, next)
                }
            },
            onDismiss = onDismissSongSortMenu,
        )
    }

    if (showAccountSelector) {
        BackHandler(onBack = onDismissAccountSelector)
        AccountProfileSelector(
            accounts = googleAccounts,
            activeAccountId = activeAccountId,
            activeProfileId = activeProfileId,
            hazeState = hazeState,
            onSelect = onSelectProfile,
            onAddAccount = onAddAccount,
            onRemoveAccount = onRemoveAccount,
            onOpenSettings = onOpenSettingsFromAccounts,
            onDismiss = onDismissAccountSelector,
        )
    }

    if (showAppLanguage) {
        BackHandler(onBack = onDismissAppLanguage)
        AppLanguageDialog(
            hazeState = hazeState,
            onDismiss = onDismissAppLanguage,
        )
    }

    if (showTranslationLanguage) {
        BackHandler(onBack = onDismissTranslationLanguage)
        TranslationLanguageDialog(
            hazeState = hazeState,
            onDismiss = onDismissTranslationLanguage,
        )
    }

    if (showListenBrainzLogin) {
        var tokenInput by remember { mutableStateOf(listenBrainzToken) }
        ListenBrainzTokenAlert(
            hazeState = hazeState,
            tokenInput = tokenInput,
            onTokenInputChange = { tokenInput = it },
            onSave = {
                AppSettings.setListenBrainzToken(tokenInput.trim())
                onDismissListenBrainzLogin()
            },
            onDismiss = onDismissListenBrainzLogin,
        )
    }

    if (showLastfmLogin) {
        var usernameInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var lastfmError by remember { mutableStateOf<String?>(null) }
        var lastfmLoading by remember { mutableStateOf(false) }
        LastfmLoginAlert(
            hazeState = hazeState,
            usernameInput = usernameInput,
            onUsernameInputChange = { usernameInput = it },
            passwordInput = passwordInput,
            onPasswordInputChange = { passwordInput = it },
            error = lastfmError,
            loading = lastfmLoading,
            onSignIn = {
                lastfmLoading = true
                lastfmError = null
                scope.launch {
                    try {
                        LastFM.initialize(
                            apiKey = AppSettings.lastfmApiKey.value,
                            secret = AppSettings.lastfmSecret.value,
                        )
                        LastFM.getMobileSession(usernameInput.trim(), passwordInput)
                            .onSuccess { auth ->
                                AppSettings.setLastfmSessionKey(auth.session.key)
                                AppSettings.setLastfmUsername(auth.session.name)
                                AppSettings.setLastfmEnabled(true)
                                onDismissLastfmLogin()
                            }
                            .onFailure { e ->
                                lastfmError = e.message ?: context.getString(R.string.login_failed)
                            }
                    } catch (e: Exception) {
                        lastfmError = e.message ?: context.getString(R.string.login_failed)
                    } finally {
                        lastfmLoading = false
                    }
                }
            },
            onDismiss = { if (!lastfmLoading) onDismissLastfmLogin() },
        )
    }

    if (showWebDavEditor) {
        BackHandler(onBack = onDismissWebDavEditor)
        ServerEditorHost(
            hazeState = hazeState,
            title = stringResource(R.string.webdav),
            description = stringResource(R.string.webdav_description),
            fields = listOf(
                FieldConfig(
                    initial = AppSettings.webdavUrl.value,
                    placeholder = stringResource(R.string.webdav_server_url_hint),
                    keyboardType = KeyboardType.Uri,
                ),
                FieldConfig(
                    initial = AppSettings.webdavUsername.value,
                    placeholder = stringResource(R.string.username),
                ),
                FieldConfig(
                    initial = AppSettings.webdavPassword.value,
                    placeholder = stringResource(R.string.password),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                ),
            ),
            canSubmit = { it[0].isNotBlank() },
            testFailedRes = R.string.webdav_test_failed,
            onTest = { (url, username, password) ->
                com.music.bitchord.data.webdav.WebDavRepository.testConnection(
                    url.trim(),
                    username.trim(),
                    password,
                )
            },
            onSave = { (url, username, password) ->
                AppSettings.setWebDavUrl(url.trim())
                AppSettings.setWebDavUsername(username.trim())
                AppSettings.setWebDavPassword(password)
                onDismissWebDavEditor()
            },
            onDismiss = onDismissWebDavEditor,
        )
    }

    if (showSmbEditor) {
        BackHandler(onBack = onDismissSmbEditor)
        ServerEditorHost(
            hazeState = hazeState,
            title = stringResource(R.string.smb),
            description = stringResource(R.string.smb_description),
            fields = listOf(
                FieldConfig(
                    initial = AppSettings.smbHost.value,
                    placeholder = stringResource(R.string.smb_server_hint),
                    keyboardType = KeyboardType.Uri,
                ),
                FieldConfig(
                    initial = AppSettings.smbShare.value,
                    placeholder = stringResource(R.string.smb_share_hint),
                ),
                FieldConfig(
                    initial = AppSettings.smbBasePath.value,
                    placeholder = stringResource(R.string.smb_folder_hint),
                ),
                FieldConfig(
                    initial = AppSettings.smbUsername.value,
                    placeholder = stringResource(R.string.username),
                ),
                FieldConfig(
                    initial = AppSettings.smbPassword.value,
                    placeholder = stringResource(R.string.password),
                    keyboardType = KeyboardType.Password,
                    isPassword = true,
                ),
            ),
            canSubmit = { it[0].isNotBlank() && it[1].isNotBlank() },
            testFailedRes = R.string.smb_test_failed,
            onTest = { (host, share, folder, username, password) ->
                com.music.bitchord.data.smb.SmbRepository.testConnection(
                    host.trim(),
                    share.trim(),
                    folder.trim(),
                    username.trim(),
                    password,
                )
            },
            onSave = { (host, share, folder, username, password) ->
                AppSettings.setSmbHost(host.trim())
                AppSettings.setSmbShare(share.trim())
                AppSettings.setSmbBasePath(folder.trim())
                AppSettings.setSmbUsername(username.trim())
                AppSettings.setSmbPassword(password)
                onDismissSmbEditor()
            },
            onDismiss = onDismissSmbEditor,
        )
    }

    val uploadConflict by WebDavUploads.conflict.collectAsStateWithLifecycle()
    uploadConflict?.let { req ->
        var applyToAll by remember(req) { mutableStateOf(false) }
        val answer: (WebDavUploads.Choice) -> Unit = { choice ->
            req.answer.complete(
                WebDavUploads.Resolution(choice, applyToAll),
            )
        }
        BackHandler { answer(WebDavUploads.Choice.SKIP) }
        WebDavConflictAlert(
            hazeState = hazeState,
            fileName = req.fileName,
            showApplyToAll = req.remaining > 0,
            applyToAll = applyToAll,
            onApplyToAllChange = { applyToAll = it },
            onOverwrite = { answer(WebDavUploads.Choice.OVERWRITE) },
            onKeepBoth = { answer(WebDavUploads.Choice.KEEP_BOTH) },
            onSkip = { answer(WebDavUploads.Choice.SKIP) },
            onDismiss = { answer(WebDavUploads.Choice.SKIP) },
        )
    }

    if (showDiscordLogin) {
        BackHandler(onBack = onDismissDiscordLogin)
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismissDiscordLogin) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        "Sign in to Discord",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                DiscordLoginScreen(
                    onTokenCaptured = { token ->
                        AppSettings.setDiscordToken(token)
                        onDismissDiscordLogin()
                    },
                )
            }
        }
    }

    if (showSpotifyCanvasAuth) {
        BackHandler(onBack = onDismissSpotifyCanvasAuth)
        SpotifyCanvasAuthScreen(
            onNavigateUp = onDismissSpotifyCanvasAuth,
        )
    }

    discordDialog?.let { which ->
        DiscordDialogHost(
            which = which,
            hazeState = hazeState,
            onDismiss = onDismissDiscordDialog,
        )
    }

    if (editingPartyServer) {
        PartyServerEditor(
            hazeState = hazeState,
            onDismiss = onDismissEditingPartyServer,
        )
    }

    editingSource?.let { config ->
        SourceEditorAlert(
            hazeState = hazeState,
            config = config,
            onDismiss = onDismissEditingSource,
            onSaved = onDismissEditingSource,
            onDelete = {
                SourceRegistry.remove(config.id)
                onDismissEditingSource()
            },
            scope = scope,
        )
    }

    if (confirmJioSaavn) {
        ConfirmationAlert(
            hazeState = hazeState,
            title = stringResource(R.string.enable_jiosaavn),
            description = stringResource(R.string.jiosaavn_mismatch_warning),
            confirmLabel = stringResource(R.string.enable_anyway),
            onConfirm = {
                SourceRegistry.configs.value
                    .firstOrNull { it.kind == SourceKind.JIOSAAVN }
                    ?.let { SourceRegistry.setEnabled(it.id, true) }
                onDismissConfirmJioSaavn()
            },
            onDismiss = onDismissConfirmJioSaavn,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitChordManagementSheets(
    songToShare: Song?,
    onDismissSongShare: () -> Unit,
    lyricsLines: List<String>,
    hazeState: HazeState,
    showDownloadManager: Boolean,
    onDismissDownloadManager: () -> Unit,
    playlistTarget: Song?,
    creatingPlaylist: Boolean,
    onDismissPlaylistPicker: () -> Unit,
    playlists: List<UserPlaylist>,
    playlistsLoading: Boolean,
    showSpotifyImportDialog: Boolean,
    onDismissSpotifyImport: () -> Unit,
    signedIn: Boolean,
    reorderTarget: UserPlaylist?,
    reorderEntries: UiState<List<Song>>,
    reorderSaving: Boolean,
    onDismissReorder: () -> Unit,
    onSaveReorder: (List<Song>) -> Unit,
    onAddToPlaylists: (List<UserPlaylist>, Song) -> Unit,
    onCreatePlaylist: (String, PlaylistPrivacy, Song?) -> Unit,
    onImportSpotifyPlaylist: (String, PlaylistPrivacy, List<Song>) -> Unit,
) {
    songToShare?.let { song ->
        SongShareSheet(
            song = song,
            hazeState = hazeState,
            lyricsLines = lyricsLines,
            onDismiss = onDismissSongShare,
        )
    }

    if (showDownloadManager) {
        BackHandler(onBack = onDismissDownloadManager)
        ModalBottomSheet(
            onDismissRequest = onDismissDownloadManager,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            DownloadManagerSheet(onDismiss = onDismissDownloadManager)
        }
    }

    if (playlistTarget != null || creatingPlaylist) {
        val target = playlistTarget
        ModalBottomSheet(
            onDismissRequest = onDismissPlaylistPicker,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            PlaylistPickerSheet(
                playlists = playlists,
                loading = playlistsLoading,
                song = target,
                startCreating = target == null,
                onAdd = { picked ->
                    target?.let { song ->
                        onAddToPlaylists(picked, song)
                    }
                    onDismissPlaylistPicker()
                },
                onCreate = { title, privacy ->
                    onCreatePlaylist(title, privacy, target)
                    onDismissPlaylistPicker()
                },
            )
        }
    }

    if (showSpotifyImportDialog) {
        BackHandler(onBack = onDismissSpotifyImport)
        SpotifyImportAlert(
            hazeState = hazeState,
            signedIn = signedIn,
            onImported = onImportSpotifyPlaylist,
            onDismiss = onDismissSpotifyImport,
        )
    }

    reorderTarget?.let { target ->
        val keepSheetStill = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available
                override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
            }
        }
        ModalBottomSheet(
            onDismissRequest = onDismissReorder,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            ReorderPlaylistSheet(
                modifier = Modifier.nestedScroll(keepSheetStill),
                playlist = target,
                entries = reorderEntries,
                saving = reorderSaving,
                onClose = onDismissReorder,
                onSave = onSaveReorder,
            )
        }
    }
}

@Composable
fun BitChordSettingsSubScreens(
    settingsSubScreen: String?,
    showDiscord: Boolean,
    showSpotify: Boolean,
    signedIn: Boolean,
    account: Account?,
    selectedChannelName: String?,
    activeJamInviteCode: String?,
    activeJamInviteServer: String?,
    listPadding: PaddingValues,
    replay: ReplayState,
    replayLabel: String,
    replayListState: LazyListState,
    replayLandingPage: ReplayStoryPage,
    showReplay: Boolean,
    showReplayShare: Boolean,
    viewModel: MainViewModel,
    onSignIn: () -> Unit,
    onSwitchChannel: () -> Unit,
    onSignOut: () -> Unit,
    onOpenListenBrainzLogin: () -> Unit,
    onOpenLastfmLogin: () -> Unit,
    onOpenDiscord: () -> Unit,
    onOpenSpotify: () -> Unit,
    onEditSource: (SourceConfig) -> Unit,
    onEditWebDav: () -> Unit,
    onEditSmb: () -> Unit,
    onConfirmJioSaavn: () -> Unit,
    onInviteHandled: () -> Unit,
    onEditPartyServer: () -> Unit,
    setReplayPeriod: (ReplayPeriod) -> Unit,
    onOpenReplayStory: (ReplayStoryPage) -> Unit,
    playRadio: (Song, QueueSource) -> Unit,
    onOpenArtist: (String?, String) -> Unit,
    onOpenAlbum: (String?, String, String?, String?) -> Unit,
    onShareReplay: () -> Unit,
) {
    when (settingsSubScreen.takeUnless { showDiscord || showSpotify }) {
        "account_scrobbling" -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                AccountAndScrobblingScreen(
                    signedIn = signedIn,
                    account = account,
                    channelName = selectedChannelName,
                    onSignIn = onSignIn,
                    onSwitchChannel = onSwitchChannel,
                    onSignOut = onSignOut,
                    onOpenListenBrainzLogin = onOpenListenBrainzLogin,
                    onOpenLastfmLogin = onOpenLastfmLogin,
                    onOpenDiscord = onOpenDiscord,
                    onOpenSpotify = onOpenSpotify,
                    contentPadding = listPadding,
                )
            }
        }
        "sources" -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                SourcesScreen(
                    contentPadding = listPadding,
                    onEditSource = onEditSource,
                    onEditWebDav = onEditWebDav,
                    onEditSmb = onEditSmb,
                    onConfirmJioSaavn = onConfirmJioSaavn,
                )
            }
        }
        "listen_together" -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                ListenTogetherScreen(
                    signedIn = signedIn,
                    inviteCode = activeJamInviteCode,
                    inviteServer = activeJamInviteServer,
                    onInviteHandled = onInviteHandled,
                    onSignIn = onSignIn,
                    contentPadding = listPadding,
                    onEditServer = onEditPartyServer,
                    viewModel = viewModel,
                )
            }
        }
        "equalizer" -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                EqualizerScreen(contentPadding = listPadding)
            }
        }
        "replay" -> {
            if (showReplay && !showReplayShare) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ReplayScreen(
                        state = replay,
                        holder = account?.name.orEmpty(),
                        onPeriodChange = setReplayPeriod,
                        onOpenStory = onOpenReplayStory,
                        onPlaySong = { song: Song ->
                            playRadio(song, QueueSource(replayLabel, PlaybackSourceType.REPLAY))
                        },
                        onOpenArtist = onOpenArtist,
                        onOpenAlbum = onOpenAlbum,
                        onShare = onShareReplay,
                        contentPadding = listPadding,
                        listState = replayListState,
                        landingPage = replayLandingPage,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FrostedSortMenu(
    hazeState: HazeState,
    selected: SongSort,
    onSelect: (SongSort) -> Unit,
    onFlipDateDirection: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val shape = MaterialTheme.shapes.extraLarge

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = .48f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopEnd,
    ) {
        Surface(
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = shape,
            modifier = Modifier
                .padding(top = 56.dp, end = 20.dp)
                .width(IntrinsicSize.Max)
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.optimizedHazeEffect(
                            state = hazeState,
                            style = HazeMaterials.thin(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(onClick = {}),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                val dateActive = selected == SongSort.DATE_ADDED_ASC ||
                    selected == SongSort.DATE_ADDED_DESC
                val arrowRotation by animateFloatAsState(
                    targetValue = if (selected == SongSort.DATE_ADDED_ASC) 180f else 0f,
                    animationSpec = tween(durationMillis = 200),
                    label = "dateAddedArrow",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { onFlipDateDirection() }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sort_date_added_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (dateActive) {
                        Icon(
                            Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.rotate(arrowRotation),
                        )
                    }
                }
                SongSort.entries
                    .filter { it != SongSort.DATE_ADDED_ASC && it != SongSort.DATE_ADDED_DESC }
                    .forEach { option: SongSort ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable(role = Role.Button) { onSelect(option) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option.localizedLabel(),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (option == selected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
            }
        }
    }
}
