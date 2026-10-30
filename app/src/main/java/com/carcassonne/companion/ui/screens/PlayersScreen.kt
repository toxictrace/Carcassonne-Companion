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

// ─── Players Screen ──────────────────────────────────────────────────────────
@Composable
fun PlayersScreen(
    players: List<PlayerEntity>,
    playerStats: List<PlayerStats>,
    onPlayerClick: (Int) -> Unit,
    onAddPlayer: () -> Unit,
    onDeletePlayer: (PlayerEntity) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(players, query) {
        if (query.isBlank()) players
        else players.filter { it.name.contains(query, ignoreCase = true) }
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
            title = { Text(stringResource(R.string.delete_players_title, selected.size), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.cannot_undone_history), color = CarcText2) },
            confirmButton = {
                Button(
                    onClick = {
                        players.filter { it.id in selected }.forEach { onDeletePlayer(it) }
                        selected = emptySet()
                        showConfirm = false
                    },
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
            placeholder = { Text(stringResource(R.string.search_players), color = CarcText3) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = CarcText3) },
            trailingIcon = {
                if (selecting) {
                    TextButton(onClick = { selected = emptySet() }) {
                        Text(stringResource(R.string.cancel), color = CarcText3, fontSize = 13.sp)
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
                item {
                    Text(
                        if (selecting) stringResource(R.string.selected_info, selected.size)
                        else stringResource(R.string.players_section),
                        fontSize = 11.sp,
                        color = if (selecting) CarcGreen else CarcText3,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (filtered.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👤", fontSize = 48.sp)
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.no_players), color = CarcText2, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = onAddPlayer) {
                                    Text(stringResource(R.string.add_first_player), color = CarcGreen)
                                }
                            }
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { player ->
                        val stats = playerStats.find { it.player.id == player.id }
                        val isSelected = player.id in selected
                        SelectablePlayerCard(
                            player = player,
                            stats = stats,
                            isSelected = isSelected,
                            selecting = selecting,
                            onClick = {
                                if (selecting) {
                                    if (isSelected) selected = selected - player.id
                                    else selected = selected + player.id
                                } else {
                                    onPlayerClick(player.id)
                                }
                            },
                            onLongClick = {
                                if (isSelected) selected = selected - player.id
                                else selected = selected + player.id
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
                onClick = { showConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = if (imeBottom > 0.dp) 8.dp else 88.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    stringResource(R.string.delete_n_players, selected.size),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}


@Composable
fun SelectablePlayerCard(
    player: PlayerEntity,
    stats: PlayerStats?,
    isSelected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val accent = meepleColor(player.meepleColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEF4444).copy(alpha = 0.12f) else CarcCard
        ),
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f)) else null
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Чекбокс — появляется в режиме выделения
            if (selecting) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFEF4444) else CarcBg3)
                        .border(1.5.dp, if (isSelected) Color(0xFFEF4444) else CarcBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        lineHeight = 14.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.width(12.dp))
            }
            PlayerAvatar(player.name, player.meepleColor, size = 48.dp, avatarPath = player.avatarPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(player.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (stats != null && stats.gamesPlayed > 0) {
                    Spacer(Modifier.height(4.dp))
                    WinRateBar(stats.winRate)
                }
            }
            if (stats != null && stats.gamesPlayed > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CarcCard2)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.wins_losses, stats.wins, stats.gamesPlayed - stats.wins),
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarcText
                        )
                    }
                    Text(
                        stringResource(R.string.win_rate_pct, (stats.winRate * 100).toInt()),
                        fontSize = 11.sp,
                        color = if (stats.winRate >= 0.5f) CarcGreen else CarcText3,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}

// Оставляем PlayerCard для обратной совместимости (используется в других местах)
@Composable
fun PlayerCard(player: PlayerEntity, stats: PlayerStats?, onClick: () -> Unit, onDeleteRequest: (() -> Unit)? = null) {
    SelectablePlayerCard(
        player = player, stats = stats,
        isSelected = false, selecting = false,
        onClick = onClick, onLongClick = {}
    )
}

// ─── Stats Screen ────────────────────────────────────────────────────────────
