package com.carcassonne.companion.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.carcassonne.companion.R

class LeaderboardWidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Если пользователь нажал назад — отмена
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_config)

        // Metric spinner
        val metricSpinner = findViewById<Spinner>(R.id.spinner_metric)
        ArrayAdapter.createFromResource(this, R.array.widget_metrics, android.R.layout.simple_spinner_item)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            .also { metricSpinner.adapter = it }

        // Period spinner
        val periodSpinner = findViewById<Spinner>(R.id.spinner_period)
        ArrayAdapter.createFromResource(this, R.array.widget_periods, android.R.layout.simple_spinner_item)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            .also { periodSpinner.adapter = it }

        // Update interval spinner
        val updateSpinner = findViewById<Spinner>(R.id.spinner_update)
        ArrayAdapter.createFromResource(this, R.array.widget_updates, android.R.layout.simple_spinner_item)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            .also { updateSpinner.adapter = it }

        // Theme spinner
        val themeSpinner = findViewById<Spinner>(R.id.spinner_theme)
        ArrayAdapter.createFromResource(this, R.array.widget_themes, android.R.layout.simple_spinner_item)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            .also { themeSpinner.adapter = it }

        // Confirm button
        findViewById<Button>(R.id.btn_add_widget).setOnClickListener {
            val metricMap = listOf(WidgetPrefs.METRIC_WINRATE, WidgetPrefs.METRIC_WINS, WidgetPrefs.METRIC_AVG)
            val periodMap = listOf(WidgetPrefs.PERIOD_ALL, WidgetPrefs.PERIOD_MONTH, WidgetPrefs.PERIOD_WEEK)
            val updateMap = listOf(WidgetPrefs.UPDATE_30M, WidgetPrefs.UPDATE_1H, WidgetPrefs.UPDATE_6H, WidgetPrefs.UPDATE_24H)
            val themeMap  = listOf(WidgetPrefs.THEME_SYSTEM, WidgetPrefs.THEME_DARK, WidgetPrefs.THEME_LIGHT)

            val prefs = WidgetPrefs(
                metric         = metricMap[metricSpinner.selectedItemPosition],
                period         = periodMap[periodSpinner.selectedItemPosition],
                updateInterval = updateMap[updateSpinner.selectedItemPosition],
                theme          = themeMap[themeSpinner.selectedItemPosition]
            )
            WidgetPrefs.save(this, appWidgetId, prefs)

            // Запустить обновление виджета
            val manager = AppWidgetManager.getInstance(this)
            LeaderboardWidget.updateWidget(this, manager, appWidgetId)

            // Запланировать периодическое обновление
            WidgetUpdateScheduler.schedule(this, appWidgetId, prefs.updateInterval)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}
