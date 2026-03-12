package com.carcassonne.companion.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val meepleColor: String = "red", // red, blue, green, yellow, black, gray
    val avatarPath: String? = null,   // path to saved image file
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String?,
    val date: Long = System.currentTimeMillis(),
    val durationSeconds: Long? = null,
    val expansions: String = "" // comma-separated: "inns,traders,dragon,abbey"
)

// Stores each player's result within a game
@Entity(tableName = "game_players", primaryKeys = ["gameId", "playerId"])
data class GamePlayerEntity(
    val gameId: Int,
    val playerId: Int,
    val meepleColor: String,
    val finalScore: Int = 0,
    val cityPoints: Int = 0,
    val roadPoints: Int = 0,
    val monasteryPoints: Int = 0,
    val farmPoints: Int = 0,
    val placement: Int = 0  // 1st, 2nd, 3rd, 4th place
)

// Convenience data class — joins game + players result
@Serializable
data class GameWithPlayers(
    val game: GameEntityData,
    val players: List<GamePlayerData>
)

@Serializable
data class GameEntityData(
    val id: Int,
    val name: String?,
    val date: Long,
    val durationSeconds: Long?,
    val expansions: String
)

@Serializable
data class GamePlayerData(
    val gameId: Int,
    val playerId: Int,
    val playerName: String,
    val meepleColor: String,
    val finalScore: Int,
    val cityPoints: Int,
    val roadPoints: Int,
    val monasteryPoints: Int,
    val farmPoints: Int,
    val placement: Int
)
