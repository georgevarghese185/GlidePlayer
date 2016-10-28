package teefourteen.glideplayer.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.music.Song;
import teefourteen.glideplayer.services.MusicService;
import static teefourteen.glideplayer.activities.MainActivity.playQueue;

public class PlayerActivity extends AppCompatActivity {
    public static PlayerActivityHandler playerActivityHandler;
    private ImageView albumArtView;

    public class PlayerActivityHandler extends Handler {
        PlayerActivityHandler() { super(getMainLooper());}

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case MusicService.SONG_STARTED:
                    showPause();
                    break;
                case MusicService.PLAYBACK_FAILED:
                    Toast toast = new Toast(getApplicationContext());
                    toast.setText("Unable to play " + playQueue.getCurrentPlaying().getTitle());
                    toast.setDuration(Toast.LENGTH_LONG);
                    toast.show();
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        playQueue = intent.getParcelableExtra(MainActivity.EXTRA_PLAY_QUEUE);

        findViewById(R.id.playerPlayButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });
        albumArtView = (ImageView) findViewById(R.id.playerAlbumArt);
        changeSongInfo(playQueue.getCurrentPlaying());

        intent = new Intent(this, MusicService.class);
        intent.putExtra(MusicService.EXTRA_SONG_COMMAND, MusicService.Command.PLAY_NEW_QUEUE);
        intent.putExtra(MusicService.EXTRA_PLAY_QUEUE, playQueue);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerActivityHandler = new PlayerActivityHandler();
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerActivityHandler = null;
    }

    public void play(View view) {
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra(MusicService.EXTRA_SONG_COMMAND, MusicService.Command.RESUME);
        startService(intent);
    }

    public void pause(View view) {
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra(MusicService.EXTRA_SONG_COMMAND, MusicService.Command.PAUSE);
        startService(intent);

        showPlay();
    }

    public void next(View view) {
        showPlay();
        changeSongInfo(playQueue.getNext());
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra(MusicService.EXTRA_SONG_COMMAND, MusicService.Command.PLAY_NEXT);
        startService(intent);
    }

    public void prev(View view) {
        showPlay();
        changeSongInfo(playQueue.getPrev());
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra(MusicService.EXTRA_SONG_COMMAND, MusicService.Command.PLAY_PREV);
        startService(intent);
    }

    private void showPlay() {
        Button playButton = (Button) findViewById(R.id.playerPlayButton);
        playButton.setText("PLAY");
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });
    }

    private void showPause() {
        Button playButton = (Button) findViewById(R.id.playerPlayButton);
        playButton.setText("PAUSE");

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause(v);
            }
        });
    }

    private void changeSongInfo(Song song) {
        TextView textView = (TextView)findViewById(R.id.playerTrackTitle);
        textView.setText(song.getTitle());
        textView = (TextView) findViewById(R.id.playerTrackAlbum);
        textView.setText(song.getAlbum());
        textView = (TextView) findViewById(R.id.playerTrackArtist);
        textView.setText(song.getArtist());

        String albumArt = song.getAlbumArt();
        if(albumArt!=null)
            albumArtView.setImageDrawable(Drawable.createFromPath(albumArt));
        else albumArtView.setImageResource(R.drawable.record);
    }






}
