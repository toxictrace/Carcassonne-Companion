package com.carcassonne.companion.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.ui.theme.*
import com.carcassonne.companion.viewmodel.ScoringObjectType

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
    val coatsOfArmsWord = pluralStringResource(R.plurals.coats_of_arms, cityShields)
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
                                Text("🛡 ${cityShields} $coatsOfArmsWord × $tileMult", fontSize = 13.sp, color = CarcText2)
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

