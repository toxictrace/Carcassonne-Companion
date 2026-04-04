package com.carcassonne.companion.widget

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
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
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
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids4x2 = manager.getAppWidgetIds(
                ComponentName(context, LeaderboardWidget4x2::class.java)
            )
            val ids2x3 = manager.getAppWidgetIds(
                ComponentName(context, LeaderboardWidget2x3::class.java)
            )
            (ids4x2 + ids2x3).forEach { updateWidget(context, manager, it) }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.carcassonne.companion.WIDGET_REFRESH"

        // Meeple colors
        private val MEEPLE_COLORS = mapOf(
            "red"    to 0xFFEF4444.toInt(),
            "blue"   to 0xFF3B82F6.toInt(),
            "green"  to 0xFF22C55E.toInt(),
            "yellow" to 0xFFEAB308.toInt(),
            "black"  to 0xFF1F2937.toInt(),
            "white"  to 0xFFF9FAFB.toInt(),
            "purple" to 0xFFA855F7.toInt(),
            "orange" to 0xFFF97316.toInt(),
            "gray"   to 0xFF6B7280.toInt(),
            "pink"   to 0xFFEC4899.toInt(),
        )

        fun getMeepleColor(color: String): Int =
            MEEPLE_COLORS[color.lowercase()] ?: 0xFF6B7280.toInt()

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = WidgetPrefs.get(context, appWidgetId)
            val is4x2 = appWidgetManager.getAppWidgetInfo(appWidgetId)
                ?.provider?.className?.contains("4x2") == true

            CoroutineScope(Dispatchers.IO).launch {
                val entries = loadLeaderboard(context, prefs)
                val views = if (is4x2)
                    buildViews4x2(context, appWidgetId, entries, prefs)
                else
                    buildViews2x3(context, appWidgetId, entries, prefs)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private suspend fun loadLeaderboard(
            context: Context,
            prefs: WidgetPrefs
        ): List<LeaderboardEntry> {
            val db = CarcassonneDatabase.getInstance(context)
            val players = db.playerDao().getAllPlayersOnce()
            val allGamePlayers = db.gamePlayerDao().getAllGamePlayersOnce()
            val allGames = db.gameDao().getAllGamesOnce()

            // Фильтр по периоду
            val sinceMs = when (prefs.period) {
                WidgetPrefs.PERIOD_WEEK  -> System.currentTimeMillis() - 7L * 24 * 3600 * 1000
                WidgetPrefs.PERIOD_MONTH -> System.currentTimeMillis() - 30L * 24 * 3600 * 1000
                else -> 0L
            }

            val filteredGameIds = if (sinceMs > 0)
                allGames.filter { it.date >= sinceMs }.map { it.id }.toSet()
            else
                allGames.map { it.id }.toSet()

            val filteredGPs = allGamePlayers.filter { it.gameId in filteredGameIds }

            return players.map { player ->
                val gps = filteredGPs.filter { it.playerId == player.id }
                val played = gps.size
                val wins = gps.count { it.placement == 1 }
                val avgScore = if (played > 0) gps.map { it.finalScore }.average().toFloat() else 0f
                val winRate = if (played > 0) wins.toFloat() / played else 0f

                LeaderboardEntry(
                    playerId = player.id,
                    name = player.name,
                    meepleColor = player.meepleColor,
                    avatarPath = player.avatarPath,
                    played = played,
                    wins = wins,
                    winRate = winRate,
                    avgScore = avgScore
                )
            }
                .filter { it.played > 0 }
                .sortedByDescending {
                    when (prefs.metric) {
                        WidgetPrefs.METRIC_WINS     -> it.wins.toFloat()
                        WidgetPrefs.METRIC_AVG      -> it.avgScore
                        else                        -> it.winRate
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
            applyTheme4x2(views, isDark)
            setupRefreshButton(context, appWidgetId, views, R.id.widget_refresh)
            setupOpenAppIntent(context, views, R.id.widget_title)

            val metricLabel = getMetricLabel(context, prefs.metric)
            views.setTextViewText(R.id.widget_metric_label, metricLabel)

            val slots = listOf(
                Triple(R.id.player1_avatar, R.id.player1_name, R.id.player1_stat),
                Triple(R.id.player2_avatar, R.id.player2_name, R.id.player2_stat),
                Triple(R.id.player3_avatar, R.id.player3_name, R.id.player3_stat),
            )
            val medalEmojis = listOf("🥇", "🥈", "🥉")

            slots.forEachIndexed { i, (avatarId, nameId, statId) ->
                val entry = entries.getOrNull(i)
                if (entry != null) {
                    views.setViewVisibility(avatarId, View.VISIBLE)
                    views.setViewVisibility(nameId, View.VISIBLE)
                    views.setViewVisibility(statId, View.VISIBLE)

                    val avatar = loadAvatar(context, entry)
                    if (avatar != null) {
                        views.setImageViewBitmap(avatarId, avatar)
                    } else {
                        views.setImageViewBitmap(avatarId, makeColorCircle(getMeepleColor(entry.meepleColor)))
                    }
                    views.setTextViewText(nameId, "${medalEmojis[i]} ${entry.name}")
                    views.setTextViewText(statId, formatStat(entry, prefs.metric))
                } else {
                    views.setViewVisibility(avatarId, View.INVISIBLE)
                    views.setViewVisibility(nameId, View.INVISIBLE)
                    views.setViewVisibility(statId, View.INVISIBLE)
                }
            }
            return views
        }

        private fun buildViews2x3(
            context: Context,
            appWidgetId: Int,
            entries: List<LeaderboardEntry>,
            prefs: WidgetPrefs
        ): RemoteViews {
            val isDark = isDarkTheme(context, prefs)
            val views = RemoteViews(context.packageName, R.layout.widget_leaderboard_2x3)
            applyTheme2x3(views, isDark)
            setupRefreshButton(context, appWidgetId, views, R.id.widget_refresh)
            setupOpenAppIntent(context, views, R.id.widget_title)

            val metricLabel = getMetricLabel(context, prefs.metric)
            views.setTextViewText(R.id.widget_metric_label, metricLabel)

            val rows = listOf(
                Triple(R.id.row1_avatar, R.id.row1_name, R.id.row1_stat),
                Triple(R.id.row2_avatar, R.id.row2_name, R.id.row2_stat),
                Triple(R.id.row3_avatar, R.id.row3_name, R.id.row3_stat),
            )
            val medalEmojis = listOf("🥇", "🥈", "🥉")

            rows.forEachIndexed { i, (avatarId, nameId, statId) ->
                val entry = entries.getOrNull(i)
                if (entry != null) {
                    views.setViewVisibility(avatarId, View.VISIBLE)
                    views.setViewVisibility(nameId, View.VISIBLE)
                    views.setViewVisibility(statId, View.VISIBLE)

                    val avatar = loadAvatar(context, entry)
                    if (avatar != null) {
                        views.setImageViewBitmap(avatarId, avatar)
                    } else {
                        views.setImageViewBitmap(avatarId, makeColorCircle(getMeepleColor(entry.meepleColor)))
                    }
                    views.setTextViewText(nameId, "${medalEmojis[i]} ${entry.name}")
                    views.setTextViewText(statId, formatStat(entry, prefs.metric))
                } else {
                    views.setViewVisibility(avatarId, View.INVISIBLE)
                    views.setViewVisibility(nameId, View.INVISIBLE)
                    views.setViewVisibility(statId, View.INVISIBLE)
                }
            }
            return views
        }

        private fun applyTheme4x2(views: RemoteViews, isDark: Boolean) {
            val bg = if (isDark) 0xCC1A2E1A.toInt() else 0xCCF0FDF4.toInt()
            val textColor = if (isDark) 0xFFE5E7EB.toInt() else 0xFF1F2937.toInt()
            val subColor = if (isDark) 0xFF9CA3AF.toInt() else 0xFF6B7280.toInt()
            views.setInt(R.id.widget_root, "setBackgroundColor", bg)
            views.setTextColor(R.id.widget_title, textColor)
            views.setTextColor(R.id.widget_metric_label, subColor)
            listOf(R.id.player1_name, R.id.player2_name, R.id.player3_name)
                .forEach { views.setTextColor(it, textColor) }
            listOf(R.id.player1_stat, R.id.player2_stat, R.id.player3_stat)
                .forEach { views.setTextColor(it, subColor) }
        }

        private fun applyTheme2x3(views: RemoteViews, isDark: Boolean) {
            val bg = if (isDark) 0xCC1A2E1A.toInt() else 0xCCF0FDF4.toInt()
            val textColor = if (isDark) 0xFFE5E7EB.toInt() else 0xFF1F2937.toInt()
            val subColor = if (isDark) 0xFF9CA3AF.toInt() else 0xFF6B7280.toInt()
            views.setInt(R.id.widget_root, "setBackgroundColor", bg)
            views.setTextColor(R.id.widget_title, textColor)
            views.setTextColor(R.id.widget_metric_label, subColor)
            listOf(R.id.row1_name, R.id.row2_name, R.id.row3_name)
                .forEach { views.setTextColor(it, textColor) }
            listOf(R.id.row1_stat, R.id.row2_stat, R.id.row3_stat)
                .forEach { views.setTextColor(it, subColor) }
        }

        private fun isDarkTheme(context: Context, prefs: WidgetPrefs): Boolean {
            return when (prefs.theme) {
                WidgetPrefs.THEME_DARK  -> true
                WidgetPrefs.THEME_LIGHT -> false
                else -> {
                    val uiMode = context.resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            }
        }

        private fun setupRefreshButton(
            context: Context,
            appWidgetId: Int,
            views: RemoteViews,
            viewId: Int
        ) {
            val intent = Intent(context, LeaderboardWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            val pi = PendingIntent.getBroadcast(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(viewId, pi)
        }

        private fun setupOpenAppIntent(context: Context, views: RemoteViews, viewId: Int) {
            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(viewId, pi)
        }

        private fun loadAvatar(context: Context, entry: LeaderboardEntry): Bitmap? {
            val path = entry.avatarPath ?: return null
            val file = File(path)
            if (!file.exists()) return null
            val bmp = BitmapFactory.decodeFile(path) ?: return null
            return cropToCircle(bmp)
        }

        private fun cropToCircle(bmp: Bitmap): Bitmap {
            val size = minOf(bmp.width, bmp.height)
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            val src = Bitmap.createScaledBitmap(bmp, size, size, true)
            canvas.drawBitmap(src, 0f, 0f, paint)
            return output
        }

        private fun makeColorCircle(color: Int): Bitmap {
            val size = 64
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            return bmp
        }

        private fun formatStat(entry: LeaderboardEntry, metric: Int): String {
            return when (metric) {
                WidgetPrefs.METRIC_WINS -> "${entry.wins}W / ${entry.played}G"
                WidgetPrefs.METRIC_AVG  -> "⌀ ${entry.avgScore.toInt()} pts"
                else                    -> "${(entry.winRate * 100).toInt()}% (${entry.played}G)"
            }
        }

        private fun getMetricLabel(context: Context, metric: Int): String {
            return when (metric) {
                WidgetPrefs.METRIC_WINS -> context.getString(R.string.widget_metric_wins)
                WidgetPrefs.METRIC_AVG  -> context.getString(R.string.widget_metric_avg)
                else                    -> context.getString(R.string.widget_metric_winrate)
            }
        }
    }
}

class LeaderboardWidget4x2 : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { LeaderboardWidget.updateWidget(context, appWidgetManager, it) }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.delete(context, it); WidgetUpdateScheduler.cancel(context, it) }
    }
}

class LeaderboardWidget2x3 : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { LeaderboardWidget.updateWidget(context, appWidgetManager, it) }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetPrefs.delete(context, it); WidgetUpdateScheduler.cancel(context, it) }
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
