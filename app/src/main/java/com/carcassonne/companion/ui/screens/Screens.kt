@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.carcassonne.companion.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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

    val bitmap = remember(currentPath) {
        if (currentPath != null && java.io.File(currentPath!!).exists()) {
            BitmapFactory.decodeFile(currentPath!!)?.asImageBitmap()
        } else null
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
            .height(if (bitmap != null) 200.dp else 100.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CarcBg3)
            .then(if (editable) Modifier.clickable { showSheet = true } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "Game photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
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
                    Text("📷 Change photo", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🗺️", fontSize = 36.sp)
                if (editable) {
                    Spacer(Modifier.height(4.dp))
                    Text("Tap to add board photo", fontSize = 12.sp, color = CarcText3)
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
                Text("Board Photo", fontWeight = FontWeight.Bold, fontSize = 17.sp)
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
                            Text("Camera", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                            Text("Gallery", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                        Text("🗑 Remove photo", fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
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
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Text(
                "GLOBAL STATS",
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
                StatCard("🃏", "TOTAL\nGAMES", stats.totalGames.toString(), Modifier.weight(1f))
                StatCard("👥", "TOTAL\nPLAYERS", stats.totalPlayers.toString(), Modifier.weight(1f))
                StatCard("🏆", "HIGHEST\nSCORE", stats.highestScore.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Activity", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort toggle
                    IconButton(onClick = onToggleSort, modifier = Modifier.size(32.dp)) {
                        Text(if (sortNewestFirst) "↓" else "↑", fontSize = 18.sp, color = CarcGreen)
                    }
                    TextButton(onClick = onViewAll) {
                        Text("View All", fontSize = 13.sp, color = CarcGreen)
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
                        Text("No games yet", color = CarcText2, fontWeight = FontWeight.SemiBold)
                        Text("Start your first game with +", color = CarcText3, fontSize = 13.sp)
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
                    val bmp = remember(game.photoPath) {
                        if (game.photoPath != null && java.io.File(game.photoPath).exists())
                            BitmapFactory.decodeFile(game.photoPath)?.asImageBitmap()
                        else null
                    }
                    if (bmp != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp, contentDescription = null,
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
            title = { Text("Delete ${selected.size} game${if (selected.size > 1) "s" else ""}?", fontWeight = FontWeight.Bold) },
            text = { Text("This cannot be undone.", color = CarcText2) },
            confirmButton = {
                Button(
                    onClick = { onDeleteGames(selected); selected = emptySet(); showConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Delete", fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = CarcText3) }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp,
                bottom = if (selecting) 96.dp else 72.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search games...", color = CarcText3) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = CarcText3) },
                    trailingIcon = {
                        if (selecting) {
                            TextButton(onClick = { selected = emptySet() }) {
                                Text("Cancel", color = CarcText3, fontSize = 13.sp)
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
            }

            if (selecting) {
                item {
                    Text(
                        "${selected.size} selected — long press to select more",
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
                            Text("No games found", color = CarcText2, fontWeight = FontWeight.SemiBold)
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
        }

        // Bottom delete bar
        androidx.compose.animation.AnimatedVisibility(
            visible = selecting,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.slideInVertically { it },
            exit = androidx.compose.animation.slideOutVertically { it }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444))
                    .clickable { showConfirm = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🗑  Delete ${selected.size} game${if (selected.size > 1) "s" else ""}",
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
                    val bmp = remember(game.photoPath) {
                        if (game.photoPath != null && java.io.File(game.photoPath).exists())
                            BitmapFactory.decodeFile(game.photoPath)?.asImageBitmap()
                        else null
                    }
                    if (bmp != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp, contentDescription = null,
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
            title = { Text("Delete ${selected.size} player(s)?", fontWeight = FontWeight.Bold) },
            text = { Text("This cannot be undone. Game history stays.", color = CarcText2) },
            confirmButton = {
                Button(
                    onClick = {
                        players.filter { it.id in selected }.forEach { onDeletePlayer(it) }
                        selected = emptySet()
                        showConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Delete", fontWeight = FontWeight.Bold, color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel", color = CarcText3) }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 16.dp,
                bottom = if (selecting) 96.dp else 72.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search players...", color = CarcText3) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = CarcText3) },
                    trailingIcon = {
                        if (selecting) {
                            TextButton(onClick = { selected = emptySet() }) {
                                Text("Cancel", color = CarcText3, fontSize = 13.sp)
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
            }
            item {
                Text(
                    if (selecting) "${selected.size} selected — long press to select more"
                    else "PLAYERS",
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
                            Text("No players yet", color = CarcText2, fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = onAddPlayer) {
                                Text("Add your first player", color = CarcGreen)
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
        }

        // Bottom delete bar — появляется при выделении
        androidx.compose.animation.AnimatedVisibility(
            visible = selecting,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.slideInVertically { it },
            exit = androidx.compose.animation.slideOutVertically { it }
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444))
                    .clickable { showConfirm = true }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🗑  Delete ${selected.size} player${if (selected.size > 1) "s" else ""}",
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
                            "${stats.wins}W / ${stats.gamesPlayed - stats.wins}L",
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarcText
                        )
                    }
                    Text(
                        "WIN RATE: ${(stats.winRate * 100).toInt()}%",
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
    onSlotChange: (Int, Int?) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    // selectedSlots driven by VM — persisted across sessions
    val selectedSlots = compareSlots.let { slots ->
        // Auto-fill empty slots with first available players if nothing saved yet
        if (slots.all { it == null } && playerStats.isNotEmpty()) {
            listOf(
                playerStats.getOrNull(0)?.player?.id,
                playerStats.getOrNull(1)?.player?.id,
                playerStats.getOrNull(2)?.player?.id
            )
        } else slots
    }
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
            title = { Text("Select player", fontWeight = FontWeight.SemiBold) },
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
                            Text("Remove player", fontSize = 14.sp, color = CarcRed)
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
                                Text("${(candidate.winRate * 100).toInt()}%% WR · %.0f avg".format(candidate.avgScore),
                                    fontSize = 11.sp, color = CarcText3)
                            }
                            if (candidate.title.isNotBlank()) {
                                Text(candidate.title, fontSize = 10.sp, color = CarcYellow)
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
                    Text("Cancel", color = CarcText3)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
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
                listOf("Global Stats", "Compare Players").forEachIndexed { i, label ->
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
                    StatCard("🃏", "TOTAL\nGAMES",    globalStats.totalGames.toString(),            Modifier.weight(1f).fillMaxHeight(), CarcText)
                    StatCard("⭐", "HIGHEST\nSCORE", globalStats.highestScore.toString(),           Modifier.weight(1f).fillMaxHeight(), CarcYellow)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("🏆", "AVG WINNER\nSCORE", "%.0f".format(globalStats.avgWinnerScore), Modifier.weight(1f).fillMaxHeight(), CarcGreen)
                    StatCard("👤", "PLAYERS",            globalStats.totalPlayers.toString(),       Modifier.weight(1f).fillMaxHeight(), CarcBlue)
                }
                Spacer(Modifier.height(16.dp))
            }
            // Metagame breakdown bar
            if (globalStats.avgScore > 0f) {
                item {
                    Text("METAGAME BREAKDOWN", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    MetagameBreakdownBar(globalStats)
                    Spacer(Modifier.height(16.dp))
                }
            }
            item {
                Text("TOP PLAYERS", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
            }
            if (playerStats.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), Alignment.Center) {
                        Text("No stats yet", color = CarcText3)
                    }
                }
            } else {
                items(playerStats.take(5)) { ps ->
                    StatsPlayerRow(ps)
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
                            Text("Need at least 2 players", color = CarcText2, fontWeight = FontWeight.SemiBold)
                            Text("Play more games to unlock comparisons", color = CarcText3, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                item {
                    ComparePlayersSection(
                        allStats = playerStats,
                        selectedSlots = selectedSlots,
                        slotColors = slotColors,
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
        Triple("🏰 Cities",      gs.avgCity,      CarcBlue),
        Triple("🛤️ Roads",       gs.avgRoad,      CarcGreen),
        Triple("⛪ Monasteries", gs.avgMonastery, CarcYellow),
        Triple("🌾 Farms",       gs.avgFarm,      CarcOrange)
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
                                Text("%.1f avg pts".format(value), fontSize = 12.sp,
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
    onSlotClick: (slotIdx: Int) -> Unit
) {
    val slotStats = selectedSlots.map { id -> allStats.find { it.player.id == id } }
    val activePlayers = slotStats.filterNotNull()
    // Map active player index → their original slot index to get correct color
    val activeSlotIndices = selectedSlots.indices.filter { selectedSlots[it] != null }
    val colors = activePlayers.indices.map { ai -> slotColors[activeSlotIndices.getOrElse(ai) { ai } % 3] }

    // ── Slot cards ─────────────────────────────────────────────────────────
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)
    ) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selectedSlots.forEachIndexed { slotIdx, selectedId ->
                val ps = allStats.find { it.player.id == selectedId }
                val slotColor = if (ps != null) slotColors[slotIdx % 3] else CarcBorder

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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
                        Text("%.0f avg".format(ps.avgScore), fontSize = 10.sp, color = CarcText3)
                    } else {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(50.dp))
                                .background(CarcBg3)
                                .border(1.dp, CarcBorder, RoundedCornerShape(50.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 18.sp, color = CarcText3)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Add", fontSize = 11.sp, color = CarcText3)
                        Text("player", fontSize = 10.sp, color = CarcText3)
                    }
                }
            }
        }
    }

    // ── If fewer than 2 active players, show hint ─────────────────────────
    if (activePlayers.size < 2) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Text("Select at least 2 players to compare", color = CarcText3, fontSize = 13.sp)
        }
        return
    }

    Spacer(Modifier.height(14.dp))

    // ── Radar Chart ────────────────────────────────────────────────────────
    Text("PLAY STYLE", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Column(Modifier.padding(14.dp)) {
            // Player legend — name + color dot
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                activePlayers.forEachIndexed { i, ps ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(colors[i]))
                        Spacer(Modifier.width(5.dp))
                        Text(ps.player.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = colors[i])
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Radar with axis labels drawn inside canvas
            RadarChart(
                players = activePlayers,
                colors  = colors,
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    // ── Stacked Score Bar ─────────────────────────────────────────────────
    Text("SCORE STRUCTURE", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
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
                    Row(modifier = Modifier.weight(ps.avgScore / maxAvg).height(18.dp).clip(RoundedCornerShape(4.dp))) {
                        cats.forEachIndexed { ci, v ->
                            val frac = (v / catTotal).coerceIn(0f, 1f)
                            if (frac > 0.02f) {
                                Box(Modifier.fillMaxHeight().weight(frac).background(catColors[ci]))
                            }
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

    // ── Metrics table ──────────────────────────────────────────────────────
    Text("METRICS", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Column(Modifier.padding(14.dp)) {
            val metrics = listOf(
                Triple("🏰 Urbanization",   activePlayers.map { it.urbanizationIndex }, "City share of total score"),
                Triple("🛤️ Road Aggr.",     activePlayers.map { it.roadAggrIndex },    "Roads vs cities ratio"),
                Triple("⛪ Monastery",      activePlayers.map { it.monasteryIndex },   "Monastery share of score"),
                Triple("🌾 Farm Dominance", activePlayers.map { it.farmDomIndex },     "Farm score vs field average"),
                Triple("🎯 Stability",      activePlayers.map { it.stabilityIndex },   "Score consistency")
            )
            metrics.forEachIndexed { mi, (label, values, hint) ->
                if (mi > 0) HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 6.dp))
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
                Text(hint, fontSize = 10.sp, color = CarcText3)
                Spacer(Modifier.height(6.dp))
                val maxV = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
                activePlayers.forEachIndexed { pi, ps ->
                    val v = values[pi]
                    val isWinner = v == maxV
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(2.dp)).background(colors[pi]))
                        Spacer(Modifier.width(5.dp))
                        Text(ps.player.name, fontSize = 10.sp, color = colors[pi],
                            modifier = Modifier.width(52.dp), maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(CarcBg3)) {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(v / maxV)
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

@Composable
fun RadarChart(
    players: List<PlayerStats>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val axisLabels = listOf("🏰 Urban", "🛤 Roads", "⛪ Monks", "🌾 Farms", "🎯 Stable")
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
                    .width(72.dp)
                    .wrapContentHeight()
                    .offset(x = xDp - 36.dp, y = yDp - 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    axisLabels[i],
                    fontSize = 9.sp,
                    color = CarcText2,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
fun StatsPlayerRow(ps: PlayerStats) {
    val accent = meepleColor(ps.player.meepleColor)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(ps.player.name, ps.player.meepleColor, avatarPath = ps.player.avatarPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ps.player.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (ps.title.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(ps.title, fontSize = 10.sp, color = CarcYellow)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${ps.wins}W / ${ps.gamesPlayed - ps.wins}L", fontSize = 11.sp, color = CarcText3)
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

// ─── Settings Screen ─────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onClearAll: () -> Unit,
    isDarkMode: Boolean = true,
    onDarkMode: (Boolean) -> Unit = {}
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Records?") },
            text = { Text("This will permanently delete all games, players, and stats. This cannot be undone.", color = CarcText2) },
            confirmButton = {
                TextButton(onClick = { onClearAll(); showClearDialog = false }) {
                    Text("Delete All", color = CarcRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
            containerColor = CarcCard2
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language") },
            text = { Text("English (US) is the only language currently available.", color = CarcText2) },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("OK", color = CarcGreen) }
            },
            containerColor = CarcCard2
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("APPEARANCE", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        item {
            SettingsRow("🌙", "Dark Mode", "Enable dark theme",
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
            SettingsRow("🌐", "Language", "English (US)", onClick = { showLanguageDialog = true })
        }
        item {
            Text("DATA MANAGEMENT", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp))
        }
        item { SettingsRow("☁️", "Backup Data", "Export stats as JSON file", onClick = onBackup) }
        item { SettingsRow("📥", "Restore Data", "Load previous session history", onClick = onRestore) }
        item {
            SettingsRow(
                "🗑️", "Clear All Records", "Permanent delete (irreversible)",
                onClick = { showClearDialog = true },
                iconBgColor = CarcRed.copy(alpha = 0.1f),
                trailing = { Text("›", fontSize = 20.sp, color = CarcRed) }
            )
        }
        item {
            Text("APPLICATION", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
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
                    Text("Carcassonne Companion v1.0.0", color = CarcGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Built with Jetpack Compose • Room DB", color = CarcText3, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
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
                Text("Gallery", fontSize = 12.sp, color = CarcGreen)
            }
            TextButton(onClick = onPickCamera) {
                Text("Camera", fontSize = 12.sp, color = CarcGreen)
            }
            if (avatarPath != null) {
                TextButton(onClick = onRemoveAvatar) {
                    Text("Remove", fontSize = 12.sp, color = CarcRed)
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
        title = { Text("Add New Player", fontWeight = FontWeight.Bold) },
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
                    label = { Text("Player Name", color = CarcText3) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
                        focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
                    )
                )
                Spacer(Modifier.height(16.dp))
                Text("Preferred Meeple Color", fontSize = 13.sp, color = CarcText2)
                Spacer(Modifier.height(8.dp))
                MeepleColorPicker(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) { onAdd(name.trim(), color, avatarPath); onDismiss() } },
                colors = ButtonDefaults.buttonColors(containerColor = CarcGreen, contentColor = CarcBg)
            ) { Text("Create Player", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CarcText3) }
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
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            SectionHeader("Select Players", "Add New +", onAddPlayer)
            Spacer(Modifier.height(12.dp))
        }

        if (players.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👤", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onAddPlayer) { Text("Add a player first", color = CarcGreen) }
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
                                    if (isSelected) "Tap to remove" else "Tap to include",
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
                            Text("SELECT MEEPLE COLOR", fontSize = 11.sp, color = CarcText3, letterSpacing = 0.5.sp)
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
            Text("Game Expansions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CarcCard)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ExpansionRow("🏰", "Inns & Cathedrals", "Exp. 1", "inns" in liveGame.expansions) { onToggleExpansion("inns") }
                    ExpansionRow("🚚", "Traders & Builders", "Exp. 2", "traders" in liveGame.expansions) { onToggleExpansion("traders") }
                    ExpansionRow("🐉", "The Princess & Dragon", "Exp. 3", "dragon" in liveGame.expansions) { onToggleExpansion("dragon") }
                    ExpansionRow("⛪", "The Abbey & Mayor", "Exp. 5", "abbey" in liveGame.expansions) { onToggleExpansion("abbey") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        item {
            Text("Game Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
        }
        item {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CarcCard)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("River Layout", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Starting with River expansion tiles", fontSize = 12.sp, color = CarcText3)
                        }
                        Switch(
                            checked = liveGame.riverLayout,
                            onCheckedChange = onSetRiver,
                            colors = SwitchDefaults.colors(checkedThumbColor = CarcBg, checkedTrackColor = CarcGreen)
                        )
                    }
                    HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Timed Turns", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("60s limit per player turn", fontSize = 12.sp, color = CarcText3)
                        }
                        Switch(
                            checked = liveGame.timedTurns,
                            onCheckedChange = onSetTimed,
                            colors = SwitchDefaults.colors(checkedThumbColor = CarcBg, checkedTrackColor = CarcGreen)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        item {
            PrimaryButton(
                "▶  START GAME",
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
                Text("$playerName — Add Object", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(16.dp))

            // Tab selector
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CarcBg3),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("🏰 City", "🛤️ Road", "⛪ Monastery").forEachIndexed { i, label ->
                    val sel = tab == i
                    Box(
                        Modifier.weight(1f).padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) accent.copy(alpha = 0.2f) else Color.Transparent)
                            .border(if (sel) 1.dp else 0.dp, if (sel) accent else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { tab = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) accent else CarcText3)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            when (tab) {
                0 -> { // City
                    // City state vars are declared above: cityTiles, cityShields, cityCathedral
                    ObjectStepperRow("Tiles", cityTiles, 1, 36, { cityTiles = it; if (cityShields > it * 2) cityShields = it * 2 })
                    Spacer(Modifier.height(16.dp))

                    // Shields — каждый тайл может нести 1 или 2 щита
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("🛡 Shields", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Up to 2 per tile (Traders & Builders)", fontSize = 11.sp, color = CarcText3)
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
                                Text("Cathedral in city", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Tiles ×3 instead of ×2", fontSize = 11.sp, color = CarcText3)
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
                            Text("🏰 ${cityTiles} tiles × $tileMult", fontSize = 13.sp, color = CarcText2)
                            Text("$tilePts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (cityShields > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛡 ${cityShields} shields × 2", fontSize = 13.sp, color = CarcText2)
                                Text("$shieldPts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = accent.copy(alpha = 0.2f), thickness = 0.5.dp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                            Text("+$cityPts", fontSize = 22.sp, fontWeight = FontWeight.Black, color = accent)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("+$cityPts pts — Add City", accent, onClick = {
                        val lbl = buildString {
                            append("🏰 ${cityTiles}t")
                            if (cityShields > 0) append("+${cityShields}🛡")
                            if (cityCathedral) append("+⛪")
                        }
                        onScore(ScoringObjectType.CITY, cityPts, lbl)
                        onDismiss()
                    })
                }
                1 -> { // Road
                    ObjectStepperRow("Tiles", roadTiles, 1, 36, { roadTiles = it })
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
                            Text("Inn (×2)", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f))
                            Box(Modifier.size(22.dp).clip(RoundedCornerShape(5.dp))
                                .background(if (roadTavern) accent else CarcBorder),
                                contentAlignment = Alignment.Center) {
                                if (roadTavern) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CarcBg)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    ObjectScorePreview("🛤️ Road", roadPts, accent,
                        if (roadTavern) "${roadTiles}t × 2 (inn)" else "${roadTiles}t × 1")
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("+$roadPts pts — Add Road", accent, onClick = {
                        val lbl = "🛤️ ${roadTiles}t" + if (roadTavern) "+🍺" else ""
                        onScore(ScoringObjectType.ROAD, roadPts, lbl)
                        onDismiss()
                    })
                }
                2 -> { // Monastery
                    Text("Completed monastery = +9", fontSize = 13.sp, color = CarcText3)
                    Spacer(Modifier.height(8.dp))
                    ObjectStepperRow("Tiles placed (1–9)", monTiles, 1, 9, { monTiles = it })
                    Spacer(Modifier.height(20.dp))
                    ObjectScorePreview("⛪ Monastery", monPts, accent,
                        if (monTiles == 9) "Fully completed!" else "Incomplete: $monTiles tiles")
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("+$monPts pts — Add Monastery", accent, onClick = {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            Triple(0, "🏰", "City"),
                            Triple(1, "🛤️", "Road"),
                            Triple(2, "⛪", "Monastery")
                        )
                        // Dragon: Fairy gives +1 per turn to adjacent player
                        val allObjects = if (hasDragon)
                            baseObjects + Triple(3, "🧚", "Fairy +1")
                        else baseObjects

                        // Показываем в 2 строки по 2 (или в одну строку если объектов 3)
                        val rows = allObjects.chunked(if (allObjects.size <= 3) 3 else 2)
                        rows.forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowItems.forEach { (tabIdx, icon, label) ->
                                    Box(
                                        Modifier.weight(1f).height(46.dp)
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
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(icon, fontSize = 16.sp, lineHeight = 18.sp)
                                            Text(label, fontSize = 10.sp, color = accent,
                                                fontWeight = FontWeight.SemiBold, lineHeight = 12.sp)
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
                Text("🏁  FINISH GAME", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CarcBg)
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
        title = { Text("✎ $playerName's Score", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreInputField("🏰 City", city, { city = it }, Modifier.weight(1f))
                    ScoreInputField("🛤️ Road", road, { road = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreInputField("⛪ Monastery", monastery, { monastery = it }, Modifier.weight(1f))
                    ScoreInputField("🌾 Farm", farm, { farm = it }, Modifier.weight(1f))
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
                    Text("Total", color = CarcText2)
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
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = CarcText3) }
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
    onSave: (String) -> Unit,
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
    var phase by remember { mutableStateOf(0) } // 0=incomplete 1=farms 2=confirm

    val totalWithEndgame = { pid: Int ->
        val p = players.find { it.playerId == pid }!!
        val eg = inputs[pid]?.value ?: EndgamePlayerInput(pid)
        p.score + eg.totalPoints()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Phase tabs
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CarcBg3),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Incomplete", "Farms", "Confirm").forEachIndexed { i, label ->
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
                    Text("Incomplete objects score 1 pt per tile/shield",
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
                                Text("Base: ${player.score}", fontSize = 13.sp, color = CarcText3)
                            }
                            Spacer(Modifier.height(12.dp))
                            // Incomplete city tiles+shields
                            EndgameStepperRow("🏰 City tiles+shields", inp.value.incompleteCity, 0, 99) {
                                inp.value = inp.value.copy(incompleteCity = it)
                            }
                            Spacer(Modifier.height(8.dp))
                            EndgameStepperRow("🛤️ Road tiles", inp.value.incompleteRoad, 0, 99) {
                                inp.value = inp.value.copy(incompleteRoad = it)
                            }
                            Spacer(Modifier.height(8.dp))
                            EndgameStepperRow("⛪ Monastery tiles", inp.value.incompleteMonastery, 0, 8) {
                                inp.value = inp.value.copy(incompleteMonastery = it)
                            }
                            // Sub-total
                            val sub = inp.value.incompleteCity + inp.value.incompleteRoad + inp.value.incompleteMonastery
                            if (sub > 0) {
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Text("+$sub pts from incomplete", fontSize = 12.sp, color = accent)
                                }
                            }
                        }
                    }
                }
                item {
                    PrimaryButton("Next → Farms", onClick = { phase = 1 })
                }
            }

            1 -> { // Farms
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(CarcBg3).padding(14.dp)) {
                        Text("🌾 Farm Scoring", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Count completed cities adjacent to each player's farm.",
                            fontSize = 13.sp, color = CarcText3)
                        Text("Formula: N cities × 3 pts", fontSize = 13.sp, color = CarcGreen,
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
                            EndgameStepperRow("Completed cities adjacent", inp.value.farmCities, 0, 30) {
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
                                    Text("${inp.value.farmCities} × 3 pts", fontSize = 13.sp, color = CarcText2)
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
                            Text("← Back", color = CarcText2, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.weight(2f).clip(RoundedCornerShape(10.dp))
                            .background(CarcGreen).clickable { phase = 2 }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text("Next → Confirm", color = CarcBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            2 -> { // Confirm & Save
                item {
                    Text("Final Scores", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
                                Text("Base ${player.score}" +
                                    (if (inp.incompleteCity + inp.incompleteRoad + inp.incompleteMonastery > 0)
                                        " + ${inp.incompleteCity + inp.incompleteRoad + inp.incompleteMonastery} incomplete" else "") +
                                    (if (inp.farmCities > 0) " + ${inp.farmCities * 3} farms" else ""),
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
                    Text("📷 Board Photo", fontSize = 13.sp, color = CarcText3, modifier = Modifier.padding(bottom = 8.dp))
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
                        label = { Text("Game name (optional)", color = CarcText3) },
                        modifier = Modifier.fillMaxWidth(),
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
                            Text("← Back", color = CarcText2, fontWeight = FontWeight.SemiBold)
                        }
                        Box(Modifier.weight(2f).clip(RoundedCornerShape(10.dp))
                            .background(CarcGreen)
                            .clickable {
                                val finalInputs = inputs.mapValues { it.value.value }
                                onApply(finalInputs)
                                onSave(gameName)
                            }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text("✓ Save Game", color = CarcBg, fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
    onEdit: () -> Unit = {}
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
        contentPadding = PaddingValues(16.dp)
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
                    Text("✎ Edit", fontSize = 13.sp, color = CarcGreen, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Winner
        sorted.firstOrNull()?.let { winner ->
            item {
                Text("🏆 Winner", fontSize = 13.sp, color = CarcText3, modifier = Modifier.padding(bottom = 8.dp))
                val p = players.find { it.id == winner.playerId }
                Card(
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
                                Text(p?.name ?: "Unknown", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(10.dp).clip(CircleShape).background(meepleColor(winner.meepleColor)))
                            }
                            Text("${winner.finalScore} pts • MVP", fontSize = 13.sp, color = CarcText3)
                        }
                        Text(winner.finalScore.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = CarcGreen)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Final Standings
        item { Text("Final Standings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp)) }
        items(sorted) { gp ->
            val p = players.find { it.id == gp.playerId }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CarcCard),
                modifier = Modifier.padding(bottom = 8.dp)
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
                            Text(p?.name ?: "Unknown", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(8.dp).clip(CircleShape).background(meepleColor(gp.meepleColor)))
                        }
                        Text("${gp.finalScore} pts", fontSize = 12.sp, color = CarcText3)
                    }
                    Text(gp.finalScore.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Points Breakdown — всегда показываем
        item {
            Spacer(Modifier.height(8.dp))
            Text("Points Breakdown", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
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
        contentPadding = PaddingValues(16.dp)
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
                    getLevelTitle(stats?.gamesPlayed ?: 0),
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
                    Text("✎ Edit Profile", fontSize = 13.sp, color = CarcGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SimpleStatCard("Games", (stats?.gamesPlayed ?: 0).toString(), Modifier.weight(1f))
                SimpleStatCard("Win Rate", "${((stats?.winRate ?: 0f) * 100).toInt()}%", Modifier.weight(1f))
                SimpleStatCard("Avg Score", (stats?.avgScore ?: 0f).toInt().toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            Text("🏅 Achievements", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AchievementItem("🏰", "First Win", (stats?.wins ?: 0) >= 1)
                AchievementItem("🗺️", "Explorer", (stats?.gamesPlayed ?: 0) >= 5)
                AchievementItem("👥", "Social", (stats?.gamesPlayed ?: 0) >= 3)
                AchievementItem("⭐", "Star Player", (stats?.wins ?: 0) >= 5)
                AchievementItem("🎯", "Veteran", (stats?.gamesPlayed ?: 0) >= 20)
            }
            Spacer(Modifier.height(20.dp))
        }
        item {
            Text("📈 Performance History", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
        }
        if (gameHistory.isEmpty()) {
            item { Text("No games played yet", color = CarcText3, modifier = Modifier.padding(vertical = 20.dp)) }
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
                            Text(if (gp.placement == 1) "1st Place" else "${gp.placement}${placeSuffix(gp.placement)} Place", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${gp.finalScore} pts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (gp.placement == 1) CarcGreen else CarcText)
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

fun getLevelTitle(games: Int) = when {
    games >= 100 -> "LEVEL 5 GRANDMASTER"
    games >= 50  -> "LEVEL 4 EXPERT"
    games >= 20  -> "LEVEL 3 ADVANCED"
    games >= 5   -> "LEVEL 2 INTERMEDIATE"
    else         -> "LEVEL 1 BEGINNER"
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("AVATAR", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
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
            Text("PLAYER NAME", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = CarcText3) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
                    focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
                )
            )
        }
        item {
            Text("DEFAULT MEEPLE COLOR", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            MeepleColorPicker(selected = color, onSelect = { color = it })
        }
        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton("✓  SAVE PROFILE", onClick = {
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
                    Text("OK", color = CarcGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = CarcText3)
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
                name = p?.name ?: "Player ${gp.playerId}",
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("GAME INFO", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = gameName,
                onValueChange = { gameName = it },
                label = { Text("Game Name", color = CarcText3) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CarcGreenDeep, unfocusedBorderColor = CarcBorder,
                    focusedTextColor = CarcText, unfocusedTextColor = CarcText, cursorColor = CarcGreen
                )
            )
        }

        item {
            Text("DATE", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
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
            Text("PLAYERS & SCORES", fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
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
                        Text("$displayTotal pts", fontSize = 15.sp, color = CarcGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("MEEPLE COLOR", fontSize = 10.sp, color = CarcText3, letterSpacing = 0.5.sp)
                    Spacer(Modifier.height(6.dp))
                    MeepleColorPicker(
                        selected = state.color,
                        onSelect = { editPlayers[i].value = state.copy(color = it) },
                        disabledColors = takenColors
                    )
                    Spacer(Modifier.height(12.dp))
                    // Total override
                    ScoreInputField(
                        "🏅 Total (override)",
                        state.total,
                        { editPlayers[i].value = state.copy(total = it) },
                        Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Или введите по категориям:",
                        fontSize = 11.sp, color = CarcText3,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScoreInputField("🏰 City", state.city, { editPlayers[i].value = state.copy(city = it, total = "") }, Modifier.weight(1f))
                        ScoreInputField("🛤️ Road", state.road, { editPlayers[i].value = state.copy(road = it, total = "") }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScoreInputField("⛪ Mon.", state.monastery, { editPlayers[i].value = state.copy(monastery = it, total = "") }, Modifier.weight(1f))
                        ScoreInputField("🌾 Farm", state.farm, { editPlayers[i].value = state.copy(farm = it, total = "") }, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton("✓  SAVE CHANGES", onClick = {
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
                    onDone = onDone
                )
            })
            Spacer(Modifier.height(72.dp))
        }
    }
}
