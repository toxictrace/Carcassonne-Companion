package com.carcassonne.companion.data.repository

import com.carcassonne.companion.data.dao.GameDao
import com.carcassonne.companion.data.dao.GamePlayerDao
import com.carcassonne.companion.data.dao.PlayerDao
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

class CarcassonneRepository(
    private val playerDao: PlayerDao,
    private val gameDao: GameDao,
    private val gamePlayerDao: GamePlayerDao
) {
    // ─── Players ────────────────────────────────────────────────
    val allPlayers: Flow<List<PlayerEntity>> = playerDao.getAllPlayers()

    suspend fun addPlayer(name: String, color: String, avatarPath: String? = null): Long =
        playerDao.insertPlayer(PlayerEntity(name = name, meepleColor = color, avatarPath = avatarPath))

    suspend fun updatePlayer(player: PlayerEntity) = playerDao.updatePlayer(player)
    suspend fun deletePlayer(player: PlayerEntity) = playerDao.deletePlayer(player)
    suspend fun getPlayerById(id: Int) = playerDao.getPlayerById(id)

    // ─── Games ──────────────────────────────────────────────────
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    suspend fun getGameById(id: Int) = gameDao.getGameById(id)

    suspend fun deleteGame(gameId: Int) {
        gamePlayerDao.deleteGamePlayers(gameId)
        val game = gameDao.getGameById(gameId) ?: return
        gameDao.deleteGame(game)
    }

    suspend fun saveGame(
        name: String?,
        durationSeconds: Long?,
        expansions: List<String>,
        playerResults: List<PlayerResult>
    ): Int {
        val gameId = gameDao.insertGame(
            GameEntity(
                name = name,
                durationSeconds = durationSeconds,
                expansions = expansions.joinToString(",")
            )
        ).toInt()

        // Sort by score to assign placements
        val sorted = playerResults.sortedByDescending { it.finalScore }
        val gamePlayers = sorted.mapIndexed { index, result ->
            GamePlayerEntity(
                gameId = gameId,
                playerId = result.playerId,
                meepleColor = result.meepleColor,
                finalScore = result.finalScore,
                cityPoints = result.cityPoints,
                roadPoints = result.roadPoints,
                monasteryPoints = result.monasteryPoints,
                farmPoints = result.farmPoints,
                placement = index + 1
            )
        }
        gamePlayerDao.insertGamePlayers(gamePlayers)
        return gameId
    }

    suspend fun getPlayersForGame(gameId: Int) = gamePlayerDao.getPlayersForGame(gameId)
    fun getGamesForPlayer(playerId: Int) = gamePlayerDao.getGamesForPlayer(playerId)
    val allGamePlayers: Flow<List<GamePlayerEntity>> = gamePlayerDao.getAllGamePlayers()

    // ─── Stats ──────────────────────────────────────────────────
    suspend fun getHighestScore() = gamePlayerDao.getHighestScore() ?: 0
    suspend fun getWinCounts() = gamePlayerDao.getWinCountPerPlayer()
    suspend fun getAvgScore(playerId: Int) = gamePlayerDao.getAvgScoreForPlayer(playerId) ?: 0f
    suspend fun getTotalCityPoints() = gamePlayerDao.getTotalCityPoints() ?: 0
    suspend fun getTotalRoadPoints() = gamePlayerDao.getTotalRoadPoints() ?: 0
    suspend fun getTotalFarmPoints() = gamePlayerDao.getTotalFarmPoints() ?: 0

    suspend fun clearAll() {
        gamePlayerDao.deleteAll()
        gameDao.deleteAllGames()
    }

    suspend fun updateGame(
        gameId: Int,
        name: String?,
        date: Long,
        playerResults: List<PlayerResult>
    ) {
        val existing = gameDao.getGameById(gameId) ?: return
        gameDao.updateGame(existing.copy(name = name, date = date))
        gamePlayerDao.deleteGamePlayers(gameId)
        val sorted = playerResults.sortedByDescending { it.finalScore }
        val gamePlayers = sorted.mapIndexed { index, result ->
            GamePlayerEntity(
                gameId = gameId,
                playerId = result.playerId,
                meepleColor = result.meepleColor,
                finalScore = result.finalScore,
                cityPoints = result.cityPoints,
                roadPoints = result.roadPoints,
                monasteryPoints = result.monasteryPoints,
                farmPoints = result.farmPoints,
                placement = index + 1
            )
        }
        gamePlayerDao.insertGamePlayers(gamePlayers)
    }
}

data class PlayerResult(
    val playerId: Int,
    val meepleColor: String,
    val finalScore: Int,
    val cityPoints: Int = 0,
    val roadPoints: Int = 0,
    val monasteryPoints: Int = 0,
    val farmPoints: Int = 0
)
