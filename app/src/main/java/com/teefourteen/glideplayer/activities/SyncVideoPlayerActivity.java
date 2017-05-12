package com.teefourteen.glideplayer.activities;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.Synchronization;
import com.teefourteen.glideplayer.dialogs.QuitSyncSessionDialog;
import com.teefourteen.glideplayer.fragments.FragmentSwitcher;
import com.teefourteen.glideplayer.fragments.library.adapters.VideoAdapter;
import com.teefourteen.glideplayer.fragments.library.VideoLibraryFragment;
import com.teefourteen.glideplayer.fragments.player.VideoPlayerFragment;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.services.PlayerService;
import com.teefourteen.glideplayer.video.Video;

import java.io.File;
import java.io.IOException;

public class SyncVideoPlayerActivity extends AppCompatActivity implements VideoAdapter.VideoClickListener {
    private FragmentSwitcher fragmentSwitcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_player);

        fragmentSwitcher = new FragmentSwitcher(getSupportFragmentManager(), R.id.sync_main_container);
        fragmentSwitcher.switchTo(VideoLibraryFragment.newInstance(this), "video_lib_fragment", true);
    }

    @Override
    public void onVideoClick(Cursor videoCursor, int position) {
        videoCursor.moveToPosition(position);
        Video video = Video.toVideo(videoCursor);
        getIntent().putExtra(VideoPlayerFragment.EXTRA_VIDEO_ID, video.videoId);
        getIntent().putExtra(VideoPlayerFragment.EXTRA_VIDEO_USERNAME, video.libraryUsername);
        fragmentSwitcher.switchTo(VideoPlayerFragment.newInstance(
                (Synchronization.VideoSession) Synchronization.getInstance().getActiveSession()),
                "vid_player_fragment", true);
    }

    @Override
    public void onVideoLongClick(Cursor videoCursor, int position) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sync_video_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.switch_to_video_player) {
            fragmentSwitcher.switchTo(VideoPlayerFragment.newInstance(
                    (Synchronization.VideoSession) Synchronization.getInstance().getActiveSession()), "video_player_fragment", true);
            return true;
        }

        return false;
    }

    @Override
    public void onBackPressed() {
        if(fragmentSwitcher.getCurrentFragment() instanceof VideoPlayerFragment) {
            ((Synchronization.VideoSession) Synchronization.getInstance().getActiveSession()).setEventListener(null);
            fragmentSwitcher.switchTo(VideoLibraryFragment.newInstance(this), "video_lib_fragment", true);
        } else {
            QuitSyncSessionDialog.newInstance(new QuitSyncSessionDialog.UserOptionListener() {
                @Override
                public void okay() {
                    Synchronization.getInstance().leaveSession();

                    File lastQueue = new File(PlayerService.PLAY_QUEUE_FILE_PATH);

                    if (lastQueue.exists()) {
                        try {
                            Global.playQueue = new PlayQueue(lastQueue);
                        } catch (IOException e) {
                            Global.playQueue = null;
                            lastQueue.delete();
                        }
                    }

                    SyncVideoPlayerActivity.super.onBackPressed();
                }
            }).show(getFragmentManager(), "quit_sync_session_dialog");
        }
    }
}
