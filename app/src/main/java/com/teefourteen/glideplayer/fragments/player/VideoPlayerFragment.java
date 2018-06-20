/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.fragments.player;


import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;

import com.teefourteen.glideplayer.Player;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.Synchronization;
import com.teefourteen.glideplayer.database.Library;
import com.teefourteen.glideplayer.music.MusicPlayer;
import com.teefourteen.glideplayer.video.Video;
import com.teefourteen.glideplayer.video.VideoPlayer;

import java.io.IOException;

public class VideoPlayerFragment extends Fragment implements SurfaceHolder.Callback,
        VideoPlayer.VideoSizeChangedListener, Player.SeekListener, MediaPlayer.OnCompletionListener, Synchronization.VideoSession.EventListener {
    public static final String EXTRA_VIDEO_ID = "video_id";
    public static final String EXTRA_VIDEO_USERNAME = "video_username";

    private VideoPlayer videoPlayer;
    private SurfaceHolder surfaceHolder;
    private SeekBar seekBar;
    private boolean userSeeking = false;
    private Video currentPlaying = null;
    private boolean syncMode = false;
    private View rootView;

    public VideoPlayerFragment() {
        // Required empty public constructor
    }

    public static VideoPlayerFragment newInstance(Synchronization.VideoSession videoSession) {
        VideoPlayerFragment fragment = new VideoPlayerFragment();
        videoSession.setEventListener(fragment);
        fragment.syncMode = true;
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        rootView = inflater.inflate(R.layout.fragment_video_player, container, false);

        videoPlayer = new VideoPlayer(getActivity(), this);
        videoPlayer.registerSeekListener(this);
        videoPlayer.registerOnCompletionListener(this);

        seekBar = (SeekBar) rootView.findViewById(R.id.player_track_seek);
        seekBar.setMax(MusicPlayer.MAX_SEEK_VALUE);

        rootView.findViewById(R.id.player_play_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {play(v);
            }
        });
        rootView.findViewById(R.id.player_next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {next(v);
            }
        });
        rootView.findViewById(R.id.player_prev_button).setOnClickListener(new View.OnClickListener() {
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

        SurfaceView videoSurface = (SurfaceView) rootView.findViewById(R.id.videoSurface);
        SurfaceHolder videoSurfaceHolder = videoSurface.getHolder();
        videoSurfaceHolder.addCallback(this);
        
        return rootView;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceHolder = holder;
        Intent intent = getActivity().getIntent();
        if(currentPlaying == null && intent.hasExtra(EXTRA_VIDEO_ID)) {
            long videoId = intent.getLongExtra(EXTRA_VIDEO_ID, 0);
            intent.removeExtra(EXTRA_VIDEO_ID);
            String username = intent.getStringExtra(EXTRA_VIDEO_USERNAME);
            intent.removeExtra(EXTRA_VIDEO_USERNAME);
            Cursor cursor = Library.getVideo(username, videoId);
            if(cursor.moveToFirst()) {
                currentPlaying = Video.toVideo(cursor);
                cursor.close();
                if(syncMode) {
                    ((Synchronization.VideoSession) Synchronization.getInstance().getActiveSession())
                            .play(currentPlaying);
                }
                else if(videoPlayer.playMedia(currentPlaying, holder)) {
                    showPause();
                }
            } else {
                cursor.close();
            }
        }
    }

    private void showPlay() {
        ImageView playButton = (ImageView) rootView.findViewById(R.id.player_play_button);
        playButton.setImageResource(R.drawable.glideplayer_play_white);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });
    }

    private void showPause() {
        ImageView playButton = (ImageView) rootView.findViewById(R.id.player_play_button);
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
    public void onDestroy() {
        super.onDestroy();
        videoPlayer.close();
    }

    @Override
    public void videoSizeChanged(int width, int height) {
        SurfaceView videoSurface = (SurfaceView) rootView.findViewById(R.id.videoSurface);
        ViewGroup.LayoutParams params =
                videoSurface.getLayoutParams();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
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
        if(syncMode) {
            seek(0);
            ((Synchronization.VideoSession) Synchronization.getInstance().getActiveSession()).play(currentPlaying);
        } else {
            videoPlayer.playMedia(currentPlaying, surfaceHolder);
        }
        showPause();
    }

    public void pause(View view) {
        if(syncMode) {
            ((Synchronization.VideoSession) Synchronization.getInstance().getActiveSession()).pause();
        } else {
            videoPlayer.pause();
        }
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

    @Override
    public void onCompletion(MediaPlayer mp) {
        showPlay();
        seekBar.setProgress(0);
        videoPlayer.trueSeek(0);
    }

    @Override
    public void prepareVideo(Video video) {
        try {
            currentPlaying = video;
            videoPlayer.prepareMedia(video, surfaceHolder);
        } catch (IOException e) {}
    }

    @Override
    public void play() {
        videoPlayer.playMedia(surfaceHolder);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showPause();
            }
        });
    }

    @Override
    public int pause() {
        videoPlayer.pause();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showPlay();
            }
        });
        return videoPlayer.getTrueSeek();
    }

    @Override
    public void seek(int seek) {
        videoPlayer.trueSeek(seek);
    }
}
