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
        // Phase tabs
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CarcBg3),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(stringResource(R.string.tab_incomplete), stringResource(R.string.tab_farms), stringResource(R.string.tab_confirm)).forEachIndexed { i, label ->
                    val sel = phase == i
                    Box(
                        Modifier.weight(1f).padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) CarcGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { phase = i }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) CarcGreen else CarcText3)
                    }
                }
            }
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
                            EndgameStepperRow(stringResource(R.string.monastery_tiles), inp.value.incompleteMonastery, 0, 8) {
                                inp.value = inp.value.copy(incompleteMonastery = it)
                            }
                            // Sub-total
                            val sub = inp.value.incompleteCity + inp.value.incompleteRoad + inp.value.incompleteMonastery
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
                    val accent = meepleColor(player.meepleColor)
                    val inp = inputs[player.playerId]!!
                    Card(shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CarcCard),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlayerAvatar(player.playerName, player.meepleColor, 36.dp, avatarPath = player.avatarPath)
                                Spacer(Modifier.width(10.dp))
                                Text(player.playerName, fontWeight = FontWeight.Bold, color = accent)
                            }
                            Spacer(Modifier.height(12.dp))
                            EndgameStepperRow(stringResource(R.string.completed_cities_adjacent), inp.value.farmCities, 0, 30) {
                                inp.value = inp.value.copy(farmCities = it)
                            }
                            if (inp.value.farmCities > 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(accent.copy(alpha = 0.1f)).padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.farm_cities_pts, inp.value.farmCities), fontSize = 13.sp, color = CarcText2)
                                    Text("+${inp.value.farmCities * 3}", fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold, color = accent)
                                }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(10.dp))
                            .clickable { phase = 0 }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.back_btn), color = CarcText2, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.weight(2f).clip(RoundedCornerShape(10.dp))
                            .background(CarcGreen).clickable { phase = 2 }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.next_confirm), color = CarcBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            2 -> { // Confirm & Save
                item {
                    Text(stringResource(R.string.final_scores), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                val sorted = players.sortedByDescending { totalWithEndgame(it.playerId) }
                items(sorted) { player ->
                    val accent = meepleColor(player.meepleColor)
                    val inp = inputs[player.playerId]?.value ?: EndgamePlayerInput(player.playerId)
                    val rank = sorted.indexOfFirst { it.playerId == player.playerId } + 1
                    Card(shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (rank == 1) CarcGreenDeep.copy(alpha = 0.25f) else CarcCard),
                        border = BorderStroke(if (rank == 1) 1.5.dp else 1.dp,
                            if (rank == 1) CarcGreenDeep else accent.copy(alpha = 0.3f))) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (rank == 1) "🏆" else "$rank", fontSize = if (rank == 1) 24.sp else 16.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                            PlayerAvatar(player.playerName, player.meepleColor, 40.dp, avatarPath = player.avatarPath)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(player.playerName, fontWeight = FontWeight.Bold, color = accent)
                                Text(stringResource(R.string.base_score_label, player.score) + (if (inp.incompleteCity + inp.incompleteRoad + inp.incompleteMonastery > 0) " " + stringResource(R.string.incomplete_pts_suffix, inp.incompleteCity + inp.incompleteRoad + inp.incompleteMonastery) else "") + (if (inp.farmCities > 0) " " + stringResource(R.string.farms_pts_suffix, inp.farmCities * 3) else ""),
                                    fontSize = 11.sp, color = CarcText3)
                            }
                            Text(totalWithEndgame(player.playerId).toString(),
                                fontSize = 32.sp, fontWeight = FontWeight.Black, color = accent)
                        }
                    }
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(10.dp))
                            .clickable { phase = 1 }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.back_btn), color = CarcText2, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.weight(2f).clip(RoundedCornerShape(10.dp))
                            .background(CarcGreen)
                            .clickable {
                                val finalInputs = inputs.mapValues { it.value.value }
                                onApply(finalInputs)
                                onSave(gameName, gameNotes.ifBlank { null })
                            }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.save_game), color = CarcBg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
fun EndgameStepperRow(label: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, color = CarcText2)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                .clickable { if (value > min) onValue(value - 1) },
                contentAlignment = Alignment.Center) {
                Text("−", fontSize = 18.sp, color = if (value > min) CarcText else CarcText3)
            }
            Box(Modifier.width(44.dp).height(32.dp).background(CarcBg3)
                .border(BorderStroke(1.dp, CarcBorder)),
                contentAlignment = Alignment.Center) {
                Text(value.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                .clickable { if (value < max) onValue(value + 1) },
                contentAlignment = Alignment.Center) {
                Text("+", fontSize = 18.sp, color = if (value < max) CarcText else CarcText3)
            }
        }
    }
}

// ─── Match Detail Screen ──────────────────────────────────────────────────────
