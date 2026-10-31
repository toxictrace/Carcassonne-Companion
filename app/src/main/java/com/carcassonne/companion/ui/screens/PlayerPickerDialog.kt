package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*

@Composable
fun PlayerPickerDialog(
    slotIdx: Int,
    currentId: Int?,
    availablePlayers: List<com.carcassonne.companion.viewmodel.PlayerStats>,
    slotColor: Color,
    onRemovePlayer: () -> Unit,
    onPlayerSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarcCard2,
        title = { Text(stringResource(R.string.select_player), fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                if (currentId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onRemovePlayer)
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✕", fontSize = 14.sp, color = CarcRed,
                            modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.remove_player), fontSize = 14.sp, color = CarcRed)
                    }
                    HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 4.dp))
                }
                availablePlayers.forEach { candidate ->
                    val isSelected = candidate.player.id == currentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) slotColor.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onPlayerSelected(candidate.player.id) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerAvatar(candidate.player.name, candidate.player.meepleColor,
                            avatarPath = candidate.player.avatarPath, size = 32.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(candidate.player.name, fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) slotColor else CarcText)
                            Text(stringResource(R.string.wr_avg, (candidate.winRate * 100).toInt(), candidate.avgScore),
                                fontSize = 11.sp, color = CarcText3)
                        }
                        if (isSelected) {
                            Spacer(Modifier.width(6.dp))
                            Text("✓", fontSize = 14.sp, color = slotColor)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = CarcText3)
            }
        }
    )
}
