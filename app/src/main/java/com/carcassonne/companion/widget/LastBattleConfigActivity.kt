package com.carcassonne.companion.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcassonne.companion.R
import com.carcassonne.companion.data.CarcassonneDatabase
import com.carcassonne.companion.data.entity.GameEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LastBattleConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        isEditMode = intent.getBooleanExtra("edit_mode", false)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isEditMode) {
            finish(); return
        }

        val existingPrefs = LastBattleWidgetPrefs.get(this, appWidgetId)

        setContent {
            LastBattleConfigScreen(
                initialPrefs = existingPrefs,
                isEditMode = isEditMode,
                onSave = { prefs ->
                    LastBattleWidgetPrefs.save(this, appWidgetId, prefs)
                    val manager = AppWidgetManager.getInstance(this)
                    LastBattleWidget.updateWidget(this, manager, appWidgetId)
                    if (prefs.gameSelection == LastBattleWidgetPrefs.GAME_RANDOM) {
                        WidgetUpdateScheduler.schedule(this, appWidgetId + 500, prefs.updateInterval)
                    } else {
                        WidgetUpdateScheduler.cancel(this, appWidgetId + 500)
                    }
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    })
                    if (isEditMode) moveTaskToBack(true) else finish()
                },
                onRefresh = {
                    val manager = AppWidgetManager.getInstance(this)
                    LastBattleWidget.updateWidget(this, manager, appWidgetId)
                    finish()
                },
                onDismiss = { finish() }
            )
        }
    }
}

