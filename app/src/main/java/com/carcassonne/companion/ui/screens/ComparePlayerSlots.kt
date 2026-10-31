package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*

@Composable
fun ComparePlayerSlots(
    allStats: List<com.carcassonne.companion.viewmodel.PlayerStats>,
    selectedSlots: List<Int?>,
    slotColors: List<Color>,
    onSlotClick: (slotIdx: Int) -> Unit
) {
    val activePlayers = selectedSlots.mapNotNull { id -> allStats.find { it.player.id == id } }
        Card(shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CarcCard)) {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selectedSlots.forEachIndexed { slotIdx, selectedId ->
                    val ps = allStats.find { it.player.id == selectedId }
                    val slotColor = if (ps != null) slotColors[slotIdx % 3] else CarcBorder
                    Column(
                        modifier = Modifier
                            .weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CarcBg2)
                            .border(1.5.dp, slotColor, RoundedCornerShape(10.dp))
                            .clickable { onSlotClick(slotIdx) }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (ps != null) {
                            PlayerAvatar(ps.player.name, ps.player.meepleColor,
                                avatarPath = ps.player.avatarPath, size = 36.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(ps.player.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = slotColor, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center)
                            Text(stringResource(R.string.avg_score_label, ps.avgScore), fontSize = 10.sp, color = CarcText3)
                        } else {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(50.dp))
                                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(50.dp)),
                                contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 18.sp, color = CarcText3)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(stringResource(R.string.add_btn), fontSize = 11.sp, color = CarcText3)
                            Text(stringResource(R.string.player_label), fontSize = 10.sp, color = CarcText3)
                        }
                    }
                }
            }
        }
}
