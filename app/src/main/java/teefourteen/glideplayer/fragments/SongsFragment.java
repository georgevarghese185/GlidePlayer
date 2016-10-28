package teefourteen.glideplayer.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.activities.MainActivity;
import teefourteen.glideplayer.activities.PlayerActivity;
import teefourteen.glideplayer.databases.library.LibraryHelper;
import teefourteen.glideplayer.music.*;
import teefourteen.glideplayer.music.adapters.SongAdapter;

public class SongsFragment extends Fragment {
    public static Cursor songCursor = null;
    private static SongAdapter songAdapter = null;
    private ListView songList = null;

    public SongsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        songList = (ListView) view.findViewById(R.id.songList);
        initSongList();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void initSongList() {

        songAdapter = new SongAdapter(getActivity(), songCursor);
        songList.setAdapter(songAdapter);
        songList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Song song = (Song) parent.getItemAtPosition(position);
                        Intent intent = new Intent(getActivity(), PlayerActivity.class);

                        Cursor albumSongsCursor = LibraryHelper.getAlbumSongs(
                                getActivity().getContentResolver(), song.getAlbumId());
                        albumSongsCursor.moveToFirst();
                        while (LibraryHelper.getLong(albumSongsCursor,LibraryHelper.AlbumArtHelper.Columns._ID)
                                != song.get_id()) {
                            albumSongsCursor.moveToNext();
                        }
                        intent.putExtra(MainActivity.EXTRA_PLAY_QUEUE, new PlayQueue(albumSongsCursor));
                        albumSongsCursor.close();
                        startActivity(intent);
                    }
                }
        );
    }
}