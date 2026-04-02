@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.carcassonne.companion.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.imePadding
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

// ─── Game Photo Box ───────────────────────────────────────────────────────────
// Универсальный блок для показа/выбора фото партии.
// editable=true — показывает кнопки камера/галерея; editable=false — только просмотр.
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GamePhotoBox(
    photoPath: String?,
    editable: Boolean = false,
    context: android.content.Context = androidx.compose.ui.platform.LocalContext.current,
    onPhotoSaved: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentPath by remember(photoPath) { mutableStateOf(photoPath) }
    var showSheet by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, currentPath) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (currentPath != null && java.io.File(currentPath!!).exists())
                BitmapFactory.decodeFile(currentPath!!)?.asImageBitmap()
            else null
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val path = ImageUtils.saveGamePhotoFromUri(context, uri, System.currentTimeMillis().toInt())
            if (path != null) { currentPath = path; onPhotoSaved(path) }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = tempCameraUri ?: return@rememberLauncherForActivityResult
            val path = ImageUtils.saveGamePhotoFromUri(context, uri, System.currentTimeMillis().toInt())
            if (path != null) { currentPath = path; onPhotoSaved(path) }
        }
    }

    val launchCamera = {
        try {
            val file = ImageUtils.createTempGamePhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) { e.printStackTrace() }
    }

    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (bitmap != null) Modifier else Modifier.height(100.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(CarcBg3)
            .then(if (editable) Modifier.clickable { showSheet = true } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val safeBitmap = bitmap
        if (safeBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = safeBitmap,
                contentDescription = "Game photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
            // Полупрозрачный оверлей снизу если editable
            if (editable) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.change_photo), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🗺️", fontSize = 36.sp)
                if (editable) {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.add_board_photo), fontSize = 12.sp, color = CarcText3)
                }
            }
        }
    }

    if (showSheet && editable) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = CarcCard2
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 40.dp)) {
                Text(stringResource(R.string.board_photo_title), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Камера
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(CarcCard)
                            .border(1.dp, CarcBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                showSheet = false
                                val perm = Manifest.permission.CAMERA
                                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, perm
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (granted) launchCamera() else cameraPermission.launch(perm)
                            }
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", fontSize = 28.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.camera_label), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    // Галерея
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .background(CarcCard)
                            .border(1.dp, CarcBorder, RoundedCornerShape(12.dp))
                            .clickable { showSheet = false; galleryLauncher.launch("image/*") }
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🖼️", fontSize = 28.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.gallery_label), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (currentPath != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { currentPath = null; onPhotoSaved(""); showSheet = false }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.remove_photo), fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

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
        contentPadding = PaddingValues(16.dp),
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

    Column(Modifier.fillMaxSize().imePadding()) {
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
                    bottom = if (selecting) 96.dp else 72.dp
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

                item { Spacer(Modifier.height(0.dp)) }
            }
        }

        // Bottom delete bar
        if (selecting) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444))
                    .clickable { showConfirm = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.delete_n_games, selected.size),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White
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

    Column(Modifier.fillMaxSize().imePadding()) {
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
                    bottom = if (selecting) 96.dp else 72.dp
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

                item { Spacer(Modifier.height(0.dp)) }
            }
        }

        // Bottom delete bar
        if (selecting) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444))
                    .clickable { showConfirm = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.delete_n_players, selected.size),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
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
            if (!selecting && stats != null && stats.gamesPlayed > 0) {
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
@Composable
fun StatsScreen(
    globalStats: GlobalStats,
    playerStats: List<PlayerStats>,
    compareSlots: List<Int?>,
    onSlotChange: (Int, Int?) -> Unit,
    allGamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity> = emptyList(),
    sectionMask: Int = 0b0011111,
    onSectionsChange: (Int) -> Unit = {},
    onPlayerClick: (Int) -> Unit = {}
) {
    var tab by remember { mutableIntStateOf(0) }
    // selectedSlots driven by VM — persisted across sessions
    val selectedSlots = compareSlots
    // Fixed slot colors — shared between dialog (StatsScreen) and ComparePlayersSection
    val slotColors = listOf(Color(0xFFEF4444), Color(0xFF22C55E), Color(0xFFEAB308))
    // Dialog must be outside LazyColumn to avoid Popup crash
    var pickingSlot by remember { mutableIntStateOf(-1) }

    // ── Player picker dialog — must be OUTSIDE LazyColumn ─────────────────
    if (pickingSlot >= 0) {
        val slotIdx = pickingSlot
        val currentId = selectedSlots.getOrNull(slotIdx)
        val available = playerStats.filter { candidate ->
            selectedSlots.indices.none { i -> i != slotIdx && selectedSlots[i] == candidate.player.id }
        }
        AlertDialog(
            onDismissRequest = { pickingSlot = -1 },
            containerColor = CarcCard2,
            title = { Text(stringResource(R.string.select_player), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    if (currentId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSlotChange(slotIdx, null); pickingSlot = -1 }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✕", fontSize = 14.sp, color = CarcRed,
                                modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.remove_player), fontSize = 14.sp, color = CarcRed)
                        }
                        HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp))
                    }
                    available.forEach { candidate ->
                        val c = slotColors.getOrElse(pickingSlot % 3) { CarcGreen }
                        val isSelected = candidate.player.id == currentId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) c.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    onSlotChange(slotIdx, candidate.player.id)
                                    pickingSlot = -1
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerAvatar(candidate.player.name, candidate.player.meepleColor,
                                avatarPath = candidate.player.avatarPath, size = 32.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(candidate.player.name, fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) c else CarcText)
                                Text(stringResource(R.string.wr_avg, (candidate.winRate * 100).toInt(), candidate.avgScore),
                                    fontSize = 11.sp, color = CarcText3)
                            }
                            if (isSelected) {
                                Spacer(Modifier.width(6.dp))
                                Text("✓", fontSize = 14.sp, color = c)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickingSlot = -1 }) {
                    Text(stringResource(R.string.cancel), color = CarcText3)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
                    .background(CarcCard)
                    .border(1.dp, CarcBorder, RoundedCornerShape(50.dp))
                    .padding(4.dp)
            ) {
                listOf(stringResource(R.string.tab_global_stats), stringResource(R.string.tab_compare_players)).forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (tab == i) CarcGreen else Color.Transparent)
                            .clickable { tab = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = if (tab == i) CarcBg else CarcText3)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (tab == 0) {
            // ── GLOBAL STATS TAB ──────────────────────────────────────────────
            item {
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("🃏", stringResource(R.string.total_games),    globalStats.totalGames.toString(),            Modifier.weight(1f).fillMaxHeight(), CarcText)
                    StatCard("⭐", stringResource(R.string.highest_score), globalStats.highestScore.toString(),           Modifier.weight(1f).fillMaxHeight(), CarcYellow)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("🏆", stringResource(R.string.avg_winner_score), "%.0f".format(globalStats.avgWinnerScore), Modifier.weight(1f).fillMaxHeight(), CarcGreen)
                    StatCard("👤", stringResource(R.string.players_section),            globalStats.totalPlayers.toString(),       Modifier.weight(1f).fillMaxHeight(), CarcBlue)
                }
                Spacer(Modifier.height(16.dp))
            }
            // Metagame breakdown bar
            if (globalStats.avgScore > 0f) {
                item {
                    MetagameBreakdownBar(globalStats)
                    Spacer(Modifier.height(16.dp))
                }
            }
            item {
                Text(stringResource(R.string.top_players), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (playerStats.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), Alignment.Center) {
                        Text(stringResource(R.string.no_stats), color = CarcText3)
                    }
                }
            } else {
                items(playerStats.take(5)) { ps ->
                    StatsPlayerRow(ps, onPlayerClick)
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            // ── COMPARE PLAYERS TAB ───────────────────────────────────────────
            if (playerStats.size < 2) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.need_2_players), color = CarcText2, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.play_more), color = CarcText3, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                item {
                    ComparePlayersSection(
                        allStats = playerStats,
                        selectedSlots = selectedSlots,
                        slotColors = slotColors,
                        allGamePlayers = allGamePlayers,
                        sectionMask = sectionMask,
                        onSectionsChange = onSectionsChange,
                        onSlotClick = { slotIdx -> pickingSlot = slotIdx }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun MetagameBreakdownBar(gs: GlobalStats) {
    val total = gs.avgCity + gs.avgRoad + gs.avgMonastery + gs.avgFarm
    if (total <= 0f) return
    val segments = listOf(
        Triple(stringResource(R.string.metagame_cities),      gs.avgCity,      CarcBlue),
        Triple(stringResource(R.string.metagame_roads),       gs.avgRoad,      CarcGreen),
        Triple(stringResource(R.string.metagame_monasteries), gs.avgMonastery, CarcYellow),
        Triple(stringResource(R.string.metagame_farms),       gs.avgFarm,      CarcOrange)
    )
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Column(Modifier.padding(14.dp)) {
            // Stacked bar
            Row(modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(6.dp))) {
                segments.forEach { (_, value, color) ->
                    val frac = (value / total).coerceIn(0f, 1f)
                    if (frac > 0.02f) {
                        Box(Modifier.fillMaxHeight().weight(frac).background(color))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // Legend
            segments.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth()) {
                    pair.forEach { (label, value, color) ->
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(label, fontSize = 11.sp, color = CarcText2)
                                Text(stringResource(R.string.avg_pts, value), fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold, color = CarcText)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun ComparePlayersSection(
    allStats: List<PlayerStats>,
    selectedSlots: List<Int?>,
    slotColors: List<Color>,
    allGamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity>,
    sectionMask: Int = 0b0011111,
    onSectionsChange: (Int) -> Unit = {},
    onSlotClick: (slotIdx: Int) -> Unit
) {
    val slotStats = selectedSlots.map { id -> allStats.find { it.player.id == id } }
    val activePlayers = slotStats.filterNotNull()
    val activeSlotIndices = selectedSlots.indices.filter { selectedSlots[it] != null }
    val colors = activePlayers.indices.map { ai -> slotColors[activeSlotIndices.getOrElse(ai) { ai } % 3] }

    // ── Slot cards ─────────────────────────────────────────────────────────
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedSlots.forEachIndexed { slotIdx, selectedId ->
                val ps = allStats.find { it.player.id == selectedId }
                val slotColor = if (ps != null) slotColors[slotIdx % 3] else CarcBorder
                Column(
                    modifier = Modifier
                        .weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarcBg2)
                        .border(1.5.dp, slotColor, RoundedCornerShape(10.dp))
                        .clickable { onSlotClick(slotIdx) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (ps != null) {
                        PlayerAvatar(ps.player.name, ps.player.meepleColor,
                            avatarPath = ps.player.avatarPath, size = 36.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(ps.player.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = slotColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center)
                        Text(stringResource(R.string.avg_score_label, ps.avgScore), fontSize = 10.sp, color = CarcText3)
                    } else {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(50.dp))
                            .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(50.dp)),
                            contentAlignment = Alignment.Center) {
                            Text("+", fontSize = 18.sp, color = CarcText3)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.add_btn), fontSize = 11.sp, color = CarcText3)
                        Text(stringResource(R.string.player_label), fontSize = 10.sp, color = CarcText3)
                    }
                }
            }
        }
    }

    if (activePlayers.size < 2) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Text(stringResource(R.string.select_2_compare), color = CarcText3, fontSize = 13.sp)
        }
        return
    }

    Spacer(Modifier.height(12.dp))

    // ── Section selector ───────────────────────────────────────────────────
    // sections: index -> (label, enabled)
    data class CompareSection(val label: String, var enabled: Boolean)
    val sectionLabels = listOf(
        stringResource(R.string.section_label_play_style),
        stringResource(R.string.section_label_results),
        stringResource(R.string.section_label_score_range),
        stringResource(R.string.section_label_together),
        stringResource(R.string.section_label_trend),
        stringResource(R.string.section_label_score_struct),
        stringResource(R.string.section_label_metrics)
    )
    val sections = remember(sectionMask) {
        mutableStateListOf(*sectionLabels.mapIndexed { i, label ->
            CompareSection(label, sectionMask and (1 shl i) != 0)
        }.toTypedArray())
    }
    var showSectionPicker by remember { mutableStateOf(false) }
    if (showSectionPicker) {
        AlertDialog(
            onDismissRequest = { showSectionPicker = false },
            containerColor = CarcCard2,
            title = { Text(stringResource(R.string.show_comparisons), fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    sections.forEachIndexed { i, sec ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    sections[i] = sec.copy(enabled = !sec.enabled)
                                    val newMask = sections.foldIndexed(0) { idx, acc, s ->
                                        if (s.enabled) acc or (1 shl idx) else acc }
                                    onSectionsChange(newMask)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sec.enabled,
                                onCheckedChange = { v ->
                                    sections[i] = sec.copy(enabled = v)
                                    val newMask = sections.foldIndexed(0) { idx, acc, s ->
                                        if (s.enabled) acc or (1 shl idx) else acc }
                                    onSectionsChange(newMask)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = CarcGreen,
                                    uncheckedColor = CarcBorder
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(sec.label, fontSize = 14.sp, color = CarcText)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSectionPicker = false }) {
                    Text(stringResource(R.string.done_btn), color = CarcGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Header row with gear button
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.compare_players), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
            modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CarcCard)
                .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                .clickable { showSectionPicker = true },
            contentAlignment = Alignment.Center
        ) { Text("⚙️", fontSize = 14.sp) }
    }

    Spacer(Modifier.height(10.dp))

    // Player legend (shown always)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        activePlayers.forEachIndexed { i, ps ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                Spacer(Modifier.width(5.dp))
                Text(ps.player.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors[i])
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    // ── 0: PLAY STYLE ──────────────────────────────────────────────────────
    if (sections[0].enabled) {
        SectionLabel(stringResource(R.string.section_play_style))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp)) {
                RadarChart(players = activePlayers, colors = colors,
                    modifier = Modifier.fillMaxWidth().height(280.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 1: RESULTS ─────────────────────────────────────────────────────────
    if (sections[1].enabled) {
        SectionLabel(stringResource(R.string.section_results))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                activePlayers.forEachIndexed { i, ps ->
                    val losses = ps.gamesPlayed - ps.wins
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 12.sp, color = colors[i],
                            modifier = Modifier.width(70.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        // Win bar
                        val winFrac = ps.winRate.coerceIn(0f, 1f)
                        Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(4.dp))
                            .background(CarcBg3)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(winFrac)
                                .background(colors[i]))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.wins_losses, ps.wins, losses),
                            fontSize = 11.sp, color = CarcText2,
                            modifier = Modifier.widthIn(min = 70.dp), textAlign = TextAlign.End)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 2.dp))
                Spacer(Modifier.height(6.dp))
                // Win rate numbers side by side
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    activePlayers.forEachIndexed { i, ps ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${(ps.winRate * 100).toInt()}%",
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors[i])
                            Text(stringResource(R.string.win_rate), fontSize = 10.sp, color = CarcText3)
                        }
                    }
                }
                // Placement breakdown
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.placements), fontSize = 11.sp, color = CarcText3)
                Spacer(Modifier.height(6.dp))
                val placeColors = listOf(CarcYellow, Color(0xFF9CA3AF), Color(0xFFB45309), Color(0xFF4B5563))
                val placeIcons  = listOf("🥇", "🥈", "🥉", "4️⃣")
                // Header row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(14.dp + 6.dp + 70.dp + 6.dp))
                    placeIcons.forEach { icon ->
                        Text(icon, fontSize = 13.sp, modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(4.dp))
                activePlayers.forEachIndexed { i, ps ->
                    val playerGames = allGamePlayers.filter { it.playerId == ps.player.id }
                    val byPlace = (1..4).map { place ->
                        playerGames.count { it.placement == place }
                    }
                    val total = playerGames.size.takeIf { it > 0 } ?: 1
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 11.sp, color = colors[i],
                            modifier = Modifier.width(70.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        byPlace.forEachIndexed { pi, cnt ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // bar
                                val frac = cnt.toFloat() / total
                                Box(Modifier.width(28.dp).height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CarcBg3)) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth(frac)
                                        .background(placeColors[pi]))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("$cnt", fontSize = 13.sp,
                                    fontWeight = if (cnt == byPlace.max()) FontWeight.Bold else FontWeight.Normal,
                                    color = if (cnt > 0) placeColors[pi] else CarcText3,
                                    textAlign = TextAlign.Center)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 2: SCORE RANGE ─────────────────────────────────────────────────────
    if (sections[2].enabled) {
        SectionLabel(stringResource(R.string.section_score_range))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                val globalMax = activePlayers.maxOf { it.maxScore }.toFloat().takeIf { it > 0f } ?: 1f
                activePlayers.forEachIndexed { i, ps ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 11.sp, color = colors[i],
                            modifier = Modifier.width(70.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.weight(1f).height(18.dp)) {
                            val maxFrac = (ps.maxScore / globalMax).coerceIn(0f, 1f)
                            val avgFrac = (ps.avgScore / globalMax).coerceIn(0f, 1f)
                            // Background range bar (0 → max)
                            Box(Modifier.fillMaxHeight().fillMaxWidth(maxFrac)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors[i].copy(alpha = 0.2f)))
                            // Avg bar (0 → avg), brighter
                            Box(Modifier.fillMaxHeight().fillMaxWidth(avgFrac)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors[i].copy(alpha = 0.7f)))
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(70.dp)) {
                            Text("▲ ${ps.maxScore}", fontSize = 10.sp, color = CarcGreen, fontWeight = FontWeight.Bold)
                            Text("● ${ps.avgScore.toInt()}", fontSize = 10.sp, color = colors[i])
                            Text("▼ ${ps.minScore}", fontSize = 10.sp, color = CarcText3)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 3: TOGETHER ────────────────────────────────────────────────────────
    if (sections[3].enabled && activePlayers.size >= 2) {
        SectionLabel(stringResource(R.string.section_together))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                // Games together matrix — all pairs
                val pairs = mutableListOf<Triple<PlayerStats, PlayerStats, Int>>()
                for (a in activePlayers.indices) {
                    for (b in a+1 until activePlayers.size) {
                        val pa = activePlayers[a]; val pb = activePlayers[b]
                        val gamesA = allGamePlayers.filter { it.playerId == pa.player.id }
                            .map { it.gameId }.toSet()
                        val gamesB = allGamePlayers.filter { it.playerId == pb.player.id }
                            .map { it.gameId }.toSet()
                        val together = gamesA.intersect(gamesB).size
                        pairs.add(Triple(pa, pb, together))
                    }
                }
                pairs.forEach { (pa, pb, count) ->
                    val idxA = activePlayers.indexOf(pa)
                    val idxB = activePlayers.indexOf(pb)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        Spacer(Modifier.width(8.dp))
                        Text(pa.player.name, fontSize = 12.sp, color = colors[idxA],
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.widthIn(max = 90.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("  &  ", fontSize = 11.sp, color = CarcText3)
                        Text(pb.player.name, fontSize = 12.sp, color = colors[idxB],
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.widthIn(max = 90.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.weight(1f))
                        Text("$count", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CarcYellow)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.games_label), fontSize = 11.sp, color = CarcText3)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                // Head-to-head wins (who won more when played together)
                if (activePlayers.size == 2) {
                    val pa = activePlayers[0]; val pb = activePlayers[1]
                    val sharedGames = allGamePlayers.filter { it.playerId == pa.player.id }
                        .map { it.gameId }.toSet()
                        .intersect(allGamePlayers.filter { it.playerId == pb.player.id }
                            .map { it.gameId }.toSet())
                    val winsA = allGamePlayers.filter { it.playerId == pa.player.id
                        && it.gameId in sharedGames && it.placement == 1 }.size
                    val winsB = allGamePlayers.filter { it.playerId == pb.player.id
                        && it.gameId in sharedGames && it.placement == 1 }.size
                    if (sharedGames.isNotEmpty()) {
                        HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 6.dp))
                        Text(stringResource(R.string.head_to_head), fontSize = 11.sp, color = CarcText3)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$winsA", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors[0])
                                Text(pa.player.name, fontSize = 11.sp, color = colors[0],
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.vs), fontSize = 16.sp, color = CarcText3,
                                    modifier = Modifier.padding(top = 8.dp))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$winsB", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = colors[1])
                                Text(pb.player.name, fontSize = 11.sp, color = colors[1],
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 4: TREND ───────────────────────────────────────────────────────────
    if (sections[4].enabled) {
        SectionLabel(stringResource(R.string.section_trend))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp)) {
                val trendTextPaint = remember {
                    android.graphics.Paint().apply {
                        isAntiAlias = true; textSize = 34f; textAlign = android.graphics.Paint.Align.CENTER
                        style = android.graphics.Paint.Style.FILL
                    }
                }
                val trendStrokePaint = remember {
                    android.graphics.Paint().apply {
                        isAntiAlias = true; textSize = 34f; textAlign = android.graphics.Paint.Align.CENTER
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 5f
                        color = android.graphics.Color.argb(220, 10, 26, 10)
                        strokeJoin = android.graphics.Paint.Join.ROUND
                    }
                }
                val isDarkTrend = LocalCarcColors.current.isDark
                // Mini line chart with score labels
                val allScoresTrend = activePlayers.flatMap { it.recentScores }
                if (allScoresTrend.isNotEmpty()) {
                    val globalMinT = allScoresTrend.min().toFloat()
                    val globalMaxT = allScoresTrend.max().toFloat()
                    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        val range = (globalMaxT - globalMinT).takeIf { it > 0f } ?: 1f
                        val w = size.width; val h = size.height
                        val padTop = 36f; val padBottom = 8f
                        val chartH = h - padTop - padBottom
                        val n = 5
                        val stepX = w / (n - 1).toFloat()
                        fun scoreY(sc: Int) = padTop + chartH - (sc - globalMinT) / range * chartH

                        // Grid
                        val gridColor = if (isDarkTrend) Color.White.copy(alpha = 0.07f)
                                        else Color(0xFF336633).copy(alpha = 0.15f)
                        repeat(4) { ri ->
                            val y = padTop + chartH * ri / 3f
                            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        }
                        // Lines
                        activePlayers.forEachIndexed { pi, ps ->
                            val scores = ps.recentScores.take(n).reversed()
                            if (scores.size < 2) return@forEachIndexed
                            val linePath = Path()
                            scores.forEachIndexed { si, sc ->
                                val x = si * stepX; val y = scoreY(sc)
                                if (si == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                            }
                            drawPath(linePath, colors[pi], style = Stroke(width = 2.5f))
                        }
                        // Dots + score labels (drawn after lines so labels are on top)
                        activePlayers.forEachIndexed { pi, ps ->
                            val scores = ps.recentScores.take(n).reversed()
                            scores.forEachIndexed { si, sc ->
                                val x = si * stepX; val y = scoreY(sc)
                                drawCircle(colors[pi], radius = 5f, center = Offset(x, y))
                                drawCircle(Color(0xFF0D1F0D), radius = 2.5f, center = Offset(x, y))
                            }
                        }
                        // Score text labels — smart placement to avoid overlap
                        drawIntoCanvas { cv ->
                            val minGap = 40f
                            val lblX = mutableListOf<Float>()
                            val lblY = mutableListOf<Float>()
                            val lblText = mutableListOf<String>()
                            val lblColor = mutableListOf<Int>()
                            activePlayers.forEachIndexed { pi, ps ->
                                val scores = ps.recentScores.take(n).reversed()
                                val colorInt = android.graphics.Color.argb(
                                    255,
                                    (colors[pi].red * 255).toInt(),
                                    (colors[pi].green * 255).toInt(),
                                    (colors[pi].blue * 255).toInt()
                                )
                                scores.forEachIndexed { si, sc ->
                                    val x = si * stepX
                                    val y = scoreY(sc)
                                    val initY = if (y < padTop + 20f) y + 32f else y - 12f
                                    lblX.add(x); lblY.add(initY)
                                    lblText.add("$sc"); lblColor.add(colorInt)
                                }
                            }
                            val cols = lblX.distinct()
                            cols.forEach { cx ->
                                val idx = lblX.indices.filter { lblX[it] == cx }
                                    .sortedBy { lblY[it] }
                                for (i in 1 until idx.size) {
                                    val prev = idx[i - 1]; val curr = idx[i]
                                    if (lblY[curr] - lblY[prev] < minGap)
                                        lblY[curr] = lblY[prev] + minGap
                                }
                            }
                            lblX.indices.forEach { i ->
                                val clampedY = lblY[i].coerceIn(padTop, h - 4f)
                                // Stroke (dark outline) — draw first, behind the fill
                                cv.nativeCanvas.drawText(lblText[i], lblX[i], clampedY, trendStrokePaint)
                                // Fill (player colour)
                                trendTextPaint.color = lblColor[i]
                                cv.nativeCanvas.drawText(lblText[i], lblX[i], clampedY, trendTextPaint)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                // Legend: player row with stats
                activePlayers.forEachIndexed { i, ps ->
                    val scores = ps.recentScores.take(5).reversed()
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()) {
                        // Color line sample
                        Box(Modifier.width(18.dp).height(3.dp)
                            .clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50.dp))
                            .background(colors[i]).align(Alignment.CenterVertically))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 12.sp, color = colors[i],
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(56.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(8.dp))
                        // Score sequence — equal-width cells for table alignment
                        Row(modifier = Modifier.weight(1f)) {
                            repeat(5) { idx ->
                                val sc = scores.getOrNull(idx)
                                Text(
                                    sc?.toString() ?: "·",
                                    fontSize = 12.sp,
                                    color = if (sc != null) CarcText2 else CarcText3,
                                    fontWeight = if (sc != null && sc == scores.maxOrNull()) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 5: SCORE STRUCTURE ─────────────────────────────────────────────────
    if (sections[5].enabled) {
        SectionLabel(stringResource(R.string.section_score_struct))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp)) {
                val maxAvg = activePlayers.maxOf { it.avgScore }.takeIf { it > 0f } ?: 1f
                val catColors = listOf(CarcBlue, CarcGreen, CarcYellow, CarcOrange)
                val catLabels = listOf("🏰", "🛤️", "⛪", "🌾")
                activePlayers.forEachIndexed { i, ps ->
                    val cats = listOf(ps.avgCity, ps.avgRoad, ps.avgMonastery, ps.avgFarm)
                    val catTotal = cats.sum().takeIf { it > 0f } ?: 1f
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(6.dp))
                        Text(ps.player.name, fontSize = 11.sp, color = CarcText2,
                            modifier = Modifier.width(64.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        Row(modifier = Modifier.weight(ps.avgScore / maxAvg).height(18.dp)
                            .clip(RoundedCornerShape(4.dp))) {
                            cats.forEachIndexed { ci, v ->
                                val frac = (v / catTotal).coerceIn(0f, 1f)
                                if (frac > 0.02f)
                                    Box(Modifier.fillMaxHeight().weight(frac).background(catColors[ci]))
                            }
                        }
                        if (ps.avgScore / maxAvg < 1f) Spacer(Modifier.weight(1f - ps.avgScore / maxAvg))
                        Spacer(Modifier.width(6.dp))
                        Text("%.0f".format(ps.avgScore), fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, color = CarcText)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    catColors.forEachIndexed { i, c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(c))
                            Spacer(Modifier.width(4.dp))
                            Text(catLabels[i], fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }

    // ── 6: METRICS ─────────────────────────────────────────────────────────
    if (sections[6].enabled) {
        SectionLabel(stringResource(R.string.section_metrics))
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Column(Modifier.padding(14.dp)) {
                val metrics = listOf(
                    Triple(stringResource(R.string.metric_urbanization),   activePlayers.map { it.urbanizationIndex }, stringResource(R.string.metric_urbanization_desc)),
                    Triple(stringResource(R.string.metric_road_aggr),     activePlayers.map { it.roadAggrIndex },    stringResource(R.string.metric_road_aggr_desc)),
                    Triple(stringResource(R.string.metric_monastery),      activePlayers.map { it.monasteryIndex },   stringResource(R.string.metric_monastery_desc)),
                    Triple(stringResource(R.string.metric_farm_dom), activePlayers.map { it.farmDomIndex },     stringResource(R.string.metric_farm_dom_desc)),
                    Triple(stringResource(R.string.metric_stability),      activePlayers.map { it.stabilityIndex },   stringResource(R.string.metric_stability_desc))
                )
                metrics.forEachIndexed { mi, (label, values, hint) ->
                    if (mi > 0) HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 6.dp))
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
                    Text(hint, fontSize = 10.sp, color = CarcText3)
                    Spacer(Modifier.height(6.dp))
                    val maxV = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
                    activePlayers.forEachIndexed { pi, ps ->
                        val v = values[pi]; val isWinner = v == maxV
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).clip(RoundedCornerShape(2.dp)).background(colors[pi]))
                            Spacer(Modifier.width(5.dp))
                            Text(ps.player.name, fontSize = 10.sp, color = colors[pi],
                                modifier = Modifier.width(52.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(CarcBg3)) {
                                Box(Modifier.fillMaxHeight().fillMaxWidth(v / maxV)
                                    .background(colors[pi].copy(alpha = if (isWinner) 1f else 0.55f)))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("%.0f%%".format(v * 100), fontSize = 11.sp,
                                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                                color = if (isWinner) colors[pi] else CarcText3,
                                modifier = Modifier.width(40.dp), textAlign = TextAlign.End,
                                maxLines = 1)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun RadarChart(
    players: List<PlayerStats>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val axisLabels = listOf(
        stringResource(R.string.radar_urban),
        stringResource(R.string.radar_roads),
        stringResource(R.string.radar_monks),
        stringResource(R.string.radar_farms),
        stringResource(R.string.radar_stable)
    )
    val playerValues = players.map { ps ->
        listOf(ps.urbanizationIndex, ps.roadAggrIndex, ps.monasteryIndex, ps.farmDomIndex, ps.stabilityIndex)
    }
    val n = 5
    val angleStep = (2 * Math.PI / n).toFloat()
    val startAngle = (-Math.PI / 2).toFloat()

    // Label offsets calculated outside Canvas so Compose Text can use them
    // We use BoxWithConstraints to know size at composition time
    val isDark = LocalCarcColors.current.isDark
    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(cx, cy) * 0.62f
        val labelRadius = radius + 44f

        fun axisAngle(i: Int) = startAngle + i * angleStep
        fun axisPoint(i: Int, frac: Float) = Offset(
            cx + radius * frac * cos(axisAngle(i)),
            cy + radius * frac * sin(axisAngle(i))
        )
        fun labelCenter(i: Int) = Offset(
            cx + labelRadius * cos(axisAngle(i)),
            cy + labelRadius * sin(axisAngle(i))
        )

        // Canvas — grid + polygons only
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridAlpha = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF336633).copy(alpha = 0.25f)
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { r ->
                val path = Path()
                for (i in 0 until n) {
                    val pt = axisPoint(i, r)
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                path.close()
                drawPath(path, gridAlpha, style = Stroke(width = 1f))
            }
            for (i in 0 until n) {
                drawLine(gridAlpha, Offset(cx, cy), axisPoint(i, 1f), strokeWidth = 1f)
            }
            playerValues.forEachIndexed { pi, values ->
                val path = Path()
                values.forEachIndexed { i, v ->
                    val pt = axisPoint(i, v.coerceIn(0.05f, 1f))
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                path.close()
                drawPath(path, colors[pi].copy(alpha = 0.18f))
                drawPath(path, colors[pi], style = Stroke(width = 2.5f))
                values.forEachIndexed { i, v ->
                    drawCircle(colors[pi], radius = 5f, center = axisPoint(i, v.coerceIn(0.05f, 1f)))
                }
            }
        }

        // Axis labels as Compose Text — positioned via offset
        val density = androidx.compose.ui.platform.LocalDensity.current
        for (i in 0 until n) {
            val lc = labelCenter(i)
            val xDp = with(density) { lc.x.toDp() }
            val yDp = with(density) { lc.y.toDp() }
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .wrapContentHeight()
                    .offset(x = xDp - 50.dp, y = yDp - 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    axisLabels[i],
                    fontSize = 9.sp,
                    color = CarcText2,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun StatsPlayerRow(ps: PlayerStats, onPlayerClick: (Int) -> Unit = {}) {
    val accent = meepleColor(ps.player.meepleColor)
    Card(
        modifier = Modifier.clickable { onPlayerClick(ps.player.id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(ps.player.name, ps.player.meepleColor, avatarPath = ps.player.avatarPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ps.player.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.wins_losses, ps.wins, ps.gamesPlayed - ps.wins), fontSize = 11.sp, color = CarcText3)
                Spacer(Modifier.height(4.dp))
                WinRateBar(ps.winRate)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "${(ps.winRate * 100).toInt()}%",
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (ps.winRate >= 0.5f) CarcGreen else CarcText2
            )
        }
    }
}

// ─── Restore From Folder Dialog ───────────────────────────────────────────────
  @Composable
  fun RestoreFromFolderDialog(
      files: List<DocumentFile>,
      onSelect: (DocumentFile) -> Unit,
      onDismiss: () -> Unit
  ) {
      var confirmFile by remember { mutableStateOf<DocumentFile?>(null) }
      if (confirmFile != null) {
          AlertDialog(
              onDismissRequest = { confirmFile = null },
              title = { Text(stringResource(R.string.restore_confirm_title)) },
              text = { Text(stringResource(R.string.restore_confirm_msg, confirmFile!!.name ?: ""), color = CarcText2) },
              confirmButton = {
                  TextButton(onClick = { onSelect(confirmFile!!); confirmFile = null; onDismiss() }) {
                      Text(stringResource(R.string.restore_btn), color = CarcGreen, fontWeight = FontWeight.Bold)
                  }
              },
              dismissButton = { TextButton(onClick = { confirmFile = null }) { Text(stringResource(R.string.cancel)) } },
              containerColor = CarcCard2
          )
      } else {
          AlertDialog(
              onDismissRequest = onDismiss,
              title = { Text(stringResource(R.string.restore_pick_title)) },
              text = {
                  Column {
                      if (files.isEmpty()) {
                          Text(stringResource(R.string.restore_no_files_in_folder), color = CarcText2, fontSize = 14.sp)
                      } else {
                          Text(stringResource(R.string.backup_folder_files_hint), color = CarcText3, fontSize = 11.sp,
                              modifier = Modifier.padding(bottom = 12.dp))
                          files.forEach { doc ->
                              val modDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(java.util.Date(doc.lastModified()))
                              val sizeKb = doc.length() / 1024
                              Row(
                                  modifier = Modifier.fillMaxWidth().clickable { confirmFile = doc }.padding(vertical = 10.dp, horizontal = 4.dp),
                                  verticalAlignment = Alignment.CenterVertically
                              ) {
                                  Text("💾", fontSize = 22.sp)
                                  Spacer(Modifier.width(12.dp))
                                  Column(Modifier.weight(1f)) {
                                      Text(doc.name ?: "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
                                      Text("$modDate · $sizeKb KB", fontSize = 11.sp, color = CarcText3)
                                  }
                              }
                              HorizontalDivider(color = CarcText3.copy(alpha = 0.15f))
                          }
                      }
                  }
              },
              confirmButton = {},
              dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
              containerColor = CarcCard2
          )
      }
  }

  // ─── Restore Picker Dialog ────────────────────────────────────────────────────
@Composable
fun RestorePickerDialog(
    files: List<File>,
    onSelect: (File) -> Unit,
    onDismiss: () -> Unit
) {
    var confirmFile by remember { mutableStateOf<File?>(null) }

    if (confirmFile != null) {
        AlertDialog(
            onDismissRequest = { confirmFile = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_msg, confirmFile!!.name), color = CarcText2) },
            confirmButton = {
                TextButton(onClick = { onSelect(confirmFile!!); confirmFile = null; onDismiss() }) {
                    Text(stringResource(R.string.restore_btn), color = CarcGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFile = null }) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = CarcCard2
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.restore_pick_title)) },
            text = {
                Column {
                    if (files.isEmpty()) {
                        Text(stringResource(R.string.restore_no_files), color = CarcText2, fontSize = 14.sp)
                    } else {
                        Text(stringResource(R.string.backup_location), color = CarcText3, fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp))
                        files.forEach { f ->
                            val modDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(java.util.Date(f.lastModified()))
                            val sizeKb = f.length() / 1024
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { confirmFile = f }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💾", fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(f.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
                                    Text("$modDate · $sizeKb KB", fontSize = 11.sp, color = CarcText3)
                                }
                            }
                            HorizontalDivider(color = CarcText3.copy(alpha = 0.15f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = CarcCard2
        )
    }
}

// ─── Settings Screen ─────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    onBackup: () -> Unit,
    onRestore: () -> Unit = {},
    onPickBackupFolder: () -> Unit = {},
    backupFolderName: String? = null,
    onClearAll: () -> Unit,
    isDarkMode: Boolean = true,
    onDarkMode: (Boolean) -> Unit = {}
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_all_title)) },
            text = { Text(stringResource(R.string.clear_all_msg), color = CarcText2) },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearDialog = false }) {
                    Text(stringResource(R.string.delete_all), color = CarcRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = CarcCard2
        )
    }

    if (showLanguageDialog) {
        val ctx = LocalContext.current
        val currentLang = remember {
            val loc = AppCompatDelegate.getApplicationLocales()
            if (!loc.isEmpty) loc[0]?.language ?: Locale.getDefault().language else Locale.getDefault().language
        }
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_title)) },
            text = {
                Column {
                    listOf("en" to "English", "ru" to "Русский").forEach { (code, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    AppCompatDelegate.setApplicationLocales(
                                        LocaleListCompat.forLanguageTags(code)
                                    )
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = code == currentLang,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = CarcGreen)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp, color = CarcText)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = CarcCard2
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Text(stringResource(R.string.settings_appearance), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        item {
            SettingsRow("🌙", stringResource(R.string.dark_mode), stringResource(R.string.dark_mode_sub),
                onClick = { onDarkMode(!isDarkMode) },
                trailing = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onDarkMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CarcBg, checkedTrackColor = CarcGreen)
                    )
                }
            )
        }
        item {
            SettingsRow("🌐", stringResource(R.string.language),
            run {
                val loc = AppCompatDelegate.getApplicationLocales()
                val lang = if (!loc.isEmpty) loc[0]?.language ?: Locale.getDefault().language else Locale.getDefault().language
                if (lang == "ru") "Русский" else "English"
            },
            onClick = { showLanguageDialog = true })
        }
        item {
            Text(stringResource(R.string.settings_data), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        }
        item {
            SettingsRow(
                "📁", stringResource(R.string.backup_folder),
                backupFolderName ?: stringResource(R.string.backup_folder_default),
                onClick = onPickBackupFolder,
                trailing = { Text("›", fontSize = 20.sp, color = CarcText2) }
            )
        }
        item { SettingsRow("☁️", stringResource(R.string.backup_data), stringResource(R.string.backup_sub), onClick = onBackup) }
        item { SettingsRow("📥", stringResource(R.string.restore_data), stringResource(R.string.restore_sub), onClick = onRestore) }
        item {
            SettingsRow(
                "🗑️", stringResource(R.string.clear_all), stringResource(R.string.clear_all_sub),
                onClick = { showClearDialog = true },
                iconBgColor = CarcRed.copy(alpha = 0.1f),
                trailing = { Text("›", fontSize = 20.sp, color = CarcRed) }
            )
        }
        item {
            Text(stringResource(R.string.settings_app_section), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        }
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CarcCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.app_version), color = CarcGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(stringResource(R.string.app_built_with), color = CarcText3, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

// ─── Avatar Picker Section ────────────────────────────────────────────────────
@Composable
fun AvatarPickerSection(
    name: String,
    color: String,
    avatarPath: String?,
    onPickGallery: () -> Unit,
    onPickCamera: () -> Unit,
    onRemoveAvatar: () -> Unit,
    size: Dp = 88.dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            PlayerAvatar(name.ifBlank { "?" }, color, size = size, avatarPath = avatarPath)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CarcGreen)
                    .border(2.dp, CarcBg, CircleShape)
                    .clickable { onPickGallery() },
                contentAlignment = Alignment.Center
            ) {
                Text("📷", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onPickGallery) {
                Text(stringResource(R.string.gallery_label), fontSize = 12.sp, color = CarcGreen)
            }
            TextButton(onClick = onPickCamera) {
                Text(stringResource(R.string.camera_label), fontSize = 12.sp, color = CarcGreen)
            }
            if (avatarPath != null) {
                TextButton(onClick = onRemoveAvatar) {
                    Text(stringResource(R.string.remove_btn), fontSize = 12.sp, color = CarcRed)
                }
            }
        }
    }
}

// ─── Permission-aware launcher helpers ───────────────────────────────────────
@Composable
fun rememberGalleryLauncher(
    onResult: (android.net.Uri?) -> Unit
): Pair<() -> Unit, androidx.activity.result.ActivityResultLauncher<String>> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(), onResult
    )
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launcher.launch("image/*") }

    val open: () -> Unit = {
        val perm = if (android.os.Build.VERSION.SDK_INT >= 33)
            android.Manifest.permission.READ_MEDIA_IMAGES
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        val state = androidx.core.content.ContextCompat.checkSelfPermission(context, perm)
        if (state == android.content.pm.PackageManager.PERMISSION_GRANTED)
            launcher.launch("image/*")
        else
            permLauncher.launch(perm)
    }
    return Pair(open, launcher)
}

@Composable
fun rememberCameraLauncher(
    onImageSaved: (java.io.File) -> Unit
): Pair<() -> Unit, () -> Unit> {
    val context = androidx.compose.ui.platform.LocalContext.current
    var tempFile by remember { mutableStateOf<java.io.File?>(null) }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success -> if (success) tempFile?.let { onImageSaved(it) } }

    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = com.carcassonne.companion.util.ImageUtils.createTempImageFile(context)
            tempFile = file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
            cameraLauncher.launch(uri)
        }
    }

    val open: () -> Unit = {
        val perm = android.Manifest.permission.CAMERA
        val state = androidx.core.content.ContextCompat.checkSelfPermission(context, perm)
        if (state == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val file = com.carcassonne.companion.util.ImageUtils.createTempImageFile(context)
            tempFile = file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.provider", file
            )
            cameraLauncher.launch(uri)
        } else {
            permLauncher.launch(perm)
        }
    }
    return Pair(open, { tempFile = null })
}

// ─── Add Player Dialog ────────────────────────────────────────────────────────
@Composable
fun AddPlayerDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String?) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("red") }
    var avatarPath by remember { mutableStateOf<String?>(null) }

    val (openGallery, _) = rememberGalleryLauncher { uri ->
        uri?.let {
            val saved = com.carcassonne.companion.util.ImageUtils.saveImageFromUri(context, it, 0)
            if (saved != null) {
                com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
                avatarPath = saved
            }
        }
    }
    val (openCamera, _) = rememberCameraLauncher { file ->
        com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
        avatarPath = file.absolutePath
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarcCard2,
        title = { Text(stringResource(R.string.add_new_player), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AvatarPickerSection(
                    name = name, color = color, avatarPath = avatarPath,
                    onPickGallery = openGallery,
                    onPickCamera = openCamera,
                    onRemoveAvatar = {
                        com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
                        avatarPath = null
                    }
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.player_name_hint), color = CarcText3) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
                        focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.preferred_color), fontSize = 13.sp, color = CarcText2)
                Spacer(Modifier.height(8.dp))
                MeepleColorPicker(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) { onAdd(name.trim(), color, avatarPath); onDismiss() } },
                colors = ButtonDefaults.buttonColors(containerColor = CarcGreen, contentColor = CarcBg)
            ) { Text(stringResource(R.string.create_player), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CarcText3) }
        }
    )
}

