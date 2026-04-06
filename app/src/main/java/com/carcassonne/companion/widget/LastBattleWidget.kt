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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.carcassonne.companion.MainActivity
import com.carcassonne.companion.R
import com.carcassonne.companion.data.CarcassonneDatabase
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LastBattleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach {
            LastBattleWidgetPrefs.delete(context, it)
            WidgetUpdateScheduler.cancel(context, it)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LastBattleWidget::class.java))
            ids.forEach { updateWidget(context, manager, it) }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.carcassonne.companion.LAST_BATTLE_REFRESH"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = LastBattleWidgetPrefs.get(context, appWidgetId)
            Log.d("LastBattleWidget", "updateWidget id=$appWidgetId prefs=$prefs")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data = loadBattleData(context, prefs)
                    val views = buildViews(context, appWidgetId, data, prefs)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e("LastBattleWidget", "Error: ${e.message}", e)
                }
            }
        }

        private suspend fun loadBattleData(
            context: Context,
            prefs: LastBattleWidgetPrefs
        ): BattleData? {
            val db = CarcassonneDatabase.getInstance(context)
            val allGames = db.gameDao().getAllGamesOnce()
            if (allGames.isEmpty()) return null

            val game = when (prefs.gameSelection) {
                LastBattleWidgetPrefs.GAME_RANDOM -> allGames.random()
                LastBattleWidgetPrefs.GAME_SPECIFIC -> {
                    allGames.find { it.id == prefs.specificGameId } ?: allGames.maxByOrNull { it.date }!!
                }
                else -> allGames.maxByOrNull { it.date }!! // GAME_LAST
            }

            val gamePlayers = db.gamePlayerDao().getPlayersForGame(game.id)
                .sortedBy { it.placement }
            val players = db.playerDao().getAllPlayersOnce()

            return BattleData(game, gamePlayers, players)
        }

        private fun buildViews(
            context: Context,
            appWidgetId: Int,
            data: BattleData?,
            prefs: LastBattleWidgetPrefs
        ): RemoteViews {
            val isDark = isDarkTheme(context, prefs.theme)
            val views = RemoteViews(context.packageName, R.layout.widget_last_battle)

            val bgColor   = if (isDark) 0xEE111811.toInt() else 0xEEF0FDF4.toInt()
            val textColor = if (isDark) 0xFFE5E7EB.toInt() else 0xFF1F2937.toInt()
            val subColor  = if (isDark) 0xFF9CA3AF.toInt() else 0xFF6B7280.toInt()
            val greenColor = 0xFF4ADE80.toInt()

            views.setInt(R.id.battle_root, "setBackgroundColor", bgColor)

            // Gear → config
            val configIntent = Intent(context, LastBattleConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("edit_mode", true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val configPi = PendingIntent.getActivity(
                context, appWidgetId + 2000, configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.battle_gear, configPi)

            // Title → open app
            val appPi = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.battle_title, appPi)

            if (data == null) {
                views.setTextViewText(R.id.battle_title, context.getString(R.string.widget_no_games))
                views.setTextColor(R.id.battle_title, textColor)
                views.setViewVisibility(R.id.battle_photo, View.GONE)
                views.setViewVisibility(R.id.battle_players_container, View.GONE)
                views.setViewVisibility(R.id.battle_notes, View.GONE)
                views.setViewVisibility(R.id.battle_share, View.GONE)
                return views
            }

            // Title — название партии
            val gameName = data.game.name ?: context.getString(R.string.widget_battle_default_name)
            views.setTextViewText(R.id.battle_title, "⚔️ $gameName")
            views.setTextColor(R.id.battle_title, textColor)

            // Дата
            val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(data.game.date))
            views.setTextViewText(R.id.battle_date, date)
            views.setTextColor(R.id.battle_date, subColor)

            // Фото
            if (prefs.showPhoto && data.game.photoPath != null) {
                val file = File(data.game.photoPath)
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(data.game.photoPath)
                    if (bmp != null) {
                        views.setImageViewBitmap(R.id.battle_photo, bmp)
                        views.setViewVisibility(R.id.battle_photo, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.battle_photo, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.battle_photo, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.battle_photo, View.GONE)
            }

            // Игроки — до 6
            data class PlayerSlot(val avatarId: Int, val nameId: Int, val scoreId: Int)
            val playerSlots = listOf(
                PlayerSlot(R.id.p1_avatar, R.id.p1_name, R.id.p1_score),
                PlayerSlot(R.id.p2_avatar, R.id.p2_name, R.id.p2_score),
                PlayerSlot(R.id.p3_avatar, R.id.p3_name, R.id.p3_score),
                PlayerSlot(R.id.p4_avatar, R.id.p4_name, R.id.p4_score),
                PlayerSlot(R.id.p5_avatar, R.id.p5_name, R.id.p5_score),
                PlayerSlot(R.id.p6_avatar, R.id.p6_name, R.id.p6_score),
            )
            val medals = listOf("🥇", "🥈", "🥉", "  4.", "  5.", "  6.")

            playerSlots.forEachIndexed { i, slot ->
                val gp = data.gamePlayers.getOrNull(i)
                if (gp != null) {
                    val player = data.players.find { it.id == gp.playerId }
                    views.setViewVisibility(slot.avatarId, View.VISIBLE)
                    views.setViewVisibility(slot.nameId, View.VISIBLE)
                    views.setViewVisibility(slot.scoreId, View.VISIBLE)


                    val mColor = LeaderboardWidget.getMeepleColor(gp.meepleColor)
                    val avatar = loadAvatar(context, player, gp.meepleColor)
                    views.setImageViewBitmap(slot.avatarId, avatar)
                    views.setTextViewText(slot.nameId, "${medals[i]} ${player?.name ?: "?"}")
                    views.setTextViewText(slot.scoreId, "${gp.finalScore}")
                    views.setTextColor(slot.nameId, mColor)
                    views.setTextColor(slot.scoreId, mColor)

                } else {
                    views.setViewVisibility(slot.avatarId, View.GONE)
                    views.setViewVisibility(slot.nameId, View.GONE)
                    views.setViewVisibility(slot.scoreId, View.GONE)

                }
            }

            // Примечания
            if (prefs.showNotes && !data.game.notes.isNullOrBlank()) {
                views.setTextViewText(R.id.battle_notes, "📝 ${data.game.notes}")
                views.setTextColor(R.id.battle_notes, subColor)
                views.setViewVisibility(R.id.battle_notes, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.battle_notes, View.GONE)
            }

            // Кнопка поделиться
            val shareIntent = Intent(context, ShareBattleActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("game_id", data.game.id)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val sharePi = PendingIntent.getActivity(
                context, appWidgetId + 3000, shareIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.battle_share, sharePi)
            views.setTextColor(R.id.battle_share, greenColor)

            return views
        }

        private fun loadAvatar(context: Context, player: PlayerEntity?, meepleColor: String): Bitmap {
            val size = 96
            val path = player?.avatarPath
            if (path != null) {
                val file = File(path)
                if (file.exists()) {
                    val raw = BitmapFactory.decodeFile(path)
                    if (raw != null) return toCircle(raw, size)
                }
            }
            return makeColorDot(LeaderboardWidget.getMeepleColor(meepleColor), size)
        }

        private fun toCircle(src: Bitmap, size: Int): Bitmap {
            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            val srcSize = minOf(src.width, src.height)
            val cropped = Bitmap.createBitmap(src, (src.width - srcSize) / 2, (src.height - srcSize) / 2, srcSize, srcSize)
            val scaled = Bitmap.createScaledBitmap(cropped, size, size, true)
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            return out
        }

        private fun makeColorDot(color: Int, size: Int): Bitmap {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            Canvas(bmp).drawCircle(size / 2f, size / 2f, size / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
            return bmp
        }

        private fun isDarkTheme(context: Context, theme: Int): Boolean = when (theme) {
            LastBattleWidgetPrefs.THEME_DARK  -> true
            LastBattleWidgetPrefs.THEME_LIGHT -> false
            else -> {
                val mask = context.resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                mask == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }

        // Генерация красивой карточки для шаринга
        fun generateShareCard(context: Context, data: BattleData): Bitmap {
            val w = 1080; val h = 720
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Фон
            canvas.drawColor(Color.parseColor("#111811"))

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Зелёная полоса сверху
            paint.color = Color.parseColor("#4ADE80")
            canvas.drawRect(0f, 0f, w.toFloat(), 8f, paint)

            // Фото слева если есть
            var playersX = 48f
            val playersW = w - 48f
            if (data.game.photoPath != null && File(data.game.photoPath).exists()) {
                val photoBmp = BitmapFactory.decodeFile(data.game.photoPath)
                if (photoBmp != null) {
                    val photoW = (w * 0.38f).toInt()
                    val photoH = h - 48
                    // Вписываем пропорционально (fitCenter)
                    val scaleX = photoW.toFloat() / photoBmp.width
                    val scaleY = photoH.toFloat() / photoBmp.height
                    val scale = minOf(scaleX, scaleY)
                    val dstW = (photoBmp.width * scale).toInt()
                    val dstH = (photoBmp.height * scale).toInt()
                    val offsetX = 24f + (photoW - dstW) / 2f
                    val offsetY = 24f + (photoH - dstH) / 2f
                    val scaled = Bitmap.createScaledBitmap(photoBmp, dstW, dstH, true)
                    canvas.drawBitmap(scaled, offsetX, offsetY, null)
                    playersX = photoW + 48f
                }
            }

            val rightW = w - playersX - 24f

            // Заголовок
            paint.color = Color.parseColor("#E5E7EB")
            paint.textSize = 42f
            paint.typeface = Typeface.DEFAULT_BOLD
            val title = "⚔️ ${data.game.name ?: context.getString(R.string.widget_battle_default_name)}"
            canvas.drawText(title, playersX, 70f, paint)

            // Дата
            paint.color = Color.parseColor("#9CA3AF")
            paint.textSize = 28f
            paint.typeface = Typeface.DEFAULT
            val date = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(data.game.date))
            canvas.drawText(date, playersX, 108f, paint)

            // Разделитель
            val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4ADE80"); alpha = 80
            }
            canvas.drawRect(playersX, 120f, w - 24f, 122f, greenPaint)

            // Игроки
            val medals = listOf("🥇", "🥈", "🥉", "  4.", "  5.", "  6.")
            val rowH = ((h - 160f) / minOf(data.gamePlayers.size, 6)).coerceAtMost(90f)

            data.gamePlayers.take(6).forEachIndexed { i, gp ->
                val player = data.players.find { it.id == gp.playerId }
                val y = 160f + i * rowH + rowH * 0.65f
                val meepleColor = LeaderboardWidget.getMeepleColor(gp.meepleColor)

                // Аватар
                val avatarSize = (rowH * 0.7f).toInt().coerceIn(40, 72)
                val avatarBmp = if (player?.avatarPath != null && File(player.avatarPath).exists()) {
                    BitmapFactory.decodeFile(player.avatarPath)?.let { toCircle(it, avatarSize) }
                } else null
                val dotBmp = makeColorDot(meepleColor, avatarSize)
                canvas.drawBitmap(avatarBmp ?: dotBmp, playersX, y - avatarSize * 0.8f, null)

                // Имя в цвете мипла
                paint.color = meepleColor
                paint.textSize = (rowH * 0.38f).coerceIn(28f, 40f)
                paint.typeface = if (i == 0) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                canvas.drawText("${medals[i]} ${player?.name ?: "?"}", playersX + avatarSize + 16f, y, paint)

                // Очки в цвете мипла
                paint.textAlign = Paint.Align.RIGHT
                paint.textSize = (rowH * 0.38f).coerceIn(28f, 40f)
                canvas.drawText("${gp.finalScore}", w - 24f, y, paint)
                paint.textAlign = Paint.Align.LEFT
            }

            // Примечания
            if (!data.game.notes.isNullOrBlank()) {
                paint.color = Color.parseColor("#9CA3AF")
                paint.textSize = 24f
                paint.typeface = Typeface.DEFAULT
                canvas.drawText("📝 ${data.game.notes.take(60)}", playersX, (h - 36).toFloat(), paint)
            }

            // Watermark
            val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#4ADE80"); alpha = 80
                textSize = 22f; textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Carcassonne Companion", (w - 24).toFloat(), (h - 12).toFloat(), wmPaint)

            return bmp
        }
    }
}

data class BattleData(
    val game: GameEntity,
    val gamePlayers: List<GamePlayerEntity>,
    val players: List<PlayerEntity>
)
