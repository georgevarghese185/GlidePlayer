package teefourteen.glideplayer.activities;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.fragments.FragmentSwitcher;
import teefourteen.glideplayer.fragments.player.PlayerFragment;
import teefourteen.glideplayer.fragments.library.SongsFragment;
import teefourteen.glideplayer.music.PlayQueue;
import static teefourteen.glideplayer.Global.playQueue;

public class PlayerActivity extends AppCompatActivity {
    //TODO: user interfaces instead of handler
    public static final String EXTRA_PLAY_QUEUE = "play_queue";
    public static final String EXTRA_CHANGE_TRACK = "change_track";
    private static final String PLAYER_FRAGMENT_TAG = "player_fragment";
    private static final String SONGS_FRAGMENT_TAG = "songs_fragment";
    private FragmentSwitcher playerFragmentSwitcher;
    private PlayerFragment playerFragment;

    public interface Navigator {
        void showQueue();
        void returnToPlayer();
        void returnToPlayer(int changeTrack);
    }

    Navigator playerNavigator = new Navigator() {
        @Override
        public void showQueue() {
            SongsFragment songsFragment = new SongsFragment();
            songsFragment.setupList(playQueue.getListAdapter(getApplicationContext()),
                    new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            returnToPlayer(position);
                        }
                    });
            playerFragmentSwitcher.switchTo(songsFragment, SONGS_FRAGMENT_TAG);
        }

        @Override
        public void returnToPlayer() {
            playerFragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG, true);
        }

        @Override
        public void returnToPlayer(int changeTrack) {
            getIntent().putExtra(EXTRA_CHANGE_TRACK, changeTrack);
            returnToPlayer();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerFragmentSwitcher = new FragmentSwitcher(getSupportFragmentManager(),
                R.id.fragment_player_main_container);
        playerFragment = PlayerFragment.newInstance(playerNavigator);
        playerFragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG);
    }

    @Override
    public void onBackPressed() {
        if(playerFragmentSwitcher.getCurrentFragment() == playerFragment)
            super.onBackPressed();
        else {
            playerNavigator.returnToPlayer();
        }
    }
}
