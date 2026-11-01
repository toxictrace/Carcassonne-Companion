package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
fun EndgameIncompletePlayerCard(
    player: LivePlayerState,
    input: EndgamePlayerInput,
    onInputChange: ((EndgamePlayerInput) -> EndgamePlayerInput) -> Unit
) {
    val accent = meepleColor(player.meepleColor)
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatar(player.playerName, player.meepleColor, 36.dp, avatarPath = player.avatarPath)
                Spacer(Modifier.width(10.dp))
                Text(player.playerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.base_score, player.score), fontSize = 13.sp, color = CarcText3)
            }
            Spacer(Modifier.height(12.dp))
            // Incomplete city tiles+shields
            EndgameStepperRow(stringResource(R.string.city_tiles_shields), input.incompleteCity, 0, 99) {
                onInputChange { current -> current.copy(incompleteCity = it) }
            }
            Spacer(Modifier.height(8.dp))
            EndgameStepperRow(stringResource(R.string.road_tiles), input.incompleteRoad, 0, 99) {
                onInputChange { current -> current.copy(incompleteRoad = it) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = input.hasIncompleteMonastery,
                    onCheckedChange = { checked ->
                        onInputChange { current -> current.copy(
                            hasIncompleteMonastery = checked,
                            incompleteMonastery = if (checked) current.incompleteMonastery else 0
                        ) }
                    }
                )
                Text(
                    stringResource(R.string.incomplete_monastery_present),
                    fontSize = 13.sp,
                    color = CarcText2
                )
            }
            if (input.hasIncompleteMonastery) {
                EndgameStepperRow(stringResource(R.string.monastery_tiles), input.incompleteMonastery, 0, 8) {
                    onInputChange { current -> current.copy(incompleteMonastery = it) }
                }
            }
            // Sub-total includes 1 point for the monastery itself.
            val monasteryPoints = if (input.hasIncompleteMonastery) 1 + input.incompleteMonastery else 0
            val sub = input.incompleteCity + input.incompleteRoad + monasteryPoints
            if (sub > 0) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(stringResource(R.string.incomplete_pts_suffix, sub), fontSize = 12.sp, color = accent)
                }
            }
        }
    }
}
