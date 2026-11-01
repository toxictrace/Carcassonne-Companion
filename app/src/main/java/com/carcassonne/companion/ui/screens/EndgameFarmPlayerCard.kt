package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.EndgamePlayerInput
import com.carcassonne.companion.viewmodel.LivePlayerState

@Composable
fun EndgameFarmPlayerCard(
    player: LivePlayerState,
    input: EndgamePlayerInput,
    onFarmCitiesChange: (Int) -> Unit
) {
    val accent = meepleColor(player.meepleColor)
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f))) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerAvatar(player.playerName, player.meepleColor, 36.dp, avatarPath = player.avatarPath)
                Spacer(Modifier.width(10.dp))
                Text(player.playerName, fontWeight = FontWeight.Bold, color = accent)
            }
            Spacer(Modifier.height(12.dp))
            EndgameStepperRow(stringResource(R.string.completed_cities_adjacent), input.farmCities, 0, 30) {
                onFarmCitiesChange(it)
            }
            if (input.farmCities > 0) {
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.1f)).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.farm_cities_pts, input.farmCities), fontSize = 13.sp, color = CarcText2)
                    Text("+${input.farmCities * 3}", fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = accent)
                }
            }
        }
    }
}
