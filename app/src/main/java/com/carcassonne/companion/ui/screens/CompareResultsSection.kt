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
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.PlayerStats

@Composable
fun CompareResultsSection(
    activePlayers: List<PlayerStats>,
    allGamePlayers: List<GamePlayerEntity>,
    colors: List<Color>
) {
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
