package com.carcassonne.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.ui.theme.CarcBg3
import com.carcassonne.companion.ui.theme.CarcText2
import com.carcassonne.companion.ui.theme.CarcYellow

@Composable
fun RankBadge(rank: Int) {
    val (bg, fg) = when (rank) {
        1 -> CarcYellow to Color.Black
        2 -> Color(0xFF9CA3AF) to Color.Black
        3 -> Color(0xFFCD7C2F) to Color.Black
        else -> CarcBg3 to CarcText2
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(rank.toString(), color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
