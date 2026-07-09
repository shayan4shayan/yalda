package com.shdarv.yalda.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase


fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
  return Room.databaseBuilder(
      context,
        AppDatabase::class.java, "yalda.db"
  )
}
