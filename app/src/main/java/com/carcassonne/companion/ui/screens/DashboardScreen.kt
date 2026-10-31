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

// ─── Dashboard Screen ────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    stats: GlobalStats,
    games: List<GameEntity>,
    players: List<PlayerEntity>,
    allGamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity> = emptyList(),
    sortNewestFirst: Boolean,
    onToggleSort: () -> Unit,
    onViewAll: () -> Unit,
    onGameClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item {
            Text(
                stringResource(R.string.global_stats),
                fontSize = 11.sp,
                color = CarcText3,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard("🃏", stringResource(R.string.total_games), stats.totalGames.toString(), Modifier.weight(1f))
                StatCard("👥", stringResource(R.string.total_players_stat), stats.totalPlayers.toString(), Modifier.weight(1f))
                StatCard("🏆", stringResource(R.string.highest_score), stats.highestScore.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.recent_activity), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort toggle
                    IconButton(onClick = onToggleSort, modifier = Modifier.size(32.dp)) {
                        Text(if (sortNewestFirst) "↓" else "↑", fontSize = 18.sp, color = CarcGreen)
                    }
                    TextButton(onClick = onViewAll) {
                        Text(stringResource(R.string.view_all), fontSize = 13.sp, color = CarcGreen)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        val recentGames = games.take(4)
        if (recentGames.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.no_games_yet), color = CarcText2, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.start_first_game), color = CarcText3, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(recentGames) { game ->
                val gps = allGamePlayers.filter { it.gameId == game.id }
                DashboardGameRow(game, players, gps) { onGameClick(game.id) }
                Spacer(Modifier.height(10.dp))
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun DashboardGameRow(
    game: GameEntity,
    players: List<PlayerEntity>,
    gamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity> = emptyList(),
    onClick: () -> Unit
) {
    val date = remember(game.date) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(game.date))
    }
    val sortedGP = remember(gamePlayers) { gamePlayers.sortedBy { it.placement } }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard),
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
                        Text("🗺️", fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(game.name ?: "Game #${game.id}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(date, fontSize = 12.sp, color = CarcText3)
            }
            if (sortedGP.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                // Игроки в 2 колонки
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
                        // Если нечётное количество — пустой placeholder
                        if (rowPlayers.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
