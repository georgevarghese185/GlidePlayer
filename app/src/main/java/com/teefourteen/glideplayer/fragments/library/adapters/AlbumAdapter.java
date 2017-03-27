package com.teefourteen.glideplayer.fragments.library.adapters;

import android.content.Context;
import android.database.Cursor;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

import com.teefourteen.glideplayer.AsyncImageLoader;
import com.teefourteen.glideplayer.CancellableAsyncTaskHandler;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.RemoteAlbumCoverLoader;
import com.teefourteen.glideplayer.music.database.AlbumTable;
import com.teefourteen.glideplayer.music.database.Library;

/**
 * Created by george on 2/11/16.
 */

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumHolder> {
    private Cursor albumCursor;
    private AsyncImageLoader asyncImageLoader = new AsyncImageLoader(2);
    private RemoteAlbumCoverLoader remoteCoverLoader = new RemoteAlbumCoverLoader(1, asyncImageLoader);
    private AlbumClickListener albumClickListener;

    public interface AlbumClickListener {
        void onAlbumClicked(Cursor albumCursor, int position);
    }


    public class AlbumHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private View itemView;
        private ImageView albumArt;
        private TextView albumName;
        private TextView artistName;
        private int position;

        AlbumHolder(View itemView) {
            super(itemView);

            albumArt = (ImageView) itemView.findViewById(R.id.album_album_art);
            albumName = (TextView) itemView.findViewById(R.id.album_album_name);
            artistName = (TextView) itemView.findViewById(R.id.album_album_artist);

            itemView.setOnClickListener(this);
        }

        void bindView(Cursor albumCursor, int position) {
            this.position = position;
            albumCursor.moveToPosition(position);

            String string = Library.getString(albumCursor, AlbumTable.Columns.ALBUM_NAME);
            if(string.equals("<unknown>")) {
                albumName.setText("Unknown Album");
            } else {
                albumName.setText(string);
            }

            string = Library.getString(albumCursor, AlbumTable.Columns.ARTIST);
            if(string.equals("<unknown>")) {
                artistName.setText("Unknown Artist");
            } else {
                artistName.setText(string);
            }

            String path = Library.getString(albumCursor, AlbumTable.Columns.ALBUM_ART);

            albumArt.setImageResource(R.drawable.ic_album_white_24dp);

            fetchCover(path, albumCursor);
        }

        private void fetchCover(String path, Cursor albumCursor) {
            if (path != null && new File(path).exists()) {
                Object tag = albumArt.getTag();
                if (tag != null) {
                    if(tag instanceof AsyncImageLoader.ImageLoadTask) {
                        asyncImageLoader.cancelTask((CancellableAsyncTaskHandler.Task) tag);
                    } else if(tag instanceof RemoteAlbumCoverLoader.RemoteCoverTask) {
                        remoteCoverLoader.cancelTask((CancellableAsyncTaskHandler.Task) tag);
                    }
                }

                AsyncImageLoader.ImageLoadTask imageLoadTask =
                        asyncImageLoader.loadImageAsync(albumArt, path);

                albumArt.setTag(imageLoadTask);
            } else {
                if(Library.getInt(albumCursor, AlbumTable.Columns.IS_REMOTE) == 1)
                fetchRemoteCover(path, albumCursor);
            }
        }

        private void fetchRemoteCover(String path, Cursor albumCursor) {
            String username = Library.getString(albumCursor, AlbumTable.Columns.REMOTE_USERNAME);
            long albumId = Library.getLong(albumCursor, AlbumTable.Columns.ALBUM_ID);

            if (albumArt.getTag() != null) {
                cancelAlbumArt();
            }

            RemoteAlbumCoverLoader.RemoteCoverTask task =
                    remoteCoverLoader.loadRemoteCover(albumArt, username, albumId, path);
            albumArt.setTag(task);
        }

        private void cancelAlbumArt() {
            Object tag = albumArt.getTag();

            if(tag != null) {
                if(tag instanceof AsyncImageLoader.ImageLoadTask) {
                    asyncImageLoader.cancelTask((CancellableAsyncTaskHandler.Task) tag);
                } else if(tag instanceof RemoteAlbumCoverLoader.RemoteCoverTask) {
                    remoteCoverLoader.cancelTask((CancellableAsyncTaskHandler.Task) tag);
                }
            }
        }

        @Override
        public void onClick(View v) {
            if(albumClickListener != null) {
                albumClickListener.onAlbumClicked(albumCursor, position);
            }
        }
    }

    public AlbumAdapter(Cursor albumCursor, AlbumClickListener listener) {
        this.albumCursor = albumCursor;
        albumClickListener = listener;
    }

    @Override
    public AlbumHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.album, parent, false);

        return new AlbumHolder(view);
    }

    public void cancelImageLoad(RecyclerView.ViewHolder viewHolder) {
        if(viewHolder instanceof AlbumHolder) {
            ((AlbumHolder)viewHolder).cancelAlbumArt();
        }
    }

    @Override
    public void onBindViewHolder(AlbumHolder holder, int position) {
        albumCursor.moveToPosition(position);
        holder.bindView(albumCursor, position);
    }

    @Override
    public int getItemCount() {
        return albumCursor.getCount();
    }

    public void changeCursor(Cursor cursor) {
        albumCursor.close();
        albumCursor = cursor;
    }

    public void closeCursor() {
        if(albumCursor != null) {
            albumCursor.close();
        }
    }
}
