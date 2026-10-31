package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.carcassonne.companion.R
import com.carcassonne.companion.viewmodel.GlobalStats
import com.carcassonne.companion.ui.theme.*

@Composable
fun MetagameBreakdownBar(gs: GlobalStats) {
    val total = gs.avgCity + gs.avgRoad + gs.avgMonastery + gs.avgFarm
    if (total <= 0f) return
    val segments = listOf(
        Triple(stringResource(R.string.metagame_cities),      gs.avgCity,      CarcBlue),
        Triple(stringResource(R.string.metagame_roads),       gs.avgRoad,      CarcGreen),
        Triple(stringResource(R.string.metagame_monasteries), gs.avgMonastery, CarcYellow),
        Triple(stringResource(R.string.metagame_farms),       gs.avgFarm,      CarcOrange)
    )
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Column(Modifier.padding(14.dp)) {
            // Stacked bar
            Row(modifier = Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(6.dp))) {
                segments.forEach { (_, value, color) ->
                    val frac = (value / total).coerceIn(0f, 1f)
                    if (frac > 0.02f) {
                        Box(Modifier.fillMaxHeight().weight(frac).background(color))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // Legend
            segments.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth()) {
                    pair.forEach { (label, value, color) ->
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text(label, fontSize = 11.sp, color = CarcText2)
                                Text(stringResource(R.string.avg_pts, value), fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold, color = CarcText)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
