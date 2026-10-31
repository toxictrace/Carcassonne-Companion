package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.components.*
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.LiveGameState

@Composable
fun NewGameScreen(
    players: List<PlayerEntity>,
    liveGame: LiveGameState,
    onTogglePlayer: (PlayerEntity) -> Unit,
    onSetPlayerColor: (Int, String) -> Unit,
    onToggleExpansion: (String) -> Unit,
    onSetRiver: (Boolean) -> Unit,
    onSetTimed: (Boolean) -> Unit,
    onStartGame: () -> Unit,
    onAddPlayer: () -> Unit
) {
    val selected = liveGame.selectedPlayers.map { it.playerId }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            SectionHeader(stringResource(R.string.select_players), stringResource(R.string.add_new), onAddPlayer)
            Spacer(Modifier.height(12.dp))
        }

        if (players.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👤", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onAddPlayer) { Text(stringResource(R.string.add_player_first), color = CarcGreen) }
                    }
                }
            }
        } else {
            val maxPlayers = if (liveGame.expansions.isNotEmpty()) 6 else 5
            items(players) { player ->
                val isSelected = player.id in selected
                val isMaxReached = !isSelected && liveGame.selectedPlayers.size >= maxPlayers
                val assignedColor = liveGame.selectedPlayers.find { it.playerId == player.id }?.meepleColor ?: player.meepleColor
                // Colors taken by OTHER selected players
                val takenColors = liveGame.selectedPlayers
                    .filter { it.playerId != player.id }
                    .map { it.meepleColor }
                    .toSet()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CarcGreenDeep.copy(alpha = 0.2f) else CarcCard
                    ),
                    border = if (isSelected) BorderStroke(1.dp, CarcGreenDeep) else BorderStroke(1.dp, CarcBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(player.name, assignedColor, avatarPath = player.avatarPath)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(player.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    when {
                                        isSelected -> stringResource(R.string.tap_to_remove)
                                        isMaxReached -> stringResource(R.string.max_players_reached, maxPlayers)
                                        else -> stringResource(R.string.tap_to_include)
                                    },
                                    fontSize = 12.sp, color = if (isMaxReached && !isSelected) CarcText3.copy(alpha = 0.5f) else CarcText3
                                )
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { if (!isMaxReached) onTogglePlayer(player) },
                                enabled = !isMaxReached || isSelected,
                                colors = CheckboxDefaults.colors(checkedColor = CarcGreen, uncheckedColor = CarcText3,
                                    disabledUncheckedColor = CarcText3.copy(alpha = 0.3f))
                            )
                        }
                        if (isSelected) {
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(R.string.select_meeple_color), fontSize = 11.sp, color = CarcText3, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(8.dp))
                            MeepleColorPicker(
                                selected = assignedColor,
                                onSelect = { onSetPlayerColor(player.id, it) },
                                disabledColors = takenColors
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.game_expansions), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CarcCard)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ExpansionRow("🏰", stringResource(R.string.expansion_inns), "", "inns" in liveGame.expansions) { onToggleExpansion("inns") }
                    ExpansionRow("⚖️", stringResource(R.string.expansion_traders), "", "traders" in liveGame.expansions) { onToggleExpansion("traders") }
                    ExpansionRow("🐉", stringResource(R.string.expansion_dragon), "", "dragon" in liveGame.expansions) { onToggleExpansion("dragon") }
                    ExpansionRow("⛪", stringResource(R.string.expansion_abbey), "", "abbey" in liveGame.expansions) { onToggleExpansion("abbey") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            PrimaryButton(
                stringResource(R.string.start_game),
                onClick = onStartGame,
                enabled = liveGame.selectedPlayers.size >= 2
            )
            Spacer(Modifier.height(72.dp))
        }
    }
}
