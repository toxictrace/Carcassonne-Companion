@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.carcassonne.companion.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.components.PlayerAvatar
import com.carcassonne.companion.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryGameCard(
    game: GameEntity,
    gamePlayers: List<com.carcassonne.companion.data.entity.GamePlayerEntity> = emptyList(),
    players: List<PlayerEntity> = emptyList(),
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    containerColor: Color? = null,
    onDelete: () -> Unit = {}
) {
    val date = remember(game.date) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(game.date))
    }
    val sortedGP = remember(gamePlayers) { gamePlayers.sortedBy { it.placement } }

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor ?: CarcCard),
        border = BorderStroke(1.dp, CarcBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Заголовок
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CarcBg3),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, game.photoPath) {
                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            if (game.photoPath != null && java.io.File(game.photoPath).exists())
                                BitmapFactory.decodeFile(game.photoPath)?.asImageBitmap()
                            else null
                        }
                    }
                    val safeBmp = bmp
                    if (safeBmp != null) {
                        androidx.compose.foundation.Image(
                            bitmap = safeBmp, contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Text("🗺️", fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(game.name ?: "Game #${game.id}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(date, fontSize = 12.sp, color = CarcText3, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Text("✎", fontSize = 18.sp, color = CarcText3)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_btn),
                        tint = Color(0xFFEF4444)
                    )
                }
            }
            if (sortedGP.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                val rows = sortedGP.chunked(2)
                rows.forEachIndexed { rowIdx, rowPlayers ->
                    if (rowIdx > 0) Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowPlayers.forEach { gp ->
                            val p = players.find { it.id == gp.playerId }
                            Row(
                                Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlayerAvatar(p?.name ?: "?", p?.meepleColor ?: "gray", size = 24.dp, avatarPath = p?.avatarPath)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    p?.name ?: "?",
                                    fontSize = 12.sp,
                                    color = CarcText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(3.dp))
                                Box(Modifier.size(7.dp).clip(CircleShape)
                                    .background(meepleColor(gp.meepleColor)))
                            }
                        }
                        if (rowPlayers.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
