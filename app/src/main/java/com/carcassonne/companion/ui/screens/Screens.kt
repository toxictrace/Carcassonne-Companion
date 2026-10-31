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
