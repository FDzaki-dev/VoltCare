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
 * Skema DB v3 - VoltCare.
 * PENTING: Ini adalah Protected Asset. Setiap perubahan struktur tabel WAJIB menaikkan
 * [version] dan menyediakan Migration eksplisit (jangan gunakan fallbackToDestructiveMigration
 * di produksi setelah rilis publik pertama).
 *
 * v1->v2 (Batch 58): tambah kolom `alarmSoundUri` (TEXT, nullable) di `smart_rule` untuk
 * fitur Custom Alarm. ALTER TABLE ADD COLUMN, tidak destruktif.
 * v2->v3 (Batch 66): tambah kolom `alarmLoop` (INTEGER/Boolean, default 0/false) di `smart_rule`
 * untuk opsi "ulangi terus sampai dimatikan manual" (Pending Queue #27 baru). ALTER TABLE ADD
 * COLUMN, tidak destruktif - data aturan lama tetap utuh, kolom baru default false (perilaku
 * lama: main 1x sampai selesai, tidak berubah kecuali user aktifkan sendiri).
 */
@Database(
    entities = [BatteryLogEntity::class, CycleEntity::class, RuleEntity::class],
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE smart_rule ADD COLUMN alarmLoop INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voltcare_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
    }
}
