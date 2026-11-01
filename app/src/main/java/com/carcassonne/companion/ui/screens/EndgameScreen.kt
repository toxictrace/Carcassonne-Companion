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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*
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
                    val accent = meepleColor(player.meepleColor)
                    val inp = inputs[player.playerId]!!
                    Card(shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CarcCard),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlayerAvatar(player.playerName, player.meepleColor, 36.dp, avatarPath = player.avatarPath)
                                Spacer(Modifier.width(10.dp))
                                Text(player.playerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent)
                                Spacer(Modifier.weight(1f))
                                Text(stringResource(R.string.base_score, player.score), fontSize = 13.sp, color = CarcText3)
                            }
                            Spacer(Modifier.height(12.dp))
                            // Incomplete city tiles+shields
                            EndgameStepperRow(stringResource(R.string.city_tiles_shields), inp.value.incompleteCity, 0, 99) {
                                inp.value = inp.value.copy(incompleteCity = it)
                            }
                            Spacer(Modifier.height(8.dp))
                            EndgameStepperRow(stringResource(R.string.road_tiles), inp.value.incompleteRoad, 0, 99) {
                                inp.value = inp.value.copy(incompleteRoad = it)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = inp.value.hasIncompleteMonastery,
                                    onCheckedChange = { checked ->
                                        inp.value = inp.value.copy(
                                            hasIncompleteMonastery = checked,
                                            incompleteMonastery = if (checked) inp.value.incompleteMonastery else 0
                                        )
                                    }
                                )
                                Text(
                                    stringResource(R.string.incomplete_monastery_present),
                                    fontSize = 13.sp,
                                    color = CarcText2
                                )
                            }
                            if (inp.value.hasIncompleteMonastery) {
                                EndgameStepperRow(stringResource(R.string.monastery_tiles), inp.value.incompleteMonastery, 0, 8) {
                                    inp.value = inp.value.copy(incompleteMonastery = it)
                                }
                            }
                            // Sub-total includes 1 point for the monastery itself.
                            val monasteryPoints = if (inp.value.hasIncompleteMonastery) 1 + inp.value.incompleteMonastery else 0
                            val sub = inp.value.incompleteCity + inp.value.incompleteRoad + monasteryPoints
                            if (sub > 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text(stringResource(R.string.incomplete_pts_suffix, sub), fontSize = 12.sp, color = accent)
                                }
                            }
                        }
                    }
                }
                item {
                    PrimaryButton(stringResource(R.string.next_farms), onClick = { phase = 1 })
                }
            }

            1 -> { // Farms
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CarcBg3).padding(14.dp)) {
                        Text(stringResource(R.string.farm_scoring), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.farm_instructions),
                            fontSize = 13.sp, color = CarcText3)
                        Text(stringResource(R.string.farm_formula), fontSize = 13.sp, color = CarcGreen,
                            fontWeight = FontWeight.SemiBold)
                    }
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
                    OutlinedTextField(
                        value = gameName, onValueChange = { gameName = it },
                        label = { Text(stringResource(R.string.game_name_hint), color = CarcText3) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
                            focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gameNotes,
                        onValueChange = { if (it.length <= 150) gameNotes = it },
                        label = { Text(stringResource(R.string.notes_label), color = CarcText3) },
                        placeholder = { Text(stringResource(R.string.notes_placeholder), color = CarcText3, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        supportingText = { Text("${gameNotes.length}/150", color = CarcText3, fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
                            focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
                        )
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
