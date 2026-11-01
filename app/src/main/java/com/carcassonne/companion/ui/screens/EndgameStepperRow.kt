package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.ui.theme.*

@Composable
fun EndgameStepperRow(label: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 13.sp, color = CarcText2)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                .clickable { if (value > min) onValue(value - 1) },
                contentAlignment = Alignment.Center) {
                Text("−", fontSize = 18.sp, color = if (value > min) CarcText else CarcText3)
            }
            Box(Modifier.width(44.dp).height(32.dp).background(CarcBg3)
                .border(BorderStroke(1.dp, CarcBorder)),
                contentAlignment = Alignment.Center) {
                Text(value.toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                .clickable { if (value < max) onValue(value + 1) },
                contentAlignment = Alignment.Center) {
                Text("+", fontSize = 18.sp, color = if (value < max) CarcText else CarcText3)
            }
        }
    }
}
