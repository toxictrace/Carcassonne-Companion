package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.PlayerStats

@Composable
fun CompareTrendSection(
    activePlayers: List<PlayerStats>,
    colors: List<Color>
) {
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
