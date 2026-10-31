@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.carcassonne.companion.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import com.carcassonne.companion.ui.components.*
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.GlobalStats
import com.carcassonne.companion.viewmodel.LiveGameState
import com.carcassonne.companion.viewmodel.MainViewModel
import com.carcassonne.companion.viewmodel.PlayerStats
import com.carcassonne.companion.viewmodel.ScoringObjectType
import com.carcassonne.companion.R
import com.carcassonne.companion.viewmodel.EndgamePlayerInput
import com.carcassonne.companion.viewmodel.LivePlayerState
import com.carcassonne.companion.util.ImageUtils
import android.Manifest
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.documentfile.provider.DocumentFile
import com.carcassonne.companion.util.BackupManager
import java.io.File

// ─── Restore Picker Dialog ────────────────────────────────────────────────────
@Composable
fun RestorePickerDialog(
    files: List<File>,
    onSelect: (File) -> Unit,
    onDismiss: () -> Unit
) {
    var confirmFile by remember { mutableStateOf<File?>(null) }

    if (confirmFile != null) {
        AlertDialog(
            onDismissRequest = { confirmFile = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_msg, confirmFile!!.name), color = CarcText2) },
            confirmButton = {
                TextButton(onClick = { onSelect(confirmFile!!); confirmFile = null; onDismiss() }) {
                    Text(stringResource(R.string.restore_btn), color = CarcGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmFile = null }) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = CarcCard2
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.restore_pick_title)) },
            text = {
                Column {
                    if (files.isEmpty()) {
                        Text(stringResource(R.string.restore_no_files), color = CarcText2, fontSize = 14.sp)
                    } else {
                        Text(stringResource(R.string.backup_location), color = CarcText3, fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp))
                        files.forEach { f ->
                            val modDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US).format(java.util.Date(f.lastModified()))
                            val sizeKb = f.length() / 1024
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { confirmFile = f }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💾", fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(f.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CarcText)
                                    Text("$modDate · $sizeKb KB", fontSize = 11.sp, color = CarcText3)
                                }
                            }
                            HorizontalDivider(color = CarcText3.copy(alpha = 0.15f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            },
            containerColor = CarcCard2
        )
    }
}


