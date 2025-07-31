package com.example.kinapp.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MyDBOpenHelper extends SQLiteOpenHelper {
    public MyDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, "kinapp.db", null, 2); // 将版本号从1更新为2
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE map (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, icon_position TEXT)");
        //地图表 地图id，地图名称，图标位置
        db.execSQL("CREATE TABLE daoju (id INTEGER PRIMARY KEY AUTOINCREMENT, map_id INTEGER, type INTEGER, position TEXT, info_id INTEGER)");
        //道具表 道具id，地图id，道具类型，道具位置，道具信息id
        db.execSQL("CREATE TABLE daojuinformation (id INTEGER PRIMARY KEY AUTOINCREMENT, throwing_method TEXT, stance_image TEXT, aim_point_image TEXT, landing_point_image TEXT, tool_name TEXT)");
        //道具信息表 道具信息id，投掷方式，站位图片，瞄点图片，落点图片，道具名称
        Log.e("SQLite数据库", "数据库创建成功");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE daojuinformation (id INTEGER PRIMARY KEY AUTOINCREMENT, throwing_method TEXT, stance_image TEXT, aim_point_image TEXT, landing_point_image TEXT, tool_name TEXT)");
            //道具信息表 道具信息id，投掷方式，站位图片，瞄点图片，落点图片，道具名称
        }
    }
}
