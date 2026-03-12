package com.carcassonne.companion.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carcassonne.companion.data.dao.GameDao
import com.carcassonne.companion.data.dao.GamePlayerDao
import com.carcassonne.companion.data.dao.PlayerDao
import com.carcassonne.companion.data.entity.GameEntity
import com.carcassonne.companion.data.entity.GamePlayerEntity
import com.carcassonne.companion.data.entity.PlayerEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE players ADD COLUMN avatarPath TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE games ADD COLUMN photoPath TEXT")
    }
}

@Database(
    entities = [PlayerEntity::class, GameEntity::class, GamePlayerEntity::class],
    version = 3,
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
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { INSTANCE = it }
            }
        }
    }
}
