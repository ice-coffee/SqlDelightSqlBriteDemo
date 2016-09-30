package com.example.sqldelight.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.sqldelight.database.model.HockeyPlayer;

/**
 * Created by mzp on 2016/9/28.
 */
public class HockeyPlayerOpenHelper extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 1;

    public HockeyPlayerOpenHelper(Context context)
    {
        super(context, null, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(HockeyPlayer.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        switch (oldVersion)
        {
            case 1:
        }
    }
}
