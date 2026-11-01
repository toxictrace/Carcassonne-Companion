package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.theme.CarcBorder
import com.carcassonne.companion.ui.theme.CarcCard
import com.carcassonne.companion.ui.theme.CarcText2
import com.carcassonne.companion.ui.theme.meepleColor

@Composable
fun MatchPointsBreakdownSection(
    sorted: List<GamePlayerEntity>,
    players: List<PlayerEntity>
) {
    // Points Breakdown — всегда показываем
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.points_breakdown), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 10.dp))
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок колонок
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Spacer(Modifier.width(16.dp))
                Text("", Modifier.weight(1f))
                Text("🏰", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("🛤️", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("⛪", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("🌾", fontSize = 13.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
            sorted.forEach { gp ->
                val p = players.find { it.id == gp.playerId }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Маркер цвета мипла в данной игре
                    Box(Modifier.size(10.dp).clip(CircleShape).background(meepleColor(gp.meepleColor)))
                    Spacer(Modifier.width(6.dp))
                    Text(p?.name ?: "?", Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(gp.cityPoints.toString(), fontSize = 13.sp, color = CarcText2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Text(gp.roadPoints.toString(), fontSize = 13.sp, color = CarcText2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Text(gp.monasteryPoints.toString(), fontSize = 13.sp, color = CarcText2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Text(gp.farmPoints.toString(), fontSize = 13.sp, color = CarcText2, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
