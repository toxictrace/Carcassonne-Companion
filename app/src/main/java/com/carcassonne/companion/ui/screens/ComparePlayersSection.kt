package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.carcassonne.companion.R
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.PlayerStats

@Composable
fun ComparePlayersSection(
    allStats: List<PlayerStats>,
    selectedSlots: List<Int?>,
    slotColors: List<Color>,
    allGamePlayers: List<GamePlayerEntity>,
    sectionMask: Int = 0b0011111,
    onSectionsChange: (Int) -> Unit = {},
    onSlotClick: (slotIdx: Int) -> Unit
) {
    val slotStats = selectedSlots.map { id -> allStats.find { it.player.id == id } }
    val activePlayers = slotStats.filterNotNull()
    val activeSlotIndices = selectedSlots.indices.filter { selectedSlots[it] != null }
    val colors = activePlayers.indices.map { ai -> slotColors[activeSlotIndices.getOrElse(ai) { ai } % 3] }

    ComparePlayerSlots(
        allStats = allStats,
        selectedSlots = selectedSlots,
        slotColors = slotColors,
        onSlotClick = onSlotClick
    )

    if (activePlayers.size < 2) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Text(stringResource(R.string.select_2_compare), color = CarcText3, fontSize = 13.sp)
        }
        return
    }

    Spacer(Modifier.height(12.dp))

    // Section visibility state is shared with the extracted picker dialog.
    val sectionLabels = listOf(
        stringResource(R.string.section_label_play_style),
        stringResource(R.string.section_label_results),
        stringResource(R.string.section_label_score_range),
        stringResource(R.string.section_label_together),
        stringResource(R.string.section_label_trend),
        stringResource(R.string.section_label_score_struct),
        stringResource(R.string.section_label_metrics)
    )
    val sections = remember(sectionMask) {
        mutableStateListOf(*sectionLabels.mapIndexed { index, label ->
            CompareSection(label, sectionMask and (1 shl index) != 0)
        }.toTypedArray())
    }
    var showSectionPicker by remember { mutableStateOf(false) }
    if (showSectionPicker) {
        CompareSectionPickerDialog(
            sections = sections,
            onSectionsChange = onSectionsChange,
            onDismiss = { showSectionPicker = false }
        )
    }

    // Header row with gear button
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.compare_players), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
            modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CarcCard)
                .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                .clickable { showSectionPicker = true },
            contentAlignment = Alignment.Center
        ) { Text("⚙️", fontSize = 14.sp) }
    }

    Spacer(Modifier.height(10.dp))

    // Player legend (shown always)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        activePlayers.forEachIndexed { i, ps ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                Spacer(Modifier.width(5.dp))
                Text(ps.player.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors[i])
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    // ── 0: PLAY STYLE ──────────────────────────────────────────────────────
    if (sections[0].enabled) {
        SectionLabel(stringResource(R.string.section_play_style))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp)) {
                RadarChart(players = activePlayers, colors = colors,
                    modifier = Modifier.fillMaxWidth().height(280.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 1: RESULTS ─────────────────────────────────────────────────────────
    if (sections[1].enabled) {
        CompareResultsSection(
            activePlayers = activePlayers,
            allGamePlayers = allGamePlayers,
            colors = colors
        )
    }

    // ── 2: SCORE RANGE ─────────────────────────────────────────────────────
    if (sections[2].enabled) {
        CompareScoreRangeSection(
            activePlayers = activePlayers,
            colors = colors
        )
    }

    // ── 3: TOGETHER ────────────────────────────────────────────────────────
    if (sections[3].enabled && activePlayers.size >= 2) {
        CompareTogetherSection(
            activePlayers = activePlayers,
            allGamePlayers = allGamePlayers,
            colors = colors
        )
    }

    // ── 4: TREND ───────────────────────────────────────────────────────────
    if (sections[4].enabled) {
        CompareTrendSection(activePlayers = activePlayers, colors = colors)
    }

    // ── 5: SCORE STRUCTURE ─────────────────────────────────────────────────
    if (sections[5].enabled) {
        CompareScoreStructureSection(activePlayers = activePlayers, colors = colors)
    }

    // ── 6: METRICS ──────────────────────────────────────────────────────────
    if (sections[6].enabled) {
        CompareMetricsSection(activePlayers = activePlayers, colors = colors)
    }
}
