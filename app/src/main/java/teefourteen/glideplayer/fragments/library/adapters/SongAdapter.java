package teefourteen.glideplayer.fragments.library.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

import teefourteen.glideplayer.AsyncImageLoader;
import teefourteen.glideplayer.CancellableAsyncTaskHandler;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.connectivity.ShareGroup;
import teefourteen.glideplayer.connectivity.RemoteAlbumCoverLoader;
import teefourteen.glideplayer.music.database.AlbumTable;
import teefourteen.glideplayer.music.database.ArtistTable;
import teefourteen.glideplayer.music.database.Library;
import teefourteen.glideplayer.music.Song;
import teefourteen.glideplayer.music.database.SongTable;


public class SongAdapter extends CursorAdapter {
    private SelectionChecker checker;
    private AsyncImageLoader asyncImageLoader = new AsyncImageLoader(2);
    private RemoteAlbumCoverLoader remoteCoverLoader = new RemoteAlbumCoverLoader(1, asyncImageLoader);

    public interface SelectionChecker {
        boolean isSelected(int position);
    }

    public void setChecker(SelectionChecker checker){this.checker = checker;}

    public SongAdapter(Context context, Cursor cursor, SelectionChecker checker) {
        super(context, cursor, 0);
        this.checker = checker;
    }

    public void cancelImageLoad(View view) {
        Object tag = view.getTag();
        cancelAlbumArt(tag);
    }

    private void cancelAlbumArt(Object tag) {
        if(tag != null) {
            if(tag instanceof AsyncImageLoader.ImageLoadTask) {
                asyncImageLoader.cancelTask((CancellableAsyncTaskHandler.Task) tag);
            } else if(tag instanceof RemoteAlbumCoverLoader.RemoteCoverTask) {
                remoteCoverLoader.cancelTask((CancellableAsyncTaskHandler.Task) tag);
            }
        }
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
        if (string != null && !string.equals("<unknown>"))
            trackArtist.setText(string);
        else trackArtist.setText(R.string.track_artist);
        trackTitle.setText(Library.getString(cursor, SongTable.Columns.TITLE));

        trackAlbumArt.setImageResource(R.drawable.ic_album_black_24dp);

        String path = Library.getString(cursor, AlbumTable.Columns.ALBUM_ART);
        if (path != null && new File(path).exists()) {
            AsyncImageLoader.ImageLoadTask imageLoadTask =
                    asyncImageLoader.loadImageAsync(trackAlbumArt, path);

            if (view.getTag() != null) {
                asyncImageLoader.cancelTask((AsyncImageLoader.ImageLoadTask) view.getTag());
            }
            view.setTag(imageLoadTask);
        } else if (isRemoteSong(cursor)) {
            fetchRemoteCover(cursor, trackAlbumArt);
        }
    }

    private boolean isRemoteSong(Cursor cursor) {
        return (ShareGroup.shareGroupWeakReference != null
                && ShareGroup.shareGroupWeakReference.get() != null
                && (Library.getInt(cursor, SongTable.Columns.IS_REMOTE) == 1));
    }

    private void fetchRemoteCover(Cursor cursor, ImageView imageView) {
        String username = Library.getString(cursor, SongTable.Columns.REMOTE_USERNAME);
        long albumId = Library.getLong(cursor, AlbumTable.Columns.ALBUM_ID);
        String path = (Library.getString(cursor, AlbumTable.Columns.ALBUM_ART));

        if (imageView.getTag() != null) {
            cancelAlbumArt(imageView.getTag());
        }

        RemoteAlbumCoverLoader.RemoteCoverTask task =
                remoteCoverLoader.loadRemoteCover(imageView, username, albumId, path);
        imageView.setTag(task);
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