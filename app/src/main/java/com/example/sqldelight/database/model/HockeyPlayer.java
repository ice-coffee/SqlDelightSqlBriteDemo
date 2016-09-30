package com.example.sqldelight.database.model;

import com.example.sqldelight.HockeyPlayerModel;
import com.google.auto.value.AutoValue;
import com.squareup.sqldelight.RowMapper;

/**
 * Created by mzp on 2016/9/28.
 */

@AutoValue
public abstract class HockeyPlayer implements HockeyPlayerModel
{
    public static final Factory<HockeyPlayer> FACTORY = new Factory<>(new Creator<HockeyPlayer>() {
        @Override public HockeyPlayer create(long _id, long number, String name) {
            return new AutoValue_HockeyPlayer(_id, number, name);
        }
    });

    public static final RowMapper<HockeyPlayer> MAPPER = FACTORY.select_by_nameMapper();
}
