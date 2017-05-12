package com.teefourteen.glideplayer.activities;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.fragments.FragmentSwitcher;
import com.teefourteen.glideplayer.fragments.player.VideoPlayerFragment;

public class VideoPlayerActivity extends AppCompatActivity  {
    private static final String VIDEO_FRAGMENT_TAG = "video_player_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        FragmentSwitcher fragmentSwitcher = new FragmentSwitcher(getSupportFragmentManager(),
                R.id.video_player_activity_main_container);

        fragmentSwitcher.switchTo(null, VIDEO_FRAGMENT_TAG, true);
        fragmentSwitcher.switchTo(new VideoPlayerFragment(), VIDEO_FRAGMENT_TAG, true);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }
}
