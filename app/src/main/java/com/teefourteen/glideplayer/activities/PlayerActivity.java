/*
 *     GlidePlayer
 *     Copyright (C) 2016-2018  George Varghese M
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.teefourteen.glideplayer.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.fragments.FragmentSwitcher;
import com.teefourteen.glideplayer.fragments.library.adapters.SongAdapter;
import com.teefourteen.glideplayer.fragments.player.MusicPlayerFragment;
import com.teefourteen.glideplayer.fragments.library.SongsFragment;
import com.teefourteen.glideplayer.music.Song;

import static com.teefourteen.glideplayer.Global.playQueue;

public class PlayerActivity extends AppCompatActivity {
    //TODO: user interfaces instead of handler
    public static final String EXTRA_PLAY_QUEUE = "play_queue";
    public static final String EXTRA_CHANGE_TRACK = "change_track";
    private static final String PLAYER_FRAGMENT_TAG = "player_fragment";
    private static final String SONGS_FRAGMENT_TAG = "songs_fragment";
    private FragmentSwitcher playerFragmentSwitcher;
    private MusicPlayerFragment playerFragment;

    MusicPlayerFragment.ShowQueueListener showQueueListener = new MusicPlayerFragment.ShowQueueListener() {
        @Override
        public void showQueue() {
            SongsFragment songsFragment = SongsFragment.newInstance(playQueue.getQueue(),
                    new SongAdapter.SongQueueClickListener() {
                        @Override
                        public void onSongClicked(ArrayList<Song> songList, int position) {
                            getIntent().putExtra(EXTRA_CHANGE_TRACK, position);
                            playerFragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG, true);
                        }
                    });

            playerFragmentSwitcher.switchTo(songsFragment, SONGS_FRAGMENT_TAG);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playerFragmentSwitcher = new FragmentSwitcher(getSupportFragmentManager(),
                R.id.fragment_player_main_container);
        playerFragment = MusicPlayerFragment.newInstance(showQueueListener);
        playerFragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG);
    }

    @Override
    public void onBackPressed() {
        if(playerFragmentSwitcher.getCurrentFragment() == playerFragment)
            super.onBackPressed();
        else {
            playerFragmentSwitcher.switchTo(playerFragment, PLAYER_FRAGMENT_TAG, true);
        }
    }
}
