package teefourteen.glideplayer.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

import teefourteen.glideplayer.R;

public class LibraryFragment extends Fragment {
    private TabHost tabHost;
    private SongsFragment songsFragment;
    private AlbumsFragment albumsFragment;
    private static final String SONGS_FRAGMENT_TAG = "songs_fragment";
    private static final String ALBUMS_FRAGMENT_TAG = "albums_fragment";

    public LibraryFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        tabHost = (TabHost) view.findViewById(R.id.libraryTabHost);
        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec("songsTab");
        spec.setContent(R.id.songsTab);
        spec.setIndicator("Songs");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("albumsTab");
        spec.setContent(R.id.albumsTab);
        spec.setIndicator("Albums");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("artistsTab");
        spec.setContent(R.id.artistsTab);
        spec.setIndicator("Artists");
        tabHost.addTab(spec);

        songsFragment = new SongsFragment();
        new FragmentSwitcher(getFragmentManager(), R.id.songsTab).switchTo(songsFragment,SONGS_FRAGMENT_TAG);

        albumsFragment = new AlbumsFragment();
        new FragmentSwitcher(getFragmentManager(), R.id.albumsTab).switchTo(albumsFragment, ALBUMS_FRAGMENT_TAG);

        return view;
    }
}
