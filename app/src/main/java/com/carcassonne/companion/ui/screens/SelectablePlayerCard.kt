package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.components.*
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.PlayerStats

@Composable
fun SelectablePlayerCard(
    player: PlayerEntity,
    stats: PlayerStats?,
    isSelected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val accent = meepleColor(player.meepleColor)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEF4444).copy(alpha = 0.12f) else CarcCard
        ),
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f)) else null
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // Чекбокс — появляется в режиме выделения
            if (selecting) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFEF4444) else CarcBg3)
                        .border(1.5.dp, if (isSelected) Color(0xFFEF4444) else CarcBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        lineHeight = 14.sp, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.width(12.dp))
            }
            PlayerAvatar(player.name, player.meepleColor, size = 48.dp, avatarPath = player.avatarPath)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(player.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (stats != null && stats.gamesPlayed > 0) {
                    Spacer(Modifier.height(4.dp))
                    WinRateBar(stats.winRate)
                }
            }
            if (stats != null && stats.gamesPlayed > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CarcCard2)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.wins_losses, stats.wins, stats.gamesPlayed - stats.wins),
                            fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarcText
                        )
                    }
                    Text(
                        stringResource(R.string.win_rate_pct, (stats.winRate * 100).toInt()),
                        fontSize = 11.sp,
                        color = if (stats.winRate >= 0.5f) CarcGreen else CarcText3,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