// ─── New Game Screen ──────────────────────────────────────────────────────────
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
            items(players) { player ->
                val isSelected = player.id in selected
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
                                    if (isSelected) stringResource(R.string.tap_to_remove) else stringResource(R.string.tap_to_include),
                                    fontSize = 12.sp, color = CarcText3
                                )
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onTogglePlayer(player) },
                                colors = CheckboxDefaults.colors(checkedColor = CarcGreen, uncheckedColor = CarcText3)
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

// ─── Add Scoring Object Bottom Sheet ─────────────────────────────────────────
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddObjectSheet(
    playerName: String,
    meepleCol: String,
    startTab: Int = 0,
    hasInns: Boolean = true,
    onDismiss: () -> Unit,
    onScore: (ScoringObjectType, Int, String) -> Unit
) {
    val accent = meepleColor(meepleCol)
    var tab by remember { mutableStateOf(startTab) }

    // City state
    var cityTiles by remember { mutableStateOf(2) }
    var cityShields by remember { mutableStateOf(0) }
    var cityCathedral by remember { mutableStateOf(false) }
    val cityPts = cityTiles * (if (cityCathedral) 3 else 2) + cityShields * 2

    // Road state
    var roadTiles by remember { mutableStateOf(2) }
    var roadTavern by remember { mutableStateOf(false) }
    val roadPts = if (roadTavern) roadTiles * 2 else roadTiles

    // Monastery state
    var monTiles by remember { mutableStateOf(9) }
    val monPts = monTiles

    // Localized abbreviations (captured for use inside onClick lambdas)
    val tileAbbr = stringResource(R.string.tile_abbr)
    val innAbbr = stringResource(R.string.inn_abbr)
    val tilesWord = stringResource(R.string.tiles_word)
    val shieldsWord = stringResource(R.string.shields_word)
    val addCityLabel = stringResource(R.string.add_city_btn, cityPts)
    val addRoadLabel = stringResource(R.string.add_road_btn, roadPts)
    val addMonLabel = stringResource(R.string.add_monastery_btn, monPts)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CarcCard2,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), Alignment.Center) {
                Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(CarcBorder))
            }
        }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_object_title, playerName), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(16.dp))

            // Tab selector
            val tabItems = listOf(
                "🏰" to stringResource(R.string.city_btn),
                "🛤️" to stringResource(R.string.road_btn),
                "⛪" to stringResource(R.string.monastery_btn)
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CarcBg3),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabItems.forEachIndexed { i, (icon, label) ->
                    val sel = tab == i
                    Box(
                        Modifier.weight(1f).padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) accent.copy(alpha = 0.2f) else Color.Transparent)
                            .border(if (sel) 1.dp else 0.dp, if (sel) accent else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { tab = i }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(icon, fontSize = 18.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(2.dp))
                            Text(label, fontSize = 11.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) accent else CarcText3,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            when (tab) {
                0 -> { // City
                    // City state vars are declared above: cityTiles, cityShields, cityCathedral
                    ObjectStepperRow(stringResource(R.string.tiles_label), cityTiles, 1, 36, { cityTiles = it; if (cityShields > it * 2) cityShields = it * 2 })
                    Spacer(Modifier.height(16.dp))

                    // Shields — каждый тайл может нести 1 или 2 щита
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.shields_label), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.traders_note), fontSize = 11.sp, color = CarcText3)
                        }
                        // compact +/- stepper inline
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                .clickable { if (cityShields > 0) cityShields-- },
                                contentAlignment = Alignment.Center) {
                                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = if (cityShields > 0) CarcText else CarcText3)
                            }
                            Box(Modifier.width(44.dp).height(36.dp).background(CarcBg3)
                                .border(BorderStroke(1.dp, CarcBorder)),
                                contentAlignment = Alignment.Center) {
                                Text(cityShields.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    color = if (cityShields > 0) accent else CarcText3)
                            }
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .clickable { if (cityShields < cityTiles * 2) cityShields++ },
                                contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = if (cityShields < cityTiles * 2) CarcText else CarcText3)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Cathedral checkbox (Inns & Cathedrals)
                    if (hasInns) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (cityCathedral) accent.copy(alpha = 0.10f) else CarcBg3)
                                .border(1.dp, if (cityCathedral) accent.copy(alpha = 0.5f) else CarcBorder, RoundedCornerShape(8.dp))
                                .clickable { cityCathedral = !cityCathedral }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("⛪", fontSize = 18.sp)
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.cathedral_label), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.tiles_x3), fontSize = 11.sp, color = CarcText3)
                            }
                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(5.dp))
                                .background(if (cityCathedral) accent else CarcBorder),
                                contentAlignment = Alignment.Center) {
                                if (cityCathedral) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CarcBg)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }

                    // Formula breakdown
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.08f))
                            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tileMult = if (cityCathedral) 3 else 2
                        val tilePts = cityTiles * tileMult
                        val shieldPts = cityShields * 2
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🏰 ${cityTiles} $tilesWord × $tileMult", fontSize = 13.sp, color = CarcText2)
                            Text("$tilePts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (cityShields > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛡 ${cityShields} $shieldsWord × 2", fontSize = 13.sp, color = CarcText2)
                                Text("$shieldPts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = accent.copy(alpha = 0.2f), thickness = 0.5.dp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.total_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                            Text("+$cityPts", fontSize = 22.sp, fontWeight = FontWeight.Black, color = accent)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(addCityLabel, accent, onClick = {
                        val lbl = buildString {
                            append("🏰 ${cityTiles}${tileAbbr}")
                            if (cityShields > 0) append("+${cityShields}🛡")
                            if (cityCathedral) append("+⛪")
                        }
                        onScore(ScoringObjectType.CITY, cityPts, lbl)
                        onDismiss()
                    })
                }
                1 -> { // Road
                    ObjectStepperRow(stringResource(R.string.tiles_label), roadTiles, 1, 36, { roadTiles = it })
                    Spacer(Modifier.height(14.dp))

                    // Compact tavern row — только если дополнение включено
                    if (hasInns) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CarcBg3)
                                .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                                .clickable { roadTavern = !roadTavern }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🍺", fontSize = 18.sp)
                            Text(stringResource(R.string.inn_label), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f))
                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(5.dp))
                                .background(if (roadTavern) accent else CarcBorder),
                                contentAlignment = Alignment.Center) {
                                if (roadTavern) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CarcBg)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    ObjectScorePreview(stringResource(R.string.road_label), roadPts, accent,
                        if (roadTavern) "${roadTiles}${tileAbbr} × 2 ($innAbbr)" else "${roadTiles}${tileAbbr} × 1")
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(addRoadLabel, accent, onClick = {
                        val lbl = "🛤️ ${roadTiles}${tileAbbr}" + if (roadTavern) "+🍺" else ""
                        onScore(ScoringObjectType.ROAD, roadPts, lbl)
                        onDismiss()
                    })
                }
                2 -> { // Monastery
                    Text(stringResource(R.string.monastery_complete), fontSize = 13.sp, color = CarcText3)
                    Spacer(Modifier.height(8.dp))
                    ObjectStepperRow(stringResource(R.string.tiles_mon_label), monTiles, 1, 9, { monTiles = it })
                    Spacer(Modifier.height(20.dp))
                    ObjectScorePreview(stringResource(R.string.metric_monastery), monPts, accent,
                        if (monTiles == 9) stringResource(R.string.fully_completed) else stringResource(R.string.incomplete_tiles, monTiles))
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(addMonLabel, accent, onClick = {
                        onScore(ScoringObjectType.MONASTERY, monPts, "⛪ $monTiles/9")
                        onDismiss()
                    })
                }
            }
        }
    }
}

