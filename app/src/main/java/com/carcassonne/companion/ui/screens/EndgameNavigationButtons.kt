package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.ui.theme.CarcBg
import com.carcassonne.companion.ui.theme.CarcBg3
import com.carcassonne.companion.ui.theme.CarcBorder
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.CarcText2

@Composable
fun EndgameNavigationButtons(
    backLabel: String,
    nextLabel: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextFontSize: TextUnit = 14.sp
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
            .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onBack).padding(vertical = 14.dp),
            contentAlignment = Alignment.Center) {
            Text(backLabel, color = CarcText2, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        Box(Modifier.weight(2f).clip(RoundedCornerShape(10.dp))
            .background(CarcGreen).clickable(onClick = onNext).padding(vertical = 14.dp),
            contentAlignment = Alignment.Center) {
            Text(nextLabel, color = CarcBg, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = nextFontSize)
        }
    }
}
