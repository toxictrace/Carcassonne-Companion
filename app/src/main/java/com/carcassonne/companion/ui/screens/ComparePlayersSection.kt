package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
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

    // ── Slot cards ─────────────────────────────────────────────────────────
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedSlots.forEachIndexed { slotIdx, selectedId ->
                val ps = allStats.find { it.player.id == selectedId }
                val slotColor = if (ps != null) slotColors[slotIdx % 3] else CarcBorder
                Column(
                    modifier = Modifier
                        .weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarcBg2)
                        .border(1.5.dp, slotColor, RoundedCornerShape(10.dp))
                        .clickable { onSlotClick(slotIdx) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (ps != null) {
                        PlayerAvatar(ps.player.name, ps.player.meepleColor,
                            avatarPath = ps.player.avatarPath, size = 36.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(ps.player.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = slotColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center)
                        Text(stringResource(R.string.avg_score_label, ps.avgScore), fontSize = 10.sp, color = CarcText3)
                    } else {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(50.dp))
                            .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(50.dp)),
                            contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 18.sp, color = CarcText3)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.add_btn), fontSize = 11.sp, color = CarcText3)
                        Text(stringResource(R.string.player_label), fontSize = 10.sp, color = CarcText3)
                    }
                }
            }
        }
    }

    if (activePlayers.size < 2) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Text(stringResource(R.string.select_2_compare), color = CarcText3, fontSize = 13.sp)
        }
        return
    }

    Spacer(Modifier.height(12.dp))

    // ── Section selector ───────────────────────────────────────────────────
    // sections: index -> (label, enabled)
    data class CompareSection(val label: String, var enabled: Boolean)
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
        mutableStateListOf(*sectionLabels.mapIndexed { i, label ->
            CompareSection(label, sectionMask and (1 shl i) != 0)
        }.toTypedArray())
    }
    var showSectionPicker by remember { mutableStateOf(false) }
    if (showSectionPicker) {
        AlertDialog(
            onDismissRequest = { showSectionPicker = false },
            containerColor = CarcCard2,
            title = { Text(stringResource(R.string.show_comparisons), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    sections.forEachIndexed { i, sec ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    sections[i] = sec.copy(enabled = !sec.enabled)
                                    val newMask = sections.foldIndexed(0) { idx, acc, s ->
                                        if (s.enabled) acc or (1 shl idx) else acc }
                                    onSectionsChange(newMask)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sec.enabled,
                                onCheckedChange = { v ->
                                    sections[i] = sec.copy(enabled = v)
                                    val newMask = sections.foldIndexed(0) { idx, acc, s ->
                                        if (s.enabled) acc or (1 shl idx) else acc }
                                    onSectionsChange(newMask)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CarcGreen,
                                    uncheckedColor = CarcBorder
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(sec.label, fontSize = 14.sp, color = CarcText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSectionPicker = false }) {
                    Text(stringResource(R.string.done_btn), color = CarcGreen, fontWeight = FontWeight.Bold)
                }
            }
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
        SectionLabel(stringResource(R.string.section_results))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                activePlayers.forEachIndexed { i, ps ->
                    val losses = ps.gamesPlayed - ps.wins
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 12.sp, color = colors[i],
                            modifier = Modifier.width(70.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        // Win bar
                        val winFrac = ps.winRate.coerceIn(0f, 1f)
                        Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(4.dp))
                            .background(CarcBg3)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(winFrac)
                                .background(colors[i]))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.wins_losses, ps.wins, losses),
                            fontSize = 11.sp, color = CarcText2,
                            modifier = Modifier.widthIn(min = 70.dp), textAlign = TextAlign.End)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 2.dp))
                Spacer(Modifier.height(6.dp))
                // Win rate numbers side by side
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    activePlayers.forEachIndexed { i, ps ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(ps.winRate * 100).toInt()}%",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors[i])
                            Text(stringResource(R.string.win_rate), fontSize = 10.sp, color = CarcText3)
                        }
                    }
                }
                // Placement breakdown
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.placements), fontSize = 11.sp, color = CarcText3)
                Spacer(Modifier.height(6.dp))
                val placeColors = listOf(CarcYellow, Color(0xFF9CA3AF), Color(0xFFB45309), Color(0xFF4B5563))
                val placeIcons  = listOf("🥇", "🥈", "🥉", "4️⃣")
                // Header row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(14.dp + 6.dp + 70.dp + 6.dp))
                    placeIcons.forEach { icon ->
                        Text(icon, fontSize = 13.sp, modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(4.dp))
                activePlayers.forEachIndexed { i, ps ->
                    val playerGames = allGamePlayers.filter { it.playerId == ps.player.id }
                    val byPlace = (1..4).map { place ->
                        playerGames.count { it.placement == place }
                    }
                    val total = playerGames.size.takeIf { it > 0 } ?: 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 11.sp, color = colors[i],
                            modifier = Modifier.width(70.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        byPlace.forEachIndexed { pi, cnt ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // bar
                                val frac = cnt.toFloat() / total
                                Box(Modifier.width(28.dp).height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CarcBg3)) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac)
                                        .background(placeColors[pi]))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("$cnt", fontSize = 13.sp,
                                    fontWeight = if (cnt == byPlace.max()) FontWeight.Bold else FontWeight.Normal,
                                    color = if (cnt > 0) placeColors[pi] else CarcText3,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
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
        SectionLabel(stringResource(R.string.section_together))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                // Games together matrix — all pairs
                val pairs = mutableListOf<Triple<PlayerStats, PlayerStats, Int>>()
                for (a in activePlayers.indices) {
                    for (b in a+1 until activePlayers.size) {
                        val pa = activePlayers[a]; val pb = activePlayers[b]
                        val gamesA = allGamePlayers.filter { it.playerId == pa.player.id }
                            .map { it.gameId }.toSet()
                        val gamesB = allGamePlayers.filter { it.playerId == pb.player.id }
                            .map { it.gameId }.toSet()
                        val together = gamesA.intersect(gamesB).size
                        pairs.add(Triple(pa, pb, together))
                    }
                }
                pairs.forEach { (pa, pb, count) ->
                    val idxA = activePlayers.indexOf(pa)
                    val idxB = activePlayers.indexOf(pb)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.width(8.dp))
                        Text(pa.player.name, fontSize = 12.sp, color = colors[idxA],
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.widthIn(max = 90.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("  &  ", fontSize = 11.sp, color = CarcText3)
                        Text(pb.player.name, fontSize = 12.sp, color = colors[idxB],
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.widthIn(max = 90.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(1f))
                        Text("$count", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CarcYellow)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.games_label), fontSize = 11.sp, color = CarcText3)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                // Head-to-head wins (who won more when played together)
                if (activePlayers.size == 2) {
                    val pa = activePlayers[0]; val pb = activePlayers[1]
                    val sharedGames = allGamePlayers.filter { it.playerId == pa.player.id }
                        .map { it.gameId }.toSet()
                        .intersect(allGamePlayers.filter { it.playerId == pb.player.id }
                            .map { it.gameId }.toSet())
                    val winsA = allGamePlayers.filter { it.playerId == pa.player.id
                        && it.gameId in sharedGames && it.placement == 1 }.size
                    val winsB = allGamePlayers.filter { it.playerId == pb.player.id
                        && it.gameId in sharedGames && it.placement == 1 }.size
                    if (sharedGames.isNotEmpty()) {
                        HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 6.dp))
                        Text(stringResource(R.string.head_to_head), fontSize = 11.sp, color = CarcText3)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$winsA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors[0])
                                Text(pa.player.name, fontSize = 11.sp, color = colors[0],
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.vs), fontSize = 16.sp, color = CarcText3,
                                    modifier = Modifier.padding(top = 8.dp))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$winsB", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors[1])
                                Text(pb.player.name, fontSize = 11.sp, color = colors[1],
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 4: TREND ───────────────────────────────────────────────────────────
    if (sections[4].enabled) {
        SectionLabel(stringResource(R.string.section_trend))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp)) {
                val trendTextPaint = remember {
                    android.graphics.Paint().apply {
                        isAntiAlias = true; textSize = 34f; textAlign = android.graphics.Paint.Align.CENTER
                        style = android.graphics.Paint.Style.FILL
                    }
                }
                val trendStrokePaint = remember {
                    android.graphics.Paint().apply {
                        isAntiAlias = true; textSize = 34f; textAlign = android.graphics.Paint.Align.CENTER
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 5f
                        color = android.graphics.Color.argb(220, 10, 26, 10)
                        strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                }
                val isDarkTrend = LocalCarcColors.current.isDark
                // Mini line chart with score labels
                val allScoresTrend = activePlayers.flatMap { it.recentScores }
                if (allScoresTrend.isNotEmpty()) {
                    val globalMinT = allScoresTrend.min().toFloat()
                    val globalMaxT = allScoresTrend.max().toFloat()
                    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        val range = (globalMaxT - globalMinT).takeIf { it > 0f } ?: 1f
                        val w = size.width; val h = size.height
                        val padTop = 36f; val padBottom = 8f
                        val chartH = h - padTop - padBottom
                        val n = 5
                        val stepX = w / (n - 1).toFloat()
                        fun scoreY(sc: Int) = padTop + chartH - (sc - globalMinT) / range * chartH

                        // Grid
                        val gridColor = if (isDarkTrend) Color.White.copy(alpha = 0.07f)
                                        else Color(0xFF336633).copy(alpha = 0.15f)
                        repeat(4) { ri ->
                            val y = padTop + chartH * ri / 3f
                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        }
                        // Lines
                        activePlayers.forEachIndexed { pi, ps ->
                            val scores = ps.recentScores.take(n).reversed()
                            if (scores.size < 2) return@forEachIndexed
                            val linePath = Path()
                            scores.forEachIndexed { si, sc ->
                                val x = si * stepX; val y = scoreY(sc)
                                if (si == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                            }
                            drawPath(linePath, colors[pi], style = Stroke(width = 2.5f))
                        }
                        // Dots + score labels (drawn after lines so labels are on top)
                        activePlayers.forEachIndexed { pi, ps ->
                            val scores = ps.recentScores.take(n).reversed()
                            scores.forEachIndexed { si, sc ->
                                val x = si * stepX; val y = scoreY(sc)
                                drawCircle(colors[pi], radius = 5f, center = Offset(x, y))
                                drawCircle(Color(0xFF0D1F0D), radius = 2.5f, center = Offset(x, y))
                            }
                        }
                        // Score text labels — smart placement to avoid overlap
                        drawIntoCanvas { cv ->
                            val minGap = 40f
                            val lblX = mutableListOf<Float>()
                            val lblY = mutableListOf<Float>()
                            val lblText = mutableListOf<String>()
                            val lblColor = mutableListOf<Int>()
                            activePlayers.forEachIndexed { pi, ps ->
                                val scores = ps.recentScores.take(n).reversed()
                                val colorInt = android.graphics.Color.argb(
                                    255,
                                    (colors[pi].red * 255).toInt(),
                                    (colors[pi].green * 255).toInt(),
                                    (colors[pi].blue * 255).toInt()
                                )
                                scores.forEachIndexed { si, sc ->
                                    val x = si * stepX
                                    val y = scoreY(sc)
                                    val initY = if (y < padTop + 20f) y + 32f else y - 12f
                                    lblX.add(x); lblY.add(initY)
                                    lblText.add("$sc"); lblColor.add(colorInt)
                                }
                            }
                            val cols = lblX.distinct()
                            cols.forEach { cx ->
                                val idx = lblX.indices.filter { lblX[it] == cx }
                                    .sortedBy { lblY[it] }
                                for (i in 1 until idx.size) {
                                    val prev = idx[i - 1]; val curr = idx[i]
                                    if (lblY[curr] - lblY[prev] < minGap)
                                        lblY[curr] = lblY[prev] + minGap
                                }
                            }
                            lblX.indices.forEach { i ->
                                val clampedY = lblY[i].coerceIn(padTop, h - 4f)
                                // Stroke (dark outline) — draw first, behind the fill
                                cv.nativeCanvas.drawText(lblText[i], lblX[i], clampedY, trendStrokePaint)
                                // Fill (player colour)
                                trendTextPaint.color = lblColor[i]
                                cv.nativeCanvas.drawText(lblText[i], lblX[i], clampedY, trendTextPaint)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                // Legend: player row with stats
                activePlayers.forEachIndexed { i, ps ->
                    val scores = ps.recentScores.take(5).reversed()
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        // Color line sample
                        Box(Modifier.width(18.dp).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50.dp))
                            .background(colors[i]).align(Alignment.CenterVertically))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 12.sp, color = colors[i],
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(56.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(8.dp))
                        // Score sequence — equal-width cells for table alignment
                        Row(modifier = Modifier.weight(1f)) {
                            repeat(5) { idx ->
                                val sc = scores.getOrNull(idx)
                                Text(
                                    sc?.toString() ?: "·",
                                    fontSize = 12.sp,
                                    color = if (sc != null) CarcText2 else CarcText3,
                                    fontWeight = if (sc != null && sc == scores.maxOrNull()) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 5: SCORE STRUCTURE ─────────────────────────────────────────────────
    if (sections[5].enabled) {
        SectionLabel(stringResource(R.string.section_score_struct))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp)) {
                val maxAvg = activePlayers.maxOf { it.avgScore }.takeIf { it > 0f } ?: 1f
                val catColors = listOf(CarcBlue, CarcGreen, CarcYellow, CarcOrange)
                val catLabels = listOf("🏰", "🛤️", "⛪", "🌾")
                activePlayers.forEachIndexed { i, ps ->
                    val cats = listOf(ps.avgCity, ps.avgRoad, ps.avgMonastery, ps.avgFarm)
                    val catTotal = cats.sum().takeIf { it > 0f } ?: 1f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 11.sp, color = CarcText2,
                            modifier = Modifier.width(64.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        Row(modifier = Modifier.weight(ps.avgScore / maxAvg).height(18.dp)
                            .clip(RoundedCornerShape(4.dp))) {
                            cats.forEachIndexed { ci, v ->
                                val frac = (v / catTotal).coerceIn(0f, 1f)
                                if (frac > 0.02f)
                                    Box(Modifier.fillMaxHeight().weight(frac).background(catColors[ci]))
                            }
                        }
                        if (ps.avgScore / maxAvg < 1f) Spacer(Modifier.weight(1f - ps.avgScore / maxAvg))
                        Spacer(Modifier.width(6.dp))
                        Text("%.0f".format(ps.avgScore), fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = CarcText)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    catColors.forEachIndexed { i, c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(c))
                            Spacer(Modifier.width(4.dp))
                            Text(catLabels[i], fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
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
