package com.example.kinapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class MapDAO {
    private MyDBOpenHelper dbHelper;

    public MapDAO(Context context) {
        dbHelper = new MyDBOpenHelper(context, "kinapp.db", null, 2);
    }

    /**
     * 根据道具信息ID获取相关道具
     * @param infoId 道具信息ID
     * @return 相关道具列表
     */
    public List<DaojuItem> getAllDaojuItemsByInfoId(int infoId) {
        List<DaojuItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("daoju", null, "info_id=?", new String[]{String.valueOf(infoId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int mapId = cursor.getInt(cursor.getColumnIndexOrThrow("map_id"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                String position = cursor.getString(cursor.getColumnIndexOrThrow("position"));
                list.add(new DaojuItem(id, mapId, type, position, infoId));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }


    /**
     * 向地图表中添加一条记录
     * @param name 地图名称
     * @param iconPosition 图标位置
     * @return 新插入记录的ID，失败返回-1
     */
    public long insertMap(String name, String iconPosition) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("icon_position", iconPosition);

        long id = db.insert("map", null, values);
        db.close();
        return id;
    }

    /**
     * 根据ID删除地图记录
     * @param id 地图ID
     * @return 删除的记录数
     */
    public int deleteMap(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("map", "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 更新地图信息
     * @param id 地图ID
     * @param name 新的地图名称
     * @param iconPosition 新的图标位置
     * @return 更新的记录数
     */
    public int updateMap(int id, String name, String iconPosition) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("icon_position", iconPosition);

        int result = db.update("map", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 查询所有地图记录
     * @return 地图记录列表
     */
    public List<MapItem> getAllMaps() {
        List<MapItem> mapList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("map", null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String iconPosition = cursor.getString(cursor.getColumnIndexOrThrow("icon_position"));
                mapList.add(new MapItem(id, name, iconPosition));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return mapList;
    }

    /**
     * 根据ID查询地图记录
     * @param id 地图ID
     * @return 地图项，未找到返回null
     */
    public MapItem getMapById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("map", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);

        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String iconPosition = cursor.getString(cursor.getColumnIndexOrThrow("icon_position"));
            cursor.close();
            db.close();
            return new MapItem(id, name, iconPosition);
        }

        cursor.close();
        db.close();
        return null;
    }

    /**
     * 根据名称查询地图记录
     * @param name 地图名称
     * @return 地图记录列表
     */
    public List<MapItem> getMapsByName(String name) {
        List<MapItem> mapList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("map", null, "name LIKE ?", new String[]{"%" + name + "%"}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String mapName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String iconPosition = cursor.getString(cursor.getColumnIndexOrThrow("icon_position"));
                mapList.add(new MapItem(id, mapName, iconPosition));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return mapList;
    }

    /**
     * 为指定地图添加道具记录
     * @param mapId 地图ID
     * @param type 道具类型
     * @param position 道具位置
     * @param infoId 道具信息ID
     * @return 新插入记录的ID，失败返回-1
     */
    public long insertDaoju(int mapId, int type, String position, int infoId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("map_id", mapId);
        values.put("type", type);
        values.put("position", position);
        values.put("info_id", infoId);

        long id = db.insert("daoju", null, values);
        db.close();
        return id;
    }

    /**
     * 根据地图ID获取相关道具
     * @param mapId 地图ID
     * @return 相关道具列表
     */
    public List<DaojuItem> getDaojuByMapId(int mapId) {
        List<DaojuItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("daoju", null, "map_id=?", new String[]{String.valueOf(mapId)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                String position = cursor.getString(cursor.getColumnIndexOrThrow("position"));
                int infoId = cursor.getInt(cursor.getColumnIndexOrThrow("info_id"));
                list.add(new DaojuItem(id, mapId, type, position, infoId));
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
        private String iconPosition;

        public MapItem(int id, String name, String iconPosition) {
            this.id = id;
            this.name = name;
            this.iconPosition = iconPosition;
        }

        // Getters
        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getIconPosition() {
            return iconPosition;
        }

        // Setters
        public void setName(String name) {
            this.name = name;
        }

        public void setIconPosition(String iconPosition) {
            this.iconPosition = iconPosition;
        }

        @Override
        public String toString() {
            return "MapItem{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", iconPosition='" + iconPosition + '\'' +
                    '}';
        }
    }

    /**
     * 道具项数据类
     */
    public static class DaojuItem {
        private int id;
        private int mapId;
        private int type;
        private String position;
        private int infoId;

        public DaojuItem(int id, int mapId, int type, String position, int infoId) {
            this.id = id;
            this.mapId = mapId;
            this.type = type;
            this.position = position;
            this.infoId = infoId;
        }

        // Getters
        public int getId() {
            return id;
        }

        public int getMapId() {
            return mapId;
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

        // Setters
        public void setId(int id) {
            this.id = id;
        }

        public void setMapId(int mapId) {
            this.mapId = mapId;
        }

        public void setType(int type) {
            this.type = type;
        }

        public void setPosition(String position) {
            this.position = position;
        }

        public void setInfoId(int infoId) {
            this.infoId = infoId;
        }

        @Override
        public String toString() {
            return "DaojuItem{" +
                    "id=" + id +
                    ", mapId=" + mapId +
                    ", type=" + type +
                    ", position='" + position + '\'' +
                    ", infoId=" + infoId +
                    '}';
        }
    }
}
