package com.teefourteen.glideplayer.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.teefourteen.glideplayer.fragments.FragmentSwitcher;
import com.teefourteen.glideplayer.fragments.connectivity.ConnectivityFragment;
import com.teefourteen.glideplayer.fragments.library.LibraryFragment;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.music.MusicPlayer;
import com.teefourteen.glideplayer.music.Song;
import com.teefourteen.glideplayer.services.PlayerService;

import static com.teefourteen.glideplayer.Global.playQueue;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        PlayerService.SongListener{

    private LibraryFragment libraryFragment;
    private ConnectivityFragment connectivityFragment;
    public static final String EXTRA_JUMP_TO_CONNECTIVITY_FRAGMENT = "jump_to_connectivity_fragment";
    private static final String LIBRARY_FRAGMENT_TAG ="songs";
    private static final String CONNECTIVITY_FRAGMENT_TAG = "connectivity";
    private FragmentSwitcher mainFragmentSwitcher;
    private ServiceConnection serviceConnection;
    private PlayerService.PlayerServiceBinder binder = null;
    private ViewGroup peekPlayerParent;
    private View peekPlayer;
    private Song currentSong = null;
    private Runnable backOverride;
    ProgressBar peekPlayerSeekBar;
    
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = this;
        findViewById(R.id.peek_player).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, PlayerActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.peek_player_play_pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {play(v);
            }
        });
        findViewById(R.id.peek_player_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next(v);
            }
        });
        findViewById(R.id.peek_player_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prev(v);
            }
        });
        

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mainFragmentSwitcher = new FragmentSwitcher(getSupportFragmentManager(), R.id.main_container);
        libraryFragment = new LibraryFragment();
        connectivityFragment = new ConnectivityFragment();

        if(getIntent().hasExtra(EXTRA_JUMP_TO_CONNECTIVITY_FRAGMENT)) {
            mainFragmentSwitcher.switchTo(connectivityFragment, CONNECTIVITY_FRAGMENT_TAG);
            getIntent().removeExtra(EXTRA_JUMP_TO_CONNECTIVITY_FRAGMENT);
        } else {
            mainFragmentSwitcher.switchTo(libraryFragment, LIBRARY_FRAGMENT_TAG);
        }

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (PlayerService.PlayerServiceBinder) service;
                initializePeekPlayer();

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        peekPlayerSeekBar = (ProgressBar)findViewById(R.id.peek_player_seek_bar);
        peekPlayerSeekBar.setMax(MusicPlayer.MAX_SEEK_VALUE);
        peekPlayer = findViewById(R.id.peek_player);
        peekPlayerParent = (ViewGroup) peekPlayer.getParent();
    }

    private void initializePeekPlayer() {
        if(playQueue!=null) {
            if(findViewById(R.id.peek_player) == null) {
                peekPlayerParent.addView(peekPlayer);
            }
            binder.registerSongListener(this);
            if(binder.isPlaying()) {
                showPause();
            } else {
                binder.restoreSavedQueue();
                peekPlayerSeekBar.setProgress(binder.getSeek());
                showPlay();
            }
            Song song = playQueue.getCurrentPlaying();
            changeTrackInfo(song);
        } else {
            if(findViewById(R.id.peek_player) != null) {
                peekPlayerParent.removeView(peekPlayer);
            }
        }
    }
    
    private void changeTrackInfo(Song song) {
        ((TextView)findViewById(R.id.peek_player_track_title)).setText(song.getTitle());
        ((TextView)findViewById(R.id.peek_player_track_artist)).setText(song.getArtist());

        if(song.getAlbumArt() != null && !song.getAlbumArt().equals("")) {
            ((ImageView) findViewById(R.id.peek_player_album_art)).setImageDrawable(
                    Drawable.createFromPath(song.getAlbumArt()));
        } else {
            ((ImageView) findViewById(R.id.peek_player_album_art)).
                    setImageResource(R.drawable.ic_album_white_24dp);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(backOverride != null) {
            backOverride.run();
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_library) {
            mainFragmentSwitcher.switchTo(libraryFragment, LIBRARY_FRAGMENT_TAG);
        } else if (id == R.id.connectivity){
            mainFragmentSwitcher.switchTo(connectivityFragment, CONNECTIVITY_FRAGMENT_TAG);
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this,"Settings coming soon(ish)", Toast.LENGTH_LONG).show();
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    public void play(View view) {
        if(binder != null) {
            binder.play();
        }
    }

    public void pause(View view) {
        if(binder != null) {
            binder.pause();
        }
    }

    public void next(View view) {
        if(binder != null) {
            binder.next();
        }
    }

    public void prev(View view) {
        if(binder != null) {
            binder.prev();
        }
    }

    public void changeTrack(int songIndex){
        if(binder != null) {
            binder.changeTrack(songIndex);
        }
    }

    private void showPlay() {
        ImageView playButton = (ImageView) findViewById(R.id.peek_player_play_pause);
        playButton.setImageResource(R.drawable.glideplayer_play_white);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });
    }

    private void showPause() {
        ImageView playButton = (ImageView) findViewById(R.id.peek_player_play_pause);
        playButton.setImageResource(R.drawable.glideplayer_pause_white);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause(v);
            }
        });
    }
    
    

    @Override
    public void onPause() {
        super.onPause();
        if(binder != null) {
            binder.unregisterSongListener(this);
        }
        binder = null;
        this.unbindService(serviceConnection);
    }

    @Override
    public void onResume() {
        super.onResume();

        //start PlayerService
        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        this.bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
    }

    @Override
    public void onSeekUpdate(int currentSeek) {
        peekPlayerSeekBar.setProgress(currentSeek);
    }

    @Override
    public void onSongStarted(Song song) {
        if(currentSong != song) {
            currentSong = song;
            changeTrackInfo(song);
        }
        
        showPause();
    }

    @Override
    public void onSongPlaybackFailed() {
        showPlay();
        Toast.makeText(this, "Unable to play " + playQueue.getCurrentPlaying().getTitle(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTrackAutoChanged() {
        changeTrackInfo(playQueue.getCurrentPlaying());
    }

    @Override
    public void onSongStopped() {
        showPlay();
    }

    @Override
    public void onPlayQueueDestroyed() {
        if(findViewById(R.id.peek_player) != null) {
            peekPlayerParent.removeView(peekPlayer);
        }
    }

    public void overrideBackButton(Runnable backOverride) {
        if(backOverride == null) {
            this.backOverride = null;
        } else {
            this.backOverride = backOverride;
        }
    }
}
