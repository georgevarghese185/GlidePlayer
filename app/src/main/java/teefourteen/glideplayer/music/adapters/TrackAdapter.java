package teefourteen.glideplayer.music.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.databases.library.SongTable;
import teefourteen.glideplayer.music.Song;

/**
 * Created by george on 14/10/16.
 */
public class TrackAdapter extends CursorAdapter {
    public TrackAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.track, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView trackArtist = (TextView) view.findViewById(R.id.trackArtist);
        TextView trackAlbum = (TextView) view.findViewById(R.id.trackAlbum);
        TextView trackTitle = (TextView) view.findViewById(R.id.trackTitle);

        trackAlbum.setText(cursor.getString(cursor.getColumnIndex(SongTable.ALBUM)));
        trackArtist.setText(cursor.getString(cursor.getColumnIndex(SongTable.ARTIST)));
        trackTitle.setText(cursor.getString(cursor.getColumnIndex(SongTable.TITLE)));
    }

    @Override
    public Object getItem(int position) {
        Cursor cursor = (Cursor) super.getItem(position);
        if(cursor != null) {
            return Song.toSong(cursor);
        }
        else return null;
    }
}