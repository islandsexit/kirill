package ru.vigtech.android.vigpark.database

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ Crime::class ], version=10)
@TypeConverters(CrimeTypeConverters::class)
abstract class CrimeDatabase : RoomDatabase() {

    abstract fun crimeDao(): CrimeDao
}

val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {

        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN suspect TEXT NOT NULL DEFAULT ''"
        )
    }
}

val migration_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN img_path TEXT NOT NULL DEFAULT ''"
        )
    }
}

val migration_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN img_path_full TEXT NOT NULL DEFAULT ''"
        )
    }
}

val migration_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN send BOOL NOT NULL DEFAULT 0 "
        )
    }
}

val migration_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN found BOOL NOT NULL DEFAULT 0 "
        )
    }
}
val migration_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN Zone INT NOT NULL DEFAULT 0 "
        )
    }
}

val migration_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN lon DOUBLE NOT NULL DEFAULT 0 "
        )
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN lat DOUBLE NOT NULL DEFAULT 0 "
        )
    }
}
val migration_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN info TEXT NOT NULL DEFAULT 0 "
        )
    }
}
val migration_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        saveMigration(database,
            "ALTER TABLE Crime ADD COLUMN Rect TEXT NOT NULL DEFAULT [] "
        )
    }
    }

    fun saveMigration(database: SupportSQLiteDatabase, SQL: String): Unit {
        try {
            database.execSQL(SQL)
        } catch (e: SQLiteException) {
            if (!e.toString().startsWith("android.database.sqlite.SQLiteException: duplicate column name:")) {
                Log.e("ERROR CRIME DATBASE" ,e.toString())
                throw e;
            }
        }

}