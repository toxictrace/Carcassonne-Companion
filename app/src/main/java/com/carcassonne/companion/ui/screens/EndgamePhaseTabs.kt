package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.CarcBg3
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.CarcText3

@Composable
fun EndgamePhaseTabs(
    phase: Int,
    onPhaseChange: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CarcBg3),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(stringResource(R.string.tab_incomplete), stringResource(R.string.tab_farms), stringResource(R.string.tab_confirm)).forEachIndexed { i, label ->
            val sel = phase == i
            Box(
                Modifier.weight(1f).padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (sel) CarcGreen.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onPhaseChange(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) CarcGreen else CarcText3)
            }
        }
    }
}
