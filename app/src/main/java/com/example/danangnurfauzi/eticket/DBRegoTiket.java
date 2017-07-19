package com.example.danangnurfauzi.eticket;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by danangnurfauzi on 7/18/17.
 */

public class DBRegoTiket {
    private static final String DBName = "rego_db";
    private static final int DBVer = 1;

    public static final String TABLE_NAME = "rego_tiket";
    public static final String COL_ID = "_id";
    public static final String COL_WISNU = "wisnu";
    public static final String COL_WISMAN = "wisman";
    public static final String COL_KENDARAAN_YA = "kendaraanYa";
    public static final String COL_KENDARAAN_TIDAK = "kendaraanTidak";

    private static final String TAG = "regoDBRegoTiket";
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private static final String DBCreate = "create table rego_tiket (_id integer primary key, wisnu integer, wisman integer, kendaraanYa integer, kendaraanTidak integer);";

    private final Context context;

    private static class DatabaseHelper extends SQLiteOpenHelper{

        public DatabaseHelper(Context context){
            super(context,DBName, null, DBVer);
        }

        public void onCreate(SQLiteDatabase db){
            db.execSQL(DBCreate);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
            Log.d(TAG, "upgrade DB");
            db.execSQL("DROP TABLE IF EXIST "+ TABLE_NAME);
            onCreate(db);
        }
    }

    public DBRegoTiket(Context context){
        this.context = context;
    }

    public DBRegoTiket open() throws SQLException{
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return this;
    }

    public void close(){
        dbHelper.close();
    }

    public void createRegoTiket(RegoTiket regoTiket){
        ContentValues val = new ContentValues();
        val.put(COL_WISMAN, regoTiket.getWisatawanMancanegara());
        val.put(COL_WISNU, regoTiket.getWisatawanNusantara());
        val.put(COL_KENDARAAN_YA, regoTiket.getKendaraanYa());
        val.put(COL_KENDARAAN_TIDAK, regoTiket.getKendaraanTidak());
        db.insert(TABLE_NAME,null,val);
    }

    public boolean deleteRegoTiket(int id){
        return db.delete(TABLE_NAME, COL_ID + "=" + id, null) > 0;
    }

    public Cursor getAllRegoTiket(){
        return db.query(TABLE_NAME, new String[]{
                COL_ID, COL_WISNU, COL_WISMAN, COL_KENDARAAN_YA, COL_KENDARAAN_TIDAK
        },null,null,null,null,null);
    }

    public Cursor getSingleRegoTiket(int id){
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query(TABLE_NAME, new String[]{
                COL_ID, COL_WISNU, COL_WISMAN, COL_KENDARAAN_YA, COL_KENDARAAN_TIDAK
        }, COL_ID + "=" + id, null, null, null, null);

        if (cursor != null)
            cursor.moveToFirst();

        return cursor;

    }

    public boolean updateRegoTiket(RegoTiket regoTiket){
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();

        ContentValues val = new ContentValues();
        val.put(COL_WISMAN, regoTiket.getWisatawanMancanegara());
        val.put(COL_WISNU, regoTiket.getWisatawanNusantara());
        val.put(COL_KENDARAAN_YA, regoTiket.getKendaraanYa());
        val.put(COL_KENDARAAN_TIDAK, regoTiket.getKendaraanTidak());

        return db.update(TABLE_NAME, val, COL_ID + "=" + regoTiket.getId(), null) > 0;
    }

    public boolean ifExists()
    {
        dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        //Cursor cursor = null;
        String checkQuery = "SELECT _id FROM rego_tiket";
        Cursor cursor= db.rawQuery(checkQuery,null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }
}
