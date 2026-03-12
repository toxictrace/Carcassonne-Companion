package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.*
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(game.date))
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarcBg3),
                    contentAlignment = Alignment.Center
                ) { Text("🗺️", fontSize = 18.sp) }
                Spacer(Modifier.width(10.dp))
                Text(game.name ?: "Game #${game.id}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(date, fontSize = 12.sp, color = CarcText3)
            }
            if (sortedGP.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                // Игроки равномерно
                Row(modifier = Modifier.fillMaxWidth()) {
                    sortedGP.forEachIndexed { idx, gp ->
                        val p = players.find { it.id == gp.playerId }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            PlayerAvatar(p?.name ?: "?", p?.meepleColor ?: "gray", size = 22.dp, avatarPath = p?.avatarPath)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                p?.name?.take(6) ?: "?",
                                fontSize = 11.sp,
                                color = CarcText2,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(meepleColor(gp.meepleColor))
                            )
                        }
                        if (idx < sortedGP.size - 1) Spacer(Modifier.width(4.dp))
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
    onEditGame: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(games, query) {
        if (query.isBlank()) games
        else games.filter { (it.name ?: "Game #${it.id}").contains(query, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
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
                    IconButton(onClick = onToggleSort) {
                        Text(if (sortNewestFirst) "↓" else "↑", fontSize = 18.sp, color = CarcGreen)
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
            items(filtered) { game ->
                val gps = allGamePlayers.filter { it.gameId == game.id }
                HistoryGameCard(
                    game = game,
                    gamePlayers = gps,
                    players = players,
                    onClick = { onGameClick(game.id) },
                    onEdit = { onEditGame(game.id) }
                )
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun HistoryGameCard(
    game: GameEntity,
    gamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity> = emptyList(),
    players: List<PlayerEntity> = emptyList(),
    onClick: () -> Unit,
    onEdit: () -> Unit = {}
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
                ) { Text("🗺️", fontSize = 20.sp) }
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
                Spacer(Modifier.height(10.dp))
                // Игроки равномерно
                Row(modifier = Modifier.fillMaxWidth()) {
                    sortedGP.forEachIndexed { idx, gp ->
                        val p = players.find { it.id == gp.playerId }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            PlayerAvatar(p?.name ?: "?", p?.meepleColor ?: "gray", size = 22.dp, avatarPath = p?.avatarPath)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                p?.name?.take(6) ?: "?",
                                fontSize = 11.sp,
                                color = CarcText2,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(3.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(meepleColor(gp.meepleColor))
                            )
                        }
                        if (idx < sortedGP.size - 1) Spacer(Modifier.width(4.dp))
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
    onAddPlayer: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(players, query) {
        if (query.isBlank()) players
        else players.filter { it.name.contains(query, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search players...", color = CarcText3) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = CarcText3) },
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
                "ACTIVE PLAYERS",
                fontSize = 11.sp,
                color = CarcText3,
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
            items(filtered) { player ->
                val stats = playerStats.find { it.player.id == player.id }
                PlayerCard(player, stats, onClick = { onPlayerClick(player.id) })
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun PlayerCard(player: PlayerEntity, stats: PlayerStats?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                            "${stats.wins}W / ${stats.gamesPlayed - stats.wins}L",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CarcText
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

// ─── Stats Screen ────────────────────────────────────────────────────────────
@Composable
fun StatsScreen(globalStats: GlobalStats, playerStats: List<PlayerStats>) {
    var tab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // Tab row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50.dp))
                    .background(CarcCard)
                    .border(1.dp, CarcBorder, RoundedCornerShape(50.dp))
                    .padding(4.dp)
            ) {
                listOf("Global Stats", "Player Comparison").forEachIndexed { i, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (tab == i) CarcGreen else Color.Transparent)
                            .clickable { tab = i },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tab == i) CarcBg else CarcText3
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (tab == 0) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("🃏", "TOTAL\nGAMES", globalStats.totalGames.toString(), Modifier.weight(1f), CarcText)
                    StatCard("⭐", "HIGHEST\nSCORE", globalStats.highestScore.toString(), Modifier.weight(1f), CarcText)
                }
                Spacer(Modifier.height(10.dp))
            }
            item {
                Text("Top Players", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
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
            item {
                Spacer(Modifier.height(12.dp))
                Text("Feature Performance", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CarcCard)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FeatureRow("🏰", "City Points", globalStats.totalCityPoints)
                        HorizontalDivider(color = CarcBorder, modifier = Modifier.padding(vertical = 4.dp))
                        FeatureRow("🛤️", "Road Points", globalStats.totalRoadPoints)
                        HorizontalDivider(color = CarcBorder, modifier = Modifier.padding(vertical = 4.dp))
                        FeatureRow("🌾", "Farm Points", globalStats.totalFarmPoints)
                    }
                }
            }
        } else {
            // Compare tab
            if (playerStats.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No data yet", color = CarcText2, fontWeight = FontWeight.SemiBold)
                            Text("Play some games to see comparisons", color = CarcText3, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(playerStats) { ps ->
                    StatsPlayerRow(ps)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
fun StatsPlayerRow(ps: PlayerStats) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerAvatar(ps.player.name, ps.player.meepleColor, avatarPath = ps.player.avatarPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ps.player.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${ps.wins}W / ${ps.gamesPlayed - ps.wins}L", fontSize = 11.sp, color = CarcText3)
                Spacer(Modifier.height(4.dp))
                WinRateBar(ps.winRate)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "${(ps.winRate * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (ps.winRate >= 0.5f) CarcGreen else CarcText2
            )
        }
    }
}

@Composable
fun FeatureRow(icon: String, label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(
            value.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ─── Settings Screen ─────────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onClearAll: () -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(true) }
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
            SettingsRow("🌙", "Dark Mode", "Enable high contrast theme",
                onClick = { darkMode = !darkMode },
                trailing = {
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { darkMode = it },
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

// ─── Live Game Screen ─────────────────────────────────────────────────────────
@Composable
fun LiveGameScreen(
    liveGame: LiveGameState,
    onAdjustScore: (Int, Int) -> Unit,
    onEditScore: (Int) -> Unit,
    onFinish: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${liveGame.selectedPlayers.size} Players", fontSize = 13.sp, color = CarcText3)
                Text("Tap score to edit", fontSize = 13.sp, color = CarcText2)
            }
        }

        items(liveGame.selectedPlayers) { player ->
            val c = meepleColor(player.meepleColor)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CarcCard),
                border = BorderStroke(1.5.dp, CarcBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerAvatar(player.playerName, player.meepleColor, avatarPath = player.avatarPath)
                        Spacer(Modifier.width(12.dp))
                        Text(player.playerName, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            player.score.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = c,
                            modifier = Modifier.clickable { onEditScore(player.playerId) }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScoreAdjustButton("−5", false, { onAdjustScore(player.playerId, -5) }, Modifier.weight(1f))
                        ScoreAdjustButton("−1", false, { onAdjustScore(player.playerId, -1) }, Modifier.weight(1f))
                        ScoreAdjustButton("+1", true,  { onAdjustScore(player.playerId, +1) }, Modifier.weight(1f))
                        ScoreAdjustButton("+5", true,  { onAdjustScore(player.playerId, +5) }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("+2","+3","+4","+9","+10").forEach { label ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CarcBg3)
                                    .border(1.dp, CarcBorder, RoundedCornerShape(6.dp))
                                    .clickable { onAdjustScore(player.playerId, label.removePrefix("+").toInt()) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarcText2)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CarcBg3)
                                .border(1.dp, CarcBorder, RoundedCornerShape(6.dp))
                                .clickable { onEditScore(player.playerId) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✎", fontSize = 13.sp, color = CarcText2)
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            PrimaryButton("🏁  FINISH GAME", onClick = onFinish)
            Spacer(Modifier.height(72.dp))
        }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)).background(CarcBg3),
                contentAlignment = Alignment.Center
            ) { Text("🗺️", fontSize = 48.sp) }
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
                        Text(if (gp.placement == 1) "🏆" else if (gp.placement == 2) "🥈" else "🥉", fontSize = 24.sp)
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
