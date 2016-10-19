package teefourteen.distroplayer.activities;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import teefourteen.distroplayer.LibraryFragment;
import teefourteen.distroplayer.R;
import teefourteen.distroplayer.music.MusicService;
import static teefourteen.distroplayer.MainActivity.playQueue;

public class PlayerActivity extends AppCompatActivity {
    public static PlayerActivityHandler playerActivityHandler;

    public class PlayerActivityHandler extends Handler {
        PlayerActivityHandler() { super(getMainLooper());}

        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case MusicService.SONG_STARTED:
                    songStarted();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        playQueue = intent.getParcelableExtra(LibraryFragment.EXTRA_PLAY_QUEUE);

        findViewById(R.id.playerPlayButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });

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

        Button playButton = (Button) findViewById(R.id.playerPlayButton);
        playButton.setText("PLAY");
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });
    }

    private void songStarted() {
        Button playButton = (Button) findViewById(R.id.playerPlayButton);
        playButton.setText("PAUSE");

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause(v);
            }
        });
    }
}
