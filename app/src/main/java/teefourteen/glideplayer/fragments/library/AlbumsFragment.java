package teefourteen.glideplayer.fragments.library;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.fragments.library.adapters.AlbumAdapter;

public class AlbumsFragment extends Fragment implements LibraryFragment.LibraryChangedListener{
    public static Cursor albumCursor;

    public AlbumsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_albums, container, false);
        ListView albumList = (ListView) view.findViewById(R.id.albumList);

        AlbumAdapter albumAdapter = new AlbumAdapter(getActivity(), albumCursor);
        albumAdapter.setForRecycling(albumList);
        albumList.setAdapter(albumAdapter);

        return view;
    }

    @Override
    public void onLibraryChanged(Cursor newCursor) {

    }
}
