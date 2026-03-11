package com.carcassonne.companion.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carcassonne.companion.data.CarcassonneDatabase
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.data.repository.CarcassonneRepository
import com.carcassonne.companion.data.repository.PlayerResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─── Live Game State ────────────────────────────────────────────────────────
data class LivePlayerState(
    val playerId: Int,
    val playerName: String,
    val meepleColor: String,
    val score: Int = 0,
    val cityPoints: Int = 0,
    val roadPoints: Int = 0,
    val monasteryPoints: Int = 0,
    val farmPoints: Int = 0
)

data class LiveGameState(
    val selectedPlayers: List<LivePlayerState> = emptyList(),
    val expansions: Set<String> = setOf("inns"),
    val riverLayout: Boolean = true,
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
    val totalFarmPoints: Int = 0
)

data class PlayerStats(
    val player: PlayerEntity,
    val wins: Int,
    val gamesPlayed: Int,
    val avgScore: Float,
    val winRate: Float = if (gamesPlayed > 0) wins.toFloat() / gamesPlayed else 0f
)

// ─── Main ViewModel ─────────────────────────────────────────────────────────
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = CarcassonneDatabase.getInstance(application)
    private val repo = CarcassonneRepository(db.playerDao(), db.gameDao(), db.gamePlayerDao())

    val players: StateFlow<List<PlayerEntity>> = repo.allPlayers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val games: StateFlow<List<GameEntity>> = repo.allGames
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
    fun addPlayer(name: String, color: String) = viewModelScope.launch {
        if (name.isBlank()) { _message.emit("Enter a player name"); return@launch }
        repo.addPlayer(name.trim(), color)
        _message.emit("${name.trim()} added!")
    }

    fun updatePlayer(player: PlayerEntity) = viewModelScope.launch {
        repo.updatePlayer(player)
    }

    fun deletePlayer(player: PlayerEntity) = viewModelScope.launch {
        repo.deletePlayer(player)
    }

    // ─── Live Game Setup ────────────────────────────────────────
    fun initLiveGame(players: List<PlayerEntity>, playerColors: Map<Int, String>) {
        _liveGame.value = LiveGameState(
            selectedPlayers = players.map { p ->
                LivePlayerState(
                    playerId = p.id,
                    playerName = p.name,
                    meepleColor = playerColors[p.id] ?: p.meepleColor
                )
            },
            startTimeMs = System.currentTimeMillis(),
            isActive = true
        )
    }

    fun adjustScore(playerId: Int, delta: Int) {
        _liveGame.update { state ->
            state.copy(
                selectedPlayers = state.selectedPlayers.map { p ->
                    if (p.playerId == playerId)
                        p.copy(score = maxOf(0, p.score + delta))
                    else p
                }
            )
        }
    }

    fun setDetailedScore(
        playerId: Int,
        city: Int, road: Int, monastery: Int, farm: Int
    ) {
        val total = city + road + monastery + farm
        _liveGame.update { state ->
            state.copy(
                selectedPlayers = state.selectedPlayers.map { p ->
                    if (p.playerId == playerId)
                        p.copy(
                            score = total,
                            cityPoints = city,
                            roadPoints = road,
                            monasteryPoints = monastery,
                            farmPoints = farm
                        )
                    else p
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
        _liveGame.value = LiveGameState()
        _message.emit("Game saved!")
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
        val cityPts = repo.getTotalCityPoints()
        val roadPts = repo.getTotalRoadPoints()
        val farmPts = repo.getTotalFarmPoints()

        _globalStats.value = GlobalStats(
            totalGames = totalGames,
            totalPlayers = totalPlayers,
            highestScore = highestScore,
            totalCityPoints = cityPts,
            totalRoadPoints = roadPts,
            totalFarmPoints = farmPts
        )

        val winCounts = repo.getWinCounts().associateBy { it.playerId }
        _playerStats.value = players.value.map { p ->
            val gamesPlayed = repo.getGamesForPlayer(p.id).first().size
            val wins = winCounts[p.id]?.wins ?: 0
            val avg = repo.getAvgScore(p.id)
            PlayerStats(p, wins, gamesPlayed, avg)
        }.filter { it.gamesPlayed > 0 }.sortedByDescending { it.winRate }
    }

    // ─── Settings ───────────────────────────────────────────────
    fun clearAllData() = viewModelScope.launch {
        repo.clearAll()
        _message.emit("All records cleared")
    }
}
