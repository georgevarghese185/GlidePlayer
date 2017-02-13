package teefourteen.glideplayer.fragments.library.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import teefourteen.glideplayer.AsyncImageLoader;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.music.database.AlbumTable;
import teefourteen.glideplayer.music.database.ArtistTable;
import teefourteen.glideplayer.music.database.Library;
import teefourteen.glideplayer.music.Song;
import teefourteen.glideplayer.music.database.SongTable;


public class SongAdapter extends CursorAdapter {
    private SelectionChecker checker;
    private AsyncImageLoader asyncImageLoader = new AsyncImageLoader(1);

    public interface SelectionChecker {
        boolean isSelected(int position);
    }

    public void setChecker(SelectionChecker checker){this.checker = checker;}

    public SongAdapter(Context context, Cursor cursor, SelectionChecker checker) {
        super(context, cursor, 0);
        this.checker = checker;
    }

    public void setForRecycling(ListView listView) {
        listView.setRecyclerListener(new AbsListView.RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                if(view.getTag() != null) {
                    asyncImageLoader.cancelTask((AsyncImageLoader.LoadTask) view.getTag());
                }
            }
        });
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


        trackAlbum.setText(Library.getString(cursor, AlbumTable.Columns.ALBUM_NAME));
        String string = Library.getString(cursor, ArtistTable.Columns.ARTIST_NAME);
        if(string!=null && !string.equals("<unknown>"))
            trackArtist.setText(string);
        else trackArtist.setText(R.string.track_artist);
        trackTitle.setText(Library.getString(cursor, SongTable.Columns.TITLE));

        trackAlbumArt.setImageResource(R.drawable.ic_album_black_24dp);

        String path = Library.getString(cursor, AlbumTable.Columns.ALBUM_ART);
        if(path != null) {
            AsyncImageLoader.LoadTask task = new AsyncImageLoader.LoadTask(trackAlbumArt, path);
            if(view.getTag() != null) {
                asyncImageLoader.cancelTask((AsyncImageLoader.LoadTask) view.getTag());
            }
            view.setTag(task);
            asyncImageLoader.loadAsync(task);
        }
    }

    public void colorBackground(View view, Context context, int position) {
        if(checker!=null && checker.isSelected(position))
            view.setBackgroundColor(ContextCompat.getColor(context,R.color.track_selected));
        else
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.track_unselected));
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
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