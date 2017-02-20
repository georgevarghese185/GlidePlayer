package teefourteen.glideplayer.fragments.player;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.activities.PlayerActivity;
import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.music.MusicPlayer;
import teefourteen.glideplayer.music.PlayQueue;
import teefourteen.glideplayer.music.Song;
import teefourteen.glideplayer.services.PlayerService;

import static teefourteen.glideplayer.Global.playQueue;


public class PlayerFragment extends Fragment implements PlayerService.SongListener{
    private static PlayerService.PlayerServiceBinder binder;
    private ImageView albumArtView;
    private View rootView;
    SeekBar seekBar;
    private boolean userSeeking = false;
    private PlayerActivity.Navigator navigator;

    public static PlayerFragment newInstance(PlayerActivity.Navigator navigator) {
        PlayerFragment fragment = new PlayerFragment();
        fragment.navigator = navigator;
        return fragment;
    }

    public PlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        albumArtView = (ImageView) view.findViewById(R.id.player_album_art);

        view.findViewById(R.id.player_play_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });
        view.findViewById(R.id.player_next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next(v);
            }
        });
        view.findViewById(R.id.player_prev_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prev(v);
            }
        });
        view.findViewById(R.id.player_queue_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigator.showQueue();
            }
        });


        // Inflate the layout for this fragment
        rootView = view;
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        seekBar = (SeekBar)rootView.findViewById(R.id.player_track_seek);
        seekBar.setMax(MusicPlayer.MAX_SEEK_VALUE);

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
                binder.seek(newSeek);
            }
        });

        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (PlayerService.PlayerServiceBinder) service;
                initializePlayer();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {/*TODO: handle*/}
        };

        if(binder == null) {
            Intent intent = new Intent(getActivity(), PlayerService.class);
            getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        } else {
            initializePlayer();
        }
    }

    private void initializePlayer(){
        binder.registerSongListener(this);
        Intent intent = getActivity().getIntent();

        if(intent.hasExtra(PlayerActivity.EXTRA_PLAY_QUEUE)) {
            PlayQueue playQueue = intent.getParcelableExtra(PlayerActivity.EXTRA_PLAY_QUEUE);
            intent.removeExtra(PlayerActivity.EXTRA_PLAY_QUEUE);
            binder.newQueue(playQueue);
            changeSongInfo(playQueue.getCurrentPlaying(), rootView);
        } else if(intent.hasExtra(PlayerActivity.EXTRA_CHANGE_TRACK)) {
            int index = intent.getIntExtra(PlayerActivity.EXTRA_CHANGE_TRACK, 0);
            intent.removeExtra(PlayerActivity.EXTRA_CHANGE_TRACK);
            changeTrack(index);
            changeSongInfo(Global.playQueue.getSongAt(index), rootView);
        } else {
            changeSongInfo(Global.playQueue.getCurrentPlaying(), rootView);
            if(binder.isPlaying()) { showPause(); }
        }
    }

    public void play(View view) {
        binder.play();
    }

    public void pause(View view) {
        binder.pause();
        showPlay();
    }

    public void next(View view) {
        changeSongInfo(playQueue.getNext(), rootView);
        binder.next();
    }

    public void prev(View view) {
        changeSongInfo(playQueue.getPrev(), rootView);
        binder.prev();
    }

    public void changeTrack(int songIndex){
        showPlay();
        binder.changeTrack(songIndex);
    }

    private void showPlay() {
        ImageView playButton = (ImageView) rootView.findViewById(R.id.player_play_button);
        playButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });
    }

    private void showPause() {
        ImageView playButton = (ImageView) rootView.findViewById(R.id.player_play_button);
        playButton.setImageResource(R.drawable.ic_pause_white_24dp);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause(v);
            }
        });
    }

    private void changeSongInfo(Song song, View rootView) {
        seekBar.setProgress(0);

        TextView textView = (TextView) (rootView.findViewById(R.id.player_track_title));
        textView.setText(song.getTitle());
        textView = (TextView) (rootView.findViewById(R.id.player_track_album));
        textView.setText(song.getAlbum());
        textView = (TextView) (rootView.findViewById(R.id.player_track_artist));
        textView.setText(song.getArtist());

        String albumArt = song.getAlbumArt();
        if(albumArt!=null)
            albumArtView.setImageDrawable(Drawable.createFromPath(albumArt));
        else albumArtView.setImageResource(R.drawable.ic_album_white_24dp);
    }

    @Override
    public void onSongStarted() {
        showPause();
    }

    @Override
    public void onSongPlaybackFailed() {
        showPlay();
        Toast.makeText(getContext(), "Unable to play " + playQueue.getCurrentPlaying().getTitle(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTrackAutoChanged() {
        changeSongInfo(playQueue.getCurrentPlaying(), rootView);
    }

    @Override
    public void onSongStopped() {
        showPlay();
        seekBar.setProgress(0);
    }

    @Override
    public void onSeekUpdate(int currentSeek) {
        if(!userSeeking) {
            seekBar.setProgress(currentSeek);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(binder != null) {
            binder.registerSongListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(binder != null) {
            binder.unregisterSongListener(this);
        }
    }
}
