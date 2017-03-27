package com.teefourteen.glideplayer.music;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.teefourteen.glideplayer.AsyncImageLoader;
import com.teefourteen.glideplayer.R;
import com.teefourteen.glideplayer.music.database.Library;

/**
 * Created by george on 15/10/16.
 */
public class PlayQueue implements Parcelable {

    private ArrayList<Song> queue;
    private int currentPlaying;

    public PlayQueue(ArrayList<Song> queue, int currentPlaying) {
        this.queue = queue;
        if(currentPlaying>=0 && currentPlaying<= queue.size())
            this.currentPlaying = currentPlaying;
    }

    public PlayQueue(Song song) {
        queue = new ArrayList<Song>();
        queue.add(song);
        currentPlaying = 0;
    }
    /** Initializes play queue from cursor and sets the initially pointed song as the current*/
    public PlayQueue(Cursor cursor) {
        queue = new ArrayList<>();
        currentPlaying = cursor.getPosition();
        cursor.moveToFirst();
        do {
            queue.add(Song.toSong(cursor));
        } while (cursor.moveToNext());
    }

    public PlayQueue(Cursor cursor, ArrayList<Integer> selections){
        currentPlaying = 0;
        queue = new ArrayList<>();
        for(Integer i : selections){
            cursor.moveToPosition(i);
            queue.add(Song.toSong(cursor));
        }
    }

    public PlayQueue(File playQueueFile)throws IOException {
        queue = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(playQueueFile)));

        for (String string = reader.readLine(); string != null; string = reader.readLine()) {
            int songId = Integer.parseInt(string);
            Cursor cursor = Library.getSong(null, songId);
            if(cursor.moveToFirst()) {
                Song song = Song.toSong(cursor);
                cursor.close();
                queue.add(song);
            }
        }

        if(queue.size()<1) {
            throw new IOException("Invalid queue");
        }
    }


    public Song getCurrentPlaying() {
        return queue.get(currentPlaying);
    }

    public int getIndex() {
        return currentPlaying;
    }

    public ArrayList<Song> getQueue() { return queue; }


    public Song changeTrack(int index) {
        return queue.get(setCurrentPlaying(index));
    }

    synchronized public int setCurrentPlaying(int index) {
        if(index>=0 && index<= queue.size()) {
            currentPlaying = index;
            return index;
        }
        else
            return 0;
    }

    synchronized public Song next() {
        if(currentPlaying< queue.size()-1)
            return queue.get(++currentPlaying);
        else {
            currentPlaying= 0;
            return queue.get(currentPlaying);
        }
    }

    public Song getNext() {
        if(currentPlaying == queue.size()-1) return queue.get(0);
        else return queue.get(currentPlaying+1);
    }

    public Song getPrev() {
        if(currentPlaying == 0) return queue.get(queue.size()-1);
        return queue.get(currentPlaying-1);
    }

    public Song getSongAt(int songIndex) { return queue.get(songIndex); }

    synchronized public Song prev() {
        if(currentPlaying>0)
            return queue.get(--currentPlaying);
        else {
            currentPlaying = queue.size() - 1;
            return queue.get(currentPlaying);
        }
    }

    public boolean isAtLastSong() {
        return (currentPlaying == queue.size()-1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(queue);
        dest.writeInt(currentPlaying);
    }

    public static Parcelable.Creator CREATOR = new Parcelable.Creator<PlayQueue>() {
        @Override
        public PlayQueue createFromParcel(Parcel source) {
            ArrayList<Song> songQueue = source.createTypedArrayList(Song.CREATOR);
            int currentPlaying = source.readInt();

            return new PlayQueue(songQueue, currentPlaying);
        }

        @Override
        public PlayQueue[] newArray(int size) {
            return new PlayQueue[size];
        }
    };

    public SongListAdapter getListAdapter(Context context) {
        return new SongListAdapter(context, queue);
    }

    public void saveQueueToFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            PrintWriter printWriter = new PrintWriter(new FileOutputStream(file), true);
            int songsSaved=0;
            for (Song song : queue) {
                if(!song.isRemote()) {
                    printWriter.println(song.get_id());
                    songsSaved++;
                }
            }

            printWriter.close();

            if(songsSaved == 0) {
                file.delete();
            }
        } catch (IOException e) {
            Log.d("save queue", "failed to save queue");
            e.printStackTrace();
        }
    }

    /** Returns true if queue is empty and playback can't continue. False otherwise */
    public boolean removeRemoteSongs(String userName) {
        for(int i=0;i<queue.size();) {
            Song song = queue.get(i);
            if(song.isRemote() && song.getLibraryUsername().equals(userName)) {
                queue.remove(i);
            } else {
                i++;
            }
        }

        return (queue.size() == 0);
    }

    public class SongListAdapter extends ArrayAdapter<Song> {
        private AsyncImageLoader asyncImageLoader = new AsyncImageLoader(2);

        public SongListAdapter(Context context, ArrayList<Song> songs) {
            super(context, 0, songs);
        }

        public void cancelImageLoad(View view) {
            if(view.getTag() != null) {
                asyncImageLoader.cancelTask((AsyncImageLoader.ImageLoadTask) view.getTag());
            }
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Song song = getItem(position);

            if(convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.track, parent,false);
            }

            TextView trackArtist = (TextView) convertView.findViewById(R.id.trackArtist);
            TextView trackAlbum = (TextView) convertView.findViewById(R.id.trackAlbum);
            TextView trackTitle = (TextView) convertView.findViewById(R.id.trackTitle);
            ImageView trackAlbumArt = (ImageView) convertView.findViewById(R.id.trackAlbumArt);

            trackAlbum.setText(song.getAlbum());
            String string = song.getArtist();
            if(!string.equals("<unknown>"))
                trackArtist.setText(string);
            else trackArtist.setText(R.string.track_artist);
            trackTitle.setText(song.getTitle());

            trackAlbumArt.setImageResource(R.drawable.ic_album_black_24dp);

            String path = song.getAlbumArt();
            if(path != null) {
                AsyncImageLoader.ImageLoadTask task = asyncImageLoader.loadImageAsync(trackAlbumArt, path);
                if(convertView.getTag() != null) {
                    asyncImageLoader.cancelTask((AsyncImageLoader.ImageLoadTask) convertView.getTag());
                }
                convertView.setTag(task);
                asyncImageLoader.loadAsync(task);
            }

            return convertView;
        }
    }

}
