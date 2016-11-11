package teefourteen.glideplayer.fragments;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.music.adapters.AlbumAdapter;

public class AlbumsFragment extends Fragment {
    public static Cursor albumCursor;
    public static SQLiteDatabase albumArtDb = null;

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
        albumList.setAdapter(albumAdapter);

        return view;
    }

}