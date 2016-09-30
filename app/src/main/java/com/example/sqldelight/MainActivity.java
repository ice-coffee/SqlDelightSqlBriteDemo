package com.example.sqldelight;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.sqldelight.database.dao.HockeyPlayerDao;
import com.example.sqldelight.database.dao.HockeyPlayerDaoImpl;
import com.example.sqldelight.database.model.HockeyPlayer;

import java.util.List;
import java.util.Random;

/**
 * Created by mzp on 2016/9/28.
 */
public class MainActivity extends Activity
{
    private HockeyPlayerDao hockeyPlayerDao;
    private StringBuffer sb = new StringBuffer();

    private Random random = new Random();

    private TextView tvShow;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvShow = (TextView) findViewById(R.id.tv_show);
        hockeyPlayerDao = new HockeyPlayerDaoImpl(this);
    }

    public void insertTable(View view)
    {
        long currentLong = random.nextLong();
        hockeyPlayerDao.insert(currentLong, "Alec");
    }

    public void queryTable(View view)
    {
        sb = new StringBuffer();
        List<HockeyPlayer> hockeyPlayers = hockeyPlayerDao.getList();
        for (HockeyPlayer hockeyPlayer : hockeyPlayers)
        {
            sb.append(hockeyPlayer.name());
        }

        tvShow.setText(sb.toString());
    }
}
