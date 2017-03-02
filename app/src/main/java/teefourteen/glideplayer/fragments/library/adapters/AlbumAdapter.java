package teefourteen.glideplayer.fragments.library.adapters;

import android.content.Context;
import android.database.Cursor;
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
import teefourteen.glideplayer.music.database.Library;

/**
 * Created by george on 2/11/16.
 */

public class AlbumAdapter extends CursorAdapter {
    private AsyncImageLoader asyncImageLoader = new AsyncImageLoader(2);

    public AlbumAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.album, parent, false);
    }

    public void setForRecycling(ListView listView) {
        listView.setRecyclerListener(new AbsListView.RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                if(view.getTag() != null) {
                    asyncImageLoader.cancelTask((AsyncImageLoader.ImageLoadTask)view.getTag());
                }
            }
        });
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView albumName = (TextView) view.findViewById(R.id.albumAlbumName);
        TextView artistName = (TextView) view.findViewById(R.id.albumArtist);

        ImageView albumArt = (ImageView) view.findViewById(R.id.album_art);

        albumName.setText(Library.getString(cursor, AlbumTable.Columns.ALBUM_NAME));
        artistName.setText(Library.getString(cursor, AlbumTable.Columns.ARTIST));

        albumArt.setImageResource(R.drawable.ic_album_black_24dp);

        String path = Library.getString(cursor, AlbumTable.Columns.ALBUM_ART);
        if(path!=null) {
            AsyncImageLoader.ImageLoadTask imageLoadTask =
                    asyncImageLoader.loadImageAsync(albumArt, path);

            if(view.getTag() != null) {
                asyncImageLoader.cancelTask((AsyncImageLoader.ImageLoadTask) view.getTag());
            }
            view.setTag(imageLoadTask);
        }
    }
}
