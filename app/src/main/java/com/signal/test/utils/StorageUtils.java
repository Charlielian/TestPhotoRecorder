package com.signal.test.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Environment;

import com.signal.test.models.SignalData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StorageUtils {
    private static final String DATABASE_NAME = "signal_test.db";
    private static final int DATABASE_VERSION = 1;
    
    private static final String TABLE_SIGNAL = "signal_data";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_OPERATOR = "operator";
    private static final String COLUMN_CGI = "cgi";
    private static final String COLUMN_FREQUENCY = "frequency";
    private static final String COLUMN_BAND = "band";
    private static final String COLUMN_RSSI = "rssi";
    private static final String COLUMN_NETWORK_TYPE = "network_type";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_LOCATION = "location";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_PHOTO_PATH = "photo_path";
    
    // 创建表语句
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_SIGNAL + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_OPERATOR + " TEXT, " +
            COLUMN_CGI + " TEXT, " +
            COLUMN_FREQUENCY + " INTEGER, " +
            COLUMN_BAND + " TEXT, " +
            COLUMN_RSSI + " INTEGER, " +
            COLUMN_NETWORK_TYPE + " TEXT, " +
            COLUMN_LATITUDE + " REAL, " +
            COLUMN_LONGITUDE + " REAL, " +
            COLUMN_LOCATION + " TEXT, " +
            COLUMN_TIMESTAMP + " TEXT, " +
            COLUMN_PHOTO_PATH + " TEXT" +
            ");";
    
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private Context context;
    
    public StorageUtils(Context context) {
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
        this.database = dbHelper.getWritableDatabase();
    }
    
    // 内部数据库帮助类
    private static class DatabaseHelper extends SQLiteOpenHelper {
        
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SIGNAL);
            onCreate(db);
        }
    }
    
    // 插入信号数据
    public long insertSignalData(SignalData data) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_OPERATOR, data.getOperator());
        values.put(COLUMN_CGI, data.getCgi());
        values.put(COLUMN_FREQUENCY, data.getFrequency());
        values.put(COLUMN_BAND, data.getBand());
        values.put(COLUMN_RSSI, data.getRssi());
        values.put(COLUMN_NETWORK_TYPE, data.getNetworkType());
        values.put(COLUMN_LATITUDE, data.getLatitude());
        values.put(COLUMN_LONGITUDE, data.getLongitude());
        values.put(COLUMN_LOCATION, data.getLocation());
        values.put(COLUMN_TIMESTAMP, data.getTimestamp());
        values.put(COLUMN_PHOTO_PATH, data.getPhotoPath());
        
        return database.insert(TABLE_SIGNAL, null, values);
    }
    
    // 获取所有信号数据
    public List<SignalData> getAllSignalData() {
        List<SignalData> dataList = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT * FROM " + TABLE_SIGNAL + " ORDER BY " + COLUMN_TIMESTAMP + " DESC", null);
        
        if (cursor.moveToFirst()) {
            do {
                SignalData data = new SignalData();
                data.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                data.setOperator(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPERATOR)));
                data.setCgi(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CGI)));
                data.setFrequency(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FREQUENCY)));
                data.setBand(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BAND)));
                data.setRssi(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RSSI)));
                data.setNetworkType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NETWORK_TYPE)));
                data.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
                data.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
                data.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)));
                data.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP)));
                data.setPhotoPath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_PATH)));
                dataList.add(data);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return dataList;
    }
    
    // 获取照片存储目录 - 使用应用专属存储
    public File getPhotoDirectory() {
        // 使用应用专属外部存储目录，无需权限且在卸载时自动清理
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SignalTest");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }
    
    // 关闭数据库
    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}