package com.example.kinapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ZhanShuInformationDAO {
    private MyDBOpenHelper dbHelper;

    public ZhanShuInformationDAO(Context context) {
        dbHelper = new MyDBOpenHelper(context, "kinapp.db", null, 5);
    }

    /**
     * 向战术信息表中添加一条记录
     */
    public long insertZhanShuInformation(int mapId, int type, String name, String description, 
                                         String member1, String member1Role, 
                                         String member2, String member2Role, 
                                         String member3, String member3Role, 
                                         String member4, String member4Role, 
                                         String member5, String member5Role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("map_id", mapId);
        values.put("type", type);
        values.put("name", name);
        values.put("description", description);
        values.put("member1", member1);
        values.put("member1_role", member1Role);
        values.put("member2", member2);
        values.put("member2_role", member2Role);
        values.put("member3", member3);
        values.put("member3_role", member3Role);
        values.put("member4", member4);
        values.put("member4_role", member4Role);
        values.put("member5", member5);
        values.put("member5_role", member5Role);

        long id = db.insert("zhanshu_information", null, values);
        db.close();
        return id;
    }

    /**
     * 根据ID删除战术信息记录
     */
    public int deleteZhanShuInformation(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("zhanshu_information", "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 更新战术信息
     */
    public int updateZhanShuInformation(int id, String description, String imagePath, String videoPath, String notes) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("description", description);
        values.put("image_path", imagePath);
        values.put("video_path", videoPath);
        values.put("notes", notes);

        int result = db.update("zhanshu_information", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 更新战术信息（类型、名称、描述、成员信息）
     */
    public int updateZhanShuInformation(int id, int type, String name, String description, 
                                         String member1, String member1Role, 
                                         String member2, String member2Role, 
                                         String member3, String member3Role, 
                                         String member4, String member4Role, 
                                         String member5, String member5Role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("name", name);
        values.put("description", description);
        values.put("member1", member1);
        values.put("member1_role", member1Role);
        values.put("member2", member2);
        values.put("member2_role", member2Role);
        values.put("member3", member3);
        values.put("member3_role", member3Role);
        values.put("member4", member4);
        values.put("member4_role", member4Role);
        values.put("member5", member5);
        values.put("member5_role", member5Role);

        int result = db.update("zhanshu_information", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 根据ID获取战术信息
     */
    public ZhanShuInformation getZhanShuInformationById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("zhanshu_information", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);

        if (cursor.moveToFirst()) {
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            String imagePath = cursor.getString(cursor.getColumnIndexOrThrow("image_path"));
            String videoPath = cursor.getString(cursor.getColumnIndexOrThrow("video_path"));
            String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
            cursor.close();
            db.close();
            return new ZhanShuInformation(id, description, imagePath, videoPath, notes);
        }

        cursor.close();
        db.close();
        return null;
    }

    /**
     * 根据ID获取战术信息项
     */
    public ZhanShuInformationItem getZhanShuInformationItemById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("zhanshu_information", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);

        if (cursor.moveToFirst()) {
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            cursor.close();
            db.close();
            return new ZhanShuInformationItem(id, name, description);
        }

        cursor.close();
        db.close();
        return null;
    }

    /**
     * 根据ID获取完整的战术信息（包含map_id和type）
     */
    public ZhanShuInformationWithMapAndType getZhanShuInformationWithMapAndTypeById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("zhanshu_information", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);

        if (cursor.moveToFirst()) {
            int mapId = cursor.getInt(cursor.getColumnIndexOrThrow("map_id"));
            int type = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            
            // 处理成员信息字段，避免字段不存在时抛出异常
            String member1 = null;
            String member1Role = null;
            String member2 = null;
            String member2Role = null;
            String member3 = null;
            String member3Role = null;
            String member4 = null;
            String member4Role = null;
            String member5 = null;
            String member5Role = null;
            
            if (cursor.getColumnIndex("member1") != -1) {
                member1 = cursor.getString(cursor.getColumnIndex("member1"));
            }
            if (cursor.getColumnIndex("member1_role") != -1) {
                member1Role = cursor.getString(cursor.getColumnIndex("member1_role"));
            }
            if (cursor.getColumnIndex("member2") != -1) {
                member2 = cursor.getString(cursor.getColumnIndex("member2"));
            }
            if (cursor.getColumnIndex("member2_role") != -1) {
                member2Role = cursor.getString(cursor.getColumnIndex("member2_role"));
            }
            if (cursor.getColumnIndex("member3") != -1) {
                member3 = cursor.getString(cursor.getColumnIndex("member3"));
            }
            if (cursor.getColumnIndex("member3_role") != -1) {
                member3Role = cursor.getString(cursor.getColumnIndex("member3_role"));
            }
            if (cursor.getColumnIndex("member4") != -1) {
                member4 = cursor.getString(cursor.getColumnIndex("member4"));
            }
            if (cursor.getColumnIndex("member4_role") != -1) {
                member4Role = cursor.getString(cursor.getColumnIndex("member4_role"));
            }
            if (cursor.getColumnIndex("member5") != -1) {
                member5 = cursor.getString(cursor.getColumnIndex("member5"));
            }
            if (cursor.getColumnIndex("member5_role") != -1) {
                member5Role = cursor.getString(cursor.getColumnIndex("member5_role"));
            }
            
            cursor.close();
            db.close();
            return new ZhanShuInformationWithMapAndType(id, mapId, type, name, description, 
                                                    member1, member1Role, 
                                                    member2, member2Role, 
                                                    member3, member3Role, 
                                                    member4, member4Role, 
                                                    member5, member5Role);
        }

        cursor.close();
        db.close();
        return null;
    }

    /**
     * 战术信息数据类（包含map_id和type）
     */
    public static class ZhanShuInformationWithMapAndType {
        private int id;
        private int mapId;
        private int type;
        private String name;
        private String description;
        private String member1;
        private String member1Role;
        private String member2;
        private String member2Role;
        private String member3;
        private String member3Role;
        private String member4;
        private String member4Role;
        private String member5;
        private String member5Role;

        public ZhanShuInformationWithMapAndType(int id, int mapId, int type, String name, String description, 
                                              String member1, String member1Role, 
                                              String member2, String member2Role, 
                                              String member3, String member3Role, 
                                              String member4, String member4Role, 
                                              String member5, String member5Role) {
            this.id = id;
            this.mapId = mapId;
            this.type = type;
            this.name = name;
            this.description = description;
            this.member1 = member1;
            this.member1Role = member1Role;
            this.member2 = member2;
            this.member2Role = member2Role;
            this.member3 = member3;
            this.member3Role = member3Role;
            this.member4 = member4;
            this.member4Role = member4Role;
            this.member5 = member5;
            this.member5Role = member5Role;
        }

        public int getId() {
            return id;
        }

        public int getMapId() {
            return mapId;
        }

        public int getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getMember1() {
            return member1;
        }

        public String getMember1Role() {
            return member1Role;
        }

        public String getMember2() {
            return member2;
        }

        public String getMember2Role() {
            return member2Role;
        }

        public String getMember3() {
            return member3;
        }

        public String getMember3Role() {
            return member3Role;
        }

        public String getMember4() {
            return member4;
        }

        public String getMember4Role() {
            return member4Role;
        }

        public String getMember5() {
            return member5;
        }

        public String getMember5Role() {
            return member5Role;
        }
    }

    /**
     * 根据地图ID和类型获取战术信息列表
     */
    public List<ZhanShuInformationItem> getZhanShuInformationByMapIdAndType(int mapId, int type) {
        List<ZhanShuInformationItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("zhanshu_information", null, "map_id=? AND type=?", new String[]{String.valueOf(mapId), String.valueOf(type)}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
                list.add(new ZhanShuInformationItem(id, name, description));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    /**
     * 战术信息数据类
     */
    public static class ZhanShuInformation {
        private int id;
        private String description;
        private String imagePath;
        private String videoPath;
        private String notes;

        public ZhanShuInformation(int id, String description, String imagePath, String videoPath, String notes) {
            this.id = id;
            this.description = description;
            this.imagePath = imagePath;
            this.videoPath = videoPath;
            this.notes = notes;
        }

        public int getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getVideoPath() {
            return videoPath;
        }

        public String getNotes() {
            return notes;
        }
    }

    /**
     * 战术信息项数据类
     */
    public static class ZhanShuInformationItem {
        private int id;
        private String name;
        private String description;

        public ZhanShuInformationItem(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }
    }
}
