package com.carcassonne.companion.data.dao

import androidx.room.*
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayers(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Int): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: PlayerEntity): Long

    @Update
    suspend fun updatePlayer(player: PlayerEntity)

    @Delete
    suspend fun deletePlayer(player: PlayerEntity)

    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY date DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Int): GameEntity?

    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGameCount(): Int

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("UPDATE games SET photoPath = :path WHERE id = :gameId")
    suspend fun updateGamePhoto(gameId: Int, path: String?)

    @Query("DELETE FROM games")
    suspend fun deleteAllGames()
}

@Dao
interface GamePlayerDao {
    @Query("SELECT * FROM game_players WHERE gameId = :gameId")
    suspend fun getPlayersForGame(gameId: Int): List<GamePlayerEntity>

    @Query("SELECT * FROM game_players")
    fun getAllGamePlayers(): Flow<List<GamePlayerEntity>>

    @Query("SELECT * FROM game_players WHERE playerId = :playerId ORDER BY gameId DESC")
    fun getGamesForPlayer(playerId: Int): Flow<List<GamePlayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePlayer(gp: GamePlayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGamePlayers(gps: List<GamePlayerEntity>)

    @Query("""
        SELECT gp.playerId, COUNT(*) as wins
        FROM game_players gp
        WHERE gp.placement = 1
        GROUP BY gp.playerId
    """)
    suspend fun getWinCountPerPlayer(): List<WinCount>

    @Query("""
        SELECT AVG(finalScore) FROM game_players WHERE playerId = :playerId
    """)
    suspend fun getAvgScoreForPlayer(playerId: Int): Float?

    @Query("SELECT MAX(finalScore) FROM game_players")
    suspend fun getHighestScore(): Int?

    @Query("SELECT SUM(cityPoints) FROM game_players")
    suspend fun getTotalCityPoints(): Int?

    @Query("SELECT SUM(roadPoints) FROM game_players")
    suspend fun getTotalRoadPoints(): Int?

    @Query("SELECT SUM(farmPoints) FROM game_players")
    suspend fun getTotalFarmPoints(): Int?

    @Query("DELETE FROM game_players")
    suspend fun deleteAll()

    @Query("DELETE FROM game_players WHERE gameId = :gameId")
    suspend fun deleteGamePlayers(gameId: Int)
}

data class WinCount(val playerId: Int, val wins: Int)
