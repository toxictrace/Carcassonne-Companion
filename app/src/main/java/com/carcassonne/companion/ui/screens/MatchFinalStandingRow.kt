package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.components.RankBadge
import com.carcassonne.companion.ui.theme.CarcCard
import com.carcassonne.companion.ui.theme.CarcText3
import com.carcassonne.companion.ui.theme.meepleColor

@Composable
fun MatchFinalStandingRow(
    gamePlayer: GamePlayerEntity,
    players: List<PlayerEntity>,
    onPlayerClick: (Int) -> Unit
) {
    val p = players.find { it.id == gamePlayer.playerId }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard),
        modifier = Modifier.padding(bottom = 8.dp).clickable { p?.id?.let { onPlayerClick(it) } }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            RankBadge(gamePlayer.placement)
            Spacer(Modifier.width(10.dp))
            PlayerAvatar(
                name = p?.name ?: "?",
                color = gamePlayer.meepleColor,
                size = 40.dp,
                avatarPath = p?.avatarPath
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p?.name ?: stringResource(R.string.unknown_player), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(meepleColor(gamePlayer.meepleColor)))
                }
                Text(stringResource(R.string.pts_label, gamePlayer.finalScore), fontSize = 12.sp, color = CarcText3)
            }
            Text(gamePlayer.finalScore.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}
