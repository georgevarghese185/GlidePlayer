package teefourteen.glideplayer.activities;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.fragments.FragmentSwitcher;
import teefourteen.glideplayer.fragments.player.PlayerFragment;
import teefourteen.glideplayer.fragments.library.SongsFragment;
import teefourteen.glideplayer.music.PlayQueue;
import static teefourteen.glideplayer.Global.playQueue;

public class PlayerActivity extends AppCompatActivity {
    public static final String EXTRA_PLAY_QUEUE = "play_queue";
    public static final String EXTRA_CHANGE_TRACK = "change_track";
    private static final String PLAYER_FRAGMENT_TAG = "player_fragment";
    private static final String SONGS_FRAGMENT_TAG = "songs_fragment";
    public static final int MESSAGE_SHOW_QUEUE = 1;
    public static final int MESSAGE_RETURN_TO_PLAYER = 2;
    private FragmentSwitcher playerFragmentSwitcher;
    private PlayerFragment playerFragment;
    static public Handler playerActivityHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerFragmentSwitcher = new FragmentSwitcher(getSupportFragmentManager(),
                R.id.fragment_player_main_container);
        playerFragment = new PlayerFragment();
        playerFragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG);
        playerActivityHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                SongsFragment songsFragment;
                if(msg.arg1 == MESSAGE_SHOW_QUEUE) {
                    songsFragment = new SongsFragment();
                    songsFragment.setupList(playQueue.getListAdapter(getApplicationContext()),
                            false,
                            new SongsFragment.SelectionHandler() {
                                @Override
                                public void handleSelection(PlayQueue playQueue, int position) {
                                    Message msg = new Message();
                                    msg.arg1 = MESSAGE_RETURN_TO_PLAYER;
                                    msg.arg2 = position;
                                    playerActivityHandler.dispatchMessage(msg);
                                }
                            });
                    playerFragmentSwitcher.switchTo(songsFragment, SONGS_FRAGMENT_TAG);

                    return true;
                }
                else if(msg.arg1 == MESSAGE_RETURN_TO_PLAYER){
                    if(msg.arg2 >= 0) {
                        getIntent().putExtra(EXTRA_CHANGE_TRACK, msg.arg2);
                    }
                    playerFragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG, true);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(playerFragmentSwitcher.getCurrentFragment() == playerFragment)
            super.onBackPressed();
        else {
            Message msg = new Message();
            msg.arg1 = MESSAGE_RETURN_TO_PLAYER;
            msg.arg2 = -1;
            playerActivityHandler.dispatchMessage(msg);
        }
    }
}
