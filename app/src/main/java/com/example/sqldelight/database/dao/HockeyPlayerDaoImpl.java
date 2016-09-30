package com.example.sqldelight.database.dao;

import android.content.Context;
import android.database.Cursor;

import com.example.sqldelight.database.HockeyPlayerOpenHelper;
import com.example.sqldelight.database.model.HockeyPlayer;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;
import com.squareup.sqlbrite.SqlBrite.Query;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by mzp on 2016/9/28.
 */
public class HockeyPlayerDaoImpl implements HockeyPlayerDao
{
    private BriteDatabase mbriteDatabase;
    private SqlBrite sqlBrite;

    public HockeyPlayerDaoImpl(Context context)
    {
        sqlBrite = SqlBrite.create();
        mbriteDatabase = sqlBrite.wrapDatabaseHelper(new HockeyPlayerOpenHelper(context) , Schedulers.io());
    }

    @Override
    public void insert(long number, String name)
    {
        mbriteDatabase.insert(HockeyPlayer.TABLE_NAME, HockeyPlayer.FACTORY.marshal()
                .number(number)
                .name(name)
                .asContentValues());

    }

    @Override
    public List<HockeyPlayer> getList()
    {
        final List<HockeyPlayer> list = new ArrayList<>();

        Observable<Query> hockeyPlayer = mbriteDatabase.createQuery(HockeyPlayer.TABLE_NAME, HockeyPlayer.SELECT_BY_NAME);

        hockeyPlayer.subscribe(new Action1<Query>()
        {
            @Override
            public void call(Query query)
            {
                Cursor cursor = query.run();
                while (cursor.moveToNext())
                {
                    list.add(HockeyPlayer.MAPPER.map(cursor));
                }
            }
        });

        return list;
    }
}
