package com.example.kinapp.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class DaojuInformationDAO {
    private MyDBOpenHelper dbHelper;

    public DaojuInformationDAO(Context context) {
        dbHelper = new MyDBOpenHelper(context, "kinapp.db", null, 2);
    }

    /**
     * 插入道具信息
     * @param throwingMethod 投掷方式
     * @param stanceImagePath 站位图片路径
     * @param aimPointImagePath 瞄点图片路径
     * @param landingPointImagePath 落点图片路径
     * @param toolName 道具名称
     * @return 新插入记录的ID，失败返回-1
     */
    public long insertDaojuInformation(String throwingMethod, String stanceImagePath,
                                       String aimPointImagePath, String landingPointImagePath,
                                       String toolName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("throwing_method", throwingMethod);
        values.put("stance_image", stanceImagePath);
        values.put("aim_point_image", aimPointImagePath);
        values.put("landing_point_image", landingPointImagePath);
        values.put("tool_name", toolName);

        long id = db.insert("daojuinformation", null, values);
        db.close();
        return id;
    }

    /**
     * 根据ID删除道具信息
     * @param id 道具信息ID
     * @return 删除的记录数
     */
    public int deleteDaojuInformation(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int result = db.delete("daojuinformation", "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 更新道具信息
     * @param id 道具信息ID
     * @param throwingMethod 投掷方式
     * @param stanceImagePath 站位图片路径
     * @param aimPointImagePath 瞄点图片路径
     * @param landingPointImagePath 落点图片路径
     * @param toolName 道具名称
     * @return 更新的记录数
     */
    public int updateDaojuInformation(int id, String throwingMethod, String stanceImagePath,
                                      String aimPointImagePath, String landingPointImagePath,
                                      String toolName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("throwing_method", throwingMethod);
        values.put("stance_image", stanceImagePath);
        values.put("aim_point_image", aimPointImagePath);
        values.put("landing_point_image", landingPointImagePath);
        values.put("tool_name", toolName);

        int result = db.update("daojuinformation", values, "id=?", new String[]{String.valueOf(id)});
        db.close();
        return result;
    }

    /**
     * 查询所有道具信息
     * @return 道具信息列表
     */
    public List<DaojuInformation> getAllDaojuInformation() {
        List<DaojuInformation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("daojuinformation", null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String throwingMethod = cursor.getString(cursor.getColumnIndexOrThrow("throwing_method"));
                String stanceImagePath = cursor.getString(cursor.getColumnIndexOrThrow("stance_image"));
                String aimPointImagePath = cursor.getString(cursor.getColumnIndexOrThrow("aim_point_image"));
                String landingPointImagePath = cursor.getString(cursor.getColumnIndexOrThrow("landing_point_image"));
                String toolName = cursor.getString(cursor.getColumnIndexOrThrow("tool_name"));
                list.add(new DaojuInformation(id, throwingMethod, stanceImagePath, aimPointImagePath, landingPointImagePath, toolName));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    /**
     * 根据ID查询道具信息
     * @param id 道具信息ID
     * @return 道具信息项，未找到返回null
     */
    public DaojuInformation getDaojuInformationById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("daojuinformation", null, "id=?", new String[]{String.valueOf(id)}, null, null, null);

        if (cursor.moveToFirst()) {
            String throwingMethod = cursor.getString(cursor.getColumnIndexOrThrow("throwing_method"));
            String stanceImagePath = cursor.getString(cursor.getColumnIndexOrThrow("stance_image"));
            String aimPointImagePath = cursor.getString(cursor.getColumnIndexOrThrow("aim_point_image"));
            String landingPointImagePath = cursor.getString(cursor.getColumnIndexOrThrow("landing_point_image"));
            String toolName = cursor.getString(cursor.getColumnIndexOrThrow("tool_name"));
            cursor.close();
            db.close();
            return new DaojuInformation(id, throwingMethod, stanceImagePath, aimPointImagePath, landingPointImagePath, toolName);
        }

        cursor.close();
        db.close();
        return null;
    }

    /**
     * 根据道具名称查询道具信息
     * @param toolName 道具名称
     * @return 道具信息列表
     */
    public List<DaojuInformation> getDaojuInformationByName(String toolName) {
        List<DaojuInformation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("daojuinformation", null, "tool_name LIKE ?", new String[]{"%" + toolName + "%"}, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String throwingMethod = cursor.getString(cursor.getColumnIndexOrThrow("throwing_method"));
                String stanceImagePath = cursor.getString(cursor.getColumnIndexOrThrow("stance_image"));
                String aimPointImagePath = cursor.getString(cursor.getColumnIndexOrThrow("aim_point_image"));
                String landingPointImagePath = cursor.getString(cursor.getColumnIndexOrThrow("landing_point_image"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("tool_name"));
                list.add(new DaojuInformation(id, throwingMethod, stanceImagePath, aimPointImagePath, landingPointImagePath, name));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    /**
     * 根据地图ID获取相关道具信息
     * @param mapId 地图ID
     * @return 相关道具信息列表
     */
    public List<DaojuInformation> getDaojuInformationByMapId(int mapId) {
        List<DaojuInformation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // 修改查询逻辑，正确关联daoju表和daojuinformation表
        String sql = "SELECT di.* FROM daojuinformation di " +
                "JOIN daoju d ON di.id = d.info_id " +
                "WHERE d.map_id = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(mapId)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String throwingMethod = cursor.getString(cursor.getColumnIndexOrThrow("throwing_method"));
                String stanceImagePath = cursor.getString(cursor.getColumnIndexOrThrow("stance_image"));
                String aimPointImagePath = cursor.getString(cursor.getColumnIndexOrThrow("aim_point_image"));
                String landingPointImagePath = cursor.getString(cursor.getColumnIndexOrThrow("landing_point_image"));
                String toolName = cursor.getString(cursor.getColumnIndexOrThrow("tool_name"));
                list.add(new DaojuInformation(id, throwingMethod, stanceImagePath, aimPointImagePath, landingPointImagePath, toolName));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }

    /**
     * 道具信息数据类
     */
    public static class DaojuInformation {
        private int id;
        private String throwingMethod;
        private String stanceImagePath;
        private String aimPointImagePath;
        private String landingPointImagePath;
        private String toolName;

        public DaojuInformation(int id, String throwingMethod, String stanceImagePath,
                                String aimPointImagePath, String landingPointImagePath, String toolName) {
            this.id = id;
            this.throwingMethod = throwingMethod;
            this.stanceImagePath = stanceImagePath;
            this.aimPointImagePath = aimPointImagePath;
            this.landingPointImagePath = landingPointImagePath;
            this.toolName = toolName;
        }

        // Getters
        public int getId() {
            return id;
        }

        public String getThrowingMethod() {
            return throwingMethod;
        }

        public String getStanceImagePath() {
            return stanceImagePath;
        }

        public String getAimPointImagePath() {
            return aimPointImagePath;
        }

        public String getLandingPointImagePath() {
            return landingPointImagePath;
        }

        public String getToolName() {
            return toolName;
        }

        // Setters
        public void setThrowingMethod(String throwingMethod) {
            this.throwingMethod = throwingMethod;
        }

        public void setStanceImagePath(String stanceImagePath) {
            this.stanceImagePath = stanceImagePath;
        }

        public void setAimPointImagePath(String aimPointImagePath) {
            this.aimPointImagePath = aimPointImagePath;
        }

        public void setLandingPointImagePath(String landingPointImagePath) {
            this.landingPointImagePath = landingPointImagePath;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        @Override
        public String toString() {
            return "DaojuInformation{" +
                    "id=" + id +
                    ", throwingMethod='" + throwingMethod + '\'' +
                    ", stanceImagePath='" + stanceImagePath + '\'' +
                    ", aimPointImagePath='" + aimPointImagePath + '\'' +
                    ", landingPointImagePath='" + landingPointImagePath + '\'' +
                    ", toolName='" + toolName + '\'' +
                    '}';
        }
    }
}
