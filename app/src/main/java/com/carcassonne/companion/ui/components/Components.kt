package com.carcassonne.companion.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.ui.theme.*

// ─── Player Avatar (в профиле/таблице) ───────────────────────────────────────
@Composable
fun PlayerAvatar(
    name: String,
    color: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
    avatarPath: String? = null
) {
    val c = meepleColor(color)

    val bitmap: ImageBitmap? = remember(avatarPath) {
        if (avatarPath != null && java.io.File(avatarPath).exists()) {
            val bmp = BitmapFactory.decodeFile(avatarPath)
            bmp?.asImageBitmap()
        } else null
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(2.dp, c, CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(c.copy(alpha = 0.15f))
                .border(1.dp, c.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = c,
                fontSize = (size.value * 0.45f).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (size.value * 0.45f).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Card Avatar (на карточках игр — нейтральный фон + цветная точка мипла) ──
@Composable
fun CardAvatar(
    name: String,
    meepleColor: String,
    size: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val dotSize = (size.value * 0.32f).dp
    Box(modifier = modifier.size(size)) {
        // Нейтральный аватар
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(CarcBg3)
                .border(1.5.dp, CarcBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = CarcText2,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.Bold,
                lineHeight = (size.value * 0.42f).sp,
                textAlign = TextAlign.Center
            )
        }
        // Точка цвета мипла снизу справа
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(com.carcassonne.companion.ui.theme.meepleColor(meepleColor))
                .border(1.dp, CarcBg, CircleShape)
                .align(Alignment.BottomEnd)
        )
    }
}

// ─── Meeple Dot ──────────────────────────────────────────────────────────────
@Composable
fun MeepleDot(color: String, size: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(meepleColor(color))
    )
}

// ─── Stat Card ───────────────────────────────────────────────────────────────
@Composable
fun StatCard(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = CarcGreen
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CarcCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 10.sp,
                color = CarcText3,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

// ─── Section Header ──────────────────────────────────────────────────────────
@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action, fontSize = 13.sp, color = CarcGreen)
            }
        }
    }
}

// ─── Primary Button ──────────────────────────────────────────────────────────
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CarcGreen,
            contentColor = CarcBg,
            disabledContainerColor = CarcBorder,
            disabledContentColor = CarcText3
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
    }
}

// ─── Win Rate Bar ─────────────────────────────────────────────────────────────
@Composable
fun WinRateBar(rate: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(CarcBg3)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(rate.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(CarcGreen)
            )
        }
    }
}

// ─── Expansion Toggle Row ────────────────────────────────────────────────────
@Composable
fun ExpansionRow(
    icon: String,
    name: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CarcGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = CarcText)
            Text(subtitle, fontSize = 12.sp, color = CarcText3)
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = CarcGreen,
                uncheckedColor = CarcText3
            )
        )
    }
    HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
}

// ─── Badge ───────────────────────────────────────────────────────────────────
@Composable
fun StatusBadge(text: String, color: Color = CarcGreen) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            letterSpacing = 0.5.sp
        )
    }
}

// ─── Meeple Color Selector ───────────────────────────────────────────────────
@Composable
fun MeepleColorPicker(
    selected: String,
    onSelect: (String) -> Unit,
    disabledColors: Set<String> = emptySet()
) {
    val colors = listOf("red", "blue", "green", "yellow", "black", "gray")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEach { c ->
            val isSelected = c == selected
            val isDisabled = c in disabledColors && !isSelected
            Box(
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

// ─── Score Button ────────────────────────────────────────────────────────────
@Composable
fun ScoreAdjustButton(
    label: String,
    positive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (positive) CarcGreen.copy(alpha = 0.15f) else CarcRed.copy(alpha = 0.12f)
    val fg = if (positive) CarcGreen else CarcRed
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

// ─── Settings Row ────────────────────────────────────────────────────────────
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

// ─── Rank Badge ──────────────────────────────────────────────────────────────
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
