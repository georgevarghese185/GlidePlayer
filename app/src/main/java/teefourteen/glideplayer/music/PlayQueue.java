package teefourteen.glideplayer.music;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;

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


}
