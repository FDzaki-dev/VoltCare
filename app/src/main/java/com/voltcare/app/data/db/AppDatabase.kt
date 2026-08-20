package com.voltcare.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.voltcare.app.data.db.dao.BatteryLogDao
import com.voltcare.app.data.db.dao.CycleDao
import com.voltcare.app.data.db.dao.RuleDao
import com.voltcare.app.data.db.entity.BatteryLogEntity
import com.voltcare.app.data.db.entity.CycleEntity
import com.voltcare.app.data.db.entity.RuleEntity

/**
 * Skema DB v2 - VoltCare.
 * PENTING: Ini adalah Protected Asset. Setiap perubahan struktur tabel WAJIB menaikkan
 * [version] dan menyediakan Migration eksplisit (jangan gunakan fallbackToDestructiveMigration
 * di produksi setelah rilis publik pertama).
 *
 * v1->v2 (Batch 58): tambah kolom `alarmSoundUri` (TEXT, nullable) di `smart_rule` untuk
 * fitur Custom Alarm (Pending Queue #24: user pilih nada alarm sendiri per aturan, bukan cuma
 * nada default sistem). ALTER TABLE ADD COLUMN, tidak destruktif - data aturan lama tetap utuh,
 * kolom baru default NULL (berarti "pakai nada default").
 */
@Database(
    entities = [BatteryLogEntity::class, CycleEntity::class, RuleEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batteryLogDao(): BatteryLogDao
    abstract fun cycleDao(): CycleDao
    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE smart_rule ADD COLUMN alarmSoundUri TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voltcare_db"
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
