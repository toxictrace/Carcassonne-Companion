package com.carcassonne.companion.widget

import android.content.Context

data class LastBattleWidgetPrefs(
    val gameSelection: Int = GAME_LAST,
    val specificGameId: Int = -1,
    val showPhoto: Boolean = true,
    val showNotes: Boolean = true,
    val theme: Int = THEME_SYSTEM
) {
    companion object {
        const val GAME_LAST     = 0
        const val GAME_RANDOM   = 1
        const val GAME_SPECIFIC = 2

        const val THEME_SYSTEM = 0
        const val THEME_DARK   = 1
        const val THEME_LIGHT  = 2

        fun get(context: Context, widgetId: Int): LastBattleWidgetPrefs {
            val p = context.getSharedPreferences("last_battle_widget_prefs", Context.MODE_PRIVATE)
            return LastBattleWidgetPrefs(
                gameSelection  = p.getInt("selection_$widgetId", GAME_LAST),
                specificGameId = p.getInt("game_id_$widgetId", -1),
                showPhoto      = p.getBoolean("photo_$widgetId", true),
                showNotes      = p.getBoolean("notes_$widgetId", true),
                theme          = p.getInt("theme_$widgetId", THEME_SYSTEM)
            )
        }

        fun save(context: Context, widgetId: Int, prefs: LastBattleWidgetPrefs) {
            context.getSharedPreferences("last_battle_widget_prefs", Context.MODE_PRIVATE)
                .edit()
                .putInt("selection_$widgetId", prefs.gameSelection)
                .putInt("game_id_$widgetId", prefs.specificGameId)
                .putBoolean("photo_$widgetId", prefs.showPhoto)
                .putBoolean("notes_$widgetId", prefs.showNotes)
                .putInt("theme_$widgetId", prefs.theme)
                .apply()
        }

        fun delete(context: Context, widgetId: Int) {
            context.getSharedPreferences("last_battle_widget_prefs", Context.MODE_PRIVATE)
                .edit()
                .remove("selection_$widgetId")
                .remove("game_id_$widgetId")
                .remove("photo_$widgetId")
                .remove("notes_$widgetId")
                .remove("theme_$widgetId")
                .apply()
        }
    }
}
