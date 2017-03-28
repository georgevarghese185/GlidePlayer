package com.teefourteen.glideplayer.fragments.library.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import com.teefourteen.glideplayer.AsyncImageLoader;
import com.teefourteen.glideplayer.CancellableAsyncTaskHandler;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.connectivity.ShareGroup;
import com.teefourteen.glideplayer.connectivity.RemoteAlbumCoverLoader;
import com.teefourteen.glideplayer.music.database.AlbumTable;
import com.teefourteen.glideplayer.music.database.ArtistTable;
import com.teefourteen.glideplayer.music.database.Library;
import com.teefourteen.glideplayer.music.Song;
import com.teefourteen.glideplayer.music.database.SongTable;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongHolder> {
    private Cursor songCursor;
    private ArrayList<Song> songList; //temporary
    private AsyncImageLoader asyncImageLoader = new AsyncImageLoader(2);
    private RemoteAlbumCoverLoader remoteCoverLoader = new RemoteAlbumCoverLoader(1, asyncImageLoader);
    private final Source source;
    private SongClickListener songClickListener;
    private SongQueueClickListener songQueueClickListener; //temporary

    private enum Source {
        CURSOR,
        ARRAY_LIST
    }
    
    public interface SongClickListener {
        void onSongClicked(Cursor songCursor, int position);
    }
    
    //temporary
    public interface SongQueueClickListener {
        void onSongClicked(ArrayList<Song> songList, int position);
    }


    class SongHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private View trackView;
        private Song song;
        private int position;
        private TextView trackArtist;
        private TextView trackAlbum;
        private TextView trackTitle;
        private ImageView trackAlbumArt;

        SongHolder(View itemView) {
            super(itemView);
            this.trackView = itemView;

            trackArtist = (TextView) trackView.findViewById(R.id.trackArtist);
            trackAlbum = (TextView) trackView.findViewById(R.id.trackAlbum);
            trackTitle = (TextView) trackView.findViewById(R.id.trackTitle);
            trackAlbumArt = (ImageView) trackView.findViewById(R.id.trackAlbumArt);
            
            itemView.setOnClickListener(this);
        }

        void bindView(Cursor songCursor, int position) {
            this.position = position;
            songCursor.moveToPosition(position);

            bindView(Song.toSong(songCursor), position);
        }

        void bindView(Song song, int position) {
            this.position = position;
            this.song = song;

            trackAlbum.setText(song.getAlbum());
            String string = song.getArtist();
            if(!string.equals("<unknown>"))
                trackArtist.setText(string);
            else trackArtist.setText(R.string.track_artist);
            trackTitle.setText(song.getTitle());

            trackAlbumArt.setImageResource(R.drawable.ic_album_white_24dp);

            fetchCover();
        }

        private void fetchCover() {
            String path = song.getAlbumArt();
            if (path != null && new File(path).exists()) {
                Object tag = trackAlbumArt.getTag();
                if (tag != null) {
                    if(tag instanceof AsyncImageLoader.ImageLoadTask) {
                        asyncImageLoader.cancelTask((CancellableAsyncTaskHandler.Task) tag);
                    } else if(tag instanceof RemoteAlbumCoverLoader.RemoteCoverTask) {
                        remoteCoverLoader.cancelTask((CancellableAsyncTaskHandler.Task) tag);
                    }
                }
                
                AsyncImageLoader.ImageLoadTask imageLoadTask =
                        asyncImageLoader.loadImageAsync(trackAlbumArt, path);
                
                trackAlbumArt.setTag(imageLoadTask);
            } else if (song.isRemote()) {
                fetchRemoteCover();
            }
        }

        private void fetchRemoteCover() {
            String username = song.getLibraryUsername();
            long albumId = song.getAlbumId();
            String path = song.getAlbumArt();

            if (trackAlbumArt.getTag() != null) {
                cancelAlbumArt();
            }

            RemoteAlbumCoverLoader.RemoteCoverTask task =
                    remoteCoverLoader.loadRemoteCover(trackAlbumArt, username, albumId, path);
            trackAlbumArt.setTag(task);
        }

        private void cancelAlbumArt() {
            Object tag = trackAlbumArt.getTag();

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
            if (source == Source.CURSOR) {
                songClickListener.onSongClicked(songCursor, position);
            } else {
                songQueueClickListener.onSongClicked(songList, position);
            }
        }
    }



    public SongAdapter(Cursor songCursor, SongClickListener songClickListener) {
        this.songCursor = songCursor;
        source = Source.CURSOR;
        this.songClickListener = songClickListener;
    }

    public SongAdapter(ArrayList<Song> songList, SongQueueClickListener songQueueClickListener) {
        this.songList = songList;
        source = Source.ARRAY_LIST;
        this.songQueueClickListener = songQueueClickListener;
    }

    @Override
    public SongHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track, parent, false);

        return new SongHolder(view);
    }

    public void cancelImageLoad(RecyclerView.ViewHolder viewHolder) {
        if(viewHolder instanceof SongHolder) {
            ((SongHolder)viewHolder).cancelAlbumArt();
        }
    }

    @Override
    public void onBindViewHolder(SongHolder holder, int position) {
        if(source == Source.CURSOR) {
            songCursor.moveToPosition(position);
            holder.bindView(songCursor, position);
        } else {
            holder.bindView(songList.get(position), position);
        }
    }

    @Override
    public int getItemCount() {
        if(source == Source.CURSOR) {
            return songCursor.getCount();
        } else {
            return songList.size();
        }
    }

    public void changeCursor(Cursor cursor) {
        songCursor.close();
        songCursor = cursor;
    }

    public void closeCursor() {
        if(songCursor != null) {
            songCursor.close();
        }
    }
}