@Composable
fun LastBattleConfigScreen(
    initialPrefs: LastBattleWidgetPrefs,
    isEditMode: Boolean,
    onSave: (LastBattleWidgetPrefs) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val CarcBg     = Color(0xFF111811)
    val CarcCard   = Color(0xFF1A2E1A)
    val CarcBorder = Color(0xFF2D4A2D)
    val CarcGreen  = Color(0xFF4ADE80)
    val CarcText   = Color(0xFFE5E7EB)
    val CarcText3  = Color(0xFF9CA3AF)

    var gameSelection  by remember { mutableIntStateOf(initialPrefs.gameSelection) }
    var specificGameId by remember { mutableIntStateOf(initialPrefs.specificGameId) }
    var showPhoto      by remember { mutableStateOf(initialPrefs.showPhoto) }
    var showNotes      by remember { mutableStateOf(initialPrefs.showNotes) }
    var updateInterval by remember { mutableIntStateOf(initialPrefs.updateInterval) }
    var theme          by remember { mutableIntStateOf(initialPrefs.theme) }
    var games          by remember { mutableStateOf<List<GameEntity>>(emptyList()) }
    var showGamePicker by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            games = CarcassonneDatabase.getInstance(context).gameDao().getAllGamesOnce()
                .sortedByDescending { it.date }
        }
    }

    val themes = stringArrayResource(R.array.widget_themes).toList()
    val themeValues = listOf(LastBattleWidgetPrefs.THEME_SYSTEM, LastBattleWidgetPrefs.THEME_DARK, LastBattleWidgetPrefs.THEME_LIGHT)

    Box(Modifier.fillMaxSize().background(CarcBg)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            // Title
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.widget_battle_settings_title),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CarcText
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = CarcText3)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Game selection
            Text(stringResource(R.string.widget_battle_label_game), fontSize = 11.sp,
                color = CarcText3, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

            // Last / Random / Specific
            listOf(
                LastBattleWidgetPrefs.GAME_LAST     to stringResource(R.string.widget_battle_game_last),
                LastBattleWidgetPrefs.GAME_RANDOM   to stringResource(R.string.widget_battle_game_random),
                LastBattleWidgetPrefs.GAME_SPECIFIC to stringResource(R.string.widget_battle_game_specific),
            ).forEach { (value, label) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(if (gameSelection == value) CarcGreen.copy(0.15f) else Color.Transparent)
                        .border(if (gameSelection == value) 1.dp else 0.dp,
                            if (gameSelection == value) CarcGreen.copy(0.5f) else Color.Transparent,
                            RoundedCornerShape(10.dp))
                        .clickable {
                            gameSelection = value
                            if (value == LastBattleWidgetPrefs.GAME_SPECIFIC) showGamePicker = true
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = gameSelection == value,
                        onClick = {
                            gameSelection = value
                            if (value == LastBattleWidgetPrefs.GAME_SPECIFIC) showGamePicker = true
                        },
                        colors = RadioButtonDefaults.colors(selectedColor = CarcGreen, unselectedColor = CarcText3)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(label, fontSize = 14.sp, color = CarcText)
                        if (value == LastBattleWidgetPrefs.GAME_SPECIFIC && gameSelection == value && specificGameId != -1) {
                            val selectedGame = games.find { it.id == specificGameId }
                            if (selectedGame != null) {
                                val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                    .format(Date(selectedGame.date))
                                Text("${selectedGame.name ?: "Партия #${selectedGame.id}"} · $date",
                                    fontSize = 11.sp, color = CarcGreen)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Game picker dialog
            if (showGamePicker && games.isNotEmpty()) {
                AlertDialog(
                    onDismissRequest = { showGamePicker = false },
                    containerColor = Color(0xFF1A2E1A),
                    title = { Text(stringResource(R.string.widget_battle_pick_game), color = CarcText) },
                    text = {
                        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                            games.forEach { game ->
                                val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                                    .format(Date(game.date))
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (game.id == specificGameId) CarcGreen.copy(0.15f) else Color.Transparent)
                                        .clickable { specificGameId = game.id; showGamePicker = false }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(game.name ?: "Партия #${game.id}", fontSize = 14.sp, color = CarcText)
                                        Text(date, fontSize = 11.sp, color = CarcText3)
                                    }
                                    if (game.id == specificGameId) Text("✓", color = CarcGreen)
                                }
                                HorizontalDivider(color = CarcBorder, thickness = 0.5.dp)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showGamePicker = false }) {
                            Text(stringResource(R.string.cancel), color = CarcText3)
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // Show photo toggle
            // Период обновления — только для случайной партии
    if (gameSelection == LastBattleWidgetPrefs.GAME_RANDOM) {
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.widget_label_update), fontSize = 11.sp,
            color = CarcText3, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
        val updates = stringArrayResource(R.array.widget_updates).toList()
        val updateValues = listOf(LastBattleWidgetPrefs.UPDATE_30M, LastBattleWidgetPrefs.UPDATE_1H,
            LastBattleWidgetPrefs.UPDATE_6H, LastBattleWidgetPrefs.UPDATE_24H)
        SegmentedSelector(
            options = updates,
            selectedIndex = updateValues.indexOf(updateInterval).takeIf { it >= 0 } ?: 1,
            onSelect = { updateInterval = updateValues[it] },
            cardColor = CarcCard, borderColor = CarcBorder,
            activeColor = CarcGreen, textColor = CarcText
        )
        Spacer(Modifier.height(20.dp))
    } else {
        Spacer(Modifier.height(20.dp))
    }

    Text(stringResource(R.string.widget_battle_label_display), fontSize = 11.sp,
                color = CarcText3, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(CarcCard).border(1.dp, CarcBorder, RoundedCornerShape(12.dp))
                    .clickable { showPhoto = !showPhoto }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.widget_battle_show_photo), fontSize = 14.sp, color = CarcText)
                Switch(checked = showPhoto, onCheckedChange = { showPhoto = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CarcBg, checkedTrackColor = CarcGreen))
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(CarcCard).border(1.dp, CarcBorder, RoundedCornerShape(12.dp))
                    .clickable { showNotes = !showNotes }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.widget_battle_show_notes), fontSize = 14.sp, color = CarcText)
                Switch(checked = showNotes, onCheckedChange = { showNotes = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = CarcBg, checkedTrackColor = CarcGreen))
            }

            Spacer(Modifier.height(20.dp))

            // Theme
            Text(stringResource(R.string.widget_label_theme), fontSize = 11.sp,
                color = CarcText3, letterSpacing = 1.sp, modifier = Modifier.padding(bottom = 8.dp))
            SegmentedSelector(
                options = themes,
                selectedIndex = themeValues.indexOf(theme),
                onSelect = { theme = themeValues[it] },
                cardColor = CarcCard, borderColor = CarcBorder,
                activeColor = CarcGreen, textColor = CarcText
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onSave(LastBattleWidgetPrefs(gameSelection, specificGameId, showPhoto, showNotes, theme, updateInterval)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarcGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    stringResource(if (isEditMode) R.string.widget_btn_save else R.string.widget_btn_add),
                    color = Color(0xFF111811), fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }

            if (isEditMode) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarcGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarcBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.widget_btn_refresh), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
