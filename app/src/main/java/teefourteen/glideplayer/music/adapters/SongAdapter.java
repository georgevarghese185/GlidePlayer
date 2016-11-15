package teefourteen.glideplayer.music.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import teefourteen.glideplayer.R;
import teefourteen.glideplayer.music.Library;

import teefourteen.glideplayer.fragments.library.AlbumsFragment;
import teefourteen.glideplayer.music.Song;

/**
 * Created by george on 14/10/16.
 */
public class SongAdapter extends CursorAdapter {
    private SelectionChecker checker;

    public interface SelectionChecker {
        public boolean isSelected(int position);
    }

    public void setChecker(SelectionChecker checker){this.checker = checker;}

    public SongAdapter(Context context, Cursor cursor, SelectionChecker checker) {
        super(context, cursor, 0);
        this.checker = checker;
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
        colorBackground(view, context, cursor.getPosition());


        trackAlbum.setText(Library.getString(cursor,Media.ALBUM));
        String string = Library.getString(cursor,Media.ARTIST);
        if(!string.equals("<unknown>"))
            trackArtist.setText(string);
        else trackArtist.setText(R.string.track_artist);
        trackTitle.setText(Library.getString(cursor,Media.TITLE));

        long albumId = Library.getLong(cursor, Media.ALBUM_ID);
        Drawable albumArt = Drawable.createFromPath(
                Library.getAlbumArt(albumId, AlbumsFragment.albumArtDb));
        if(albumArt!=null)
            trackAlbumArt.setImageDrawable(albumArt);
        else trackAlbumArt.setImageResource(R.drawable.record);
}

    public void colorBackground(View view, Context context, int position) {
        if(checker!=null && checker.isSelected(position))
            view.setBackgroundColor(ContextCompat.getColor(context,R.color.track_selected));
        else
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.track_unselected));
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