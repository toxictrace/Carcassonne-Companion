package com.carcassonne.companion.widget

import android.content.Context
import android.content.SharedPreferences

data class WidgetPrefs(
    val metric: Int = METRIC_WINRATE,
    val period: Int = PERIOD_ALL,
    val updateInterval: Int = UPDATE_1H,
    val theme: Int = THEME_SYSTEM
) {
    companion object {
        const val METRIC_WINRATE = 0
        const val METRIC_WINS    = 1
        const val METRIC_AVG     = 2

        const val PERIOD_ALL   = 0
        const val PERIOD_MONTH = 1
        const val PERIOD_WEEK  = 2

        const val UPDATE_30M = 30
        const val UPDATE_1H  = 60
        const val UPDATE_6H  = 360
        const val UPDATE_24H = 1440

        const val THEME_SYSTEM = 0
        const val THEME_DARK   = 1
        const val THEME_LIGHT  = 2

        private fun prefs(context: Context): SharedPreferences =
            context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

        fun get(context: Context, widgetId: Int): WidgetPrefs {
            val p = prefs(context)
            return WidgetPrefs(
                metric         = p.getInt("metric_$widgetId", METRIC_WINRATE),
                period         = p.getInt("period_$widgetId", PERIOD_ALL),
                updateInterval = p.getInt("update_$widgetId", UPDATE_1H),
                theme          = p.getInt("theme_$widgetId",  THEME_SYSTEM)
            )
        }

        fun save(context: Context, widgetId: Int, prefs: WidgetPrefs) {
            context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("metric_$widgetId", prefs.metric)
                .putInt("period_$widgetId", prefs.period)
                .putInt("update_$widgetId", prefs.updateInterval)
                .putInt("theme_$widgetId",  prefs.theme)
                .apply()
        }

        fun delete(context: Context, widgetId: Int) {
            context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("metric_$widgetId")
                .remove("period_$widgetId")
                .remove("update_$widgetId")
                .remove("theme_$widgetId")
                .apply()
        }
    }
}
