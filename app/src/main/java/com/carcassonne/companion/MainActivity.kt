package com.carcassonne.companion

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.screens.*
import com.carcassonne.companion.util.BackupManager
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.EndgamePlayerInput
import com.carcassonne.companion.viewmodel.MainViewModel
import com.carcassonne.companion.viewmodel.ScoringObjectType
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.activity.compose.rememberLauncherForActivityResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Navigation Routes ───────────────────────────────────────────────────────
object Routes {
    const val DASHBOARD      = "dashboard"
    const val HISTORY        = "history"
    const val PLAYERS        = "players"
    const val STATS          = "stats"
    const val SETTINGS       = "settings"
    const val NEW_GAME       = "new_game"
    const val LIVE_GAME      = "live_game"
    const val ENDGAME        = "endgame"
    const val MATCH_DETAIL   = "match_detail/{gameId}"
    const val PLAYER_PROFILE = "player_profile/{playerId}"
    const val EDIT_GAME      = "edit_game/{gameId}"
    const val EDIT_PLAYER    = "edit_player/{playerId}"

    fun matchDetail(id: Int) = "match_detail/$id"
    fun playerProfile(id: Int) = "player_profile/$id"
    fun editGame(id: Int) = "edit_game/$id"
    fun editPlayer(id: Int) = "edit_player/$id"
}

data class BottomNavItem(val route: String, val icon: ImageVector, val labelRes: Int)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, Icons.Default.Dashboard, R.string.nav_dashboard),
    BottomNavItem(Routes.HISTORY,   Icons.Default.History,   R.string.nav_history),
    BottomNavItem(Routes.PLAYERS,   Icons.Default.People,    R.string.nav_players),
    BottomNavItem(Routes.STATS,     Icons.Default.BarChart,  R.string.nav_stats),
    BottomNavItem(Routes.SETTINGS,  Icons.Default.Settings,  R.string.nav_settings),
)

