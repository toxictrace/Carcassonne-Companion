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
fun CompareTogetherSection(
    activePlayers: List<PlayerStats>,
    allGamePlayers: List<GamePlayerEntity>,
    colors: List<Color>
) {
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
