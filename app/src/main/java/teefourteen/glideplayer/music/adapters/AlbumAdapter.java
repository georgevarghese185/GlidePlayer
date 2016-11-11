package teefourteen.glideplayer.music.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.Library;

/**
 * Created by george on 2/11/16.
 */

public class AlbumAdapter extends CursorAdapter {

    public AlbumAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.album, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView albumName = (TextView) view.findViewById(R.id.albumAlbumName);
        TextView artistName = (TextView) view.findViewById(R.id.albumArtist);

        ImageView albumArt = (ImageView) view.findViewById(R.id.album_art);

        albumName.setText(Library.getString(cursor, MediaStore.Audio.Albums.ALBUM));
        artistName.setText(Library.getString(cursor, MediaStore.Audio.Albums.ARTIST));
        String path = Library.getString(cursor, MediaStore.Audio.Albums.ALBUM_ART);
        if(path!=null)
            albumArt.setImageDrawable(Drawable.createFromPath(path));
        else
            albumArt.setImageResource(R.drawable.record);
    }
}
