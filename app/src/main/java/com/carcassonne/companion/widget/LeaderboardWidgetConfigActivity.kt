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

class LeaderboardWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        isEditMode = intent.getBooleanExtra("edit_mode", false)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID && !isEditMode) {
            finish()
            return
        }

        val existingPrefs = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
            WidgetPrefs.get(this, appWidgetId)
        else WidgetPrefs()

        // Не показывать в recent apps
        if (!isEditMode) {
            setTaskDescription(android.app.ActivityManager.TaskDescription("", null, 0))
        }
        setContent {
            WidgetConfigScreen(
                initialPrefs = existingPrefs,
                isEditMode = isEditMode,
                onSave = { prefs ->
                    WidgetPrefs.save(this, appWidgetId, prefs)
                    val manager = AppWidgetManager.getInstance(this)
                    LeaderboardWidget.updateWidget(this, manager, appWidgetId)
                    WidgetUpdateScheduler.schedule(this, appWidgetId, prefs.updateInterval)
                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(Activity.RESULT_OK, resultValue)
                    finish()
                },
                onRefresh = {
                    val manager = AppWidgetManager.getInstance(this)
                    LeaderboardWidget.updateWidget(this, manager, appWidgetId)
                    finish()
                },
                onDismiss = { finish() }
            )
        }
    }
}

@Composable
fun WidgetConfigScreen(
    initialPrefs: WidgetPrefs,
    isEditMode: Boolean,
    onSave: (WidgetPrefs) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val CarcBg     = Color(0xFF111811)
    val CarcCard   = Color(0xFF1A2E1A)
    val CarcCard2  = Color(0xFF1F3B1F)
    val CarcBorder = Color(0xFF2D4A2D)
    val CarcGreen  = Color(0xFF4ADE80)
    val CarcText   = Color(0xFFE5E7EB)
    val CarcText2  = Color(0xFFD1D5DB)
    val CarcText3  = Color(0xFF9CA3AF)

    var metric by remember { mutableIntStateOf(initialPrefs.metric) }
    var period by remember { mutableIntStateOf(initialPrefs.period) }
    var updateInterval by remember { mutableIntStateOf(initialPrefs.updateInterval) }
    var theme by remember { mutableIntStateOf(initialPrefs.theme) }

    val metrics = stringArrayResource(R.array.widget_metrics).toList()
    val periods = stringArrayResource(R.array.widget_periods).toList()
    val updates = stringArrayResource(R.array.widget_updates).toList()
    val themes  = stringArrayResource(R.array.widget_themes).toList()

    val metricValues = listOf(WidgetPrefs.METRIC_WINRATE, WidgetPrefs.METRIC_WINS, WidgetPrefs.METRIC_AVG)
    val periodValues = listOf(WidgetPrefs.PERIOD_ALL, WidgetPrefs.PERIOD_MONTH, WidgetPrefs.PERIOD_WEEK)
    val updateValues = listOf(WidgetPrefs.UPDATE_30M, WidgetPrefs.UPDATE_1H, WidgetPrefs.UPDATE_6H, WidgetPrefs.UPDATE_24H)
    val themeValues  = listOf(WidgetPrefs.THEME_SYSTEM, WidgetPrefs.THEME_DARK, WidgetPrefs.THEME_LIGHT)

    Box(
        Modifier
            .fillMaxSize()
            .background(CarcBg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Title
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.widget_settings_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarcText
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = CarcText3)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Metric
            SectionLabel(stringResource(R.string.widget_label_metric), CarcText3)
            SegmentedSelector(
                options = metrics,
                selectedIndex = metricValues.indexOf(metric),
                onSelect = { metric = metricValues[it] },
                cardColor = CarcCard,
                borderColor = CarcBorder,
                activeColor = CarcGreen,
                textColor = CarcText
            )

            Spacer(Modifier.height(16.dp))

            // Period
            SectionLabel(stringResource(R.string.widget_label_period), CarcText3)
            SegmentedSelector(
                options = periods,
                selectedIndex = periodValues.indexOf(period),
                onSelect = { period = periodValues[it] },
                cardColor = CarcCard,
                borderColor = CarcBorder,
                activeColor = CarcGreen,
                textColor = CarcText
            )

            Spacer(Modifier.height(16.dp))

            // Update interval
            SectionLabel(stringResource(R.string.widget_label_update), CarcText3)
            SegmentedSelector(
                options = updates,
                selectedIndex = updateValues.indexOf(updateInterval).takeIf { it >= 0 } ?: 1,
                onSelect = { updateInterval = updateValues[it] },
                cardColor = CarcCard,
                borderColor = CarcBorder,
                activeColor = CarcGreen,
                textColor = CarcText
            )

            Spacer(Modifier.height(16.dp))

            // Theme
            SectionLabel(stringResource(R.string.widget_label_theme), CarcText3)
            SegmentedSelector(
                options = themes,
                selectedIndex = themeValues.indexOf(theme),
                onSelect = { theme = themeValues[it] },
                cardColor = CarcCard,
                borderColor = CarcBorder,
                activeColor = CarcGreen,
                textColor = CarcText
            )

            Spacer(Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    onSave(WidgetPrefs(metric, period, updateInterval, theme))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CarcGreen),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    stringResource(if (isEditMode) R.string.widget_btn_save else R.string.widget_btn_add),
                    color = Color(0xFF111811),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Refresh button — только в режиме редактирования
            if (isEditMode) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CarcGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarcBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(R.string.widget_btn_refresh),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text,
        fontSize = 11.sp,
        color = color,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SegmentedSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    cardColor: Color,
    borderColor: Color,
    activeColor: Color,
    textColor: Color
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, option ->
            val isSelected = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        if (isSelected) 1.dp else 0.dp,
                        if (isSelected) activeColor.copy(alpha = 0.6f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    option,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) activeColor else textColor,
                    maxLines = 1
                )
            }
        }
    }
}
