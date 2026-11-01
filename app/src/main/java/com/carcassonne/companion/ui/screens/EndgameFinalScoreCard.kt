package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.EndgamePlayerInput
import com.carcassonne.companion.viewmodel.LivePlayerState

@Composable
fun EndgameFinalScoreCard(
    player: LivePlayerState,
    input: EndgamePlayerInput,
    rank: Int,
    totalScore: Int
) {
    val accent = meepleColor(player.meepleColor)
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rank == 1) CarcGreenDeep.copy(alpha = 0.25f) else CarcCard),
        border = BorderStroke(if (rank == 1) 1.5.dp else 1.dp,
            if (rank == 1) CarcGreenDeep else accent.copy(alpha = 0.3f))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (rank == 1) "🏆" else "$rank", fontSize = if (rank == 1) 24.sp else 16.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
            PlayerAvatar(player.playerName, player.meepleColor, 40.dp, avatarPath = player.avatarPath)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(player.playerName, fontWeight = FontWeight.Bold, color = accent)
                Text(stringResource(R.string.base_score_label, player.score) + (if (input.incompleteCity + input.incompleteRoad + (if (input.hasIncompleteMonastery) 1 + input.incompleteMonastery else 0) > 0) " " + stringResource(R.string.incomplete_pts_suffix, input.incompleteCity + input.incompleteRoad + (if (input.hasIncompleteMonastery) 1 + input.incompleteMonastery else 0)) else "") + (if (input.farmCities > 0) " " + stringResource(R.string.farms_pts_suffix, input.farmCities * 3) else ""),
                    fontSize = 11.sp, color = CarcText3)
            }
            Text(totalScore.toString(),
                fontSize = 32.sp, fontWeight = FontWeight.Black, color = accent)
        }
    }
}
