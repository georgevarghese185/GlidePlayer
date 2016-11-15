package teefourteen.glideplayer.fragments.player;


import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.activities.PlayerActivity;
import teefourteen.glideplayer.Global;
import teefourteen.glideplayer.music.PlayQueue;
import teefourteen.glideplayer.music.Song;
import teefourteen.glideplayer.services.PlayerService;

import static android.os.Looper.getMainLooper;
import static teefourteen.glideplayer.Global.playQueue;


public class PlayerFragment extends Fragment {
    public static PlayerFragmentHandler playerFragmentHandler;
    private ImageView albumArtView;
    private View rootView;

    public class PlayerFragmentHandler extends Handler {
        PlayerFragmentHandler() { super(getMainLooper());}

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case PlayerService.MESSAGE_SONG_STARTED:
                    showPause();
                    break;
                case PlayerService.MESSAGE_PLAYBACK_FAILED:
                    showPlay();
                    Toast toast = new Toast(getActivity());
                    toast.setText("Unable to play " + playQueue.getCurrentPlaying().getTitle());
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                    break;
            }
        }
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
                Message msg = new Message();
                msg.arg1 = PlayerActivity.MESSAGE_SHOW_QUEUE;
                PlayerActivity.playerActivityHandler.dispatchMessage(msg);
            }
        });

        Intent intent = getActivity().getIntent();
        if(intent.hasExtra(PlayerActivity.EXTRA_PLAY_QUEUE)) {
            PlayQueue playQueue = intent.getParcelableExtra(PlayerActivity.EXTRA_PLAY_QUEUE);
            intent.removeExtra(PlayerActivity.EXTRA_PLAY_QUEUE);

            intent = new Intent(getActivity(), PlayerService.class);
            intent.putExtra(PlayerService.EXTRA_SONG_COMMAND, PlayerService.Command.NEW_QUEUE);
            intent.putExtra(PlayerService.EXTRA_PLAY_QUEUE, playQueue);
            getActivity().startService(intent);
            changeSongInfo(playQueue.getCurrentPlaying(), view);
        }
        else if(intent.hasExtra(PlayerActivity.EXTRA_CHANGE_TRACK)){
            int index = intent.getIntExtra(PlayerActivity.EXTRA_CHANGE_TRACK, 0);
            changeTrack(index);
            changeSongInfo(Global.playQueue.getSongAt(index), view);
        }
        else {
            changeSongInfo(Global.playQueue.getCurrentPlaying(), view);
            Intent checkPlaying = new Intent(getContext(), PlayerService.class);
            checkPlaying.putExtra(PlayerService.EXTRA_SONG_COMMAND,
                    PlayerService.Command.CHECK_IS_PLAYING);
            getActivity().startService(checkPlaying);
        }
        // Inflate the layout for this fragment
        rootView = view;
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        playerFragmentHandler = new PlayerFragmentHandler();
    }

    @Override
    public void onPause() {
        super.onPause();
        playerFragmentHandler = null;
    }

    public void play(View view) {
        Intent intent = new Intent(getActivity(), PlayerService.class);
        intent.putExtra(PlayerService.EXTRA_SONG_COMMAND, PlayerService.Command.PLAY);
        getActivity().startService(intent);
    }

    public void pause(View view) {
        Intent intent = new Intent(getActivity(), PlayerService.class);
        intent.putExtra(PlayerService.EXTRA_SONG_COMMAND, PlayerService.Command.PAUSE);
        getActivity().startService(intent);

        showPlay();
    }

    public void next(View view) {
        changeSongInfo(playQueue.getNext(), rootView);
        Intent nextIntent = new Intent(getActivity(), PlayerService.class);
        nextIntent.putExtra(PlayerService.EXTRA_SONG_COMMAND, PlayerService.Command.PLAY_NEXT);
        getActivity().startService(nextIntent);
    }

    public void prev(View view) {
        changeSongInfo(playQueue.getPrev(), rootView);
        Intent prevIntent = new Intent(getActivity(), PlayerService.class);
        prevIntent.putExtra(PlayerService.EXTRA_SONG_COMMAND, PlayerService.Command.PLAY_PREV);
        getActivity().startService(prevIntent);
    }

    public void changeTrack(int songIndex){
        showPlay();
        Intent changeIntent = new Intent(getActivity(), PlayerService.class);
        changeIntent.putExtra(PlayerService.EXTRA_SONG_COMMAND, PlayerService.Command.CHANGE_TRACK);
        changeIntent.putExtra(PlayerService.EXTRA_SONG_INDEX, songIndex);
        getActivity().startService(changeIntent);
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
        TextView textView = (TextView) (rootView.findViewById(R.id.player_track_title));
        textView.setText(song.getTitle());
        textView = (TextView) (rootView.findViewById(R.id.player_track_album));
        textView.setText(song.getAlbum());
        textView = (TextView) (rootView.findViewById(R.id.player_track_artist));
        textView.setText(song.getArtist());

        String albumArt = song.getAlbumArt();
        if(albumArt!=null)
            albumArtView.setImageDrawable(Drawable.createFromPath(albumArt));
        else albumArtView.setImageResource(R.drawable.record);
    }
}
