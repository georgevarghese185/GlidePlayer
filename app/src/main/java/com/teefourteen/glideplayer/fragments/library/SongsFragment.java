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

public class SongsFragment extends Fragment implements LibraryFragment.LibraryChangedListener,
        LibraryFragment.CloseCursorsListener {
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