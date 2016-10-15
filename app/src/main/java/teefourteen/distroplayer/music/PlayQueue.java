package teefourteen.distroplayer.music;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by george on 15/10/16.
 */
public class PlayQueue implements Parcelable {

    private ArrayList<Song> songQueue;
    private Song currentPlaying;

    public PlayQueue(ArrayList<Song> songQueue, Song currentPlaying) {
        this.songQueue = songQueue;
        this.currentPlaying = currentPlaying;
    }

    public PlayQueue(Song song) {
        songQueue = new ArrayList<Song>();
        songQueue.add(song);
        currentPlaying = song;
    }

    public Song getCurrentPlaying() {
        return currentPlaying;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(songQueue);
        dest.writeParcelable(currentPlaying, 0);
    }

    public static Parcelable.Creator CREATOR = new Parcelable.Creator<PlayQueue>() {
        @Override
        public PlayQueue createFromParcel(Parcel source) {
            ArrayList<Song> songQueue = source.createTypedArrayList(Song.CREATOR);
            Song currentPlaying = source.readParcelable(Song.class.getClassLoader());

            return new PlayQueue(songQueue, currentPlaying);
        }

        @Override
        public PlayQueue[] newArray(int size) {
            return new PlayQueue[size];
        }
    };


}
