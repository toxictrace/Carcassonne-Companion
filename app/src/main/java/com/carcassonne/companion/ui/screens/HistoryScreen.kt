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
    var pendingDeleteIds by remember { mutableStateOf(setOf<Int>()) }
    var showConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = selecting) {
        selected = emptySet()
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = CarcCard2,
            title = { Text(stringResource(R.string.delete_games_title, pendingDeleteIds.size), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.cannot_undone), color = CarcText2) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGames(pendingDeleteIds)
                        selected = emptySet()
                        pendingDeleteIds = emptySet()
                        showConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text(stringResource(R.string.delete_btn), fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDeleteIds = emptySet()
                    showConfirm = false
                }) { Text(stringResource(R.string.cancel), color = CarcText3) }
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
                            onEdit = { onEditGame(game.id) },
                            onDelete = {
                                pendingDeleteIds = setOf(game.id)
                                showConfirm = true
                            }
                        )
                    }
                }

                item { Spacer(Modifier.height(88.dp)) }
            }
        }

        // Bottom delete bar
        if (selecting) {
            Button(
                onClick = {
                    pendingDeleteIds = selected
                    showConfirm = true
                },
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


