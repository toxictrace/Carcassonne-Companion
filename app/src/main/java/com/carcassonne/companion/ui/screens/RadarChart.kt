package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.CarcText2
import com.carcassonne.companion.ui.theme.LocalCarcColors
import com.carcassonne.companion.viewmodel.PlayerStats
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarChart(
    players: List<PlayerStats>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val axisLabels = listOf(
        stringResource(R.string.radar_urban),
        stringResource(R.string.radar_roads),
        stringResource(R.string.radar_monks),
        stringResource(R.string.radar_farms),
        stringResource(R.string.radar_stable)
    )
    val playerValues = players.map { ps ->
        listOf(ps.urbanizationIndex, ps.roadAggrIndex, ps.monasteryIndex, ps.farmDomIndex, ps.stabilityIndex)
    }
    val n = 5
    val angleStep = (2 * Math.PI / n).toFloat()
    val startAngle = (-Math.PI / 2).toFloat()

    // Label offsets calculated outside Canvas so Compose Text can use them
    // We use BoxWithConstraints to know size at composition time
    val isDark = LocalCarcColors.current.isDark
    BoxWithConstraints(modifier = modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(cx, cy) * 0.62f
        val labelRadius = radius + 44f

        fun axisAngle(i: Int) = startAngle + i * angleStep
        fun axisPoint(i: Int, frac: Float) = Offset(
            cx + radius * frac * cos(axisAngle(i)),
            cy + radius * frac * sin(axisAngle(i))
        )
        fun labelCenter(i: Int) = Offset(
            cx + labelRadius * cos(axisAngle(i)),
            cy + labelRadius * sin(axisAngle(i))
        )

        // Canvas — grid + polygons only
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridAlpha = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF336633).copy(alpha = 0.25f)
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { r ->
                val path = Path()
                for (i in 0 until n) {
                    val pt = axisPoint(i, r)
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                path.close()
                drawPath(path, gridAlpha, style = Stroke(width = 1f))
            }
            for (i in 0 until n) {
                drawLine(gridAlpha, Offset(cx, cy), axisPoint(i, 1f), strokeWidth = 1f)
            }
            playerValues.forEachIndexed { pi, values ->
                val path = Path()
                values.forEachIndexed { i, v ->
                    val pt = axisPoint(i, v.coerceIn(0.05f, 1f))
                    if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                path.close()
                drawPath(path, colors[pi].copy(alpha = 0.18f))
                drawPath(path, colors[pi], style = Stroke(width = 2.5f))
                values.forEachIndexed { i, v ->
                    drawCircle(colors[pi], radius = 5f, center = axisPoint(i, v.coerceIn(0.05f, 1f)))
                }
            }
        }

        // Axis labels as Compose Text — positioned via offset
        val density = androidx.compose.ui.platform.LocalDensity.current
        for (i in 0 until n) {
            val lc = labelCenter(i)
            val xDp = with(density) { lc.x.toDp() }
            val yDp = with(density) { lc.y.toDp() }
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .wrapContentHeight()
                    .offset(x = xDp - 50.dp, y = yDp - 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    axisLabels[i],
                    fontSize = 9.sp,
                    color = CarcText2,
                    textAlign = TextAlign.Center,
                    lineHeight = 11.sp,
                    softWrap = false
                )
            }
        }
    }
}
