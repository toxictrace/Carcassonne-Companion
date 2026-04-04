package com.carcassonne.companion.widget

import android.util.Log
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View
import android.widget.RemoteViews
import com.carcassonne.companion.MainActivity
import com.carcassonne.companion.R
import com.carcassonne.companion.data.CarcassonneDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class LeaderboardWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            WidgetPrefs.delete(context, it)
            WidgetUpdateScheduler.cancel(context, it)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids4x2 = manager.getAppWidgetIds(ComponentName(context, LeaderboardWidget4x2::class.java))
            ids4x2.forEach { updateWidget(context, manager, it) }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.carcassonne.companion.WIDGET_REFRESH"

        private val MEEPLE_COLORS = mapOf(
            "red"    to 0xFFEF4444.toInt(),
            "blue"   to 0xFF3B82F6.toInt(),
            "green"  to 0xFF22C55E.toInt(),
            "yellow" to 0xFFEAB308.toInt(),
            "black"  to 0xFF374151.toInt(),
            "white"  to 0xFFF9FAFB.toInt(),
            "purple" to 0xFFA855F7.toInt(),
            "orange" to 0xFFF97316.toInt(),
            "gray"   to 0xFF6B7280.toInt(),
            "pink"   to 0xFFEC4899.toInt(),
        )

        fun getMeepleColor(color: String) =
            MEEPLE_COLORS[color.lowercase()] ?: 0xFF6B7280.toInt()

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = WidgetPrefs.get(context, appWidgetId)
            CoroutineScope(Dispatchers.IO).launch {
                val entries = loadLeaderboard(context, prefs)
                val views = buildViews4x2(context, appWidgetId, entries, prefs)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private suspend fun loadLeaderboard(context: Context, prefs: WidgetPrefs): List<LeaderboardEntry> {
            val db = CarcassonneDatabase.getInstance(context)
            val players = db.playerDao().getAllPlayersOnce()
            val allGamePlayers = db.gamePlayerDao().getAllGamePlayersOnce()
            val allGames = db.gameDao().getAllGamesOnce()

            val sinceMs = when (prefs.period) {
                WidgetPrefs.PERIOD_WEEK  -> System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                WidgetPrefs.PERIOD_MONTH -> System.currentTimeMillis() - 30L * 24 * 3600 * 1000
                else -> 0L
            }

            val filteredGameIds = if (sinceMs > 0)
                allGames.filter { it.date >= sinceMs }.map { it.id }.toSet()
            else allGames.map { it.id }.toSet()

            val filteredGPs = allGamePlayers.filter { it.gameId in filteredGameIds }

            return players.map { player ->
                val gps = filteredGPs.filter { it.playerId == player.id }
                val played = gps.size
                val wins = gps.count { it.placement == 1 }
                val avgScore = if (played > 0) gps.map { it.finalScore }.average().toFloat() else 0f
                val winRate = if (played > 0) wins.toFloat() / played else 0f
                LeaderboardEntry(player.id, player.name, player.meepleColor, player.avatarPath,
                    played, wins, winRate, avgScore)
            }
                .filter { it.played > 0 }
                .sortedByDescending {
                    when (prefs.metric) {
                        WidgetPrefs.METRIC_WINS -> it.wins.toFloat()
                        WidgetPrefs.METRIC_AVG  -> it.avgScore
                        else                    -> it.winRate
                    }
                }
                .take(3)
        }

        private fun buildViews4x2(
            context: Context,
            appWidgetId: Int,
            entries: List<LeaderboardEntry>,
            prefs: WidgetPrefs
        ): RemoteViews {
            val isDark = isDarkTheme(context, prefs)
            val views = RemoteViews(context.packageName, R.layout.widget_leaderboard_4x2)

            // Theme colors
            val bgColor   = if (isDark) 0xEE111811.toInt() else 0xEEF0FDF4.toInt()
            val textColor = if (isDark) 0xFFE5E7EB.toInt() else 0xFF1F2937.toInt()
            val subColor  = if (isDark) 0xFF9CA3AF.toInt() else 0xFF6B7280.toInt()
            views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)
            views.setTextColor(R.id.widget_title, textColor)
            views.setTextColor(R.id.widget_metric_label, subColor)
            listOf(R.id.player1_name, R.id.player2_name, R.id.player3_name)
                .forEach { views.setTextColor(it, textColor) }
            listOf(R.id.player1_stat, R.id.player2_stat, R.id.player3_stat)
                .forEach { views.setTextColor(it, subColor) }

            // Title
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_label))

            // Metric label
            val metricLabel = when (prefs.metric) {
                WidgetPrefs.METRIC_WINS -> context.getString(R.string.widget_metric_wins)
                WidgetPrefs.METRIC_AVG  -> context.getString(R.string.widget_metric_avg)
                else                    -> context.getString(R.string.widget_metric_winrate)
            }
            views.setTextViewText(R.id.widget_metric_label, metricLabel)

            // Gear button → open config
            val configIntent = Intent(context, LeaderboardWidgetConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("edit_mode", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val configPi = PendingIntent.getActivity(
                context, appWidgetId + 1000, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_gear, configPi)

            // Title click → open app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPi = PendingIntent.getActivity(
                context, 0, appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, appPi)

            // Players
            val slots = listOf(
                Triple(R.id.player1_avatar, R.id.player1_name, R.id.player1_stat),
                Triple(R.id.player2_avatar, R.id.player2_name, R.id.player2_stat),
                Triple(R.id.player3_avatar, R.id.player3_name, R.id.player3_stat),
            )
            val medals = listOf("🥇", "🥈", "🥉")

            slots.forEachIndexed { i, (avatarId, nameId, statId) ->
                val entry = entries.getOrNull(i)
                if (entry != null) {
                    views.setViewVisibility(avatarId, View.VISIBLE)
                    views.setViewVisibility(nameId, View.VISIBLE)
                    views.setViewVisibility(statId, View.VISIBLE)

                    // Avatar — круглый, без искажений
                    val avatar = loadAvatar(context, entry)
                    views.setImageViewBitmap(avatarId, avatar)

                    views.setTextViewText(nameId, "${medals[i]} ${entry.name}")
                    views.setTextViewText(statId, formatStat(context, entry, prefs.metric))
                } else {
                    views.setViewVisibility(avatarId, View.INVISIBLE)
                    views.setViewVisibility(nameId, View.INVISIBLE)
                    views.setViewVisibility(statId, View.INVISIBLE)
                }
            }
            return views
        }

        private fun loadAvatar(context: Context, entry: LeaderboardEntry): Bitmap {
            val size = 128 // фиксированный размер для RemoteViews
            val path = entry.avatarPath
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    val raw = BitmapFactory.decodeFile(path)
                    if (raw != null) return toCircle(raw, size)
                }
            }
            return makeColorDot(getMeepleColor(entry.meepleColor), size)
        }

        // Обрезает bitmap в круг с заданным размером — без искажений
        private fun toCircle(src: Bitmap, size: Int): Bitmap {
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Рисуем круглую маску
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

            // Накладываем изображение поверх маски
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            // Crop центральный квадрат из источника
            val srcSize = minOf(src.width, src.height)
            val srcX = (src.width - srcSize) / 2
            val srcY = (src.height - srcSize) / 2
            val cropped = Bitmap.createBitmap(src, srcX, srcY, srcSize, srcSize)
            val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)
            canvas.drawBitmap(scaled, 0f, 0f, paint)

            return out
        }

        private fun makeColorDot(color: Int, size: Int): Bitmap {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            return bmp
        }

        private fun formatStat(context: Context, entry: LeaderboardEntry, metric: Int): String {
            return when (metric) {
                WidgetPrefs.METRIC_WINS -> "${entry.wins}В / ${entry.played}И"
                WidgetPrefs.METRIC_AVG  -> "⌀ ${entry.avgScore.toInt()}"
                else                    -> "${(entry.winRate * 100).toInt()}% (${entry.played}И)"
            }
        }

        private fun isDarkTheme(context: Context, prefs: WidgetPrefs): Boolean {
            return when (prefs.theme) {
                WidgetPrefs.THEME_DARK  -> true
                WidgetPrefs.THEME_LIGHT -> false
                else -> {
                    val mask = context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    mask == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
        }
    }
}

class LeaderboardWidget4x2 : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        android.util.Log.e("CarcWidget4x2", "onUpdate called! ids=${appWidgetIds.toList()}")
        try {
            appWidgetIds.forEach { LeaderboardWidget.updateWidget(context, appWidgetManager, it) }
        } catch (e: Exception) {
            android.util.Log.e("CarcWidget4x2", "onUpdate crash: ${e.message}", e)
            // Показать простой fallback
            appWidgetIds.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_leaderboard_4x2)
                views.setTextViewText(R.id.widget_title, "Ошибка: ${e.message?.take(50)}")
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            WidgetPrefs.delete(context, it)
            WidgetUpdateScheduler.cancel(context, it)
        }
    }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == LeaderboardWidget.ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, LeaderboardWidget4x2::class.java)
            )
            ids.forEach { LeaderboardWidget.updateWidget(context, manager, it) }
        }
    }
}

data class LeaderboardEntry(
    val playerId: Int,
    val name: String,
    val meepleColor: String,
    val avatarPath: String?,
    val played: Int,
    val wins: Int,
    val winRate: Float,
    val avgScore: Float
)
