package com.teefourteen.glideplayer.activities;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.VideoView;

import com.teefourteen.glideplayer.Player;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.database.Library;
import com.teefourteen.glideplayer.fragments.FragmentSwitcher;
import com.teefourteen.glideplayer.fragments.player.VideoPlayerFragment;
import com.teefourteen.glideplayer.music.MusicPlayer;
import com.teefourteen.glideplayer.video.Video;
import com.teefourteen.glideplayer.video.VideoPlayer;

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
