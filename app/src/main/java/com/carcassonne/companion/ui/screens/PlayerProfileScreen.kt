package com.carcassonne.companion.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.MainViewModel
import com.carcassonne.companion.viewmodel.PlayerStats
import kotlinx.coroutines.launch

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
