package com.teefourteen.glideplayer.fragments.player;

import android.view.View;

import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.connectivity.Synchronization;


public class SyncPlayerFragment extends PlayerFragment {
    private Synchronization.MusicSession session =
            (Synchronization.MusicSession) Synchronization.getInstance().getActiveSession();

    public static SyncPlayerFragment newInstance(ShowQueueListener showQueueListener) {
        SyncPlayerFragment fragment = new SyncPlayerFragment();
        fragment.showQueueListener = showQueueListener;
        return fragment;
    }

    @Override
    protected void initializeDefault() {

    }

    @Override
    public void play(View view) {
        session.play(Global.playQueue.getIndex());
    }

    @Override
    public void pause(View view) {
        session.pause();
    }

    @Override
    public void next(View view) {
        session.play(Global.playQueue.getNextIndex());
    }

    @Override
    public void prev(View view) {
        session.play(Global.playQueue.getPrevIndex());
    }

    @Override
    public void changeTrack(int songIndex) {
        session.play(songIndex);
    }
}
