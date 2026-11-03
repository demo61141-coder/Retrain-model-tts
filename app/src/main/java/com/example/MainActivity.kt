package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AudioPlayerScreen
import com.example.ui.HomeStudioScreen
import com.example.ui.LocalHistoryScreen
import com.example.ui.NoticeDialogState
import com.example.ui.StudioDestination
import com.example.ui.SystemTtsSetupScreen
import com.example.ui.TtsStudioViewModel
import com.example.ui.theme.DeepDarkBackground
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.MutedSlateSubtext
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonSkyAccent
import com.example.ui.theme.PureWhiteText
import com.example.ui.theme.SlateGraySurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ReporterHasanTtsApp()
            }
        }
    }
}

@Composable
fun ReporterHasanTtsApp(
    studioViewModel: TtsStudioViewModel = viewModel()
) {
    val uiState by studioViewModel.uiState.collectAsStateWithLifecycle()
    val historyList by studioViewModel.historyList.collectAsStateWithLifecycle()

    // Auto-dismiss toast banner after 3.5 seconds
    LaunchedEffect(uiState.toastBannerMessage) {
        if (uiState.toastBannerMessage != null) {
            delay(3500L)
            studioViewModel.clearToastBanner()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            StudioBottomNavigationBar(
                currentDestination = uiState.currentDestination,
                onSelectDestination = { studioViewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentDestination) {
                StudioDestination.HOME -> {
                    HomeStudioScreen(
                        uiState = uiState,
                        recentHistory = historyList,
                        onSelectTemplate = studioViewModel::selectTemplate,
                        onScriptChange = studioViewModel::updateScriptText,
                        onSpeedChange = studioViewModel::updateSpeed,
                        onPitchChange = studioViewModel::updatePitch,
                        onResetVoiceSettings = studioViewModel::resetVoiceSettings,
                        onGenerateClick = studioViewModel::generateVoice,
                        onPlayHistoryItem = { studioViewModel.openAudioPlayerForItem(it, autoPlay = true) },
                        onSaveHistoryItem = studioViewModel::saveAudioToDevice,
                        onOpenSettings = { studioViewModel.navigateTo(StudioDestination.TTS_SETUP) },
                        onOpenAllHistory = { studioViewModel.navigateTo(StudioDestination.HISTORY) }
                    )
                }

                StudioDestination.AUDIO_PLAYER -> {
                    AudioPlayerScreen(
                        item = uiState.activeAudioItem,
                        isPlaying = uiState.isPlaying,
                        positionMs = uiState.playbackPositionMs,
                        durationMs = uiState.playbackDurationMs,
                        onBackClick = studioViewModel::navigateBack,
                        onTogglePlayPause = studioViewModel::togglePlayPause,
                        onSeekFraction = studioViewModel::seekToFraction,
                        onRewind5s = { studioViewModel.seekRelativeMs(-5000) },
                        onForward5s = { studioViewModel.seekRelativeMs(5000) },
                        onSaveToDevice = studioViewModel::saveAudioToDevice,
                        onShareAudio = studioViewModel::shareAudioItem
                    )
                }

                StudioDestination.HISTORY -> {
                    LocalHistoryScreen(
                        historyItems = historyList,
                        searchQuery = uiState.historySearchQuery,
                        onSearchQueryChange = studioViewModel::updateHistorySearchQuery,
                        onBackClick = studioViewModel::navigateBack,
                        onPlayItem = { studioViewModel.openAudioPlayerForItem(it, autoPlay = true) },
                        onSaveItem = studioViewModel::saveAudioToDevice,
                        onLoadScriptToEditor = studioViewModel::loadHistoryScriptIntoEditor,
                        onDeleteItem = studioViewModel::deleteHistoryItem,
                        onClearAll = studioViewModel::clearAllHistory
                    )
                }

                StudioDestination.TTS_SETUP -> {
                    SystemTtsSetupScreen(
                        modelStatus = uiState.modelStatus,
                        isGenerating = uiState.isGenerating,
                        progressPercent = uiState.progressPercent,
                        progressStatusText = uiState.progressStatusText,
                        onBackClick = studioViewModel::navigateBack,
                        onOpenPhoneTtsSettings = studioViewModel::openSystemTtsSettings,
                        onDownloadOfflineModel = studioViewModel::downloadOfflineModelOnly
                    )
                }
            }

            // Floating Feedback Banner
            AnimatedVisibility(
                visible = uiState.toastBannerMessage != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateGraySurface,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonSkyAccent, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.toastBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhiteText,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = studioViewModel::clearToastBanner,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = MutedSlateSubtext,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Fallback / Error Notice Dialog: ⚠️ Notice ... [ OK Got It ]
    uiState.noticeDialog?.let { dialogState ->
        StudioNoticeDialog(
            dialogState = dialogState,
            onDismiss = studioViewModel::dismissNoticeDialog
        )
    }
}

@Composable
private fun StudioBottomNavigationBar(
    currentDestination: StudioDestination,
    onSelectDestination: (StudioDestination) -> Unit
) {
    NavigationBar(
        containerColor = SlateGraySurface,
        contentColor = PureWhiteText,
        modifier = Modifier
            .border(width = 1.dp, color = SlateSurfaceElevated)
            .testTag("bottom_navigation_bar")
    ) {
        val isHomeSelected = currentDestination == StudioDestination.HOME ||
            currentDestination == StudioDestination.AUDIO_PLAYER
        NavigationBarItem(
            selected = isHomeSelected,
            onClick = { onSelectDestination(StudioDestination.HOME) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("🏠 Home", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonSkyAccent,
                selectedTextColor = NeonSkyAccent,
                indicatorColor = ElectricBluePrimary.copy(alpha = 0.25f),
                unselectedIconColor = MutedSlateSubtext,
                unselectedTextColor = MutedSlateSubtext
            ),
            modifier = Modifier.testTag("nav_home_tab")
        )

        NavigationBarItem(
            selected = currentDestination == StudioDestination.HISTORY,
            onClick = { onSelectDestination(StudioDestination.HISTORY) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "History"
                )
            },
            label = { Text("📂 History", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonSkyAccent,
                selectedTextColor = NeonSkyAccent,
                indicatorColor = ElectricBluePrimary.copy(alpha = 0.25f),
                unselectedIconColor = MutedSlateSubtext,
                unselectedTextColor = MutedSlateSubtext
            ),
            modifier = Modifier.testTag("nav_history_tab")
        )

        NavigationBarItem(
            selected = currentDestination == StudioDestination.TTS_SETUP,
            onClick = { onSelectDestination(StudioDestination.TTS_SETUP) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "TTS Setup"
                )
            },
            label = { Text("⚙️ TTS Setup", fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonSkyAccent,
                selectedTextColor = NeonSkyAccent,
                indicatorColor = ElectricBluePrimary.copy(alpha = 0.25f),
                unselectedIconColor = MutedSlateSubtext,
                unselectedTextColor = MutedSlateSubtext
            ),
            modifier = Modifier.testTag("nav_setup_tab")
        )
    }
}

@Composable
fun StudioNoticeDialog(
    dialogState: NoticeDialogState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateGraySurface,
        titleContentColor = PureWhiteText,
        textContentColor = PureWhiteText,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dialogState.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhiteText
                )
            }
        },
        text = {
            Column {
                Text(
                    text = dialogState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PureWhiteText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonSkyAccent,
                    contentColor = DeepDarkBackground
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("notice_dialog_ok_button")
            ) {
                Text(
                    text = "OK Got It",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        },
        modifier = Modifier.testTag("notice_dialog")
    )
}
