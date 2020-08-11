package com.tellula.olaitanadetayo.smartcardsecurity;

/**
 * Created by Olaitan Adetayo on 18/06/2018.
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

public class DBHelper extends SQLiteOpenHelper  {
    public static final String DATABASE_NAME = "Security1004.db";
    public static final String SMARTCARDS_TABLE_NAME = "smartcards";
    public static final String SMARTCARDS_COLUMN_ID = "id";
    public static final String SMARTCARDS_COLUMN_NAME = "name";
    public static final String SMARTCARDS_COLUMN_CLUSTER = "cluster";
    public static final String SMARTCARDS_COLUMN_SERIAL = "serial";
    public static final String SMARTCARDS_COLUMN_MOBILE = "mobile_no";
    public static final String SMARTCARDS_COLUMN_EMAIL = "email";


    private HashMap hp;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table smartcards " +
                        "(id integer primary key autoincrement, name text,cluster text,email text,mobile_no text,holder_category text,serial varchar(50))"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS smartcards");
        onCreate(db);
    }
    public static boolean checkDataBase() {
        SQLiteDatabase checkDB = null;
        try {
            checkDB = SQLiteDatabase.openDatabase(DATABASE_NAME, null,
                    SQLiteDatabase.OPEN_READONLY);
            checkDB.close();
        } catch (SQLiteException e) {
            // database doesn't exist yet.
        }
        return checkDB != null;
    }
    public boolean insertCard (String name, String cluster,String email,String mobile_no,String holder_category, String serial) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("cluster", cluster);
        contentValues.put("email", email);
        contentValues.put("mobile_no", mobile_no);
        contentValues.put("holder_category", holder_category);
        contentValues.put("serial", serial);
        db.insert("smartcards", null, contentValues);
        return true;
    }

    public Cursor getData(String serial) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from smartcards where serial='"+serial+"'", null );
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, SMARTCARDS_TABLE_NAME);
        return numRows;
    }

    public boolean updateCard (Integer id, String name, String cluster, String serial) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("cluster", cluster);
        contentValues.put("serial", serial);
        db.update("smartcards", contentValues, "serial = ? ", new String[] { serial } );
        return true;
    }

    public Integer deleteCard (String serial) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("smartcards", "serial = ? ", new String[] { serial });
    }

    public ArrayList<String> getAllCards() {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from smartcards", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            array_list.add(res.getString(res.getColumnIndex(SMARTCARDS_COLUMN_NAME)));
            res.moveToNext();
        }
        return array_list;
    }
}
