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

package com.teefourteen.glideplayer.fragments.player;

import android.view.View;

import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.connectivity.Synchronization;


public class SyncPlayerFragment extends MusicPlayerFragment {
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
