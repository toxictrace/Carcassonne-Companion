package com.carcassonne.companion.viewmodel

import com.carcassonne.companion.data.entity.PlayerEntity

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
