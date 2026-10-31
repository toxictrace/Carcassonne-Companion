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
        SectionLabel(stringResource(R.string.section_score_range))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                val globalMax = activePlayers.maxOf { it.maxScore }.toFloat().takeIf { it > 0f } ?: 1f
                activePlayers.forEachIndexed { i, ps ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 11.sp, color = colors[i],
                            modifier = Modifier.width(70.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f).height(18.dp)) {
                            val maxFrac = (ps.maxScore / globalMax).coerceIn(0f, 1f)
                            val avgFrac = (ps.avgScore / globalMax).coerceIn(0f, 1f)
                            // Background range bar (0 → max)
                            Box(Modifier.fillMaxHeight().fillMaxWidth(maxFrac)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors[i].copy(alpha = 0.2f)))
                            // Avg bar (0 → avg), brighter
                            Box(Modifier.fillMaxHeight().fillMaxWidth(avgFrac)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors[i].copy(alpha = 0.7f)))
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(70.dp)) {
                            Text("▲ ${ps.maxScore}", fontSize = 10.sp, color = CarcGreen, fontWeight = FontWeight.Bold)
                            Text("● ${ps.avgScore.toInt()}", fontSize = 10.sp, color = colors[i])
                            Text("▼ ${ps.minScore}", fontSize = 10.sp, color = CarcText3)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
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

    // ── 6: METRICS ─────────────────────────────────────────────────────────
    if (sections[6].enabled) {
        SectionLabel(stringResource(R.string.section_metrics))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp)) {
                val metrics = listOf(
                    Triple(stringResource(R.string.metric_urbanization),   activePlayers.map { it.urbanizationIndex }, stringResource(R.string.metric_urbanization_desc)),
                    Triple(stringResource(R.string.metric_road_aggr),     activePlayers.map { it.roadAggrIndex },    stringResource(R.string.metric_road_aggr_desc)),
                    Triple(stringResource(R.string.metric_monastery),      activePlayers.map { it.monasteryIndex },   stringResource(R.string.metric_monastery_desc)),
                    Triple(stringResource(R.string.metric_farm_dom), activePlayers.map { it.farmDomIndex },     stringResource(R.string.metric_farm_dom_desc)),
                    Triple(stringResource(R.string.metric_stability),      activePlayers.map { it.stabilityIndex },   stringResource(R.string.metric_stability_desc))
                )
                metrics.forEachIndexed { mi, (label, values, hint) ->
                    if (mi > 0) HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 6.dp))
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
                    Text(hint, fontSize = 10.sp, color = CarcText3)
                    Spacer(Modifier.height(6.dp))
                    val maxV = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
                    activePlayers.forEachIndexed { pi, ps ->
                        val v = values[pi]; val isWinner = v == maxV
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(RoundedCornerShape(2.dp)).background(colors[pi]))
                            Spacer(Modifier.width(5.dp))
                            Text(ps.player.name, fontSize = 10.sp, color = colors[pi],
                                modifier = Modifier.width(52.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(CarcBg3)) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(v / maxV)
                                    .background(colors[pi].copy(alpha = if (isWinner) 1f else 0.55f)))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("%.0f%%".format(v * 100), fontSize = 11.sp,
                                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                                color = if (isWinner) colors[pi] else CarcText3,
                                modifier = Modifier.width(40.dp), textAlign = TextAlign.End,
                                maxLines = 1)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}
