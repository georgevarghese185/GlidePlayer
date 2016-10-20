package teefourteen.distroplayer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import java.util.ArrayList;

import teefourteen.distroplayer.activities.PlayerActivity;
import teefourteen.distroplayer.music.*;
import teefourteen.distroplayer.music.adapters.TrackAdapter;


///**
// * A simple {@link Fragment} subclass.
// * Activities that contain this fragment must implement the
// * {@link LibraryFragment.OnFragmentInteractionListener} interface
// * to handle interaction events.
// */
public class LibraryFragment extends Fragment {
    ArrayList<Song> songLibrary;
    public static final String EXTRA_PLAY_QUEUE = "play_queue";
    //private OnFragmentInteractionListener mListener;

    public LibraryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        updateLibrary();
        updateTrackList((ListView) view.findViewById(R.id.trackList));
        return view;
    }

    // TO DO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction(uri);
//        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }

    public void updateLibrary() {
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            return;

        songLibrary = Song.getSongArrayList(getContext().getContentResolver());
    }

    public boolean updateTrackList(ListView trackList) {
        if(songLibrary==null)
            return false;
        TrackAdapter trackAdapter = new TrackAdapter(getContext(),R.layout.track, songLibrary);
        trackList.setAdapter(trackAdapter);

        trackList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Song song = (Song) parent.getItemAtPosition(position);
                        Intent intent = new Intent(getActivity(), PlayerActivity.class);

                        //Temporary multi-item queue playback testing
                        ArrayList<Song> albumList = new ArrayList<Song>();
                        for(Song s : songLibrary) {
                            if (s.getAlbumId()==song.getAlbumId())
                                albumList.add(s);
                        }
                        //end of temporary code
                        
                        intent.putExtra(EXTRA_PLAY_QUEUE, new PlayQueue(albumList, 0));
                        startActivity(intent);
                    }
                }
        );
        return true;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
//    public interface OnFragmentInteractionListener {
//        // TO DO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }
}