// ─── New Game Screen ──────────────────────────────────────────────────────────
// ─── Add Scoring Object Bottom Sheet ─────────────────────────────────────────
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AddObjectSheet(
    playerName: String,
    meepleCol: String,
    startTab: Int = 0,
    hasInns: Boolean = true,
    onDismiss: () -> Unit,
    onScore: (ScoringObjectType, Int, String) -> Unit
) {
    val accent = meepleColor(meepleCol)
    var tab by remember { mutableStateOf(startTab) }

    // City state
    var cityTiles by remember { mutableStateOf(2) }
    var cityShields by remember { mutableStateOf(0) }
    var cityCathedral by remember { mutableStateOf(false) }
    val cityMultiplier = if (hasInns && cityCathedral) 3 else 2
    val cityPts = cityTiles * cityMultiplier + cityShields * cityMultiplier

    // Road state
    var roadTiles by remember { mutableStateOf(2) }
    var roadTavern by remember { mutableStateOf(false) }
    val roadPts = if (hasInns && roadTavern) roadTiles * 2 else roadTiles

    // Monastery state
    val monPts = 9

    // Localized abbreviations (captured for use inside onClick lambdas)
    val tileAbbr = stringResource(R.string.tile_abbr)
    val innAbbr = stringResource(R.string.inn_abbr)
    val tilesWord = stringResource(R.string.tiles_word)
    val shieldsWord = stringResource(R.string.shields_word)
    val addCityLabel = stringResource(R.string.add_city_btn, cityPts)
    val addRoadLabel = stringResource(R.string.add_road_btn, roadPts)
    val addMonLabel = stringResource(R.string.add_monastery_btn, monPts)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CarcCard2,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), Alignment.Center) {
                Box(Modifier.size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(CarcBorder))
            }
        }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_object_title, playerName), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(16.dp))

            // Tab selector
            val tabItems = listOf(
                "🏰" to stringResource(R.string.city_btn),
                "🛤️" to stringResource(R.string.road_btn),
                "⛪" to stringResource(R.string.monastery_btn)
            )
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CarcBg3),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabItems.forEachIndexed { i, (icon, label) ->
                    val sel = tab == i
                    Box(
                        Modifier.weight(1f).padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) accent.copy(alpha = 0.2f) else Color.Transparent)
                            .border(if (sel) 1.dp else 0.dp, if (sel) accent else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { tab = i }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(icon, fontSize = 18.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(2.dp))
                            Text(label, fontSize = 11.sp,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                color = if (sel) accent else CarcText3,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            when (tab) {
                0 -> { // City
                    // City state vars are declared above: cityTiles, cityShields, cityCathedral
                    ObjectStepperRow(stringResource(R.string.tiles_label), cityTiles, 1, 36, { cityTiles = it; if (cityShields > it * 2) cityShields = it * 2 })
                    Spacer(Modifier.height(16.dp))

                    // Shields — каждый тайл может нести 1 или 2 щита
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.shields_label), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.traders_note), fontSize = 11.sp, color = CarcText3)
                        }
                        // compact +/- stepper inline
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                                .clickable { if (cityShields > 0) cityShields-- },
                                contentAlignment = Alignment.Center) {
                                Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = if (cityShields > 0) CarcText else CarcText3)
                            }
                            Box(Modifier.width(44.dp).height(36.dp).background(CarcBg3)
                                .border(BorderStroke(1.dp, CarcBorder)),
                                contentAlignment = Alignment.Center) {
                                Text(cityShields.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                                    color = if (cityShields > 0) accent else CarcText3)
                            }
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                .clickable { if (cityShields < cityTiles * 2) cityShields++ },
                                contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    color = if (cityShields < cityTiles * 2) CarcText else CarcText3)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Cathedral checkbox (Inns & Cathedrals)
                    if (hasInns) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (cityCathedral) accent.copy(alpha = 0.10f) else CarcBg3)
                                .border(1.dp, if (cityCathedral) accent.copy(alpha = 0.5f) else CarcBorder, RoundedCornerShape(8.dp))
                                .clickable { cityCathedral = !cityCathedral }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("⛪", fontSize = 18.sp)
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.cathedral_label), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.tiles_x3), fontSize = 11.sp, color = CarcText3)
                            }
                            Checkbox(
                                checked = cityCathedral,
                                onCheckedChange = { cityCathedral = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accent,
                                    uncheckedColor = CarcText3,
                                    checkmarkColor = CarcBg
                                )
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    } else {
                        Spacer(Modifier.height(16.dp))
                    }

                    // Formula breakdown
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(accent.copy(alpha = 0.08f))
                            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val tileMult = if (hasInns && cityCathedral) 3 else 2
                        val tilePts = cityTiles * tileMult
                        val shieldPts = cityShields * tileMult
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🏰 ${cityTiles} $tilesWord × $tileMult", fontSize = 13.sp, color = CarcText2)
                            Text("$tilePts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (cityShields > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("🛡 ${cityShields} $shieldsWord × $tileMult", fontSize = 13.sp, color = CarcText2)
                                Text("$shieldPts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider(color = accent.copy(alpha = 0.2f), thickness = 0.5.dp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.total_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                            Text("+$cityPts", fontSize = 22.sp, fontWeight = FontWeight.Black, color = accent)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(addCityLabel, accent, onClick = {
                        val lbl = buildString {
                            append("🏰 ${cityTiles}${tileAbbr}")
                            if (cityShields > 0) append("+${cityShields}🛡")
                            if (hasInns && cityCathedral) append("+⛪")
                        }
                        onScore(ScoringObjectType.CITY, cityPts, lbl)
                        onDismiss()
                    })
                }
                1 -> { // Road
                    ObjectStepperRow(stringResource(R.string.tiles_label), roadTiles, 1, 36, { roadTiles = it })
                    Spacer(Modifier.height(14.dp))

                    // Compact tavern row — только если дополнение включено
                    if (hasInns) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CarcBg3)
                                .border(1.dp, CarcBorder, RoundedCornerShape(8.dp))
                                .clickable { roadTavern = !roadTavern }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🍺", fontSize = 18.sp)
                            Text(stringResource(R.string.inn_label), fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = roadTavern,
                                onCheckedChange = { roadTavern = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accent,
                                    uncheckedColor = CarcText3,
                                    checkmarkColor = CarcBg
                                )
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    ObjectScorePreview(stringResource(R.string.road_label), roadPts, accent,
                        if (hasInns && roadTavern) "${roadTiles}${tileAbbr} × 2 ($innAbbr)" else "${roadTiles}${tileAbbr} × 1")
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(addRoadLabel, accent, onClick = {
                        val lbl = "🛤️ ${roadTiles}${tileAbbr}" + if (hasInns && roadTavern) "+🍺" else ""
                        onScore(ScoringObjectType.ROAD, roadPts, lbl)
                        onDismiss()
                    })
                }
                2 -> { // Monastery
                    Text(stringResource(R.string.monastery_complete), fontSize = 13.sp, color = CarcText3)
                    Spacer(Modifier.height(8.dp))
                    ObjectScorePreview(stringResource(R.string.metric_monastery), monPts, accent,
                        stringResource(R.string.fully_completed))
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(addMonLabel, accent, onClick = {
                        onScore(ScoringObjectType.MONASTERY, monPts, "⛪ 9/9")
                        onDismiss()
                    })
                }
            }
        }
    }
}

