package com.github.eprendre.tingshu.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.github.eprendre.tingshu.utils.Book

@Database(entities = [Book::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                    // 官方版用的也是 book-db：它的库版本/表结构与本项目不同，
                    // 若沿用同名文件，Room 校验不过会抛 IllegalStateException 直接崩溃。
                    // 这里换一个独立文件名，和官方版的数据彻底隔离。
                    // 两个 fallback 都要留着：升级（旧版本<新版本）和降级（旧版本>新版本）都能自毁重建。
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build().also { instance = it }
            }
        }

        private const val DB_NAME = "book-db-zc"
    }
}
