package com.carcassonne.companion.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.carcassonne.companion.ui.theme.CarcBorder
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.CarcText
import com.carcassonne.companion.ui.theme.CarcText3
import androidx.compose.foundation.background

@Composable
fun SettingsRow(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconBgColor: Color = CarcGreen.copy(alpha = 0.1f),
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = CarcText)
            Text(subtitle, fontSize = 12.sp, color = CarcText3)
        }
        trailing?.invoke() ?: Text("›", fontSize = 20.sp, color = CarcText3)
    }
    HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
}
