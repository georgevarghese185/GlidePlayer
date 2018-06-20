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

package com.teefourteen.glideplayer.fragments.library;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.music.*;
import com.teefourteen.glideplayer.fragments.library.adapters.SongAdapter;

public class SongsFragment extends Fragment implements MusicLibraryFragment.LibraryChangedListener,
        MusicLibraryFragment.CloseCursorsListener {
    private SongAdapter songAdapter = null;
    private View rootView;

    public SongsFragment() {
    }

    public static SongsFragment newInstance(Cursor songCursor,
                                            SongAdapter.SongClickListener songClickListener) {
        SongsFragment fragment = new SongsFragment();
        fragment.songAdapter = new SongAdapter(songCursor,
                songClickListener);

        return fragment;
    }

    //temporary
    public static SongsFragment newInstance(ArrayList<Song> songList,
                                            SongAdapter.SongQueueClickListener songQueueClickListener) {
        SongsFragment fragment = new SongsFragment();
        fragment.songAdapter = new SongAdapter(songList, songQueueClickListener);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_songs, container, false);

        RecyclerView songRecyclerView = (RecyclerView) rootView.findViewById(R.id.songList);

        songRecyclerView.setHasFixedSize(true);

        songRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        songRecyclerView.setAdapter(songAdapter);

        songRecyclerView.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                songAdapter.cancelImageLoad(holder);
            }
        });

        songAdapter.notifyDataSetChanged();

        return rootView;
    }

    @Override
    public void onLibraryChanged(Cursor newCursor) {
        songAdapter.changeCursor(newCursor);
        songAdapter.notifyDataSetChanged();
    }

    @Override
    public void closeCursors() {
        if(songAdapter != null) {
            songAdapter.closeCursor();
        }
    }
}