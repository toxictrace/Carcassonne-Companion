package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.components.MeepleColorPicker
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.components.PrimaryButton
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditGameScreen(
    gameId: Int,
    viewModel: MainViewModel,
    allPlayers: List<PlayerEntity>,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var game by remember { mutableStateOf<com.carcassonne.companion.data.entity.GameEntity?>(null) }
    var gamePlayers by remember { mutableStateOf<List<com.carcassonne.companion.data.entity.GamePlayerEntity>>(emptyList()) }

    LaunchedEffect(gameId) {
        val (g, gps) = viewModel.getGameWithPlayers(gameId)
        game = g
        gamePlayers = gps
    }

    if (game == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = CarcGreen) }
        return
    }

    var gameName by remember(game) { mutableStateOf(game?.name ?: "") }
    var gameNotes by remember(game) { mutableStateOf(game?.notes ?: "") }

    // DatePicker state
    val initialMs = game?.date ?: System.currentTimeMillis()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedDateMs = datePickerState.selectedDateMillis ?: initialMs
    val displayDate = remember(selectedDateMs) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(selectedDateMs))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.ok), color = CarcGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel), color = CarcText3)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = CarcCard2)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = CarcCard2,
                    titleContentColor = CarcText,
                    headlineContentColor = CarcGreen,
                    weekdayContentColor = CarcText3,
                    subheadContentColor = CarcText2,
                    navigationContentColor = CarcText,
                    yearContentColor = CarcText,
                    currentYearContentColor = CarcGreen,
                    selectedYearContentColor = CarcBg,
                    selectedYearContainerColor = CarcGreen,
                    dayContentColor = CarcText,
                    selectedDayContentColor = CarcBg,
                    selectedDayContainerColor = CarcGreen,
                    todayContentColor = CarcGreen,
                    todayDateBorderColor = CarcGreen
                )
            )
        }
    }

    // Player state
    data class EditPlayerState(
        val playerId: Int,
        val name: String,
        var color: String,
        var total: String,
        var city: String,
        var road: String,
        var monastery: String,
        var farm: String,
        val avatarPath: String? = null
    )

    val editPlayers = remember(gamePlayers, allPlayers) {
        gamePlayers.map { gp ->
            val p = allPlayers.find { it.id == gp.playerId }
            mutableStateOf(EditPlayerState(
                playerId = gp.playerId,
                name = p?.name ?: context.getString(R.string.unknown_player),
                color = gp.meepleColor,
                total = gp.finalScore.takeIf { it > 0 }?.toString() ?: "",
                city = gp.cityPoints.takeIf { it > 0 }?.toString() ?: "",
                road = gp.roadPoints.takeIf { it > 0 }?.toString() ?: "",
                monastery = gp.monasteryPoints.takeIf { it > 0 }?.toString() ?: "",
                farm = gp.farmPoints.takeIf { it > 0 }?.toString() ?: "",
                avatarPath = p?.avatarPath
            ))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.game_info_section), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = gameName,
                onValueChange = { gameName = it },
                label = { Text(stringResource(R.string.game_name_label), color = CarcText3) },
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
            Text(stringResource(R.string.date_section), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CarcCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CarcBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📅", fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(displayDate, fontSize = 15.sp, color = CarcText, modifier = Modifier.weight(1f))
                    Text("›", fontSize = 20.sp, color = CarcGreen)
                }
            }
        }

        item {
            Text(stringResource(R.string.players_scores_section), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
        }

        items(editPlayers.size) { i ->
            val state = editPlayers[i].value
            val takenColors = editPlayers
                .filterIndexed { idx, _ -> idx != i }
                .map { it.value.color }.toSet()
            val catSum = (state.city.toIntOrNull() ?: 0) + (state.road.toIntOrNull() ?: 0) +
                         (state.monastery.toIntOrNull() ?: 0) + (state.farm.toIntOrNull() ?: 0)
            val displayTotal = state.total.toIntOrNull() ?: catSum

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CarcCard)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(state.name, state.color, avatarPath = state.avatarPath)
                        Spacer(Modifier.width(10.dp))
                        Text(state.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("$displayTotal очк.", fontSize = 15.sp, color = CarcGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.meeple_color_label), fontSize = 10.sp, color = CarcText3, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(6.dp))
                    MeepleColorPicker(
                        selected = state.color,
                        onSelect = { editPlayers[i].value = state.copy(color = it) },
                        disabledColors = takenColors
                    )
                    Spacer(Modifier.height(12.dp))
                    // Total override
                    ScoreInputField(
                        stringResource(R.string.total_override),
                        state.total,
                        { editPlayers[i].value = state.copy(total = it) },
                        Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.or_enter_categories),
                        fontSize = 11.sp, color = CarcText3,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScoreInputField(stringResource(R.string.city_label), state.city, { editPlayers[i].value = state.copy(city = it, total = "") }, Modifier.weight(1f))
                        ScoreInputField(stringResource(R.string.road_label), state.road, { editPlayers[i].value = state.copy(road = it, total = "") }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScoreInputField(stringResource(R.string.mon_input), state.monastery, { editPlayers[i].value = state.copy(monastery = it, total = "") }, Modifier.weight(1f))
                        ScoreInputField(stringResource(R.string.farm_input), state.farm, { editPlayers[i].value = state.copy(farm = it, total = "") }, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(stringResource(R.string.save_changes), onClick = {
                val results = editPlayers.map { ep ->
                    val s = ep.value
                    val city = s.city.toIntOrNull() ?: 0
                    val road = s.road.toIntOrNull() ?: 0
                    val mon  = s.monastery.toIntOrNull() ?: 0
                    val farm = s.farm.toIntOrNull() ?: 0
                    val finalScore = s.total.toIntOrNull() ?: (city + road + mon + farm)
                    com.carcassonne.companion.data.repository.PlayerResult(
                        playerId = s.playerId,
                        meepleColor = s.color,
                        finalScore = finalScore,
                        cityPoints = city,
                        roadPoints = road,
                        monasteryPoints = mon,
                        farmPoints = farm
                    )
                }
                viewModel.updateGame(
                    gameId = gameId,
                    name = gameName.ifBlank { null },
                    date = selectedDateMs,
                    playerResults = results,
                    notes = gameNotes.ifBlank { null },
                    onDone = onDone
                )
            })
            Spacer(Modifier.height(72.dp))
        }
    }
}
