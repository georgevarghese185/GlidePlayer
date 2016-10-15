package teefourteen.distroplayer.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import teefourteen.distroplayer.LibraryFragment;
import teefourteen.distroplayer.R;
import teefourteen.distroplayer.music.PlayQueue;

public class PlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PlayQueue playQueue = null;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Intent intent = getIntent();
        playQueue = intent.getParcelableExtra(LibraryFragment.EXTRA_PLAY_QUEUE);
        Toast.makeText(this, playQueue.getCurrentPlaying().getTitle(),Toast.LENGTH_SHORT).show();
    }
}
