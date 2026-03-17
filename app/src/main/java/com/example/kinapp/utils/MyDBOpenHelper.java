package com.example.kinapp.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBOpenHelper extends SQLiteOpenHelper {

    public MyDBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建地图表
        db.execSQL("CREATE TABLE IF NOT EXISTS map (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
        // 创建道具表
        db.execSQL("CREATE TABLE IF NOT EXISTS prop (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, category TEXT, price INTEGER, image_path TEXT)");
        // 创建道具类别表
        db.execSQL("CREATE TABLE IF NOT EXISTS prop_category (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
        // 创建战术信息表
        db.execSQL("CREATE TABLE IF NOT EXISTS zhanshu_information (id INTEGER PRIMARY KEY AUTOINCREMENT, map_id INTEGER, type INTEGER, name TEXT, description TEXT, image_path TEXT, video_path TEXT, notes TEXT, member1 TEXT, member1_role TEXT, member2 TEXT, member2_role TEXT, member3 TEXT, member3_role TEXT, member4 TEXT, member4_role TEXT, member5 TEXT, member5_role TEXT)");

        // 插入初始地图数据
        db.execSQL("INSERT INTO map (name) VALUES ('dust2')");
        db.execSQL("INSERT INTO map (name) VALUES ('mirage')");
        db.execSQL("INSERT INTO map (name) VALUES ('inferno')");
        db.execSQL("INSERT INTO map (name) VALUES ('nuke')");
        db.execSQL("INSERT INTO map (name) VALUES ('overpass')");
        db.execSQL("INSERT INTO map (name) VALUES ('vertigo')");
        // 插入初始道具类别数据
        db.execSQL("INSERT INTO prop_category (name) VALUES ('投掷物')");
        db.execSQL("INSERT INTO prop_category (name) VALUES ('武器')");
        db.execSQL("INSERT INTO prop_category (name) VALUES ('装备')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 版本1到2的升级
            db.execSQL("ALTER TABLE prop ADD COLUMN image_path TEXT");
        }
        if (oldVersion < 3) {
            // 版本2到3的升级
            db.execSQL("CREATE TABLE IF NOT EXISTS prop_category (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
            db.execSQL("INSERT INTO prop_category (name) VALUES ('投掷物')");
            db.execSQL("INSERT INTO prop_category (name) VALUES ('武器')");
            db.execSQL("INSERT INTO prop_category (name) VALUES ('装备')");
            db.execSQL("ALTER TABLE prop ADD COLUMN category TEXT");
        }
        if (oldVersion < 4) {
            // 版本3到4的升级
            db.execSQL("CREATE TABLE IF NOT EXISTS zhanshu_information (id INTEGER PRIMARY KEY AUTOINCREMENT, map_id INTEGER, type INTEGER, name TEXT, description TEXT, image_path TEXT, video_path TEXT, notes TEXT)");
        }
        if (oldVersion < 5) {
            // 版本4到5的升级，添加成员信息字段
            db.execSQL("CREATE TABLE IF NOT EXISTS zhanshu_information_new (id INTEGER PRIMARY KEY AUTOINCREMENT, map_id INTEGER, type INTEGER, name TEXT, description TEXT, image_path TEXT, video_path TEXT, notes TEXT, member1 TEXT, member1_role TEXT, member2 TEXT, member2_role TEXT, member3 TEXT, member3_role TEXT, member4 TEXT, member4_role TEXT, member5 TEXT, member5_role TEXT)");
            db.execSQL("INSERT INTO zhanshu_information_new (id, map_id, type, name, description, image_path, video_path, notes) SELECT id, map_id, type, name, description, image_path, video_path, notes FROM zhanshu_information");
            db.execSQL("DROP TABLE IF EXISTS zhanshu_information");
            db.execSQL("ALTER TABLE zhanshu_information_new RENAME TO zhanshu_information");
        }
    }
}
