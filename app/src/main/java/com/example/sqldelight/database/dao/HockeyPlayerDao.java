package com.example.sqldelight.database.dao;

import android.database.sqlite.SQLiteDatabase;

import com.example.sqldelight.database.model.HockeyPlayer;

import java.util.List;

/**
 * Created by mzp on 2016/9/28.
 */
public interface HockeyPlayerDao
{
    void insert(long number, String name);

    List<HockeyPlayer> getList();
}
