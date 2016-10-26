package teefourteen.glideplayer.fragments;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import java.util.ArrayList;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.activities.MainActivity;
import teefourteen.glideplayer.activities.PlayerActivity;
import teefourteen.glideplayer.databases.library.LibraryHelper;
import teefourteen.glideplayer.databases.library.SongTable;
import teefourteen.glideplayer.music.*;
import teefourteen.glideplayer.music.adapters.TrackAdapter;

import static teefourteen.glideplayer.activities.MainActivity.libraryDb;

public class SongsFragment extends Fragment {


    public SongsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);

        updateTrackList((ListView) view.findViewById(R.id.trackList));
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

    public boolean updateTrackList(final ListView trackList) {
        //TODO: Move this work to SplashActivity. But think about it first
        if(libraryDb==null) {
            LibraryHelper libraryHelper = new LibraryHelper(getActivity());
            libraryDb = libraryHelper.getReadableDatabase();
        }

        Cursor trackCursor = libraryDb.query(false, SongTable.TABLE_NAME,
                new String[]{SongTable._ID, SongTable.FILE_PATH, SongTable.TITLE, SongTable.ALBUM,
                        SongTable.ALBUM_ID, SongTable.ARTIST, SongTable.ARTIST_ID,
                        SongTable.DURATION},null,null,null,null,SongTable.TITLE,null);

        final TrackAdapter trackAdapter = new TrackAdapter(getActivity(), trackCursor);
        trackList.setAdapter(trackAdapter);
        trackList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Song song = (Song) parent.getItemAtPosition(position);
                        Intent intent = new Intent(getActivity(), PlayerActivity.class);

                        Cursor trackCursor = LibraryHelper.getAlbumSongs(song.getAlbumId());
                        //Temporary multi-item queue playback testing
                        trackCursor.moveToFirst();
                        while(trackCursor.getLong(trackCursor.getColumnIndex(SongTable._ID))
                                != song.getSongId()) {
                            trackCursor.moveToNext();
                        }
                        intent.putExtra(MainActivity.EXTRA_PLAY_QUEUE, new PlayQueue(trackCursor));
                        trackCursor.close();
                        startActivity(intent);
                    }
                }
        );
        return true;
    }
}