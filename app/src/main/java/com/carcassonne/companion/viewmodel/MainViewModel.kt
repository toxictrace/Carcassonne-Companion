package com.carcassonne.companion.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carcassonne.companion.data.CarcassonneDatabase
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.data.repository.CarcassonneRepository
import com.carcassonne.companion.data.repository.PlayerResult
import com.carcassonne.companion.util.BackupManager
import kotlinx.coroutines.flow.*
import com.carcassonne.companion.R
import kotlinx.coroutines.launch
import java.io.File

// ─── Scoring object types ────────────────────────────────────────────────────
enum class ScoringObjectType { CITY, ROAD, MONASTERY, FARM }

data class ScoringEvent(
    val type: ScoringObjectType,
    val points: Int,
    val label: String   // e.g. "Город 4 плитки+2щита"
)

// ─── Endgame input per player ────────────────────────────────────────────────
data class EndgamePlayerInput(
    val playerId: Int,
    val incompleteCity: Int = 0,    // tiles + shields × 1pt each
    val incompleteRoad: Int = 0,    // tiles × 1pt each
    val incompleteMonastery: Int = 0, // 1-8 tiles around
    val farmCities: Int = 0         // completed cities adjacent to farm × 3pt
) {
    fun totalPoints() = incompleteCity + incompleteRoad + incompleteMonastery + farmCities * 3
}

// ─── Live Game State ────────────────────────────────────────────────────────
data class LivePlayerState(
    val playerId: Int,
    val playerName: String,
    val meepleColor: String,
    val avatarPath: String? = null,
    val score: Int = 0,
    val cityPoints: Int = 0,
    val roadPoints: Int = 0,
    val monasteryPoints: Int = 0,
    val farmPoints: Int = 0,
    val events: List<ScoringEvent> = emptyList()   // history of scored objects
)

data class LiveGameState(
    val selectedPlayers: List<LivePlayerState> = emptyList(),
    val expansions: Set<String> = emptySet(),   // всё выключено по умолчанию
    val riverLayout: Boolean = false,           // выключено по умолчанию
    val timedTurns: Boolean = false,
    val startTimeMs: Long = 0L,
    val isActive: Boolean = false
)

// ─── Stats ──────────────────────────────────────────────────────────────────
data class GlobalStats(
    val totalGames: Int = 0,
    val totalPlayers: Int = 0,
    val highestScore: Int = 0,
    val totalCityPoints: Int = 0,
    val totalRoadPoints: Int = 0,
    val totalFarmPoints: Int = 0,
    val totalMonasteryPoints: Int = 0,
    // Metagame averages
    val avgScore: Float = 0f,
    val avgWinnerScore: Float = 0f,
    val avgCity: Float = 0f,
    val avgRoad: Float = 0f,
    val avgMonastery: Float = 0f,
    val avgFarm: Float = 0f
)

data class PlayerStats(
    val player: PlayerEntity,
    val wins: Int,
    val gamesPlayed: Int,
    val avgScore: Float,
    val winRate: Float = if (gamesPlayed > 0) wins.toFloat() / gamesPlayed else 0f,
    // Category averages
    val avgCity: Float = 0f,
    val avgRoad: Float = 0f,
    val avgMonastery: Float = 0f,
    val avgFarm: Float = 0f,
    // Computed metrics (0..1)
    val urbanizationIndex: Float = 0f,
    val roadAggrIndex: Float = 0f,
    val monasteryIndex: Float = 0f,
    val farmDomIndex: Float = 0f,
    val stabilityIndex: Float = 0f,
    // Extra stats for Compare
    val maxScore: Int = 0,
    val minScore: Int = 0,
    val recentScores: List<Int> = emptyList(),   // last 5, newest first
    // Title
    val title: String = ""
)

