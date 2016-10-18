package teefourteen.distroplayer.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import teefourteen.distroplayer.LibraryFragment;
import teefourteen.distroplayer.R;
import teefourteen.distroplayer.music.MusicPlayer;
import teefourteen.distroplayer.music.PlayQueue;

public class PlayerActivity extends AppCompatActivity {

    private PlayQueue playQueue;
    private MusicPlayer musicPlayer;
    private boolean playing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Intent intent = getIntent();
        playQueue = intent.getParcelableExtra(LibraryFragment.EXTRA_PLAY_QUEUE);
        musicPlayer = new MusicPlayer(getApplicationContext());
    }

    public void play(View view) {
        if(!playing)
            playing = musicPlayer.playSong(playQueue.getCurrentPlaying());
        else {
            musicPlayer.pauseSong();
            playing = false;
        }
    }
}
