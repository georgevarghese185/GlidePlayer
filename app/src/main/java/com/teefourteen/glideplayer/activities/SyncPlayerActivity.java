package com.teefourteen.glideplayer.activities;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.teefourteen.glideplayer.EasyHandler;
import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.Synchronization;
import com.teefourteen.glideplayer.dialogs.QuitSyncSessionDialog;
import com.teefourteen.glideplayer.fragments.FragmentSwitcher;
import com.teefourteen.glideplayer.fragments.library.MusicLibraryFragment;
import com.teefourteen.glideplayer.fragments.library.adapters.AlbumAdapter;
import com.teefourteen.glideplayer.fragments.library.adapters.SongAdapter;
import com.teefourteen.glideplayer.fragments.player.PlayQueueFragment;
import com.teefourteen.glideplayer.fragments.player.PlayerFragment;
import com.teefourteen.glideplayer.fragments.player.SyncPlayerFragment;
import com.teefourteen.glideplayer.music.PlayQueue;
import com.teefourteen.glideplayer.music.Song;
import com.teefourteen.glideplayer.services.PlayerService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SyncPlayerActivity extends AppCompatActivity implements SongAdapter.SongClickListener,
        AlbumAdapter.AlbumClickListener, SongAdapter.SongQueueClickListener,
        Synchronization.MusicSession.EventListener{
    private SyncPlayerFragment playerFragment;
    private static final String PLAYER_FRAGMENT_TAG = "sync_player_fragment";
    private PlayQueueFragment playQueueFragment;
    private static final String PLAY_QUEUE_TAG = "sync_play_queue_fragment";
    private FragmentSwitcher fragmentSwitcher;
    private static final String LIBRARY_FRAGMENT_TAG = "sync_library_fragment";

    private Synchronization.MusicSession session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_player);

        session = (Synchronization.MusicSession) Synchronization.getInstance().getActiveSession();
        session.initialize(this, new Synchronization.MusicSession.InitListener() {
            @Override
            public void initialized() {

            }
        }, this);
        fragmentSwitcher = new FragmentSwitcher(getSupportFragmentManager(),  R.id.sync_main_container);
        playerFragment = SyncPlayerFragment.newInstance(new PlayerFragment.ShowQueueListener() {
            @Override
            public void showQueue() {
                switchToPlayQueue();
            }
        });

        if(Global.playQueue.getQueue().size() == 0) {
           switchToPlayQueue();
        } else {
            fragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG, true);
        }
    }

    private void switchToPlayQueue() {
        final PlayQueueFragment.NavigationListener navigationListener =
                new PlayQueueFragment.NavigationListener() {
                    @Override
                    public void returnToPlayer() {
                        if(Global.playQueue.getQueue().size() == 0) {
                            Toast.makeText(getApplicationContext(), "Add some songs to the queue first",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            fragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG, true);
                        }
                    }

                    @Override
                    public void showLibrary() {
                        fragmentSwitcher.switchTo(MusicLibraryFragment.newInstance(
                                SyncPlayerActivity.this, SyncPlayerActivity.this),
                                LIBRARY_FRAGMENT_TAG, true);
                    }
                };

        playQueueFragment = PlayQueueFragment.newInstance(navigationListener,
                this);
        fragmentSwitcher.switchTo(playQueueFragment, PLAY_QUEUE_TAG, true);
    }

    @Override
    public void onBackPressed() {
        if(fragmentSwitcher.getCurrentFragment() instanceof MusicLibraryFragment) {
            switchToPlayQueue();
        } else {
            QuitSyncSessionDialog.newInstance(new QuitSyncSessionDialog.UserOptionListener() {
                @Override
                public void okay() {
                    Synchronization.getInstance().leaveSession();

                    Intent intent = new Intent(SyncPlayerActivity.this, PlayerService.class);
                    intent.putExtra(PlayerService.EXTRA_CLEAR_PLAYER, 0);
                    SyncPlayerActivity.this.startService(intent);

                    Global.playQueue = null;

                    File lastQueue = new File(PlayerService.PLAY_QUEUE_FILE_PATH);

                    if (lastQueue.exists()) {
                        try {
                            Global.playQueue = new PlayQueue(lastQueue);
                        } catch (IOException e) {
                            Global.playQueue = null;
                            lastQueue.delete();
                        }
                    }

                    SyncPlayerActivity.super.onBackPressed();
                }
            }).show(getFragmentManager(), "quit_sync_session_dialog");
        }
    }

    @Override
    public void onAlbumClicked(Cursor albumCursor, int position) {

    }

    @Override
    public void onSongClicked(Cursor songCursor, int position) {
        songCursor.moveToPosition(position);
        Global.playQueue = new PlayQueue(songCursor);
        getIntent().putExtra(PlayerActivity.EXTRA_CHANGE_TRACK,0);
        fragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG, true);
    }

    @Override
    public void onSongLongClicked(Cursor songCursor, int position) {
        songCursor.moveToPosition(position);
        final Song song = Song.toSong(songCursor);

        session.addSong(song, new Synchronization.MusicSession.AddSongListener() {
            @Override
            public void songAdded() {
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SyncPlayerActivity.this,
                                song.getTitle() + " added to queue",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void songAddFailed() {
                EasyHandler.executeOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SyncPlayerActivity.this,
                                "Failed to add song",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onSongClicked(ArrayList<Song> songList, int position) {
        getIntent().putExtra(PlayerActivity.EXTRA_CHANGE_TRACK, position);
        fragmentSwitcher.switchTo(playerFragment,PLAYER_FRAGMENT_TAG,true);
    }


    @Override
    public void queueUpdated() {
        playQueueFragment.updateList();
    }
}