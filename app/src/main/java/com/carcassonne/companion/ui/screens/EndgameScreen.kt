package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.CarcText3
import com.carcassonne.companion.viewmodel.EndgamePlayerInput
import com.carcassonne.companion.viewmodel.LiveGameState

@Composable
fun EndgameScreen(
    liveGame: LiveGameState,
    onApply: (Map<Int, EndgamePlayerInput>) -> Unit,
    onSave: (String, String?) -> Unit,
    onBack: () -> Unit,
    pendingPhotoPath: String? = null,
    onSetPhoto: (String?) -> Unit = {}
) {
    val players = liveGame.selectedPlayers
    val inputs = remember {
        players.associate { p ->
            p.playerId to mutableStateOf(EndgamePlayerInput(p.playerId))
        }
    }
    var gameName by remember { mutableStateOf("") }
    var gameNotes by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(0) } // 0=incomplete 1=farms 2=confirm

    val totalWithEndgame = { pid: Int ->
        val p = players.find { it.playerId == pid }!!
        val eg = inputs[pid]?.value ?: EndgamePlayerInput(pid)
        p.score + eg.totalPoints()
    }

    LazyColumn(
        Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            EndgamePhaseTabs(phase = phase, onPhaseChange = { phase = it })
        }

        when (phase) {
            0 -> { // Incomplete objects
                item {
                    Text(stringResource(R.string.incomplete_note),
                        fontSize = 13.sp, color = CarcText3)
                }
                items(players) { player ->
                    val inp = inputs[player.playerId]!!
                    EndgameIncompletePlayerCard(
                        player = player,
                        input = inp.value,
                        onInputChange = { update -> inp.value = update(inp.value) }
                    )
                }
                item {
                    PrimaryButton(stringResource(R.string.next_farms), onClick = { phase = 1 })
                }
            }

            1 -> { // Farms
                item {
                    EndgameFarmInstructionsCard()
                }
                items(players) { player ->
                    EndgameFarmPlayerCard(
                        player = player,
                        input = inputs[player.playerId]!!.value,
                        onFarmCitiesChange = { value ->
                            inputs[player.playerId]!!.value =
                                inputs[player.playerId]!!.value.copy(farmCities = value)
                        }
                    )
                }
                item {
                    EndgameNavigationButtons(
                        backLabel = stringResource(R.string.back_btn),
                        nextLabel = stringResource(R.string.next_confirm),
                        onBack = { phase = 0 },
                        onNext = { phase = 2 }
                    )
                }
            }

            2 -> { // Confirm & Save
                item {
                    Text(stringResource(R.string.final_scores), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                val sorted = players.sortedByDescending { totalWithEndgame(it.playerId) }
                items(sorted) { player ->
                    val inp = inputs[player.playerId]?.value ?: EndgamePlayerInput(player.playerId)
                    val rank = sorted.indexOfFirst { it.playerId == player.playerId } + 1
                    EndgameFinalScoreCard(
                        player = player,
                        input = inp,
                        rank = rank,
                        totalScore = totalWithEndgame(player.playerId)
                    )
                }
                item { Spacer(Modifier.height(4.dp)) }
                item {
                    // Фото поля партии
                    Text(stringResource(R.string.board_photo_btn), fontSize = 13.sp, color = CarcText3, modifier = Modifier.padding(bottom = 8.dp))
                    GamePhotoBox(
                        photoPath = pendingPhotoPath,
                        editable = true,
                        onPhotoSaved = { path -> onSetPhoto(if (path.isEmpty()) null else path) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    EndgameGameDetailsFields(
                        gameName = gameName,
                        onGameNameChange = { gameName = it },
                        gameNotes = gameNotes,
                        onGameNotesChange = { if (it.length <= 150) gameNotes = it }
                    )
                }
                item {
                    EndgameNavigationButtons(
                        backLabel = stringResource(R.string.back_btn),
                        nextLabel = stringResource(R.string.save_game),
                        onBack = { phase = 1 },
                        onNext = {
                            val finalInputs = inputs.mapValues { it.value.value }
                            onApply(finalInputs)
                            onSave(gameName, gameNotes.ifBlank { null })
                        },
                        nextFontSize = 15.sp
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

// ─── Match Detail Screen ──────────────────────────────────────────────────────
