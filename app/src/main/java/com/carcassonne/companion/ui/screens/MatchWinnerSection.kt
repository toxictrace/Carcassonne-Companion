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
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.CarcGreenDeep
import com.carcassonne.companion.ui.theme.CarcText3
import com.carcassonne.companion.ui.theme.meepleColor

@Composable
fun MatchWinnerSection(
    winner: GamePlayerEntity,
    players: List<PlayerEntity>,
    onPlayerClick: (Int) -> Unit
) {
    Text(stringResource(R.string.winner_label), fontSize = 13.sp, color = CarcText3, modifier = Modifier.padding(bottom = 8.dp))
    val p = players.find { it.id == winner.playerId }
    Card(
        modifier = Modifier.clickable { p?.id?.let { onPlayerClick(it) } },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcGreenDeep.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarcGreenDeep)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(
                name = p?.name ?: "?",
                color = winner.meepleColor,
                size = 52.dp,
                avatarPath = p?.avatarPath
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p?.name ?: stringResource(R.string.unknown_player), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(meepleColor(winner.meepleColor)))
                }
                Text(stringResource(R.string.pts_mvp, winner.finalScore), fontSize = 13.sp, color = CarcText3)
            }
            Text(winner.finalScore.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = CarcGreen)
        }
    }
    Spacer(Modifier.height(16.dp))
}
