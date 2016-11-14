package teefourteen.glideplayer.music;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import teefourteen.glideplayer.Library;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.fragments.AlbumsFragment;

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


    public Song getCurrentPlaying() {
        return queue.get(currentPlaying);
    }


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

    private class SongListAdapter extends ArrayAdapter<Song> {
        public SongListAdapter(Context context, ArrayList<Song> songs) {
            super(context, 0, songs);
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

            long albumId = song.getAlbumId();
            Drawable albumArt = Drawable.createFromPath(
                    Library.getAlbumArt(albumId, AlbumsFragment.albumArtDb));
            if(albumArt!=null)
                trackAlbumArt.setImageDrawable(albumArt);
            else trackAlbumArt.setImageResource(R.drawable.record);

            return convertView;
        }
    }

}
