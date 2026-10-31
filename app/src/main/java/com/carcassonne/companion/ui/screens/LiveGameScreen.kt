package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.LiveGameState
import com.carcassonne.companion.viewmodel.LivePlayerState
import com.carcassonne.companion.viewmodel.ScoringObjectType

@Composable
fun LiveGameScreen(
    liveGame: LiveGameState,
    onAdjustScore: (Int, Int) -> Unit,
    onAddObject: (Int, ScoringObjectType, Int, String) -> Unit,
    onUndoLast: (Int) -> Unit,
    onFinish: () -> Unit
) {
    var addObjectFor by remember { mutableStateOf<Pair<LivePlayerState, Int>?>(null) }
    // tab: 0=city 1=road 2=monastery 3=dragon(fairy)
    val hasDragon = "dragon" in liveGame.expansions
    val hasInns   = "inns"   in liveGame.expansions

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${liveGame.selectedPlayers.size} players", fontSize = 13.sp, color = CarcText3)
                    val leader = liveGame.selectedPlayers.maxByOrNull { it.score }
                    if (leader != null)
                        Text("👑 ${leader.playerName.take(10)}: ${leader.score}",
                            fontSize = 12.sp, color = meepleColor(leader.meepleColor))
                }
            }

            items(liveGame.selectedPlayers) { player ->
                val accent = meepleColor(player.meepleColor)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CarcCard),
                    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        // Header: avatar + name + score
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(player.playerName, player.meepleColor, 44.dp, avatarPath = player.avatarPath)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(player.playerName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                if (player.events.isNotEmpty()) {
                                    Text(
                                        player.events.takeLast(3).joinToString(" · ") { it.label },
                                        fontSize = 11.sp, color = CarcText3, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(player.score.toString(),
                                fontSize = 44.sp, fontWeight = FontWeight.Black, color = accent,
                                lineHeight = 44.sp)
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = accent.copy(alpha = 0.18f))
                        Spacer(Modifier.height(10.dp))

                        // Object buttons grid
                        val baseObjects = listOf(
                            Triple(0, "🏰", stringResource(R.string.city_btn)),
                            Triple(1, "🛤️", stringResource(R.string.road_btn)),
                            Triple(2, "⛪", stringResource(R.string.monastery_btn))
                        )
                        // Dragon: Fairy gives +1 per turn to adjacent player
                        val allObjects = if (hasDragon)
                            baseObjects + Triple(3, "🧚", stringResource(R.string.fairy_btn))
                        else baseObjects

                        // Показываем в 2 строки по 2 (или в одну строку если объектов 3)
                        val rows = allObjects.chunked(if (allObjects.size <= 3) 3 else 2)
                        rows.forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowItems.forEach { (tabIdx, icon, label) ->
                                    Box(
                                        Modifier.weight(1f).height(64.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accent.copy(alpha = 0.12f))
                                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (tabIdx == 3) {
                                                    // Fairy: instant +1
                                                    onAddObject(player.playerId, ScoringObjectType.CITY, 1, "🧚 +1")
                                                } else {
                                                    addObjectFor = Pair(player, tabIdx)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(icon, fontSize = 22.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth())
                                            Spacer(Modifier.height(4.dp))
                                            Text(label, fontSize = 10.sp, color = accent,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 11.sp,
                                                modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        // Undo last event
                        if (player.events.isNotEmpty()) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CarcBg3)
                                    .clickable { onUndoLast(player.playerId) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("↩  ${player.events.last().label}",
                                    fontSize = 11.sp, color = CarcText3)
                            }
                        }
                    }
                }
            }
        }

        // FAB Finish Game
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
            Box(
                Modifier.clip(RoundedCornerShape(16.dp))
                    .background(CarcGreen)
                    .clickable(onClick = onFinish)
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.finish_game), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarcBg)
            }
        }
    }

    // Bottom sheet — открывается на нужном табе
    addObjectFor?.let { (player, startTab) ->
        AddObjectSheet(
            playerName = player.playerName,
            meepleCol = player.meepleColor,
            startTab = startTab,
            hasInns = hasInns,
            onDismiss = { addObjectFor = null },
            onScore = { type, pts, label ->
                onAddObject(player.playerId, type, pts, label)
            }
        )
    }
}

