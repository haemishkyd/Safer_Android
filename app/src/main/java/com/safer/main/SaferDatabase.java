package com.safer.main;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by haemish on 2016/07/15.
 */
public class SaferDatabase extends SQLiteOpenHelper
{
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INT";
    private static final String COMMA_SEP = ",";

    //Queries
    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + UserDetail.TABLE_NAME_USER_DETAIL + " (" +
                    UserDetail._ID + " INTEGER PRIMARY KEY," +
                    UserDetail.COLUMN_NAME_USER_NAME    + TEXT_TYPE + COMMA_SEP +
                    UserDetail.COLUMN_NAME_REAL_NAME    + TEXT_TYPE + COMMA_SEP +
                    UserDetail.COLUMN_NAME_REAL_SURNAME + TEXT_TYPE + COMMA_SEP +
                    UserDetail.COLUMN_NAME_GLOB_USER_ID + INT_TYPE  + COMMA_SEP +
                    UserDetail.COLUMN_NAME_CURRENT_ROLE + INT_TYPE  + COMMA_SEP +
                    UserDetail.COLUMN_NAME_PASSWORD     + TEXT_TYPE  + " )";

    private static final String SQL_DELETE_TABLE =
            "DROP TABLE IF EXISTS " + UserDetail.TABLE_NAME_USER_DETAIL;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "SaferDatabase.db";

    public SaferDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static abstract class UserDetail implements BaseColumns
    {
        public static final String TABLE_NAME_USER_DETAIL   = "userinfo";
        public static final String COLUMN_NAME_USER_NAME    = "username";
        public static final String COLUMN_NAME_REAL_NAME    = "realname";
        public static final String COLUMN_NAME_REAL_SURNAME = "realsurname";
        public static final String COLUMN_NAME_PASSWORD     = "password";
        public static final String COLUMN_NAME_GLOB_USER_ID = "global_user_id";
        public static final String COLUMN_NAME_CURRENT_ROLE = "current_role";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addUserDetail(String username,String password)
    {
        SQLiteDatabase db=this.getWritableDatabase();
        if (!isThereAUser())
        {
            removeUserData();
        }
        ContentValues values = new ContentValues();
        values.put(UserDetail.COLUMN_NAME_USER_NAME,username);
        values.put(UserDetail.COLUMN_NAME_PASSWORD,password);
        db.insert(UserDetail.TABLE_NAME_USER_DETAIL,null,values);
        db.close();
    }

    public void removeUserData()
    {
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL("delete from "+ UserDetail.TABLE_NAME_USER_DETAIL);
    }

    public void updateUserDetail(String realname,String realsurname,Integer currentrole,Integer globalid,String username )
    {
        SQLiteDatabase db = this.getWritableDatabase();
        String strSQL = "UPDATE "+UserDetail.TABLE_NAME_USER_DETAIL+" SET ";
        strSQL += UserDetail.COLUMN_NAME_REAL_NAME+"='"+realname+"',";
        strSQL += UserDetail.COLUMN_NAME_REAL_SURNAME+"='"+realsurname+"',";
        strSQL += UserDetail.COLUMN_NAME_CURRENT_ROLE+"="+currentrole+",";
        strSQL += UserDetail.COLUMN_NAME_GLOB_USER_ID+"="+globalid;
        strSQL += " WHERE "+UserDetail.COLUMN_NAME_USER_NAME+"='"+username+"';";
        db.execSQL(strSQL);
        db.close();
    }

    public void getCurrentLoggedInUserDetails(Operator passedOperator)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        String strSQL = "SELECT * from userinfo;";
        Cursor cursor = db.rawQuery(strSQL, null);
        if (cursor.moveToFirst())
        {
            passedOperator.Username = cursor.getString(1);
            passedOperator.RealName = cursor.getString(2);
            passedOperator.RealSurname = cursor.getString(3);
            passedOperator.OperatorId = Integer.valueOf(cursor.getString(4));
            passedOperator.CurrentRole = Integer.valueOf(cursor.getString(5));
            passedOperator.Password = cursor.getString(6);
        }
    }

    public boolean isThereAUser()
    {
        boolean return_value = false;
        SQLiteDatabase db=this.getReadableDatabase();
        String selectQuery = "SELECT " + UserDetail.COLUMN_NAME_GLOB_USER_ID + " FROM " + UserDetail.TABLE_NAME_USER_DETAIL;
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst())
        {
            do
            {
                if (cursor.getString(0) != null)
                {
                    return_value = true;
                }
            } while ((cursor.moveToNext()));
        }
        return return_value;
    }
}
