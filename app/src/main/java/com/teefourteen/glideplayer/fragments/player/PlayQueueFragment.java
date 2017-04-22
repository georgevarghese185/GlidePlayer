package com.teefourteen.glideplayer.fragments.player;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.teefourteen.glideplayer.Global;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.fragments.library.adapters.SongAdapter;
import com.teefourteen.glideplayer.music.Song;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class PlayQueueFragment extends Fragment {
    private View rootView;
    private NavigationListener navigationListener;
    private SongAdapter songAdapter;

    public interface NavigationListener {
        void returnToPlayer();
        void showLibrary();
    }

    public static PlayQueueFragment newInstance(NavigationListener listener,
                                                SongAdapter.SongQueueClickListener clickListener) {
        PlayQueueFragment fragment = new PlayQueueFragment();
        fragment.navigationListener = listener;
        fragment.songAdapter = new SongAdapter(
                (Global.playQueue == null)?  new ArrayList<Song>()
                        : Global.playQueue.getQueue(), clickListener);
        return fragment;
    }

    public PlayQueueFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_play_queue, container, false);

        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.play_queue_toolbar);

        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(true);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Play Queue");
        toolbar.setTitleTextColor(Color.WHITE);

        setHasOptionsMenu(true);

        RecyclerView songRecyclerView = (RecyclerView) rootView.findViewById(R.id.queue_recycler_view);

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

    public void updateList() {
        songAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.sync_play_queue, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.sync_menu_return) {
            navigationListener.returnToPlayer();
        } else if(item.getItemId() == R.id.sync_menu_add) {
            navigationListener.showLibrary();
        }

        return true;
    }
}
