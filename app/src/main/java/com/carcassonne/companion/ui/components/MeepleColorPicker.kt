package com.carcassonne.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.carcassonne.companion.ui.theme.CarcGreen
import com.carcassonne.companion.ui.theme.meepleColor

@Composable
fun MeepleColorPicker(
    selected: String,
    onSelect: (String) -> Unit,
    disabledColors: Set<String> = emptySet()
) {
    val colors = listOf("red", "blue", "green", "yellow", "black", "gray")
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            val isSelected = c == selected
            val isDisabled = c in disabledColors && !isSelected
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDisabled) meepleColor(c).copy(alpha = 0.25f) else meepleColor(c))
                    .then(
                        if (isSelected) Modifier.border(3.dp, Color.White, CircleShape)
                        else Modifier.border(2.dp, Color.Transparent, CircleShape)
                    )
                    .then(
                        if (!isDisabled) Modifier.clickable { onSelect(c) } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else if (isDisabled) {
                    Text("✕", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}
