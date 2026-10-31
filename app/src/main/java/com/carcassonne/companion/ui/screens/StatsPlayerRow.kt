package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.components.WinRateBar
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.PlayerStats

@Composable
fun StatsPlayerRow(ps: PlayerStats, onPlayerClick: (Int) -> Unit = {}) {
    val accent = meepleColor(ps.player.meepleColor)
    Card(
        modifier = Modifier.clickable { onPlayerClick(ps.player.id) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PlayerAvatar(ps.player.name, ps.player.meepleColor, avatarPath = ps.player.avatarPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ps.player.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.wins_losses, ps.wins, ps.gamesPlayed - ps.wins), fontSize = 11.sp, color = CarcText3)
                Spacer(Modifier.height(4.dp))
                WinRateBar(ps.winRate)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "${(ps.winRate * 100).toInt()}%",
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = if (ps.winRate >= 0.5f) CarcGreen else CarcText2
            )
        }
    }
}
