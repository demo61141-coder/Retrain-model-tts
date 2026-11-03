package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TtsHistoryEntity
import com.example.tts.BengaliTextChunker
import com.example.tts.NewsTemplate
import com.example.tts.NewsTemplates
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
fun HomeStudioScreen(
    uiState: StudioUiState,
    recentHistory: List<TtsHistoryEntity>,
    onSelectTemplate: (NewsTemplate) -> Unit,
    onScriptChange: (String) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onResetVoiceSettings: () -> Unit,
    onGenerateClick: () -> Unit,
    onPlayHistoryItem: (TtsHistoryEntity) -> Unit,
    onSaveHistoryItem: (TtsHistoryEntity) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var templateMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
            .testTag("home_studio_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Studio Header Bar: 🎙️ Reporter Hasan TTS   [⚙️ Settings]
        item {
            StudioTopHeader(
                isOfflineReady = uiState.modelStatus.isFullyReadyOffline,
                onOpenSettings = onOpenSettings
            )
        }

        // 2. Template Selector Dropdown: [Select Template: Reporter Hasan News ▼]
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(14.dp))
                        .clickable(enabled = !uiState.isGenerating) {
                            templateMenuExpanded = true
                        }
                        .testTag("template_dropdown_button"),
                    color = SlateGraySurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Select Template:",
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonSkyAccent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.selectedTemplate.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhiteText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Template Dropdown",
                            tint = NeonSkyAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = templateMenuExpanded,
                    onDismissRequest = { templateMenuExpanded = false },
                    modifier = Modifier
                        .background(SlateGraySurface)
                        .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(8.dp))
                ) {
                    NewsTemplates.templates.forEach { template ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = template.title,
                                    color = if (template.id == uiState.selectedTemplate.id) {
                                        NeonSkyAccent
                                    } else {
                                        PureWhiteText
                                    },
                                    fontWeight = if (template.id == uiState.selectedTemplate.id) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            },
                            onClick = {
                                templateMenuExpanded = false
                                onSelectTemplate(template)
                            }
                        )
                    }
                }
            }
        }

        // 3. Script Input Card with Word Count & Punctuation Chunk Preview
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
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📝 নিউজ স্ক্রিপ্ট এডিটর",
                            style = MaterialTheme.typography.labelLarge,
                            color = NeonSkyAccent
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        onScriptChange(clip)
                                    }
                                },
                                enabled = !uiState.isGenerating,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("paste_script_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste script",
                                    tint = NeonSkyAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste", color = NeonSkyAccent, fontSize = 12.sp)
                            }

                            if (uiState.scriptText.isNotEmpty()) {
                                TextButton(
                                    onClick = { onScriptChange("") },
                                    enabled = !uiState.isGenerating,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("clear_script_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear script",
                                        tint = MutedSlateSubtext,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", color = MutedSlateSubtext, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.scriptText,
                        onValueChange = onScriptChange,
                        enabled = !uiState.isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .testTag("script_input_field"),
                        placeholder = {
                            Text(
                                text = "এখানে আপনার নিউজ বা স্ক্রিপ্ট পেস্ট করুন...\n(বাংলা অথবা ইংরেজি টেক্সট)",
                                color = MutedSlateSubtext
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonSkyAccent,
                            unfocusedBorderColor = SlateSurfaceElevated,
                            focusedTextColor = PureWhiteText,
                            unfocusedTextColor = PureWhiteText,
                            cursorColor = NeonSkyAccent,
                            focusedContainerColor = DeepDarkBackground.copy(alpha = 0.55f),
                            unfocusedContainerColor = DeepDarkBackground.copy(alpha = 0.55f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val isOverLimit = uiState.wordCount > BengaliTextChunker.MAX_WORD_LIMIT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Word Count: ${uiState.wordCount}/${BengaliTextChunker.MAX_WORD_LIMIT}",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOverLimit) ErrorCrimson else MutedSlateSubtext,
                            modifier = Modifier.testTag("word_count_text")
                        )

                        Text(
                            text = "Chunks: ${uiState.previewChunksCount} (। ? ! . , ')",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                            color = NeonSkyAccent,
                            modifier = Modifier.testTag("chunk_count_text")
                        )
                    }
                }
            }
        }

        // 4. 🎚️ Voice Settings Card (Speed & Pitch)
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
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = NeonSkyAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🎚️ Voice Settings:",
                                style = MaterialTheme.typography.titleMedium,
                                color = PureWhiteText
                            )
                        }

                        if (uiState.speed != 1.0f || uiState.pitch != 1.0f) {
                            TextButton(
                                onClick = onResetVoiceSettings,
                                enabled = !uiState.isGenerating,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset speed and pitch",
                                    tint = NeonSkyAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", color = NeonSkyAccent, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Speed Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Speed:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhiteText
                        )
                        Text(
                            text = "%.2fx".format(java.util.Locale.US, uiState.speed),
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonSkyAccent
                        )
                    }
                    Slider(
                        value = uiState.speed,
                        onValueChange = onSpeedChange,
                        enabled = !uiState.isGenerating,
                        valueRange = 0.5f..1.5f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonSkyAccent,
                            activeTrackColor = ElectricBluePrimary,
                            inactiveTrackColor = DeepDarkBackground
                        ),
                        modifier = Modifier.testTag("speed_slider")
                    )

                    // Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pitch:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PureWhiteText
                        )
                        Text(
                            text = uiState.pitchLabel,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonSkyAccent
                        )
                    }
                    Slider(
                        value = uiState.pitch,
                        onValueChange = onPitchChange,
                        enabled = !uiState.isGenerating,
                        valueRange = 0.8f..1.2f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonSkyAccent,
                            activeTrackColor = ElectricBluePrimary,
                            inactiveTrackColor = DeepDarkBackground
                        ),
                        modifier = Modifier.testTag("pitch_slider")
                    )
                }
            }
        }

        // 5. [ GENERATE VOICE 🔊 ] Button + Real-time Progress Indicator
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onGenerateClick,
                    enabled = !uiState.isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("generate_voice_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonSkyAccent,
                        contentColor = DeepDarkBackground,
                        disabledContainerColor = SlateSurfaceElevated,
                        disabledContentColor = MutedSlateSubtext
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (uiState.isGenerating) {
                            "⏳ GENERATING OFFLINE AUDIO... (${uiState.progressPercent}%)"
                        } else {
                            "GENERATE VOICE 🔊"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Smooth Progress State Card when generating
                if (uiState.isGenerating) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonSkyAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .testTag("generation_progress_card"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏳ Generating offline audio... (Please wait)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhiteText
                                )
                                Text(
                                    text = "${uiState.progressPercent}%",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NeonSkyAccent,
                                    modifier = Modifier.testTag("progress_percent_text")
                                )
                            }

                            if (uiState.progressStatusText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uiState.progressStatusText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSlateSubtext
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { (uiState.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = NeonSkyAccent,
                                trackColor = DeepDarkBackground
                            )
                        }
                    }
                }
            }
        }

        // 6. 📁 Recent History Section on Main Dashboard
        item {
            HorizontalDivider(color = SlateSurfaceElevated, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📁 Recent History:",
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhiteText
                )
                if (recentHistory.isNotEmpty()) {
                    TextButton(
                        onClick = onOpenAllHistory,
                        modifier = Modifier.testTag("view_all_history_button")
                    ) {
                        Text(
                            text = "View All (${recentHistory.size})",
                            color = NeonSkyAccent,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (recentHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = NeonSkyAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এখনো কোনো অডিও জেনারেট করা হয়নি",
                            style = MaterialTheme.typography.titleMedium,
                            color = PureWhiteText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "উপরে বাংলা নিউজ বা স্ক্রিপ্ট লিখে 'GENERATE VOICE 🔊' বাটনে ট্যাপ করুন। প্রথমবার রান করলে অফলাইন ভয়েস মডেল (model_fullmeta.onnx) স্বয়ংক্রিয়ভাবে ডাউনলোড হবে।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlateSubtext
                        )
                    }
                }
            }
        } else {
            items(
                items = recentHistory.take(5),
                key = { it.id }
            ) { item ->
                RecentHistoryRowCard(
                    item = item,
                    onPlayClick = { onPlayHistoryItem(item) },
                    onDownloadClick = { onSaveHistoryItem(item) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StudioTopHeader(
    isOfflineReady: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            ElectricBluePrimary.copy(alpha = 0.28f),
                            SlateGraySurface
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ElectricBluePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Reporter Hasan TTS Microphone",
                            tint = PureWhiteText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🎙️ Reporter Hasan TTS",
                            style = MaterialTheme.typography.titleLarge,
                            color = PureWhiteText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isOfflineReady) VibrantEmeraldReady else WarningAmber
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOfflineReady) {
                                    "v1.9.4-fixed • Offline Ready"
                                } else {
                                    "v1.9.4-fixed • First-Run Download Needed"
                                },
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = MutedSlateSubtext
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NeonSkyAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Settings",
                        color = PureWhiteText,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun RecentHistoryRowCard(
    item: TtsHistoryEntity,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(12.dp))
            .clickable { onPlayClick() }
            .testTag("recent_history_item_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "• ${item.fileName}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = PureWhiteText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.scriptText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSlateSubtext,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBluePrimary,
                        contentColor = PureWhiteText
                    ),
                    modifier = Modifier.testTag("play_history_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play ${item.fileName}",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalIconButton(
                    onClick = onDownloadClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DeepDarkBackground,
                        contentColor = NeonSkyAccent
                    ),
                    modifier = Modifier.testTag("save_history_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Save ${item.fileName} to Device",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
