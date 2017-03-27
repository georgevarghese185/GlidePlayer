package com.teefourteen.glideplayer.fragments.library;


import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.fragments.library.adapters.AlbumAdapter;

public class AlbumsFragment extends Fragment implements LibraryFragment.LibraryChangedListener,
        LibraryFragment.CloseCursorsListener {
    private AlbumAdapter albumAdapter;
    private int savedPosition = -1;
    private View rootView;

    public AlbumsFragment() {
        // Required empty public constructor
    }

    public static AlbumsFragment newInstance(Cursor albumCursor,
                                             final AlbumAdapter.AlbumClickListener listener) {
        final AlbumsFragment fragment = new AlbumsFragment();
        fragment.albumAdapter = new AlbumAdapter(albumCursor, new AlbumAdapter.AlbumClickListener() {
            @Override
            public void onAlbumClicked(Cursor albumCursor, int position) {
                fragment.savedPosition = position;
                listener.onAlbumClicked(albumCursor, position);
            }
        });
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_albums, container, false);

        RecyclerView albumRecyclerView = (RecyclerView) rootView.findViewById(R.id.albumList);

        albumRecyclerView.setHasFixedSize(true);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        albumRecyclerView.setLayoutManager(layoutManager);

        albumRecyclerView.setAdapter(albumAdapter);

        albumRecyclerView.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                albumAdapter.cancelImageLoad(holder);
            }
        });

        albumAdapter.notifyDataSetChanged();

        if(savedPosition != -1) {
            layoutManager.scrollToPosition(savedPosition);
        }

        return rootView;
    }

    public void resetSavedScroll() {
        savedPosition = -1;
    }

    @Override
    public void onLibraryChanged(Cursor newCursor) {
        albumAdapter.changeCursor(newCursor);
        albumAdapter.notifyDataSetChanged();
        savedPosition = -1;
    }

    @Override
    public void closeCursors() {
        if(albumAdapter != null) {
            albumAdapter.closeCursor();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }
}