@Composable
fun ObjectStepperRow(label: String, value: Int, min: Int, max: Int, onValue: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .clickable { if (value > min) onValue(value - 1) },
                contentAlignment = Alignment.Center
            ) { Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (value > min) CarcText else CarcText3) }
            Box(Modifier.width(52.dp).height(40.dp).background(CarcBg3).border(BorderStroke(1.dp, CarcBorder)),
                contentAlignment = Alignment.Center) {
                Text(value.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(CarcBg3).border(1.dp, CarcBorder, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .clickable { if (value < max) onValue(value + 1) },
                contentAlignment = Alignment.Center
            ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (value < max) CarcText else CarcText3) }
        }
    }
}

@Composable
fun ObjectScorePreview(title: String, pts: Int, accent: Color, formula: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.1f))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = accent, fontWeight = FontWeight.SemiBold)
            Text(formula, fontSize = 12.sp, color = CarcText3)
        }
        Text("+$pts", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
fun PrimaryButton(label: String, accent: Color = CarcGreen, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) accent else CarcBg3)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            color = if (enabled) CarcBg else CarcText3)
    }
}

// ─── Live Game Screen ─────────────────────────────────────────────────────────
@Composable
fun QuickScoreBtn(label: String, positive: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (positive) accent.copy(alpha = 0.15f) else CarcBg3)
            .border(1.dp, if (positive) accent.copy(alpha = 0.5f) else CarcBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (positive) accent else CarcText2)
    }
}

// ─── Score Edit Dialog ────────────────────────────────────────────────────────
@Composable
fun ScoreEditDialog(
    playerName: String,
    initial: Triple<Int, Int, Int>, // city, road, monastery
    initialFarm: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int, Int) -> Unit
) {
    var city by remember { mutableStateOf(initial.first.takeIf { it > 0 }?.toString() ?: "") }
    var road by remember { mutableStateOf(initial.second.takeIf { it > 0 }?.toString() ?: "") }
    var monastery by remember { mutableStateOf(initial.third.takeIf { it > 0 }?.toString() ?: "") }
    var farm by remember { mutableStateOf(initialFarm.takeIf { it > 0 }?.toString() ?: "") }
    val total = (city.toIntOrNull() ?: 0) + (road.toIntOrNull() ?: 0) +
                (monastery.toIntOrNull() ?: 0) + (farm.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CarcCard2,
        title = { Text(stringResource(R.string.score_edit_title, playerName), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreInputField(stringResource(R.string.city_label), city, { city = it }, Modifier.weight(1f))
                    ScoreInputField(stringResource(R.string.road_label), road, { road = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreInputField(stringResource(R.string.metric_monastery), monastery, { monastery = it }, Modifier.weight(1f))
                    ScoreInputField(stringResource(R.string.farm_input), farm, { farm = it }, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarcBg3)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.total_label), color = CarcText2)
                    Text(total.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CarcGreen)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        city.toIntOrNull() ?: 0,
                        road.toIntOrNull() ?: 0,
                        monastery.toIntOrNull() ?: 0,
                        farm.toIntOrNull() ?: 0
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CarcGreen, contentColor = CarcBg)
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = CarcText3) }
        }
    )
}

@Composable
fun ScoreInputField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter { c -> c.isDigit() }) },
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CarcGreenDeep,
            unfocusedBorderColor = CarcBorder,
            focusedLabelColor = CarcGreen,
            unfocusedLabelColor = CarcText3,
            focusedTextColor = CarcText,
            unfocusedTextColor = CarcText,
            cursorColor = CarcGreen
        )
    )
}

// ─── Endgame Screen ───────────────────────────────────────────────────────────
