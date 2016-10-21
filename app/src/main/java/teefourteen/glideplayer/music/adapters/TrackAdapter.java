package teefourteen.glideplayer.music.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import teefourteen.glideplayer.R;
import teefourteen.glideplayer.music.Song;

/**
 * Created by george on 14/10/16.
 */
public class TrackAdapter extends ArrayAdapter<Song>{

    private @LayoutRes int layoutResource;

    public TrackAdapter(Context context, @LayoutRes int resource, ArrayList<Song> songList) {
        super(context, resource, songList);
        layoutResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Song song = super.getItem(position);
        View trackView;

        LayoutInflater trackLayoutInflater = LayoutInflater.from(getContext());
        trackView = trackLayoutInflater.inflate(layoutResource, parent, false);

        TextView textView = (TextView) trackView.findViewById(R.id.trackArtist);
        textView.setText(song.getArtist());

        textView = (TextView) trackView.findViewById(R.id.trackTitle);
        textView.setText(song.getTitle());

        textView = (TextView) trackView.findViewById(R.id.trackAlbum);
        textView.setText(song.getAlbum());

        return trackView;
    }
}
