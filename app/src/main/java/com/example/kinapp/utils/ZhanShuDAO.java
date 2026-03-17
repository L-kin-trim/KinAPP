package com.example.kinapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ZhanShuDAO {
    private MyDBOpenHelper dbHelper;

    public ZhanShuDAO(Context context) {
        dbHelper = new MyDBOpenHelper(context, "kinapp.db", null, 5);
    }

    /**
     * 向地图表中添加一条记录
     */
    public long insertMap(String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);

        long id = db.insert("map", null, values);
        db.close();
        return id;
    }

    /**
     * 根据ID删除地图记录
     */
    public int deleteMap(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("map", "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 获取所有地图
     */
    public List<MapItem> getAllMaps() {
        List<MapItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("map", null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                list.add(new MapItem(id, name));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    /**
     * 向战术表中添加一条记录
     */
    public long insertZhanShu(int mapId, String name, int type, String position, int infoId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("map_id", mapId);
        values.put("name", name);
        values.put("type", type);
        values.put("position", position);
        values.put("info_id", infoId);

        long id = db.insert("zhanshu", null, values);
        db.close();
        return id;
    }

    /**
     * 根据ID删除战术记录
     */
    public int deleteZhanShu(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("zhanshu", "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 根据地图ID获取相关战术
     */
    public List<ZhanShuItem> getZhanShuByMapId(int mapId) {
        List<ZhanShuItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("zhanshu", null, "map_id=?", new String[]{String.valueOf(mapId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                String position = cursor.getString(cursor.getColumnIndexOrThrow("position"));
                int infoId = cursor.getInt(cursor.getColumnIndexOrThrow("info_id"));
                list.add(new ZhanShuItem(id, mapId, name, type, position, infoId));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    /**
     * 地图项数据类
     */
    public static class MapItem {
        private int id;
        private String name;

        public MapItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 战术项数据类
     */
    public static class ZhanShuItem {
        private int id;
        private int mapId;
        private String name;
        private int type;
        private String position;
        private int infoId;

        public ZhanShuItem(int id, int mapId, String name, int type, String position, int infoId) {
            this.id = id;
            this.mapId = mapId;
            this.name = name;
            this.type = type;
            this.position = position;
            this.infoId = infoId;
        }

        public int getId() {
            return id;
        }

        public int getMapId() {
            return mapId;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }

        public String getPosition() {
            return position;
        }

        public int getInfoId() {
            return infoId;
        }
    }
}
