package com.powervault.health.pro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.powervault.health.pro.data.db.dao.BatteryLogDao
import com.powervault.health.pro.data.db.dao.CycleDao
import com.powervault.health.pro.data.db.dao.RuleDao
import com.powervault.health.pro.data.db.entity.BatteryLogEntity
import com.powervault.health.pro.data.db.entity.CycleEntity
import com.powervault.health.pro.data.db.entity.RuleEntity

/**
 * Skema DB v1 - PowerVault Health Pro.
 * PENTING: Ini adalah Protected Asset. Setiap perubahan struktur tabel WAJIB menaikkan
 * [version] dan menyediakan Migration eksplisit (jangan gunakan fallbackToDestructiveMigration
 * di produksi setelah rilis publik pertama).
 */
@Database(
    entities = [BatteryLogEntity::class, CycleEntity::class, RuleEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batteryLogDao(): BatteryLogDao
    abstract fun cycleDao(): CycleDao
    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "powervault_db"
                ).build().also { INSTANCE = it }
            }
    }
}
