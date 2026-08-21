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
 * Skema DB v4 - VoltCare.
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
 * v3->v4 (Batch 73): tambah kolom `activeDays` (TEXT, default "1,2,3,4,5,6,7" - SEMUA hari,
 * format Calendar.DAY_OF_WEEK: 1=Minggu..7=Sabtu) di `smart_rule` untuk jadwal hari aktif rule
 * mirip Google Clock. ALTER TABLE ADD COLUMN, tidak destruktif - default "semua hari" = perilaku
 * lama tidak berubah sampai UI picker hari selesai diwiring (Pending Queue batch berikutnya).
 */
@Database(
    entities = [BatteryLogEntity::class, CycleEntity::class, RuleEntity::class],
    version = 4,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE smart_rule ADD COLUMN activeDays TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voltcare_db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { INSTANCE = it }
            }
    }
}
