package com.carcassonne.companion.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object WidgetUpdateScheduler {

    fun schedule(context: Context, widgetId: Int, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LeaderboardWidget::class.java).apply {
            action = LeaderboardWidget.ACTION_REFRESH
        }
        val pi = PendingIntent.getBroadcast(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMs = intervalMinutes * 60 * 1000L
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + intervalMs,
            intervalMs,
            pi
        )
    }

    fun scheduleLastBattle(context: Context, widgetId: Int, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LastBattleWidget::class.java).apply {
            action = LastBattleWidget.ACTION_REFRESH
        }
        val pi = PendingIntent.getBroadcast(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMs = intervalMinutes * 60 * 1000L
        alarmManager.setInexactRepeating(
            AlarmManager.RTC,
            System.currentTimeMillis() + intervalMs,
            intervalMs,
            pi
        )
    }

    fun cancel(context: Context, widgetId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, LeaderboardWidget::class.java).apply {
            action = LeaderboardWidget.ACTION_REFRESH
        }
        val pi = PendingIntent.getBroadcast(
            context, widgetId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }
}
