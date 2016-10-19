package teefourteen.distroplayer.music;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;

/**
 * Created by george on 15/10/16.
 */
public class PlayQueue implements Parcelable {

    private ArrayList<Song> playQueue;
    private int currentPlaying;

    public PlayQueue(ArrayList<Song> playQueue, int currentPlaying) {
        this.playQueue = playQueue;
        if(currentPlaying>=0 && currentPlaying<=playQueue.size())
            this.currentPlaying = currentPlaying;
    }

    public PlayQueue(Song song) {
        playQueue = new ArrayList<Song>();
        playQueue.add(song);
        currentPlaying = 0;
    }

    public Song getCurrentPlaying() {
        return playQueue.get(currentPlaying);
    }

    public Song getSongAt(int index) {
        if(currentPlaying>=0 && currentPlaying<=playQueue.size())
            return playQueue.get(index);
        else
            return null;
    }

    synchronized public Song setCurrentPlaying(int index) {
        if(index>=0 && index<=playQueue.size()) {
            currentPlaying = index;
            return playQueue.get(currentPlaying);
        }
        else
            return null;
    }

    synchronized public Song next() {
        if(currentPlaying<playQueue.size()-1)
            return playQueue.get(++currentPlaying);
        else {
            currentPlaying= 0;
            return playQueue.get(currentPlaying);
        }
    }

    synchronized public Song prev() {
        if(currentPlaying>0)
            return playQueue.get(--currentPlaying);
        else {
            currentPlaying = playQueue.size() - 1;
            return playQueue.get(currentPlaying);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(playQueue);
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
