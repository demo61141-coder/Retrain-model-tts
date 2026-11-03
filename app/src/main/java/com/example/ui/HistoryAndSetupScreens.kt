package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TtsHistoryEntity
import com.example.tts.ModelFileStatus
import com.example.tts.SherpaOfflineTtsEngine
import com.example.ui.theme.DeepDarkBackground
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSlateSubtext
import com.example.ui.theme.NeonSkyAccent
import com.example.ui.theme.PureWhiteText
import com.example.ui.theme.SlateGraySurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.VibrantEmeraldReady
import com.example.ui.theme.WarningAmber

@Composable
fun SystemTtsSetupScreen(
    modelStatus: ModelFileStatus,
    isGenerating: Boolean,
    progressPercent: Int,
    progressStatusText: String,
    onBackClick: () -> Unit,
    onOpenPhoneTtsSettings: () -> Unit,
    onDownloadOfflineModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("system_tts_setup_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: < Back          System TTS Setup
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("setup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonSkyAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back",
                        color = NeonSkyAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "System TTS Setup",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhiteText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(68.dp))
            }
        }

        // Guide Card: ℹ️ Make Reporter Hasan your Default Engine
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonSkyAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ℹ️ Make Reporter Hasan your Default Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhiteText
                    )
                }

                SetupStepRow(
                    stepNumber = "Step 1",
                    stepTitle = "Open Phone Settings",
                    stepSubtitle = "ফোনের মূল Settings অ্যাপে প্রবেশ করুন"
                )
                SetupStepRow(
                    stepNumber = "Step 2",
                    stepTitle = "Go to Accessibility / System Languages",
                    stepSubtitle = "System > Languages & input অথবা Accessibility মেনুতে যান"
                )
                SetupStepRow(
                    stepNumber = "Step 3",
                    stepTitle = "Select Text-to-Speech Output",
                    stepSubtitle = "Text-to-speech output অপশনটি নির্বাচন করুন"
                )
                SetupStepRow(
                    stepNumber = "Step 4",
                    stepTitle = "Choose \"Reporter Hasan TTS\"",
                    stepSubtitle = "ডিফল্ট ভয়েস ও অফলাইন স্টুডিও হিসেবে ব্যবহার করুন"
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onOpenPhoneTtsSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("open_tts_settings_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonSkyAccent,
                        contentColor = DeepDarkBackground
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsApplications,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "OPEN PHONE TTS SETTINGS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Status Bar: Status: 🟢 Ready (Offline Mode Active)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeepDarkBackground)
                        .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("tts_setup_status_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (modelStatus.isFullyReadyOffline) VibrantEmeraldReady else WarningAmber
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (modelStatus.isFullyReadyOffline) {
                            "Status: 🟢 Ready (Offline Mode Active)"
                        } else {
                            "Status: 🟡 Ready to Download on First Generate (Offline Mode)"
                        },
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PureWhiteText
                    )
                }
            }
        }

        // Offline Model Verification & Pre-Download Card (v1.9.4-fixed metadata-fixed retrain model)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "📦 অফলাইন ভয়েস মডেল (v1.9.4-fixed Retrain)",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonSkyAccent
                )

                Text(
                    text = "• Model: filesDir/model_fullmeta.onnx (${modelStatus.modelActualBytes} / ${SherpaOfflineTtsEngine.EXPECTED_MODEL_BYTES} bytes)\n" +
                        "• Tokens: filesDir/tokens_sherpa.txt (${modelStatus.tokensActualBytes} / ${SherpaOfflineTtsEngine.EXPECTED_TOKENS_BYTES} bytes)\n" +
                        "• Engine: sherpa-onnx 2.0.1 VITS (CPU 2 threads, 22050 Hz, noiseScale 0.667, noiseScaleW 0.8, lengthScale 1.0)\n" +
                        "• Security: Room SQLite Local DB + SHA-256 WAV Integrity Check",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 12.sp,
                    color = MutedSlateSubtext
                )

                if (isGenerating) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = progressStatusText.ifBlank { "⏳ প্রসেসিং চলছে... ($progressPercent%)" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = PureWhiteText
                    )
                    LinearProgressIndicator(
                        progress = { (progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonSkyAccent,
                        trackColor = DeepDarkBackground
                    )
                } else if (!modelStatus.isFullyReadyOffline) {
                    OutlinedButton(
                        onClick = onDownloadOfflineModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("predownload_model_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = NeonSkyAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "এখনই অফলাইন মডেল ডাউনলোড ও ভেরিফাই করুন (108.9 MB)",
                            color = PureWhiteText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VibrantEmeraldReady,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "মডেল ও টোকেন ফাইল শতভাগ ভেরিফাইড — সম্পূর্ণ ইন্টারনেট ছাড়াই চলবে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VibrantEmeraldReady
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    stepNumber: String,
    stepTitle: String,
    stepSubtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DeepDarkBackground.copy(alpha = 0.65f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ElectricBluePrimary)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = stepNumber,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = PureWhiteText
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stepTitle,
                style = MaterialTheme.typography.labelLarge,
                color = PureWhiteText
            )
            Text(
                text = stepSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlateSubtext,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun LocalHistoryScreen(
    historyItems: List<TtsHistoryEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onPlayItem: (TtsHistoryEntity) -> Unit,
    onSaveItem: (TtsHistoryEntity) -> Unit,
    onLoadScriptToEditor: (TtsHistoryEntity) -> Unit,
    onDeleteItem: (TtsHistoryEntity) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val filteredItems = if (searchQuery.isBlank()) {
        historyItems
    } else {
        val q = searchQuery.trim().lowercase()
        historyItems.filter {
            it.fileName.lowercase().contains(q) ||
                it.scriptText.lowercase().contains(q) ||
                it.templateName.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
            .testTag("local_history_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = NeonSkyAccent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "📂 Local Audio History (${historyItems.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = PureWhiteText
                            )
                        }

                        if (historyItems.isNotEmpty()) {
                            TextButton(
                                onClick = onClearAll,
                                modifier = Modifier.testTag("clear_all_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear all history",
                                    tint = ErrorCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All", color = ErrorCrimson, fontSize = 12.sp)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = VibrantEmeraldReady,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সকল অডিও ও স্ক্রিপ্ট সম্পূর্ণ অফলাইনে ফোনের প্রাইভেট Room ডাটাবেসে সুরক্ষিত থাকে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlateSubtext,
                            fontSize = 12.sp
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_search_field"),
                        placeholder = {
                            Text("ফাইলের নাম বা বাংলা স্ক্রিপ্ট দিয়ে খুঁজুন...", color = MutedSlateSubtext)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = NeonSkyAccent
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonSkyAccent,
                            unfocusedBorderColor = SlateSurfaceElevated,
                            focusedTextColor = PureWhiteText,
                            unfocusedTextColor = PureWhiteText
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "কোনো সংরক্ষিত অডিও পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhiteText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "হোম স্ক্রিন থেকে নতুন বাংলা ভয়েস জেনারেট করলে এখানে স্বয়ংক্রিয়ভাবে সংরক্ষিত হবে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlateSubtext,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredItems,
                key = { it.id }
            ) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(14.dp))
                        .testTag("history_card_${item.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.fileName,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NeonSkyAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "%.1fs • %d KB".format(
                                    java.util.Locale.US,
                                    item.durationSeconds,
                                    (item.fileSizeBytes / 1024L).coerceAtLeast(1L)
                                ),
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = MutedSlateSubtext
                            )
                        }

                        Text(
                            text = item.scriptText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhiteText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onPlayItem(item) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricBluePrimary,
                                        contentColor = PureWhiteText
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onSaveItem(item) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Save to device",
                                        tint = NeonSkyAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", color = PureWhiteText, fontSize = 12.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalIconButton(
                                    onClick = { onLoadScriptToEditor(item) },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = DeepDarkBackground,
                                        contentColor = NeonSkyAccent
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = "Load script into editor",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                FilledTonalIconButton(
                                    onClick = { onDeleteItem(item) },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = DeepDarkBackground,
                                        contentColor = ErrorCrimson
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete item",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
