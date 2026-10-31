package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.PlayerStats

@Composable
fun CompareScoreRangeSection(
    activePlayers: List<PlayerStats>,
    colors: List<Color>
) {
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
