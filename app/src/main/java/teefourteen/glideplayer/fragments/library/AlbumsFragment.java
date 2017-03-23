package teefourteen.glideplayer.fragments.library;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.fragments.library.adapters.AlbumAdapter;

public class AlbumsFragment extends Fragment implements LibraryFragment.LibraryChangedListener{
    private AlbumAdapter albumAdapter;

    public AlbumsFragment() {
        // Required empty public constructor
    }

    public static AlbumsFragment newInstance(Cursor albumCursor,
                                      AlbumAdapter.AlbumClickListener listener) {
        AlbumsFragment fragment = new AlbumsFragment();
        fragment.albumAdapter = new AlbumAdapter(albumCursor, listener);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_albums, container, false);

        RecyclerView albumRecyclerView = (RecyclerView) view.findViewById(R.id.albumList);

        albumRecyclerView.setHasFixedSize(true);
        albumRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        albumRecyclerView.setAdapter(albumAdapter);

        albumRecyclerView.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                albumAdapter.cancelImageLoad(holder);
            }
        });

        albumAdapter.notifyDataSetChanged();

        return view;
    }

    @Override
    public void onLibraryChanged(Cursor newCursor) {
        albumAdapter.changeCursor(newCursor);
        albumAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(albumAdapter != null) {
            albumAdapter.closeCursor();
        }
    }
}
