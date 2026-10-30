@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.carcassonne.companion.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.components.*
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.GlobalStats
import com.carcassonne.companion.viewmodel.LiveGameState
import com.carcassonne.companion.viewmodel.MainViewModel
import com.carcassonne.companion.viewmodel.PlayerStats
import com.carcassonne.companion.viewmodel.ScoringObjectType
import com.carcassonne.companion.R
import com.carcassonne.companion.viewmodel.EndgamePlayerInput
import com.carcassonne.companion.viewmodel.LivePlayerState
import com.carcassonne.companion.util.ImageUtils
import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.documentfile.provider.DocumentFile
import com.carcassonne.companion.util.BackupManager
import java.io.File

// ─── History Screen ──────────────────────────────────────────────────────────
@Composable
fun HistoryScreen(
    games: List<GameEntity>,
    players: List<PlayerEntity>,
    allGamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity> = emptyList(),
    sortNewestFirst: Boolean,
    onToggleSort: () -> Unit,
    onGameClick: (Int) -> Unit,
    onEditGame: (Int) -> Unit,
    onDeleteGames: (Set<Int>) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(games, query) {
        if (query.isBlank()) games
        else games.filter { (it.name ?: "Game #${it.id}").contains(query, ignoreCase = true) }
    }
    var selected by remember { mutableStateOf(setOf<Int>()) }
    val selecting = selected.isNotEmpty()
    var showConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = selecting) {
        selected = emptySet()
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = CarcCard2,
            title = { Text(stringResource(R.string.delete_games_title, selected.size), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.cannot_undone), color = CarcText2) },
            confirmButton = {
                Button(
                    onClick = { onDeleteGames(selected); selected = emptySet(); showConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text(stringResource(R.string.delete_btn), fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.cancel), color = CarcText3) }
            }
        )
    }

    val imeBottom = with(LocalDensity.current) { WindowInsets.ime.getBottom(this).toDp() }
    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        // Поле поиска — фиксированное, не скроллируется
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            placeholder = { Text(stringResource(R.string.search_games), color = CarcText3) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = CarcText3) },
            trailingIcon = {
                if (selecting) {
                    TextButton(onClick = { selected = emptySet() }) {
                        Text(stringResource(R.string.cancel), color = CarcText3, fontSize = 13.sp)
                    }
                } else {
                    IconButton(onClick = onToggleSort) {
                        Text(if (sortNewestFirst) "↓" else "↑", fontSize = 18.sp, color = CarcGreen)
                    }
                }
            },
            shape = RoundedCornerShape(50.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CarcGreenDeep,
                unfocusedBorderColor = CarcBorder,
                focusedTextColor = CarcText,
                unfocusedTextColor = CarcText,
                cursorColor = CarcGreen,
                focusedContainerColor = CarcCard,
                unfocusedContainerColor = CarcCard
            )
        )

        Box(Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 4.dp,
                    bottom = if (selecting) 160.dp else 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selecting) {
                    item {
                        Text(
                            stringResource(R.string.selected_info, selected.size),
                            fontSize = 11.sp, color = CarcGreen, letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📜", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.no_games_found), color = CarcText2, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { game ->
                        val gps = allGamePlayers.filter { it.gameId == game.id }
                        val isSelected = game.id in selected
                        SelectableHistoryCard(
                            game = game,
                            gamePlayers = gps,
                            players = players,
                            isSelected = isSelected,
                            selecting = selecting,
                            onClick = {
                                if (selecting) {
                                    if (isSelected) selected = selected - game.id else selected = selected + game.id
                                } else {
                                    onGameClick(game.id)
                                }
                            },
                            onLongClick = {
                                if (isSelected) selected = selected - game.id else selected = selected + game.id
                            },
                            onEdit = { onEditGame(game.id) }
                        )
                    }
                }

                item { Spacer(Modifier.height(88.dp)) }
            }
        }

        // Bottom delete bar
        if (selecting) {
            Button(
                onClick = { showConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = if (imeBottom > 0.dp) 8.dp else 88.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    stringResource(R.string.delete_n_games, selected.size),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}


@Composable
fun SelectableHistoryCard(
    game: GameEntity,
    gamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity>,
    players: List<PlayerEntity>,
    isSelected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (isSelected) Modifier.border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.7f), RoundedCornerShape(14.dp)) else Modifier)
    ) {
        HistoryGameCard(
            game = game,
            gamePlayers = gamePlayers,
            players = players,
            onClick = onClick,
            onLongClick = onLongClick,
            onEdit = if (selecting) ({}) else onEdit,
            containerColor = if (isSelected) Color(0xFFEF4444).copy(alpha = 0.10f) else null
        )
        // Чекбокс сверху слева
        if (selecting) {
            Box(
                Modifier
                    .padding(10.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFFEF4444) else CarcBg3)
                    .border(1.5.dp, if (isSelected) Color(0xFFEF4444) else CarcBorder, RoundedCornerShape(6.dp))
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    lineHeight = 14.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun HistoryGameCard(
    game: GameEntity,
    gamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity> = emptyList(),
    players: List<PlayerEntity> = emptyList(),
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    containerColor: Color? = null
) {
    val date = remember(game.date) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(game.date))
    }
    val sortedGP = remember(gamePlayers) { gamePlayers.sortedBy { it.placement } }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor ?: CarcCard),
        border = BorderStroke(1.dp, CarcBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarcBg3),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, game.photoPath) {
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            if (game.photoPath != null && java.io.File(game.photoPath).exists())
                                BitmapFactory.decodeFile(game.photoPath)?.asImageBitmap()
                            else null
                        }
                    }
                    val safeBmp = bmp
                    if (safeBmp != null) {
                        androidx.compose.foundation.Image(
                            bitmap = safeBmp, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text("🗺️", fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(game.name ?: "Game #${game.id}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(date, fontSize = 12.sp, color = CarcText3, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Text("✎", fontSize = 18.sp, color = CarcText3)
                }
            }
            if (sortedGP.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                val rows = sortedGP.chunked(2)
                rows.forEachIndexed { rowIdx, rowPlayers ->
                    if (rowIdx > 0) Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowPlayers.forEach { gp ->
                            val p = players.find { it.id == gp.playerId }
                            Row(
                                Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatar(p?.name ?: "?", p?.meepleColor ?: "gray", size = 24.dp, avatarPath = p?.avatarPath)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    p?.name ?: "?",
                                    fontSize = 12.sp,
                                    color = CarcText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(3.dp))
                                Box(Modifier.size(7.dp).clip(CircleShape)
                                    .background(meepleColor(gp.meepleColor)))
                            }
                        }
                        if (rowPlayers.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
