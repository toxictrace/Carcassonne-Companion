package com.carcassonne.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.carcassonne.companion.data.dao.GameDao
import com.carcassonne.companion.data.dao.GamePlayerDao
import com.carcassonne.companion.data.dao.PlayerDao
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity

@Database(
    entities = [PlayerEntity::class, GameEntity::class, GamePlayerEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CarcassonneDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun gamePlayerDao(): GamePlayerDao

    companion object {
        @Volatile private var INSTANCE: CarcassonneDatabase? = null

        fun getInstance(context: Context): CarcassonneDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    CarcassonneDatabase::class.java,
                    "carcassonne_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
