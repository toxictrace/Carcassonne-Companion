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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.carcassonne.companion.util.BackupManager
import java.io.File

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

    // Player picker dialog stays outside LazyColumn.
    if (pickingSlot >= 0) {
        val slotIdx = pickingSlot
        val currentId = selectedSlots.getOrNull(slotIdx)
        val available = playerStats.filter { candidate ->
            selectedSlots.indices.none { i -> i != slotIdx && selectedSlots[i] == candidate.player.id }
        }
        PlayerPickerDialog(
            slotIdx = slotIdx,
            currentId = currentId,
            availablePlayers = available,
            slotColor = slotColors.getOrElse(slotIdx % 3) { CarcGreen },
            onRemovePlayer = { onSlotChange(slotIdx, null); pickingSlot = -1 },
            onPlayerSelected = { playerId -> onSlotChange(slotIdx, playerId); pickingSlot = -1 },
            onDismiss = { pickingSlot = -1 }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
    ) {
        item {
            StatsTabSelector(
                selectedTab = tab,
                onTabSelected = { tab = it }
            )
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
fun SectionLabel(text: String) {
    Text(text, fontSize = 11.sp, color = CarcText3, letterSpacing = 1.sp)
    Spacer(Modifier.height(8.dp))
}
