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

@Composable
fun MatchDetailScreen(
    gameId: Int,
    viewModel: MainViewModel,
    players: List<PlayerEntity>,
    onEdit: () -> Unit = {},
    onPlayerClick: (Int) -> Unit = {}
) {
    var gamePlayers by remember { mutableStateOf<List<com.carcassonne.companion.data.entity.GamePlayerEntity>>(emptyList()) }
    var game by remember { mutableStateOf<GameEntity?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(gameId) {
        scope.launch {
            val (g, gps) = viewModel.getGameWithPlayers(gameId)
            game = g; gamePlayers = gps
        }
    }

    val sorted = gamePlayers.sortedBy { it.placement }
    val date = game?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it.date)) } ?: ""

    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            GamePhotoBox(
                photoPath = game?.photoPath,
                editable = true,
                context = context,
                onPhotoSaved = { path ->
                    game?.let { g ->
                        val newPath = if (path.isEmpty()) null else path
                        game = g.copy(photoPath = newPath)
                        viewModel.updateGamePhoto(g.id, newPath)
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
        }
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    game?.name ?: "Match #$gameId",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "📅 $date",
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = 12.sp,
                    color = CarcText3,
                    maxLines = 1,
                    softWrap = false
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarcCard2)
                            .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                // Build the share image in the background, then open Android Sharesheet
                                // directly from this screen to avoid a visible intermediate Activity.
                                scope.launch {
                                    try {
                                        val appContext = context.applicationContext
                                        val battleData = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            val db = com.carcassonne.companion.data.CarcassonneDatabase.getInstance(appContext)
                                            val selectedGame = db.gameDao().getGameById(gameId)
                                            if (selectedGame == null) {
                                                null
                                            } else {
                                                val selectedPlayers = db.gamePlayerDao().getPlayersForGame(gameId)
                                                    .sortedBy { it.placement }
                                                val allPlayers = db.playerDao().getAllPlayersOnce()
                                                com.carcassonne.companion.widget.BattleData(
                                                    selectedGame,
                                                    selectedPlayers,
                                                    allPlayers
                                                )
                                            }
                                        } ?: return@launch

                                        val uri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            val bitmap = com.carcassonne.companion.widget.LastBattleWidget
                                                .generateShareCard(appContext, battleData)
                                            val file = File(appContext.cacheDir, "share_battle_${gameId}.png")
                                            file.outputStream().use { out ->
                                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                            }
                                            bitmap.recycle()
                                            FileProvider.getUriForFile(
                                                appContext,
                                                "${appContext.packageName}.provider",
                                                file
                                            )
                                        }

                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            clipData = android.content.ClipData.newUri(
                                                appContext.contentResolver,
                                                "battle_image",
                                                uri
                                            )
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                    } catch (e: Exception) {
                                        android.util.Log.e("MatchDetailScreen", "Failed to share match $gameId", e)
                                    }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.share_game),
                            fontSize = 13.sp,
                            color = CarcGreen,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CarcCard2)
                            .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                            .clickable { onEdit() }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(R.string.edit_btn),
                            fontSize = 13.sp,
                            color = CarcGreen,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // Notes — показываем только если есть
            val notes = game?.notes
            if (!notes.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CarcCard),
                    border = BorderStroke(1.dp, CarcBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("📝", fontSize = 13.sp, color = CarcText3)
                        Spacer(Modifier.height(4.dp))
                        Text(notes, fontSize = 14.sp, color = CarcText, lineHeight = 20.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Winner
        sorted.firstOrNull()?.let { winner ->
            item {
                Text(stringResource(R.string.winner_label), fontSize = 13.sp, color = CarcText3, modifier = Modifier.padding(bottom = 8.dp))
                val p = players.find { it.id == winner.playerId }
                Card(
                    modifier = Modifier.clickable { p?.id?.let { onPlayerClick(it) } },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CarcGreenDeep.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, CarcGreenDeep)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(
                            name = p?.name ?: "?",
                            color = winner.meepleColor,
                            size = 52.dp,
                            avatarPath = p?.avatarPath
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p?.name ?: stringResource(R.string.unknown_player), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(10.dp).clip(CircleShape).background(meepleColor(winner.meepleColor)))
                            }
                            Text(stringResource(R.string.pts_mvp, winner.finalScore), fontSize = 13.sp, color = CarcText3)
                        }
                        Text(winner.finalScore.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = CarcGreen)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Final Standings
        item { Text(stringResource(R.string.final_standings), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp)) }
        items(sorted) { gp ->
            val p = players.find { it.id == gp.playerId }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CarcCard),
                modifier = Modifier.padding(bottom = 8.dp).clickable { p?.id?.let { onPlayerClick(it) } }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    RankBadge(gp.placement)
                    Spacer(Modifier.width(10.dp))
                    PlayerAvatar(
                        name = p?.name ?: "?",
                        color = gp.meepleColor,
                        size = 40.dp,
                        avatarPath = p?.avatarPath
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(p?.name ?: stringResource(R.string.unknown_player), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(meepleColor(gp.meepleColor)))
                        }
                        Text(stringResource(R.string.pts_label, gp.finalScore), fontSize = 12.sp, color = CarcText3)
                    }
                    Text(gp.finalScore.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Points Breakdown — всегда показываем
        item {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.points_breakdown), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CarcCard)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Заголовок колонок
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Spacer(Modifier.width(16.dp))
                        Text("", Modifier.weight(1f))
                        Text("🏰", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        Text("🛤️", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        Text("⛪", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        Text("🌾", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    }
                    HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                    sorted.forEach { gp ->
                        val p = players.find { it.id == gp.playerId }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Маркер цвета мипла в данной игре
                            Box(Modifier.size(10.dp).clip(CircleShape).background(meepleColor(gp.meepleColor)))
                            Spacer(Modifier.width(6.dp))
                            Text(p?.name ?: "?", Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(gp.cityPoints.toString(), fontSize = 13.sp, color = CarcText2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            Text(gp.roadPoints.toString(), fontSize = 13.sp, color = CarcText2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            Text(gp.monasteryPoints.toString(), fontSize = 13.sp, color = CarcText2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            Text(gp.farmPoints.toString(), fontSize = 13.sp, color = CarcText2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}
