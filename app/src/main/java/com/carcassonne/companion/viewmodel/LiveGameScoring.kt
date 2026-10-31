package com.carcassonne.companion.viewmodel

/**
 * Pure LiveGameState transformations for scoring during an active game.
 * Kept separate from MainViewModel so the ViewModel only coordinates state updates.
 */

internal fun LiveGameState.withScoreAdjustment(playerId: Int, delta: Int): LiveGameState =
    copy(
        selectedPlayers = selectedPlayers.map { player ->
            if (player.playerId == playerId) {
                val event = ScoringEvent(ScoringObjectType.CITY, delta, "+$delta")
                player.copy(
                    score = maxOf(0, player.score + delta),
                    cityPoints = player.cityPoints + maxOf(0, delta),
                    events = player.events + event
                )
            } else player
        }
    )

internal fun LiveGameState.withScoringObject(
    playerId: Int,
    type: ScoringObjectType,
    points: Int,
    label: String
): LiveGameState =
    copy(
        selectedPlayers = selectedPlayers.map { player ->
            if (player.playerId != playerId) return@map player
            val event = ScoringEvent(type, points, label)
            player.copy(
                score = player.score + points,
                cityPoints = player.cityPoints + if (type == ScoringObjectType.CITY) points else 0,
                roadPoints = player.roadPoints + if (type == ScoringObjectType.ROAD) points else 0,
                monasteryPoints = player.monasteryPoints + if (type == ScoringObjectType.MONASTERY) points else 0,
                events = player.events + event
            )
        }
    )

internal fun LiveGameState.withLastScoringEventUndone(playerId: Int): LiveGameState =
    copy(
        selectedPlayers = selectedPlayers.map { player ->
            if (player.playerId != playerId || player.events.isEmpty()) return@map player
            val last = player.events.last()
            player.copy(
                score = maxOf(0, player.score - last.points),
                cityPoints = if (last.type == ScoringObjectType.CITY) maxOf(0, player.cityPoints - last.points) else player.cityPoints,
                roadPoints = if (last.type == ScoringObjectType.ROAD) maxOf(0, player.roadPoints - last.points) else player.roadPoints,
                monasteryPoints = if (last.type == ScoringObjectType.MONASTERY) maxOf(0, player.monasteryPoints - last.points) else player.monasteryPoints,
                events = player.events.dropLast(1)
            )
        }
    )

internal fun LiveGameState.withEndgameScoring(
    results: Map<Int, EndgamePlayerInput>
): LiveGameState =
    copy(
        selectedPlayers = selectedPlayers.map { player ->
            val endgame = results[player.playerId] ?: return@map player
            val endgamePoints = endgame.totalPoints()
            player.copy(
                score = player.score + endgamePoints,
                cityPoints = player.cityPoints + endgame.incompleteCity,
                roadPoints = player.roadPoints + endgame.incompleteRoad,
                monasteryPoints = player.monasteryPoints + endgame.incompleteMonastery,
                farmPoints = endgame.farmCities * 3
            )
        }
    )