val mainRoutes = bottomNavItems.map { it.route }

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val isDark by vm.isDarkMode.collectAsState()
            CarcassonneTheme(darkMode = isDark) {
                CarcassonneApp(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarcassonneApp(vm: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark by vm.isDarkMode.collectAsState()

    val players by vm.players.collectAsState()
    val games   by vm.sortedGames.collectAsState()
    val stats   by vm.globalStats.collectAsState()
    val pStats  by vm.playerStats.collectAsState()
    val liveGame by vm.liveGame.collectAsState()
    val sortNewestFirst by vm.sortNewestFirst.collectAsState()
    val compareSlots by vm.compareSlots.collectAsState()
    val allGamePlayers by vm.allGamePlayers.collectAsState()

    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val showBottomNav = currentRoute in mainRoutes
    val showFab = currentRoute in listOf(Routes.DASHBOARD, Routes.HISTORY, Routes.PLAYERS, Routes.STATS)

    // Show VM messages as snackbars
    LaunchedEffect(Unit) {
        vm.message.collect { msg -> snackbarHostState.showSnackbar(msg) }
    }

    // Add player dialog state
    var showAddPlayer by remember { mutableStateOf(false) }
    if (showAddPlayer) {
        AddPlayerDialog(
            onDismiss = { showAddPlayer = false },
            onAdd = { name, color, avatarPath -> vm.addPlayer(name, color, avatarPath) }
        )
    }

    // Score edit dialog — убран, теперь через AddObjectSheet в LiveGameScreen

    // End game dialog — убран, теперь отдельный экран ENDGAME

    Scaffold(
        containerColor = CarcBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when (currentRoute) {
                Routes.DASHBOARD -> TopAppBar(
                    title = { Text(stringResource(R.string.nav_dashboard), fontWeight = FontWeight.Bold, color = CarcText) },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.carcassonne_title), fontSize = 13.sp, color = CarcGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.width(12.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CarcBg)
                )
                Routes.HISTORY -> TopAppBar(
                    title = { Text(stringResource(R.string.match_history), fontWeight = FontWeight.Bold, color = CarcText) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CarcBg)
                )
                Routes.PLAYERS -> TopAppBar(
                    title = { Text(stringResource(R.string.nav_players), fontWeight = FontWeight.Bold, color = CarcText) },
                    actions = {
                        IconButton(onClick = { showAddPlayer = true }) {
                            Icon(Icons.Default.PersonAdd, null, tint = CarcGreen)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CarcBg)
                )
                Routes.STATS -> TopAppBar(
                    title = { Text(stringResource(R.string.stats_title), fontWeight = FontWeight.Bold, color = CarcText) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CarcBg)
                )
                Routes.SETTINGS -> TopAppBar(
                    title = { Text(stringResource(R.string.nav_settings), fontWeight = FontWeight.Bold, color = CarcText) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CarcBg)
                )
                Routes.NEW_GAME -> TopAppBar(
                    title = { Text(stringResource(R.string.new_match_title), fontWeight = FontWeight.Bold, color = CarcText) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = CarcText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CarcBg)
                )
                Routes.LIVE_GAME -> TopAppBar(
                    title = { Text(stringResource(R.string.game_in_progress), fontWeight = FontWeight.Bold, color = CarcText) },
                    navigationIcon = {
                        IconButton(onClick = {
                            // Confirm abandon
                            vm.abandonGame()
                            navController.popBackStack(Routes.DASHBOARD, false)
                        }) {
                            Icon(Icons.Default.Close, null, tint = CarcText)
                        }
                    },
                    actions = {
                        TextButton(onClick = { navController.navigate(Routes.ENDGAME) }) {
                            Text(stringResource(R.string.finish_btn), color = CarcGreen, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = CarcBg)
                )
                else -> if (currentRoute?.startsWith("match_detail") == true ||
                           currentRoute?.startsWith("player_profile") == true ||
                           currentRoute?.startsWith("edit_game") == true ||
                           currentRoute?.startsWith("edit_player") == true ||
                           currentRoute == Routes.ENDGAME) {
                    TopAppBar(
                        title = {
                            Text(
                                when {
                                    currentRoute.startsWith("match_detail") -> stringResource(R.string.match_details)
                                    currentRoute.startsWith("edit_game")    -> stringResource(R.string.edit_game)
                                    currentRoute.startsWith("edit_player")  -> stringResource(R.string.edit_profile)
                                    currentRoute == Routes.ENDGAME          -> stringResource(R.string.final_scoring)
                                    else -> stringResource(R.string.player_profile_title)
                                },
                                fontWeight = FontWeight.Bold, color = CarcText
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, null, tint = CarcText)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = CarcBg)
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(containerColor = CarcBg2, tonalElevation = 0.dp) {
                    bottomNavItems.forEach { item ->
                        val selected = currentBackStack?.destination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, null) },
                            label = { Text(stringResource(item.labelRes), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CarcGreen,
                                selectedTextColor = CarcGreen,
                                unselectedIconColor = CarcText3,
                                unselectedTextColor = CarcText3,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.NEW_GAME) },
                    containerColor = CarcGreen,
                    contentColor = CarcBg,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->
        val tabRoutes = listOf(Routes.DASHBOARD, Routes.HISTORY, Routes.PLAYERS, Routes.STATS, Routes.SETTINGS)
        var prevTabIndex by remember { mutableIntStateOf(0) }

        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(top = padding.calculateTopPadding()),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(200)) }
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    stats = stats,
                    games = games,
                    players = players,
                    allGamePlayers = allGamePlayers,
                    sortNewestFirst = sortNewestFirst,
                    onToggleSort = { vm.toggleSortOrder() },
                    onViewAll = {
                        navController.navigate(Routes.HISTORY) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onGameClick = { navController.navigate(Routes.matchDetail(it)) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    games = games,
                    players = players,
                    allGamePlayers = allGamePlayers,
                    sortNewestFirst = sortNewestFirst,
                    onToggleSort = { vm.toggleSortOrder() },
                    onGameClick = { navController.navigate(Routes.matchDetail(it)) },
                    onEditGame = { navController.navigate(Routes.editGame(it)) },
                    onDeleteGames = { ids -> ids.forEach { vm.deleteGame(it) } }
                )
            }
            composable(Routes.PLAYERS) {
                PlayersScreen(
                    players = players,
                    playerStats = pStats,
                    onPlayerClick = { navController.navigate(Routes.playerProfile(it)) },
                    onAddPlayer = { showAddPlayer = true },
                    onDeletePlayer = { vm.deletePlayer(it) }
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    globalStats = stats,
                    playerStats = pStats,
                    compareSlots = compareSlots,
                    onSlotChange = { idx, id -> vm.setCompareSlot(idx, id) },
                    allGamePlayers = allGamePlayers,
                    sectionMask = vm.loadCompareSections(),
                    onSectionsChange = { mask -> vm.saveCompareSections(mask) },
                    onPlayerClick = { navController.navigate(Routes.playerProfile(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                val context = LocalContext.current
                val backupFolderUri by vm.backupFolderUri.collectAsState()

                val folderPickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree()
                ) { uri ->
                    uri?.let {
                        context.contentResolver.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        vm.setBackupFolderUri(it.toString())
                    }
                }
                val createBackupLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/octet-stream")
                ) { uri -> uri?.let { vm.exportBackupToUri(context, it) } }
                val openBackupLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> uri?.let { vm.importBackupFromUri(context, it) } }

                val folderDisplayName = remember(backupFolderUri) {
                    backupFolderUri?.let { uriStr ->
                        try { DocumentFile.fromTreeUri(context, Uri.parse(uriStr))?.name }
                        catch (e: Exception) { null }
                    }
                }

                var showRestoreFromFolder by remember { mutableStateOf(false) }
                val backupFilesInFolder = remember(backupFolderUri, showRestoreFromFolder) {
                    if (backupFolderUri != null && showRestoreFromFolder) {
                        try { BackupManager.listBackupFilesInFolderUri(context, Uri.parse(backupFolderUri!!)) }
                        catch (e: Exception) { emptyList() }
                    } else emptyList()
                }

                if (showRestoreFromFolder) {
                    RestoreFromFolderDialog(
                        files = backupFilesInFolder,
                        onSelect = { doc -> vm.importBackupFromUri(context, doc.uri); showRestoreFromFolder = false },
                        onDismiss = { showRestoreFromFolder = false }
                    )
                }

                SettingsScreen(
                    onBackup = {
                        if (backupFolderUri != null) vm.exportBackupToFolder(context)
                        else { val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date()); createBackupLauncher.launch("$dateStr.ccbackup") }
                    },
                    onRestore = {
                        if (backupFolderUri != null) showRestoreFromFolder = true
                        else openBackupLauncher.launch(arrayOf("*/*"))
                    },
                    onPickBackupFolder = { folderPickerLauncher.launch(null) },
                    backupFolderName = folderDisplayName,
                    onClearAll = { vm.clearAllData() },
                    isDarkMode = isDark,
                    onDarkMode = { vm.setDarkMode(it) }
                )
            }
            composable(Routes.NEW_GAME) {
                // Track per-player color overrides locally
                val colorOverrides = remember { mutableStateMapOf<Int, String>() }
                NewGameScreen(
                    players = players,
                    liveGame = liveGame,
                    onTogglePlayer = { player ->
                        val selectedIds = liveGame.selectedPlayers.map { it.playerId }
                        if (player.id in selectedIds) {
                            // Re-init without this player
                            val remaining = players.filter {
                                it.id in selectedIds && it.id != player.id
                            }
                            vm.initLiveGame(remaining, colorOverrides)
                        } else {
                            val newList = players.filter { it.id in selectedIds + player.id }
                            vm.initLiveGame(newList, colorOverrides)
                        }
                    },
                    onSetPlayerColor = { pid, color ->
                        colorOverrides[pid] = color
                        val selectedPlayers = players.filter { it.id in liveGame.selectedPlayers.map { lp -> lp.playerId } }
                        vm.initLiveGame(selectedPlayers, colorOverrides)
                    },
                    onToggleExpansion = { vm.toggleExpansion(it) },
                    onSetRiver = { vm.setRiverLayout(it) },
                    onSetTimed = { vm.setTimedTurns(it) },
                    onStartGame = {
                        if (liveGame.selectedPlayers.size >= 2) {
                            navController.navigate(Routes.LIVE_GAME)
                        }
                    },
                    onAddPlayer = { showAddPlayer = true }
                )
            }
            composable(Routes.LIVE_GAME) {
                LiveGameScreen(
                    liveGame = liveGame,
                    onAdjustScore = { pid, delta -> vm.adjustScore(pid, delta) },
                    onAddObject = { pid, type, pts, label -> vm.addScoringObject(pid, type, pts, label) },
                    onUndoLast = { vm.undoLastEvent(it) },
                    onFinish = { navController.navigate(Routes.ENDGAME) }
                )
            }
            composable(Routes.ENDGAME) {
                val pendingPhoto by vm.pendingGamePhoto.collectAsState()
                EndgameScreen(
                    liveGame = liveGame,
                    onApply = { results ->
                        vm.applyEndgame(results)
                        navController.navigate(Routes.LIVE_GAME) { popUpTo(Routes.ENDGAME) { inclusive = true } }
                    },
                    onSave = { name, notes ->
                        vm.saveGame(name, notes) { gameId ->
                            navController.navigate(Routes.matchDetail(gameId)) {
                                popUpTo(Routes.DASHBOARD) { inclusive = false }
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                    pendingPhotoPath = pendingPhoto,
                    onSetPhoto = { vm.setPendingGamePhoto(it) }
                )
            }
            composable(Routes.MATCH_DETAIL) { back ->
                val gameId = back.arguments?.getString("gameId")?.toIntOrNull() ?: return@composable
                MatchDetailScreen(
                    gameId = gameId,
                    viewModel = vm,
                    players = players,
                    onEdit = { navController.navigate(Routes.editGame(gameId)) },
                    onPlayerClick = { navController.navigate(Routes.playerProfile(it)) }
                )
            }
            composable(Routes.PLAYER_PROFILE) { back ->
                val playerId = back.arguments?.getString("playerId")?.toIntOrNull() ?: return@composable
                PlayerProfileScreen(
                    playerId = playerId,
                    viewModel = vm,
                    onEdit = { navController.navigate(Routes.editPlayer(playerId)) }
                )
            }
            composable(Routes.EDIT_PLAYER) { back ->
                val playerId = back.arguments?.getString("playerId")?.toIntOrNull() ?: return@composable
                val player = players.find { it.id == playerId } ?: return@composable
                EditPlayerScreen(
                    player = player,
                    onSave = { vm.updatePlayer(it) },
                    onDone = { navController.popBackStack() }
                )
            }
            composable(Routes.EDIT_GAME) { back ->
                val gameId = back.arguments?.getString("gameId")?.toIntOrNull() ?: return@composable
                EditGameScreen(
                    gameId = gameId,
                    viewModel = vm,
                    allPlayers = players,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
