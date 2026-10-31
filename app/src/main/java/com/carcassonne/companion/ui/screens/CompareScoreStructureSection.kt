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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.PlayerStats

@Composable
fun CompareScoreStructureSection(
    activePlayers: List<PlayerStats>,
    colors: List<Color>
) {
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
