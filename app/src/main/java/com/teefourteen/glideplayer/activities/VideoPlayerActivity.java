package com.teefourteen.glideplayer.activities;

import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
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
import com.teefourteen.glideplayer.music.MusicPlayer;
import com.teefourteen.glideplayer.video.Video;
import com.teefourteen.glideplayer.video.VideoPlayer;

public class VideoPlayerActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        VideoPlayer.VideoSizeChangedListener, Player.SeekListener {
    public static final String EXTRA_VIDEO_ID = "video_id";
    public static final String EXTRA_VIDEO_USERNAME = "video_username";

    private VideoPlayer videoPlayer;
    private SurfaceHolder surfaceHolder;
    private SeekBar seekBar;
    private boolean userSeeking = false;
    private Video currentPlaying = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoPlayer = new VideoPlayer(this, this);
        videoPlayer.registerSeekListener(this);

        seekBar = (SeekBar) findViewById(R.id.player_track_seek);
        seekBar.setMax(MusicPlayer.MAX_SEEK_VALUE);

        findViewById(R.id.player_play_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {play(v);
            }
        });
        findViewById(R.id.player_next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {next(v);
            }
        });
        findViewById(R.id.player_prev_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {prev(v);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int newSeek = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    newSeek = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                videoPlayer.seek(newSeek);
            }
        });

        SurfaceView videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
        SurfaceHolder videoSurfaceHolder = videoSurface.getHolder();
        videoSurfaceHolder.addCallback(this);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        Intent intent = getIntent();
        if(currentPlaying == null && intent.hasExtra(EXTRA_VIDEO_ID)) {
            long videoId = intent.getLongExtra(EXTRA_VIDEO_ID, 0);
            String username = intent.getStringExtra(EXTRA_VIDEO_USERNAME);
            Cursor cursor = Library.getVideo(username, videoId);
            if(cursor.moveToFirst()) {
                currentPlaying = Video.toVideo(cursor);
                cursor.close();
                if(videoPlayer.playMedia(currentPlaying, holder)) {
                    showPause();
                }
            } else {
                cursor.close();
            }
        }
    }

    private void showPlay() {
        ImageView playButton = (ImageView) findViewById(R.id.player_play_button);
        playButton.setImageResource(R.drawable.glideplayer_play_white);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });
    }

    private void showPause() {
        ImageView playButton = (ImageView) findViewById(R.id.player_play_button);
        playButton.setImageResource(R.drawable.glideplayer_pause_white);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause(v);
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer.close();
    }

    @Override
    public void videoSizeChanged(int width, int height) {
        SurfaceView videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
        ViewGroup.LayoutParams params =
                videoSurface.getLayoutParams();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if(rotation == Surface.ROTATION_0||rotation == Surface.ROTATION_180) {
            params.width = displayMetrics.widthPixels;
            params.height = (int) (((double)height/width) * params.width);
        } else {
            params.height = displayMetrics.heightPixels;
            params.width = (int) ((double)width/height) * params.height;
        }
        videoSurface.getHolder().setFixedSize(params.width, params.height);
    }

    public void play(View view) {
        videoPlayer.playMedia(currentPlaying, surfaceHolder);
        showPause();
    }

    public void pause(View view) {
        videoPlayer.pause();
        showPlay();
    }

    public void next(View view) {
        videoPlayer.trueSeek(videoPlayer.getTrueSeek() + 5000);
    }

    public void prev(View view) {
        videoPlayer.trueSeek(videoPlayer.getTrueSeek() - 5000);
    }

    @Override
    public void onSeekUpdated(int newSeek) {
        if(!userSeeking) {
            seekBar.setProgress(newSeek);
        }
    }

    @Override
    public void onBufferingUpdated(int percent) {
        seekBar.setSecondaryProgress(percent);
    }
}