@Composable
fun ObjectStepperRow(label: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .clickable { if (value > min) onValue(value - 1) },
                contentAlignment = Alignment.Center
            ) { Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (value > min) CarcText else CarcText3) }
            Box(Modifier.width(52.dp).height(40.dp).background(CarcBg3).border(BorderStroke(1.dp, CarcBorder)),
                contentAlignment = Alignment.Center) {
                Text(value.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .clickable { if (value < max) onValue(value + 1) },
                contentAlignment = Alignment.Center
            ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (value < max) CarcText else CarcText3) }
        }
    }
}

@Composable
fun ObjectScorePreview(title: String, pts: Int, accent: Color, formula: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = accent, fontWeight = FontWeight.SemiBold)
            Text(formula, fontSize = 12.sp, color = CarcText3)
        }
        Text("+$pts", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
fun PrimaryButton(label: String, accent: Color = CarcGreen, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent else CarcBg3)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            color = if (enabled) CarcBg else CarcText3)
    }
}

// ─── Live Game Screen ─────────────────────────────────────────────────────────
@Composable
fun LiveGameScreen(
    liveGame: LiveGameState,
    onAdjustScore: (Int, Int) -> Unit,
    onAddObject: (Int, ScoringObjectType, Int, String) -> Unit,
    onUndoLast: (Int) -> Unit,
    onFinish: () -> Unit
) {
    var addObjectFor by remember { mutableStateOf<Pair<LivePlayerState, Int>?>(null) }
    // tab: 0=city 1=road 2=monastery 3=dragon(fairy)
    val hasDragon = "dragon" in liveGame.expansions
    val hasInns   = "inns"   in liveGame.expansions

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${liveGame.selectedPlayers.size} players", fontSize = 13.sp, color = CarcText3)
                    val leader = liveGame.selectedPlayers.maxByOrNull { it.score }
                    if (leader != null)
                        Text("👑 ${leader.playerName.take(10)}: ${leader.score}",
                            fontSize = 12.sp, color = meepleColor(leader.meepleColor))
                }
            }

            items(liveGame.selectedPlayers) { player ->
                val accent = meepleColor(player.meepleColor)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CarcCard),
                    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        // Header: avatar + name + score
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PlayerAvatar(player.playerName, player.meepleColor, 44.dp, avatarPath = player.avatarPath)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(player.playerName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                if (player.events.isNotEmpty()) {
                                    Text(
                                        player.events.takeLast(3).joinToString(" · ") { it.label },
                                        fontSize = 11.sp, color = CarcText3, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text(player.score.toString(),
                                fontSize = 44.sp, fontWeight = FontWeight.Black, color = accent,
                                lineHeight = 44.sp)
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = accent.copy(alpha = 0.18f))
                        Spacer(Modifier.height(10.dp))

                        // Object buttons grid
                        val baseObjects = listOf(
                            Triple(0, "🏰", stringResource(R.string.city_btn)),
                            Triple(1, "🛤️", stringResource(R.string.road_btn)),
                            Triple(2, "⛪", stringResource(R.string.monastery_btn))
                        )
                        // Dragon: Fairy gives +1 per turn to adjacent player
                        val allObjects = if (hasDragon)
                            baseObjects + Triple(3, "🧚", stringResource(R.string.fairy_btn))
                        else baseObjects

                        // Показываем в 2 строки по 2 (или в одну строку если объектов 3)
                        val rows = allObjects.chunked(if (allObjects.size <= 3) 3 else 2)
                        rows.forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowItems.forEach { (tabIdx, icon, label) ->
                                    Box(
                                        Modifier.weight(1f).height(64.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(accent.copy(alpha = 0.12f))
                                            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                if (tabIdx == 3) {
                                                    // Fairy: instant +1
                                                    onAddObject(player.playerId, ScoringObjectType.CITY, 1, "🧚 +1")
                                                } else {
                                                    addObjectFor = Pair(player, tabIdx)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(icon, fontSize = 22.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth())
                                            Spacer(Modifier.height(4.dp))
                                            Text(label, fontSize = 10.sp, color = accent,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 11.sp,
                                                modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        // Undo last event
                        if (player.events.isNotEmpty()) {
                            Box(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CarcBg3)
                                    .clickable { onUndoLast(player.playerId) }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("↩  ${player.events.last().label}",
                                    fontSize = 11.sp, color = CarcText3)
                            }
                        }
                    }
                }
            }
        }

        // FAB Finish Game
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)) {
            Box(
                Modifier.clip(RoundedCornerShape(16.dp))
                    .background(CarcGreen)
                    .clickable(onClick = onFinish)
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.finish_game), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarcBg)
            }
        }
    }

    // Bottom sheet — открывается на нужном табе
    addObjectFor?.let { (player, startTab) ->
        AddObjectSheet(
            playerName = player.playerName,
            meepleCol = player.meepleColor,
            startTab = startTab,
            hasInns = hasInns,
            onDismiss = { addObjectFor = null },
            onScore = { type, pts, label ->
                onAddObject(player.playerId, type, pts, label)
            }
        )
    }
}

