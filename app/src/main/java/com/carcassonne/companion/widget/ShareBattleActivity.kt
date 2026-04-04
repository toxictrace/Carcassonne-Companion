package com.carcassonne.companion.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.core.content.FileProvider
import com.carcassonne.companion.data.CarcassonneDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ShareBattleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val gameId = intent.getIntExtra("game_id", -1)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = CarcassonneDatabase.getInstance(this@ShareBattleActivity)
                val game = db.gameDao().getGameById(gameId) ?: run {
                    finish(); return@launch
                }
                val gamePlayers = db.gamePlayerDao().getPlayersForGame(gameId).sortedBy { it.placement }
                val players = db.playerDao().getAllPlayersOnce()
                val data = BattleData(game, gamePlayers, players)

                val cardBitmap = LastBattleWidget.generateShareCard(this@ShareBattleActivity, data)

                // Сохранить во временный файл
                val file = File(cacheDir, "share_battle.png")
                FileOutputStream(file).use { out ->
                    cardBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, out)
                }

                val uri = FileProvider.getUriForFile(
                    this@ShareBattleActivity,
                    "${packageName}.provider",
                    file
                )

                withContext(Dispatchers.Main) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Поделиться результатом"))
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { finish() }
            }
        }
    }
}
