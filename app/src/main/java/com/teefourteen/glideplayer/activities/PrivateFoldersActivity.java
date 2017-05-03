package com.teefourteen.glideplayer.activities;

import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.teefourteen.glideplayer.R;

public class PrivateFoldersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_folders);

        Toolbar toolbar = (Toolbar) findViewById(R.id.private_folders_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar =  getSupportActionBar();

        if(actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle("Set Private Folders");
            toolbar.setTitleTextColor(Color.WHITE);
        }
    }
}