@Composable
fun QuickScoreBtn(label: String, positive: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (positive) accent.copy(alpha = 0.15f) else CarcBg3)
            .border(1.dp, if (positive) accent.copy(alpha = 0.5f) else CarcBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (positive) accent else CarcText2)
    }
}

// ─── Score Edit Dialog ────────────────────────────────────────────────────────
@Composable
fun ScoreEditDialog(
    playerName: String,
    initial: Triple<Int, Int, Int>, // city, road, monastery
    initialFarm: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int, Int) -> Unit
) {
    var city by remember { mutableStateOf(initial.first.takeIf { it > 0 }?.toString() ?: "") }
    var road by remember { mutableStateOf(initial.second.takeIf { it > 0 }?.toString() ?: "") }
    var monastery by remember { mutableStateOf(initial.third.takeIf { it > 0 }?.toString() ?: "") }
    var farm by remember { mutableStateOf(initialFarm.takeIf { it > 0 }?.toString() ?: "") }
    val total = (city.toIntOrNull() ?: 0) + (road.toIntOrNull() ?: 0) +
                (monastery.toIntOrNull() ?: 0) + (farm.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarcCard2,
        title = { Text(stringResource(R.string.score_edit_title, playerName), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreInputField(stringResource(R.string.city_label), city, { city = it }, Modifier.weight(1f))
                    ScoreInputField(stringResource(R.string.road_label), road, { road = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreInputField(stringResource(R.string.metric_monastery), monastery, { monastery = it }, Modifier.weight(1f))
                    ScoreInputField(stringResource(R.string.farm_input), farm, { farm = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarcBg3)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.total_label), color = CarcText2)
                    Text(total.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CarcGreen)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        city.toIntOrNull() ?: 0,
                        road.toIntOrNull() ?: 0,
                        monastery.toIntOrNull() ?: 0,
                        farm.toIntOrNull() ?: 0
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CarcGreen, contentColor = CarcBg)
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CarcText3) }
        }
    )
}

