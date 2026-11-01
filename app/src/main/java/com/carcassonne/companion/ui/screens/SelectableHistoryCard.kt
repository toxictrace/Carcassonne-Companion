package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.theme.*

@Composable
fun SelectableHistoryCard(
    game: GameEntity,
    gamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity>,
    players: List<PlayerEntity>,
    isSelected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (isSelected) Modifier.border(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.7f), RoundedCornerShape(14.dp)) else Modifier)
    ) {
        HistoryGameCard(
            game = game,
            gamePlayers = gamePlayers,
            players = players,
            onClick = onClick,
            onLongClick = onLongClick,
            onEdit = if (selecting) ({}) else onEdit,
            onDelete = if (selecting) ({}) else onDelete,
            containerColor = if (isSelected) Color(0xFFEF4444).copy(alpha = 0.10f) else null
        )
        // Чекбокс сверху слева
        if (selecting) {
            Box(
                Modifier
                    .padding(10.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) Color(0xFFEF4444) else CarcBg3)
                    .border(1.5.dp, if (isSelected) Color(0xFFEF4444) else CarcBorder, RoundedCornerShape(6.dp))
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) Text("✓", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    lineHeight = 14.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
