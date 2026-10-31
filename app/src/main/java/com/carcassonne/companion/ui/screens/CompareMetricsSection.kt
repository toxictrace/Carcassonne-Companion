package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.PlayerStats

@Composable
fun CompareMetricsSection(
    activePlayers: List<PlayerStats>,
    colors: List<Color>
) {
    SectionLabel(stringResource(R.string.section_metrics))
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)) {
        Column(Modifier.padding(14.dp)) {
            val metrics = listOf(
                Triple(stringResource(R.string.metric_urbanization),   activePlayers.map { it.urbanizationIndex }, stringResource(R.string.metric_urbanization_desc)),
                Triple(stringResource(R.string.metric_road_aggr),     activePlayers.map { it.roadAggrIndex },    stringResource(R.string.metric_road_aggr_desc)),
                Triple(stringResource(R.string.metric_monastery),      activePlayers.map { it.monasteryIndex },   stringResource(R.string.metric_monastery_desc)),
                Triple(stringResource(R.string.metric_farm_dom), activePlayers.map { it.farmDomIndex },     stringResource(R.string.metric_farm_dom_desc)),
                Triple(stringResource(R.string.metric_stability),      activePlayers.map { it.stabilityIndex },   stringResource(R.string.metric_stability_desc))
            )
            metrics.forEachIndexed { mi, (label, values, hint) ->
                if (mi > 0) HorizontalDivider(color = CarcBorder, thickness = 0.5.dp,
                    modifier = Modifier.padding(vertical = 6.dp))
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
                Text(hint, fontSize = 10.sp, color = CarcText3)
                Spacer(Modifier.height(6.dp))
                val maxV = values.maxOrNull()?.takeIf { it > 0f } ?: 1f
                activePlayers.forEachIndexed { pi, ps ->
                    val v = values[pi]; val isWinner = v == maxV
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(RoundedCornerShape(2.dp)).background(colors[pi]))
                        Spacer(Modifier.width(5.dp))
                        Text(ps.player.name, fontSize = 10.sp, color = colors[pi],
                            modifier = Modifier.width(52.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(CarcBg3)) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(v / maxV)
                                .background(colors[pi].copy(alpha = if (isWinner) 1f else 0.55f)))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("%.0f%%".format(v * 100), fontSize = 11.sp,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                            color = if (isWinner) colors[pi] else CarcText3,
                            modifier = Modifier.width(40.dp), textAlign = TextAlign.End,
                            maxLines = 1)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