@Composable
fun ScoreInputField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter { c -> c.isDigit() }) },
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CarcGreenDeep,
            unfocusedBorderColor = CarcBorder,
            focusedLabelColor = CarcGreen,
            unfocusedLabelColor = CarcText3,
            focusedTextColor = CarcText,
            unfocusedTextColor = CarcText,
            cursorColor = CarcGreen
        )
    )
}

// ─── Endgame Screen ───────────────────────────────────────────────────────────
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(game?.name ?: "Match #$gameId", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("📅 $date", fontSize = 13.sp, color = CarcText3, modifier = Modifier.padding(top = 4.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarcCard2)
                        .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                        .clickable { onEdit() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(stringResource(R.string.edit_btn), fontSize = 13.sp, color = CarcGreen, fontWeight = FontWeight.SemiBold)
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

// ─── Player Profile Screen ────────────────────────────────────────────────────
@Composable
fun PlayerProfileScreen(playerId: Int, viewModel: MainViewModel, onEdit: () -> Unit = {}) {
    var stats by remember { mutableStateOf<PlayerStats?>(null) }
    var gameHistory by remember { mutableStateOf<List<com.carcassonne.companion.data.entity.GamePlayerEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(playerId) {
        scope.launch {
            stats = viewModel.getPlayerStats(playerId)
            viewModel.getGamesForPlayer(playerId).collect { gameHistory = it.take(5) }
        }
    }

    val p = stats?.player ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerAvatar(p.name, p.meepleColor, 88.dp, avatarPath = p.avatarPath)
                Spacer(Modifier.height(12.dp))
                Text(p.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(getLevelTitle(stats?.gamesPlayed ?: 0)),
                    fontSize = 12.sp, color = CarcGreen, letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarcCard2)
                        .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                        .clickable { onEdit() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.edit_profile), fontSize = 13.sp, color = CarcGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleStatCard(stringResource(R.string.stat_games), (stats?.gamesPlayed ?: 0).toString(), Modifier.weight(1f))
                SimpleStatCard(stringResource(R.string.stat_win_rate), "${((stats?.winRate ?: 0f) * 100).toInt()}%", Modifier.weight(1f))
                SimpleStatCard(stringResource(R.string.stat_avg_score), (stats?.avgScore ?: 0f).toInt().toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            Text(stringResource(R.string.achievements), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AchievementItem("🏰", stringResource(R.string.ach_first_win), (stats?.wins ?: 0) >= 1)
                AchievementItem("🗺️", stringResource(R.string.ach_explorer), (stats?.gamesPlayed ?: 0) >= 5)
                AchievementItem("👥", stringResource(R.string.ach_social), (stats?.gamesPlayed ?: 0) >= 3)
                AchievementItem("⭐", stringResource(R.string.ach_star_player), (stats?.wins ?: 0) >= 5)
                AchievementItem("🎯", stringResource(R.string.ach_veteran), (stats?.gamesPlayed ?: 0) >= 20)
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            Text(stringResource(R.string.perf_history), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
        }
        if (gameHistory.isEmpty()) {
            item { Text(stringResource(R.string.no_games_played), color = CarcText3, modifier = Modifier.padding(vertical = 20.dp)) }
        } else {
            items(gameHistory) { gp ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CarcCard),
                    modifier = Modifier.padding(bottom = 8.dp).fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Правильное отображение места без бага с медалькой
                        val medal = when (gp.placement) {
                            1 -> "🏆"; 2 -> "🥈"; 3 -> "🥉"; else -> null
                        }
                        if (medal != null) {
                            Text(medal, fontSize = 24.sp)
                        } else {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(CarcBg3),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${gp.placement}", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                    color = CarcText2, lineHeight = 14.sp, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (gp.placement == 1) stringResource(R.string.place_first) else stringResource(R.string.place_n, gp.placement), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text(stringResource(R.string.pts_label, gp.finalScore), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (gp.placement == 1) CarcGreen else CarcText)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun SimpleStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = CarcText3, textAlign = TextAlign.Center)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AchievementItem(icon: String, name: String, unlocked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(70.dp)) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (unlocked) CarcGreen.copy(alpha = 0.15f) else CarcBg3)
                .border(2.dp, if (unlocked) CarcGreen else CarcBorder, CircleShape)
                .then(if (!unlocked) Modifier else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 26.sp, color = if (unlocked) Color.Unspecified else CarcText3.copy(alpha = 0.4f))
        }
        Spacer(Modifier.height(6.dp))
        Text(name, fontSize = 10.sp, color = CarcText2, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@androidx.annotation.StringRes
fun getLevelTitle(games: Int): Int = when {
    games >= 100 -> R.string.level_5
    games >= 50  -> R.string.level_4
    games >= 20  -> R.string.level_3
    games >= 5   -> R.string.level_2
    else         -> R.string.level_1
}

fun placeSuffix(n: Int) = when (n) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

// ─── Edit Player Screen ───────────────────────────────────────────────────────
@Composable
fun EditPlayerScreen(
    player: PlayerEntity,
    onSave: (PlayerEntity) -> Unit,
    onDone: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(player.name) }
    var color by remember { mutableStateOf(player.meepleColor) }
    var avatarPath by remember { mutableStateOf(player.avatarPath) }

    val (openGallery, _) = rememberGalleryLauncher { uri ->
        uri?.let {
            val saved = com.carcassonne.companion.util.ImageUtils.saveImageFromUri(context, it, player.id)
            if (saved != null) {
                if (avatarPath != player.avatarPath) com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
                avatarPath = saved
            }
        }
    }
    val (openCamera, _) = rememberCameraLauncher { file ->
        if (avatarPath != player.avatarPath) com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
        avatarPath = file.absolutePath
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(stringResource(R.string.section_avatar), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AvatarPickerSection(
                    name = name, color = color, avatarPath = avatarPath,
                    onPickGallery = openGallery,
                    onPickCamera = openCamera,
                    onRemoveAvatar = {
                        if (avatarPath != player.avatarPath) com.carcassonne.companion.util.ImageUtils.deleteAvatarFile(avatarPath)
                        avatarPath = null
                    },
                    size = 100.dp
                )
            }
        }
        item {
            Text(stringResource(R.string.section_player_name_label), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name_field), color = CarcText3) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
                    focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
                )
            )
        }
        item {
            Text(stringResource(R.string.default_meeple), fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            MeepleColorPicker(selected = color, onSelect = { color = it })
        }
        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton(stringResource(R.string.save_profile), onClick = {
                if (name.isNotBlank()) {
                    onSave(player.copy(name = name.trim(), meepleColor = color, avatarPath = avatarPath))
                    onDone()
                }
            })
        }
    }
}

// ─── Edit Game Screen ─────────────────────────────────────────────────────────
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
