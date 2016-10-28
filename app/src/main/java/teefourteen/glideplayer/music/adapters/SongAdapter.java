package teefourteen.glideplayer.music.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.databases.library.LibraryHelper;

import teefourteen.glideplayer.fragments.AlbumsFragment;
import teefourteen.glideplayer.music.Song;

/**
 * Created by george on 14/10/16.
 */
public class SongAdapter extends CursorAdapter {
    public SongAdapter(Context context, Cursor cursor) {
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
        ImageView trackAlbumArt = (ImageView) view.findViewById(R.id.trackAlbumArt);

        trackAlbum.setText(LibraryHelper.getString(cursor,Media.ALBUM));
        String string = LibraryHelper.getString(cursor,Media.ARTIST);
        if(!string.equals("<unknown>"))
            trackArtist.setText(string);
        else trackArtist.setText(R.string.track_artist);
        trackTitle.setText(LibraryHelper.getString(cursor,Media.TITLE));

        long albumId = LibraryHelper.getLong(cursor, Media.ALBUM_ID);
        Drawable albumArt = Drawable.createFromPath(
                LibraryHelper.getAlbumArt(albumId, AlbumsFragment.albumArtDb));
        if(albumArt!=null)
            trackAlbumArt.setImageDrawable(albumArt);
        else trackAlbumArt.setImageResource(R.drawable.record);
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