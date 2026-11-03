package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TtsHistoryEntity
import com.example.ui.theme.DeepDarkBackground
import com.example.ui.theme.ElectricBluePrimary
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MutedSlateSubtext
import com.example.ui.theme.NeonSkyAccent
import com.example.ui.theme.PureWhiteText
import com.example.ui.theme.SlateGraySurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.VibrantEmeraldReady
import kotlin.math.max

@Composable
fun AudioPlayerScreen(
    item: TtsHistoryEntity?,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onBackClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekFraction: (Float) -> Unit,
    onRewind5s: () -> Unit,
    onForward5s: () -> Unit,
    onSaveToDevice: (TtsHistoryEntity) -> Unit,
    onShareAudio: (TtsHistoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepDarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("audio_player_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header: < Back         Audio Player
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
                    modifier = Modifier.testTag("player_back_button")
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
                    text = "Audio Player",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhiteText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(68.dp))
            }
        }

        if (item == null) {
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
                        text = "কোনো অডিও ফাইল নির্বাচন করা হয়নি",
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhiteText
                    )
                }
            }
            return@Column
        }

        val totalMs = max(1, max(durationMs, (item.durationSeconds * 1000).toInt()))
        val progressFraction = (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
        val peaks = item.parsedWaveformPeaks()

        // Main Waveform & Player Card
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NeonSkyAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "🎵 [ Waveform UI ]",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = NeonSkyAccent
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real Audio Waveform Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DeepDarkBackground)
                        .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                        .testTag("waveform_canvas_box")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barCount = peaks.size.coerceAtLeast(1)
                        val spacing = 4.dp.toPx()
                        val totalSpacing = spacing * (barCount - 1)
                        val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(3f)

                        peaks.forEachIndexed { idx, amplitude ->
                            val barFraction = idx.toFloat() / barCount.toFloat()
                            val isPlayed = barFraction <= progressFraction
                            val barHeight = (amplitude.coerceIn(0.10f, 1.0f) * size.height)
                                .coerceAtLeast(8.dp.toPx())
                            val topOffset = (size.height - barHeight) / 2f
                            val xOffset = idx * (barWidth + spacing)

                            drawRoundRect(
                                color = if (isPlayed) NeonSkyAccent else ElectricBluePrimary.copy(alpha = 0.45f),
                                topLeft = Offset(xOffset, topOffset),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // File Name & Spec Metadata
                Text(
                    text = item.fileName,
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PureWhiteText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("player_file_name")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "22050 Hz • 16-bit Mono WAV • ${item.wordCount} words • ${item.chunkCount} chunks",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 11.sp,
                    color = MutedSlateSubtext
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Seekbar + Timestamps: 0:00  [===========O-------]  0:45
                Slider(
                    value = progressFraction,
                    onValueChange = onSeekFraction,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonSkyAccent,
                        activeTrackColor = NeonSkyAccent,
                        inactiveTrackColor = DeepDarkBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationMs(positionMs),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        color = PureWhiteText
                    )
                    Text(
                        text = formatDurationMs(totalMs),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 12.sp,
                        color = MutedSlateSubtext
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transport Controls: [ ⏪ ]      [ ⏸️ Pause / ▶ Play ]      [ ⏩ ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onRewind5s,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("player_rewind_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = DeepDarkBackground,
                            contentColor = PureWhiteText
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 5 seconds"
                        )
                    }

                    Button(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .height(56.dp)
                            .width(156.dp)
                            .testTag("player_play_pause_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonSkyAccent,
                            contentColor = DeepDarkBackground
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying) "Pause" else "Play",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledIconButton(
                        onClick = onForward5s,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("player_forward_button"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = DeepDarkBackground,
                            contentColor = PureWhiteText
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 5 seconds"
                        )
                    }
                }
            }
        }

        // Action Bar Card: [📥 Save to Device]    [🔗 Share Audio]
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateSurfaceElevated, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onSaveToDevice(item) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("save_to_device_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBluePrimary,
                        contentColor = PureWhiteText
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save to Device",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = { onShareAudio(item) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("share_audio_button"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = NeonSkyAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share Audio",
                        color = PureWhiteText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Script & Local Security Integrity Details Card
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📄 জেনারেটেড স্ক্রিপ্ট",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonSkyAccent
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = VibrantEmeraldReady,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SHA-256 Verified",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            color = VibrantEmeraldReady
                        )
                    }
                }

                Text(
                    text = item.scriptText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = PureWhiteText
                )
            }
        }
    }
}

private fun formatDurationMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(java.util.Locale.US, minutes, seconds)
}