// ─── Main ViewModel ─────────────────────────────────────────────────────────
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CarcassonneDatabase.getInstance(getApplication<Application>())
    private val repo = CarcassonneRepository(db.playerDao(), db.gameDao(), db.gamePlayerDao())

    // ─── Dark mode — persisted in SharedPreferences ─────────────────────────
    private val prefs = getApplication<Application>().getSharedPreferences("carc_settings", android.content.Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
        prefs.edit().putBoolean("dark_mode", dark).apply()
    }

    // ─── Compare slots persistence ───────────────────────────────────────────
    // stored as "id0,id1,id2" — -1 means empty slot
    private fun loadCompareSlots(): List<Int?> {
        val raw = prefs.getString("compare_slots", "") ?: ""
        if (raw.isEmpty()) return listOf(null, null, null)
        return raw.split(",").map { s -> s.trim().toIntOrNull()?.takeIf { it >= 0 } }
            .let { list -> List(3) { list.getOrNull(it) } }
    }

    private val _compareSlots = MutableStateFlow(loadCompareSlots())
    val compareSlots: StateFlow<List<Int?>> = _compareSlots

    // ─── Compare sections visibility persistence ────────────────────────────
    // stored as bitmask: bit i = sections[i].enabled
    fun loadCompareSections(): Int =
        prefs.getInt("compare_sections", 0b0011111)  // default: first 5 ON

    fun saveCompareSections(mask: Int) =
        prefs.edit().putInt("compare_sections", mask).apply()

    fun setCompareSlot(index: Int, playerId: Int?) {
        val updated = _compareSlots.value.toMutableList().also { it[index] = playerId }
        _compareSlots.value = updated
        prefs.edit().putString("compare_slots", updated.map { it ?: -1 }.joinToString(",")).apply()
    }

    val players: StateFlow<List<PlayerEntity>> = repo.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val games: StateFlow<List<GameEntity>> = repo.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGamePlayers: StateFlow<List<com.carcassonne.companion.data.entity.GamePlayerEntity>> = repo.allGamePlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _globalStats = MutableStateFlow(GlobalStats())
    val globalStats: StateFlow<GlobalStats> = _globalStats

    private val _playerStats = MutableStateFlow<List<PlayerStats>>(emptyList())
    val playerStats: StateFlow<List<PlayerStats>> = _playerStats

    // Live game state
    private val _liveGame = MutableStateFlow(LiveGameState())
    val liveGame: StateFlow<LiveGameState> = _liveGame

    // Snackbar / toast messages
    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    init {
        // Refresh stats whenever games or players change
        viewModelScope.launch {
            combine(games, players) { g, p -> Pair(g, p) }.collect {
                refreshStats()
            }
        }
    }

    // ─── Player CRUD ────────────────────────────────────────────
    fun addPlayer(name: String, color: String, avatarPath: String? = null) = viewModelScope.launch {
        if (name.isBlank()) { _message.emit(getApplication<Application>().getString(R.string.enter_player_name)); return@launch }
        repo.addPlayer(name.trim(), color, avatarPath)
        _message.emit(getApplication<Application>().getString(R.string.player_added, name.trim()))
    }

    fun updatePlayer(player: PlayerEntity) = viewModelScope.launch {
        repo.updatePlayer(player)
        _message.emit(getApplication<Application>().getString(R.string.profile_updated))
    }

    fun deletePlayer(player: PlayerEntity) = viewModelScope.launch {
        repo.deletePlayer(player)
        _message.emit(getApplication<Application>().getString(R.string.player_deleted))
    }

    fun deleteGame(gameId: Int) = viewModelScope.launch {
        repo.deleteGame(gameId)
        _message.emit(getApplication<Application>().getString(R.string.game_deleted))
    }

    fun updateGamePhoto(gameId: Int, path: String?) = viewModelScope.launch {
        repo.updateGamePhoto(gameId, path)
    }

    // Pending photo — set before game is saved, applied after saveGame
    private val _pendingGamePhoto = MutableStateFlow<String?>(null)
    val pendingGamePhoto: StateFlow<String?> = _pendingGamePhoto.asStateFlow()

    fun setPendingGamePhoto(path: String?) { _pendingGamePhoto.value = path }
    fun clearPendingGamePhoto() { _pendingGamePhoto.value = null }

    // ─── Live Game Setup ────────────────────────────────────────
    fun initLiveGame(players: List<PlayerEntity>, playerColors: Map<Int, String>) {
        // Сохраняем текущие настройки если игра уже настраивается (isActive=false)
        val current = _liveGame.value
        _liveGame.value = LiveGameState(
            selectedPlayers = players.map { p ->
                LivePlayerState(
                    playerId = p.id,
                    playerName = p.name,
                    meepleColor = playerColors[p.id] ?: p.meepleColor,
                    avatarPath = p.avatarPath
                )
            },
            expansions = current.expansions,      // сохраняем выбранные дополнения
            riverLayout = current.riverLayout,    // сохраняем River Layout
            timedTurns = current.timedTurns,      // сохраняем Timed Turns
            startTimeMs = System.currentTimeMillis(),
            isActive = true
        )
    }

    fun adjustScore(playerId: Int, delta: Int) {
        _liveGame.update { state ->
            state.copy(
                selectedPlayers = state.selectedPlayers.map { p ->
                    if (p.playerId == playerId) {
                        val event = ScoringEvent(ScoringObjectType.CITY, delta, "+$delta")
                        p.copy(
                            score = maxOf(0, p.score + delta),
                            cityPoints = p.cityPoints + maxOf(0, delta),
                            events = p.events + event
                        )
                    } else p
                }
            )
        }
    }

    /** Adds a completed scoring object (city/road/monastery) during gameplay */
    fun addScoringObject(
        playerId: Int,
        type: ScoringObjectType,
        points: Int,
        label: String
    ) {
        _liveGame.update { state ->
            state.copy(
                selectedPlayers = state.selectedPlayers.map { p ->
                    if (p.playerId != playerId) return@map p
                    val event = ScoringEvent(type, points, label)
                    p.copy(
                        score = p.score + points,
                        cityPoints = p.cityPoints + if (type == ScoringObjectType.CITY) points else 0,
                        roadPoints = p.roadPoints + if (type == ScoringObjectType.ROAD) points else 0,
                        monasteryPoints = p.monasteryPoints + if (type == ScoringObjectType.MONASTERY) points else 0,
                        events = p.events + event
                    )
                }
            )
        }
    }

    /** Remove last scoring event for a player */
    fun undoLastEvent(playerId: Int) {
        _liveGame.update { state ->
            state.copy(
                selectedPlayers = state.selectedPlayers.map { p ->
                    if (p.playerId != playerId || p.events.isEmpty()) return@map p
                    val last = p.events.last()
                    p.copy(
                        score = maxOf(0, p.score - last.points),
                        cityPoints = if (last.type == ScoringObjectType.CITY) maxOf(0, p.cityPoints - last.points) else p.cityPoints,
                        roadPoints = if (last.type == ScoringObjectType.ROAD) maxOf(0, p.roadPoints - last.points) else p.roadPoints,
                        monasteryPoints = if (last.type == ScoringObjectType.MONASTERY) maxOf(0, p.monasteryPoints - last.points) else p.monasteryPoints,
                        events = p.events.dropLast(1)
                    )
                }
            )
        }
    }

    /** Apply endgame scoring (incomplete objects + farms) for all players */
    fun applyEndgame(results: Map<Int, EndgamePlayerInput>) {
        _liveGame.update { state ->
            state.copy(
                selectedPlayers = state.selectedPlayers.map { p ->
                    val eg = results[p.playerId] ?: return@map p
                    val egPoints = eg.totalPoints()
                    p.copy(
                        score = p.score + egPoints,
                        cityPoints = p.cityPoints + eg.incompleteCity,
                        roadPoints = p.roadPoints + eg.incompleteRoad,
                        monasteryPoints = p.monasteryPoints + eg.incompleteMonastery,
                        farmPoints = eg.farmCities * 3
                    )
                }
            )
        }
    }

    fun toggleExpansion(exp: String) {
        _liveGame.update { state ->
            val exps = state.expansions.toMutableSet()
            if (exp in exps) exps.remove(exp) else exps.add(exp)
            state.copy(expansions = exps)
        }
    }

    fun setRiverLayout(on: Boolean) = _liveGame.update { it.copy(riverLayout = on) }
    fun setTimedTurns(on: Boolean) = _liveGame.update { it.copy(timedTurns = on) }

    fun abandonGame() {
        _liveGame.value = LiveGameState()
    }

    // ─── Save Game ──────────────────────────────────────────────
    fun saveGame(gameName: String, onDone: (Int) -> Unit) = viewModelScope.launch {
        val state = _liveGame.value
        val elapsed = (System.currentTimeMillis() - state.startTimeMs) / 1000L
        val results = state.selectedPlayers.map { p ->
            PlayerResult(
                playerId = p.playerId,
                meepleColor = p.meepleColor,
                finalScore = p.score,
                cityPoints = p.cityPoints,
                roadPoints = p.roadPoints,
                monasteryPoints = p.monasteryPoints,
                farmPoints = p.farmPoints
            )
        }
        val gameId = repo.saveGame(
            name = gameName.ifBlank { null },
            durationSeconds = elapsed,
            expansions = state.expansions.toList(),
            playerResults = results
        )
        // Применяем фото поля если было добавлено
        val photo = _pendingGamePhoto.value
        if (photo != null) {
            repo.updateGamePhoto(gameId, photo)
            _pendingGamePhoto.value = null
        }
        _liveGame.value = LiveGameState()
        _message.emit(getApplication<Application>().getString(R.string.game_saved))
        onDone(gameId)
    }

    // ─── Match Detail ───────────────────────────────────────────
    suspend fun getGameWithPlayers(gameId: Int): Pair<GameEntity?, List<GamePlayerEntity>> {
        val game = repo.getGameById(gameId)
        val players = repo.getPlayersForGame(gameId)
        return Pair(game, players)
    }

    // ─── Player Profile ─────────────────────────────────────────
    suspend fun getPlayerStats(playerId: Int): PlayerStats? {
        val player = repo.getPlayerById(playerId) ?: return null
        val wins = repo.getWinCounts().find { it.playerId == playerId }?.wins ?: 0
        val gamesPlayed = repo.getGamesForPlayer(playerId).first().size
        val avg = repo.getAvgScore(playerId)
        return PlayerStats(player, wins, gamesPlayed, avg)
    }

    fun getGamesForPlayer(playerId: Int) = repo.getGamesForPlayer(playerId)

    // ─── Stats ──────────────────────────────────────────────────
    private suspend fun refreshStats() {
        val totalGames = games.value.size
        val totalPlayers = players.value.size
        val highestScore = repo.getHighestScore()

        val globalAvgCity      = repo.getGlobalAvgCity()
        val globalAvgRoad      = repo.getGlobalAvgRoad()
        val globalAvgMonastery = repo.getGlobalAvgMonastery()
        val globalAvgFarm      = repo.getGlobalAvgFarm()

        _globalStats.value = GlobalStats(
            totalGames           = totalGames,
            totalPlayers         = totalPlayers,
            highestScore         = highestScore,
            totalCityPoints      = repo.getTotalCityPoints(),
            totalRoadPoints      = repo.getTotalRoadPoints(),
            totalFarmPoints      = repo.getTotalFarmPoints(),
            totalMonasteryPoints = repo.getTotalMonasteryPoints(),
            avgScore             = repo.getGlobalAvgScore(),
            avgWinnerScore       = repo.getAvgWinnerScore(),
            avgCity              = globalAvgCity,
            avgRoad              = globalAvgRoad,
            avgMonastery         = globalAvgMonastery,
            avgFarm              = globalAvgFarm
        )

        val winCounts = repo.getWinCounts().associateBy { it.playerId }
        val allStats = players.value.map { p ->
            val gamesPlayed = repo.getGamesForPlayer(p.id).first().size
            val wins        = winCounts[p.id]?.wins ?: 0
            val avg         = repo.getAvgScore(p.id)
            val avgCity     = repo.getAvgCityPoints(p.id)
            val avgRoad     = repo.getAvgRoadPoints(p.id)
            val avgMon      = repo.getAvgMonasteryPoints(p.id)
            val avgFarm     = repo.getAvgFarmPoints(p.id)
            val scores      = repo.getAllScores(p.id)

            // Stability = 1 - CV (coefficient of variation)
            val stability = if (scores.size >= 2 && avg > 0f) {
                val mean = scores.map { it.toFloat() }.average().toFloat()
                val variance = scores.map { (it - mean) * (it - mean) }.average().toFloat()
                val stddev = kotlin.math.sqrt(variance.toDouble()).toFloat()
                (1f - stddev / mean).coerceIn(0f, 1f)
            } else if (scores.size == 1) 1f else 0f

            // UI  = city / total
            val ui = if (avg > 0f) avgCity / avg else 0f
            // RA  = road / (city + road)
            val ra = if (avgCity + avgRoad > 0f) avgRoad / (avgCity + avgRoad) else 0f
            // MI  = monastery / total
            val mi = if (avg > 0f) avgMon / avg else 0f
            // FD  = avgFarm / globalAvgFarm, normalised 0..1 (cap at 2x)
            val fd = if (globalAvgFarm > 0f) (avgFarm / globalAvgFarm / 2f).coerceIn(0f, 1f) else 0f

            val maxScore    = repo.getMaxScore(p.id)
            val minScore    = repo.getMinScore(p.id)
            val recentScores = repo.getRecentScores(p.id, 5)

            PlayerStats(
                player = p, wins = wins, gamesPlayed = gamesPlayed, avgScore = avg,
                avgCity = avgCity, avgRoad = avgRoad, avgMonastery = avgMon, avgFarm = avgFarm,
                urbanizationIndex = ui.coerceIn(0f, 1f),
                roadAggrIndex     = ra.coerceIn(0f, 1f),
                monasteryIndex    = mi.coerceIn(0f, 1f),
                farmDomIndex      = fd,
                stabilityIndex    = stability,
                maxScore          = maxScore,
                minScore          = minScore,
                recentScores      = recentScores
            )
        }.filter { it.gamesPlayed > 0 }

        // Assign titles based on which metric is highest among all players
        val sorted = assignTitles(allStats).sortedByDescending { it.winRate }
        _playerStats.value = sorted

        // Auto-fill compare slots on first launch (all null = never saved)
        if (_compareSlots.value.all { it == null } && sorted.isNotEmpty()) {
            val autoSlots = listOf(
                sorted.getOrNull(0)?.player?.id,
                sorted.getOrNull(1)?.player?.id,
                sorted.getOrNull(2)?.player?.id
            )
            _compareSlots.value = autoSlots
            prefs.edit().putString("compare_slots",
                autoSlots.map { it ?: -1 }.joinToString(",")).apply()
        }
    }

    private fun assignTitles(stats: List<PlayerStats>): List<PlayerStats> = stats

    // ─── Game sort order — persisted in SharedPreferences ────────
    private val _sortNewestFirst = MutableStateFlow(prefs.getBoolean("sort_newest_first", true))
    val sortNewestFirst: StateFlow<Boolean> = _sortNewestFirst

    fun toggleSortOrder() {
        _sortNewestFirst.value = !_sortNewestFirst.value
        prefs.edit().putBoolean("sort_newest_first", _sortNewestFirst.value).apply()
    }

    val sortedGames: StateFlow<List<GameEntity>> = combine(games, _sortNewestFirst) { g, newest ->
        if (newest) g.sortedByDescending { it.date } else g.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Update existing game ────────────────────────────────────
    fun updateGame(
        gameId: Int,
        name: String?,
        date: Long,
        playerResults: List<PlayerResult>,
        onDone: () -> Unit = {}
    ) = viewModelScope.launch {
        repo.updateGame(gameId, name, date, playerResults)
        _message.emit(getApplication<Application>().getString(R.string.game_updated))
        onDone()
    }

    // ─── Settings ───────────────────────────────────────────────
    fun clearAllData() = viewModelScope.launch {
        repo.clearAll()
        _message.emit(getApplication<Application>().getString(R.string.records_cleared))
    }

    // ─── Backup ──────────────────────────────────────────────────
    fun exportBackup(context: Context) = viewModelScope.launch {
        try {
            val players = repo.getAllPlayersOnce()
            val games = repo.getAllGamesOnce()
            val gamePlayers = repo.getAllGamePlayersOnce()
            val file = BackupManager.createBackup(context, players, games, gamePlayers)
            _message.emit(getApplication<Application>().getString(R.string.backup_saved, file.name))
        } catch (e: Exception) {
            _message.emit(getApplication<Application>().getString(R.string.backup_error, e.message ?: ""))
        }
    }

    fun importBackup(context: Context, file: File) = viewModelScope.launch {
        try {
            val result = BackupManager.restoreBackup(context, file)
            repo.restoreFromBackup(result.players, result.games, result.gamePlayers)
            _message.emit(getApplication<Application>().getString(R.string.restore_success,
                result.players.size, result.games.size))
        } catch (e: Exception) {
            _message.emit(getApplication<Application>().getString(R.string.restore_error, e.message ?: ""))
        }
    }

    fun exportBackupToUri(context: Context, uri: android.net.Uri) = viewModelScope.launch {
        try {
            val players = repo.getAllPlayersOnce()
            val games = repo.getAllGamesOnce()
            val gamePlayers = repo.getAllGamePlayersOnce()
            BackupManager.createBackupToUri(context, uri, players, games, gamePlayers)
            _message.emit(getApplication<Application>().getString(R.string.backup_saved, "✓"))
        } catch (e: Exception) {
            _message.emit(getApplication<Application>().getString(R.string.backup_error, e.message ?: ""))
        }
    }

    fun importBackupFromUri(context: Context, uri: android.net.Uri) = viewModelScope.launch {
        try {
            val result = BackupManager.restoreBackupFromUri(context, uri)
            repo.restoreFromBackup(result.players, result.games, result.gamePlayers)
            _message.emit(getApplication<Application>().getString(R.string.restore_success,
                result.players.size, result.games.size))
        } catch (e: Exception) {
            _message.emit(getApplication<Application>().getString(R.string.restore_error, e.message ?: ""))
        }
    }
}
